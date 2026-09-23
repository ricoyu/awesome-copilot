# copilot-security

Spring Security 配套组件（包名 `com.awesomecopilot.security`）：统一认证入口、REST 风格未认证响应、XSS 过滤器、JWT 解析工具。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 认证过滤器 | `UsernamePasswordAuthenticationFilter` | 先按表单提交解析凭证，失败再尝试 request body（JSON 登录）；支持 RSA 加密密码（`setRsaEncrypted`） |
| 未认证端点 | `RestAuthenticationEntryPoint` | 未认证访问时返回 REST JSON（而不是 302 跳登录页） |
| 异常过滤 | `SecurityExceptionFilter` | 过滤器链上抛出的安全异常统一转 JSON 响应 |
| Token 切面 | `TokenEndpointAspect` | `/oauth/**` 出错时不再打 ERROR 日志（噪音治理） |
| XSS | `XSSFilter` + `XSSRequestWrapper` | 注册为 bean 即启用；参数/Header/请求流过 JSoup+ESAPI 清洗（`XSSUtils.stripXSS`） |
| JWT | `Jwts` / `SecurityUtils` | 从 token 串解析 Claims/Jws（解析失败回调 `onError`）；`signingKey(key)` 拿 JwtBuilder |
| OAuth2 | `OAuthUtils` / `SecurityRequestUtils` / `AuthRequest` / `LoginRequest` | Bearer 头解析、认证请求参数拆分、路径匹配（跳过认证清单） |
| 异常映射 | `SpringSecurityExceptions` | Spring Security 常见异常 → 框架 `ErrorType` 统一错误码 |
| 异常 | `JwtTokenParseException` / `TokenExpiredException` / `TimestampInvalid(Missing)Exception` | token 相关细粒度异常 |
| 常量 | `CopilotSecurityConstants` / `GrantType` | 安全相关常量与授权类型 |

# 二 典型装配

```java
http.addFilterBefore(new XSSFilter(),
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
    .exceptionHandling(e -> e.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
    .addFilter(copilotUsernamePasswordAuthenticationFilter()); // 支持 JSON body 登录
```

# 三 注意

- XSS 清洗只覆盖参数、Header 和请求输入流；JSON 反序列化层的清洗在 `copilot-json` 的 `XssStringJsonDeserializer` / `copilot-web` 的 `XssCleanUtils`，避免同一内容重复转义。
- `AuthRequest.requestPathMatchs` 用 Ant 风格匹配"哪些路径免认证"。
