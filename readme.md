# awesome-copilot

Maven 多模块 Java 基础设施工具库集合（groupId `com.awesomecopilot`，JDK 21）。每个模块封装一类基础能力，对外提供 fluent 风格静态门面（`XxxUtils`）。各模块详细能力见其目录下 readme.md，本页只做索引。

# 模块一览

| 模块 | 一句话能力 | readme |
|------|-----------|--------|
| commons-lang | 全局地基：雪花 ID、StringUtils/DateUtils/反射/序列化、线程池、限流、配置读取、Result/Page、统一异常 | [→](commons-lang/readme.md) |
| commons-spring | Spring 支撑：ServletUtils、i18n、事务时机回调 TransactionEvents、@PostInitialize、RestTemplate 版请求构建 | [→](commons-spring/readme.md) |
| commons-cloud | Sentinel 流控异常的网关/Web 统一 REST 处理 | [→](commons-cloud/readme.md) |
| copilot-json | Jackson 统一入口 JacksonUtils + 日期/枚举/精度/货币序列化器 + JsonPath | [→](copilot-json/readme.md) |
| copilot-codec | AES/DES/RSA/哈希/Base64/进制转换/JWT | [→](copilot-codec/readme.md) |
| copilot-cache | Jedis 封装：JedisUtils 数据结构分组、分布式锁(watchdog)、pub/sub、Lua、Redis 登录认证 | [→](copilot-cache/readme.md) |
| copilot-orm | Hibernate 6 封装：JpaDao、Criteria fluent、named-sql 动态模板、逻辑删除/多租户拦截 | [→](copilot-orm/readme.md) |
| copilot-search | ES 7.9.3 transport-client 封装：ElasticUtils 门面 + Query/Aggs/Admin/Mappings 分组 | [→](copilot-search/readme.md) |
| copilot-search-8.x | 同上的 ES 8 版（elasticsearch-java REST 客户端），API 与 7.x 保持一致 | [→](copilot-search-8.x/readme.md) |
| copilot-networking | Apache HttpClient 版 fluent 请求构建 + IP/域名工具 + 连接池回收 | [→](copilot-networking/README.md) |
| copilot-validation | 自定义 JSR303 校验器：@Password/@UniqueValue/@MandatoryIf 等 10 个注解 | [→](copilot-validation/README.md) |
| copilot-web | Web 层组件：全局异常 Advice、RestUtils 输出/下载、XSS 清洗、日期绑定、TraceFilter | [→](copilot-web/README.md) |
| copilot-security | Spring Security 配套：JSON 登录过滤器、REST 未认证端点、XSSFilter、JWT 解析 | [→](copilot-security/README.md) |
| copilot-workbook | POJO List ⇄ Excel（模板写出 / @Col 读入 + 校验） | [→](copilot-workbook/readme.md) |
| copilot-netty | ByteBufUtils + 50 个 Netty 示例（echo/chat/心跳/编解码/HTTP/WebSocket） | [→](copilot-netty/readme.md) |
| copilot-bigdata | HDFS 工具 | [→](copilot-bigdata/readme.md) |
| copilot-test | 1500+ 技术验证用例（并发/JDK 特性/中间件/序列化……），不被依赖，当活文档用 | [→](copilot-test/README.md) |

# 常用命令

```bash
mvn -DskipTests clean install              # 全量构建（多数测试依赖外部服务，本地建议跳测试）
mvn -DskipTests -pl copilot-search -am clean install   # 构建单模块及上游
mvn -pl copilot-search test -Dtest=AggTest#testBankAddressTerms   # 跑单个测试方法
```

# 依赖分层

`commons-lang` 是地基 → `commons-spring` / `copilot-json` / `copilot-cache` / `copilot-orm` / `copilot-search` 等各按需依赖上游。新增子模块需在根 `pom.xml` `<modules>` 注册并复用父 POM 的 `dependencyManagement`。
