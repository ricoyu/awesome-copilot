# commons-lang 代码评审报告（2026-09-28 全量重扫）

模块：`commons-lang`（JDK 21，Spring Boot 3.3.6 体系）。本轮为 2026-09-22 报告的下一轮全量重扫。

## 一、范围与方法

- 上一轮报告（`CODE_REVIEW_REPORT.md`，2026-09-22）69 条逐条对账：64 条确认已修，P1-22（await 失败不抛出）与 P1-25（ThreadLocal 清理时机）为作者裁决保留，P2-35、P2-38 本轮修复，P2-39 剩余 4 处经业主拍板不修（详见第三节）。
- 全模块按包分 6 路独立重扫（IO/文件、字符串/反射/类型、日期/序列化/转换、并发/上下文、限流/配置、移植包/杂项），每路评审人在全新上下文里读代码 + JDK 21 探针实测，全部结论由主代理二次复核（探针复跑或逐行读码），推翻 2 条、降级 3 条。
- 与上游对照：io 包与 Spring 6.1.14 逐行 diff（结论：忠实移植，仅 2 处偏差）；AntPathMatcher/PathMatchingResourcePatternResolver 与上游 v6.1.12 规范化逐行 diff + 2914 项匹配语义探针对照（diff=0）；CopilotLinkedBlockingQueue Spliterator 对照 JDK 21 源码。
- 修复批次全部走 TDD（先失败测试后实现），并由独立评审子代理对最终 diff 复审（Cleaner 捕获链 javap 验证、Windows 文件锁双进程探针、全量 clean 复跑）。
- 测试基线：本轮开始时 307 个测试全绿；修复+补测试后 **55 个测试类、321 个测试，0 失败 0 错误**（`mvn -pl commons-lang clean test`，surefire 报告逐类核对）。

## 二、本轮已修复（用户点名项 + 评审补漏）

| 项 | 内容 | 验证 |
|---|---|---|
| P2-38 雪花 ID ① | 槽位登记 AUTO_SLOTS 只进不出 → `java.lang.ref.Cleaner` 在实例被回收时释放租约并移出登记 | SnowflakeP238FixTest 循环 40 轮构造（>32 槽）通过 |
| P2-38 雪花 ID ② | 跨进程重号 → 槽位认领加文件锁租约层（`java.io.tmpdir/copilot-snowflake-{slot}.lock` 独占锁），租不到顺延下一槽，32 槽全占构造抛异常并提示配置 `copilot.snowflake.worker-id` | 双进程探针实测 Windows 下 tryLock 冲突返回 null、顺延生效 |
| P2-38 雪花 ID ③ | 回拨 ≤50ms 在 `synchronized` 临界区内 `sleep(1)` 等待（最长 1 秒，把无关取号线程全部排在锁上）→ 删除等待分支，改两档：≤60s 立即虚拟时间续发，>60s 抛异常请人工检查 | 断言耗时 <200ms（修复前约 1000ms） |
| P2-38 评审补漏 | 独立评审指出租约只防"自动推导 vs 自动推导"，显式配置实例与自动推导实例撞槽无人检测 → 显式路径构造时也探测租约，冲突 WARN（编号保持配置值，不擅自顺延） | 新用例 testExplicitWorkerIdAlsoAcquiresLeaseButNeverDefers，先 RED 后 GREEN |
| P2-35 ThreadContext | 两处语义陷阱按你指示改 javadoc：`getResources()` 返回的是拷贝（对它 put 不会写进真实绑定）、`setResources(null/empty)` 是空操作；新增契约测试固定两种行为 | ThreadContextContractTest 通过 |
| 无 Locale 大小写 | StringUtils 5 处、OrderBean.DIRECTION、EnumUtils.lookupEnum、SizeUnit、ResourceUtils.isJarFileURL 共 10 余处转 `Locale.ROOT` | 土耳其语 Locale 探针 + LocaleCaseAndLoggingFixTest |
| decodeUrl | `printStackTrace()` → `log.error(..., charset, e)`，与 encodeUrl 同型 | ListAppender 测试验证日志内容 |
| randomStr | 每次 `new Random()`（线性同余，可被观测反推）→ 复用静态 `SecureRandom`；字符集经核对不变（97..122） | StringUtilsRandomStrTest |
| main() 残留 | 删 2 个库类演示 main() + 零引用死类 `LeakyBucketRateLimiterOld`（删除前全仓 grep 零引用） | `grep "public static void main" src/main` 归零 |
| ErrorTypes 注释 | 按业主拍板路 A：码值不动（"0"/"4002"/"5001" 是对外响应契约），把 ErrorType 接口"推荐 7 位"与 AbstractErrorType"强制 8 位"的矛盾注释改成如实描述 | 纯文档，编译通过 |
| AlgorithmUtils 行尾 | 修混合行尾（5 处 `\r\r\n` 归一为 `\r\n`） | 逐字节核对 |

设计取舍已同步写入 `readme.md`「时钟回拨处理」一节（含删除等待分支的原因、虚拟时间代价、租约在 Linux/Windows 的边界差异）。

## 三、业主拍板不修（本轮记录在案）

- P2-39 原 4 处遗留：`AbstractErrorType` 的 `@Data`（暴露 setter）、`ErrorType.message()` 接口默认方法、`FileUtils` 文件名哈希截 12 位、错误码 4/8 位长度不一——**均不修**，对外响应契约保持不变。
- P1-22、P1-25：沿用上轮作者裁决，保留现状。

## 四、本轮新发现（未修，按严重度排列）

编号 R28-*，与上一轮 P0/P1/P2 序列独立。现象→原因→影响→改法。

### P0（2 条）

**R28-P0-1 `DateUtils.parse(source, format)` 非连字符日期的多位数小数秒产出错值且不报错（P0-4 修复没盖住的路径）**
- 现象：探针实测 `parse("20260922 10:20:30.456789", "yyyyMMdd HH:mm:ss.SSSSSS")` 返回 10:28:06——小数秒 456789 被当毫秒整数进位 456 秒；`parse("09/22/2026 10:20:30.45", "MM/dd/yyyy HH:mm:ss.SS")` 毫秒位得 45（应为 450）。
- 原因：`normalizeFractionalSeconds` 第一步要求整串匹配 `PT_ALL`（日期分隔符固定 `-`），不匹配就原样返回，归一化没执行。9-23 修复记录声称 7 个 parse 入口全部接入，实际只修住了连字符族。
- 影响：调用方拿到分钟/秒被改错的 Date，不抛任何异常直接入库——和 P0-4 同级数据污染。
- 改法：归一化不依赖整串匹配，改为"按模式串定位 `.` 后的 S 段，把输入小数秒按位数截断/补零"；给 `parse(source, format)` 补 `yyyyMMdd+.SSSSSS`、`MM/dd/yyyy+.SS` 两个往返测试。

**R28-P0-2 `FileUtils.cleanFilename` 后缀段未消毒，路径穿越从主清洗路径绕过（P2-29 修复不完整）**
- 现象：输入 `evil.pdf/../../etc/passwd` 输出含 `/` 与 `..` 的文件名。
- 原因：只对最后一个点之前的 namePart 消毒，`suffixPart`（最后一个点之后的全部字符）原样拼接；P2-29 修复只封死了"已 clean 前缀短路"一条路。
- 影响：上传文件名完全由攻击者控制，javadoc 声明产物用于拼接存储路径——可写出上传目录、覆盖可写文件，生产可触发。
- 改法：suffixPart 同样执行非法字符替换 + 点折叠，或白名单 `^[a-z0-9_.-]{1,10}$` 不合规则置空。

### P1（17 条）

**IOUtils 一族 5 条（Scanner 与返回值丢弃）**
- **R28-P1-1** `readAsString(InputStream[,autoClose])` 用 Scanner 逐行读：底层流中途 IOException 被 Scanner 内部捕获，`hasNextLine()` 直接返回 false——半截/空内容照常返回，无异常外流（JDK 21 探针实测复现）。与 9-23 在 readFileAsString(InputStream) 上明确消灭的问题是同一形态，此处漏改。改法：改 BufferedReader 循环 + IOException 包装上抛。
- **R28-P1-2** `readFileAsString(String/File)` 两处重载换了 UTF-8 但仍是 Scanner：注释承诺"读取中途失败不再返回半截内容"不成立——catch 分支实际永不可达（Scanner 已内部消化异常），只有 exists() 前置检查起作用。改法同上。
- **R28-P1-3** `readLines(String)` / `readLines(InputStream)`：IOException 只 `log.warn` 返回残缺行列表；文件不存在与文件为空都返回空列表，调用方无法区分；且仍是平台默认字符集。
- **R28-P1-4** `writeTempFile(...)`：丢弃 `write()` 的布尔返回值（write 内部失败只 log.warn 返回 false），磁盘满/权限不足时方法照常返回路径——调用方拿着指向空/不存在文件的路径继续用（典型：导出报表当附件发走），失败转化为下游更难排查的问题。方法签名声明 throws IOException 却从不抛。
- **R28-P1-5** `readClasspathFileAsFile(...)`：对 jar 内资源 `new File(url.getFile())` 产出含 `file:/…jar!/…` 的假路径 File（exists()=false 不报错），后面会正确抛 FileNotFound 的 resolver 分支永远走不到；URL 编码路径（含空格 %20）也不解码。javadoc 恰恰写着"从 jar 里面找"。改法：加 `ResourceUtils.isFileURL` 守卫，非 file 协议交给 resolver。

**反射/分页/URL/字符串 5 条**
- **R28-P1-6** `ReflectionUtils.getFields`：缓存数组本体直接交给调用方（P2-28 只给 getDeclaredFields/getDeclaredMethods 加了 clone）。探针实测两次调用返回同一实例，调用方改一个元素全 JVM 该类字段视图被污染 1 分钟。下游 copilot-search、copilot-orm 都在长期持有该数组。改法：对外返回 clone。
- **R28-P1-7** `PageDTO.getPage`：排序方向未 trim（P1-20 只 trim 了字段名）——类注释自带的官方示例 `"字段1:asc, 字段2:desc"` 变体（逗号后带空格）实测抛 IllegalArgumentException。前端最常见写法直接失败。
- **R28-P1-8** `Page.getFirstResult`：`(pageNum-1)*pageSize` int 乘法溢出为负（实测 200000×20000 → -294987296）。Page 由 REST @RequestBody 绑定、两参数无上限，客户端可稳定构造负 offset——JPA setFirstResult 抛异常变 500，或被驱动当 0 返回第一页。改法：long 计算 + 钳制 + 入参下限校验。
- **R28-P1-9** `StringUtils.split(s, spliter)`：把分隔符当正则用——`split("a.b.c",".")` 返回空数组且无任何提示（`.` 匹配每个字符），`split("a[bc","[")` 抛 PatternSyntaxException。javadoc 承诺的是字面分隔。改法：`Pattern.quote(spliter)`，与 split2List（Guava 字面语义）一致。
- **R28-P1-10** `StringUtils.format`：9-23 的 Number→toString 修复引入回归——模板含 `{0,number,#,##0.00}` 显式子格式时，Number 被提前转成字符串，MessageFormat 无法再消费，实测抛 IllegalArgumentException。修前该用法正常。改法：模板含 `{N,number`/`{N,date` 子格式时保留原始对象。

**日期/枚举/数值转换 4 条**
- **R28-P1-11** `DateUtils` 全部 SimpleDateFormat 保持 lenient（`setLenient` 零命中）：`parse("2026-02-31","yyyy-MM-dd")` 返回 2026-03-03，`"2026-13-45"` 返回 2027-02-14，不报错。而 toLocalDate 对同类非法输入抛异常——同一坏输入两条入口一半拒绝一半给错值。改法：构造时 setLenient(false) + ParsePosition 校验消费到串尾。
- **R28-P1-12** `EnumUtils.lookup(Class, Long/BigInteger)` 按 ordinal 查找：负数进 `ordinal < size` 判断返回错误枚举，超 int 值 intValue 截断返回错误枚举；防越界用的是 `assert`（生产 JVM 默认关闭，实测失效）。Integer 版有真判断，Long/BigInteger 版是把 if 写成了 assert。与已修 P2-25 同型。
- **R28-P1-13** `PrimitiveUtils.toInt`：P1-15/P2-23 批次唯一漏网的孪生方法——`toInt(3.9d)` 返回 0（无 Double 分支）、`toInt(3000000000L)` 返回 -1294967296（intValue 截断不报错）；同文件 toBigDecimal 与 MathUtils.toInteger 已修同样三种形态。
- **R28-P1-14** `ValueHandlerFactory`：①DoubleValueHandler/FloatValueHandler 缺自身类型与 String/整数分支——最普通的恒等转换 `Transformers.convert(1.5d, Double.class)` 实测抛"Unaware how to convert"（P1-4 修复只补了报告点名的 Float→Double 一例）；②IntegerValueHandler 对 Long/BigDecimal/BigInteger 用 intValue 截断——DB bigint 列转 Integer 字段溢出变负数不报错（与已修 P2-23 同型）。

**并发/上下文 3 条（与已修的 P2-35 javadoc 是不同的问题）**
- **R28-P1-15** `ThreadContext` 的 TransmittableThreadLocal 子类只覆盖了 `childValue` 没覆盖 `copy()`：TTL 线程池（TtlRunnable/TtlExecutors）捕获上下文走 copy()，默认实现直接返回父线程 Map 本体——提交线程与池内多个工作线程并发读写同一个可变 HashMap，跨请求上下文互相可见（数据竞争 + 信息泄漏）。本轮 P2-35 按你指示只改了 javadoc，此问题未修。改法：覆盖 copy() 返回 new HashMap<>(parentValue)。
- **R28-P1-16** `TraceThreadPoolExecutor.execute` 包装任务 `finally { MDC.clear(); }`：线程池饱和回落 CallerRunsPolicy 时任务在提交者线程上跑，无条件清空提交者自己的 MDC——该请求后续日志全部丢失 traceId。改法：执行前保存 `MDC.getCopyOfContextMap()`，结束后恢复。
- **R28-P1-17** `YamlProfileReaders.getBoolean`：profile 未命中时缺回落 base 配置的分支（getInt/getString 都有）——application.yml 里显式写 `copilot.mvc.cors.enabled: false`、profile 文件没写该键时，false 被跳过、吃调用方默认值 true，CORS 关不掉。真实调用点 copilot-web/RestUtils.java:52。同类：`spring.profiles.active` 配多值（`prod,canary` 或列表）只按单值拼文件名，profile 覆盖整体失效且无任何提示。

### P2（按包成簇，共 33 条）

**IOUtils / FileUtils / Resources（10）**
| 编号 | 内容 |
|---|---|
| R28-P2-1 | `tempFile(fileName,suffix)` 只拼路径不创建、无唯一性、无 deleteOnExit，与单参版语义相反；javadoc 写"创建"名不副实——同名并发互相覆盖 |
| R28-P2-2 | `write(Path,String[,charset])` 的 `Optional.of(data).orElse("")` 空值防护是死代码：Optional.of(null) 直接抛 NPE，实测无 message |
| R28-P2-3 | `fileCopy(String)` 文件既不在磁盘也不在 classpath 时拿到无 message 的 NPE（requireNonNull 未带说明，readClasspathFileAsInputStream 未命中只 debug 日志） |
| R28-P2-4 | `readClasspathFileAsInputStream(dir,fileName)` 未接入 P2-16 的 60 秒负缓存；fileName 以 `/` 开头时拼出的模式必然查不到仍走全量扫描 |
| R28-P2-5 | `doReadClasspathFileAsInputStream` 查询顺序反了：先递归穷举 java.class.path 全部目录（Spring Boot fat-jar 下必然空手），后做 O(1) 的 classLoader.getResource |
| R28-P2-6 | `merge` 打开目标文件即截断旧内容，中途某块读失败时旧合并产物已毁、目标文件剩半截——改先写临时文件再原子替换 |
| R28-P2-7 | `readCommandLine`：System.in EOF 后 readLine 恒返回 null，循环不退出，consumer 被 null 无限循环喂 |
| R28-P2-8 | `FileUtils.isImage` 的 mark/reset 恢复依赖调用方给的 size 真实；客户端谎报小 size 时 reset 失败被 ignored 空 catch，流位置未复原仍返回布尔——P2-14 契约经不可信 size 参数失效，且无日志 |
| R28-P2-9 | `Resources.getResourcesFromDirectory` 把 getCanonicalPath 的 IOException 包成 `new Error(e)` 上抛，只 catch Exception 的调用方接不住；目录递归无符号链接环防护 |
| R28-P2-10 | `RegexUtils.teardown`：①URL_REGEX host 组不含 userinfo，带凭证 URL 拆成 host=user/path 带残渣（同批 UrlUtils 修过 userinfo，本文件没同步）；②各段全部可选导致任何字符串都 matches，"非法返回 null"守卫失效；③超 int 端口抛运行时异常 |

**字符串/URL/类型（6）**
| 编号 | 内容 |
|---|---|
| R28-P2-11 | `UrlUtils.encodeUrl(null)` 抛指向私有方法内部变量的 NPE；缺 `://` 的 URL（file:/、mailto:）按第 2 个字符错位拆分；`split("&")` 丢尾部空段（与 encodePath 特意保空段的原则矛盾） |
| R28-P2-12 | `UrlUtils` 同一输入里 `%XX` 按已编码透传、`+` 按字面量编成 %2B，两种编码约定混用，form-urlencoded 空格往返丢失 |
| R28-P2-13 | `StringUtils.trimAll` 把待删串当正则：传 "." 删光所有字符不报错、传 "(" 抛 PatternSyntaxException（与方法名语义相反） |
| R28-P2-14 | `RegexUtils` I18N 模板键 `[^\\s]+` 贪婪越过 `}`，`{a}{b}` 提出键 `a}{b`，i18n 查询失败但按模板路径返回空白消息 |
| R28-P2-15 | `UrlParts.paramMap` 单值也是 ArrayList（MultiValueMap 特性），按 Map<String,String> 心智强转必抛 ClassCastException，javadoc 未说明 |
| R28-P2-16 | `ArrayUtils.parseTwoDimensionArray` 无入参守卫："" 抛 StringIndexOutOfBoundsException、null 抛 NPE（姊妹方法 parseOneDimensionArray 有全套守卫）；`EscapeUtils.escape(null)` 抛无 message NPE |

**日期/序列化/转换（7）**
| 编号 | 内容 |
|---|---|
| R28-P2-17 | `DateConstants` 里 d-MMM-yy 家族（P1-16 修复点名的 `15-Sep-18`）正则改对了但没接进任何解析链——实测 `parse("15-Sep-18")` 仍返回 null；同族三件套常量零引用 |
| R28-P2-18 | `DFT_UTC_DATETIME` 模式串（MM/dd/yyyy 开头）与自身注释声称的格式（yyyy-MM-dd 开头）矛盾，注释承诺的 UTC 格式实测解析失败；同区 3 个 UTC 常量零引用 |
| R28-P2-19 | `PT_ALL` 时区段只接受 `+` 偏移：`2026-09-22 10:20:30.456789-0500` 自动识别返回 null（`+0800` 正常、`-05:00` 走 ISO 支正常）——同一形态三种结果，至少要在文档写明 |
| R28-P2-20 | 两个日期 holder 的 ThreadLocal 清理入口 clearThreadLocal 生产代码零调用（只有测试在用）；DateConstants.SDT_GMT 连清理入口都没有；Date 与 LocalDateTime 双自动识别链对多空格宽容度相反（parse 成功、toLocalDateTime 抛异常） |
| R28-P2-21 | `MathUtils`：toLong 解析失败分支忽略 defaults 参数返回 null（与 blank 分支回默认值语义相反）；add/sub/mul/div 缺 precision<0 校验（同文件 round 已修，`add(5.0,0.0,-1)` 实测返回 10.0）；toInteger 的 BigDecimal 分支仍 intValue 截断（同方法 Long 分支已改 toIntExact） |
| R28-P2-22 | `ValueHandlerFactory`：BooleanValueHandler null→false（族内其余 handler null→null）、Long 用 intValue 判零（2^32 判成 false）、Byte 只认 1、最后一个分支强转抛 CCE——布尔语义三种输入给出与族内相反的值；ShortValueHandler 最后一个分支直接 (Short) 强转，Integer/Long 入参抛未声明的 CCE |
| R28-P2-23 | 序列化四兄弟契约仍不齐：`SerializeUtils.deserialize(new byte[0])` 抛异常而其余三家返回 null；`ProtostuffUtils.toObject(bytes, null)` 抛无主语 NPE（getSchema 判 null 返回 null，调用方直接解引用） |

**并发（6）**
| 编号 | 内容 |
|---|---|
| R28-P2-24 | `Concurrent.schedule()` 丢弃 ScheduledFuture：延迟任务抛异常被 ScheduledThreadPoolExecutor 存进 future 后彻底消失，日志无痕迹（任务异常不走 UncaughtExceptionHandler） |
| R28-P2-25 | `Concurrent.awaitTermination` 忽略超时返回值（等没等到都无法区分）、catch InterruptedException 后不恢复中断标记（与同批 FutureResult P2-37 确立的规范矛盾） |
| R28-P2-26 | 池内线程 `submit()+await()` 无防护：全部池线程阻塞在 join 等子任务、子任务排队永远拿不到线程 → 共享 IO_POOL 整体阻塞（固定池固有形态，至少 javadoc 明令禁止或入口检测抛错） |
| R28-P2-27 | `FutureResult` implements Serializable 但字段 CompletableFuture 不可序列化——任何序列化路径运行时抛 NotSerializableException；两个 orElseGet 重载漏 catch CancellationException（与同文件 get() 已修的契约矛盾，取消场景回落失效） |
| R28-P2-28 | `CopilotLinkedBlockingQueue.Spliterator.trySplit` 仍是 JDK 8 的 `batch = i` 写法，注释宣称按 JDK 21 重写——队列有被 remove 的洞时批次缩水、分裂次数与全队列锁开销高于 JDK 21 单调增长策略（功能正确，等价性声明不符） |
| R28-P2-29 | `CopilotExecutors` builder：`allowCoreThreadTimeout(true)` 而不设 keepAliveTime（默认 0）= 空闲 0 毫秒杀核心线程，池每次任务都新建线程——合法链式组合，build() 应校验拒绝 |

**限流/配置/移植（10）**
| 编号 | 内容 |
|---|---|
| R28-P2-30 | `LeakyBucketRateLimiter.shutdown()` 第二次调用抛 RejectedExecutionException（非幂等，try-with-resources+手工 shutdown 必现，还会顶掉业务异常）；submitRequest 与 shutdown 有窄竞态窗口：accepting 检查通过到 offer 之间完成关停的请求返回 true 后无人消费 |
| R28-P2-31 | `LeakyBucketRateLimiter` 漏出任务只 catch Exception：业务抛 Error（NoClassDefFoundError 等）穿出后 scheduleAtFixedRate 永久取消该桶的漏出——javadoc 自己警告的形态只堵了一半 |
| R28-P2-32 | `TokenBucketRateLimiter.refillTokens` 无任何异常防护（log.debug 链路抛出即任务被取消、桶永不再补令牌）；溢出注释的数学论证不成立（long 溢出环绕为负，`updated>capacity` 为假、把负值写回） |
| R28-P2-33 | `SlidingWindow.canPass` 全局 synchronized 内对 ConcurrentLinkedDeque 调 O(n) 的 size()，高配额下限流器自身成瓶颈；用墙钟毫秒计时，NTP 步进校时直接扭曲放行配额 |
| R28-P2-34 | 漏桶每实例固定 1ms 轮询，与 leakRate 和队列空满无关：leakRate=3 时 333 次 tick 里 332 次什么都没做，N 个实例 N×1000 次/秒唤醒 |
| R28-P2-35 | `SlidingWindowRateLimiterBuilder.build()` 无参数校验（timeWindow/timeUnit null → 库内部 NPE；limit<=0 → 全拒且无告警）；类 javadoc 错写"令牌桶算法"、RateLimits 注释提到不存在的 precision 参数 |
| R28-P2-36 | `YamlReader` 的 Logger 用 `getLogger(PropertyReader.class)`——YAML 解析告警全部打在 PropertyReader 名下，按 logger 过滤排障找不到也关不掉；`YamlProfileReaders` 缓存键未做 .yml/.yaml 后缀归一，同一份配置解析两遍、两份实例并存 |
| R28-P2-37 | `PropertyReader` 的 classpath 源走 ResourceBundle.getBundle 隐式启用 locale 变体解析（存在 zh_CN 变体文件时读到的不是你以为的文件），与工作目录/config 目录两级的精确文件名匹配不一致 |
| R28-P2-38 | `PathMatchingResourcePatternResolver.convertClassLoaderURL` 丢失上游 v6.1.12 的非 file 协议 cleanPath 分支（逐行 diff 少 11 行）：容器/自定义 ClassLoader 返回带 `//`、`../` 片段的 URL 时，同一资源以两种形态进结果集无法去重 |
| R28-P2-39 | `DynamicUtils.createObject`：值为 null 的属性无任何提示地丢弃（后续 IAE 检查因此永不可达，代码还留着误导读者）；非法属性名（`class`、`a/b`）时 ByteBuddy 的 IllegalStateException 在 try 块外未经包装直接抛出、错误信息泄漏内部细节；值全 null 与空 Map 走两条不同路径返回不同形态；每次调用生成加载新类无缓存（热路径 metaspace 单调增长——探针实测相同入参得两个类） |
| R28-P2-40 | `BusinessException(String)`、`BusinessException(code,msgTemplate,default)`、`ServiceException(String)` 构造器未 `super(message)`——Throwable.detailMessage 为 null，P1-21 消除的形态在其余构造器上可经序列化/诊断工具复活；`AntPathMatcher` 尾部 private isEmpty 是死代码；`Types` 的孪生实现 copilot-cache 侧仍只手工注册 9 种数组类型（跨模块修复不完整） |

### 复核中被推翻/修正的子代理结论（留档，防止照抄）

- PT_ALL"负偏移必失败"结论修正：`+05:30` 与 `-05:30` 带冒号形态走 DateFormatterHolder 的 ISO 严格解析分支实测正确，只有紧凑形态 `-0500` 失败（已并入 R28-P2-19 如实描述）。
- `getMap` 冒号分隔语义、SDT_GMT 单例形态、UrlResource Base64 改进：读 readme/javadoc 确认是作者知情选择，不列为缺陷。
- AlgorithmUtils"洗牌算法边界"审计目标：该文件不存在洗牌代码（历史三个版本 grep 零命中），审计输入有误；其余 20 万轮探针验证正确性无问题。
- PatternMatchException：该异常属 spring-web PathPatternParser，与本模块无关，任务提示不成立。

## 五、优先建议

1. **先修 R28-P0-1 / R28-P0-2**：一个持续产生错时间入库，一个是攻击者可控输入的路径穿越，都有实测触发路径。
2. **IOUtils 一族 5 条（R28-P1-1..5）一起修**：Scanner 三兄弟换 BufferedReader、两处返回值丢弃补检查，一批测试可全覆盖。
3. **R28-P1-15（TTL copy 共享）**：一行覆盖 copy() 即修，建议随下批走——javadoc 已写明陷阱但共享本身还在。
4. P2 簇里的 3 条"9-23 修复的等价声明与实际写法不符"（R28-P2-28 等）属文档诚信问题，改注释或改实现二选一，成本很低。

—— 报告生成：2026-09-28，基线提交 1c27589 + 当日未提交修复。测试：55 类 / 321 用例全绿。
