# copilot-web

Web 层通用组件封装，为 Spring MVC / Spring Cloud 微服务提供全局异常处理、REST 输出、消息国际化、XSS 清洗、请求包装、日期绑定、链路追踪等基础能力。

> **重要：本模块是"组件库"，不是"starter"。** 它自己不带任何自动装配声明（无 `AutoConfiguration.imports`）。自动装配在独立仓库 `copilot-starter` 中提供：引入 `copilot-spring-boot-web-starter` / `copilot-spring-cloud-starter` / `copilot-spring-security6-starter` 才会把本模块的组件注册进过滤器链和 MVC 配置；只依赖 `copilot-web` 时，所有组件都需要手工装配。下文每项能力都标注了实际的启用方式。

---

# 一 能力总览

| 能力 | 核心类 | 启用方式 |
| --- | --- | --- |
| 全局异常处理 | `RestExceptionAdvice` | starter 自动装配（`copilot-spring-boot-web-starter` 的 `CopilotMvcConfiguration`，可用 `copilot.mvc.rest-exception-advice-enabled=false` 关闭）；裸依赖时靠 `@RestControllerAdvice` 注解 + 组件扫描到本包才生效 |
| REST 输出 / 文件下载 | `RestUtils` | 直接调用静态方法 |
| 跨域响应头 | `CORS` | 手动调用，或由 `RestUtils` 按开关自动附加（见注意事项 3） |
| 消息国际化 | `MessageHelper` / `LocalizedException` | 直接调用；要求容器里有名为 `messageSource` 的标准 MessageSource Bean |
| XSS 清洗 | `XssCleanUtils` | 直接调用，或装配 `XssHttpServletRequestWrapper`（XssFilter 在 starter 的 `copilot-spring-boot-web` 模块里，按 `copilot.filter.xss-enabled=true` 开启，默认关） |
| 请求体重复读取 | `HttpServletRequestRepeatedReadFilter` + `RepeatedReadHttpServletRequestWarpper` | 开关 `copilot.filter.repeated-read` 存在时由 starter 注册；或在 Spring Security 装配路径（`copilot-spring-security6-starter`）中被加入链；裸依赖时手动注册 FilterRegistrationBean |
| Filter 里修改请求头 | `RequestHeaderModifiableFilter` + `HeaderMapRequestWrapper` | 继承抽象 Filter 后手动注册 |
| 日期参数绑定 | `GlobalBindingAdvice` + 4 个日期 ArgumentResolver | starter 自动装配（`CopilotMvcConfiguration`）；裸依赖时手动注册 Advice 并 `addArgumentResolvers` |
| 枚举 / 逗号数组参数转换 | `GenericEnumConverter`、`ObjectToEnumConverterFactory`、`StringToArrayConverter`、`CustomConversionServiceFactoryBean` | starter 里 `addFormatters` 注册了 GenericEnumConverter（属性 code/desc）；逗号转数组需注册 ConversionService Bean（starter 已注册，属性为 code） |
| 链路追踪 | `TraceFilter` | **没有任何 starter 装配它**。需组件扫描到 `com.awesomecopilot.web.filter` 包（它标了 `@Component`），或手动 `new TraceFilter()` 注册 FilterRegistrationBean |
| 过滤器链异常处理 | `ExceptionFilter` | 未标注解，需手动注册（`copilot-spring-cloud` 模块有自己的同名 Filter，别混淆） |
| ThreadLocal 清理 | `ThreadLocalCleanupListener` | starter 自动装配（`CopilotThreadAutoConfiguration`，注册为 ServletListener） |

---

# 二 核心功能

## 2.1 全局异常处理

`RestExceptionAdvice` 基于 `@RestControllerAdvice`，统一拦截 Controller 层抛出的异常并以标准 `Result` JSON 格式返回。

| 异常类型 | 响应 |
| --- | --- |
| `BusinessException` | HTTP 200 + 业务码，消息走 i18n（`I18N.i18nMessage`） |
| `ServiceException` / `ApplicationException` | HTTP 200 + 异常自带 code/message |
| `LocalizedException` | HTTP 200 + statusCode + 国际化消息（取不到消息时回退 defaultMessage，不会返回 null） |
| `ValidationException` / `GeneralValidationException` / `MethodArgumentNotValidException` | HTTP 200 + `4002` + 字段错误列表 |
| `UniqueConstraintViolationException` | HTTP 200 + `5001` |
| `EntityNotFoundException` | HTTP 200 + `404` 段业务码 |
| `HttpRequestMethodNotSupportedException` | HTTP 200 + `4051` |
| `HttpMessageNotReadableException`（请求体解析失败） | **HTTP 500** + `5001`（评审报告 P2-4 指出该语义值得商榷，尚未改） |
| `TypeMismatchException` | 走 Spring 默认处理（HTTP 400） |
| `MaxUploadSizeExceededException` | HTTP 200 + `4005` + i18n 消息（从异常文本解析实际/限制大小填入消息参数） |
| 其余 `Throwable` | 非业务异常返回 **HTTP 500**（便于微服务间调用方感知失败并触发熔断统计）；cause 里能翻出业务异常时按业务异常返回 200 |

兼容 Sa-Token 的 `NotLoginException` / `NotPermissionException`（按类名匹配，避免硬依赖）。整合 Sentinel（存在 `restBlockExceptionHandler` Bean 时）会对异常调用 `Tracer.trace(e)` 计入熔断统计，响应仍由本 Advice 产出。

## 2.2 REST 输出工具

`RestUtils` 供 Filter / Interceptor / 认证回调等非 Controller 环境直接写响应：

```java
RestUtils.writeJson(response, result);                              // 200 + JSON
RestUtils.writeJson(response, HttpStatus.UNAUTHORIZED, result);     // 指定状态码
RestUtils.writeRawJson(response, jsonString);                       // 原样输出字符串

RestUtils.download(file);                                           // 以文件名下载
RestUtils.download(path, "自定义文件名.xlsx");                        // 流式拷贝, 带 Content-Length
```

`download` 要求当前线程有 Spring 的 RequestAttributes（Controller/Filter 内满足），否则抛 `DownloadException`。大文件为流式写出，峰值内存与文件大小无关。

## 2.3 消息国际化

`MessageHelper` 懒加载容器里的 `MessageSource`（Bean 名须为 `messageSource`）：

```java
String msg = MessageHelper.getMessage("error.user.notfound");            // 取不到返回 null
String msg = MessageHelper.getMessage("error.order.expired", "订单过期");  // 取不到返回默认消息
String msg = MessageHelper.getMessage("code", Locale.ENGLISH, userId);     // 取不到返回 code 本身
```

行为约定（2026-09-17 评审修复后）：容器里没有 MessageSource 时**不抛异常**，按上述规则降级，并 WARN 一次提示装配问题；单个 code 查不到记 DEBUG 日志。`LocalizedException.getLocalizedMessage()` 在 i18n 取不到时自动回退构造时传入的 defaultMessage。

## 2.4 XSS 清洗

`XssCleanUtils.clean(String)` 移除 HTML 注入点：`<script>` 标签及内容、`javascript:` 伪协议前缀、`onXXX` 事件属性（带引号/不带引号）。

**清洗只作用于标签与协议层。** 纯文本里的 `alert(...)` / `eval(...)` 等函数名不再删除（评审报告 P0-2）——它们出现在业务文案里属正常内容；文本要渲染成 HTML 时请在输出侧做转义（如 commons-lang 的 `StringUtils.escapeHtml4`）。

```java
String clean = XssCleanUtils.clean(dirtyInput);
List<String> cleaned = (List<String>) XssCleanUtils.cleanObject(list);  // 递归清洗集合/数组/Map, 注意用返回值
```

装配 `XssHttpServletRequestWrapper` 后可在 Servlet 层自动清洗参数、请求头和 JSON/表单请求体（starter 里对应 `copilot.filter.xss-enabled=true`）。

## 2.5 请求体重复读取

`HttpServletRequestRepeatedReadFilter` 用 `RepeatedReadHttpServletRequestWarpper` 按**原始字节**缓存请求体（评审报告 P1-1 修复后），字段值含换行/制表符的 JSON 也能原样二次读取；`multipart/form-data`（文件上传）不做缓存直接放行。

适用场景：签名校验 Filter 读过 body 后 Controller 还要再读。注意 body 会完整驻留内存，大请求体请评估。

## 2.6 Filter 中修改请求头

```java
public class MyHeaderFilter extends RequestHeaderModifiableFilter {
    public boolean matches(HttpServletRequest req) { return true; }
    public void addHeader(HeaderMapRequestWrapper req) {
        req.addHeader("tenantId", "abc");   // 注意: 实际是覆盖(set)语义, 同名只留最后一个(评审报告 P2-9)
    }
}
```

## 2.7 日期类型自动绑定

两层机制并存，解析优先级明确：

1. **Controller 方法参数**（`@RequestParam Date d` 等）：4 个 ArgumentResolver 处理。参数上写了 `@DateTimeFormat(pattern=...)` 按 pattern **严格**解析（输入形态不符直接报错，不会悄悄解析出错值）；写了 `@DateTimeFormat(iso=...)` 按 ISO 解析且兼容带 `Z`/`+08:00` 偏移的值；**没写注解**则走 `DateUtils.parse` 自动匹配常见格式（yyyy-MM-dd HH:mm:ss、yyyy-MM-dd、ISO8601、RFC1123 等 40+ 种，解析失败返回 null）。
2. **@ModelAttribute 表单对象字段**：`GlobalBindingAdvice` 注册的 PropertyEditor 处理，走 `DateUtils` 自动匹配，字段上的 `@DateTimeFormat` 会被编辑器抢先、不生效（评审报告 P1-4 已知边界）。

时区：注解 pattern/iso 路径与自动匹配路径都按 Asia/Shanghai 解释无时区信息的值。

## 2.8 枚举与数组参数转换

- `GenericEnumConverter`（starter 默认注册，属性 code、desc）：枚举参数可按 code 属性值、desc 属性值、name、ordinal 匹配。
- `CustomConversionServiceFactoryBean`：整表替换应用的 ConversionService，注册 `StringToArrayConverter` + 按配置属性匹配的枚举转换。注册为 Bean 名 `conversionService` 即被 Spring MVC 采用；`properties` 未配置时枚举转换会失效，务必显式配置。
- `StringToArrayConverter`：逗号分隔字符串 → `Integer[]`/`Long[]`/`Double[]`/`Float[]`/`BigDecimal[]`/`String[]`/`Character[]`/`Boolean[]` 参数。
- ⚠️ ConversionService 会覆盖内建转换链，替换 `conversionService` 后需回归验证集合/Map 参数绑定。

## 2.9 链路追踪与过滤器链异常处理

- `TraceFilter`：从请求头 `TRACE_ID` 取 traceId（无则生成），写入 MDC 键 `traceId`，logback pattern 加 `%X{traceId}` 即可输出。已知不足（评审报告 P2-5，未修）：随机数生成有撞号概率、MDC 不在请求结束清理、starter 无装配入口——跨请求串号与残留风险自负。
- `ExceptionFilter`：捕获过滤器链上 RestExceptionAdvice 管不到的异常，返回 500 + JSON。它把根因放进 `ThreadContext("routeCause")` 目前无消费方（评审报告 P2-6，未修），依赖 `ThreadLocalCleanupListener` 在请求结束时清理。
- `ThreadLocalCleanupListener`：请求开始/结束时清理 ThreadContext，starter 已自动注册。

---

# 三 配置项一览（copilot-web 自身读取）

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `copilot.mvc.cors.enabled` | **true** | 控制 `RestUtils` 输出是否附加 `CORS.allowAll()` 响应头。类加载时从 application.yml 读一次，运行期不可改。Spring Cloud 网关两端都开跨域会报 CORS error，网关链路请显式设为 false |

starter 侧另有 `copilot.mvc.rest-exception-advice-enabled`（默认 true）、`copilot.filter.xss-enabled`（默认 false）、`copilot.filter.repeated-read`（存在即启用）、`copilot.mvc.cors.*`（Spring MVC 标准跨域配置）等键，归 starter 文档管辖。

---

# 四 依赖说明

| 模块 | 说明 |
| --- | --- |
| `commons-lang` | 基础工具、异常体系、Result、DateUtils/EnumUtils |
| `copilot-validation` | Bean 校验异常与 ErrorMessage |
| `copilot-json` | Jackson 序列化（writeJson） |
| `commons-spring` | ApplicationContextHolder / I18N / LocaleContextHolder（经 copilot-validation 传递引入） |
| `sentinel-core` | Tracer 统计（compile 依赖，未用 Sentinel 时相关代码只是不命中分支） |
| Spring Web / WebMVC / Context | MVC 框架 |
| Jakarta Servlet API | 模块内覆盖为 6.0.0（Spring 6.1.5 按 Servlet 6 编译；父 pom 管理的 5.0.0 与本模块不匹配），provided 范围不打进 jar |

---

# 五 注意事项

1. **裸依赖 copilot-web ≠ 能力生效**。`XssHttpServletRequestWrapper`、`HttpServletRequestRepeatedReadFilter`、`ExceptionFilter`、`TraceFilter`、`RequestHeaderModifiableFilter` 在只有本模块依赖时都不会进过滤器链，本文宣传的能力必须装配后才存在（评审报告 P0-3 的核实结论：装配入口在 copilot-starter 仓库，不在本模块）。
2. **Sentinel 整合**：存在 `restBlockExceptionHandler` Bean 时异常会计入 Sentinel 统计，但响应仍由本 Advice 返回（不会"重新抛出让过滤器处理"，注释与实现的矛盾见评审报告 P2-1）。
3. **跨域**：网关层和本服务不要同时开；`allowAll()` 输出 `*`，与 `Allow-Credentials` 互斥（带 Cookie 的跨域需求请用 `CORS.builder().allowedOrigins(具体域名)` 或走 MVC 的 CorsRegistry，评审报告 P2-7 的 allowCredentials 分支未实现）。
4. **i18n 装配**：`MessageHelper` 按类型从容器取 MessageSource，Bean 名必须注册为 `messageSource`；取不到时降级返回 null/默认消息并 WARN 一次，不会抛异常打断请求。
5. **日期参数带 @DateTimeFormat 的宽容度变化**（2026-09-17）：输入与 pattern 不符现在抛异常而非悄悄返回 null/错值；需要旧的宽容行为就不要写注解。

---

# 六 使用示例

## 6.1 业务异常处理

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

异常将被 `RestExceptionAdvice` 捕获并返回统一 JSON 格式（需按"能力总览"完成装配）。

## 6.2 在 Filter 中输出 JSON

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

## 6.3 文件下载

```java
@GetMapping("/export")
public void export(HttpServletResponse response) {
    File excel = reportService.generateExcel();
    RestUtils.download(excel, "报表.xlsx");
}
```

## 6.4 国际化异常

```java
// messages_zh_CN.properties
error.user.notfound=用户 {0} 不存在

// messages_en_US.properties
error.user.notfound=User {0} not found

// 业务代码
throw new LocalizedException("5001", "error.user.notfound", List.of(userId), "User not found");
```
