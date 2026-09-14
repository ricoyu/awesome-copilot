# 一 全局ID生成

雪花算法ID生成器 `SnowflakeId`。`workerId` / `datacenterId` 按以下优先级解析（从高到低）：

1. JVM 系统属性：`-Dcopilot.snowflake.worker-id=7`（排查问题时临时覆盖一切）
2. 环境变量：`COPILOT_SNOWFLAKE_WORKER_ID`（容器部署正解：K8s StatefulSet 的 pod 序号注入这里，每个实例天然唯一）
3. classpath / 工作目录下的 `application*.properties`：

```properties
copilot.snowflake.worker-id=1
copilot.snowflake.datacenter-id=1
```

4. classpath / 工作目录下的 `application*.yml`：

```yaml
copilot:
  snowflake:
    worker-id: 1
    datacenter-id: 1
```

5. 都没配置时才按本机 IP+进程号自动推导（会打 WARN 日志），`datacenter-id` 缺省为 1。自动推导只适合本地开发：workerId 只有 0~31 共 32 个槽，跨机器不保证不重号，生产环境务必走上面的配置。同 JVM 内多个自动推导实例撞同一编号时，后构造的会自动顺延到空闲编号，不会生成重复 ID。

注意键名是 `worker-id` / `datacenter-id`（中划线），不是 `workerId`。

# 二 读取配置文件

commons-lang 提供两套配置读取入口，统一在 `com.awesomecopilot.common.lang.resource` 包：

| 类 | 读什么 | 推荐入口 |
|----|--------|----------|
| `PropertyReader` | `xxx.properties` | `new PropertyReader("elastic")` |
| `YamlReader` | `xxx.yml` / `xxx.yaml` | 不建议直接用，用下面的 |
| `YamlProfileReaders` | `xxx.yml` + `xxx-<profile>.yml` | `YamlProfileReaders.instance("application")` |
| `YamlOps` | 接口 | 接收 `YamlReader` / `YamlProfileReaders` 两种实现的统一变量类型 |

## 文件查找优先级（两套都一样）

同一个资源名会在三个位置各找一遍，**优先级从高到低**：

1. 工作目录下的 `config/` 目录（`./config/xxx.properties`）
2. 工作目录（`./xxx.properties`）
3. classpath 根（jar 里打包的那份）

YAML 每个位置先找 `.yml` 再回退 `.yaml`。用途：jar 里带默认值，部署时在应用目录放同名文件覆盖，不用重新打包。

## PropertyReader 用法

```java
PropertyReader reader = new PropertyReader("elastic");   // 读 elastic.properties
if (reader.resourceExists()) {                            // 三个位置都没找到时是 false
    String host = reader.getString("elastic.host");       // 找不到返回 null
    int port = reader.getInt("elastic.port", 9200);       // 带默认值
    boolean on = reader.getBoolean("elastic.auth", false);
    List<String> hosts = reader.getStrList("elastic.hosts");          // 逗号分隔转List
    Set<String> tags = reader.getStringAsSet("scan.tags");            // 逗号分隔转Set
    Map<String, String> accounts = reader.getMap("company.accounts"); // "RBK:7125, CMT:" 转Map
    LocalDate day = reader.getLocalDate("stat.date");
}
```

注意点：

- **用带资源名的构造器**。无参构造器 `new PropertyReader()` 不加载任何文件，读出来全是默认值，是个空壳。
- `getInt(property)`（不带默认值那种）找不到或不是数字时返回 **-1**——如果 -1 在你的配置里是合法值就区分不开了，建议一律用带默认值的重载。
- 资源名可以带包路径（如 `config/jdbc`），`ResourceBundle` 还支持语言后缀文件（`elastic_zh_CN.properties` 优先于 `elastic.properties`）。
- 编码：classpath 那份由 `ResourceBundle.getBundle` 读取（JDK 9+ 按 UTF-8 解码，非法 UTF-8 字节才回退 ISO-8859-1），工作目录/`config/` 那两份由 `new PropertyResourceBundle(FileInputStream)` 读取（JDK 21 同样是 UTF-8 优先）。实测同一份 UTF-8 中文值两个位置读出的字符完全一致（`你好世界`），**配置文件用 UTF-8 保存即可**；Windows 控制台打印出的"乱码"多是终端 GBK 显示问题，不是配置读错。
- 实例每次 `new` 都重新读盘，建议创建一次长期复用。

## YAML 用法（推荐入口 YamlProfileReaders）

```java
YamlOps yaml = YamlProfileReaders.instance("application");   // 读 application.yml
if (yaml.exists()) {
    Integer timeout = yaml.getInt("spring.redis.timeout");    // 找不到返回 null
    String host = yaml.getString("spring.redis.host", "127.0.0.1");
    Boolean enable = yaml.getBoolean("copilot.trace.enabled", false);
}
```

行为与坑：

- **支持 Spring 风格的 profile**：主文件里有 `spring.profiles.active: prod` 时，会额外加载 `application-prod.yml`，取值时 profile 文件优先、主文件兜底。profile 文件同样按"config目录→工作目录→classpath"找。
- **例外：`getBoolean` 没有主文件兜底**——profile 文件存在时它只查 profile 文件，profile 里没配就直接返回 null，不会回落到主文件（`getInt`/`getString` 都会回落）。布尔开关建议写在同一个文件里，或改用 `getBoolean(path, 默认值)` 显式给默认。
- **进程级缓存**：同一资源名只读盘解析一次，之后 `instance()` 返回共享实例（构造后只读，线程安全）。改了磁盘上的 yml 想让它生效，要调 `YamlProfileReaders.clearCache()` 再重新 `instance()`——热加载和单元测试里最常踩的就是这个。
- **key 两种写法都能取**：`getInt("spring.redis.timeout")` 既能命中扁平写法（`spring.redis.timeout: 5000` 整体一个 key），也能命中嵌套写法（逐层缩进），内部先整体查、再按 `.` 拆开逐层下钻。
- 取值返回**包装类型**，`null` 表示没有这个配置——和 PropertyReader 用 -1/false 当哨兵的风格相反，判空请以 `null` 为准。

## 两套怎么选

properties 和 yml 各用各的没有强制约定（`elastic.properties`、`redis.properties` 走 PropertyReader，Spring 项目的 `application.yml` 走 YamlProfileReaders）。需要"同一组键两种格式都能配"时，参考 `SnowflakeId` 无参构造器的做法：先 `new PropertyReader("application")`，`resourceExists()` 为 false 再退回 `YamlProfileReaders.instance("application")`。
