# copilot-cache 从 Nacos 配置中心读取 Redis 配置指南

## 配置加载优先级

`JedisOperationFactory.create()` 按以下优先级加载 Redis 配置:

1. **Spring Environment**(支持 Nacos 配置中心) — 先读自定义 `redis.*`, 再回退 `spring.data.redis.*` / `spring.redis.*`
2. **系统属性**(`-Dredis.host=...`)
3. **环境变量**(`redis.host` -> `REDIS_HOST`)
4. **本地配置文件**(`classpath:redis.properties` / 工作目录 / 工作目录 config 目录)
5. **Spring Boot yml 回退**(无 `redis.properties` 且未注册 Spring 环境时, 读 `application.yml` 的 `spring.redis.*`)
6. **默认值**(`localhost:6379`)

## 核心说明

`copilot-cache` **不引入 Nacos SDK / Spring 运行依赖**(`spring-context` 为 `provided` 作用域), 而是通过反射读取 Spring `Environment`。Nacos 配置由 Spring Cloud Alibaba 在应用侧注入到 `Environment` 中。

**在 Spring Boot 应用中, 无需任何手动配置或调用**——`RedisConfigInitializer` 通过 `META-INF/spring.factories` 在应用启动阶段(早于任何 Bean 实例化)自动把 Spring `Environment` 注册到 `RedisConfigReader`。业务代码里直接 `JedisUtils.set(...)` / `JedisUtils.get(...)` 即可自动读到 Nacos 配置。

## 接入方式

### 方式1: Spring Boot 零配置(推荐)

**什么都不用做。** 只要应用依赖了 copilot-cache, 启动时自动装配就绪:

```java
// 业务代码中直接使用, 自动读取 Nacos 配置
JedisUtils.set("key", "value");
String value = JedisUtils.get("key");
```

### 方式2: 非 Spring Boot 的 Spring 应用手动注册

仅当使用**原生 Spring(非 Spring Boot)**时, `spring.factories` 的自动装配不会生效, 需要手动注册:

```java
import com.awesomecopilot.cache.factory.JedisOperationFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class MyApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = /* 创建/刷新 Spring 上下文 */;
        JedisOperationFactory.registerContext(context);
    }
}
```

## Nacos 配置示例

### 写法 A: 自定义 `redis.*` key(与现有 redis.properties 一致)

```yaml
redis:
  host: 192.168.1.100
  port: 6379
  password: your_password
  db: 0
  connectionTimeout: 5000
  socketTimeout: 1000
```

哨兵模式:

```yaml
redis:
  sentinels: 192.168.1.101:26379,192.168.1.102:26379,192.168.1.103:26379
  maserName: mymaster
  password: your_password
```

集群模式:

```yaml
redis:
  clusters: 192.168.1.101:6379,192.168.1.101:6380,192.168.1.102:6379
  password: your_password
```

### 写法 B: Spring Boot 标准 key(spring.data.redis.* / spring.redis.*)

```yaml
spring:
  data:
    redis:
      host: 192.168.1.100
      port: 6379
      password: your_password
      database: 0
```

若同时配置了 `redis.*` 与 `spring.data.redis.*`, **以 `redis.*` 为准**(自定义 key 优先级更高)。

## 配置项映射

| redis.properties key | Spring Boot 标准 key |
|---|---|
| redis.host | spring.data.redis.host / spring.redis.host |
| redis.port | spring.data.redis.port / spring.redis.port |
| redis.password | spring.data.redis.password / spring.redis.password |
| redis.db | spring.data.redis.database / spring.redis.database |
| redis.connectionTimeout / redis.socketTimeout / redis.timeout | spring.data.redis.timeout / spring.redis.timeout |
| redis.sentinels | spring.data.redis.sentinel.nodes / spring.redis.sentinel.nodes |
| redis.maserName | spring.data.redis.sentinel.master / spring.redis.sentinel.master |
| redis.clusters | spring.data.redis.cluster.nodes / spring.redis.cluster.nodes |
| redis.maxTotal | spring.data.redis.jedis.pool.max-active / spring.redis.jedis.pool.max-active |
| redis.maxIdle | spring.data.redis.jedis.pool.max-idle / spring.redis.jedis.pool.max-idle |
| redis.minIdle | spring.data.redis.jedis.pool.min-idle / spring.redis.jedis.pool.min-idle |

> 未列入的 key(如 `redis.debug`、`redis.warmUp`、`redis.testOnBorrow`、`redis.cluster.maxAttempts` 等)不映射 Spring Boot 标准 key, 仍按 `redis.*` 读取。

## 常见问题

### Q1: 如何验证 Nacos 配置是否生效?

查看启动日志, 会输出:

```
已注册 Spring Environment, 将优先从 Spring Environment(Nacos) 读取 Redis 配置
从 Spring Environment 读取配置: redis.host=192.168.1.100
```

### Q2: 配置动态刷新?

`JedisUtils` 的 `JedisOperations` 在类加载时即初始化, 不支持运行时动态刷新。如需变更配置, 需重启应用。

### Q3: 无 Spring 环境会发生什么?

工厂会按原逻辑降级: `redis.properties` -> `application.yml` -> 默认 `localhost:6379`, 与本次改动之前的行为完全一致。
