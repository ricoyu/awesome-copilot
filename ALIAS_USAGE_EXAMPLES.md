# 索引别名管理 - 使用示例

## 基本用法

### 1. 创建索引别名

```java
// 首先创建索引
ElasticUtils.Admin.createIndex("my_index").create();

// 为索引创建别名
boolean success = ElasticUtils.Admin.createIndexAlias("my_index", "my_alias");
if (success) {
    System.out.println("别名创建成功！");
}
```

### 2. 通过别名操作文档

```java
// 通过原始索引写入文档
String docId = ElasticUtils.index("my_index", "{\"name\": \"John\", \"age\": 30}");

// 通过别名读取文档（完全透明）
String doc = ElasticUtils.get("my_alias", docId);
System.out.println(doc); // {"name": "John", "age": 30}

// 也可以通过别名写入
String docId2 = ElasticUtils.index("my_alias", "{\"name\": \"Jane\", \"age\": 25}");
```

### 3. 删除索引别名

```java
boolean success = ElasticUtils.Admin.deleteIndexAlias("my_index", "my_alias");
if (success) {
    System.out.println("别名删除成功！");
}
```

---

## 高级用法

### 4. 为多个索引创建别名（带过滤条件）

```java
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

// 创建多个索引
ElasticUtils.Admin.createIndex("logs_2024_01").create();
ElasticUtils.Admin.createIndex("logs_2024_02").create();

// 创建带过滤条件的别名，只返回 status=active 的文档
QueryBuilder filter = QueryBuilders.termQuery("status", "active");
boolean success = ElasticUtils.Admin.createIndexAlias(
    new String[]{"logs_2024_01", "logs_2024_02"}, 
    "active_logs", 
    filter
);

// 查询时会自动应用过滤条件
SearchResponse response = ElasticUtils.Query.query("active_logs")
    .matchAll()
    .search();
```

### 5. 实现索引滚动（Index Rollover）

```java
// 场景：日志系统需要按月滚动索引

// 1. 创建当前月索引
String currentIndex = "logs_" + YearMonth.now().toString().replace("-", "_");
ElasticUtils.Admin.createIndex(currentIndex).create();

// 2. 创建指向当前索引的别名
ElasticUtils.Admin.createIndexAlias(currentIndex, "logs_current");

// 3. 下个月时，创建新索引并切换别名
String nextIndex = "logs_2024_02";
ElasticUtils.Admin.createIndex(nextIndex).create();

// 先删除旧别名
ElasticUtils.Admin.deleteIndexAlias(currentIndex, "logs_current");

// 再创建新别名
ElasticUtils.Admin.createIndexAlias(nextIndex, "logs_current");

// 应用层只需使用 "logs_current" 别名，无需关心实际索引名
```

### 6. A/B 测试场景

```java
// 创建两个版本的索引
ElasticUtils.Admin.createIndex("products_v1").create();
ElasticUtils.Admin.createIndex("products_v2").create();

// 为不同用户组创建不同的别名
// 50% 用户访问 v1
ElasticUtils.Admin.createIndexAlias("products_v1", "products_group_a");

// 50% 用户访问 v2
ElasticUtils.Admin.createIndexAlias("products_v2", "products_group_b");

// 根据用户 ID 路由到不同版本
String alias = (userId.hashCode() % 2 == 0) ? "products_group_a" : "products_group_b";
List<Product> products = searchProducts(alias, query);
```

---

## 实际应用场景

### 场景 1: 零停机索引重建

```java
public class ZeroDowntimeReindex {
    
    public void rebuildIndex(String originalIndex) {
        String newIndex = originalIndex + "_new";
        String tempAlias = originalIndex + "_temp";
        
        try {
            // 1. 创建新索引
            ElasticUtils.Admin.createIndex(newIndex).create();
            
            // 2. 数据迁移（reindex）
            ElasticUtils.Admin.reindex(originalIndex, newIndex).execute();
            
            // 3. 创建临时别名指向新索引
            ElasticUtils.Admin.createIndexAlias(newIndex, tempAlias);
            
            // 4. 验证新索引数据正确性
            verifyDataIntegrity(tempAlias);
            
            // 5. 原子性切换别名
            // 注意：这里需要使用 bulk alias API 确保原子性
            // 当前实现是两步操作，生产环境建议改进
            
            // 删除旧别名
            ElasticUtils.Admin.deleteIndexAlias(originalIndex, originalIndex);
            
            // 创建新别名
            ElasticUtils.Admin.createIndexAlias(newIndex, originalIndex);
            
            // 6. 删除旧索引
            ElasticUtils.Admin.deleteIndex(originalIndex);
            
            // 7. 清理临时别名
            ElasticUtils.Admin.deleteIndexAlias(newIndex, tempAlias);
            
            System.out.println("索引重建完成！");
            
        } catch (Exception e) {
            // 回滚：删除新索引和临时别名
            ElasticUtils.Admin.deleteIndex(newIndex);
            ElasticUtils.Admin.deleteIndexAlias(newIndex, tempAlias);
            throw new RuntimeException("索引重建失败", e);
        }
    }
    
    private void verifyDataIntegrity(String index) {
        long originalCount = ElasticUtils.docCount("original_index");
        long newCount = ElasticUtils.docCount(index);
        
        if (originalCount != newCount) {
            throw new RuntimeException("数据量不一致！");
        }
    }
}
```

### 场景 2: 多租户数据隔离

```java
public class MultiTenantSearch {
    
    /**
     * 为每个租户创建独立的索引别名
     */
    public void setupTenantIndex(String tenantId, String baseIndex) {
        String tenantIndex = baseIndex + "_" + tenantId;
        String tenantAlias = "tenant_" + tenantId;
        
        // 创建租户专属索引
        ElasticUtils.Admin.createIndex(tenantIndex).create();
        
        // 创建租户别名（可添加过滤条件确保数据隔离）
        QueryBuilder filter = QueryBuilders.termQuery("tenant_id", tenantId);
        ElasticUtils.Admin.createIndexAlias(
            new String[]{tenantIndex}, 
            tenantAlias, 
            filter
        );
    }
    
    /**
     * 租户只能访问自己的数据
     */
    public List<Document> searchForTenant(String tenantId, String query) {
        String tenantAlias = "tenant_" + tenantId;
        
        // 自动应用租户过滤条件
        return ElasticUtils.Query.query(tenantAlias)
            .queryString(query)
            .search()
            .getDocuments();
    }
}
```

### 场景 3: 蓝绿部署

```java
public class BlueGreenDeployment {
    
    private static final String BLUE_INDEX = "app_blue";
    private static final String GREEN_INDEX = "app_green";
    private static final String PRODUCTION_ALIAS = "app_production";
    
    /**
     * 切换到新版本（绿环境）
     */
    public void switchToGreen() {
        // 验证绿环境健康
        if (!isHealthy(GREEN_INDEX)) {
            throw new IllegalStateException("绿环境不健康，无法切换");
        }
        
        // 切换别名
        ElasticUtils.Admin.deleteIndexAlias(BLUE_INDEX, PRODUCTION_ALIAS);
        ElasticUtils.Admin.createIndexAlias(GREEN_INDEX, PRODUCTION_ALIAS);
        
        System.out.println("已切换到绿环境");
    }
    
    /**
     * 快速回滚到蓝环境
     */
    public void rollbackToBlue() {
        ElasticUtils.Admin.deleteIndexAlias(GREEN_INDEX, PRODUCTION_ALIAS);
        ElasticUtils.Admin.createIndexAlias(BLUE_INDEX, PRODUCTION_ALIAS);
        
        System.out.println("已回滚到蓝环境");
    }
    
    private boolean isHealthy(String index) {
        // 检查索引健康状态、数据完整性等
        return true;
    }
}
```

---

## 注意事项

### ⚠️ 重要提醒

1. **别名不是索引**: 别名只是索引的引用，不能直接对别名执行某些管理操作
2. **一个别名可以指向多个索引**: 查询时会从所有关联索引中获取数据
3. **一个索引可以有多个别名**: 方便不同场景使用不同的访问路径
4. **原子性**: 当前的 `createIndexAlias` 和 `deleteIndexAlias` 是分开的操作，如果需要原子性切换，需要改进实现
5. **性能**: 通过别名访问与直接访问索引性能相同，无额外开销

### 💡 最佳实践

1. **命名规范**: 使用有意义的别名名称，如 `users_current`, `logs_active`, `products_v2`
2. **文档化**: 记录每个别名的用途和指向的索引
3. **监控**: 监控别名的使用情况，及时清理不再使用的别名
4. **测试**: 在生产环境切换别名前，先在测试环境充分验证
5. **备份**: 在进行重要的别名操作前，确保有数据备份

---

## 常见问题

### Q1: 如何查看某个索引的所有别名？
```java
// 可以通过 Cat API 或 Indices API 查询
// 目前项目中可能需要添加相应的方法
```

### Q2: 别名会影响查询性能吗？
不会。别名只是一个逻辑层，查询时会自动解析到实际的索引，性能与直接查询索引相同。

### Q3: 可以嵌套使用别名吗？
不可以。别名只能指向实际的索引，不能指向另一个别名。

### Q4: 如何确保别名切换的原子性？
当前实现是两步操作（先删后增），在高并发场景下可能存在短暂的不一致。生产环境建议：
- 使用 Elasticsearch 的 bulk aliases API（一次性提交多个动作）
- 或在应用层加锁保证互斥

---

## 相关文档

- [迁移完成报告](ALIAS_MIGRATION_COMPLETE.md)
- [整体迁移计划](migrate.md)
- [Elasticsearch 官方文档 - Aliases](https://www.elastic.co/guide/en/elasticsearch/reference/8.19/indices-aliases.html)
