# Elasticsearch 8.19.14 迁移状态统计报告

**生成时间**: 2026-06-20  
**项目**: copilot-search-8.x  
**目标版本**: Elasticsearch 8.19.14 (ElasticsearchClient)

---

## 📊 总体概况

### 客户端使用情况
- **QUERY_CLIENT** (ElasticsearchClient 8.x): ✅ 已用于查询、文档 CRUD
- **CLIENT** (RestHighLevelClient 7.x): ⚠️ 仍用于聚合、集群管理、部分索引管理

### 迁移进度评估
- **完全迁移模块**: 文档 CRUD、基础查询
- **部分迁移模块**: 索引管理、Settings 管理
- **未迁移模块**: 聚合 API、集群管理、Template 管理、部分高级功能

---

## ❌ 未迁移 API 清单

### 1. ElasticUtils 主类下的未迁移 API

#### 1.1 聚合相关 (Aggs)
所有聚合 API 仍使用 RestHighLevelClient，未迁移到 ElasticsearchClient：

- `Aggs.totalHits()` - 从 ThreadContext 获取总命中数
- `Aggs.terms(String... indices)` - Terms 聚合
- `Aggs.multiTerms(String... indices)` - Multi Terms 聚合
- `Aggs.range(String... indices)` - Range 聚合
- `Aggs.histogram(String... indices)` - Histogram 聚合
- `Aggs.dateHistogram(String... indices)` - Date Histogram 聚合
- `Aggs.min(String... indices)` - Min 聚合
- `Aggs.max(String... indices)` - Max 聚合
- `Aggs.avg(String... indices)` - Avg 聚合
- `Aggs.sum(String... indices)` - Sum 聚合
- `Aggs.stats(String... indices)` - Stats 聚合
- `Aggs.cardinality(String... indices)` - Cardinality 聚合
- `Aggs.composite(String... indices)` - Composite 聚合

**说明**: 
- 已有 `AggsV8` 作为替代入口（仅包含 terms 和 multiTerms）
- 其他聚合类型需要补充 V8 版本的 Builder

#### 1.2 搜索与查询相关
以下方法内部仍依赖 RestHighLevelClient 执行搜索：

- `docCount(String index)` - 使用 SearchRequestSupport.search(QUERY_CLIENT, ...)，但返回的是 7.x SearchResponse
- `updateByQuery(String... indices)` - 使用底层 RestClient HTTP 请求，非原生 API
- `deleteBy(String index, String field, String value)` - 使用底层 RestClient HTTP 请求

#### 1.3 Suggest 相关
所有 Suggest API 未迁移：

- `suggest(String... indices)` - 创建 Suggest Builder
- `termSuggest(String text, String field, String... indices)` - Term Suggest
- `phraseSuggest(String text, String field, String... indices)` - Phrase Suggest
- `completionSuggest(String prefix, String field, String... indices)` - Completion Suggest
- `contextSuggest(String... indices)` - Context Suggest

#### 1.4 批量操作相关
- `bulkIndex(String index, String... docs)` - 使用 BulkRequest + RestHighLevelClient
- `bulkIndex(String index, List<?> docs)` - 使用 BulkRequest + RestHighLevelClient
- `bulkIndexConcurrent(String index, List<?> docs)` - 使用 BulkProcessor (已废弃的 API)
- `bulkUpdate()` - 返回 ElasticBulkUpdateBuilder，内部使用 RestHighLevelClient

#### 1.5 其他未迁移 API
- `analyze(Analyzer analyzer, String... texts)` - ✅ 已迁移（使用 QUERY_CLIENT.indices().analyze()）
- `functionScoreQuery(ScoreFunctionBuilder scoreFunctionBuilder, String... indices)` - 使用 7.x ScoreFunctionBuilder
- `constantScoreQuery(String... indices)` - 返回 ElasticQueryBuilder，内部可能使用 7.x API

---

### 2. ElasticUtils.Admin 下的未迁移 API

#### 2.1 Index Template 管理
以下方法仍使用 RestHighLevelClient (CLIENT)：

- `getIndexTemplate(String templateName)` - 返回 Map<String, IndexTemplateMetadata> (7.x 类型)
  ```java
  GetIndexTemplatesResponse response = IndicesRestSupport.getIndexTemplates(CLIENT, templateName);
  ```

- `deleteIndexTemplate(String templateName)` - 使用 CLIENT
  ```java
  return IndicesRestSupport.deleteIndexTemplate(CLIENT, templateName);
  ```

- `putIndexTemplateByFile(String templateName)` - 返回 ElasticIndexTemplateBuilder（需检查内部实现）

- `putIndexTemplateByFile(String templateName, String templateFileName)` - 使用 HTTP 请求（非 Client API）

- `putIndexTemplate(String templateName, String templateContent)` - 使用 HTTP 请求（非 Client API）

#### 2.2 Search Template 管理
以下方法仍使用 RestHighLevelClient (CLIENT)：

- `createSearchTemplate(String templateName, String templateFileName)`
  ```java
  PutStoredScriptRequest request = new PutStoredScriptRequest();
  AcknowledgedResponse response = CLIENT.putScript(request, RequestOptions.DEFAULT);
  ```

- `deleteSearchTemplate(String templateName)`
  ```java
  DeleteStoredScriptRequest request = new DeleteStoredScriptRequest(templateName);
  AcknowledgedResponse response = CLIENT.deleteScript(request, RequestOptions.DEFAULT);
  ```

#### 2.3 集群与索引维护
以下方法仍使用 RestHighLevelClient (CLIENT)：

- `forceMerge(String indices)` - 段合并
  ```java
  return IndicesRestSupport.forceMerge(CLIENT, indices);
  ```

- `setReadOnly(String... indices)` - ✅ 已迁移（使用 QUERY_CLIENT）

#### 2.4 Pipeline 管理
- `pipeline(String pipelineName)` - 返回 ElasticPipelineBuilder（需检查内部实现是否使用 CLIENT）

#### 2.5 Reindex
- `reindex(String srcIndex, String destIndex)` - 返回 ElasticReindexBuilder（需检查内部实现）

---

### 3. ElasticUtils.Mappings 下的未迁移 API

#### 3.1 Field Mapping 查询
- `getMapping(String index, String... fields)` - 获取字段级别的 Mapping
  ```java
  return IndicesRestSupport.getFieldMapping(CLIENT, index, fields);
  ```
  **说明**: 该方法仍使用 RestHighLevelClient

#### 3.2 Put Mapping
- `putMapping(String index, String mapping)` - 使用 HTTP 请求（非 Client API）
  ```java
  result = HttpUtils.put(host + "/" + index + "/_mapping")
          .body(mapping)
          .method(HttpMethod.PUT)
          .request();
  ```
  **说明**: 虽然 IndicesRestSupport 已有 `putMapping(ElasticsearchClient, ...)` 方法，但此处的静态方法未调用它

---

### 4. ElasticUtils.Settings 下的未迁移 API

#### 4.1 Put Settings
- `putSettings(String index, String settings)` - 使用 HTTP 请求（非 Client API）
  ```java
  result = HttpUtils.put(host + "/" + index + "/_settings")
          .body(settings)
          .method(HttpMethod.PUT)
          .request();
  ```
  **说明**: 虽然 IndicesRestSupport 已有 `updateIndexSettings(ElasticsearchClient, ...)` 方法，但此处的静态方法未调用它

---

### 5. ElasticUtils.Cluster 下的未迁移 API（全部未迁移）

整个 Cluster 子类都使用 RestHighLevelClient (CLIENT)：

- `health()` - 集群健康状态
  ```java
  ClusterHealthResponse response = IndicesRestSupport.clusterHealth(CLIENT);
  ```

- `settings()` - 返回 ClusterSettingBuilder（需检查内部实现）

- `allSettings()` - 使用 HTTP 请求
  ```java
  String settings = HttpUtils.get(url).basicAuth(username, password).request();
  ```

- `createMultiFieldAgg()` - 创建存储脚本
  ```java
  PutStoredScriptRequest request = new PutStoredScriptRequest();
  AcknowledgedResponse response = CLIENT.putScript(request, RequestOptions.DEFAULT);
  ```

---

### 6. ElasticUtils.Query 下的未迁移 API

大部分 Query Builder 返回的是自定义 Builder，但最终执行时可能仍依赖 7.x API：

- `scrollQuery(String... indices)` - Scroll 查询（需检查内部实现）
- `templateQuery(String... indices)` - Search Template 查询（需检查内部实现）

**注意**: 以下 Query 方法返回的 Builder 在执行 search() 时，内部可能调用的是 RestHighLevelClient：
- `idsQuery(...)`
- `uriQuery(...)`
- `queryString(...)`
- `matchQuery(...)`
- `matchAllQuery(...)`
- `termQuery(...)`
- `termsQuery(...)`
- `matchPhraseQuery(...)`
- `matchPhrasePrefixQuery(...)`
- `geoDistance(...)`
- `exists(...)`
- `prefix(...)`
- `range(...)`
- `query(...)`
- `multiMatch(...)`
- `bool(...)`

需要检查这些 Builder 的 `.execute()` 或 `.search()` 方法内部使用的是哪个客户端。

---

### 7. Support 类中的未迁移方法

#### 7.1 IndicesRestSupport
以下方法仍使用 RestHighLevelClient：

- `createIndex(RestHighLevelClient client, CreateIndexRequest request)` - @Deprecated
- `existsIndex(RestHighLevelClient client, String... indices)`
- `deleteIndex(RestHighLevelClient client, String... indices)`
- `listIndexNames(RestHighLevelClient client)`
- `getIndices(RestHighLevelClient client, String... indices)`
- `updateAliases(RestHighLevelClient client, IndicesAliasesRequest request)`
- `addAlias(RestHighLevelClient client, ...)` - 3 个重载方法
- `removeAlias(RestHighLevelClient client, String index, String alias)`
- `getIndexTemplates(RestHighLevelClient client, String templateName)`
- `deleteIndexTemplate(RestHighLevelClient client, String templateName)`
- `putIndexTemplate(RestHighLevelClient client, PutIndexTemplateRequest request)`
- `updateIndexSettings(RestHighLevelClient client, String[] indices, Settings settings)`
- `forceMerge(RestHighLevelClient client, String index)`
- `getMapping(RestHighLevelClient client, String index)` - 有 ES 8.x 版本的重载
- `getFieldMapping(RestHighLevelClient client, String index, String... fields)`
- `clusterHealth(RestHighLevelClient client)`

**已迁移的方法**（有 ElasticsearchClient 版本）：
- ✅ `createIndex(ElasticsearchClient, String, Map, Map)`
- ✅ `listIndexNames(ElasticsearchClient)`
- ✅ `listIndices(ElasticsearchClient)`
- ✅ `addAlias(ElasticsearchClient, ...)` - 3 个重载方法
- ✅ `removeAlias(ElasticsearchClient, String, String)`
- ✅ `putIndexTemplate(ElasticsearchClient, ...)`
- ✅ `updateIndexSettings(ElasticsearchClient, String[], Map)`
- ✅ `putMapping(ElasticsearchClient, String, Map)`
- ✅ `getMapping(ElasticsearchClient, String)`

#### 7.2 DocumentRestSupport
以下方法仍使用 RestHighLevelClient：

- `index(RestHighLevelClient client, ...)`
- `get(RestHighLevelClient client, ...)`
- `delete(RestHighLevelClient client, ...)`
- `update(RestHighLevelClient client, ...)` - @Deprecated
- `bulk(RestHighLevelClient client, BulkRequest request)`
- `deleteByQuery(RestHighLevelClient client, String index, QueryBuilder query)`

**已迁移的方法**（有 ElasticsearchClient 版本）：
- ✅ `indexWithResult(ElasticsearchClient, ...)` - 2 个重载
- ✅ `get(ElasticsearchClient, String, String, boolean)`
- ✅ `deleteWithResult(ElasticsearchClient, String, String)`
- ✅ `updateWithResult(ElasticsearchClient, ...)`
- ✅ `bulk(ElasticsearchClient, BulkRequest)`

#### 7.3 SearchRequestSupport
以下方法仍使用 RestHighLevelClient：

- `search(RestHighLevelClient client, String index, Consumer<SearchSourceBuilder>)`
- `search(RestHighLevelClient client, String[] indices, SearchSourceBuilder)`
- `search(RestHighLevelClient client, String[] indices, SearchOptions)`

**已迁移的方法**（有 ElasticsearchClient 版本）：
- ✅ `search(ElasticsearchClient client, ...)` - 多个重载

#### 7.4 AggResultSupport
整个类都使用 7.x 聚合响应类型（InternalMax, InternalMin, StringTerms 等），未迁移到 ES 8.x 聚合 API。

---

## ✅ 已迁移 API 清单

### 1. ElasticUtils 主类下的已迁移 API

#### 1.1 文档 CRUD
- ✅ `index(String index, String doc, String id)` - 使用 QUERY_CLIENT
- ✅ `index(String index, Object doc, String id)` - 使用 QUERY_CLIENT
- ✅ `create(String index, String doc, String id)` - 使用 QUERY_CLIENT
- ✅ `create(String index, Object doc, String id)` - 使用 QUERY_CLIENT
- ✅ `get(String index, String id)` - 使用 QUERY_CLIENT
- ✅ `get(String index, String id, Class<T> clazz)` - 使用 QUERY_CLIENT
- ✅ `getWithVersion(String index, String id)` - 使用 QUERY_CLIENT
- ✅ `getWithVersion(String index, String id, Class<T> clazz)` - 使用 QUERY_CLIENT
- ✅ `delete(String index, Integer id)` - 使用 QUERY_CLIENT
- ✅ `delete(String index, String id)` - 使用 QUERY_CLIENT
- ✅ `exists(String index, String id)` - 使用 QUERY_CLIENT

#### 1.2 更新操作
- ✅ `update(String index, String id, String doc)` - 使用 QUERY_CLIENT
- ✅ `update(String index, Integer id, String doc)` - 使用 QUERY_CLIENT
- ✅ `update(String index, String id, Map<String, Object> doc)` - 使用 QUERY_CLIENT
- ✅ `upsert(String index, String id, String doc)` - 使用 QUERY_CLIENT

#### 1.3 分析器
- ✅ `analyze(Analyzer analyzer, String... texts)` - 使用 QUERY_CLIENT.indices().analyze()

#### 1.4 多文档获取
- ✅ `mget()` - 返回 ElasticMultiGetBuilder(QUERY_CLIENT)

---

### 2. ElasticUtils.Admin 下的已迁移 API

#### 2.1 索引管理
- ✅ `createIndex(Class entityClass)` - 使用 QUERY_CLIENT
- ✅ `createIndex(Class entityClass, String index)` - 使用 QUERY_CLIENT
- ✅ `createIndex(String index)` - 返回 ElasticIndexBuilder(QUERY_CLIENT, index)
- ✅ `existsIndex(String... indices)` - 使用 IndicesClientSupport.existsIndex(QUERY_CLIENT, ...)
- ✅ `deleteIndex(String... indices)` - 使用 IndicesClientSupport.deleteIndex(QUERY_CLIENT, ...)
- ✅ `listIndexNames()` - 使用 IndicesRestSupport.listIndexNames(QUERY_CLIENT)
- ✅ `listIndices()` - 使用 IndicesRestSupport.listIndices(QUERY_CLIENT)

#### 2.2 别名管理
- ✅ `createIndexAlias(String index, String alias)` - 使用 IndicesRestSupport.addAlias(QUERY_CLIENT, ...)
- ✅ `createIndexAlias(String[] indices, String alias, QueryBuilder queryBuilder)` - 使用 QUERY_CLIENT
- ✅ `deleteIndexAlias(String index, String alias)` - 使用 IndicesRestSupport.removeAlias(QUERY_CLIENT, ...)

#### 2.3 Index Template
- ✅ `putIndexTemplateByFile(String templateName)` - 返回 ElasticIndexTemplateBuilder(QUERY_CLIENT, ...)

#### 2.4 Settings
- ✅ `setReadOnly(String... indices)` - 使用 IndicesRestSupport.updateIndexSettings(QUERY_CLIENT, ...)

---

### 3. ElasticUtils.Mappings 下的已迁移 API

- ✅ `getMapping(String index)` - 使用 IndicesRestSupport.getMapping(QUERY_CLIENT, index)
- ✅ `putMapping(String index, Dynamic dynamic)` - 返回 ElasticPutMappingBuilder（需检查内部实现）

---

### 4. ElasticUtils.Query 下的已迁移 API

以下方法返回的 Builder 在执行时应该使用 QUERY_CLIENT（需验证）：

- ✅ `byId(String index, Object id)` - 使用 QUERY_CLIENT
- ✅ `byId(String index, Object id, Class<T> resultType)` - 使用 QUERY_CLIENT

---

### 5. Support 类中的已迁移方法

#### 5.1 IndicesRestSupport
- ✅ `createIndex(ElasticsearchClient, String, Map, Map)`
- ✅ `listIndexNames(ElasticsearchClient)`
- ✅ `listIndices(ElasticsearchClient)`
- ✅ `addAlias(ElasticsearchClient, String, String)`
- ✅ `addAlias(ElasticsearchClient, String[], String, QueryBuilder)`
- ✅ `removeAlias(ElasticsearchClient, String, String)`
- ✅ `putIndexTemplate(ElasticsearchClient, String, List<String>, int, Integer, Map, Map)`
- ✅ `updateIndexSettings(ElasticsearchClient, String[], Map)`
- ✅ `putMapping(ElasticsearchClient, String, Map)`
- ✅ `getMapping(ElasticsearchClient, String)`

#### 5.2 DocumentRestSupport
- ✅ `indexWithResult(ElasticsearchClient, String, String, String, boolean)`
- ✅ `indexWithResult(ElasticsearchClient, String, String, String, boolean, String, boolean)`
- ✅ `get(ElasticsearchClient, String, String, boolean)`
- ✅ `deleteWithResult(ElasticsearchClient, String, String)`
- ✅ `updateWithResult(ElasticsearchClient, String, String, String, boolean)`
- ✅ `bulk(ElasticsearchClient, BulkRequest)`

#### 5.3 SearchRequestSupport
- ✅ `search(ElasticsearchClient, String, Consumer<SearchSourceBuilder>)`
- ✅ `search(ElasticsearchClient, String[], SearchSourceBuilder)`
- ✅ `search(ElasticsearchClient, String[], SearchOptions)`
- ✅ `search(ElasticsearchClient, String[], SearchSourceConfigurer)`

#### 5.4 IndicesClientSupport
- ✅ `existsIndex(ElasticsearchClient, String...)`
- ✅ `deleteIndex(ElasticsearchClient, String...)`

---

## 🔍 需要进一步检查的 Builder 类

以下 Builder 类的内部实现需要检查，确认它们执行时使用的是哪个客户端：

### 查询 Builder
- `ElasticQueryBuilder` - 通用查询 Builder
- `ElasticScrollQueryBuilder` - Scroll 查询
- `ElasticTemplateQueryBuilder` - Search Template 查询
- `ElasticIdsQueryBuilder` - IDs 查询
- `ElasticUriQueryBuilder` - URI 查询
- `ElasticQueryStringBuilder` - Query String 查询
- `ElasticMatchQueryBuilder` - Match 查询
- `ElasticMatchAllQueryBuilder` - Match All 查询
- `ElasticTermQueryBuilder` - Term 查询
- `ElasticTermsQueryBuilder` - Terms 查询
- `ElasticMatchPhraseQueryBuilder` - Match Phrase 查询
- `ElasticMatchPhrasePrefixQueryBuilder` - Match Phrase Prefix 查询
- `ElasticGeoDistanceQueryBuilder` - Geo Distance 查询
- `ElasticExistsQueryBuilder` - Exists 查询
- `ElasticPrefixQueryBuilder` - Prefix 查询
- `ElasticRangeQueryBuilder` - Range 查询
- `ElasticMultiMatchQueryBuilder` - Multi Match 查询
- `ElasticBoolQueryBuilder` - Bool 查询

### 聚合 Builder
- `ElasticTermsAggregationBuilder`
- `ElasticMultiTermsAggregationBuilder`
- `ElasticRangeAggregationBuilder`
- `ElasticHistogramAggregationBuilder`
- `ElasticDateHistogramAggregationBuilder`
- `ElasticMinAggregationBuilder`
- `ElasticMaxAggregationBuilder`
- `ElasticAvgAggregationBuilder`
- `ElasticSumAggregationBuilder`
- `ElasticStatsAggregationBuilder`
- `ElasticCardinalityAggregationBuilder`
- `ElasticCompositeAggregationBuilder`

### 管理 Builder
- `ElasticIndexTemplateBuilder`
- `ElasticPipelineBuilder`
- `ElasticReindexBuilder`
- `ElasticPutMappingBuilder`
- `ElasticUpdateSettingBuilder`
- `ClusterSettingBuilder`

### 批量 Builder
- `ElasticBulkIndexBuilder`
- `ElasticBulkUpdateBuilder`
- `ESBulkProcessor`

### 其他 Builder
- `ElasticSuggestBuilder`
- `ElasticContextSuggestBuilder`
- `ElasticMultiGetBuilder`
- `ElasticUpdateBuilder`
- `ElasticIndexDocBuilder`

---

## 📝 迁移建议与优先级

### 高优先级（核心功能）
1. **聚合 API 迁移** - 这是最大的未迁移模块，影响面最广
   - 为所有聚合类型创建 V8 版本的 Builder
   - 迁移 AggResultSupport 以支持 ES 8.x 聚合响应类型
   - 参考已有的 `AggsV8.terms()` 和 `V8TermsAggregationBuilder` 实现模式

2. **集群管理 API 迁移**
   - `Cluster.health()` - 集群健康检查
   - `Cluster.settings()` - 集群设置
   - `Cluster.createMultiFieldAgg()` - 存储脚本管理

3. **Template 管理 API 迁移**
   - `Admin.getIndexTemplate()` - 使用 ES 8.x API
   - `Admin.deleteIndexTemplate()` - 使用 ES 8.x API
   - `Admin.createSearchTemplate()` - 使用 ES 8.x Script API
   - `Admin.deleteSearchTemplate()` - 使用 ES 8.x Script API

### 中优先级（常用功能）
4. **Suggest API 迁移**
   - Term Suggest
   - Phrase Suggest
   - Completion Suggest
   - Context Suggest

5. **批量操作迁移**
   - `bulkIndex()` 方法改为使用 ElasticsearchClient
   - `bulkUpdate()` 方法改为使用 ElasticsearchClient
   - 替换已废弃的 BulkProcessor

6. **高级查询功能**
   - Scroll 查询
   - Search Template 查询
   - Function Score 查询

### 低优先级（辅助功能）
7. **HTTP 请求方式改为 Client API**
   - `Admin.putIndexTemplate(String, String)` - 改用 Client API
   - `Mappings.putMapping(String, String)` - 改用 Client API
   - `Settings.putSettings(String, String)` - 改用 Client API
   - `Cluster.allSettings()` - 改用 Client API

8. **Field Mapping 查询**
   - `Mappings.getMapping(String, String...)` - 迁移到 ES 8.x API

9. **索引维护**
   - `Admin.forceMerge()` - 段合并功能

---

## 🎯 迁移技术要点

### 1. 聚合响应类型转换
ES 8.x 的聚合响应类型与 7.x 完全不同，需要：
- 使用 `co.elastic.clients.elasticsearch._types.aggregations.*` 包中的类型
- 创建桥接层或使用 Visitor 模式解析聚合结果
- 参考 `SearchResponseBridge` 和 `AggregationBridge` 的实现

### 2. Script API 迁移
ES 8.x 的 Script API 变化较大：
- `PutStoredScriptRequest` → 使用 `client.putScript()`
- `DeleteStoredScriptRequest` → 使用 `client.deleteScript()`
- 请求参数结构有变化

### 3. Template API 迁移
Index Template API 在 ES 8.x 中有重大变化：
- 旧的 `_template` API → 新的 `_index_template` API
- 需要使用 Composable Index Templates
- `GetIndexTemplatesResponse` → 使用新的响应类型

### 4. 保持向后兼容
- 保留现有的 RestHighLevelClient 重载方法，标记为 @Deprecated
- 提供迁移指南文档
- 逐步替换调用方代码

---

## 📌 注意事项

1. **依赖保留**: 即使部分 API 已迁移，仍需保留 elasticsearch-rest-high-level-client 依赖，因为：
   - 聚合 API 仍在使用
   - 某些 Builder 内部可能仍依赖 7.x 类型
   - 响应解析可能需要 7.x 的 XContent 工具类

2. **测试覆盖**: 每个迁移的 API 都需要充分的单元测试和集成测试

3. **性能对比**: 迁移后需要对比性能，确保没有退化

4. **文档更新**: 更新 JavaDoc 和使用示例

---

## 🔗 相关资源

- [Elasticsearch Java Client 8.x 官方文档](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [迁移指南](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/migration.html)
- 项目内的迁移参考：
  - `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/support/SearchResponseBridge.java`
  - `copilot-search-8.x/src/main/java/com/awesomecopilot/search8x/builder/agg/v8/V8TermsAggregationBuilder.java`
  - `migrate.md`

---

**报告结束**
