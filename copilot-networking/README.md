# copilot-networking

HTTP / 网络操作封装（包名 `com.awesomecopilot.networking`），底层 Apache HttpClient（含 httpasyncclient、httpmime、fluent-hc）。提供 fluent 请求构建器、OAuth2 支持、IP/域名匹配、连接池空闲回收等能力。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 门面 | `HttpUtils` | `get/post/put/delete/form` 一行起一个 fluent 请求 |
| 构建器 | `AbstractRequestBuilder` / `JsonRequestBuilder` / `FormRequestBuilder` | 链式配置 URL、方法、头、认证、body、文件、重试、SSL，再 `request()` |
| OAuth2 | `OAuth2Support` 接口 + `GrantType` / `Scope` 枚举 | basic/bearer 认证、授权类型与权限范围 |
| 枚举 | `HttpMethod` / `Scheme` / `ContentType` | 请求方法、URL scheme、Content-Type |
| 常量 | `HttpHeaders` / `HttpMethods` / `ContentTypes` / `MediaType` | 常用 header/method/MIME 常量 |
| IP | `IPUtils` | IPv4/IPv6 校验、子网掩码计算、IP 是否落在网段内 |
| 匹配 | `IpAddressMatcher` / `RequestMatcher` | 按 IP 或网段匹配请求（黑白名单） |
| 域名 | `DomainUtils` | 从 host 提取主域名 |
| 连接池 | `IdleConnectionEvictor` | 后台线程定期清除过期/空闲连接 |
| 错误 | `ErrorUtils` / `HttpRequestException` | 响应状态码检查、请求异常封装 |

# 二 HttpUtils 用法

```java
// JSON POST，自动把 body 序列化成 JSON，按 responseType 反序列化回对象
String accountUrl = "http://localhost:8083/account/reduce-balance";
Result accountResult = HttpUtils.post(accountUrl)
        .body(accountDTO)
        .responseType(Result.class)
        .request();

// GET + header
String body = HttpUtils.get(url).addHeader("X-Token", t).request();

// 表单提交（file(...) 自动走 multipart）
HttpUtils.form(url).param("k", "v").file("f", new File("a.xlsx")).request();
```

# 三 构建器可配置项（AbstractRequestBuilder）

链式方法覆盖 URL 各部分与请求行为：

- `url / scheme / host / port / path / method`：拆解式配置目标地址。
- `basicAuth(user, pwd)` / `bearerAuth(token)`：认证。
- `addHeader` / `addCookie`：请求头与 Cookie。
- `body(obj)` / `responseType(Class)` / `returnBytes(true)`：请求体（Object 自动 Jackson 序列化）与响应处理方式。
- `trustAllCerts(true)`：显式开启才信任所有证书（内网自签场景）；默认走正常证书校验。
- 三个超时 `connectionTimeout / soTimeout / connectionManagerTimeout(value, TimeUnit)`：分别控制建连、传输（两次数据包间最大空闲）、从连接池借连接。**不调用也有默认值**：5s / 10s / 2s；可在 classpath 或工作目录放 `http.properties` 按 `http.connection.timeout` / `http.socket.timeout` / `http.connection-manager.timeout`（毫秒）整体覆盖（值必须为正整数才生效），显式调用优先级最高（评审报告 P0-6 修复——此前三个超时不设 = 无限等待，慢下游会占满每路由 20 个连接的共享池，把同池所有调用拖到排队）。注意与 `timeout(...)`（整个请求生命周期的中断上限）的交互：只设 `timeout(60s)` 而不设 `soTimeout` 的请求，现在会先在 10s 处抛读超时——明知下游慢的场景请显式放宽 `soTimeout`。
- `resolveRetryHandler(...)`：重试策略；`cookieStore(...)`：Cookie 管理。
- `request()`：执行；非 2xx 由 `ErrorUtils.checkError` 判定并抛 `HttpRequestException`。

# 四 IP 与域名工具

```java
IPUtils.isValidIpV4("192.168.1.1");
IPUtils.isValidIpV6("2001:db8::1");
IPUtils.isIpInRange("10.0.0.5", "10.0.0.0/24");      // 是否落在网段
int mask = IPUtils.subnetMask("255.255.255.0");

new IpAddressMatcher("192.168.0.0/16").matches(request); // 按网段匹配请求

DomainUtils.getDomain("www.example.com");            // -> example.com
```

# 五 安全说明

`AbstractRequestBuilder` 默认走**正常的 HTTPS 证书与主机名校验**；只有显式调用 `trustAllCerts(true)` 才信任所有证书（为内网自签证书场景留的开关，评审报告 P2-1 修复引入）。对接公网不要开这个开关。

# 六 与 commons-spring 的区别

`commons-spring` 也有一套 `Requests`/`FormRequestBuilder`/`JsonRequestBuilder`，但底层是 Spring `RestTemplate`，适合已引 spring-web 的项目；本模块是 Apache HttpClient 实现，不依赖 Spring，连接池与超时控制更细。二选一按项目栈决定。
