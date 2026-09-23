# commons-cloud

Spring Cloud / 网关场景的公共组件（包名 `com.awesomecopilot.cloud`）。当前核心是 Alibaba Sentinel 流控异常的 REST 风格统一处理。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 网关限流 | `cloud.sentinel.handler.GatewayBlockRequestHandler` | 实现 Sentinel `BlockRequestHandler`，Spring Cloud Gateway（webflux）被限流/熔断时统一处理 `handleBlockRequest`，返回 REST 风格响应 |
| Web 限流 | `cloud.sentinel.utils.BlockExceptionUtils` | `responseBody(Throwable)` 把 `BlockException`（FlowException/DegradeException/ParamFlowException/SystemBlockException…）翻译成统一响应体字符串，供 webmvc 侧异常处理器复用 |

# 二 限流异常处理

Sentinel 官方的 `RestBlockExceptionHandler` 提供 REST 风格的统一流控异常处理；本模块补齐两块：

1. **网关侧**：把 `GatewayBlockRequestHandler` 注册进 Sentinel 网关适配器的 fallback 配置，被限流的请求即返回本项目统一的 Result JSON（而不是默认文本）。
2. **服务侧**：自定义 `@ExceptionHandler(BlockException.class)` 或 Filter 中调用 `BlockExceptionUtils.responseBody(e)` 生成响应体，避免每个服务重复写异常到 JSON 的转换。

```yaml
spring:
  cloud:
    sentinel:
      scg:
        fallback:
          mode: response
          response-status: 429
```

# 三 依赖

`sentinel-core` / `sentinel-spring-cloud-gateway-adapter` / `sentinel-spring-webmvc-adapter`（版本走根 pom dependencyManagement）。
