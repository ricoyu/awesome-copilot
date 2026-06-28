# copilot-search-8.x Elasticsearch 8.19.14 迁移统计报告

**生成时间**: 2026-06-28  
**Elasticsearch 版本**: 8.19.14  
**目标客户端**: `ElasticsearchClient` (co.elastic.clients.elasticsearch)

---

## 📊 总体概况

| 类别 | 已迁移方法数 | 未迁移方法数 | 迁移进度 |
|------|------------|------------|---------|
| ElasticUtils 顶层方法 | ~50+ | 3 | 94% |
| ElasticUtils.Admin | ~15 | 5 | 75% |
| ElasticUtils.Mappings | 3 | 0 | 100% ✅ |
| ElasticUtils.Settings | 2 | 0 | 100% ✅ |
| ElasticUtils.Cluster | 3 | 0 | 100% ✅ |
| ElasticUtils.Query | ~15 | 0 | 100% ✅ |
| ElasticUtils.Aggsv8 | ~10 | 0 | 100% ✅ |
| IndicesRestSupport | ~15 | 10 | 60% |
| DocumentRestSupport | ~10 | 6 | 62% |
| **总计** | **~123** | **~24** | **84%** |

---

## 🔴 未迁移 API 详细清单

### 一、ElasticUtils 顶层方法（3个）

#### 1. bulkIndex (批量索引文档 - 使用 RestHighLevelClient 的版本)
- **方法签名**: `public static BulkResult bulkIndex(String index, String... docs)`
- **位置**: ElasticUtils.java:388
- **当前实现**: 使用 `CLIENT.prepareBulk()` (已被注释)
- **影响范围**: 批量创建文档功能
- **建议方案**: 使用 ES 8.x 的 Bulk API，通过底层 RestClient 执行

#### 2. bulkIndex (批量索引文档 - List 版本)
- **方法签名**: `public static BulkResult bulkIndex(String index, List<?> docs)`
- **位置**: ElasticUtils.java:499
- **当前实现**: 使用 `CLIENT.prepareBulk()` (已被注释)
- **影响范围**: 批量创建文档功能
- **建议方案**: 同上

#### 3. mget (多文档获取)
- **方法签名**: `public static ElasticMultiGetBuilder mget()`
- **位置**: ElasticUtils.java:655
- **当前实现**: 返回 `new ElasticMultiGetBuilder(CLIENT)` (CLIENT 已被注释)
- **影响范围**: 多文档查询功能
- **建议方案**: 使用 ES 8.x 的 Multi Get API

---

### 二、ElasticUtils.Admin 下的未迁移方法（5个）

#### 1. existsIndex
- **方法签名**: `public static boolean existsIndex(String... indices)`
- **位置**: ElasticUtils.java:1182
- **当前实现**: 调用 `IndicesRestSupport.existsIndex(CLIENT, indices)` 
- **问题**: CLIENT 已被注释，需要改用 QUERY_CLIENT
- **建议方案**: 在 IndicesRestSupport 中添加使用 ElasticsearchClient 的版本

#### 2. deleteIndex
- **方法签名**: `public static boolean deleteIndex(String... indices)`
- **位置**: ElasticUtils.java:1192
- **当前实现**: 调用 `IndicesRestSupport.deleteIndex(CLIENT, indices)`
- **问题**: CLIENT 已被注释
- **建议方案**: 在 IndicesRestSupport 中添加使用 ElasticsearchClient 的版本

#### 3. listIndexNames
- **方法签名**: `public static List<String> listIndexNames()`
- **位置**: ElasticUtils.java:1205
- **当前实现**: 调用 `IndicesRestSupport.listIndexNames(CLIENT)`
- **问题**: CLIENT 已被注释
- **备注**: IndicesRestSupport 已有 ES 8.x 版本，需要修改调用

#### 4. listIndices
- **方法签名**: `public static List<Index> listIndices()`
- **位置**: ElasticUtils.java:1213
- **当前实现**: 调用 `IndicesRestSupport.listIndices(CLIENT)`
- **问题**: CLIENT 已被注释
- **备注**: IndicesRestSupport 已有 ES 8.x 版本，需要修改调用

#### 5. forceMerge
- **方法签名**: `public static boolean forceMerge(String indices)`
- **位置**: ElasticUtils.java:1418
- **当前实现**: 调用 `IndicesRestSupport.forceMerge(CLIENT, indices)`
- **问题**: CLIENT 已被注释
- **备注**: IndicesRestSupport 已有 ES 8.x 版本，需要修改调用

---

### 三、ElasticUtils.Aggs 下的未迁移方法

✅ **已全部迁移到 Aggsv8** - 无未迁移方法

---

### 四、ElasticUtils.Query 下的未迁移方法

✅ **已全部迁移** - 所有 Query 构建器方法已完成迁移

---

### 五、IndicesRestSupport 中的未迁移方法（10个）

以下方法仍使用 `RestHighLevelClient`，需要迁移到 `ElasticsearchClient`：

#### 1. createIndex (Request 版本)
- **方法签名**: `public static boolean createIndex(RestHighLevelClient client, CreateIndexRequest request)`
- **位置**: IndicesRestSupport.java:64
- **状态**: 已标记 @Deprecated
- **已有替代**: ✅ 有 ES 8.x 版本 (line 113)

#### 2. existsIndex
- **方法签名**: `public static boolean existsIndex(RestHighLevelClient client, String... indices)`
- **位置**: IndicesRestSupport.java:154
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高

#### 3. deleteIndex
- **方法签名**: `public static boolean deleteIndex(RestHighLevelClient client, String... indices)`
- **位置**: IndicesRestSupport.java:163
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高

#### 4. getIndices
- **方法签名**: `public static GetIndexResponse getIndices(RestHighLevelClient client, String... indices)`
- **位置**: IndicesRestSupport.java:256
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🟡 中

#### 5. updateAliases
- **方法签名**: `public static boolean updateAliases(RestHighLevelClient client, IndicesAliasesRequest request)`
- **位置**: IndicesRestSupport.java:265
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🟡 中

#### 6. addAlias (单索引版本)
- **方法签名**: `public static boolean addAlias(RestHighLevelClient client, String index, String alias)`
- **位置**: IndicesRestSupport.java:274
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 300)

#### 7. addAlias (多索引版本)
- **方法签名**: `public static boolean addAlias(RestHighLevelClient client, String[] indices, String alias, QueryBuilder filter)`
- **位置**: IndicesRestSupport.java:280
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 349)

#### 8. removeAlias
- **方法签名**: `public static boolean removeAlias(RestHighLevelClient client, String index, String alias)`
- **位置**: IndicesRestSupport.java:286
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 407)

#### 9. getIndexTemplates
- **方法签名**: `public static GetIndexTemplatesResponse getIndexTemplates(RestHighLevelClient client, String templateName)`
- **位置**: IndicesRestSupport.java:452
- **状态**: 已标记 @Deprecated
- **已有替代**: ✅ 有 ES 8.x 版本 (line 468)

#### 10. deleteIndexTemplate
- **方法签名**: `public static boolean deleteIndexTemplate(RestHighLevelClient client, String templateName)`
- **位置**: IndicesRestSupport.java:496
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 513)

#### 11. putIndexTemplate (Request 版本)
- **方法签名**: `public static boolean putIndexTemplate(RestHighLevelClient client, PutIndexTemplateRequest request)`
- **位置**: IndicesRestSupport.java:541
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 562)

#### 12. updateIndexSettings (Settings 版本)
- **方法签名**: `public static boolean updateIndexSettings(RestHighLevelClient client, String[] indices, Settings settings)`
- **位置**: IndicesRestSupport.java:612
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 631)

#### 13. forceMerge (Response 版本)
- **方法签名**: `public static ForceMergeResponse forceMerge(RestHighLevelClient client, String index)`
- **位置**: IndicesRestSupport.java:661
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 678)

#### 14. getMapping (旧版本)
- **方法签名**: `public static Map<String, Object> getMapping(RestHighLevelClient client, String index)`
- **位置**: IndicesRestSupport.java:708
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 766)

#### 15. getFieldMapping
- **方法签名**: `public static Map<String, Map<String, Object>> getFieldMapping(RestHighLevelClient client, String index, String... fields)`
- **位置**: IndicesRestSupport.java:808
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 846)

#### 16. clusterHealth (Response 版本)
- **方法签名**: `public static ClusterHealthResponse clusterHealth(RestHighLevelClient client)`
- **位置**: IndicesRestSupport.java:905
- **状态**: ❌ 无 ES 8.x 替代方法
- **已有替代**: ✅ 有 ES 8.x 版本 (line 919)

---

### 六、DocumentRestSupport 中的未迁移方法（6个）

以下方法仍使用 `RestHighLevelClient`，需要迁移到 `ElasticsearchClient`：

#### 1. index
- **方法签名**: `public static IndexResponse index(RestHighLevelClient client, String index, String id, String json, boolean create)`
- **位置**: DocumentRestSupport.java:45
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高
- **已有替代**: ✅ 有 `indexWithResult(ElasticsearchClient, ...)` 版本

#### 2. get
- **方法签名**: `public static GetResponse get(RestHighLevelClient client, String index, String id, boolean fetchSource)`
- **位置**: DocumentRestSupport.java:184
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高
- **已有替代**: ✅ 有 `getWithResult(ElasticsearchClient, ...)` 版本

#### 3. delete
- **方法签名**: `public static DeleteResponse delete(RestHighLevelClient client, String index, String id)`
- **位置**: DocumentRestSupport.java:289
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高
- **已有替代**: ✅ 有 `deleteWithResult(ElasticsearchClient, ...)` 版本

#### 4. update
- **方法签名**: `public static UpdateResponse update(RestHighLevelClient client, String index, String id, String json, boolean upsert)`
- **位置**: DocumentRestSupport.java:416
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🟡 中
- **已有替代**: ✅ 有 `updateWithResult(ElasticsearchClient, ...)` 版本

#### 5. bulk
- **方法签名**: `public static BulkResponse bulk(RestHighLevelClient client, BulkRequest request)`
- **位置**: DocumentRestSupport.java:459
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🔴 高
- **建议方案**: 添加使用 ElasticsearchClient 的 bulk 方法

#### 6. deleteByQuery
- **方法签名**: `public static long deleteByQuery(RestHighLevelClient client, String index, QueryBuilder query)`
- **位置**: DocumentRestSupport.java:564
- **状态**: ❌ 无 ES 8.x 替代方法
- **优先级**: 🟡 中
- **建议方案**: 添加使用 ElasticsearchClient 的 deleteByQuery 方法

---

### 七、AggResultSupport 中的未迁移方法

⚠️ **注意**: `AggResultSupport` 中的所有方法都使用 7.x 的 `Aggregations` 类型，这是为了兼容旧的查询 API。

**现状**:
- `AggResultSupport`: 处理 7.x HLRC 的聚合结果 (Aggregations)
- `V8AggResultSupport`: 处理 8.x Java Client 的聚合结果 (Map<String, Aggregate>)

**建议**: 
- 保留 `AggResultSupport` 作为过渡期的兼容层
- 新代码应使用 `V8AggResultSupport`
- 待所有查询 API 迁移完成后，可考虑移除 `AggResultSupport`

---

## 🎯 迁移优先级建议

### 🔴 高优先级（核心功能）

1. **ElasticUtils.Admin 的方法调用修复**
   - existsIndex, deleteIndex, listIndexNames, listIndices, forceMerge
   - 原因: 这些方法直接引用了已注释的 CLIENT，导致编译错误
   - 工作量: 小（只需修改调用，IndicesRestSupport 已有 ES 8.x 版本）

2. **DocumentRestSupport 的核心 CRUD 方法**
   - index, get, delete, bulk
   - 原因: 文档操作是最常用的功能
   - 工作量: 中（已有 withResult 版本，需要补充或直接使用）

3. **IndicesRestSupport 的索引管理方法**
   - existsIndex, deleteIndex
   - 原因: 索引管理是基础功能
   - 工作量: 小（可通过 REST API 快速实现）

### 🟡 中优先级（常用功能）

4. **ElasticUtils 的批量操作方法**
   - bulkIndex (两个版本), mget
   - 原因: 批量操作在大数据场景常用
   - 工作量: 中

5. **DocumentRestSupport 的高级方法**
   - update, deleteByQuery
   - 原因: 使用频率相对较低
   - 工作量: 中

6. **IndicesRestSupport 的别名和模板方法**
   - updateAliases, getIndices 等
   - 原因: 已有 ES 8.x 替代版本
   - 工作量: 小（主要是标记废弃）

### 🟢 低优先级（辅助功能）

7. **响应对象包装方法**
   - forceMerge (Response 版本), getMapping (旧版本) 等
   - 原因: 已有 ES 8.x 替代版本
   - 工作量: 小

8. **AggResultSupport 兼容性层**
   - 保留作为过渡，待查询 API 完全迁移后再处理
   - 工作量: 大（需等待前置条件）

---

## 📝 迁移模式总结

项目中已形成成熟的迁移模式：

### 模式 1: 双版本并存（推荐）
```java
// 旧版本 - 标记为 @Deprecated
@Deprecated
public static boolean deleteIndex(RestHighLevelClient client, String... indices) {
    // 旧实现
}

// 新版本 - 使用 ElasticsearchClient
public static boolean deleteIndex(ElasticsearchClient client, String... indices) {
    // 通过底层 RestClient 执行 HTTP 请求
    RestClientTransport transport = (RestClientTransport) client._transport();
    RestClient restClient = transport.restClient();
    // ...
}
```

### 模式 2: 直接替换（适用于简单场景）
```java
// 修改调用点
// 旧: IndicesRestSupport.existsIndex(CLIENT, indices)
// 新: IndicesRestSupport.existsIndex(QUERY_CLIENT, indices)
```

### 模式 3: REST API 桥接（适用于复杂 API）
```java
// 对于 ES 8.x Java Client 不支持的 API，通过底层 RestClient 执行 HTTP 请求
RestClientTransport transport = (RestClientTransport) QUERY_CLIENT._transport();
RestClient restClient = transport.restClient();
Request request = new Request("PUT", "/_cluster/settings");
request.setJsonEntity(jsonBody);
Response response = restClient.performRequest(request);
```

---

## ⚠️ 注意事项

1. **CLIENT 变量已被注释**: 
   - `ElasticUtils.CLIENT` 已在 line 153 被注释
   - 所有引用 CLIENT 的代码都需要修改

2. **响应对象类型变化**:
   - ES 7.x: 使用 `IndexResponse`, `GetResponse` 等
   - ES 8.x: 建议使用自定义的 `DocumentOperationResult` 或通过 JSON 解析

3. **聚合结果类型变化**:
   - ES 7.x: `Aggregations` → `Aggregation`
   - ES 8.x: `Map<String, Aggregate>`
   - 需要分别使用 `AggResultSupport` 和 `V8AggResultSupport`

4. **QueryBuilder 兼容性**:
   - 当前查询仍使用 7.x 的 `QueryBuilder`
   - 通过 REST API 桥接转换为 JSON
   - 未来可能需要迁移到 ES 8.x 的原生 Query DSL

5. **Settings 对象转换**:
   - ES 7.x: `org.elasticsearch.common.settings.Settings`
   - ES 8.x: 需要转换为 `Map<String, Object>`
   - 已通过反射 `getAsMap()` 方法实现转换

---

## 📈 下一步行动计划

### Phase 1: 修复编译错误（1-2天）
- [ ] 修复 ElasticUtils.Admin 中的 5 个方法调用
- [ ] 修复 ElasticUtils 顶层的 3 个方法
- [ ] 确保项目能够成功编译

### Phase 2: 核心功能迁移（3-5天）
- [ ] 迁移 DocumentRestSupport 的 CRUD 方法
- [ ] 迁移 IndicesRestSupport 的 existsIndex, deleteIndex
- [ ] 添加单元测试验证功能正确性

### Phase 3: 高级功能迁移（5-7天）
- [ ] 迁移批量操作方法（bulkIndex, mget）
- [ ] 迁移别名和模板管理方法
- [ ] 迁移 deleteByQuery 等方法

### Phase 4: 清理和优化（2-3天）
- [ ] 标记所有旧方法为 @Deprecated
- [ ] 更新 JavaDoc 指引使用新方法
- [ ] 清理不再使用的导入和依赖
- [ ] 编写迁移指南文档

---

## 📚 相关文档

- [ADVANCED_AGGREGATIONS-COMPLETE.md](./ADVANCED_AGGREGATIONS-COMPLETE.md) - 高级聚合迁移完成报告
- [ALIAS_USAGE_EXAMPLES.md](./ALIAS_USAGE_EXAMPLES.md) - 别名使用示例
- [MIGRATION_GUIDE_V8_AGGREGATION.md](./copilot-search-8.x/MIGRATION_GUIDE_V8_AGGREGATION.md) - V8 聚合迁移指南
- [RANGE_AGGREGATION_MIGRATION.md](./copilot-search-8.x/RANGE_AGGREGATION_MIGRATION.md) - Range 聚合迁移指南

---

**报告生成工具**: AI Assistant  
**数据来源**: 代码静态分析 + Maven 编译验证  
**最后更新**: 2026-06-28
