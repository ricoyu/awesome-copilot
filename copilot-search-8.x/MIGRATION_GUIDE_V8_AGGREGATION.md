# Elasticsearch 8.x 原生 API 聚合迁移指南

## 📋 概述

本文档说明如何将聚合功能从 RestHighLevelClient (ES 7.x API) 迁移到真正的 ElasticsearchClient (ES 8.x 原生 API)。

## 🎯 目标

- ✅ 使用 `co.elastic.clients.elasticsearch.ElasticsearchClient` 执行聚合查询
- ❌ 移除对 `org.elasticsearch.client.RestHighLevelClient` 的依赖
- ✅ 保持与现有业务代码的 API 兼容性
- ✅ 复用现有的结果解析逻辑 (`AggResultSupport`)

## 🏗️ 架构设计

### 混合客户端策略(过渡期)

```java
// ElasticUtils.java
public static final ElasticsearchClient QUERY_CLIENT = ...;  // ES 8.x 原生
public static final RestHighLevelClient CLIENT = ...;        // ES 7.x (已废弃)
```

### 桥接层设计

```
┌─────────────────────────────────────────┐
│   V8TermsAggregationBuilder (新)        │
│   - 使用 ES 8.x Aggregation.Builder     │
│   - 构建 co.elastic.clients API         │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│   AggregationBridge (桥接层)            │
│   - 序列化 ES 8.x Aggregation → JSON    │
│   - 通过 RestClient 执行 HTTP 请求      │
│   - 解析响应为 7.x SearchResponse       │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│   AggResultSupport (复用)               │
│   - 解析 7.x SearchResponse             │
│   - 返回 Map<String, Object>            │
└─────────────────────────────────────────┘
```

## 📁 新增文件

### 1. AggregationBridge.java
**位置**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/support/AggregationBridge.java`

**功能**: 
- 将 ES 8.x Aggregation 序列化为 JSON
- 通过底层 RestClient 执行 HTTP 请求
- 将响应解析为 7.x SearchResponse

**核心方法**:
```java
public static SearchResponse search(
    ElasticsearchClient client, 
    String[] indices,
    Map<String, Aggregation> aggregations, 
    String queryBuilder)
```

### 2. V8TermsAggregationBuilder.java
**位置**: `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8TermsAggregationBuilder.java`

**功能**: 使用 ES 8.x 原生 API 构建 Terms 聚合

**示例**:
```java
List<Map<String, Object>> result = ElasticUtils.AggsV8.terms("bank")
    .of("age_agg", "age")
    .size(20)
    .get();
```

### 3. V8AggTest.java
**位置**: `copilot-search-8.x/src/test/java/com/awesomecopilot/search8x/V8AggTest.java`

**功能**: 测试 ES 8.x 原生 API 聚合

## 🔧 修改的文件

### 1. AbstractAggregationBuilder.java
**新增方法**:
```java
protected SearchResponse searchWithV8Client(Map<String, Aggregation> aggregations) {
    String queryJson = null;
    if (baseQueryBuilder != null) {
        QueryBuilder queryBuilder = ReflectionUtils.invokeMethod("builder", baseQueryBuilder);
        queryJson = queryBuilder.toString();
    }
    return AggregationBridge.search(ElasticUtils.QUERY_CLIENT, indices, aggregations, queryJson);
}
```

### 2. SearchResponseBridge.java
**修改**: 将 `parseSearchResponse` 从 private 改为 public,供 AggregationBridge 复用

### 3. ElasticUtils.java
**新增入口类**:
```java
public static class AggsV8 {
    public static V8TermsAggregationBuilder terms(String... indices) {
        return V8TermsAggregationBuilder.instance(indices);
    }
}
```

## 💡 使用示例

### 旧方式 (RestHighLevelClient)
```java
List<Map<String, Object>> results = ElasticUtils.Aggs.terms("employees")
    .of("jobs", "job")
    .size(20)
    .get();
```

### 新方式 (ElasticsearchClient)
```java
List<Map<String, Object>> results = ElasticUtils.AggsV8.terms("employees")
    .of("jobs", "job")
    .size(20)
    .get();
```

**API 完全一致**,只是入口从 `Aggs` 改为 `AggsV8`!

## 🚀 迁移步骤

### 阶段 1: 基础架构 (已完成 ✅)
- [x] 创建 AggregationBridge 桥接层
- [x] 修改 AbstractAggregationBuilder 支持 ES 8.x 客户端
- [x] 创建 V8TermsAggregationBuilder 示例
- [x] 添加 ElasticUtils.AggsV8 入口

### 阶段 2: 迁移其他聚合 (待进行)
需要迁移的聚合 Builder:
- [ ] ElasticMinAggregationBuilder
- [ ] ElasticMaxAggregationBuilder
- [ ] ElasticAvgAggregationBuilder
- [ ] ElasticSumAggregationBuilder
- [ ] ElasticStatsAggregationBuilder
- [ ] ElasticCardinalityAggregationBuilder
- [ ] ElasticRangeAggregationBuilder
- [ ] ElasticHistogramAggregationBuilder
- [ ] ElasticDateHistogramAggregationBuilder
- [ ] ElasticMultiTermsAggregationBuilder
- [ ] ElasticCompositeAggregationBuilder

### 阶段 3: 迁移子聚合 (待进行)
需要迁移的子聚合:
- [ ] ElasticTermsSubAggregation
- [ ] ElasticAvgSubAggregation
- [ ] ElasticMinSubAggregation
- [ ] ElasticMaxSubAggregation
- [ ] ElasticSumSubAggregation
- [ ] ElasticStatsSubAggregation
- [ ] ElasticDateHistogramSubAggregation
- [ ] ElsticHistogramSubAggregation
- [ ] ElasticTopHitsSubAggregation
- [ ] ElasticBucketSortSubAggregation

### 阶段 4: 测试验证 (待进行)
- [ ] 单元测试覆盖所有聚合类型
- [ ] 集成测试验证多层嵌套子聚合
- [ ] 性能测试对比新旧实现

### 阶段 5: 移除旧代码 (最终)
- [ ] 将所有业务代码切换到 AggsV8
- [ ] 删除旧的 Aggs 入口
- [ ] 移除 RestHighLevelClient 依赖

## ⚠️ 注意事项

### 1. API 兼容性
- ES 8.x 的 `Aggregation` 类与 7.x 的 `AggregationBuilder` 完全不同
- 需要使用 Builder 模式构建: `new TermsAggregation.Builder().field("xxx").build()`

### 2. 序列化问题
- ES 8.x Aggregation 需要通过 Jackson 序列化为 JSON
- 确保 ObjectMapper 配置正确

### 3. 响应解析
- 目前仍复用 7.x 的 `SearchResponse` 和 `AggResultSupport`
- 未来可以考虑直接解析 ES 8.x 的响应

### 4. 性能考虑
- 桥接层增加了序列化/反序列化开销
- 建议在生产环境进行性能测试

## 📊 对比分析

| 特性 | RestHighLevelClient | ElasticsearchClient |
|------|---------------------|---------------------|
| **状态** | ❌ 已废弃 (7.15+) | ✅ 官方推荐 |
| **版本支持** | ES 6.x - 8.x | ES 8.x+ |
| **API 风格** | 传统 Java API | Fluent Builder |
| **类型安全** | 部分 | 完全 |
| **JSON 序列化** | 自动 | 需手动(当前实现) |
| **维护成本** | 高(即将移除) | 低(长期支持) |

## 🎓 学习资源

- [Elasticsearch Java API Client 官方文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Migration Guide from HLRC](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/migration-java-api.html)
- [Aggregations Examples](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/aggregations.html)

## 📝 下一步行动

1. **立即**: 运行 `V8AggTest` 验证基础功能
2. **本周**: 迁移 Metric 聚合 (Min/Max/Avg/Sum/Stats)
3. **本月**: 迁移所有 Bucket 聚合和子聚合
4. **下季度**: 完全移除 RestHighLevelClient 依赖

---

**作者**: Rico Yu  
**日期**: 2026-06-09  
**版本**: 1.0
