# Range Aggregation 迁移指南

## 概述

本次改造将 `Aggs.range(String... indices)` API 从使用 RestHighLevelClient (7.x) 迁移到 ElasticsearchClient (8.19.14)。

## 改动文件清单

### 1. 新增文件

#### V8RangeAggregationBuilder.java
- **路径**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8RangeAggregationBuilder.java`
- **功能**: ES 8.x 原生 Range Aggregation Builder
- **特点**:
  - 使用 `co.elastic.clients.elasticsearch._types.aggregations.Aggregation` API
  - 支持自定义范围、带 key 的范围、无界范围
  - 通过 `AbstractAggregationBuilder.searchWithV8Client()` 执行查询
  - 返回结果通过 `V8AggResultSupport.rangeResult()` 解析

#### V8RangeAggregationTest.java  
- **路径**: `copilot-search-8.x/src/test/java/com/awesomecopilot/search8x/V8RangeAggregationTest.java`
- **功能**: 使用示例和测试用例
- **包含**:
  - 基本 Range 聚合示例
  - 带自定义 Key 的 Range 聚合
  - 年龄分布 Range 聚合
  - 带查询条件的 Range 聚合
  - 新旧 API 对比测试

### 2. 修改文件

#### V8AggResultSupport.java
- **路径**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/support/V8AggResultSupport.java`
- **改动**: 新增 `rangeResult(Map<String, Aggregate>, String)` 方法
- **功能**: 解析 ES 8.x Range 聚合响应
- **实现细节**:
  - 检查聚合类型是否为 `aggregate.isRange()`
  - 遍历 buckets 数组
  - 自动处理带 key 和不带 key 的范围
  - 不带 key 时使用 `from-to` 格式生成标识

#### ElasticUtils.java
- **路径**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/ElasticUtils.java`
- **改动**: 
  - 导入 `V8RangeAggregationBuilder`
  - 在 `AggsV8` 类中新增 `range(String... indices)` 静态方法

## API 使用对比

### 旧 API（仍可用，但使用 7.x RestHighLevelClient）

```java
Map<String, Long> result = ElasticUtils.Aggs
    .range("products")
    .of("price_ranges", "price")
    .addRange(0.0, 100.0)
    .addRange(100.0, 500.0)
    .addUnboundedFrom(500.0)
    .get();
```

**内部实现**:
- 使用 `org.elasticsearch.search.aggregations.AggregationBuilders.range()`
- 通过 `SearchRequestSupport.search(ElasticUtils.QUERY_CLIENT, ...)` 执行
- 返回 7.x `SearchResponse`
- 使用 `AggResultSupport.rangeResult()` 解析

### 新 API（推荐使用，使用 8.x ElasticsearchClient）

```java
Map<String, Long> result = ElasticUtils.AggsV8
    .range("products")
    .of("price_ranges", "price")
    .addRange(0.0, 100.0)
    .addRange(100.0, 500.0)
    .addUnboundedFrom(500.0)
    .get();
```

**内部实现**:
- 使用 `co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of()`
- 通过 `AbstractAggregationBuilder.searchWithV8Client()` 执行
- 返回 8.x `SearchResponse`
- 使用 `V8AggResultSupport.rangeResult()` 解析

## 主要区别

| 特性 | 旧 API (Aggs.range) | 新 API (AggsV8.range) |
|------|---------------------|------------------------|
| 客户端 | RestHighLevelClient (7.x) | ElasticsearchClient (8.x) |
| Builder | `RangeAggregationBuilder` | `V8RangeAggregationBuilder` |
| 构建方式 | 命令式 API | 函数式 API (Lambda) |
| 响应类型 | 7.x SearchResponse | 8.x SearchResponse |
| 结果解析 | `AggResultSupport` | `V8AggResultSupport` |
| 状态 | ⚠️ 待废弃 | ✅ 推荐 |

## 迁移步骤

### 1. 代码替换

将所有使用 `ElasticUtils.Aggs.range()` 的地方改为 `ElasticUtils.AggsV8.range()`：

```diff
- Map<String, Long> result = ElasticUtils.Aggs
+ Map<String, Long> result = ElasticUtils.AggsV8
      .range("products")
      .of("price_ranges", "price")
      .addRange(0.0, 100.0)
      .get();
```

### 2. 验证功能

运行测试用例确保功能正常：

```bash
mvn test -Dtest=V8RangeAggregationTest
```

### 3. 性能对比

对比新旧 API 的性能，确保没有退化。

## 技术要点

### 1. 函数式 API 构建

ES 8.x 使用函数式 API 构建聚合：

```java
return Aggregation.of(agg -> agg
    .range(r -> {
        r.field(field);
        
        for (Range range : ranges) {
            r.ranges(rangeBuilder -> {
                if (isNotBlank(range.getKey())) {
                    rangeBuilder.key(range.getKey());
                }
                if (range.getFrom() != null) {
                    rangeBuilder.from(range.getFrom());
                }
                if (range.getTo() != null) {
                    rangeBuilder.to(range.getTo());
                }
                return rangeBuilder;
            });
        }
        
        return r;
    })
);
```

### 2. 结果解析

ES 8.x 的聚合响应类型完全不同：

```java
if (aggregate.isRange()) {
    RangeAggregate rangeAggregate = aggregate.range();
    List<?> buckets = rangeAggregate.buckets().array();
    
    for (Object obj : buckets) {
        var bucket = (RangeBucket) obj;
        String key = bucket.key();
        long docCount = bucket.docCount();
        // ...
    }
}
```

### 3. 向后兼容

- 旧的 `ElasticRangeAggregationBuilder` 仍然保留
- 标记为 `@Deprecated`（可选）
- 提供迁移时间窗口

## 注意事项

1. **依赖保留**: 仍需保留 elasticsearch-rest-high-level-client 依赖，因为其他聚合尚未迁移

2. **测试覆盖**: 每个使用场景都需要充分测试

3. **文档更新**: 更新 JavaDoc 和使用示例

4. **逐步迁移**: 建议先在新代码中使用新 API，再逐步迁移旧代码

## 后续工作

完成 Range Aggregation 迁移后，可以继续迁移其他聚合类型：

- [ ] Histogram Aggregation
- [ ] DateHistogram Aggregation  
- [ ] Min/Max/Avg/Sum Aggregation
- [ ] Stats Aggregation
- [ ] Cardinality Aggregation
- [ ] Terms Aggregation (已有 V8TermsAggregationBuilder)
- [ ] MultiTerms Aggregation (已有 V8MultiTermsAggregationBuilder)

## 相关资源

- [Elasticsearch Java Client 8.x 官方文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Aggregation API 参考](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/_aggregation_api.html)
- 项目内参考实现:
  - `V8TermsAggregationBuilder.java`
  - `V8MultiTermsAggregationBuilder.java`
  - `AggregationBridge.java`

---

**迁移完成日期**: 2026-06-20  
**负责人**: Rico Yu
