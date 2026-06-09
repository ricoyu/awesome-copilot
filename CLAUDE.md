# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

`awesome-copilot` 是一个 Maven 多模块的 Java 基础设施工具库集合（version 21.0.3，JDK 21）。每个 `copilot-*` / `commons-*` 子模块封装一类基础能力（缓存、ORM、搜索、网络、编解码等），对外提供 fluent 风格、易用的 API。`groupId` 统一为 `com.awesomecopilot`，所有 Java 代码位于 `com.awesomecopilot.*` 包下。

## 常用命令

```bash
# 全量构建（跳过测试），多数模块依赖 ES/Redis/MySQL 等外部服务，本地构建建议跳过测试
mvn -DskipTests clean install

# 构建单个模块及其依赖（-am 一并构建上游模块）
mvn -DskipTests -pl copilot-search -am clean install

# 运行某模块全部测试
mvn -pl copilot-search test

# 运行单个测试类
mvn -pl copilot-search test -Dtest=AggTest

# 运行单个测试方法
mvn -pl copilot-search test -Dtest=AggTest#testBankAddressTerms
```

测试框架为 JUnit 5（jupiter）+ AssertJ + Mockito + Hamcrest。`verify` 阶段会通过 `maven-source-plugin` 附带打 source jar。

## 模块依赖与架构

模块间存在严格的分层依赖，`commons-lang` 是所有模块的地基：

- **commons-lang**：基础工具层，被几乎所有模块依赖。提供 `StringUtils`、`ReflectionUtils`、`CollectionUtils`、`DateUtils`、序列化（`KryoUtils`/`FstUtils`/`ProtostuffUtils`）、`SnowflakeId`、`PropertyReader`（读取 classpath 下 `xxx.properties` 的核心配置入口）等。修改此模块会影响全局。
- **commons-spring** → 依赖 commons-lang。Spring 相关支撑（context、AOP 等）。
- **copilot-json** → 依赖 commons-lang。`JacksonUtils`（统一 JSON 序列化入口，测试中大量用 `toJson`/`toPrettyJson`）、JsonPath 封装（`JsonPathUtils`）。
- **copilot-networking** → HTTP 客户端封装，`AbstractRequestBuilder` 为 fluent request 构建核心。
- **copilot-cache** → Redis 封装。入口 `JedisUtils`，底层 `JedisPoolOperations`/`JedisClusterOperations`；`concurrent/BlockingLock` 是带 watchdog 续期的分布式锁。
- **copilot-orm** → 基于 Hibernate 6 的 ORM 封装。`JpaDao`、`EntityOperations`、`CriteriaQueryBuilder`/`SqlQueryBuilder`/`NativeSqlQueryBuilder` 提供 criteria 与原生 SQL 的 fluent 构建。
- **copilot-search** → Elasticsearch 客户端封装（详见下节）。
- **copilot-codec / copilot-netty / copilot-security / copilot-validation / copilot-web / copilot-workbook / copilot-bigdata**：分别封装编解码、Netty、Spring Security、校验、Web、POI Excel、大数据相关能力，均以 commons-lang 为基础按需依赖上游模块。
- **copilot-test**：集中存放各类示例与集成测试（1500+ 测试文件），不被其他模块依赖，用作验证与示例参考。

新增子模块时，需在根 `pom.xml` 的 `<modules>` 中注册，并复用父 POM 的 `dependencyManagement` 统一版本。

## copilot-search 与 ES 8.x 迁移（当前工作重点）

`copilot-search` 是基于 **Elasticsearch transport-client 7.9.3** 的 fluent 封装，核心是 `ElasticUtils`（`com.awesomecopilot.search.ElasticUtils`）：

- 静态门面类，持有 `public static final TransportClient CLIENT = TransportClientFactory.create()`。
- 顶层静态方法覆盖文档 CRUD（`index`/`create`/`get`/`update`/`upsert`/`delete`/`bulkIndex`/`mget`）。
- 通过嵌套静态类组织领域 API：`ElasticUtils.Query`、`ElasticUtils.Aggs`、`ElasticUtils.Admin`、`ElasticUtils.Mappings`、`ElasticUtils.Settings`、`ElasticUtils.Cluster`，配合 `builder/` 下的各类 fluent builder（query/agg/bulk/admin）。
- 连接配置由 `TransportClientFactory` 通过 `PropertyReader("elastic")` 读取 `elastic.properties`（`cluster.name`、`elastic.hosts`（9300，transport）、`elastic.rest.hosts`（9200）、`elastic.username`/`password`）。测试需要一个真实运行的 ES 集群。

**TASKS.md 描述的迁移任务**：新建子模块 `copilot-search-8.x`，将底层从 transport-client 替换为官方推荐的 `co.elastic.clients:elasticsearch-java:8.19.16`（ES 8 已移除 transport-client），包名由 `com.awesomecopilot.search` 改为 `com.awesomecopilot.search8x`。目标是**保持 `ElasticUtils` 接口与代码风格一致，使 `copilot-search` 原有单元测试无需改动即可在新模块跑通**；若完全无缝迁移困难，API 可做微调。

## 已知问题（来自 CODE_REVIEW_REPORT.md，改动相关代码时注意）

- `copilot-networking/.../AbstractRequestBuilder.java`：默认全局关闭了 HTTPS 证书与 hostname 校验（`TrustStrategy` 全信任 + `NoopHostnameVerifier`），且 `request()` 中 `CloseableHttpClient`/`CloseableHttpResponse` 未用 try-with-resources，存在连接池泄漏。
- `copilot-cache/.../concurrent/BlockingLock.java`：`watchDogStopped` 实例复用后未重置，二次加锁 watchdog 失效；`interruptExecutorThread()` 用反射访问 JDK 内部线程池字段。
- 根 `pom.xml`：历史上存在同一依赖多版本漂移（caffeine / slf4j / logback / jackson），新增依赖请统一走 `dependencyManagement`。

## 约定

- 注释、文档、提交信息均使用简体中文；类头注释保留 `@author Rico Yu` 风格。
- 工具类多为 `final` + 静态方法的门面模式（`XxxUtils`），新增能力优先沿用该风格并复用 commons-lang 现有工具，避免引入重复实现。
- 提交信息遵循 `type(scope): 描述` 格式（如 `feat(search): ...`）。
- 永远用中文回复
