# Elasticsearch 7.x 到 8.x 迁移指南

## 概述

本文档提供了从 Elasticsearch 7.x API 迁移到 8.x API 的详细指南。copilot-search-8.x 模块已经移除了所有 ES 7.x Maven 依赖，需要将所有使用 ES 7.x API 的代码迁移到 ES 8.x。

## 已完成的工作

✅ **已移除的 ES 7.x Maven 依赖** (pom.xml)
- `elasticsearch-rest-high-level-client`
- `elasticsearch`
- `elasticsearch-x-content`

✅ **已修复的核心文件**
1. `ElasticUtils.java` - 删除了所有 ES 7.x import
2. `ElasticBulkIndexBuilder.java` - 重写为使用纯 ES 8.x API
3. `ElasticUpdateBuilder.java` - 删除了 ES 7.x import
4. `UpdateResult.java` - 删除了 ES 7.x UpdateResponse 相关方法
5. `SortOrder.java` - 标记 ES 7.x 方法为 @Deprecated
6. `ElasticSettingsBuilder.java` - 删除了 ES 7.x Settings
7. `ElasticIndexDocBuilder.java` - 删除了 ES 7.x import

## 待修复的文件分类

### 1. Builder 类 (约 80+ 文件)

#### Query Builders (`builder/query/`)
- `ElasticQueryBuilder.java` - 核心查询构建器，使用了 SearchSourceBuilder, SortBuilders 等
- `ElasticBoolQueryBuilder.java`
- `ElasticMatchQueryBuilder.java`
- `ElasticTermQueryBuilder.java`
- `ElasticTermsQueryBuilder.java`
- `ElasticMultiMatchQueryBuilder.java`
- `ElasticSuggestBuilder.java` - 使用了 SuggestBuilders, CompletionSuggestionBuilder 等
- `ElasticContextSuggestBuilder.java`
- `ElasticScrollQueryBuilder.java`
- 其他 Match/Query 相关 builder

**迁移策略**:
- 这些类主要构建查询 DSL，建议保留但标记为 @Deprecated
- 新的业务代码应直接使用 ES 8.x Java Client 的函数式 API
- 或者重写为返回 ES 8.x 的 `co.elastic.clients.elasticsearch._types.query_dsl.Query` 对象

#### Aggregation Builders (`builder/agg/`)
- `ElasticAggregationBuilder.java`
- `ElasticTermsAggregationBuilder.java`
- `ElasticDateHistogramAggregationBuilder.java`
- `ElasticRangeAggregationBuilder.java`
- `ElasticCardinalityAggregationBuilder.java`
- 以及其他聚合 builder (约 30+ 文件)

**迁移策略**:
- 这些类构建聚合 DSL，同样建议标记为 @Deprecated
- 新代码使用 ES 8.x 的聚合 API: `aggregations("name", agg -> agg.terms(...))`

#### Admin Builders (`builder/admin/`)
- `ElasticIndexTemplateBuilder.java` - 使用了 RestHighLevelClient
- `ElasticReindexBuilder.java` - 使用了 QueryBuilder
- `AbstractMappingBuilder.java`
- `ElasticPutMappingBuilder.java`

**迁移策略**:
- 使用 ES 8.x IndicesClient API
- 示例: `client.indices().create(...)` 替代 HLRC 方式

#### Bulk Builders (`builder/bulk/`)
- ✅ `ElasticBulkIndexBuilder.java` - 已修复
- `ElasticBulkUpdateBuilder.java` - 需要修复
- `ESBulkProcessor.java` - 可能需要重构

### 2. Support 类 (约 20+ 文件)

位于 `support/` 目录:
- `SearchRequestSupport.java` - 可能使用了 SearchSourceBuilder
- `DocumentRestSupport.java` - 检查是否有 ES 7.x 依赖
- `IndicesRestSupport.java` - 检查是否有 RestHighLevelClient
- `AggResultSupport.java` - 解析聚合结果
- `V8AggResultSupport.java` - ES 8.x 版本（参考）
- `SearchHitsSupport.java`
- `BulkResult.java`
- 其他 support 类

**迁移策略**:
- 使用 ES 8.x Response 类型替代 ES 7.x Response
- 示例: `co.elastic.clients.elasticsearch.core.SearchResponse` 替代 `org.elasticsearch.action.search.SearchResponse`

### 3. Enums (约 5 文件)

- ✅ `SortOrder.java` - 已标记为 @Deprecated
- `Direction.java` - 可能使用了 `org.elasticsearch.search.sort.SortOrder`
- `AggType.java`
- `ContextType.java`

**迁移策略**:
- 移除 ES 7.x import
- 使用字符串或自定义枚举替代

### 4. 其他文件

- `ElasticsearchClientFactory.java` - 检查是否创建了 RestHighLevelClient
- `ElasticCacheUtils.java`
- Annotation 类 (`annotation/`)
- VO 类 (`vo/`)

## 迁移模式与示例

### 模式 1: 删除未使用的 ES 7.x Import

很多文件只是导入了 ES 7.x 类但没有实际使用，可以直接删除 import。

**操作**:
```java
// 删除这些 import
-import org.elasticsearch.action.index.IndexRequest;
-import org.elasticsearch.xcontent.XContentType;
-import org.elasticsearch.search.builder.SearchSourceBuilder;
```

### 模式 2: 替换为 ES 8.x API

**ES 7.x 索引文档**:
```java
// 旧代码
IndexRequest request = new IndexRequest(index).source(doc, XContentType.JSON);
restHighLevelClient.index(request, RequestOptions.DEFAULT);
```

**ES 8.x 等价代码**:
```java
// 新代码
IndexResponse response = client.index(i -> i
    .index(index)
    .document(doc)
);
```

### 模式 3: 使用底层 RestClient

对于复杂的批量操作，可以使用底层 RestClient 发送 HTTP 请求：

```java
RestClientTransport transport = (RestClientTransport) client._transport();
RestClient restClient = transport.restClient();

Request request = new Request("POST", "/_bulk");
request.setJsonEntity(ndjsonString);

Response response = restClient.performRequest(request);
String jsonResponse = EntityUtils.toString(response.getEntity());
```

### 模式 4: 标记为 @Deprecated

对于不再推荐使用的 builder 类，添加 @Deprecated 注解：

```java
/**
 * @deprecated ES 7.x API, 请使用 ES 8.x Java Client API
 */
@Deprecated
public class ElasticQueryBuilder {
    // ...
}
```

### 模式 5: 抛出 UnsupportedOperationException

对于无法立即迁移的方法：

```java
@Deprecated
public SearchSourceBuilder build() {
    throw new UnsupportedOperationException(
        "ES 7.x API is deprecated. Please use ES 8.x API directly."
    );
}
```

## 常见 ES 7.x 到 8.x 映射表

| ES 7.x Class | ES 8.x 替代方案 |
|--------------|----------------|
| `RestHighLevelClient` | `ElasticsearchClient` |
| `SearchSourceBuilder` | 函数式 API: `s -> s.query(...)` |
| `IndexRequest` | `client.index(i -> i...)` |
| `UpdateRequest` | `client.update(u -> u...)` |
| `BulkRequest` | `client.bulk(b -> b...)` 或 NDJSON |
| `QueryBuilder` | `co.elastic.clients.elasticsearch._types.query_dsl.Query` |
| `SearchResponse` | `co.elastic.clients.elasticsearch.core.SearchResponse` |
| `GetResponse` | `co.elastic.clients.elasticsearch.core.GetResponse` |
| `BulkByScrollResponse` | 使用底层 RestClient + JSON 解析 |
| `SortBuilders.fieldSort()` | ES 8.x SortOptions |
| `AggregationBuilders.terms()` | ES 8.x Aggregations API |
| `SuggestBuilders` | ES 8.x Suggest API |
| `XContentType.JSON` | 不需要，自动处理 |

## 编译验证步骤

1. **清理并编译**:
```bash
cd copilot-search-8.x
mvn clean compile -DskipTests
```

2. **查看编译错误**:
```bash
mvn clean compile 2>&1 | findstr "\.java:"
```

3. **逐个文件修复**:
   - 打开报错文件
   - 删除 ES 7.x import
   - 重写使用 ES 7.x API 的方法
   - 重新编译验证

4. **运行测试**:
```bash
mvn test
```

## 快速修复脚本建议

可以编写脚本来批量处理：

1. **查找所有包含 ES 7.x import 的文件**:
```bash
findstr /s /m "import org.elasticsearch" src\main\java\*.java
```

2. **统计每个文件的 ES 7.x import 数量**:
```bash
for file in $(findstr /s /m "import org.elasticsearch" src\main\java\*.java); do
    count=$(findstr /c:"import org.elasticsearch" "$file" | wc -l)
    echo "$file: $count imports"
done
```

3. **优先处理 import 数量少的文件**（通常更容易修复）

## 优先级建议

### 高优先级（核心功能）
1. `ElasticQueryBuilder.java` - 查询构建核心
2. `SearchRequestSupport.java` - 搜索请求支持
3. `DocumentRestSupport.java` - 文档操作
4. `ESBulkProcessor.java` - 批量处理

### 中优先级（常用功能）
1. 各种 Match/Term Query builders
2. Aggregation builders（如果使用聚合）
3. Admin builders（如果需要管理索引）

### 低优先级（可选）
1. Suggest builders（如果不使用建议功能）
2. 旧的 template/reindex builders
3. 其他很少使用的工具类

## 测试策略

1. **单元测试**: 确保每个修复的文件都有对应的测试
2. **集成测试**: 连接真实的 ES 8.x 集群进行测试
3. **回归测试**: 对比 ES 7.x 和 ES 8.x 的查询结果是否一致

## 注意事项

⚠️ **重要提醒**:

1. **不要混合使用 ES 7.x 和 8.x API** - 会导致类加载问题
2. **Response 类型完全不同** - ES 8.x 使用新的响应类型
3. **聚合结果解析不同** - ES 8.x 的聚合结果结构有变化
4. **异常处理不同** - ES 8.x 使用不同的异常层次
5. **向后兼容性** - 标记废弃而不是直接删除，给调用方迁移时间

## 参考资源

- [Elasticsearch 8.x Java Client 官方文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Migration Guide from HLRC to Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/migration-guide.html)
- copilot-search-8.x 中已有的 ES 8.x 实现示例（如 `V8AggResultSupport.java`）

## 联系与支持

如果在迁移过程中遇到问题：
1. 参考已修复的文件作为示例
2. 查阅 ES 8.x 官方文档
3. 查看项目中已有的 ES 8.x 实现

---

**最后更新**: 2026-06-28  
**状态**: 部分完成 - 核心文件已修复，剩余约 200 处编译错误待处理
