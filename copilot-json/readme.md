# copilot-json

Jackson 统一封装（包名 `com.awesomecopilot.json`）+ JsonPath 封装。全框架的 JSON 序列化入口，测试里大量使用的 `toJson` / `toPrettyJson` 就出自这里。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 入口 | `JacksonUtils` | 静态门面：对象↔JSON↔Map↔字节 全向转换 |
| 配置 | `ObjectMapperFactory` / `ObjectMapperDecorator` | 统一创建配置好的 ObjectMapper；装饰器开枚举/日期/货币等特殊行为 |
| 反序列化 | `deserializer/` 包 | 日期串→Date/LocalDate/LocalDateTime、毫秒/秒时间戳→LocalDateTime、枚举容错、XSS 字符串清洗、Mongo ObjectId、Spring Page |
| 序列化 | `serializer/` 包 | LocalDate/LocalDateTime/LocalTime（可选 EPOCH 毫秒）、double/float 精度控制、BigDecimal→货币格式、Map key 排序、Result 空 page 省略、全局 HTML 转义 |
| 注解 | `@Precision` / `@EnumI18N` / `@UnescapeHtml` | 字段级序列化精度；枚举国际化输出；类级豁免 HTML 转义 |
| JsonPath | `JsonPathUtils` / `JsonContext` / `JsonPathCache` | parse 一次读多次（DocumentContext 全套重载）、类型化读取、内嵌 JSON 穿透、无锁路径编译缓存 |
| 工具 | `JsonNodeUtils` | JsonNode 安全取值：readStr/readInt/readEnum/readList（节点缺失返回 null 而不是抛异常） |

# 二 JacksonUtils 常用 API

```java
String json = JacksonUtils.toJson(obj);
String pretty = JacksonUtils.toPrettyJson(obj);
byte[] bytes = JacksonUtils.toBytes(obj);

User user = JacksonUtils.toObject(json, User.class);       // 也有 byte[] 重载
List<User> users = JacksonUtils.toList(json, User.class);
Map<String, Object> map = JacksonUtils.toMap(json);        // toGenericMap 可指定 K/V
User u2 = JacksonUtils.mapToPojo(map, User.class);
Map<String, Object> m2 = JacksonUtils.pojoToMap(user);

// 其他：toJsonNode/parseObject/readNode 拿 JsonNode 树；isValidJson 校验；
// objectMapper() 拿共享实例；addMixIn 混入定制；formatJsonString 美化
```

# 三 ObjectMapper 工厂与装饰器

```java
// 统一入口拿一个全局配置好的 mapper（优先复用 Spring 容器里 bean 名为 objectMapper 的实例）
ObjectMapper mapper = ObjectMapperFactory.createOrFromBeanFactory();

// 需要特殊行为时用装饰器（先 set 开关，再 decorate 产出新 mapper）
ObjectMapper customized = new ObjectMapperDecorator()
        .decorate(mapper);
// 可用开关：setEpochBased(日期按 EPOCH 毫秒读写)、setIgnorePropertiesCase(属性名大小写不敏感)、
// setFailOnUnknownProperties(未知属性是否抛异常)、setEnumProperties(参与定制反序列化的枚举属性)
```

# 四 序列化器行为速查

| 场景 | 类 / 注解 | 行为 |
|------|-----------|------|
| `LocalDate` 输出 | `LocalDateSerializer` | 默认 `yyyy-MM-dd`；开 EPOCH 后输出当前时区（东八区）毫秒数 |
| `LocalDateTime` 输出 | `LocalDateTimeSerializer` | `yyyy-MM-dd HH:mm:ss` |
| double/float 精度 | `@Precision(scale = 2)` | 字段级控制小数位（`DoubleContextualSerializer`/`FloatContextualSerializer`） |
| BigDecimal 货币化 | `MoneySerializer` | 输出 `12,333.23` 形式 |
| Map 按 key 排序 | `MapSerializer` + `KeyComparator` | 输出稳定顺序（签名/对账场景常用） |
| Result 空 page | `ResultSerializer` | `page == null` 时 JSON 里不出现 page 字段 |
| 全局 HTML 转义 | `HtmlEscapeStringSerializer` | 所有字符串输出转义；某类不想转义标 `@UnescapeHtml` |
| XSS 入参清洗 | `XssStringJsonDeserializer` | 读入字符串时先清洗 |
| 枚举国际化 | `@EnumI18N` | 枚举输出按 MessageSource 取国际化名 |

# 五 JsonPath（2026-09-26 改造后）

## 5.1 收 String 的旧入口（每次调用完整解析一遍）

```java
import static com.awesomecopilot.json.jsonpath.JsonPathUtils.*;

String name = readNode(json, "$.store.book[0].title");           // 路径未命中返回 null（SUPPRESS_EXCEPTIONS, 不抛）
String safe = readNodeIfExists(json, "$.store.book[0].title");   // 未命中或空集合都返回 null
Double price = readNode(json, "$.amount", Double.class);         // 带类型转换
List<String> titles = readListNode(json, "$..title");
List<User> users = readListNode(json, "$.users", User.class);
Object single = readNodeSingleValue(json, "$.store.book[*].isbn"); // 集合结果取第一个
boolean has = ifExists(json, "$.store.book[2].price");
```

## 5.2 解析一次、读多次（同一份报文读 2 个以上字段必须走这组）

```java
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.json.jsonpath.context.DocumentContext;

DocumentContext ctx = JsonPathUtils.parse(json);   // 整篇文档只解析这一次
String v1 = JsonPathUtils.readNode(ctx, "$.a.b");
Money m2 = JsonPathUtils.readNode(ctx, "$.amount", Money.class);
boolean has = JsonPathUtils.ifExists(ctx, "$.x");
// readNode / readNodeIfExists / readListNode / readNodeSingleValue / ifExists 全套都有 ctx 重载,
// 参数类型是 com.jayway.jsonpath.DocumentContext, 官方 JsonPath.parse() 的产物也能直接传
```

| 场景（实测, 本机与评审机比率一致、绝对值 ±40% 浮动） | 收 String 入口 | ctx 重载 |
|---|---|---|
| 单次读取 | 40KB 报文 ≈466µs、2MB 报文 ≈41ms（大头是解析, 与报文大小成正比） | ≈0.2-1µs（只剩路径求值, 与报文大小无关） |
| 同一份 40KB 报文读 5 个字段 | ≈2.4ms（解析 5 遍） | ≈0.49ms（解析 1 遍, 快 5 倍） |
| 32 线程并发读 | ≈1.2 万 ops/s | ≈2,000 万 ops/s |

## 5.3 行为与坑清单

- **内嵌 JSON 自动展开**：`"billJson":"{\"FBillNo\":...}"` 这类"整段 JSON 被序列化成字符串"的值，`parse(json)` 默认会做树级展开，`$.billJson.FBillNo` 可直接穿透读到；取整棵子树 `readNode(json,"$.billJson")` 拿到的是展开后的 Map 而非原始串。报文确定没有内嵌 JSON 时用 `JsonPathUtils.parse(json, false)` 跳过整树遍历（2MB 文档实测解析从 33.8ms 降到 18.2ms），此时 `$.billJson` 保持原始字符串。
- **路径未命中不抛异常**：配置了 `SUPPRESS_EXCEPTIONS`，确定路径未命中返回 null、不确定路径（通配/过滤）未命中返回空 List；只有 path 表达式本身语法错误才走 ERROR 日志（内部仍返回 null/空集合）。判断"不存在"用 `== null` 或 `ifExists`。
- **类型转换语义**：带 `Class` 的目标类型走两跳——先 `objectMapper.convertValue`，标量再按 ValueHandlerFactory 名单二次转换（Integer/Long/Double/Float/BigDecimal/Boolean/String/Date/LocalDateTime/LocalDate/LocalTime/Short/List）。目标类型是 Map/List/POJO 时直接返回 convertValue 的副本。⚠️ 2026-09-26 前的旧版对 Map/List/POJO 目标会因空指针被捕获而**永远返回 null**，已修——业务里若拿 `readNode(json,path,Map.class)==null` 当"路径不存在"判过分支，升级后该分支行为会变（有值时不再进 null 侧）。
- **函数路径**：`sum()/max()/min()/append()` 等含 `(` 的表达式每次求值都会重新 compile（不进缓存）——json-path 3.0.0 的编译产物在函数求值期写共享字段，跨文档并发复用会串数据（实测 48 万次串 10.4 万次且无异常）。属性/通配/过滤器路径无此问题。`append()` 会直接改文档树，并发共用 ctx 时禁用。
- **ctx 生命周期**：读出的 Map/List（不带 Class 的 readNode）就是树本身不是副本，别改；大文档的 ctx 用完即弃，别塞进字段长期持有（等于扣住整棵树）。
- **`readNode(null, path)` 字面量**：第一参写死 null 会编译报"引用不明确"（新旧重载都匹配），写 `(String) null` 或先赋给变量。
- path 首尾空白会被自动 trim（2026-09-26 起两入口统一）。
- 带 `(` 的路径（函数表达式 `sum()` 及过滤器 `[?(@.id==1)]`）不进编译缓存、每次求值前重新 compile（约 3-7µs）；纯属性/通配路径才走缓存。高频循环里避免每次拼接动态 filter 字符串，能固定则固定。

# 六 注意事项

- 业务代码不要再各自 `new ObjectMapper()`，统一走 `JacksonUtils` / `ObjectMapperFactory`，否则日期、枚举、精度行为不一致。
- 时间戳↔LocalDateTime 有两套反序列化器：`MillisToLocalDateTimeDeserializer`（13 位毫秒）与 `SecondsToLocalDateTimeDeserializer`（10 位秒），按上游给的单位选。
- `DateStr2LongDeserializer` 默认按东八区解析日期字符串。
