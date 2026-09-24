# copilot-search-8.x

Elasticsearch **8.x** 客户端封装（包名 `com.awesomecopilot.search8x`），底层官方 `co.elastic.clients:elasticsearch-java`（REST 协议）。它是 `copilot-search`（transport-client 7.9.3 版，**已废弃**）的替代品：**`ElasticUtils` 门面与 API 分组和 7.x 版保持一致**，让原有业务代码/测试尽量少改。

> 本模块是 Elasticsearch 集成的当前主力，所有新代码一律用 `com.awesomecopilot.search8x.ElasticUtils`。`copilot-search` 仅为仍在跑 ES 7.x 集群的老项目保留。

# 一 能力总览（与 copilot-search 同构）

| 分组 | 核心入口 | 提供什么 |
|------|----------|----------|
| 文档 CRUD | `ElasticUtils.index/create/get/getWithVersion/update/upsert/delete/deleteBy/exists/docCount/mget` | 单条与批量读写，POJO 自动序列化 |
| 批量 | `bulkIndex` / `bulkIndexConcurrent` / `bulkUpdate` + builder | Bulk 与多线程分片写入 |
| 查询 | `ElasticUtils.Query` + `builder/query/` 一系列 builder | match/multiMatch/term/terms/bool/range/exists/ids/scroll/template 等 |
| 补全建议 | `ElasticSuggestBuilder` / `ElasticContextSuggestBuilder` | term/phrase/completion suggest |
| 聚合 | `ElasticUtils.Aggs` + `builder/agg/` | terms/composite/dateHistogram/stats/cardinality… |
| 索引管理 | `ElasticUtils.Admin` / `Mappings` / `Settings` / `Cluster` | 建删索引、别名、模板、mapping、设置 |
| 注解 | `@Index` / `@Field` / `@DocId` | POJO → mapping 自动生成 |
| 工厂 | `ElasticsearchClientFactory` | 多来源配置创建 `ElasticsearchClient` |

与 7.x 的差异细节、待完成项记录在同目录 `ES7_TO_ES8_MIGRATION_GUIDE.md`（部分旧 builder 标记 @Deprecated，新代码建议用返回 `co.elastic.clients` 原生对象的方法）。

# 二 连接配置

`ElasticsearchClientFactory` 按以下优先级解析配置（高→低）：

1. Spring Environment（配合 `registerContext(applicationContext)`，可接 Nacos 配置中心，详见 `NACOS_CONFIG_GUIDE.md`）
2. 系统属性 `-Delastic.rest.hosts=...`
3. 环境变量 `ELASTIC_REST_HOSTS`
4. classpath / 工作目录下 `elastic.properties`

```properties
elastic.rest.hosts=http://127.0.0.1:9200,http://10.0.0.2:9200
elastic.username=elastic
elastic.password=xxx
cluster.name=es8-application
```

注意：8.x 只走 REST（9200），不再需要 9300 transport 地址；静态工厂不在 Spring 容器内，想用 Nacos 配置必须显式 `registerContext`，否则读不到。

# 三 用法

入口与 `copilot-search` 完全同名（改 import 到 `com.awesomecopilot.search8x.ElasticUtils` 即可），示例风格：

```java
// 写文档（返回文档 id）
String id = ElasticUtils.index("rico", product);            // 自动序列化 POJO
ElasticUtils.index("rico").id("1").doc(product).request();  // builder 版

// 查询：matchQuery 起 builder，结果可 queryForList / queryForObject 映射回 POJO
List<Blog> blogs = ElasticUtils.Query.matchQuery("bank")
        .field("name").query("招行")
        .queryForList(Blog.class);

// range 查询
var q = ElasticUtils.Query.range("employees").field("age").gt(20);

// 聚合 + filter
ElasticUtils.Aggs.terms(...)
        .filter(ElasticUtils.Query.termQuery("pay_status", 1));
```

各查询/聚合的完整示例可参考 `copilot-search/readme.md` 第一~四章（DSL 语义相同），以及本模块 `src/test` 下的 `ElasticUtilsTest` / `AdminTest` / `AggTest` 用例。

## scroll 深度遍历与上下文释放（2026-09-24，评审报告 P0-8 修复）

`Query.scrollQuery(index)` 返回的 `ElasticScrollQueryBuilder` 实现了 `AutoCloseable`，服务端 scroll 上下文在以下时机自动释放（`clearScroll`），不再只等 `scrollTime` 到期回收：

- 遍历到空批次（数据读完）时；
- `queryForList()` 通信失败（IOException）或 ES 返回错误（ElasticsearchException）时。

结果解析异常（如 `resultType` 配错导致反序列化失败）不会释放上下文——catch 之后重试可以跳过坏批继续遍历。

**提前放弃遍历（中途 return/break、或拿到 `getScrollId()` 后不再使用）不会自动释放**，必须自己 close，否则上下文仍保留到 `scrollTime` 到期。推荐 try-with-resources：

```java
try (ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery("product").size(1000)) {
    while (true) {
        List<Product> batch = builder.queryForList();
        if (batch.isEmpty()) break;
        export(batch);
    }
}   // 即使中途 break/抛异常, 上下文也会在这里被 clear
```

`close()` 幂等，重复调用无害；close 后 `scrollId` 置空，同一 builder 再次 `queryForList()` 会发起新的初始查询（注意：是重新从头遍历，不是续跑）。builder 非线程安全，不要跨线程共享。不主动 clear 的后果：全集群打开的 scroll 上下文堆到 `search.max_open_scroll_context`（默认 500）后，所有新 scroll 请求被拒。
