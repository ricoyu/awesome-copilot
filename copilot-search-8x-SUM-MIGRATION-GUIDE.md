# Sum 聚合迁移指南 - Elasticsearch 8.x

## 概述

本次改造将 `Aggs.sum(String... indices)` 接口迁移到使用 Elasticsearch 8.19.14 的 `ElasticsearchClient`（QUERY_CLIENT）。

## 改造内容

### 1. 新增文件

#### V8SumAggregationBuilder.java
- **位置**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8SumAggregationBuilder.java`
- **功能**: 使用 ES 8.x 原生 API 实现 Sum 聚合
- **特点**:
  - 使用 `co.elastic.clients.elasticsearch._types.aggregations.Aggregation` 构建聚合
  - 通过 `AbstractAggregationBuilder.searchWithV8Client()` 执行查询
  - 返回 ES 8.x 原生的 `SearchResponse`

### 2. 修改文件

#### V8AggResultSupport.java
- **位置**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/support/V8AggResultSupport.java`
- **新增方法**: `sumResult(Map<String, Aggregate> aggregations, String aggName)`
- **功能**: 解析 ES 8.x Sum 聚合响应，提取总和值

#### ElasticUtils.java
- **位置**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/ElasticUtils.java`
- **新增导入**: `import com.awesomecopilot.search8x.builder.agg.v8.V8SumAggregationBuilder;`
- **新增方法**: `AggsV8.sum(String... indices)`

### 3. 测试文件

#### V8SumAggregationTest.java
- **位置**: `copilot-search-8.x/src/test/java/com/awesomecopilot/search8x/V8SumAggregationTest.java`
- **功能**: 提供 V8 Sum 聚合的使用示例和测试用例

## 使用示例

### 旧 API（基于 RestHighLevelClient）

```java
// 旧的 Aggs.sum() 方法仍然可用，但返回的是基于 7.x API 的 Builder
Double total = ElasticUtils.Aggs.sum("sales")
    .of("total_amount", "price")
    .get();
```

### 新 API（基于 ElasticsearchClient 8.x）✅ 推荐

```java
// 新的 AggsV8.sum() 方法，使用 ES 8.x 原生 API
Double total = ElasticUtils.AggsV8.sum("sales")
    .of("total_amount", "price")
    .get();
```

### 带查询条件的示例

```java
// 先构建查询条件
ElasticMatchQueryBuilder queryBuilder = ElasticUtils.Query.matchQuery("sales")
    .of("status", "completed");

// 执行带条件的 Sum 聚合
Double completedSalesTotal = ElasticUtils.AggsV8.sum("sales")
    .setQuery(queryBuilder)
    .of("completed_total", "amount")
    .fetchTotalHits(true)
    .get();

// 获取总命中数
Long totalHits = ElasticUtils.Aggs.totalHits();
```

### 多索引聚合示例

```java
// 跨多个索引进行 Sum 聚合
Double crossIndexTotal = ElasticUtils.AggsV8.sum("sales_2024", "sales_2025", "sales_2026")
    .of("multi_year_total", "revenue")
    .get();
```

## API 对比

| 特性 | 旧 API (Aggs.sum) | 新 API (AggsV8.sum) |
|------|------------------|---------------------|
| 客户端 | RestHighLevelClient (7.x) | ElasticsearchClient (8.x) |
| 聚合构建器 | ElasticSumAggregationBuilder | V8SumAggregationBuilder |
| 响应类型 | org.elasticsearch.action.search.SearchResponse | co.elastic.clients.elasticsearch.core.SearchResponse |
| 结果解析 | AggResultSupport.sumResult() | V8AggResultSupport.sumResult() |
| 状态 | ⚠️ 建议迁移 | ✅ 推荐使用 |

## 技术细节

### 1. 聚合构建

```java
private Aggregation buildV8Aggregation() {
    // 使用 function-style API 创建 sum 聚合
    return new Aggregation.Builder()
        .sum(s -> s.field(field))
        .build();
}
```

### 2. 查询执行

```java
public Double get() {
    // 构建 ES 8.x 聚合
    Map<String, Aggregation> aggregations = new HashMap<>();
    aggregations.put(name, buildV8Aggregation());
    
    // 使用 ES 8.x 客户端执行查询
    SearchResponse searchResponse = searchWithV8Client(aggregations);
    addTotalHitsToThreadLocal(searchResponse);
    
    // 使用 ES 8.x 原生解析器解析结果
    return V8AggResultSupport.sumResult(searchResponse.aggregations(), name);
}
```

### 3. 结果解析

```java
public static Double sumResult(Map<String, Aggregate> aggregations, String aggName) {
    if (aggregations == null || aggregations.isEmpty()) {
        return null;
    }
    
    Aggregate aggregate = aggregations.get(aggName);
    if (aggregate == null) {
        log.warn("Aggregation [{}] not found in response", aggName);
        return null;
    }
    
    // 处理 Sum 聚合
    if (aggregate.isSum()) {
        SumAggregate sumAggregate = aggregate.sum();
        Double value = sumAggregate.value();
        
        log.debug("Sum Aggregation [{}]: Value={}", aggName, value);
        return value;
    } else {
        log.warn("Aggregation [{}] is not a Sum aggregation, type: {}", aggName, aggregate._kind());
        return null;
    }
}
```

## 迁移建议

### 渐进式迁移策略

1. **第一阶段**: 新功能开发直接使用 `AggsV8.sum()`
2. **第二阶段**: 逐步将现有代码中的 `Aggs.sum()` 替换为 `AggsV8.sum()`
3. **第三阶段**: 将旧的 `Aggs.sum()` 标记为 `@Deprecated`
4. **第四阶段**: 在下一个大版本中移除旧 API

### 注意事项

1. **兼容性**: 新旧 API 可以同时使用，互不影响
2. **性能**: ES 8.x 客户端性能更优，建议优先使用
3. **类型安全**: ES 8.x API 提供更好的类型安全性
4. **未来支持**: Elasticsearch 官方将持续优化 8.x Java Client

## 相关文件

- [V8SumAggregationBuilder.java](file://D:\Learning\awesome-copilot\copilot-search-8.x\src\main\java\com\awesomecopilot\search8x\builder\agg\v8\V8SumAggregationBuilder.java)
- [V8AggResultSupport.java](file://D:\Learning\awesome-copilot\copilot-search-8.x\src\main\java\com\awesomecopilot\search8x\support\V8AggResultSupport.java)
- [ElasticUtils.java](file://D:\Learning\awesome-copilot\copilot-search-8.x\src\main\java\com\awesomecopilot\search8x\ElasticUtils.java)
- [V8SumAggregationTest.java](file://D:\Learning\awesome-copilot\copilot-search-8.x\src\test\java\com\awesomecopilot\search8x\V8SumAggregationTest.java)

## 参考

- [Elasticsearch Java Client 8.x 官方文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Aggregation API](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/aggregations.html)

---

**改造完成时间**: 2026-06-23  
**改造人员**: AI Assistant
