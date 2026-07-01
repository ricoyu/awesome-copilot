# Elasticsearch 8.x Nacos 配置指南

## 配置加载优先级

ElasticsearchClientFactory 支持多种配置方式,按以下优先级加载:

1. **Spring Environment** (支持 Nacos 配置中心)
2. **系统属性** (`-Delastic.rest.hosts=...`)
3. **环境变量** (`ELASTIC_REST_HOSTS`)
4. **本地配置文件** (`classpath:elastic.properties`)

## Spring Boot + Nacos 集成

### 重要说明

由于 `ElasticsearchClientFactory` 是**静态工厂类**,不在 Spring 容器中,**无法自动从 Nacos 读取配置**。

必须通过以下**任一方式**让工厂类能够访问 Spring/Nacos 配置:

---

### 方式1: 注册 Spring ApplicationContext(推荐)

#### 步骤1: 创建配置类

在 Spring Boot 应用中创建如下配置类:

```java
import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MyApplication {
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MyApplication.class, args);
        
        // 注册 Spring Context,使工厂能够从 Nacos 读取配置
        ElasticsearchClientFactory.registerContext(context);
    }
}
```

或者使用 `@PostConstruct`:

```java
import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ElasticSearchConfigInitializer {
    
    @Autowired
    private ConfigurableApplicationContext context;
    
    @PostConstruct
    public void init() {
        // 注册 Spring Context
        ElasticsearchClientFactory.registerContext(context);
        log.info("已注册 ElasticsearchClientFactory 的 Spring Context");
    }
}
```

#### 步骤2: 在 Nacos 中配置

按照上面的配置示例,在 Nacos 中添加配置即可。工厂类会自动从注册的 Spring Context 中读取 Nacos 配置。

---

### 方式2: 直接初始化客户端(最灵活)

不依赖静态工厂的配置加载机制,直接在 Spring Bean 中初始化:

```java
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.search8x.config.ElasticsearchConfig;
import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {
    
    @Value("${elastic.rest.hosts}")
    private String hosts;
    
    @Value("${elastic.username:}")
    private String username;
    
    @Value("${elastic.password:}")
    private String password;
    
    @Bean
    @RefreshScope  // 支持 Nacos 动态刷新
    public ElasticsearchClient elasticsearchClient() {
        ElasticsearchConfig config = new ElasticsearchConfig();
        config.setRestHosts(hosts);
        config.setUsername(username);
        config.setPassword(password);
        
        return ElasticsearchClientFactory.initClient(config);
    }
}
```

这种方式的优势:
- ✅ 完全由 Spring 管理,自动支持 Nacos
- ✅ 支持 `@RefreshScope` 动态刷新
- ✅ 可以使用 `@ConfigurationProperties` 批量注入
- ✅ 更符合 Spring Boot 最佳实践

---

### 方式3: 通过环境变量传递

在微服务环境中,可以通过环境变量传递配置:

```bash
# Docker/Kubernetes 环境变量
export ELASTIC_REST_HOSTS=http://es-node1:9200,http://es-node2:9200
export ELASTIC_USERNAME=elastic
export ELASTIC_PASSWORD=changeme

# 启动应用
java -jar my-application.jar
```

工厂类会自动读取这些环境变量(优先级高于本地配置文件)。

---

### 方式4: 系统属性

通过 JVM 参数传递:

```bash
java -Delastic.rest.hosts=http://es-node1:9200 \
     -Delastic.username=elastic \
     -Delastic.password=changeme \
     -jar my-application.jar
```

---

## 配置加载优先级

工厂类按以下优先级加载配置(从高到低):

1. **Spring Environment**(需要先调用 `registerContext()`)
2. **系统属性**(`-D` 参数)
3. **环境变量**(`ELASTIC_REST_HOSTS` 等)
4. **本地配置文件**(`classpath:elastic.properties`)

**注意**: 如果不使用方式1注册 Spring Context,即使添加了 Nacos 依赖,工厂类也**无法自动读取 Nacos 配置**!

配置内容:

```yaml
elastic:
  rest:
    hosts: http://192.168.1.100:9200,http://192.168.1.101:9200
  username: elastic
  password: your_password_here
```

### 步骤2: 在 bootstrap.yml 中配置 Nacos

```yaml
spring:
  application:
    name: copilot-search-8x
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        # 可选:启用动态刷新
        refresh-enabled: true
```

## 其他配置方式

### 方式2: 系统属性

启动时通过 `-D` 参数传入:

```bash
java -jar \
  -Delastic.rest.hosts=http://192.168.1.100:9200 \
  -Delastic.username=elastic \
  -Delastic.password=your_password \
  your-application.jar
```

### 方式3: 环境变量

设置环境变量(注意将`.`转换为`_`并转大写):

```bash
export ELASTIC_REST_HOSTS=http://192.168.1.100:9200
export ELASTIC_USERNAME=elastic
export ELASTIC_PASSWORD=your_password
```

### 方式4: 本地配置文件(向后兼容)

在 `src/main/resources/elastic.properties` 中配置:

```properties
# ES 集群地址,多个用逗号分隔
elastic.rest.hosts=http://192.168.1.100:9200,http://192.168.1.101:9200

# 认证信息(可选)
elastic.username=elastic
elastic.password=your_password
```

**注意**: 如果同时使用了 Spring + Nacos 但没有调用 `registerContext()`,工厂类会降级到读取本地配置文件!

## 微服务环境最佳实践

### 推荐方案: Nacos + Profile

1. **开发环境**: 使用本地配置文件
2. **测试环境**: 使用 Nacos 配置中心的 `dev` profile
3. **生产环境**: 使用 Nacos 配置中心的 `prod` profile

Nacos 配置示例:

**copilot-search-8x-dev.yaml** (测试环境)
```yaml
elastic:
  rest:
    hosts: http://test-es-cluster:9200
  username: elastic
  password: test_password
```

**copilot-search-8x-prod.yaml** (生产环境)
```yaml
elastic:
  rest:
    hosts: http://prod-es-node1:9200,http://prod-es-node2:9200,http://prod-es-node3:9200
  username: elastic
  password: ${ES_PASSWORD:}  # 可通过环境变量覆盖密码
```

### 安全建议

1. **不要在代码仓库中存储密码**,使用以下方式之一:
   - Nacos 配置加密
   - 环境变量注入密码
   - Kubernetes Secrets

2. **生产环境建议使用 HTTPS**:
   ```yaml
   elastic:
     rest:
       hosts: https://es-cluster.example.com:9200
   ```

## 配置项说明

| 配置项 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `elastic.rest.hosts` | ES 集群地址,多个用逗号分隔 | `http://localhost:9200` | 否 |
| `elastic.username` | ES 用户名(可选) | 空 | 否 |
| `elastic.password` | ES 密码(可选) | 空 | 否 |

## 常见问题

### Q1: 如何验证配置是否生效?

查看应用启动日志,会输出:

```
初始化 ES 8.x 客户端, hosts=http://192.168.1.100:9200, username=elastic
ES 8.x 客户端初始化成功
```

### Q2: 如何实现配置动态刷新?

目前 `ElasticsearchClient` 采用单例模式,不支持运行时动态刷新。如需刷新,需要重启应用。

未来可以考虑:
1. 实现配置监听器
2. 重建 `ElasticsearchClient` 实例
3. 使用连接池管理

### Q3: 多租户场景如何配置?

可以为不同租户创建不同的 Client:

```java
// 租户A的ES客户端
ElasticsearchClient clientA = createClientForTenant(tenantAConfig);

// 租户B的ES客户端  
ElasticsearchClient clientB = createClientForTenant(tenantBConfig);
```

## 迁移检查清单

- [ ] 确认 Nacos 配置已创建
- [ ] 确认应用能连接到 Nacos Server
- [ ] 验证配置项格式正确
- [ ] 检查应用启动日志
- [ ] 测试 ES 连接是否正常

## 技术支持

如有问题,请联系: rico.yu520@gmail.com
