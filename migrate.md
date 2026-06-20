# Elasticsearch 8.x 迁移状态报告

**生成时间**: 2026-06-20  
**目标版本**: Elasticsearch 8.19.14  
**模块**: copilot-search-8.x

---

## 📊 迁移概览

| 类别 | 已迁移 | 未迁移 | 总计 | 完成率 |
|------|--------|--------|------|--------|
| 文档 CRUD | ✅ 全部 | 0 | 7 | 100% |
| 索引管理 | ⚠️ 部分 | 7 | 15 | 53% ↑ |
| 映射管理 | ⚠️ 部分 | 1 | 3 | 67% |
| 模板管理 | ❌ 无 | 4 | 4 | 0% |
| 集群管理 | ❌ 无 | 2 | 2 | 0% |
| 搜索查询 | ✅ 全部 | 0 | - | 100% |
| 聚合查询 | ⚠️ 部分 | 1* | - | 60%** ↑ |

> **说明**: 
> - ✅ 已完全迁移到 `ElasticsearchClient` (QUERY_CLIENT)
> - ⚠️ 部分迁移，仍有方法使用 `RestHighLevelClient` (CLIENT)
> - ❌ 尚未开始迁移，仍使用 `RestHighLevelClient` (CLIENT)
> - * 多字段聚合脚本创建需要迁移
> - ** AggsV8 已提供原生 8.x API（terms + multiTerms），但旧 Aggs 仍在使用 CLIENT

---

## 🔴 未迁移接口清单（需优先处理）

### 1. 索引别名管理 (Index Aliases) ✅ 已完成

| 方法签名 | 当前实现 | 所在类 | 优先级 | 状态 | 备注 |
|---------|---------|--------|--------|------|------|
| `Admin.createIndexAlias(String index, String alias)` | ~~使用 `IndicesRestSupport.addAlias(CLIENT, ...)`~~ → **已改为 `QUERY_CLIENT`** | ElasticUtils.Admin | P0 | ✅ 已迁移 | 高频使用 |
| `Admin.createIndexAlias(String[] indices, String alias, QueryBuilder filter)` | ~~使用 `IndicesRestSupport.addAlias(CLIENT, ...)`~~ → **已改为 `QUERY_CLIENT`** | ElasticUtils.Admin | P0 | ✅ 已迁移 | 支持过滤条件 |
| `Admin.deleteIndexAlias(String index, String alias)` | ~~使用 `IndicesRestSupport.removeAlias(CLIENT, ...)`~~ → **已改为 `QUERY_CLIENT`** | ElasticUtils.Admin | P0 | ✅ 已迁移 | 高频使用 |

**底层支持方法**:
- ✅ `IndicesRestSupport.addAlias(ElasticsearchClient client, String index, String alias)` - **新增 ES 8.x 版本**
- ✅ `IndicesRestSupport.addAlias(ElasticsearchClient client, String[] indices, String alias, QueryBuilder filter)` - **新增 ES 8.x 版本**
- ✅ `IndicesRestSupport.removeAlias(ElasticsearchClient client, String index, String alias)` - **新增 ES 8.x 版本**
- ⚠️ `IndicesRestSupport.updateAliases(RestHighLevelClient client, IndicesAliasesRequest request)` - 仍保留旧版（兼容需要）

**迁移技术方案**: 使用底层 RestClient 执行 HTTP POST /_aliases 请求，构建标准的 aliases API JSON 格式

---

### 2. 索引模板管理 (Index Templates)

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Admin.putIndexTemplateByFile(String templateName)` | 使用 `ElasticIndexTemplateBuilder.newInstance(CLIENT, ...)` | ElasticUtils.Admin | P1 | Builder 模式 |
| `Admin.getIndexTemplate(String templateName)` | 使用 `IndicesRestSupport.getIndexTemplates(CLIENT, ...)` | ElasticUtils.Admin | P1 | 返回旧版元数据 |
| `Admin.deleteIndexTemplate(String templateName)` | 使用 `IndicesRestSupport.deleteIndexTemplate(CLIENT, ...)` | ElasticUtils.Admin | P1 | - |

**底层支持方法**:
- `IndicesRestSupport.getIndexTemplates(RestHighLevelClient client, String templateName)` → 返回 `GetIndexTemplatesResponse`
- `IndicesRestSupport.deleteIndexTemplate(RestHighLevelClient client, String templateName)` → 返回 `boolean`
- `IndicesRestSupport.putIndexTemplate(RestHighLevelClient client, PutIndexTemplateRequest request)` → 返回 `boolean`

**注意**: `putIndexTemplate(String templateName, String templateContent)` 已通过 HTTP REST API 实现，不依赖 CLIENT

**迁移建议**: 
- ES 8.x 推荐使用 Composable Index Templates (`_index_template`)
- 需要适配新的 API: `indices().putIndexTemplate()`, `indices().getIndexTemplate()`, `indices().deleteIndexTemplate()`

---

### 3. Search Template 管理

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Admin.createSearchTemplate(String templateName, String templateFileName)` | 使用 `CLIENT.putScript(request, RequestOptions.DEFAULT)` | ElasticUtils.Admin | P2 | 存储脚本 |
| `Admin.deleteSearchTemplate(String templateName)` | 使用 `CLIENT.deleteScript(request, RequestOptions.DEFAULT)` | ElasticUtils.Admin | P2 | 删除脚本 |

**迁移建议**: 使用 ES 8.x Java Client 的 `scripts().putStoredScript()` 和 `scripts().deleteStoredScript()` API

---

### 4. 索引段合并 (Force Merge)

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Admin.forceMerge(String indices)` | 使用 `IndicesRestSupport.forceMerge(CLIENT, indices)` | ElasticUtils.Admin | P2 | 返回 `ForceMergeResponse` |

**底层支持方法**:
- `IndicesRestSupport.forceMerge(RestHighLevelClient client, String index)` → 返回 `ForceMergeResponse`

**迁移建议**: 使用 ES 8.x Java Client 的 `indices().forcemerge()` API

---

### 5. 字段映射查询 (Field Mapping)

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Mappings.getMapping(String index, String... fields)` | 使用 `IndicesRestSupport.getFieldMapping(CLIENT, index, fields)` | ElasticUtils.Mappings | P1 | 获取特定字段映射 |

**底层支持方法**:
- `IndicesRestSupport.getFieldMapping(RestHighLevelClient client, String index, String... fields)` → 返回 `Map<String, Map<String, Object>>`

**注意**: `Mappings.getMapping(String index)` 已迁移到使用 `QUERY_CLIENT`

**迁移建议**: 使用 ES 8.x Java Client 的 `indices().getFieldMapping()` API

---

### 6. 集群健康检查 (Cluster Health)

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Cluster.health()` | 使用 `IndicesRestSupport.clusterHealth(CLIENT)` | ElasticUtils.Cluster | P0 | 高频使用 |

**底层支持方法**:
- `IndicesRestSupport.clusterHealth(RestHighLevelClient client)` → 返回 `ClusterHealthResponse`

**迁移建议**: 使用 ES 8.x Java Client 的 `cluster().health()` API

---

### 7. 多字段聚合脚本创建 (Multi-Field Aggregation Script)

| 方法签名 | 当前实现 | 所在类 | 优先级 | 备注 |
|---------|---------|--------|--------|------|
| `Cluster.createMultiFieldAgg()` | 使用 `CLIENT.putScript(request, RequestOptions.DEFAULT)` | ElasticUtils.Cluster | P3 | 创建 stored script |

**迁移建议**: 同 Search Template，使用 `scripts().putStoredScript()` API

---

### 8. 聚合查询 (Aggregations) - 部分未迁移

**现状**:
- ✅ `AggsV8` 类已提供基于 `co.elastic.clients` 的原生 8.x API
- ❌ 旧的 `Aggs` 类仍在使用 `RestHighLevelClient` (通过 `AbstractAggregationBuilder`)

**影响范围**:
- `ElasticTermsAggregationBuilder`
- `ElasticMinAggregationBuilder`
- `ElasticMaxAggregationBuilder`
- `ElasticAvgAggregationBuilder`
- `ElasticSumAggregationBuilder`
- `ElasticStatsAggregationBuilder`
- `ElasticCardinalityAggregationBuilder`
- `ElasticRangeAggregationBuilder`
- `ElasticHistogramAggregationBuilder`
- `ElasticDateHistogramAggregationBuilder`
- `ElasticMultiTermsAggregationBuilder`
- `ElasticCompositeAggregationBuilder`

**迁移建议**: 
- 推荐使用 `AggsV8` 新 API
- 逐步废弃旧的 `Aggs` API
- 或为旧 Builder 添加 8.x Client 支持

---

## 🟡 已迁移但需注意的接口

### 1. 索引 Settings 更新

| 方法签名 | 状态 | 备注 |
|---------|------|------|
| `Admin.setReadOnly(String... indices)` | ✅ 已迁移 | 使用 `IndicesRestSupport.updateIndexSettings(QUERY_CLIENT, ...)` |
| `Settings.update(String... indices)` | ⚠️ Builder 模式 | `ElasticUpdateSettingBuilder` 内部可能仍使用 CLIENT |

---

## 🟢 已完全迁移的接口

### 1. 文档 CRUD 操作
- ✅ `index()` - 创建/索引文档
- ✅ `create()` - 创建文档（存在则报错）
- ✅ `get()` - 获取文档
- ✅ `delete()` - 删除文档
- ✅ `update()` - 更新文档
- ✅ `upsert()` - 更新或创建
- ✅ `bulkIndex()` - 批量索引
- ✅ `bulkUpdate()` - 批量更新
- ✅ `deleteBy()` - 按条件删除

### 2. 索引基础管理
- ✅ `Admin.createIndex()` - 创建索引
- ✅ `Admin.existsIndex()` - 检查索引是否存在
- ✅ `Admin.deleteIndex()` - 删除索引
- ✅ `Admin.listIndexNames()` - 列出索引名称
- ✅ `Admin.listIndices()` - 列出索引详情

### 3. 映射管理
- ✅ `Mappings.getMapping(String index)` - 获取完整映射
- ✅ `Mappings.putMapping(String index, String mapping)` - 通过 HTTP REST API 设置映射

### 4. 搜索查询
- ✅ 所有 Query Builder 已迁移到使用 `QUERY_CLIENT`
- ✅ `Query.byId()`, `Query.idsQuery()`, `Query.matchQuery()` 等全部已迁移

---

## 📋 迁移优先级建议

### P0 - 高优先级（核心功能，频繁使用）
1. ✅ 文档 CRUD（已完成）
2. ✅ 索引别名管理（3个方法）- **本次已完成**
3. 🔴 集群健康检查（1个方法）

### P1 - 中优先级（常用功能）
4. 🔴 字段映射查询（1个方法）
5. 🔴 索引模板管理（3个方法）

### P2 - 低优先级（较少使用）
6. 🔴 Search Template 管理（2个方法）
7. 🔴 索引段合并（1个方法）

### P3 - 最低优先级（特殊场景）
8. 🔴 多字段聚合脚本创建（1个方法）
9. ⚠️ 聚合查询重构（考虑直接使用 AggsV8）

---

## 🛠️ 迁移技术方案

### 通用迁移模式

```java
// 旧代码 (RestHighLevelClient)
public static boolean addAlias(RestHighLevelClient client, String index, String alias) {
    IndicesAliasesRequest request = new IndicesAliasesRequest();
    request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(index).alias(alias));
    AcknowledgedResponse response = client.indices().updateAliases(request, RequestOptions.DEFAULT);
    return response.isAcknowledged();
}

// 新代码 (ElasticsearchClient)
public static boolean addAlias(ElasticsearchClient client, String index, String alias) {
    try {
        UpdateAliasesRequest request = UpdateAliasesRequest.of(r -> r
            .actions(a -> a
                .add(add -> add
                    .index(index)
                    .alias(alias)
                )
            )
        );
        UpdateAliasesResponse response = client.indices().updateAliases(request);
        return response.acknowledged();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
```

### 特殊情况处理

对于复杂 API（如 Index Templates），可以考虑：
1. **直接 HTTP REST API 调用**（如 `putIndexTemplate` 已采用）
2. **使用底层 RestClient**（通过 `client._transport().restClient()`）
3. **桥接转换层**（保留旧响应类型，内部使用新 API）

---

## 📝 注意事项

1. **向后兼容性**: 迁移过程中应保持 API 签名不变，避免影响调用方
2. **测试覆盖**: 每个迁移的方法都需要编写单元测试验证功能
3. **异常处理**: ES 8.x 使用 `ErrorCause` 对象，需要统一异常处理策略
4. **依赖清理**: 完成迁移后，可以移除 `elasticsearch-rest-high-level-client` 依赖
5. **文档更新**: 同步更新 JavaDoc 和使用示例

---

## 🎯 下一步行动

1. **立即开始**: P0 优先级的索引别名管理和集群健康检查
2. **本周完成**: P1 优先级的字段映射和索引模板管理
3. **本月完成**: P2/P3 优先级的剩余方法
4. **长期规划**: 评估是否完全替换旧的 Aggs API 为 AggsV8

---

**报告维护**: 请在完成每个方法的迁移后更新此文档，标记状态为 ✅ 已迁移。
