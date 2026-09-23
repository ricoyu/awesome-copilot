# commons-spring

Spring 环境下的支撑工具层（包名 `com.awesomecopilot.common.spring`），依赖 commons-lang。提供 Servlet 读写、国际化、事务时机回调、Bean 拷贝、SpEL、HTTP 客户端封装等能力。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| Servlet | `ServletUtils` | 静态方法读写 request/response/header/参数/属性，取 IP、判断 Ajax 请求 |
| 国际化 | `I18N` / `LocaleContextHolder` / `LocaleUtils` / `LocaleConfigurerFilter` | 非注入场景取 i18n 文案；线程级 Locale/时区上下文 + Filter 自动设置 |
| 事务 | `TransactionEvents` | 编程式拿到事务各时机回调（beforeCommit/afterCommit/afterCompletion…），可挂起/恢复/flush |
| Bean | `BeanUtils` | copyProperties 多重载（忽略 null、指定属性、类型转换） |
| 容器 | `ApplicationContextHolder` / `SpringBeanUtil` | 静态 getBean/getBeans/getProperty，脱离注入取 Bean |
| SpEL | `SpElUtils` | `SpElUtils.parse("#root.brandId", obj)` 一行求值 |
| HTTP | `Requests` / `FormRequestBuilder` / `JsonRequestBuilder` | 基于 RestTemplate 的 fluent 请求构建，带 OAuth2 basic/bearer 认证与 onError 回调 |
| 注解 | `@SmartLogger` / `@PostInitialize` / `@AutoPage` | 方法入参日志切面；容器就绪后异步执行初始化；返回结果自动填充分页 |
| AOP | `SmartLoggerAspect` / `PageResultAspect` | 上面两个注解的实现 |
| 异常 | `LocalizedException`（在 copilot-web）配合使用 | 带国际化消息模板的异常 |

# 二 事务时机回调（TransactionEvents）

不依赖 `@TransactionalEventListener`，在任何有事务同步上下文的地方都能注册回调：

```java
TransactionEvents.instance()
    .afterCommit(() -> messageProducer.send(afterCommitOnly))  // 事务提交成功后才发消息
    .afterCompletion(status -> cleanup());                      // 提交/回滚都会走

// 也可在业务方法内挂起当前事务做隔离操作
TransactionEvents instance = TransactionEvents.instance();
instance.suspend(); ... instance.resume();
```

# 三 @PostInitialize：容器就绪后再初始化

`@PostConstruct` 执行时容器还没完全就绪（其他 Bean 未必齐）。标注 `@PostInitialize` 的方法在所有单例实例化完成后（SmartInitializingSingleton 时机）由独立线程池**并行**执行，缩短启动时间：

```java
@PostInitialize                  // 全部单例就绪后异步执行
public void warmUp() { ... }
```

- 注意：**方法内不要长时间阻塞**——多个 @PostInitialize 方法共享一个线程池，一个方法长时间不返回会占住线程，后面的方法要排队等待。
- 普通版 `PostInitializeProcessor`；需要分组、按序、延迟执行时用 `PostInitializeGroupOrderedBeanProcessor`（支持 delay）。
- `@AutoPage`：`PageResultAspect` 原本拦截 `@PostMapping` 方法填充分页信息；Controller 实现 Feign 接口且不带 @PostMapping 注解时拦不到，用 `@AutoPage` 显式标注补上。

# 四 HTTP 请求构建（RestTemplate 版）

`copilot-networking` 是 HttpClient 版，这里是 RestTemplate 版，适合已引入 spring-web 的项目：

```java
// 快捷入口起 builder（底层走 RestTemplate，request() 无参版用内部默认 RestTemplate，
// 也可 request(restTemplate) 传入自己的实例）
CustomRule rule = Requests.post("https://api.x.com/rules")
        .bearerAuth(token)
        .body(jsonString)                 // JsonRequestBuilder 的 body 接收 JSON 字符串
        .responseType(CustomRule.class)   // 泛型结果改用 typeReference(...)
        .onError(ex -> log.error("调用失败", ex))
        .request();

String resp = Requests.get(url).request();

// 表单 / OAuth2 取 token
Requests.form(authUrl).grantType(GrantType.CLIENT_CREDENTIALS)
        .userpwd(clientId, secret).scope(Scope.READ).request();

// 透传：把当前请求原样转发到目标地址（网关/代理场景）
T result = Requests.transmit(httpServletRequest, "http://backend/api", Resp.class);
```

# 五 注意事项

- `LocaleContextHolder` 是**自研 ThreadLocal**（不是 Spring 同名类），必须配合 `LocaleConfigurerFilter`（或手动 set/clear）使用，线程池场景记得传递+清理。
- `Requests.fillSchema` 用于给相对路径补 scheme/host。
- `@SmartLogger` 会打印全部入参，含敏感字段的方法慎用。
