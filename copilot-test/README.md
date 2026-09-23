# copilot-test

技术验证与示例集合（1500+ 测试文件），不被任何其他模块依赖，用途是验证各组件行为、沉淀踩坑经验、当活文档查。`src/main` 下只有两个演示程序：`ClassLoaderEcho`（类加载器打印）和 `jvm.HeapOOMLeakDemo`（模拟本地缓存只 put 不 evict 的堆溢出）。

# 一 测试用例按主题分组（src/test/java/com/awesomecopilot/ 下的包）

| 主题 | 包 |
|------|-----|
| Java 基础/OOP/泛型/反射 | general, oop, innerclass, generic, reflection, proxy, spi, paramname, methodhandles, unsafe, primary, tree, map, collections, optional, functional, stream 相关 |
| 并发/JVM | concurrent, juc, atomic, threadlocal, forkjoin, parallel, allocation, jmm, classloader, jvm, thread |
| JDK 版本特性 | java8, java9, java11, java12, java13, java14, java15, java16, java17, defaultstaticmethod |
| IO/NIO/AIO | io, nio, niodemo, aio, networking, http, httpclient, okhttp3, websocket, sftp, mail |
| 序列化/编解码 | serialize, codec, kryo, base64, binary, bitop, chars, regex, pattern, hash, number |
| JSON | json, jsonpath, jackson, gson, fastjson, xml, xpath, ognl |
| 缓存/Redis | redis, redisson, caffeine, bloom, sentinel(限流), ratelimiter |
| 数据库/大数据 | jdbc, sql, entity, velocity, lucene, excel, pdf, mongo |
| 中间件/分布式 | zookeeper, curator, nacos, fastdfs, message(mq), distributed 相关 |
| 安全 | security, encrypt, jwt, oauth2, xss |
| 测试框架/杂项 | utils, enums, fluent, interview(面试题验证), excel, faker |

# 二 使用方式

```bash
# 跑本模块全部测试（部分用例需要真实 Redis/ES/ZK 等外部服务）
mvn -pl copilot-test test

# 只跑某个主题
mvn -pl copilot-test test -Dtest=*Redisson*
```

`src/test/resources` 下备好了各主题的素材：excel 样本、json 数据、lua 脚本（cjson/条件自增等）、named-sql、hbm.xml、jms/spring 配置、公私钥、suricata.yaml 等。
