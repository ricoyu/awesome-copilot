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
| JsonPath | `JsonPathUtils` / `JsonContext` | read / readNode / ifExists / 类型化读取 |
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

# 五 JsonPath

```java
import com.awesomecopilot.json.jsonpath.context.DocumentContext;
import static com.awesomecopilot.json.jsonpath.JsonPathUtils.*;

String name = readNode(json, "$.store.book[0].title");           // 路径不存在会抛异常
String safe = readNodeIfExists(json, "$.store.book[0].title");   // 不存在返回 null
Money m = readNode(json, "$.amount", Money.class);               // 带类型转换（走 ValueHandlerFactory）
List<String> titles = readListNode(json, "$..title");
List<User> users = readListNode(json, "$.users", User.class);
Object single = readNodeSingleValue(json, "$.store.book[*].isbn"); // 单值结果（数组只有一个元素时取出来）
boolean has = ifExists(json, "$.store.book[2].price");

// 同一份报文读多个字段: 先 parse 一次, 再走 ctx 重载(收 String 的入口每次调用都会重解析整篇文档)
DocumentContext ctx = JsonPathUtils.parse(json);
String v1 = readNode(ctx, "$.a.b");
Money m2 = readNode(ctx, "$.amount", Money.class);
// 报文确定不含内嵌JSON字符串时可用 parse(json, false) 跳过整树展开(2MB 文档实测解析省 45%)
```

# 六 注意事项

- 业务代码不要再各自 `new ObjectMapper()`，统一走 `JacksonUtils` / `ObjectMapperFactory`，否则日期、枚举、精度行为不一致。
- 时间戳↔LocalDateTime 有两套反序列化器：`MillisToLocalDateTimeDeserializer`（13 位毫秒）与 `SecondsToLocalDateTimeDeserializer`（10 位秒），按上游给的单位选。
- `DateStr2LongDeserializer` 默认按东八区解析日期字符串。
