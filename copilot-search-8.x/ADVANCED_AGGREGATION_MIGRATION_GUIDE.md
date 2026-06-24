# Stats、Cardinality、Composite 聚合迁移指南

## 📋 概述

本文档记录了 `copilot-search-8.x` 模块中三个高级聚合方法的 ES 8.x 原生 API 迁移工作：
- **Stats 聚合** - 统计聚合，一次性返回 count、min、max、avg、sum
- **Cardinality 聚合** - 基数聚合，字段去重后统计唯一值数量
- **Composite 聚合** - 组合聚合，将多个聚合作为一个整体返回

---

## ✅ 已完成的工作

### 1. 创建了三个 V8 Builder 类

#### V8StatsAggregationBuilder.java
- **位置**: `src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8StatsAggregationBuilder.java`
- **功能**: 使用 ES 8.x 原生 API 实现统计聚合
- **关键方法**:
  - `of(String name, String field)` - 设置聚合名称和字段
  - `setQuery(BaseQueryBuilder queryBuilder)` - 设置查询条件
  - `fetchTotalHits(boolean fetchTotalHits)` - 是否包含总命中数
  - `get()` - 执行聚合并返回 `StatsAggResult`

**示例代码**:
```java
// 使用 ES 8.x 原生 API
StatsAggResult result = ElasticUtils.AggsV8.stats("sales")
    .of("price_stats", "price")
    .get();

long count = result.getCount();
double min = result.getMin();
double max = result.getMax();
double avg = result.getAvg();
double sum = result.getSum();
```

#### V8CardinalityAggregationBuilder.java
- **位置**: `src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8CardinalityAggregationBuilder.java`
- **功能**: 使用 ES 8.x 原生 API 实现基数聚合
- **关键方法**:
  - `of(String name, String field)` - 设置聚合名称和字段
  - `setQuery(BaseQueryBuilder queryBuilder)` - 设置查询条件
  - `fetchTotalHits(boolean fetchTotalHits)` - 是否包含总命中数
  - `get()` - 执行聚合并返回 `Long`（唯一值数量）

**示例代码**:
```java
// 使用 ES 8.x 原生 API
Long uniqueCount = ElasticUtils.AggsV8.cardinality("users")
    .of("unique_users", "user_id")
    .get();

log.info("Unique user count: {}", uniqueCount);
```

#### V8CompositeAggregationBuilder.java
- **位置**: `src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8CompositeAggregationBuilder.java`
- **功能**: 使用 ES 8.x 原生 API 实现组合聚合
- **关键方法**:
  - `addTerms(String name, String field)` - 添加 Terms 子聚合
  - `addAvg(String name, String field)` - 添加 Avg 子聚合
  - `addSum(String name, String field)` - 添加 Sum 子聚合
  - `addMin(String name, String field)` - 添加 Min 子聚合
  - `addMax(String name, String field)` - 添加 Max 子聚合
  - `addCardinality(String name, String field)` - 添加 Cardinality 子聚合
  - `fetchTotalHits(boolean fetchTotalHits)` - 是否包含总命中数
  - `get()` - 执行聚合并返回 `Map<String, Object>`（所有子聚合结果）

**示例代码**:
```java
// 使用 ES 8.x 原生 API
Map<String, Object> results = ElasticUtils.AggsV8.composite("products")
    .addTerms("categories", "category")
    .addAvg("avg_price", "price")
    .addSum("total_sales", "sales")
    .get();

List<Map<String, Object>> categories = (List) results.get("categories");
Double avgPrice = (Double) results.get("avg_price");
Double totalSales = (Double) results.get("total_sales");
```

### 2. 扩展了 V8AggResultSupport.java

添加了三个新的结果解析方法：

#### statsResult()
```java
/**
 * 解析 Stats 聚合结果
 *
 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
 * @param aggName      聚合名称
 * @return StatsAggResult 统计结果，包含 count、min、max、avg、sum
 */
public static StatsAggResult statsResult(Map<String, Aggregate> aggregations, String aggName)
```

#### cardinalityResult()
```java
/**
 * 解析 Cardinality 聚合结果
 *
 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
 * @param aggName      聚合名称
 * @return Long 去重后的唯一值数量
 */
public static Long cardinalityResult(Map<String, Aggregate> aggregations, String aggName)
```

#### compositeResult()
```java
/**
 * 解析 Composite 聚合结果（简化版，返回所有子聚合）
 *
 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
 * @return Map<String, T> 包含所有子聚合结果的 Map
 */
public static <T> Map<String, T> compositeResult(Map<String, Aggregate> aggregations)
```

### 3. 更新了 ElasticUtils.java

在 `AggsV8` 类中添加了三个新的静态方法：

```java
/**
 * Stats 聚合 (ES 8.x 原生 API)
 * 一次性返回 count、min、max、avg、sum 五个统计指标，适用于综合统计分析
 */
public static V8StatsAggregationBuilder stats(String... indices)

/**
 * Cardinality 聚合 (ES 8.x 原生 API)
 * 对字段去重后统计唯一值数量，适用于 UV 统计、独立用户数等场景
 */
public static V8CardinalityAggregationBuilder cardinality(String... indices)

/**
 * Composite 聚合 (ES 8.x 原生 API)
 * 将多个聚合作为一个整体返回，支持组合多种聚合类型
 */
public static V8CompositeAggregationBuilder composite(String... indices)
```

### 4. 创建了测试文件

- **位置**: `src/test/java/com/awesomecopilot/search8x/V8AdvancedAggregationTest.java`
- **内容**: 包含 7 个测试用例，覆盖三种聚合的基本功能和链式调用

---

## 🔄 API 对比

### Stats 聚合

#### ❌ 旧 API（已标记为 @Deprecated）
```java
import com.awesomecopilot.search8x.ElasticUtils;

StatsAggResult result = ElasticUtils.Aggs.stats("sales")
    .of("price_stats", "price")
    .get();
```

#### ✅ 新 API（推荐使用）
```java
import com.awesomecopilot.search8x.ElasticUtils;

StatsAggResult result = ElasticUtils.AggsV8.stats("sales")
    .of("price_stats", "price")
    .get();
```

### Cardinality 聚合

#### ❌ 旧 API（已标记为 @Deprecated）
```java
Long uniqueCount = ElasticUtils.Aggs.cardinality("users")
    .of("unique_users", "user_id")
    .get();
```

#### ✅ 新 API（推荐使用）
```java
Long uniqueCount = ElasticUtils.AggsV8.cardinality("users")
    .of("unique_users", "user_id")
    .get();
```

### Composite 聚合

#### ❌ 旧 API（已标记为 @Deprecated）
```java
Map<String, Object> results = ElasticUtils.Aggs.composite("products")
    .addTerms("categories", "category")
    .addAvg("avg_price", "price")
    .get();
```

#### ✅ 新 API（推荐使用）
```java
Map<String, Object> results = ElasticUtils.AggsV8.composite("products")
    .addTerms("categories", "category")
    .addAvg("avg_price", "price")
    .get();
```

---

## 📊 技术实现细节

### 1. 使用 Function-style API

ES 8.x 采用函数式构建器模式：

```java
// Stats 聚合
private Aggregation buildV8Aggregation() {
    return new Aggregation.Builder()
        .stats(s -> s.field(field))
        .build();
}

// Cardinality 聚合
private Aggregation buildV8Aggregation() {
    return new Aggregation.Builder()
        .cardinality(c -> c.field(field))
        .build();
}

// Terms 子聚合（在 Composite 中）
Aggregation termsAgg = new Aggregation.Builder()
    .terms(t -> t.field(field))
    .build();
```

### 2. 客户端使用

所有 V8 Builder 都通过 `AbstractAggregationBuilder.searchWithV8Client()` 执行查询：

```java
SearchResponse searchResponse = searchWithV8Client(aggregations);
```

这确保了使用 `co.elastic.clients.elasticsearch.ElasticsearchClient` 而非旧的 `RestHighLevelClient`。

### 3. 结果解析

使用 ES 8.x 原生的聚合类型检查和方法：

```java
// Stats 解析
if (aggregate.isStats()) {
    StatsAggregate statsAggregate = aggregate.stats();
    long count = statsAggregate.count();
    Double min = statsAggregate.min();
    // ...
}

// Cardinality 解析
if (aggregate.isCardinality()) {
    CardinalityAggregate cardinalityAggregate = aggregate.cardinality();
    Long value = cardinalityAggregate.value();
    // ...
}
```

---

## 🧪 编译验证

```bash
mvn clean compile -pl copilot-search-8.x -am -DskipTests
```

**结果**: ✅ BUILD SUCCESS

---

## 📝 使用示例

### 示例 1: 商品价格统计分析

```java
// 获取商品价格的综合统计信息
StatsAggResult stats = ElasticUtils.AggsV8.stats("products")
    .of("price_stats", "price")
    .get();

System.out.println("商品总数: " + stats.getCount());
System.out.println("最低价格: " + stats.getMin());
System.out.println("最高价格: " + stats.getMax());
System.out.println("平均价格: " + stats.getAvg());
System.out.println("价格总和: " + stats.getSum());
```

### 示例 2: 独立访客统计

```java
// 统计今日独立访客数（UV）
Long uv = ElasticUtils.AggsV8.cardinality("page_views")
    .of("unique_visitors", "visitor_id")
    .setQuery(ElasticUtils.Query.term("date", "2026-06-23"))
    .get();

System.out.println("今日独立访客数: " + uv);
```

### 示例 3: 多维度销售分析

```java
// 按类别分组，同时计算平均价格和总销售额
Map<String, Object> results = ElasticUtils.AggsV8.composite("orders")
    .addTerms("categories", "category")
    .addAvg("avg_price", "price")
    .addSum("total_revenue", "revenue")
    .addCardinality("unique_customers", "customer_id")
    .get();

// 处理结果
List<Map<String, Object>> categories = (List) results.get("categories");
Double avgPrice = (Double) results.get("avg_price");
Double totalRevenue = (Double) results.get("total_revenue");
Long uniqueCustomers = (Long) results.get("unique_customers");

System.out.println("类别分布: " + categories);
System.out.println("平均价格: " + avgPrice);
System.out.println("总收入: " + totalRevenue);
System.out.println("独立客户数: " + uniqueCustomers);
```

---

## ⚠️ 注意事项

### 1. 向后兼容性

旧的 `Aggs` 类中的方法已被标记为 `@Deprecated`，但仍然可用。建议逐步迁移到 `AggsV8`。

### 2. 返回值类型差异

- **Stats**: 返回 `StatsAggResult` 对象（与旧 API 一致）
- **Cardinality**: 返回 `Long`（与旧 API 一致）
- **Composite**: 返回 `Map<String, Object>`（简化版本，后续可扩展）

### 3. Composite 聚合的限制

当前实现的 `V8CompositeAggregationBuilder` 是简化版本：
- 不支持分页（after key）
- 不支持复杂的 sources 配置
- 仅支持预定义的子聚合类型

如需更复杂的功能，可以后续扩展。

---

## 🎯 迁移进度

| 聚合类型 | 状态 | 迁移时间 |
|---------|------|---------|
| Terms | ✅ 已完成 | 之前 |
| MultiTerms | ✅ 已完成 | 之前 |
| Range | ✅ 已完成 | 之前 |
| Histogram | ✅ 已完成 | 之前 |
| DateHistogram | ✅ 已完成 | 之前 |
| Min | ✅ 已完成 | 之前 |
| Max | ✅ 已完成 | 之前 |
| Avg | ✅ 已完成 | 之前 |
| Sum | ✅ 已完成 | 之前 |
| **Stats** | ✅ **已完成** | **2026-06-23** |
| **Cardinality** | ✅ **已完成** | **2026-06-23** |
| **Composite** | ✅ **已完成** | **2026-06-23** |

**迁移完成率**: 12/12 (100%) 🎉

---

## 📚 相关文档

- [SUM 聚合迁移指南](copilot-search-8x-SUM-MIGRATION-GUIDE.md)
- [Range 聚合迁移指南](RANGE_AGGREGATION_MIGRATION.md)
- [多 Terms 聚合迁移完成报告](MULTITERMS_MIGRATION_COMPLETE.md)

---

## 👥 作者

- **Rico Yu** ricoyu520@gmail.com

## 📅 更新日期

2026-06-23
