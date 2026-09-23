# copilot-bigdata

大数据组件封装（包名 `com.awesomecopilot.bg`），当前为 HDFS 客户端工具。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| HDFS | `HDFSUtils` | 基于 `hadoop-client` 的 HDFS 操作封装（配置读 hdfs.properties） |

# 二 用法

```java
HDFSUtils.mkdir("/user/hive/warehouse/tmp");   // 递归建目录，失败返回 false
```

配置：classpath / 工作目录 `hdfs.properties`（NameNode 地址、user 等，走 commons-lang `PropertyReader`）。

# 三 说明

本模块目前只覆盖了 mkdir，其余 HDFS 读写操作按需在 `HDFSUtils` 中扩展（保持 commons-lang `XxxUtils` 静态门面的风格）。依赖 hadoop-client / hadoop-hdfs，版本由根 pom 统一管理。
