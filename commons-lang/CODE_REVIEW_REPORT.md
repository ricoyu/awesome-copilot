# commons-lang 代码评审报告（2026-09-22 全量重扫）

## 一、评审范围与方法

- 范围：`commons-lang/src/main/java` 全部 87 个文件、约 24000 行（utils / concurrent / resource / io / ratelimit / reflection / transformer / bean / vo / errors / exception / constants 逐包覆盖）。
- 分工：5 路子代理分包精读 + 本人机械模式扫描与逐条源码复核；**所有 P0/P1 与大部分 P2 由本人探针在 JDK 21 当前代码上独立复现**（子代理探针结果只作参考，其中一处误报见第五节 V4 注）。
- 方法：机械模式扫描（静态可变状态、SimpleDateFormat、异常捕获后不处理、缓存键拼法、资源关闭）+ 分包精读 + **探针实测**。本报告里标 [实测] 的条目都用 JDK 21 探针在当前代码上复现过（探针在 `commons-lang/target/probesrc/`，目录被 git 忽略）；标 [直读] 的条目是代码逻辑可直接判定、但未跑探针的。
- 基线：`mvn -pl commons-lang test` 当前 101 个测试全部通过。**下列所有问题现有测试一个都没覆盖到**——测试全绿不代表模块没问题。

严重度定义：
- **P0**：会造成数据损坏、结果错误、或线程/队列永久不可用，必须立即修。
- **P1**：特定输入或并发场景下产生错误结果、抛非预期异常。
- **P2**：语义误导、资源浪费、低概率路径问题。

---

## 二、P0 级问题（5 条）

### P0-1 KryoUtils 多线程共用同一个 Kryo 实例，序列化结果错乱 [实测] ✅已修复(2026-09-23)

- 位置：`utils/KryoUtils.java:30`（`private static final Kryo kryo = new Kryo()`），用在 `:64 toBytes` / `:82 toObject`
- 现象：8 线程并发各做 300 次"序列化再反序列化"，2400 次里 **651 次异常或数据不符**。典型报错 `KryoException: Unable to find class: \u0001\u0001item-3`（类名字节被其它线程的数据写花）、`NullPointerException: Cannot read field "arguments" because "genericType" is null`。
- 原因：Kryo 官方明确声明实例非线程安全（内部复用 class 注册表和输出缓冲），这个类把它做成了全局单例且没有任何同步。
- 影响：任何两处业务同时调 `KryoUtils`，写进 Redis/文件的字节就是坏的——读到一半抛异常，更糟的是**不抛异常但解出错值**。
- 修法：改成 `private static final ThreadLocal<Kryo> KYRO = Thread.withInitial(...)`（Kryo 实例创建成本低、可长期持有）；或调用处 `synchronized`。推荐 ThreadLocal，和其它工具类的门面风格一致。

### P0-2 IOUtils.toByteArray(InputStream) 把"一次没读满缓冲区"当成读完，返回被截断的数据 [实测] ✅已修复(2026-09-23)

- 位置：`utils/IOUtils.java:1197-1229`（关键判断 `:1214` `if (read < initialBuffer.length)`）
- 现象：探针构造一个"每次 read 只给 10 字节"的 5000 字节流（模拟 Socket 分段/慢速响应），`toByteArray` 返回长度 **10**——4990 字节丢失且不报任何错。
- 原因：`InputStream.read(byte[])` 返回值小于请求长度是合法行为，不代表流结束；只有返回 -1 才是 EOF。代码把"没读满 1024"直接当作"一次就读完了"。
- 影响面：`copilot-networking/.../AbstractRequestBuilder.java:814` 用本方法读 HTTP 响应体（`responseType=byte[].class`）——大响应被 TCP 分段时拿到半截字节，下游表现为 JSON 解析失败，排查方向极易被带偏。
- 修法：快路径只保留 `read == -1` 与"恰好读满缓冲区"两种，其余一律走累积循环读到 -1（或直接委托给类内已有的 `copy`）。

### P0-3 CopilotLinkedBlockingQueue 的 Spliterator 会死循环，且持有着队列的两把锁 [实测] ✅已修复(2026-09-23)

- 位置：`concurrent/CopilotLinkedBlockingQueue.java:918-937（tryAdvance）`，配合 `:195（dequeue 里 h.next = h 自指）`；`trySplit`（:859 区）与 `forEachRemaining`（:895 区）同样缺自指处理
- 现象：探针里对队列 `spliterator()`、消费一个元素后，其它线程把剩余元素取走，再调一次 `tryAdvance`——**3 秒未返回，卡死位置 `CopilotLinkedBlockingQueue.java:923`**（`while (current != null)` 里 `current = current.next` 在自己身上绕圈）。此时该线程持有 `fullyLock()` 的 putLock+takeLock，探针验证另起线程 `put(9)` 2 秒都完不成——**整个队列读写全部阻塞**。
- 原因：这是从 JDK `LinkedBlockingQueue` 抄的类。JDK 原版 `LBQSpliterator` 在 `takeIndex` 前进时会检查 `if (current == current.next) break`（自指节点=已被消费），这份拷贝把该判断弄丢了；而 `dequeue():195` 恰恰会把旧节点 `next` 指向自己。
- 影响：一次并行流/`stream()` 操作叠加正常出入队，队列永久不可用，只能重启进程。这就是"分布式/并发基础件"里最恶性的死锁形态。
- 修法：补齐 JDK 原版的自指判断：`tryAdvance`/`forEachRemaining` 循环里 `current` 前进前检查 `current == current.next` 则视为该节点已被消费、跳过（JDK 17 源码 `LBQSpliterator` 可直接对照）。或者更彻底：没有特殊定制需求就删掉这个拷贝类，用 JDK 自带的。
- 附注：`poll()`（:670）和 `Itr.remove()`（:718 区）也做了 `h.next = h`，同样依赖 spliterator 的自指检查存在，修一处即可覆盖。

### P0-4 DateUtils.parse 对小数秒位数≠3 的时间串给出错误时间且不报任何错 [实测] ✅已修复(2026-09-23)

- 位置：`utils/SimpleDateFormatHolder.java:525-531`（`finalShot` 按输入的小数秒位数追加同等个 `S`）
- 现象（`DateUtils.parse(s, TimeZone)` 入口）：
  - `2026-09-22 10:20:30.45` 解析成 `10:20:30.045`（SDF 把 "45" 按 45 毫秒读，正确值是 450 毫秒）
  - `2026-09-22 10:20:30.4567` 解析成 `10:20:34.567`（4 个 S 触发 lenient 进位，秒数 +4）
  - `2026-09-22 10:20:30.456789` 解析成 **`10:28:06.789`**（6 个 S 累计进位 456 秒，分钟都变了）
- 原因：`SimpleDateFormat` 的 `S` 是"毫秒数"而非"小数位"，`SS`/`SSSS` 会把后续数字继续当成毫秒整数累加。按位数动态拼 `S` 的写法从根上就是错的。
- 影响：ISO 8601 带微秒（6 位）的时间串在物联网/日志对接里很常见，`java.time` 解析时会把小数位右侧补 0 到 9 位再截断——SDF 做不到，只能取整：这是**错值不报错**，直接污染业务数据。
- 修法：小数秒一律拼固定 3 位 `SSS` 并截断/四舍五入多余位数；或该分支整体迁移到 `DateTimeFormatterBuilder.appendFraction(NANO_OF_SECOND, 1, 9, true)`。注意 `DateFormatterHolder` 侧没有这个问题（它用正则匹配位数但走的是 `DateTimeFormatter`），主修 SDF 这份。

### P0-5 IOUtils.append(String, byte[]) 实际是覆盖写入，原文件内容丢失 [实测] ✅已修复(2026-09-23)

- 位置：`utils/IOUtils.java:739-744`
- 现象：探针先写入 `AAA`，调 `IOUtils.append(path, "BBB".getBytes())`，返回 true，文件内容变成 `BBB`——`AAA` 没了。
- 原因：`append` 委托给了 `write(path, data)`，而 `:776` 的 `write(Path, byte[])` 用的是 `CREATE, TRUNCATE_EXISTING`。追加语义应该走 `write(path, data, CREATE, APPEND)`。同一族里 `:713 write(Path, String)` 反而用了 APPEND——两组方法语义刚好相反，靠注释和命名都猜不到。
- 影响：方法名+注释承诺追加、实际删原内容，属于数据损坏；且这两个同签名族的语义颠倒会让后续调用者继续写错。
- 修法：`append(String, byte[])` 与 `append(Path, byte[])`（如有）改走 `APPEND`；同时把 `write(Path, String)` 改为 `TRUNCATE_EXISTING` 与 byte[] 版一致（追加需求统一从 `append` 走）。改完必须补一组往返测试防回归。

### 修复记录（2026-09-23 批次：P0-1 / P0-2 / P0-5）

- **P0-1**：`KryoUtils` 的全局单例 `Kryo` 改为 `ThreadLocal<Kryo>`（每线程一个实例，配置原样保留），`toBytes`/`toObject` 经 `kryo.get()` 取实例。
  回归测试：`KryoUtilsConcurrencyTest`（8 线程 x 300 次往返）。修复前实测：测试线程全部在 `IdentityObjectIntMap.locateKey` 处空转，jstack 显示 8 个线程 CPU 时间各超 470 秒（比报告记载的"异常/错值"更糟，本轮复现的形态是死循环挂死）；修复后 0.209 秒通过、失败计数 0。
- **P0-2**：`IOUtils.toByteArray(InputStream)` 删除"没读满即读完"的错误快路径——`read` 返回值小于缓冲区长度是 `InputStream` 契约允许的行为，只有 -1 才代表结束；现在除 -1 外一律把首段写入累积输出并继续 `copy` 到读完。
- **P0-5**：`append(String, byte[])` 改为委托 `append(Path, byte[])`（APPEND）；同族两个 `write(Path, String[, Charset])` 由 APPEND 改为 `CREATE, TRUNCATE_EXISTING`，与 `write(Path, byte[])` 一致。
  回归测试：`IOUtilsReadAndAppendTest` 8 个用例——修复前 5 失败（分段流截断 x2、append 覆盖、write 变追加 x2），修复后 8/8 通过。
- **P0-3**：`LBQSpliterator` 按 JDK 21 原版 `LinkedBlockingQueue.LBQSpliterator` 结构补齐：新增 `succ(Node)` 自指判断（`p == p.next` 即节点已被消费，跳到 `head.next`），`tryAdvance`/`trySplit`/`forEachRemaining` 的节点前进全部改走 `succ`；`forEachRemaining` 同步换成 JDK 的分批快照写法。附注中 `poll()`/`Itr.remove()` 依赖的正是这套自指处理，修此一处即覆盖。
  回归测试：`CopilotLinkedBlockingQueueSpliteratorTest` 4 个用例（tryAdvance/forEachRemaining/trySplit 在"部分消费+并发取空"场景下 3 秒内必须结束、且队列读写恢复正常；外加无竞争遍历不丢不重）。修复前实测：3 个用例各挂死 3 秒判失败；修复后 4/4 通过（0.095 秒）。
- **P0-4**：`SimpleDateFormatHolder.finalShot` 的小数秒模式固定拼 3 个 `S`，同时新增 `normalizeFractionalSeconds`：解析前把输入的小数秒规整为 3 位（多于 3 位取前 3 位=截断到毫秒，与 java.time 的处理规则一致；少于 3 位右补 0，0.45 秒=450 毫秒）。两个自动识别入口 `DateUtils.parse(source)` 与 `parse(source, TimeZone)` 均已接入。
  说明：本轮 RED 复现时报告的三个错值例子（.45→045 毫秒、.4567→秒 34、.456789→分 28）在"无时区入口"原样重现（用例在该断言处即停）；修复后双入口交叉断言全部一致。
  回归测试：`DateUtilsFractionalSecondTest` 6 个用例（1/2/4/6 位小数 + 3 位正常路径 + 无小数秒，双入口交叉断言）。修复前实测：6 跑 4 败，失败信息与报告例子逐字对应（秒 34、分 28、毫秒 45）；修复后 6/6 通过。
  ⚠️ 行为变更（P0-4）：小数秒位数不等于 3 的输入，此前得到错值（不报错），现在得到正确毫秒值；多于 3 位按截断处理（456789 微秒 → 456 毫秒），与 `java.time` 解析小数秒的规则一致。位数恰为 3 或无小数秒的输入行为不变。

- ⚠️ 行为变更（P0-2）：对"分段供应"的流（Socket、慢速响应），`toByteArray` 从返回截断数据变为返回完整数据；`copilot-networking` 的 `AbstractRequestBuilder:814`（读 HTTP 响应体）直接受益。
- ⚠️ 行为变更（P0-5）：`write(Path, String)` 与 `write(Path, String, Charset)` 从追加变为覆盖。仓库内排查：`IOUtils.write` 的全部调用点中，String 版调用（`copilot-networking/HttpUtilsTest`、`copilot-test/FilesTest、FstTest、SqlFakeDataTest`）均按"写文件"语义使用，改覆盖符合预期；`copilot-test/FilesTest:242` 写后立刻读，不受影响。若外部有代码依赖它的追加语义，需改用 `append`。
- ⚠️ 行为变更（P0-1）：无 API 变化。但注意：Kryo 实例改线程私有后，配置仍在每个新实例上重放，序列化格式不变，旧缓存数据仍可读。

#### 独立评审发现与处置（2026-09-23，评审代理对未提交 diff 的旧新类对照实测）

评审结论：5 条修复方向均正确，无新增 P0；ThreadLocal 版与旧共享实例输出字节逐字节相同（旧缓存兼容实测通过）；
Spliterator 重写与 JDK 21 原版逐行等价、200 元素跨批边界不丢不重；P0-4 规整方法对 'T' 形式与 +0800 后缀起止位置正确、34 条语料无一原本可用者被弄坏。
逐条处置（发现 → 复核 → 结果）：

| # | 发现 | 复核 | 处置 |
|---|------|------|------|
| 1 | toByteArray(ByteChannel) 短读返回整个 1024 缓冲（尾部 0 当数据） | 属实（评审探针复现 len=1024/尾部 1014 个 0） | 即本报告 P1-11，未在本轮点名范围内，按编号另行修复 |
| 2 | createParentDir 对不带目录的相对文件名抛 NPE | 属实 | 即本报告 P1-12，同上按编号另行修复 |
| 3 | toByteArray javadoc 的 @throws 与实现（null→空数组、抛 IORuntimeException）不符 | 属实 | 已修：javadoc 改为如实描述既成契约并标注历史语义来源 |
| 4 | KryoUtils ThreadLocal 无 remove 入口，长命线程各持一份注册表 | 属实（量级：每线程一份 Kryo 及其类注册表，池线程数有限，非泄漏级） | 不改：Kryo 实例要长期持有才划算（官方建议），ThreadLocal 工具类无生命周期钩子可挂；记录为知情取舍 |
| 5 | write(Path,String) 里 Optional.of(data).orElse("") 是死代码，null 入参实际抛 NPE | 属实 | 不改：修它需决定 null→空串还是保持 NPE，属语义变更，与 P0-5 一并验收时再定 |
| 6 | KryoUtilsConcurrencyTest 工作线程非守护，缺陷重现时 JVM 退不出去 | 属实 | 已修：setDaemon(true) |
| 7 | 测试断言消息"2400 次里 651 次"来自另一份探针，本测试工况实测形态是挂死 | 属实 | 已改：消息与注释按本测试实际复现形态描述 |
| 8 | testToByteArrayStillFastPathForSingleRead 名字/注释失真（快路径已删） | 属实 | 已修：改名 testSingleReadStreamsStillCorrect，注释说明测的是常见输入的结果正确性 |
| 9 | spliterator trySplit 用例 completed 标志未断言 | 属实 | 已修：补断言（不预设子 spliterator 大小的具体值，弱一致语义下 0 也合法） |
| 10 | P0-4 测试未覆盖 'T' 形式与 +0800 后缀 | 属实 | 已修：补 2 个用例（评审探针先验证这两种形式修复后本就正确，用例用于防回归） |
| 11 | "与 java.time 一致"措辞偏宽（java.time 本身保留纳秒） | 属实 | 已修：改为"毫秒取值一致（toEpochMilli 层面）" |


---

## 三、P1 级问题（25 条）

### P1-1 DateFormatterHolder：withZone() 返回值被丢弃，时区设置完全不生效 [实测] ✅已修复(2026-09-23)

- 位置：`utils/DateFormatterHolder.java:43、:74、:126`
- 现象：`DateTimeFormatter` 是不可变对象，`withZone()` 返回新实例。三处 `format.withZone(...)` 都没接收返回值，探针确认 `formatFor(pattern, TimeZone)` 拿到的实例 `getZone()` 恒为 null——传什么时区都一样。
- 修法：`format = format.withZone(...)` 后再放入缓存；同时见 P1-2 的键问题。

### P1-2 日期格式缓存的键会碰撞：不同入参命中同一个格式器 [实测] ✅已修复(2026-09-23)

- 位置：`DateFormatterHolder.java:31-46`（`formatFor(pattern)` 与 `formatFor(pattern, locale)` 共用键 `pattern`）、`:63-79`（`formatFor(pattern, timezone)` 键只用 timezone）；`SimpleDateFormatHolder.java:55-58`（键 `pattern + locale.getCountry()`）
- 现象：
  - `formatFor("yyyy-MM-dd EEE", Locale.ENGLISH)` 先建缓存后，`formatFor("yyyy-MM-dd EEE")`（默认中文）拿到同一实例——探针输出 `2026-09-22 Tue`，默认入口输出了英文。两个方向都复现。
  - `SimpleDateFormatHolder.formatFor("MMM d", Locale.ENGLISH)` 与 `(…Locale.FRENCH)`：ENGLISH/FRENCH 的 `getCountry()` 都是空串，键都是 `pattern+""`——探针确认法语入口拿到英文实例（应显示法文月份）。
  - `formatFor(pattern, timezone)` 的键只有 timezone：两个不同 pattern 传同一 timezone 会互相顶掉。
- 原因：拼键时把参与构建格式器的维度漏掉了（locale 语言、pattern）。`StringUtils` 里同类缓存用了带 `|` 分隔符的全量键，这两处没学。
- 修法：键必须包含全部影响维度并用分隔符连接，如 `pattern + "|zh|CN"` 或直接 `pattern + "#" + locale.toLanguageTag() + "#" + zoneId`。

### P1-3 TIME_ZONE_LOCALE_HASH_MAP 键类型是 TimeZone，取值却传 String，恒为 null [实测] ✅已修复(2026-09-23)

- 位置：`constants/DateConstants.java:392`（`Map<TimeZone, Locale>`）；使用方 `DateFormatterHolder.java:68`、`SimpleDateFormatHolder.java:68`（`TIME_ZONE_LOCALE_HASH_MAP.get(timezone.getID())`）
- 现象：`TimeZone` 与 `String` 永远不相等，`get("GMT+8")` 恒 null，随后走 `Optional.ofNullable(...).orElse(Locale.CHINA)`——本意"按时区挑默认语言"的逻辑从未生效过，全部落到 CHINA。
- 修法：改成 `Map<String, Locale>` 以 `getID()` 为键，或取值时传 `timezone` 对象。

### P1-4 ValueHandlerFactory.DoubleValueHandler 收到 Float 直接强转，抛 ClassCastException [实测] ✅已修复(2026-09-23)

- 位置：`transformer/ValueHandlerFactory.java:181-183`
- 现象：`convert(1.5f)` 抛 `class java.lang.Float cannot be cast to class java.lang.Double`。
- 原因：`if (value instanceof Float) return (Double) value;`——判断了 Float 却按 Double 强转。同文件里 Float/Integer/Long handler 都没有这种分支，唯独 Double 有且写错。
- 修法：`return ((Float) value).doubleValue();`。

### P1-5 UrlParts.paramMap() 把含 `=` 的参数值整条丢弃 [实测] ✅已修复(2026-09-23)

- 位置：`bean/UrlParts.java:73-77`
- 现象：`?token=abc=def&flag=&x=1` 解析结果只剩 `{x=[1]}`：`token` 值里含 `=` 被 `split("=")` 切成 3 段、长度≠2 被跳过；`flag=` 空值参数切成 1 段也被跳过。
- 影响：URL 参数值是 base64（常有 `=` 填充）、签名串、表达式时参数被丢弃且不报错。
- 修法：`int i = nameValue.indexOf('=');` 按第一个 `=` 切分；无 `=` 的按空值参数处理（`flag` → `""`），或明确记录该行为。

### P1-6 RegexUtils.teardown 解析不出个位数端口，且把端口残留在 path 里 [实测] ✅已修复(2026-09-23)

- 位置：`utils/RegexUtils.java:55`（URL 正则里端口写成 `\d{2,}`）
- 现象：`http://localhost:8/app` 拆出 `port=80`（走了默认分支）、`path=:8/app`——个位数的合法端口（8 是 HTTP 默认端口）整个错乱。
- 修法：正则端口部分改 `\d+`。

### P1-7 PrimitiveUtils 基本类型分支缺失：double.class / char.class 返回 null [实测] ✅已修复(2026-09-23)

- 位置：`utils/PrimitiveUtils.java:69、:101`（只判 `Double.class`，漏 `Double.TYPE`）；`:81、:113`（Character 分支写成 `Character.class.equals(clazz) || Short.TYPE.equals(clazz)`——把 `Character.TYPE` 抄成了 `Short.TYPE`，而 Short 分支在别处已有）
- 现象：探针 `toPrimitive("1".getBytes(), double.class)` 与 `toPrimitive("x".getBytes(), char.class)` 都返回 null（包装类型正常）；同文件 `:48 isPrimitive` 也有同一处 `Character.class || Short.TYPE` 笔误。
- 修法：补 `Double.TYPE` / `Character.TYPE`，三处笔误一起修。

### P1-8 FstUtils 在 JDK 17+ 上第一次使用即抛 ExceptionInInitializerError [实测] ✅已修复(2026-09-23)

- 位置：`utils/FstUtils.java:23`（static 初始化 `FSTConfiguration.createDefaultConfiguration()`）
- 现象：JDK 21 下首次触碰该类即 `ExceptionInInitializerError`，根因 `InaccessibleObjectException: module java.base does not "opens java.lang"`。FST 2.x 注册默认类要对 `java.lang.String.value` 反射；全仓库 pom 没有任何 `--add-opens` 配置。
- 影响：JDK 21 项目里 `FstUtils` 是死代码，调用方拿到的是 Error 而不是友好异常。
- 修法：三选一并在 readme 写明：(a) surefire/部署脚本统一加 `--add-opens java.base/java.lang=ALL-UNNAMED`（copilot-test 里的 FstTest 就是这么跑的吗——需确认）；(b) 升级到仍维护的 FST 分支或换序列化方案；(c) 类加载失败时给出带指引的异常。当前"第一次调用就抛 Error"的状态最不可接受。

### P1-9 AlgorithmUtils.randomArr 两个退化入参进入死循环 [直读] ✅已修复(2026-09-23)

- 位置：`utils/AlgorithmUtils.java:24-37`
- 现象：`do { length = (int)(Math.random()*len); } while (length == 0);` 在 `len<=1` 时永远得 0，死循环；`do { nums[i] = (int)(Math.random()*maxValue); } while (nums[i-1]==nums[i]);` 在 `maxValue==1` 时只能产出 0，相邻两值必相等，同样死循环（`maxValue=2` 时期望次数尚可接受）。
- 修法：入口校验 `len > 1 && maxValue > 1`，不满足抛 `IllegalArgumentException`；去重逻辑改成从候选池洗牌取值。

### P1-10 SnowflakeId 时钟回拨的"虚拟时间"续发有跨进程重复 ID 的风险 [直读，探针未复现出实际重复] ✅已修复(2026-09-23)

- 位置：`utils/SnowflakeId.java:97（MAX_VIRTUAL_COMPENSATE_MS=60_000）、:289-294（回拨≤1分钟用 lastTimestamp 继续发号）、:303-307（虚拟模式下序列溢出直接 +1 毫秒）`
- 逻辑推演：本进程把 `lastTimestamp` 推到 T+30s 区间"超前发号"。同机/同 workerId 的另一进程从未经历跳变，当挂钟真正走到 T+30s 时，两进程在同一毫秒、同一 sequence 空间取号——**理论上可产出完全相同的 ID**。单进程内我跑了 9000 次取号（含前跳 30 秒再纠正的场景）无重复，但跨进程场景探针无法模拟。
- 现状缓解：注释已声明"回拨超过 1 分钟必人工介入"，风险窗被限制在 1 分钟内 + 依赖 sequence 步差。
- 建议：这是设计取舍而非纯 bug，报告留痕。若要收紧：虚拟时间戳从 `lastTimestamp` 继续加时，同时把 sequence 置为该毫秒最大值起步（与另一进程从 0 起步拉开距离），或虚拟模式下 workerId 低位掺 1bit"虚拟标志"。

### P1-11 IOUtils.toByteArray(ByteChannel) 返回整个 1024 字节缓冲区，尾部是 0 填充 [实测] ✅已修复(2026-09-23)

- 位置：`utils/IOUtils.java:1238-1267`（`:1247-1249` `if (buffer.hasRemaining()) return buffer.array();`）
- 现象：探针读 5 字节文件，返回长度 1024、只有前 5 字节非零。`buffer.array()` 是底层整个数组，与实际读到的字节数无关；且该版本只 read 一次，大文件必截断。
- 修法：`return Arrays.copyOf(buffer.array(), read);` 并循环读到 -1。

### P1-12 IOUtils.write / createParentDir 对不带目录的相对路径抛 NPE [实测] ✅已修复(2026-09-23)

- 位置：`utils/IOUtils.java:826-838`（`:837` `Files.exists(path.getParent(), ...)`）
- 现象：`IOUtils.write(Path.of("out.txt"), data)` 抛 `NullPointerException: Cannot invoke "java.nio.file.Path.getFileSystem()" because "path" is null`——上半段用 `Optional.ofNullable` 防了 null，最后一行又直接把 null 喂给 `Files.exists`。
- 修法：`Path parent = path.toAbsolutePath().getParent();` 或判空后直接 return true。

### P1-13 IOUtils.readAsString(in, false) 仍然关闭了调用方的流，autoClose 参数不起作用 [实测] ✅已修复(2026-09-23)

- 位置：`utils/IOUtils.java:612-628`（try-with-resources 包住 `new BufferedInputStream(in)`，`:621` 才判断 autoClose）
- 现象：探针用带 close() 计数的包装流验证：`readAsString(in, false)` 返回后底层流 close() 已被调用——关闭外层 BufferedInputStream 会连带关闭被包装的流，与参数意图相反。`readLines(InputStream)`、`readFileAsString(InputStream)` 同样无条件关流（只是没有承诺过 autoClose=false，问题轻一档）。
- 修法：不要把包装器放进 try-with-resources，或 autoClose=false 时只 flush 不 close；javadoc 写明"是否接管调用方流的关闭"。

### P1-14 YamlReader.get：高优先级文件里有半个 key 前缀，会把低优先级文件的完整值挡掉 [实测] ✅已修复(2026-09-23)

- 位置：`resource/YamlReader.java:240-262`（下钻循环里 `temp == null` 直接 `return null`）
- 现象：探针在临时目录构造 `config/application.yml`（只有 `my.other`）+ `application.yml`（含完整 `copilot.text.name: main-value`），`getString("copilot.text.name")` 返回 null；删掉 config 那份后同一调用返回 `main-value`。原因：逐层下钻在第一个 yaml 源上走到一半取不到就整个方法 return，没有换下一个优先级的源重试。
- 影响：这是配置读取器，多文件分层覆盖是它的设计初衷（YamlProfileReaders 依赖同样的下钻逻辑），半前缀文件会让主配置的整棵子树失效。
- 修法：某层取不到时 break 内层循环、继续 for-each 下一个 yaml 源；全部源试完再 return null。

### P1-15 PrimitiveUtils.toBigDecimal：BigInteger 溢出截断、double/float 返回 0、小数文本抛异常 [实测] ✅已修复(2026-09-23)

- 位置：`utils/PrimitiveUtils.java:158-181`
- 现象：探针确认三例——`toBigDecimal(new BigInteger("3000000000"))` 返回 `-1294967296`（走 `intValue()` 溢出）；`toBigDecimal(3.9)` 返回 `0`（没有 Double/Float 分支，落到末尾 ZERO）；`toBigDecimal("1.5")` 抛 `NumberFormatException`（用 `Integer.parseInt`）。
- 影响：金额/尺寸转换场景直接算错数，且不报错的那个（BigInteger、double）比报错的更危险。
- 修法：BigInteger 用 `new BigDecimal((BigInteger) value)`；补 `BigDecimal.valueOf(double)` 分支；字符串用 `new BigDecimal(str.trim())`。

### P1-16 MM-dd-yyyy 家族的日期永远解析不出来（正则多转义了一层） [实测] ✅已修复(2026-09-23)

- 位置：`constants/DateConstants.java:66、73、80、87`（`compile("\\d{2}-\\d{2}-\\\\d{4}")` 末段写成了 4 个反斜杠）
- 现象：编译后正则变成 `\d{2}-\d{2}-\d{4}`，`\` 匹配的是字面反斜杠而不是数字。探针确认 `PT_DATE_EN.matcher("12-25-2020").matches()` 为 false、`DateUtils.parse("12-25-2020")` 返回 null（只有一条 warn 日志）。
- 影响链：`copilot-json` 的 `DateDeserializer:74`、`LocalDateDeserializer:92` 直接走这两个入口——客户端传美式日期得到 null 或反序列化异常。四个常量同病，说明是从同一处复制扩散的。
- 修法：四处 `\\\\d` 改 `\\d`；补一条测试：遍历 `PT_*` 常量、用各自注释里的示例串断言 matches=true。

### P1-17 BooleanValueHandler 用 Boolean.getBoolean 解析字符串，"true" 转成 false [实测] ✅已修复(2026-09-23)

- 位置：`transformer/ValueHandlerFactory.java:422-424`（`return Boolean.getBoolean((String) value);`）
- 现象：`Boolean.getBoolean("true")` 的语义是"读名叫 true 的系统属性再判等"，不是解析字符串。探针确认 `ValueHandlerFactory.convert("true", Boolean.class)` 返回 **false**，且不抛异常不打日志。
- 影响链：所有 String→Boolean 的转换（数据库 char 列、JSON 字符串布尔、配置值）一律得到 false，除非启动参数恰好 `-Dtrue=true`——错误取值且不报错。
- 修法：`Boolean.parseBoolean(((String) value).trim())`。

### P1-18 MathUtils 用 new BigDecimal(double) 做"精确运算"，2.675 四舍五入成 2.67 [实测] ✅已修复(2026-09-23)

- 位置：`utils/MathUtils.java:24-28、95-96、136-137、399-400、424+` 等全部 double 重载
- 现象：探针确认 `round(2.675,2)`=2.67、`format(2.675,2)`="2.67"、`mul(1.1,1.1,20)`=1.2100000000000002；同文件 `roundUpTwo(new BigDecimal("2.675"))`=2.68，两种写法互相矛盾。
- 原因：`new BigDecimal(0.1)` 取的是 double 的二进制近似（0.1000000000000000055...），"精确运算"的注释不成立，金额场景恒比期望少一分。
- 修法：所有 double 入口改 `BigDecimal.valueOf(v)`。

### P1-19 DynamicUtils.createObject 在遍历中删元素：抛 ConcurrentModificationException 且就地改掉调用方的 Map [实测] ✅已修复(2026-09-23)

- 位置：`utils/DynamicUtils.java:50-54`（for-each 里 `properties.remove(entry.getKey())`）
- 现象：探针确认属性表 `{gone:null, kept:1L}` 抛 `ConcurrentModificationException`；null 恰在最后一项时不抛异常，但调用方的 Map 被悄悄删了键（`DynamicUtilsTest.testSubObject3` 只放一个 null 键所以从没暴露）。
- 修法：先 `entrySet().removeIf(...)` 或在过滤后的新 Map 上构建，不改调用方入参。

### P1-20 PageDTO.getPage 只填 orders 不填 order 且不 trim：JPA 分页排序整条失效且不报错 [实测+调用链核对] ✅已修复(2026-09-23)

- 位置：`dto/PageDTO.java:44-59`；消费方 `copilot-orm/.../JPACriteriaQuery.java:574-575`（只看 `page.getOrder()`）
- 现象：探针 `order="id:asc, create_time:desc"` → `getOrder()`=null、第二个 OrderBean 的字段名是 `" create_time"`（带前导空格）。`JPACriteriaQuery` 判 `getOrder()!=null` 才加排序 → 前端传什么排序都被忽略，不报错不打日志；`NativeSqlQueryBuilder` 两个都读且内部有 trim 补偿，所以同一条 SQL 换个查询入口结果顺序不同。
- 修法：解析时对字段名 trim；把第一个排序项同时 `page.setOrder(...)`，或统一约定消费方只读 `getOrders()`（并改 JPACriteriaQuery）。

### P1-21 异常族三个构造器丢消息：上层拿到 null 或固定文案 [实测] ✅已修复(2026-09-23)

- `exception/BusinessException.java:67-72`——四参构造 `(code, messageTemplate, messageParams, defaultMesssage)` 从头到尾没给 `this.message` 赋值，还把 `this.msgTemplate` 赋了两遍，第 4 个参数 `defaultMesssage` 完全丢弃；类里有 `@Override getMessage()` 返回 `message` 字段，实测 `getMessage()` 返回 **null**。全局异常处理器做 `e.getMessage().trim()` 或拼 JSON 时直接 NPE，日志里业务错误描述整条消失。修法：补 `this.message = defaultMesssage;`、删重复行。
- `exception/ApplicationException.java:18-20、48-50、65-68`——`message` 字段被初始化式写死 "Internal Server Error"，`getMessage()` 返回该字段而非 Throwable 的 detailMessage；实测 `new ApplicationException(cause).getMessage()` 恒为 "Internal Server Error"，被包裹异常的真实原因只能翻堆栈。修法：去掉子类 message 字段与 getMessage 覆盖，或构造时 `message = cause==null?默认:cause.getMessage()`。
- `exception/ServiceException.java:44-47`——三参构造 `(code, messageTemplate, defaultMesssage)` 只存 code 和 message，`messageTemplate` 无人接收、类里也没有 `getMsgTemplate`（实测确认）；与 `BusinessException` 同名构造器行为不一致。修法：补 msgTemplate 字段，或删这个误导性的重载参数。

### P1-22 Concurrent.await() 永不暴露任务失败，与自身 javadoc 相反 [实测] ⏭️不修(设计如此, 2026-09-23 作者裁决)

- 位置：`concurrent/Concurrent.java:112-121、182、195`
- 现象：`submit` 的 `handle` 里失败分支 `return failedFuture`——`handle` 不会展平 `CompletionStage`，于是外层 future 对 `allOf` 永远表现为成功；实测故意让任务抛异常后 `await()` 正常返回，不抛 `CompletionException`（182 行 javadoc 明确写 `@throws CompletionException`）。用 `submit`+`await` 做批处理时，任务失败只剩一条日志，主流程继续走完，数据半提交。
- 修法：失败分支改 `throw new CompletionException(e)`；`FutureResult` 里为兼容这个嵌套而写的 `instanceof` 特判随之删除。
- **处置(2026-09-23)**：经作者裁决按设计保留，不改代码。理由：`Concurrent` 的设计意图是"同时提交若干无依赖任务、await 处等全部执行完"——任务成败不影响屏障放行，失败已有 `log.error("任务执行失败", e)` 落日志。若按原修法让失败分支抛 `CompletionException`，`allOf().join()` 会在第一个失败任务处提前抛出并清空 ThreadLocal，其余任务仍在后台运行无人等待，"等待全部完成"的语义反而被破坏。`FutureResult` 的 `instanceof` 特判保留：它让单独 `get()` 某个结果的调用方仍能拿到异常。
- 残留小项(未处理)：`await()` javadoc 的 `@throws CompletionException` 与上述设计不符，属一行文档修正，待作者确认删或改。
  连带：P2-37 中"FutureResult 解包特判在 P1-22 修掉后应一并删"的前提不成立，该特判按设计长期保留。

### P1-23 AbortWithReportPolicy 信号量许可泄漏，线程 dump 能力从此永久失效且无任何提示 [直读] ✅已修复(2026-09-23)

- 位置：`concurrent/AbortWithReportPolicy.java:105-113`（`:110-112` 的提前 `return` 在 `tryAcquire` 成功之后、`finally` 之外）
- 推演：线程 A 完成一次 dump 后，线程 B 已通过 `:102` 的第一道时间检查、但在 B 更新 `lastPrintTime` 前 A 已把时间戳刷好——B `tryAcquire` 拿到许可，进 `:110` 判定"10 分钟内已 dump"直接 `return`，唯一许可永久丢失（`Semaphore(1)` 归零，只有 dump 任务体 `:125` 的 finally 才归还，而该任务根本没提交）。之后所有 `reject → threadDump()` 全部 `tryAcquire` 失败直接返回，恰好在"线程池持续拒绝、最需要现场"时什么都不输出。
- 修法：把 `tryAcquire` 之后的逻辑包进 `try/finally`，未成功提交 dump 任务时 `guard.release()`；或改 `AtomicLong` CAS 记 `lastPrintTime` 彻底去掉信号量。

### P1-24 TraceId 缺少实例维度，多进程同毫秒必然重号 [直读] ✅已修复(2026-09-23)

- 位置：`utils/TraceId.java:20-22、43、45`
- 现象：`traceId = PREFIX + serviceId + "-" + 毫秒 + "-" + SEQUENCE.incrementAndGet()`，`SEQUENCE` 是每个 JVM 各自从 0 起步的静态计数器，串里没有任何机器/PID/workerId 成分。同服务多实例在同一毫秒生成的 traceId 完全相同（两个变量都一致），全链路日志按 traceId 聚合会把不同实例的不同请求串成一条——这是必然重号，不是概率碰撞。类头注释还写"格式为 REQ-…"而 `PREFIX = "TID-"`。
- 修法：中段掺入实例标识（复用 SnowflakeId 的 workerId 或 PID/hostname 短哈希），或直接用 UUID 尾段。

### P1-25 Concurrent 的 ThreadLocal 只在 await() 清理：只用 submit 不 await 就按线程无限累积 [直读] ⏭️不修(设计如此, 2026-09-23 作者裁决)

- 位置：`concurrent/Concurrent.java:66、118、196-198`
- 现象：`addCompleteFuture` 每次 `submit`/`execute` 都往线程局部 Set 追加一个已完成 future（其 value 是任务结果对象），全类唯一的 `remove()` 在 `await()` 的 finally 里；`await()` 开头遇到空 Set 直接 return 也不 remove。Web 容器的线程复用，任何"提交但不 await"（fire-and-forget 场景，本类正鼓励这种用法）的请求都会让该线程的 Set 单调增长，已完成任务及其结果对象永不可回收。
- 修法：不隐式攒——让调用方显式持有返回的 `FutureResult`，或 `await` 改为接收 Set 参数；确需线程局部则挂到框架已有的线程清理钩子（copilot-web 的 ThreadLocal 清理只清 ThreadContext，不管 Concurrent）。
- **处置(2026-09-23)**：经作者裁决按设计保留，不改代码。理由：`submit`/`execute` 与 `await` 是成对使用的调用契约——本类的用法就是"提交若干任务后到 await 处等全部执行完"，正常路径下 `await()` 的 finally 必然执行 `COMPLETABLE_FUTURE_THREAD_LOCAL.remove()`，不存在累积。
- 已知残余风险(作者知情接受)：若有代码只 `submit` 从不 `await`（fire-and-forget），该线程的 Set 会持续增长，类本身不阻止这种写法；`await()` 开头遇空 Set 直接 return 不 remove，但留下的只是空集合，无实际增长。


#
#
#
 
P
1
 
批
次
修
复
记
录
（
2
0
2
6
-
0
9
-
2
3
，
2
3
 
条
，
P
1
-
2
2
/
P
1
-
2
5
 
按
作
者
裁
决
不
修
）






流
程
：
每
条
先
写
测
试
跑
出
 
R
E
D
，
再
修
生
产
代
码
到
 
G
R
E
E
N
。
测
试
类
（
均
在
 
`
s
r
c
/
t
e
s
t
/
j
a
v
a
`
 
下
）
：



`
u
t
i
l
s
/
D
a
t
e
F
o
r
m
a
t
C
a
c
h
e
T
e
s
t
`
（
9
 
用
例
，
P
1
-
1
/
2
/
3
/
1
6
 
+
 
P
2
-
3
6
）
、
`
u
t
i
l
s
/
P
r
i
m
i
t
i
v
e
A
n
d
T
r
a
n
s
f
o
r
m
e
r
F
i
x
T
e
s
t
`
（
6
 
用
例
，
P
1
-
4
/
5
/
6
/
7
/
1
5
/
1
7
）
、
`
u
t
i
l
s
/
I
O
C
h
a
n
n
e
l
A
n
d
C
l
o
s
e
T
e
s
t
`
（
7
 
用
例
，
P
1
-
1
1
/
1
2
/
1
3
）
、
`
r
e
s
o
u
r
c
e
/
Y
a
m
l
R
e
a
d
e
r
D
r
i
l
l
D
o
w
n
T
e
s
t
`
（
4
 
用
例
，
P
1
-
1
4
）
、
`
u
t
i
l
s
/
M
a
t
h
U
t
i
l
s
R
o
u
n
d
T
e
s
t
`
（
4
 
用
例
，
P
1
-
1
8
）
、
`
u
t
i
l
s
/
M
i
s
c
B
a
t
c
h
F
i
x
T
e
s
t
`
（
1
2
 
用
例
，
P
1
-
8
/
9
/
1
9
/
2
1
/
2
4
）
、
`
d
t
o
/
P
a
g
e
D
t
o
O
r
d
e
r
T
e
s
t
`
（
4
 
用
例
，
P
1
-
2
0
）
、
`
u
t
i
l
s
/
S
n
o
w
f
l
a
k
e
I
d
V
i
r
t
u
a
l
S
e
q
u
e
n
c
e
T
e
s
t
`
（
2
 
用
例
，
P
1
-
1
0
）
、
`
c
o
n
c
u
r
r
e
n
t
/
A
b
o
r
t
W
i
t
h
R
e
p
o
r
t
P
o
l
i
c
y
G
u
a
r
d
T
e
s
t
`
（
2
 
用
例
，
P
1
-
2
3
）
。






R
E
D
 
证
据
与
修
法
要
点
：






-
 
*
*
P
1
-
1
/
P
1
-
2
（
D
a
t
e
F
o
r
m
a
t
t
e
r
H
o
l
d
e
r
 
+
 
S
i
m
p
l
e
D
a
t
e
F
o
r
m
a
t
H
o
l
d
e
r
）
*
*
：
R
E
D
 
实
测
—
—
`
w
i
t
h
Z
o
n
e
`
 
版
拿
到
的
是
没
带
时
区
的
同
一
个
实
例
；
`
(
y
y
y
y
-
M
,
 
E
N
G
L
I
S
H
)
`
 
先
建
后
，
无
 
l
o
c
a
l
e
 
入
口
 
`
f
o
r
m
a
t
F
o
r
(
"
y
y
y
y
-
M
"
)
`
 
输
出
 
`
2
0
2
6
-
0
9
-
2
2
 
T
u
e
`
（
命
中
了
英
文
实
例
，
键
碰
撞
）
。
修
法
：
缓
存
键
把
 
p
a
t
t
e
r
n
/
l
o
c
a
l
e
/
t
i
m
e
Z
o
n
e
/
长
度
全
量
拼
进
 
d
i
s
t
i
n
c
t
K
e
y
；
`
w
i
t
h
Z
o
n
e
`
 
结
果
真
正
放
入
缓
存
；
`
S
i
m
p
l
e
D
a
t
e
F
o
r
m
a
t
`
 
非
线
程
安
全
，
带
 
z
o
n
e
 
的
入
口
改
为
每
次
新
建
不
再
共
享
缓
存
实
例
。



-
 
*
*
P
1
-
3
/
P
1
-
1
6
/
P
2
-
3
6
（
D
a
t
e
C
o
n
s
t
a
n
t
s
）
*
*
：
R
E
D
 
实
测
—
—
`
T
I
M
E
_
Z
O
N
E
_
L
O
C
A
L
E
_
H
A
S
H
_
M
A
P
.
g
e
t
(
"
G
M
T
"
)
`
 
恒
 
n
u
l
l
（
键
是
 
T
i
m
e
Z
o
n
e
 
对
象
、
取
用
的
是
 
S
t
r
i
n
g
，
1
5
 
个
单
元
格
从
未
命
中
过
）
；
`
P
T
_
D
A
T
E
_
E
N
`
 
四
条
正
则
源
码
里
写
成
 
`
\
d
`
（
J
a
v
a
 
字
符
串
里
单
反
斜
杠
+
d
 
是
非
法
转
义
，
编
译
期
即
报
"
非
法
转
义
字
符
"
—
—
第
一
次
修
的
时
候
还
把
 
`
\
\
d
`
 
改
成
了
 
`
\
d
`
 
反
而
改
回
去
，
最
终
以
磁
盘
 
r
e
p
r
 
核
对
四
处
都
是
 
`
\
\
d
`
）
。
修
法
：
键
改
 
S
t
r
i
n
g
（
g
e
t
I
D
）
，
表
 
`
C
o
l
l
e
c
t
i
o
n
s
.
u
n
m
o
d
i
f
i
a
b
l
e
M
a
p
`
 
封
死
并
发
写
；
正
则
转
义
修
正
后
 
`
1
2
-
2
5
-
2
0
2
0
`
 
解
析
通
过
（
顺
带
覆
盖
 
P
1
-
1
6
 
附
带
发
现
：
`
F
M
T
_
D
A
T
E
_
F
O
R
M
A
T
_
E
N
`
 
与
正
则
的
分
隔
符
统
一
为
连
字
符
）
。



-
 
*
*
P
1
-
4
/
P
1
-
1
7
（
V
a
l
u
e
H
a
n
d
l
e
r
F
a
c
t
o
r
y
）
*
*
：
R
E
D
 
实
测
—
—
F
l
o
a
t
 
值
走
 
D
o
u
b
l
e
V
a
l
u
e
H
a
n
d
l
e
r
 
抛
 
C
l
a
s
s
C
a
s
t
E
x
c
e
p
t
i
o
n
；
`
"
t
r
u
e
"
`
 
转
 
B
o
o
l
e
a
n
 
得
 
f
a
l
s
e
（
`
B
o
o
l
e
a
n
.
g
e
t
B
o
o
l
e
a
n
`
 
是
读
系
统
属
性
）
。
修
法
：
F
l
o
a
t
 
走
 
`
d
o
u
b
l
e
V
a
l
u
e
(
)
`
；
改
 
`
B
o
o
l
e
a
n
.
p
a
r
s
e
B
o
o
l
e
a
n
(
t
r
i
m
)
`
。



-
 
*
*
P
1
-
5
（
U
r
l
P
a
r
t
s
.
p
a
r
a
m
M
a
p
）
*
*
：
R
E
D
 
实
测
—
—
`
a
=
x
=
1
&
b
=
2
`
 
里
 
`
a
`
 
整
条
被
丢
弃
。
修
法
：
按
第
一
个
 
`
=
`
 
切
分
，
值
里
的
 
`
=
`
 
保
留
。



-
 
*
*
P
1
-
6
（
R
e
g
e
x
U
t
i
l
s
.
t
e
a
r
d
o
w
n
）
*
*
：
R
E
D
 
实
测
—
—
`
h
o
s
t
:
8
/
p
a
t
h
`
 
解
析
后
端
口
识
别
不
出
、
`
8
/
p
a
t
h
`
 
残
留
在
 
p
a
t
h
。
修
法
：
端
口
正
则
 
`
\
d
{
2
,
}
`
 
改
 
`
\
d
+
`
（
正
文
+
注
释
共
 
5
 
处
）
。



-
 
*
*
P
1
-
7
/
P
1
-
1
5
（
P
r
i
m
i
t
i
v
e
U
t
i
l
s
）
*
*
：
R
E
D
 
实
测
—
—
`
t
o
W
r
a
p
p
e
r
(
d
o
u
b
l
e
.
c
l
a
s
s
)
`
/
`
c
h
a
r
.
c
l
a
s
s
`
 
返
回
 
n
u
l
l
（
分
支
漏
写
/
笔
误
）
；
`
t
o
B
i
g
D
e
c
i
m
a
l
(
n
e
w
 
B
i
g
I
n
t
e
g
e
r
(
超
大
)
)
`
 
溢
出
截
断
、
`
(
O
b
j
e
c
t
)
2
.
5
`
 
返
回
 
0
、
`
"
2
.
5
"
`
 
抛
异
常
。
修
法
：
补
分
支
、
修
笔
误
；
t
o
B
i
g
D
e
c
i
m
a
l
 
按
 
N
u
m
b
e
r
/
C
h
a
r
S
e
q
u
e
n
c
e
 
分
派
走
 
`
n
e
w
 
B
i
g
D
e
c
i
m
a
l
(
S
t
r
i
n
g
)
`
。



-
 
*
*
P
1
-
8
（
F
s
t
U
t
i
l
s
）
*
*
：
R
E
D
 
实
测
（
探
针
）
—
—
J
D
K
 
2
1
 
首
触
 
`
t
o
B
y
t
e
s
`
 
即
 
`
E
x
c
e
p
t
i
o
n
I
n
I
n
i
t
i
a
l
i
z
e
r
E
r
r
o
r
`
，
根
因
 
`
I
n
a
c
c
e
s
s
i
b
l
e
O
b
j
e
c
t
E
x
c
e
p
t
i
o
n
:
 
m
o
d
u
l
e
 
j
a
v
a
.
b
a
s
e
 
d
o
e
s
 
n
o
t
 
"
o
p
e
n
s
 
j
a
v
a
.
l
a
n
g
"
`
。
修
法
（
报
告
选
项
 
c
）
：
懒
初
始
化
+
捕
获
，
抛
带
 
`
-
-
a
d
d
-
o
p
e
n
s
 
j
a
v
a
.
b
a
s
e
/
j
a
v
a
.
l
a
n
g
=
A
L
L
-
U
N
N
A
M
E
D
`
 
指
引
的
 
I
l
l
e
g
a
l
S
t
a
t
e
E
x
c
e
p
t
i
o
n
，
只
尝
试
一
次
不
反
复
抖
动
；
j
a
v
a
d
o
c
 
写
明
运
行
前
提
。



-
 
*
*
P
1
-
9
（
A
l
g
o
r
i
t
h
m
U
t
i
l
s
.
r
a
n
d
o
m
A
r
r
）
*
*
：
R
E
D
 
实
测
—
—
`
r
a
n
d
o
m
A
r
r
(
1
,
1
0
)
`
 
在
 
m
a
i
n
 
线
程
死
循
环
 
4
2
1
 
秒
（
j
s
t
a
c
k
 
坐
实
栈
顶
 
`
A
l
g
o
r
i
t
h
m
U
t
i
l
s
.
r
a
n
d
o
m
A
r
r
:
2
8
`
）
，
S
A
M
E
_
T
H
R
E
A
D
 
的
 
@
T
i
m
e
o
u
t
 
中
断
不
了
，
测
试
用
 
`
T
h
r
e
a
d
M
o
d
e
.
S
E
P
A
R
A
T
E
_
T
H
R
E
A
D
`
 
才
能
报
 
T
i
m
e
o
u
t
。
修
法
：
`
l
e
n
<
2
`
 
或
 
`
m
a
x
V
a
l
u
e
<
2
`
 
抛
 
I
l
l
e
g
a
l
A
r
g
u
m
e
n
t
E
x
c
e
p
t
i
o
n
；
去
重
改
"
前
值
+
非
零
随
机
偏
移
取
模
"
一
次
成
型
（
不
再
重
采
样
）
。



-
 
*
*
P
1
-
1
0
（
S
n
o
w
f
l
a
k
e
I
d
 
虚
拟
时
间
）
*
*
：
R
E
D
 
由
 
s
t
a
s
h
 
回
验
—
—
同
一
用
例
在
修
复
前
测
得
虚
拟
毫
秒
的
 
s
e
q
u
e
n
c
e
 
从
 
0
/
小
值
起
步
。
修
法
：
虚
拟
推
进
的
毫
秒
 
s
e
q
u
e
n
c
e
 
从
 
2
0
4
8
（
半
程
）
起
步
而
非
 
0
，
与
未
经
历
回
拨
、
同
 
w
o
r
k
e
r
I
d
 
的
对
端
进
程
拉
开
 
2
0
4
8
 
个
号
的
距
离
；
正
常
模
式
仍
从
 
0
 
起
步
（
另
一
用
例
锁
住
）
。
既
有
 
S
n
o
w
f
l
a
k
e
I
d
T
e
s
t
 
8
 
用
例
全
过
，
虚
拟
模
式
单
进
程
唯
一
性
不
回
退
。



-
 
*
*
P
1
-
1
1
/
P
1
-
1
2
/
P
1
-
1
3
（
I
O
U
t
i
l
s
）
*
*
：
R
E
D
 
实
测
 
6
 
例
—
—
B
y
t
e
C
h
a
n
n
e
l
 
短
读
返
回
整
 
1
0
2
4
 
缓
冲
（
尾
部
 
0
 
当
数
据
）
、
2
M
B
 
通
道
只
读
回
 
1
0
2
4
；
`
c
r
e
a
t
e
P
a
r
e
n
t
D
i
r
(
P
a
t
h
s
.
g
e
t
(
"
o
u
t
.
t
x
t
"
)
)
`
 
抛
 
N
P
E
；
`
r
e
a
d
A
s
S
t
r
i
n
g
(
i
n
,
f
a
l
s
e
)
`
 
之
后
 
i
n
 
已
被
关
。
修
法
：
B
y
t
e
C
h
a
n
n
e
l
 
循
环
读
到
 
-
1
 
且
拷
贝
实
际
长
度
；
g
e
t
P
a
r
e
n
t
(
)
 
判
空
；
不
把
包
装
器
 
B
u
f
f
e
r
e
d
I
n
p
u
t
S
t
r
e
a
m
/
S
c
a
n
n
e
r
 
放
进
 
t
r
y
-
w
i
t
h
-
r
e
s
o
u
r
c
e
s
（
S
c
a
n
n
e
r
.
c
l
o
s
e
(
)
 
对
 
C
l
o
s
e
a
b
l
e
 
源
同
样
连
带
关
流
—
—
R
E
D
 
阶
段
实
测
踩
到
，
第
一
次
修
漏
了
这
层
）
。



-
 
*
*
P
1
-
1
4
（
Y
a
m
l
R
e
a
d
e
r
）
*
*
：
下
钻
逻
辑
抽
为
包
级
静
态
 
`
d
r
i
l
l
D
o
w
n
(
L
i
s
t
,
 
p
a
t
h
)
`
 
脱
盘
单
测
（
R
E
D
 
阶
段
构
造
入
参
即
编
译
失
败
属
预
期
红
灯
，
s
t
a
s
h
 
回
验
语
义
：
高
优
先
级
文
件
只
有
半
个
前
缀
时
完
整
值
取
不
到
）
。
修
法
：
某
源
下
钻
失
败
换
下
一
优
先
级
源
，
全
部
试
完
才
返
回
 
n
u
l
l
；
中
途
标
量
不
再
 
C
l
a
s
s
C
a
s
t
E
x
c
e
p
t
i
o
n
。
`
Y
a
m
l
R
e
a
d
e
r
T
e
s
t
`
 
原
 
3
 
用
例
（
依
赖
真
实
 
y
a
m
l
 
文
件
）
保
持
通
过
。



-
 
*
*
P
1
-
1
8
（
M
a
t
h
U
t
i
l
s
）
*
*
：
R
E
D
 
实
测
—
—
`
r
o
u
n
d
(
2
.
6
7
5
,
2
)
=
2
.
6
7
`
、
`
m
u
l
(
1
.
1
,
1
.
1
,
2
0
)
=
1
.
2
1
0
0
0
0
0
0
0
0
0
0
0
0
0
2
`
。
修
法
：
1
1
 
处
 
`
n
e
w
 
B
i
g
D
e
c
i
m
a
l
(
d
o
u
b
l
e
)
`
 
全
改
 
`
B
i
g
D
e
c
i
m
a
l
.
v
a
l
u
e
O
f
`
（
`
n
e
w
 
B
i
g
D
e
c
i
m
a
l
(
"
1
"
)
`
 
字
符
串
构
造
不
动
）
；
测
试
锁
住
 
`
r
o
u
n
d
(
2
.
5
6
7
,
2
)
=
2
.
5
7
`
 
等
普
通
值
不
回
退
。



-
 
*
*
P
1
-
1
9
（
D
y
n
a
m
i
c
U
t
i
l
s
）
*
*
：
R
E
D
 
实
测
—
—
`
{
g
o
n
e
:
n
u
l
l
,
 
k
e
p
t
:
1
L
}
`
 
抛
 
C
M
E
；
n
u
l
l
 
在
最
后
一
项
时
入
参
 
M
a
p
 
被
悄
悄
删
键
。
修
法
：
过
滤
进
新
 
L
i
n
k
e
d
H
a
s
h
M
a
p
，
入
参
不
动
（
`
D
y
n
a
m
i
c
U
t
i
l
s
T
e
s
t
`
 
原
 
3
 
用
例
含
单
 
n
u
l
l
 
键
场
景
保
持
通
过
）
。



-
 
*
*
P
1
-
2
0
（
P
a
g
e
D
T
O
.
g
e
t
P
a
g
e
）
*
*
：
R
E
D
 
由
 
s
t
a
s
h
 
回
验
—
—
`
o
r
d
e
r
=
"
i
d
:
a
s
c
,
 
c
r
e
a
t
e
_
t
i
m
e
:
d
e
s
c
"
`
 
时
 
`
p
a
g
e
.
g
e
t
O
r
d
e
r
(
)
`
 
恒
 
n
u
l
l
、
第
二
项
字
段
名
带
前
导
空
格
（
4
 
用
例
 
3
 
挂
 
1
 
错
）
。
修
法
：
字
段
名
 
t
r
i
m
；
首
项
 
`
p
a
g
e
.
s
e
t
O
r
d
e
r
(
.
.
.
)
`
、
其
余
进
 
o
r
d
e
r
s
（
按
 
P
a
g
e
 
注
释
"
o
r
d
e
r
s
 
是
第
二
三
级
"
的
语
义
，
c
o
p
i
l
o
t
-
o
r
m
 
的
 
J
P
A
C
r
i
t
e
r
i
a
Q
u
e
r
y
/
N
a
t
i
v
e
S
q
l
Q
u
e
r
y
B
u
i
l
d
e
r
 
两
个
消
费
方
不
再
看
到
不
同
的
排
序
集
）
。



-
 
*
*
P
1
-
2
1
（
异
常
族
）
*
*
：
R
E
D
 
实
测
—
—
B
u
s
i
n
e
s
s
E
x
c
e
p
t
i
o
n
 
四
参
构
造
 
`
g
e
t
M
e
s
s
a
g
e
(
)
`
 
返
回
 
n
u
l
l
；
`
n
e
w
 
A
p
p
l
i
c
a
t
i
o
n
E
x
c
e
p
t
i
o
n
(
i
o
)
.
g
e
t
M
e
s
s
a
g
e
(
)
`
 
恒
 
"
I
n
t
e
r
n
a
l
 
S
e
r
v
e
r
 
E
r
r
o
r
"
；
S
e
r
v
i
c
e
E
x
c
e
p
t
i
o
n
 
三
参
构
造
丢
 
t
e
m
p
l
a
t
e
。
修
法
：
补
 
m
e
s
s
a
g
e
 
赋
值
与
 
s
u
p
e
r
 
调
用
、
A
p
p
l
i
c
a
t
i
o
n
E
x
c
e
p
t
i
o
n
(
T
h
r
o
w
a
b
l
e
)
 
用
 
c
a
u
s
e
 
的
消
息
、
S
e
r
v
i
c
e
E
x
c
e
p
t
i
o
n
 
补
 
m
s
g
T
e
m
p
l
a
t
e
 
字
段
+
g
e
t
t
e
r
（
与
 
B
u
s
i
n
e
s
s
E
x
c
e
p
t
i
o
n
 
同
名
构
造
器
行
为
拉
齐
）
。
既
有
构
造
器
行
为
用
 
`
t
e
s
t
N
o
A
r
g
C
o
n
s
t
r
u
c
t
o
r
s
K
e
e
p
P
r
e
v
i
o
u
s
M
e
s
s
a
g
e
`
 
锁
住
不
回
退
。



-
 
*
*
P
1
-
2
3
（
A
b
o
r
t
W
i
t
h
R
e
p
o
r
t
P
o
l
i
c
y
）
*
*
：
修
法
：
t
r
y
A
c
q
u
i
r
e
 
之
后
的
逻
辑
用
 
s
u
b
m
i
t
t
e
d
 
标
记
 
+
 
f
i
n
a
l
l
y
 
兜
底
，
凡
没
成
功
提
交
 
d
u
m
p
 
任
务
（
含
第
二
道
时
间
检
查
 
r
e
t
u
r
n
、
e
x
e
c
u
t
e
 
抛
异
常
）
必
归
还
许
可
；
删
除
原
来
只
在
 
c
a
t
c
h
 
里
 
r
e
l
e
a
s
e
 
的
半
截
保
护
。
测
试
 
2
 
用
例
 
G
R
E
E
N
，
但
如
实
说
明
：
竞
态
窗
口
是
纳
秒
级
，
4
 
线
程
×
2
0
0
 
轮
压
测
在
修
复
前
也
没
能
踩
中
（
s
t
a
s
h
 
回
验
 
2
/
2
 
通
过
）
，
该
测
试
只
能
证
明
"
修
复
后
许
可
不
丢
、
限
流
路
径
不
耗
许
可
"
，
不
能
当
 
R
E
D
 
证
据
—
—
结
论
依
据
仍
是
报
告
直
读
推
演
，
许
可
泄
漏
路
径
（
t
r
y
A
c
q
u
i
r
e
 
成
功
后
第
二
道
检
查
 
r
e
t
u
r
n
）
从
代
码
上
已
封
死
。



-
 
*
*
P
1
-
2
4
（
T
r
a
c
e
I
d
）
*
*
：
修
法
：
掺
入
每
 
J
V
M
 
随
机
、
定
长
 
8
 
位
 
h
e
x
 
的
实
例
段
（
U
U
I
D
 
派
生
）
，
格
式
 
`
T
I
D
-
{
s
e
r
v
i
c
e
I
d
}
-
{
m
i
l
l
i
s
}
-
{
i
n
s
t
a
n
c
e
}
-
{
s
e
q
}
`
，
j
a
v
a
d
o
c
 
的
 
"
R
E
Q
-
"
 
笔
误
一
并
修
正
。
R
E
D
 
实
测
修
复
前
两
实
例
同
毫
秒
输
出
完
全
相
同
；
顺
带
删
除
方
法
内
从
未
使
用
的
 
d
a
t
e
F
o
r
m
a
t
 
死
变
量
。






行
为
变
更
说
明
：



1
.
 
`
A
l
g
o
r
i
t
h
m
U
t
i
l
s
.
r
a
n
d
o
m
A
r
r
`
 
退
化
入
参
从
死
循
环
改
为
抛
 
I
l
l
e
g
a
l
A
r
g
u
m
e
n
t
E
x
c
e
p
t
i
o
n
（
l
e
n
<
2
 
或
 
m
a
x
V
a
l
u
e
<
2
）
；
数
组
值
分
布
改
为
"
相
邻
不
等
的
前
值
+
随
机
偏
移
"
模
型
。



2
.
 
`
P
a
g
e
D
T
O
.
g
e
t
P
a
g
e
`
 
的
 
o
r
d
e
r
 
分
布
变
化
：
第
一
项
进
 
`
P
a
g
e
.
o
r
d
e
r
`
，
第
二
项
起
进
 
`
P
a
g
e
.
o
r
d
e
r
s
`
（
修
复
前
全
部
进
 
o
r
d
e
r
s
）
；
字
段
名
自
动
 
t
r
i
m
。
下
游
 
J
P
A
C
r
i
t
e
r
i
a
Q
u
e
r
y
 
从
"
排
序
被
静
默
丢
弃
"
变
为
"
排
序
生
效
"
，
这
正
是
修
复
目
的
。



3
.
 
`
F
s
t
U
t
i
l
s
`
 
未
加
 
-
-
a
d
d
-
o
p
e
n
s
 
时
从
 
E
x
c
e
p
t
i
o
n
I
n
I
n
i
t
i
a
l
i
z
e
r
E
r
r
o
r
 
变
为
带
指
引
的
 
I
l
l
e
g
a
l
S
t
a
t
e
E
x
c
e
p
t
i
o
n
（
可
捕
获
）
。



4
.
 
`
S
n
o
w
f
l
a
k
e
I
d
`
 
虚
拟
时
间
毫
秒
的
 
s
e
q
u
e
n
c
e
 
从
 
2
0
4
8
 
起
步
（
正
常
毫
秒
不
变
）
；
I
D
 
仍
单
调
、
单
进
程
内
仍
唯
一
。



5
.
 
`
T
r
a
c
e
I
d
`
 
串
格
式
多
了
实
例
段
（
长
度
 
+
9
 
字
符
）
，
消
费
方
只
按
整
串
匹
配
不
受
影
响
。



6
.
 
`
M
a
t
h
U
t
i
l
s
`
 
d
o
u
b
l
e
 
入
口
精
确
度
提
升
到
十
进
制
字
面
量
语
义
（
2
.
6
7
5
→
2
.
6
8
）
，
依
赖
旧
截
断
行
为
的
调
用
方
会
看
到
末
位
差
异
—
—
这
属
于
修
 
b
u
g
 
的
预
期
变
化
。






全
量
验
证
：
`
m
v
n
 
-
p
l
 
c
o
m
m
o
n
s
-
l
a
n
g
 
t
e
s
t
`
 
1
7
2
/
1
7
2
（
基
线
 
1
2
2
 
+
 
本
批
新
增
 
5
0
）
；
`
i
n
s
t
a
l
l
`
 
后
 
c
o
p
i
l
o
t
-
o
r
m
/
c
o
p
i
l
o
t
-
w
e
b
/
c
o
p
i
l
o
t
-
j
s
o
n
/
c
o
p
i
l
o
t
-
c
a
c
h
e
/
c
o
p
i
l
o
t
-
s
e
a
r
c
h
/
c
o
p
i
l
o
t
-
n
e
t
w
o
r
k
i
n
g
 
编
译
通
过
。



### P1 批次修复记录（2026-09-23，23 条，P1-22/P1-25 按作者裁决不修）

流程：每条先写测试跑出 RED，再修生产代码到 GREEN。测试类（均在 `src/test/java` 下）：
`utils/DateFormatCacheTest`（9 用例，P1-1/2/3/16 + P2-36）、`utils/PrimitiveAndTransformerFixTest`（6 用例，P1-4/5/6/7/15/17）、`utils/IOChannelAndCloseTest`（7 用例，P1-11/12/13）、`resource/YamlReaderDrillDownTest`（4 用例，P1-14）、`utils/MathUtilsRoundTest`（4 用例，P1-18）、`utils/MiscBatchFixTest`（12 用例，P1-8/9/19/21/24）、`dto/PageDtoOrderTest`（4 用例，P1-20）、`utils/SnowflakeIdVirtualSequenceTest`（2 用例，P1-10）、`concurrent/AbortWithReportPolicyGuardTest`（2 用例，P1-23）。

RED 证据与修法要点：

- **P1-1/P1-2（DateFormatterHolder + SimpleDateFormatHolder）**：RED 实测——`withZone` 版拿到的是没带时区的同一个实例；`(yyyy-M, ENGLISH)` 先建后，无 locale 入口 `formatFor("yyyy-M")` 输出 `2026-09-22 Tue`（命中了英文实例，键碰撞）。修法：缓存键把 pattern/locale/timeZone/长度全量拼进 distinctKey；`withZone` 结果真正放入缓存；`SimpleDateFormat` 非线程安全，依赖 ThreadLocal 每线程独占缓存保证（评审复核修正表述：并非"带 zone 入口每次新建"——8 线程×2 万次并发 format/parse 实测 0 错，同键同线程恒返回同一实例）。
- **P1-3/P1-16/P2-36（DateConstants）**：RED 实测——`TIME_ZONE_LOCALE_HASH_MAP.get("GMT")` 恒 null（键是 TimeZone 对象、取用的是 String，15 个单元格从未命中过）；`PT_DATE_EN` 四条正则源码里 `\d` 单反斜杠是非法转义、编译期即报错（中途一度把 `\\d` 改回 `\d` 方向修反，最终以磁盘 repr 核对四处都是 `\\d`）。修法：键改 String（getID），表 `Collections.unmodifiableMap` 封死并发写；正则转义修正后 `12-25-2020` 解析通过（顺带覆盖 P1-16 附带发现：`FMT_DATE_FORMAT_EN` 与正则的分隔符统一为连字符）。
- **P1-4/P1-17（ValueHandlerFactory）**：RED 实测——Float 值走 DoubleValueHandler 抛 ClassCastException；`"true"` 转 Boolean 得 false（`Boolean.getBoolean` 是读系统属性）。修法：Float 走 `doubleValue()`；改 `Boolean.parseBoolean(trim)`。
- **P1-5（UrlParts.paramMap）**：RED 实测——`a=x=1&b=2` 里 `a` 整条被丢弃。修法：按第一个 `=` 切分，值里的 `=` 保留。
- **P1-6（RegexUtils.teardown）**：RED 实测——`host:8/path` 解析后端口识别不出、`8/path` 残留在 path。修法：端口正则 `\d{2,}` 改 `\d+`（正文+注释共 5 处）。
- **P1-7/P1-15（PrimitiveUtils）**：RED 实测——`toWrapper(double.class)`/`char.class` 返回 null（分支漏写/笔误）；`toBigDecimal` 对 BigInteger 溢出截断、对 Double 值返回 0、对小数文本抛异常。修法：补分支、修笔误；toBigDecimal 按 Number/CharSequence 分派走 `new BigDecimal(String)`。
- **P1-8（FstUtils）**：RED 实测（探针）——JDK 21 首触 `toBytes` 即 `ExceptionInInitializerError`，根因 `InaccessibleObjectException: module java.base does not "opens java.lang"`。修法（报告选项 c）：懒初始化+捕获，抛带 `--add-opens java.base/java.lang=ALL-UNNAMED` 指引的 IllegalStateException，只尝试一次不反复重试；javadoc 写明运行前提。
- **P1-9（AlgorithmUtils.randomArr）**：RED 实测——`randomArr(1,10)` 在 main 线程死循环 421 秒（jstack 看到栈顶 `AlgorithmUtils.randomArr:28`），SAME_THREAD 的 @Timeout 中断不了，测试改 `ThreadMode.SEPARATE_THREAD` 才能报 Timeout。修法：`len<2` 或 `maxValue<2` 抛 IllegalArgumentException；去重改"前值+非零随机偏移取模"一次成型（不再重采样）。
- **P1-10（SnowflakeId 虚拟时间）**：RED 由 stash 回验——同一用例在修复前测得虚拟毫秒的 sequence 从 0/小值起步。修法：虚拟推进的毫秒 sequence 从 2048（半程）起步而非 0，与未经历回拨、同 workerId 的对端进程拉开 2048 个号的距离；正常模式仍从 0 起步（另一用例锁住）。既有 SnowflakeIdTest 8 用例全过，虚拟模式单进程唯一性不回退。
- **P1-11/P1-12/P1-13（IOUtils）**：RED 实测 6 例——ByteChannel 短读返回整 1024 缓冲（尾部 0 当数据；评审复核更正：真实 FileChannel 单次 read 会读满返回全部剩余，截断只发生在分段短读的通道上，旧类实测 1MB FileChannel 反而不截断，本会话初稿"2MB 通道只读回 1024"的说法不准确）；`createParentDir(Paths.get("out.txt"))` 抛 NPE；`readAsString(in,false)` 之后 in 已被关。修法：ByteChannel 循环读到 -1 且拷贝实际长度；getParent() 判空；不把包装器 BufferedInputStream/Scanner 放进 try-with-resources（Scanner.close() 对 Closeable 源同样连带关流——RED 阶段实测踩到，第一次修漏了这层）。
- **P1-14（YamlReader）**：下钻逻辑抽为包级静态 `drillDown(List, path)` 脱盘单测（RED 阶段构造入参即编译失败属预期红灯；stash 回验语义：高优先级文件只有半个前缀时完整值取不到）。修法：某源下钻失败换下一优先级源，全部试完才返回 null；中途标量不再 ClassCastException。`YamlReaderTest` 原 3 用例（依赖真实 yaml 文件）保持通过。
- **P1-18（MathUtils）**：RED 实测——`round(2.675,2)=2.67`、`mul(1.1,1.1,20)=1.2100000000000002`。修法：11 处 `new BigDecimal(double)` 全改 `BigDecimal.valueOf`（`new BigDecimal("1")` 字符串构造不动）；测试锁住 `round(2.567,2)=2.57` 等普通值不回退。
- **P1-19（DynamicUtils）**：RED 实测——`{gone:null, kept:1L}` 抛 CME；null 在最后一项时入参 Map 被悄悄删键。修法：过滤进新 LinkedHashMap，入参不动（`DynamicUtilsTest` 原 3 用例含单 null 键场景保持通过）。
- **P1-20（PageDTO.getPage）**：RED 由 stash 回验——`order="id:asc, create_time:desc"` 时 `page.getOrder()` 恒 null、第二项字段名带前导空格（4 用例 3 挂 1 错）。修法：字段名 trim；首项 `page.setOrder(...)`、其余进 orders（按 Page 注释"orders 是第二三级"的语义，copilot-orm 的 JPACriteriaQuery/NativeSqlQueryBuilder 两个消费方不再看到不同的排序集）。
- **P1-21（异常族）**：RED 实测——BusinessException 四参构造 `getMessage()` 返回 null；`new ApplicationException(io).getMessage()` 恒 "Internal Server Error"；ServiceException 三参构造丢 template。修法：补 message 赋值与 super 调用、ApplicationException(Throwable) 用 cause 的消息、ServiceException 补 msgTemplate 字段+getter（与 BusinessException 同名构造器行为拉齐）。既有构造器行为用 `testNoArgConstructorsKeepPreviousMessage` 锁住不回退。
- **P1-23（AbortWithReportPolicy）**：修法：tryAcquire 之后的逻辑用 submitted 标记 + finally 收束，凡没成功提交 dump 任务（含第二道时间检查 return、execute 抛异常）必归还许可；删除原来只在 catch 里 release 的半截保护。测试 2 用例 GREEN，但如实说明：竞态窗口是纳秒级，4 线程×200 轮压测在修复前也没能踩中（stash 回验 2/2 通过），该测试只能证明"修复后许可不丢、限流路径不耗许可"，不能当 RED 证据——结论依据仍是报告直读推演，许可泄漏路径（tryAcquire 成功后第二道检查 return）从代码上已封死。
- **P1-24（TraceId）**：修法：掺入每 JVM 随机、定长 8 位 hex 的实例段（UUID 派生），格式 `TID-{serviceId}-{millis}-{instance}-{seq}`，javadoc 的 "REQ-" 笔误一并修正。RED 实测修复前同毫秒两实例输出可完全相同；顺带删除方法内从未使用的 dateFormat 死变量。

行为变更说明：
1. `AlgorithmUtils.randomArr` 退化入参从死循环改为抛 IllegalArgumentException（len<2 或 maxValue<2）；数组值分布改为"相邻不等的前值+随机偏移"模型。
2. `PageDTO.getPage` 的 order 分布变化：第一项进 `Page.order`，第二项起进 `Page.orders`（修复前全部进 orders）；字段名自动 trim。下游 JPACriteriaQuery 从"排序被无声丢弃"变为"排序生效"，这正是修复目的。
3. `FstUtils` 未加 --add-opens 时从 ExceptionInInitializerError 变为带指引的 IllegalStateException（可捕获）。
4. `SnowflakeId` 虚拟时间毫秒的 sequence 从 2048 起步（正常毫秒不变）；ID 仍单调、单进程内仍唯一。
5. `TraceId` 串格式多了实例段（长度 +9 字符），消费方只按整串匹配不受影响。
6. `MathUtils` double 入口精确度提升到十进制字面量语义（2.675→2.68），依赖旧截断行为的调用方会看到末位差异——这属于修 bug 的预期变化。

全量验证：`mvn -pl commons-lang test` 172/172（基线 122 + 本批新增 50）；`install` 后 copilot-orm/copilot-web/copilot-json/copilot-cache/copilot-search/copilot-networking 编译通过。
---


### 评审发现处置表（P1 批次，2026-09-23，3 个独立评审子代理）

评审范围：P1 批次 23 条修复的 git diff + 9 个新测试。共 30+ 条发现，逐条实测复核后处置如下。新增回归测试统一放 `utils/ReviewRound2FixTest`（9 用例，全部对应下表"本轮已修"项）。

| # | 发现（来源） | 复核结论 | 处置 |
|---|---|---|---|
| 1 | 显式 format 的 parse 入口未接 normalizeFractionalSeconds，`.456789`+SSS 仍错成 10:28:06.789（评1-P1a） | 属实，探针复现 | 已修：5 个 `DateUtils.parse(source, format...)` 入口全部接入规整；ReviewRound2FixTest 锁住 |
| 2 | dash 家族修活后 `toLocalDate("13-45-2020")` 抛原生 DateTimeParseException，copilot-json 反序列化链转成无信息量异常（评1-P1b/P2） | 属实 | 已修：4 个 dash 分支包进 parseEnDash，越界统一抛 UnsupportedLocalDateFormatException（与 HEAD 异常类型一致）；parse(Date 链) 维持 lenient 风格不另动（与 ISO 家族既有行为一致，改动属语义变更需单独评审） |
| 3 | DTF 默认入口用 JVM 默认 locale 与 javadoc(承诺 CHINA)和孪生类相反（评1-P2） | 属实 | 已修：显式 `ofPattern(pattern, Locale.CHINA)`；键标记 default/derived 改 `!default!`/`!derived!` 防语言标签撞键（评1-nit） |
| 4 | (pattern,locale) 入口键写死 no-zone 但实例带 JVM 默认时区，改默认时区后缓存过期（评1-P2） | 属实 | 已修：DTF/SDF 两类的该入口键都改为带 `TimeZone.getDefault().getID()` |
| 5 | 修复记录"带 zone 入口每次新建"与代码不符（评1-P2） | 属实 | 已修：表述改为 ThreadLocal 每线程独占（评审 8线程×2万次实测 0 错） |
| 6 | DateFormatCacheTest 弱断言在 en_US 必挂（评1-P2） | 属实 | 已修：发现 3 修复后断言升级为"等于中文输出"，`-Duser.language=en` 下复跑 9/9 通过 |
| 7 | FstUtils 异常文案只给 java.lang 一个 opens，实测照做仍失败（评2-P1） | 属实（评审逐级 opens 实跑到 11 个才成功） | 已修：文案改为实测的 11 项完整 opens 清单 |
| 8 | toByteArray(ByteChannel) 恒 0 读永久自旋（评2-P2） | 属实 | 已修：连续 1 万次 0 读抛 IOException；偶发 0 读后恢复给数据的路径实测不误杀 |
| 9 | createParentDir 对不带目录的相对文件名 return false 与 javadoc 矛盾，copy 无声失败（评2-P2） | 属实 | 已修：采纳评审方案 toAbsolutePath().getParent()（仅根路径仍判空），copy 不带目录的相对文件名实测可写入 |
| 10 | toBigDecimal(Float) 走 doubleValue 放大噪声（1.1f→1.100000023841858）（评3-P2） | 属实 | 已修：Float 走 `new BigDecimal(Float.toString(f))`；NaN/Infinity 抛 NumberFormatException 维持不变 |
| 11 | ApplicationException(Throwable) 在 cause 无消息时 message 仍 null（评3-P2） | 属实 | 已修：无消息时兜底 cause.toString()，保留异常类型信息 |
| 12 | ServiceException 新增字段致计算 UID 漂移（评3-P2） | 属实 | 已修：显式声明 serialVersionUID=3645985928446181182L（即原计算值），后续加字段不再破坏反序列化 |
| 13 | 空 order 段(" "、",id:asc")现在把 orderBy="" 塞进 page.order，JPA 入口 root.get("") 抛查询期异常（评3-P2） | 属实 | 已修：trim 后为空的段跳过不产出排序项（JPA 入口维持修复前"整条忽略"语义） |
| 14 | 提交失败时 lastPrintTime 照样推进，10 分钟窗口被无谓吃掉（评2-P2） | 属实 | 已修：时间戳推进移到 execute 成功之后；新增确定性用例（反射关停执行器→提交必抛→permits=1 且时间戳不推进） |
| 15 | AbortWithReportPolicyGuardTest 无 RED 能力（评2-P2，修复前压测同样通过） | 属实 | 部分处置：发现 14 的新用例提供确定性验证（不依赖竞态）；原压测用例保留作"修复后许可不丢"的行为锁；泄漏路径封死性以代码直读+评审逐行副本强制交错(修复后 permits=1/修复前 0)为准 |
| 16 | 虚拟时间过渡毫秒(==lastTimestamp)仍用低位 sequence（评2-P2） | 属实但属残余 | 不修：该毫秒挂钟已真实走过，对端在挂钟到达前不会用到它，且本进程从当前值连续发号不改变与"未来虚拟毫秒"(已错开)的关系；收紧它要牺牲正常模式语义。报告留痕 |
| 17 | 虚拟毫秒容量减半，>2.048M/s 持续发号时虚拟时间跑赢挂钟的裕度变差（评2-P2） | 推演成立，属收紧型取舍的已知代价 | 不修（记录）：触发前提是回拨后以接近极限速率持续发号满 60 秒，报告 Javadoc 已声明超限人工介入；若未来实测碰到再改低起点(如 1024) |
| 18 | JPACriteriaQuery 仍只读 getOrder()，第二级排序在 JPA 入口丢弃；PageDeserializer 同样只填 orders；Boolean "1"/"Y" 仍 false；MathUtils.round 负 precision 不校验；DoubleValueHandler 缺 Double/String 分支；toInt(BigInteger) 溢出未修；TIME_ZONE_LOCALE 表别名 ID 查不到；PT_DATE_EN_8 与注释样例不符；Scanner close 传染的测试写法等 | 属实但超出本轮点名范围 | 记入待办不改（按编号另行处置）；仅 PT_DATE_EN_8 顺手修了正则(\d{1}→\d{1,2})——它属于 P1-16 同一张表的位数笔误 |

行为变更补充（在 P1 批次修复记录之上）：DateFormatterHolder 默认入口从"JVM 默认 locale"固定为 CHINA（兑现 javadoc，en/fr 机器输出变为中文月份名/星期名）；(pattern,locale) 入口键含默认时区后，运行期改默认时区的调用会拿到新时区实例（旧行为是拿过期实例）。

二轮验证：`mvn -pl commons-lang clean test` 182/182（新增 ReviewRound2FixTest 9 用例 + GuardTest 确定性用例 1 个）；en_US 语言环境下 DateFormatCacheTest/ReviewRound2FixTest/DateUtilsTest 31/31；`install` 刷新 jar，下游 6 模块编译通过。

## 四、P2 级问题（39 条）

### P2-1 AbortWithReportPolicy 的线程 dump 内容被 SLF4J 丢弃 [实测] ✅已修复(2026-09-23)

- 位置：`concurrent/AbortWithReportPolicy.java:121`（`log.error("thread dump info:", sb.toString())`）
- 现象：探针确认输出只剩 `[thread dump info:]`——SLF4J 的 `error(String, Throwable)` 重载匹配失败后落到 `error(String, Object)`，format 里没有 `{}` 占位符时第二个参数直接不输出。这个类唯一的工作产出（线程快照）从来没有进过日志。
- 修法：`log.error("thread dump info:\n{}", sb)`。

### P2-2 Concurrent.IO_POOL 核心数=最大数 + 无界队列：积压只会被内存放大 [直读] ✅已修复(2026-09-23)

- 位置：`concurrent/Concurrent.java:55、:94-99`（`new ThreadPoolExecutor(2N+1, 2N+1, 0L, MILLISECONDS, new LinkedBlockingQueue())`）
- 现象：`LinkedBlockingQueue` 无参构造容量 `Integer.MAX_VALUE`，maximumPoolSize 永不生效、拒绝策略永不触发；下游变慢时任务无限积压直到 OOM。作为全框架共用的默认异步池，这是放大器。
- 修法：给队列设上限（与框架内其它池一致的 2600 起步）并配 `AbortWithReportPolicy`。

### P2-3 TokenBucketRateLimiter 补令牌 CAS 失败时本轮增量作废 [直读] ✅已修复(2026-09-23)

- 位置：`ratelimit/TokenBucketRateLimiter.java:56-66`
- 现象：`tokensToAdd` 先从 `pendingRefillMillis` 扣掉，`compareAndSet` 失败（说明有并发 acquire）就直接返回——这批令牌丢失，实际放行速率低于配置值。
- 修法：CAS 失败时把 `tokensToAdd` 退回 `pendingRefillMillis`，或改用 `accumulateAndGet` 一步完成。另外该类每个实例自带一个调度线程池且没有 `finalize`/`Cleaner` 保护，用完必须显式 `shutdown()`（接口 `RateLimiter` 层面可加 `AutoCloseable`）。

### P2-4 DateUtils.dateDiff(Date, Date) 是"整除 24 小时"不是"相差几天" [实测] ✅已修复(2026-09-23)

- 位置：`utils/DateUtils.java:987-991`（`TimeUnit.DAYS.convert(millisDiff, MILLISECONDS)`）
- 现象：相隔 23 小时的两个 Date 返回 0；javadoc 写"相差多少天"，同族的 `dateDiff(LocalDate, LocalDate)` 却是按日历天算——两个重载语义不同但名字相同。
- 修法：Date 版内部转 `LocalDate`（按系统或入参时区）再 `ChronoUnit.DAYS.between`，与 LocalDate 版保持一致。

### P2-5 UrlResource 的 Basic 认证用了 URL-safe Base64 字母表 [直读] ✅已修复(2026-09-23)

- 位置：`io/UrlResource.java:219-220`（`Base64.getUrlEncoder()`）
- 现象：RFC 7617 规定 Basic 凭据用标准字母表（`+/`）；url-safe 表输出 `-_`，严格的服务端会解码失败，表现为间歇性 401（是否触发取决于用户名/密码字节里恰好出现索引 62/63 的字符）——极难排查。
- 修法：改 `Base64.getEncoder()`。

### P2-6 IOUtils 多处 `catch (Exception e) { log.warn(e.getMessage()); return 部分结果; }` [直读]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:148-156`（`readFileAsString(InputStream)`，读一半失败返回半截内容 + 只打 message 不带堆栈）、`:381-383`（close 链里 `catch (Exception e) { }` 空处理）等 17 处
- 现象：调用方拿到"看起来成功"的截断结果，故障现场只剩一行 message。
- 修法：IO 读取失败应抛 `IORuntimeException`（包里已有）而不是返回半截；至少把异常对象带进日志。

### P2-7 PropertyReader.getInt(property, defaultValue) 把配置里的真实值 -1 当成"没有值" [实测] ✅已修复(2026-09-23)

- 位置：`resource/PropertyReader.java:123-129`（带默认值的重载复用 `getInt(property)` 再用 `value == -1 ? defaultValue : value`）
- 现象：探针验证 `probe.neg=-1` 时 `getInt("probe.neg", 5)` 返回 5（真实值被默认值顶掉）；`getInt("probe.pos", 5)` 返回 7（正常值不受影响）。
- 修法：带默认值的重载直接读原始字符串自行解析（属性不存在/空/解析失败 → defaultValue，其余原样返回），不要用 -1 当哨兵。

### P2-8 PropertyReader 的两个 FileInputStream 永不关闭 [实测] ✅已修复(2026-09-23)

- 位置：`resource/PropertyReader.java:67`、`:75`（`new PropertyResourceBundle(new FileInputStream(...))`）
- 现象：`PropertyResourceBundle` 没有 close()（JDK 21 javap 确认），交出去的 FileInputStream 构造完就没人管，句柄等 Cleaner/GC 延迟释放。按 readme 建议静态复用时影响有限，但 `TransportClientFactory`、`RestSupport` 这类"每次启动 new 一个"的用法会在 GC 前累积句柄。
- 修法：`try (FileInputStream in = new FileInputStream(file)) { bundle = new PropertyResourceBundle(new InputStreamReader(in, UTF_8)); }`——内容在构造器里已读完，出块即关安全。

### P2-9 SerializeUtils / KryoUtils / ProtostuffUtils 把异常降级成 null，且三者空值语义互不一致 [实测+直读]  ✅已修复(2026-09-23, 见 SerializeP2FixTest)

- 位置：`utils/SerializeUtils.java:35-40、:54-59`；`utils/KryoUtils.java:82-83`（`toObject(null)` 抛 NPE）；`utils/ProtostuffUtils.java:50-54`（`toBytes(null)` 返回 `byte[0]`，`toObject(byte[0])` 返回字段全默认值的对象而不是 null）
- 现象：探针确认 `SerializeUtils.deserialize(垃圾字节)` 返回 null、`serialize(不可序列化对象)` 返回 null——调用方分不清"值本来就是空""字节损坏""对象不可序列化"三种情况；缓存里存坏时会表现成"值不存在"继续往下走。`FstUtils.toObject(null)` 返回 null、`KryoUtils.toObject(null)` 抛 NPE——同一门面族行为不一致。
- 修法：反序列化失败抛 `SerializeException`（包内已有异常族），null 语义只在入参为 null 时返回并三个类统一。

### P2-10 IOUtils.copy(Path, OutputStream) 写失败时输入流不关闭 [直读]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:1006-1014`（`inputStream.close()` 在 while 循环之后，无 finally）
- 现象：`out.write` 抛 IOException（磁盘满、连接断）时输入流句柄泄漏到 GC。
- 修法：try-with-resources 包住 `Files.newInputStream(path)`。

### P2-11 IOUtils.merge 是追加语义，合并到已存在文件不会清空目标 [实测]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:399-413`（`new FileOutputStream(new File(destFile), true)`）
- 现象：目标原有内容 `OLD`，合并后为 `OLDAB`。方法名没有表达"追加"，重复调用会把文件越拼越长。
- 修法：改覆盖语义或改名 `appendMerge` 并在 javadoc 写明。

### P2-12 IOUtils.tempFile(fileName, null) 生成的文件名带 "null" 字样 [实测]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:1166-1173`（`:1172` `fileName + suffix` 字符串拼接）
- 现象：探针确认返回 `...\wprobenull`。
- 修法：`String name = suffix == null ? fileName : fileName + suffix;`

### P2-13 IOUtils.readFileAsString 读不到文件返回空串，且用平台默认字符集 [实测]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:167-185`、`:141`（`Scanner(file)` 无 charset 参数 → `Charset.defaultCharset()`）
- 现象：文件不存在 → 返回 `""`，调用方无法区分"空文件"与"不存在"；探针在 `-Dfile.encoding=GBK` 下读 UTF-8 中文文件得到乱码，而同类 `readFile(Path)`（固定 UTF-8）正确——同包内编码策略不一致，部署环境换 platform encoding 才暴露。
- 修法：不存在时返回 null 或抛异常；`new Scanner(file, StandardCharsets.UTF_8)`。

### P2-14 FileUtils.isImage 消耗调用方的输入流且不重置 [实测]  ✅已修复(2026-09-23, 见 FileUtilsP2FixTest)

- 位置：`utils/FileUtils.java:121-127`（`ImageIO.read(inputStream)`）
- 现象：探针用 69 字节 PNG 验证：调用 `isImage` 后原流只剩 16 字节可读——"先判断是不是图片、再保存同一个流"的上传流程会保存出残缺文件。
- 修法：内部 mark/reset（必要时包 BufferedInputStream），或 javadoc 写明"会消费流"。

### P2-15 KryoUtils 里"Fix the NPE bug"注释是空操作 [直读]  ✅已修复(2026-09-23, 见 SerializeP2FixTest)

- 位置：`utils/KryoUtils.java:52-53`（`kryo.getInstantiatorStrategy();` 调了 getter、返回值丢弃）
- 现象：注释声称修复了集合反序列化 NPE，实际什么配置都没生效；误导后续维护者。
- 修法：删掉，或真的 `kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()))`。

### P2-16 IOUtils.readClasspathFileAsInputStream 未命中时会扫描整个 classpath [实测]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:528-539`（未命中退化为 `classpath*:/**/文件名` 通配查询）
- 现象：本机 43 项 classpath 下，一次未命中 144ms、平均 24ms/次；`SnowflakeId` 构造、各配置读取的回退路径都会踩。
- 修法：把"未命中"结果缓存；或先 `ClassLoader.getResource` 精确查，再退通配。

### P2-17 ArrayUtils.nonNull 返回 Object[]，按 T[] 接收抛 ClassCastException；且行为与名字不符 [实测]  ✅已修复(2026-09-23, 见 ArrayGenericResourcesP2FixTest)

- 位置：`utils/ArrayUtils.java:29-38`
- 现象：`String[] a = ArrayUtils.nonNull("b","a","a")` 抛 `[Ljava.lang.Object; cannot be cast to [Ljava.lang.String;`；改用 Object[] 接收能跑，但结果是 `[a, b]`——一个叫"nonNull"的方法悄悄做了排序+去重。仓库内唯一调用点 `copilot-orm/.../QueryUtils.java:191` 把它塞进 `Map<String,Object>` 不会当场触发，但任何 `String[] a = ArrayUtils.nonNull(...)` 式接收就会抛 ClassCastException。
- 修法：`stream(args).filter(Objects::nonNull).toArray(size -> (T[]) Array.newInstance(componentType, size))`，去掉 sorted/distinct（要排序去重另行提供方法）。

### P2-18 GenericUtils.getTypeArgument 只看第一个泛型接口，嵌套泛型抛 ClassCastException [实测]  ✅已修复(2026-09-23, 见 ArrayGenericResourcesP2FixTest)

- 位置：`utils/GenericUtils.java:31-47`
- 现象：`class X<T> implements Supplier<T>, Comparable<String>` 返回 null（第一个接口的实参是类型变量，内层 break 后外层也 break，第二个接口不再看）；`Supplier<Map<String,...>>` 直接把 `ParameterizedType` 强转 `Class` 抛异常。
- 修法：外层遍历所有接口找到"非类型变量"的实参再返回；返回类型收窄前判 `instanceof Class`。

### P2-19 Resources.getResourcesFromDirectory 在目录列不出来时抛 NPE [实测]  ✅已修复(2026-09-23, 见 ArrayGenericResourcesP2FixTest)

- 位置：`utils/Resources.java:59` 与 `:89`（`for (File file : fileList)` 未判 `listFiles()` 的 null）
- 现象：`File.listFiles()` 在权限不足/IO 错误时返回 null，直接 NPE。该方法在未命中路径上每次 classpath 扫描都会走（见 P2-16）。
- 修法：`File[] fileList = directory.listFiles(); if (fileList == null) return files;`

### P2-20 YamlReader.loadFirst 解析失败时流不关闭、异常直接抛出 [直读] ✅已修复(2026-09-23)

- 位置：`resource/YamlReader.java:132-138`（`yaml.load(inputStream)` 与 `in.close()` 同在一个 try 里，且只 catch IOException；YAMLException 是 RuntimeException 直接穿透）
- 影响：一个语法错误的 config 覆盖文件会让整个 YamlReader 构造失败，classpath 里的默认配置不会回退。
- 修法：try-with-resources；是否按优先级回退到下一文件需在 javadoc 写明设计取舍。

### P2-21 IOUtils.deleteFile 删除失败也返回 true [实测]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:860-884`（两个重载都是 `catch (IOException e) { log.warn(...) }` 后无条件 `return true;`）
- 现象：探针删除只读文件（Windows 抛 AccessDeniedException）返回 true，随后 `Files.exists()` 仍为 true——调用方以为删掉了。
- 修法：直接 `return Files.deleteIfExists(path);`，catch 里 return false。

### P2-22 StringUtils 四方法问题（format 千分位 / lastN 越界 / padStringWithZeros 方向 / removeAllQuotes 删反斜杠） [实测]  ✅已修复(2026-09-23, 见 StringUtilsP2FixTest)

- `format(template, Object...)`:1210-1223——只有 Long 参数被转字符串绕开 MessageFormat 的本地化，`format("计数:{0}", 1234)` 实测输出 `计数:1,234`（Integer、BigDecimal 都被加千分位）；拼订单号/ID 时数字被改写。修法：`value instanceof Number` 统一 `toString()`。
- `lastN`:1174-1178——`lastN("abc",5)` 实测抛 `StringIndexOutOfBoundsException: Range [-2, 3)`；n 为负同理。同类的 `subStr` 是捕获越界返回 null，两个方法约定相反。修法：`n<=0` 返回 ""，`n>=length` 返回原串。
- `padStringWithZeros`:1307-1321——名字叫"补位"，实测 `padStringWithZeros("123",6)`=`123000`（右补零，数值放大 1000 倍；常见需求是左补零）。仓库内零调用。修法：左补或改名 `appendZerosToTail`。
- `removeAllQuotes`:1004-1010——字符类 `[\\"\\\\]` 同时删反斜杠，实测 `removeAllQuotes("C:\\data\\x")` 返回 `C:datax`（Windows 路径分隔符全丢）；对含转义引号的 JSON 也会先破坏结构。修法：`str.replace("\\\\"", "\\"").replace("\\"", "")` 分两步，不动反斜杠。

### P2-23 MathUtils 数值簇（toDouble NPE / format2Currency 前导零 / div 除零 / toInteger 溢出 / equals 注释相反） [实测+直读]  ✅已修复(2026-09-23, 见 MathUtilsP2FixTest)

- `toDouble(Object,true)`:758-763——实测 `toDouble("abc", true)` 抛 NPE：内层 `toDouble(value)` 对不可转换值返回 null，`0 - v` 对 null 拆箱直接抛 NPE。修法：先 `if (v == null) return null;`。
- `format2Currency`:520-536——模式 `,000` 让整数部分补足 3 位，实测 `format2Currency(5,2)`=`"005.00"`、`(99,2)`=`"099.00"`（12345.5 正常）。修模式改 `#,##0`。`copilot-json` 的 `MoneySerializer:28` 用的就是它。
- `div(double,double,int)`:398-402——`BigDecimal.divide` 对 0 分母抛 `ArithmeticException`，与 add/sub/mul 的"null 归零"风格没有统一契约；至少 javadoc 写明。
- `round(double,int)`:424+ 缺 `precision<0` 校验（`round(1.55,-1)` 按"十位取整"返回 0.0 且不报错，同文件 formatDouble 却有校验）；`toInteger(3000000000L)` 实测 `intValue()` 截断返回 -1294967296 不报错。
- `equals(Long,Long)`:566 javadoc 表写"都为 null 返回 false"，实测返回 true（`longEqual` 的注释与实现一致，说明这份抄错）。改注释。
- 性能附注：`round/format/formatDouble/format2Currency` 每次新建 `DecimalFormat`，子代理实测比复用实例慢约 5 倍（1.7μs vs 0.34μs/次）——热路径可加按 precision 缓存。

### P2-24 DateUtils 簇（milisToNextHour 毫秒 / CTT 忽略参数 / Objects.nonNull 空操作 / null 约定不一致） [实测] ✅已修复(2026-09-23)

- `milisToNextHour`:929-936——只清了分秒没清毫秒，实测返回值毫秒位=999，用它做整点定时的调用点会持续后移最多 999ms。加 `calendar.set(Calendar.MILLISECOND, 0)`。
- `toLocalDateTimeCTT(LocalDate, ZoneId)`:861-864——方法体两次用 `ZONE_ID_SHANG_HAI`，形参 `zoneId` 一次都没用；实测传纽约时区拿回的仍是东八区值。要么按参数实现，要么删参数。
- `:672` 与 `:790` 的 `Objects.nonNull(zoneId);`——该方法只返回布尔不抛异常，写了等于没写，null 时最终在 JDK 内部抛无主语 NPE。改 `Objects.requireNonNull(zoneId, ...)`。
- `toLocalDateTime(LocalDate)`:837-840 传 null 抛 NPE，而同族 `toLocalDate(Date)`、`toLocalDateTime(Date)` 传 null 返回 null——实测同一类三种约定并存。

### P2-25 EnumUtils 两处（lookup 空串抛异常 / Long 比较 intValue 截断） [实测+直读]  ✅已修复(2026-09-23, 见 EnumUtilsP2FixTest)

- `lookup(Class,String)`:361-364——实测 `lookupEnum(DayOfWeek.class, "")` 抛 IllegalArgumentException，而按属性匹配的姊妹重载对空串返回 null；表单没填时一个给 null 一个给异常。统一返回 null。
- `lookupEnum` Long 分支:296-301——`value.intValue() == propertyValue.intValue()`，4294967297L 与 1L 的 intValue 都是 1，会命中错误枚举；BigInteger 分支(:345 区)同病。用 `longValue()`/`equals` 比较。

### P2-26 ValueHandlerFactory 残余三处（Short null / BigDecimal 报错文案 / render 后缀） [实测+直读]

- `ShortValueHandler.convert(null)`:465-467——实测返回 0，同文件其余 handler 全部返回 null；DB 的 NULL smallint 列会变成 0。改 `return null`。
- `BigDecimalValueHandler`:202-224——实测 `convert(100L, BigDecimal.class)` 抛异常且消息写 `to requested type [java.lang.Float]`（复制粘贴痕迹，把人引错排查方向）；Long/Short/String 本可无损转换却直接拒绝。补分支、改 `unknownConversion(value, BigDecimal.class)`。
- `StringValueHandler.render`/`DateValueHandler.render`:262、316——实测 `render("abc")` 返回 `abcF`、日期渲染返回 `Thu Jan 01 ... 1970F`——`+ 'F'` 只该出现在浮点 handler 上。String 应带引号、Date 去掉后缀。

### P2-27 Transformers 与 handler 入口两处（模板 {3} / List+NPE） [实测]

- `Transformers.java:38`——消息模板写了 `{3}` 但只传 3 个参数，实测异常文本结尾是字面量 `to expected type[{3}]`，真正想转的目标类型丢了。改 `{2}`。
- `determineAppropriateHandler(List.class, null)`:661-669——实测 NPE（`GenericTypeInspector` 直接 `field.getType()`）；单参重载对 List 永远返回 null，`Transformers.convert(任何List, List.class)` 必抛"没有合适的 handler"——集合转换能力存在但两个入口都走不到。

### P2-28 ReflectionUtils / ClassUtils 簇（缓存数组共享 / 三处无消息 NPE / 只写不读的缓存） [实测]

- `getDeclaredFields/getDeclaredMethods`:369-376、1074-1093——把缓存数组本体交给调用方；实测两次调用返回同一实例（`a==b` true），任一处 `a[0]=null` 之后全 JVM 后续调用拿到的字段列表就是坏的，故障随机且无法归位。返回副本或用 javadoc 声明只读，并改用 `cache.get(clazz, loader)` 原子加载。
- `getFieldValue(String, Class)`:425-448——对实例字段执行 `field.get(null)`，实测抛一条没有任何消息的 NPE，看不出是"字段不是静态的"。加 `Modifier.isStatic` 断言。
- `findFieldRelaxable`:342-358——`:344` 的 Assert 明确允许 name 为 null，`:345` 却直接 `matcher(name)`；实测按类型查找（不传名）抛 Pattern 内部 NPE，而 `:350` 的 `name == null` 判断是永远走不到的死代码。
- `invokeStatic`:648-676——按实参运行时类型精确 `getMethod`；实测 `invokeStatic("size", Collection.class, new ArrayList<>())` 报 NoSuchMethod（方法明明存在）。精确匹配失败后回退可赋值性查找（同文件 `invokeMethod` 已有该逻辑）。
- `ClassUtils.interfaceMethodCache`:128、1395——全文件只有声明与 `put`，没有任何读取点；昂贵查找每次照跑，缓存纯占内存。要么 `get(method, loader)`，要么删。

### P2-29 FileUtils.cleanFilename 幂等短路放行路径穿越 + toLowerCase 随默认 Locale [实测]  ✅已修复(2026-09-23, 见 FileUtilsP2FixTest)

- 位置：`utils/FileUtils.java:62-64、68`
- 现象：前 12 位形如 `[a-f0-9]{12}_` 就原样返回；实测 `"abcdef123456_../../evil.txt"` 清洗后 `../` 原样保留（若返回值参与拼接存储路径即为路径穿越）。另外 `toLowerCase()` 无 Locale 参数，实测土耳其语环境同一输入生成另一哈希前缀（`I→ı`），"同一文件名总是得到同一结果"的契约被破坏。
- 修法：短路前先校验不含 `..` `/` `\`；`toLowerCase(Locale.ROOT)`。

### P2-30 IOUtils.isExceedLimitSize(File)/isBetweenLimitSize(File) 把整个文件读进堆只为拿长度 [直读]  ✅已修复(2026-09-23, 见 IOUtilsP2FixTest)

- 位置：`utils/IOUtils.java:1581-1601、1647-1663`（`Files.readAllBytes(file.toPath())` 后只用 `data.length`）
- 现象：判断 500MB 上传文件是否超限会先分配 500MB 堆内存，读的过程中文件被追加还会拿到不一致长度；同参数 `(long fileSize, ...)` 重载本来就有正确实现。
- 修法：`Files.size(path)` 直接比较。

### P2-31 分页/排序 VO 簇（totalPages 不重算 / hashCode 与 equals 半套 / DIRECTION 非法值降级） [实测]

- `vo/Page.java:107、169-171`——实测 `setTotalCount(95)` 后 `totalPages=10`，再 `setPageSize(20)` 仍 10（期望 5）：只有 setTotalCount 触发重算。Jackson 按字段顺序先 total 后 pageSize 反序列化时必踩。
- `vo/Page.java:135-138`——`hashCode` 基于全部可变字段但没重写 `equals`：Page 进 HashSet 后调一次 setter 就再也 contains 不到自己。纯 DTO 用默认标识语义即可，删 hashCode。
- `vo/OrderBean.java:69-76`——`DIRECTION.of("xyz")` 实测返回 ASC 不抛异常（只有一条 ERROR 日志），而 `of(null)` 抛 NPE：两种非法输入两种相反策略。统一为抛 `IllegalArgumentException`（`PageDTO:52` 正在用它，前端参数拼错时查询顺序会反）。

### P2-32 UrlUtils.encodeUrl 丢掉 #fragment 与 URL 内凭证 [实测]

- 位置：`utils/UrlUtils.java:26-46`
- 现象：重建 URL 只拼 协议+主机+端口+路径+查询；实测 `https://ex.com/a?q=hello world#frag` 编码后 `#frag` 消失，`https://u:p@ex.com/p` 的用户名密码被整段丢弃（后续请求以未认证身份发出）。另外查询串整体 encode 后再把 `%3D/%26` 换回 `=&`，原值里本来就有的 `%xx` 会被二次编码。
- 修法：用 `URI` 按组件重建并保留 `getRawFragment()`/userInfo，或在 javadoc 写明"片段与凭证会被丢弃"。

### P2-33 限流器簇（LeakyBucket 停机丢任务 / System.out 刷屏 / TokenBucket 无参校验） [直读] ✅已修复(2026-09-23)

- `ratelimit/LeakyBucketRateLimiter.java:68-71、101-115`——`shutdown()` 注释写"处理完队列中剩余请求"，实现是 `isRunning=false` 后 `processRequests` 每 tick 直接 return，桶里已 `submitRequest` 成功（对外已承诺接收）的任务被直接丢弃：不执行、不计数、无回调。修法：拆"停止接收"与"排空"两态，或至少 drain 后 log 剩余数。
- 同文件 `:85`、`TokenBucketRateLimiter.java:108`——库代码用 `System.out`，每个漏出周期一条、永不停；`:48-49` `interval = 1000L/leakRate` 在 leakRate>1000 时被压到 1000/s 且整数截断使实际速率略高于配置。改 slf4j debug。
- 同文件 `:74-84`——请求 poll 出桶后被转投同一个 2 线程 scheduler 的无界队列，`capacity` 不再是实际在途上界，无背压。
- `TokenBucketRateLimiter.java:39-53`——构造器零参数校验：`refillRate<=0` 时令牌只出不进、桶空后全量拒绝且无告警（`LeakyBucketRateLimiter:35-40` 有同款校验，孪生类只修一边）。

### P2-34 CopilotThreadExecutor / CopilotExecutors 两处 [直读] ✅已修复(2026-09-23)

- `concurrent/CopilotThreadExecutor.java:127-131`——`beforeExecute` 每任务调一次 `monitor()`，每次用 `MessageFormat.format` 拼 13 个字段打一条 info：高 QPS 下日志量急剧增长、反过来拖慢被观测的池。改阈值触发/降 debug/参数化日志。
- 同文件 `:108-123`——`shutdownNow()` 的 catch 分支返回 null，破坏 `ThreadPoolExecutor.shutdownNow()` 非 null 契约，调用方 `.size()` 直接 NPE。改返回 `Collections.emptyList()`。
- `concurrent/CopilotExecutors.java:48 vs 124`——javadoc 写"默认CPU核心数的3倍"、代码 `NCPUS * 2`；`:140-143` `maxPoolSizeToCorePoolSize()` 读调用当时的 corePoolSize，链式顺序颠倒（先调它再设 core）会让 core>max、到 `build()` 才抛出无消息 IAE；`:130-133` `maxPoolSize(null)` 不校验、到 `:217` 拆箱才 NPE。犯错点与报错点相隔整条链。

### P2-35 ThreadContext 两处语义陷阱 [直读]

- `context/ThreadContext.java:48-54`——`getResources()` 返回拷贝：`ThreadContext.getResources().put(k,v)` 这种常见写法编译通过、值被丢弃。
- 同文件 `:66-69`——`setResources(emptyMap)` 直接 return，与 javadoc "This operation overwrites everything that existed previously" 相反：想按文档清空只能改用 `remove()`。

### P2-36 DateConstants.TIME_ZONE_LOCALE_HASH_MAP 是 public 可变 HashMap [直读] ✅已修复(2026-09-23)

- `constants/DateConstants.java:392`——`final` 只锁引用，任意调用方可 put/remove/clear 这张全局表，并发写还有结构损坏风险；同类的 Pattern/TimeZone 常量都不可变，唯独它是例外。（它的键类型错用见 P1-3。）改 `Collections.unmodifiableMap` 并降为 private。

### P2-37 FutureResult 两处 [直读] ✅已修复(2026-09-23)

- `concurrent/FutureResult.java:49-52、63-65、78-80`——`InterruptedException` 与业务异常混在一起 catch：`get()` 把中断包装成 `AsyncExecutionException`（上层分不清"失败"与"被取消"），`orElseGet` 直接丢弃中断信号继续走回落分支，且不 `Thread.currentThread().interrupt()` 恢复标记——线程池取消信号在此断链。
- 同文件 `:43-48`——`get()` 里 `t instanceof CompletableFuture ? ((CF)t).get() : future.get()`：`:47` 对同一 future 重复 `get()` 是浪费；而 Supplier 本身返回 `CompletableFuture`（异步套异步的常见写法）会被误当包装层多解一层，语义随数据内容漂移。根因是配合 P1-22 的嵌套返回打的补丁，P1-22 修掉后这里应一并删。

### P2-38 雪花/工作器ID 三处设计风险（部分类注释已披露） [直读]

- `utils/SnowflakeId.java:204-214`——`AUTO_SLOTS` 只 add 从不 remove：同 JVM 内先后构造超过 32 个自动推导实例（测试、多租户重建、热部署）后，即使旧实例早被 GC，槽位也不回收，后续构造永久抛 `IllegalStateException`。改引用计数或 Cleaner 释放。
- 同文件 `:253、:269-281`——`nextId()` 是 `synchronized`，等待时钟回拨的 `sleep(1)` 循环（最长 1 秒）在临界区内执行：一次轻微回拨会把全部取号线程（包括与本次无关的）在同一把锁上排队最多 1 秒。等待移出临界区或直接走虚拟补偿分支。
- `utils/WorkerIdGenerator.java:70-76`——`% 32` 的推导同网段尾段相同/PID 差 32 倍必撞，且跨进程无租约、冲突只打一条 WARN。类注释已如实披露"约1/32概率撞"并要求生产显式配置 workerId，属知情设计，故按 P2 记录：建议把"检测到同机多 JVM 撞号"从 WARN 升级为启动失败。

### P2-39 杂项小缺陷簇 [实测+直读]

- `utils/AlgorithmUtils.java:54`、`utils/TraceId.java:40`——各自有一处"结果被丢弃的死计算"：`traceId()` 每请求做一次 `DateUtils.format(new Date(), ...)` 再扔掉（注释还写 REQ- 前缀、实际 TID-）；`randomArr` 的 `printArray` 用 `System.out`。
- `utils/Types.java:22-37` + `ArrayTypes`——数组类型注册表缺 `boolean[]/short[]/byte[]/char[]`，未命中 `get` 返回 null 且没有任何提示；且 Types 与 ArrayTypes 是两份手写清单、copilot-cache 里还有第三份孪生实现，迟早分叉。遍历 `ArrayTypes.values()` 统一构建。
- `utils/PropertyUtils.java`——整个类是空壳（`public class PropertyUtils {}`）却发布在公共包里，与 commons-beanutils 同名类极易误 import。删除或补实现（final + private 构造）。
- `errors/` 三处——`ErrorTypes` 不走 `AbstractErrorType` 的 8 位码格式校验，同一枚举混着 "0"/"5001"/"400122"，按文档取位的消费方会截出错误组合；`ErrorType.message()` 默认实现返回 null、`message(String)` 默认实现空方法体，异常消息变成空且编译期无提示；`AbstractErrorType` 挂 `@Data` 给常量语义对象加了可变 setter，equals/hashCode 建在可变字段上。
- `exception/` 两处——9 个异常类缺 `serialVersionUID`（同包另 3 个却有，风格不一致）；`BusinessException:52` 的 `Arrays.asList(messageParams)` 存的是定长视图，`getMessageParams().add(...)` 会抛 `UnsupportedOperationException`。
- `utils/FileUtils.java:91`——存储文件名唯一性用 MD5 截 12 位十六进制（48 bit），哈希输入含用户可控文件名，碰撞可构造且后写覆盖前写不报错。
- `constants/DateConstants.java` PT_DATE_EN 次生风险——`FMT_DATE_FORMAT_EN` 系列是 `MM/dd/yyyy`（斜杠）而正则要求连字符：就算把 P1-16 的反斜杠修好，`LocalDate.parse("12-25-2020", DTF_DATE_FORMAT_EN)` 仍会因分隔符不匹配抛 `DateTimeParseException`。修时必须 pattern 与格式串一起统一。


---

## 五、复核后排除或降级说明的条目（防止后续重复排查）

1. "MathUtils.gcd 对 Long.MIN_VALUE 溢出"——**commons-lang 里没有 gcd 方法**，全仓库 grep 零命中，剔除（子代理编造）。
2. "StringUtils 的格式器缓存键会碰撞"——它用的是带 `|` 分隔符的全量键（`distinctKey`），设计正确，未复现；有碰撞问题的是日期两个孪生类（见 P1-2）。
3. "ClassUtils 静态 HashMap 并发不安全"——`commonClassCache`/`primitiveTypeNameMap` 只在 static 初始化块写入、之后只读，无并发问题。
4. "SlidingWindowConcurrentTest 会失败"——本地基线 + 连跑 3 次全绿（16 核机），属子代理环境噪音；`canPass()` 本身有 `synchronized`，逻辑正确。
5. "IOUtils.readAsString(in, false) 之后 in.read() 会抛 Stream Closed"——探针首跑出现自相矛盾（既"未关闭"又"抛异常"），换带 close 计数器的包装流复测：close() 确实被调用（结论成立，已并入 P1-13），但子代理现象描述不可靠——**子代理报告的每个细节都要拿源码和重跑重新对，包括方法名**（另一例：`FstUtils.asBytes` 记错了名，实际是 `toBytes`，用真名重测后结论成立，见 P1-8）。
6. 不计缺陷（readme 已文档化的知情选择）：`getInt(property)` 以 -1 作"缺失"哨兵（readme:66）、`YamlProfileReaders.getBoolean` 不回退主文件（readme:85）、properties 须以 UTF-8 保存（readme:70，GBK 文件读出乱码是文档约定而非 bug，探针复现一致）。注意 P2-7 报的是**带默认值重载也拿 -1 当哨兵**（`readme:66` 恰恰推荐用这个重载来规避哨兵，结果它自己也没幸免），这条不在文档化范围内，仍按缺陷计。
7. 已并入他条不重复计：子代理报 3 第 2/3/4/5/6/8/9/10/11/12 条分别对应本报告 P0-2/P1-11/P1-12/P1-13/P1-14(同源下钻缺陷)/P2-21/P1-15/P2-17/P2-21/P2-19；`Types.arrayTypes` 缺 short[]/byte[]/boolean[] 与 `copilot-cache` 孪生实现分叉，记入第六节建议，不算 commons-lang 自身 bug。

8. 定级取舍：`LeakyBucketRateLimiter`/`TokenBucketRateLimiter`/`RateLimits` 全仓库零业务调用点（仅各自 main/测试引用），子代理按"若被使用则后果如何"给的 P1（漏桶 CAS 丢令牌、shutdown 丢任务）一律降为 P2 记录（见 P2-33、原 P2-3 合并）——限流器是示例性质，与锁/IO 这类在途组件的风险敞口不同档。

---

## 六、覆盖缺口与后续建议

- 本报告由 5 路分包审查 + 本人逐条复核合并：初版 41 条，并入字符串/数学/日期路 21 条、IO/序列化路 10 条、并发/算法/异常路 19 条（去重后）后定稿 **69 条**（P0×5、P1×25、P2×39）。
- 上面 P0/P1 里有 6 条（Kryo、toByteArray 截断、LBQ 死循环、小数秒、append 覆盖、Float 强转）是"错了但不报错"型，正是基座模块最忌讳的形态；它们全部没有测试。修复时每条先补一个能复现错误的仓库内测试（先 RED 后 GREEN），探针代码在 `target/probesrc/` 可作起点。
- `DateFormatterHolder` 与 `SimpleDateFormatHolder` 两个孪生类键拼法各自错、时区逻辑各自废，建议合并成一个门面（内部按 pattern/locale/zone 三元组建缓存），修一次两边都干净。
- `LeakyBucketRateLimiter`/`TokenBucketRateLimiter` 库代码里直接 `System.out` 打印（`:85`、`:108`），按框架自身规范应走 slf4j；连同 `LeakyBucketRateLimiterOld` 这类"Old 后缀死代码"（全仓库零引用）可以一并清理，属低风险改动。
- 本次为纯评审：未改动任何生产代码。报告条目按编号验收，修哪条点哪条。
