# copilot-web

Web 层通用组件封装，为 Spring MVC / Spring Cloud 微服务提供全局异常处理、REST 输出、消息国际化、XSS 防护、链路追踪等基础能力。

---

# 一 模块简介

copilot-web 是 awesome-copilot 体系中面向 Web 层的核心模块，基于 Jakarta Servlet + Spring WebMVC 构建。  
引入该模块后，业务系统可获得：

- 统一的全局异常处理（REST 风格 JSON 响应）
- 便捷的 JSON 输出与文件下载工具
- 消息国际化（i18n）支持
- 请求链路追踪（traceId）
- XSS 防护与请求体重复读取
- 日期类型自动绑定
- ThreadLocal 自动清理

---

# 二 核心功能

## 2.1 全局异常处理

`RestExceptionAdvice` 基于 `@RestControllerAdvice`，统一拦截 Controller 层抛出的异常并以标准 `Result` JSON 格式返回。

支持的异常类型：

| 异常类型 | 说明 |
| --- | --- |
| `BusinessException` | 通用业务异常，支持 i18n 消息 |
| `ServiceException` | 服务层异常 |
| `ApplicationException` | 应用级异常 |
| `ValidationException` | 表单 / Bean 校验失败 |
| `GeneralValidationException` | 手工校验不通过 |
| `EntityNotFoundException` | 实体未找到 |
| `LocalizedException` | 国际化异常 |
| `MaxUploadSizeExceededException` | 文件上传超限 |
| `Throwable` | 兜底处理，整合 Sentinel 熔断统计 |

同时兼容 Sa-Token 认证框架的 `NotLoginException`、`NotPermissionException`。

## 2.2 REST 输出工具

`RestUtils` 提供在 Filter / Interceptor 等非 Controller 环境中直接输出 JSON 或下载文件的能力：

```java
// 输出 JSON
RestUtils.writeJson(response, result);
RestUtils.writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR, result);

// 输出原始 JSON 字符串
RestUtils.writeRawJson(response, jsonString);

// 文件下载
RestUtils.download(file);
RestUtils.download(path, "自定义文件名.xlsx");
```

## 2.3 消息国际化

`MessageHelper` 封装 Spring `MessageSource`，根据当前请求 Locale 自动返回对应语言的消息：

```java
// 根据 code 获取消息
String msg = MessageHelper.getMessage("error.user.notfound");

// 带参数
String msg = MessageHelper.getMessage("error.order.expired", List.of(orderId));

// 指定 Locale
String msg = MessageHelper.getMessage("error.user.notfound", Locale.ENGLISH, userId);
```

配合 `LocalizedException` 使用，可实现异常消息的国际化：

```java
throw new LocalizedException("5001", "error.user.notfound", List.of(userId), "User not found");
```

## 2.4 链路追踪

`TraceFilter` 自动为每个请求生成或透传 `traceId`，并写入 MDC，便于日志输出：

- 优先读取请求头 `TRACE_ID`
- 若不存在则自动生成

在 `logback-spring.xml` 中配置 `%X{traceId}` 即可在日志中输出 traceId。

## 2.5 XSS 防护

`XssCleanUtils` 提供 XSS 清洗能力，可清理 `<script>` 标签、`javascript:` 伪协议、`onXXX` 事件属性等：

```java
String clean = XssCleanUtils.clean(dirtyInput);
```

`XssHttpServletRequestWrapper` 可在 Filter 层自动清洗请求参数。

## 2.6 请求体重复读取

`HttpServletRequestRepeatedReadFilter` 将请求体缓存，允许在 Filter、Interceptor、Controller 中多次读取 Body：

```java
// 注册 Filter
@Bean
public FilterRegistrationBean<HttpServletRequestRepeatedReadFilter> repeatedReadFilter() {
    FilterRegistrationBean<HttpServletRequestRepeatedReadFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new HttpServletRequestRepeatedReadFilter());
    reg.addUrlPatterns("/*");
    return reg;
}
```

## 2.7 日期类型自动绑定

`GlobalBindingAdvice` 自动将字符串参数绑定为日期类型，支持：

- `java.util.Date`（yyyy-MM-dd HH:mm:ss / yyyy-MM-dd HH:mm）
- `LocalDate`
- `LocalDateTime`
- `LocalTime`

## 2.8 跨域支持

`CORS` 提供 Builder 模式配置跨域响应头：

```java
CORS.builder()
    .allowedOrigins("https://example.com")
    .allowedMethods("GET", "POST")
    .allowedHeaders("Content-Type", "Authorization")
    .build(response);

// 或允许所有
CORS.builder().allowAll().build(response);
```

## 2.9 ThreadLocal 清理

`ThreadLocalCleanupListener` 在请求开始和结束时自动清理 `ThreadContext`，防止线程复用导致的数据污染。

---

# 三 依赖说明

| 模块 | 说明 |
| --- | --- |
| `commons-lang` | 基础工具类、异常体系、Result 封装 |
| `copilot-validation` | Bean 校验支持 |
| `copilot-json` | Jackson 序列化 |
| `sentinel-core`（可选） | 整合 Alibaba Sentinel 熔断统计 |
| Spring Web / WebMVC | Spring MVC 框架 |
| Jakarta Servlet API | Servlet 规范 |

---

# 四 使用示例

## 4.1 业务异常处理

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new BusinessException(NOT_FOUND.code(), "error.user.notfound");
        }
        return Results.ok(BeanUtils.copyProperties(user, UserVO.class));
    }
}
```

异常将被 `RestExceptionAdvice` 捕获并返回统一 JSON 格式。

## 4.2 在 Filter 中输出 JSON

```java
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletResponse response = (HttpServletResponse) res;
        if (!authenticated) {
            Result result = Results.fail().status(TOKEN_EXPIRED).build();
            RestUtils.writeJson(response, HttpStatus.UNAUTHORIZED, result);
            return;
        }
        chain.doFilter(req, res);
    }
}
```

## 4.3 文件下载

```java
@GetMapping("/export")
public void export(HttpServletResponse response) {
    File excel = reportService.generateExcel();
    RestUtils.download(excel, "报表.xlsx");
}
```

## 4.4 国际化异常

```java
// messages_zh_CN.properties
error.user.notfound=用户 {0} 不存在

// messages_en_US.properties
error.user.notfound=User {0} not found

// 业务代码
throw new LocalizedException("5001", "error.user.notfound", List.of(userId), "User not found");
```

---

# 五 注意事项

1. **Spring Boot 自动装配**  
   `RestExceptionAdvice`、`GlobalBindingAdvice`、`TraceFilter` 均通过 `@Component` / `@RestControllerAdvice` 自动注册，引入依赖即生效。

2. **Sentinel 整合**  
   当 classpath 中存在 `restBlockExceptionHandler` Bean 时，`RestExceptionAdvice` 会将异常委托给 Sentinel 处理，避免冲突。

3. **跨域配置**  
   `RestUtils` 默认不开启跨域（由 `copilot.mvc.cors.enabled` 控制），在 Spring Cloud 网关环境下建议由网关统一处理跨域，避免双重 CORS 头导致浏览器报错。

4. **请求体重复读取**  
   `HttpServletRequestRepeatedReadFilter` 需手动注册，适用于需要在 Filter 和 Controller 中同时读取 Body 的场景（如签名校验 + 业务处理）。

5. **ThreadLocal 清理**  
   `ThreadLocalCleanupListener` 需手动注册为 `ServletRequestListener`，建议在使用 `ThreadContext` 的应用中配置，防止内存泄漏。

6. **日期格式**  
   `GlobalBindingAdvice` 支持的日期格式为 `yyyy-MM-dd HH:mm:ss` 和 `yyyy-MM-dd HH:mm`，如需其他格式请自行扩展。
