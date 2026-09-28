# JedisUtils 在 Redis 集群下的可用性分析报告

日期：2026-09-28
分析对象：copilot-cache 模块（JedisUtils / JedisClusterOperations / 锁 / 登录脚本 / Lua 脚本）
依赖客户端版本：Jedis 5.0.2（父 pom `jedis.version` 写死）

## 一句话结论

**常用的读写、单键 Lua、分布式锁主体、登录脚本这四大块在集群下是好的**，键的分区设计（hash tag）经过实测全部落在同一个 slot（slot 就是集群把 16384 个哈希槽分给各个主节点的分区，两个键要一起操作必须先保证分到同一个槽）。
**但有三处会直接出问题：滑动窗口限流在集群下每次都报错、普通限流 rateLimit 在集群下几乎每次都报"脚本不存在"、HASH.time() 在集群下随机路由导致脚本找不到且自动补救无效。** 另外订阅功能在集群下没实现、keys() 只能查一个分区，这两点要有明确认知。

## 我是怎么验证的（不是凭印象）

1. 读了本模块全部相关源码：`JedisUtils.java`、`JedisClusterOperations.java`、`JedisOperations.java`、锁三个类、全部 11 个 Lua 脚本、`AuthUtils.java`、`JedisClusterPoolFactory.java`、`JedisOperationFactory.java`。
2. 下载了 **Jedis 5.0.2 官方 sources jar**，反读它的集群路由源码，确认了三件关键事实：
   - `ClusterCommandArguments.processKey()`：命令携带的每个"路由键"都会算 slot，不同槽直接抛 `Keys must belong to same hashslot`；
   - `ClusterCommandObjects.keys()`：pattern 里没有非空 `{tag}` 会**直接抛 IllegalArgumentException**，就算有 tag 也只查那一个槽，不是全库扫描；
   - `UnifiedJedis.scriptLoad(script)`（不带 sampleKey 的版本）会向**所有节点广播**加载；`scriptLoad(script, sampleKey)` 只加载到 sampleKey 所属槽的**主节点**。
3. 写了离线探针（用 jedis 自带的 `JedisClusterCRC16.getSlot()`），把封装里所有"派生键 vs 原键"的槽位逐一算过，实测结果贴在文末附录。
4. **没有连真实集群做在线验证**（本机没有可用的 6 节点集群），涉及运行时行为的判断都基于上面 1-3 的静态验证，证据充分但在线回归仍建议补一轮（重建方法见 `RedisClusterCompatTest` 类头注释）。

---

## 一、集群下工作正常的部分 ✓

### 1. 常规单键命令（set/get/incr/hash/list/zset/setbit/geo 等绝大多数）
`JedisClusterOperations` 里这些方法全部是一键一命令直接委托 `JedisCluster`，Jedis 自己按 key 算槽路由、遇到 MOVED 自动重试（maxAttempts 默认 3，重试时会重建槽缓存）。没有多键陷阱。**没问题。**

### 2. 锁的 Lua 脚本（setnx.lua / renew.lua / unlock.lua）
`JedisUtils.evalLua()` 这条链路在集群下是正确的：
- 加载脚本时用真实锁 key 做路由键（`scriptLoad(script, sampleKey)`），脚本恰好加载到锁 key 所在槽的主节点；
- 执行 `evalsha(sha, 1, key, ...)` 时，Jedis 取**前 keyCount 个参数**算槽（源码 `ClusterCommandObjects` 里 `processKeys(Arrays.copyOf(params, keyCount))`），也就是锁 key 本身——加载点和执行点是同一个主节点，闭环成立；
- Redis 端脚本缓存丢了（重启/主从切换后 `SCRIPT FLUSH` 效果）会报 NOSCRIPT，`evalLua` 捕获后删缓存、重新加载、重试一次（JedisUtils.java:200-213），能自动恢复。

### 3. HASH 的 field 级过期（hash.lua，双键脚本）
`timeoutZsetKey()` 的派生规则是集群兼容改造后的正确写法（key 不带 `{}` 就整键包 tag；已带 tag 就复用 tag，不套两层）。探针实测：
- `user:cart:1` 与它的 zset 键：同为 slot 990；
- `{auth}:token:login:info` 与派生键：同为 slot 13741。
两个 KEYS 同槽，Jedis 的客户端校验和 Redis 服务端校验都能通过。
一个已知边角：key 的第一对花括号为空（如 `a{}.b`）时派生不出同槽键（实测 slot 7347 vs 14311），代码注释 1708-1710 行已声明这个限制，Redis 的 tag 规则本来如此，接受即可。

### 4. 登录脚本 AuthUtils（spring-security-multi-auth.lua）
集群改造做对了：所有数据键统一 `{auth}` 前缀（实测 `{auth}:token:username`、`{auth}:token:ttl:zset`、`{auth}:alice:token` 全部 slot 13741）；调用方显式把路由键作为 KEYS[1] 传入（AuthUtils.java:342-348），脚本加载和 EVALSHA 执行落同一个分片；NOSCRIPT 自愈用 volatile 读 + synchronized 重载处理并发（342-375 行），设计正确。废弃的 spring-security-auth.lua 已无加载点，不影响。

### 5. PUBLISH 发布消息
集群下 `jedisCluster.publish` 被路由到随机节点，但 Redis 普通发布订阅消息会经集群总线转发给订阅了该频道的节点，发布本身不要求同槽。锁 key 和它的 `:channel` 通知频道实测不同槽（12213 vs 11008），**这不构成问题**，因为频道消息不走槽路由。

---

## 二、集群下会出错的部分 ✗

### 【P0-1】slidingWindows 滑动窗口限流：集群下每次调用都失败

- **现象**：Redis 对脚本执行报错（类似 `Lua script attempted to access a non local key`），限流功能完全不可用。
- **原因**：脚本入参 KEYS[1] 只是业务名字（如 `order`），脚本内部却拼出另一个键 `slading_window:zset:order` 去 ZRANGE/ZADD（slidingWindow.lua 第 20、59 行）。集群下脚本只能碰与 KEYS 同槽的键，实测 `order` 在 slot 16025，`slading_window:zset:order` 在 slot 3001，跨槽，服务端拒绝。
- **影响**：所有用 `JedisUtils.SLIDING_WINDOW.slidingWindows()` 的调用方，切集群后功能直接坏掉。
- **怎么改**：把"拼前缀"这件事从 Lua 里挪到 Java 里——Java 侧生成完整键 `slading_window:zset:<name>` 作为 KEYS[1] 传入，脚本直接用 KEYS[1] 操作，删掉脚本内的 `KEY_PREFIX` 拼接。改动量很小。

### 【P0-2】rateLimit 限流：脚本加载点和执行点不是同一个节点，几乎每次都报 NOSCRIPT

- **现象**：调用 `JedisUtils.rateLimit(key, expire, count)` 在集群下抛 NOSCRIPT，自愈重试一次后仍然失败。
- **原因**：JedisUtils.java:2561-2563 —— 路由加载用的 sampleKey 是**原始 key**，脚本实际操作的 KEYS[1] 却是 `"rate:" + ":" + "limit:" + key`。探针实测 `api:x` 在 slot 8643，`rate:limit:api:x` 在 slot 618。脚本被加载到 slot 8643 的主节点，EVALSHA 却按 KEYS[1] 路由到 slot 618 的节点——那里没有脚本。自愈逻辑再执行一次，加载的还是 8643 那个节点，所以重试也没用（两键同槽的概率只有约 1/16384）。
- **影响**：限流功能集群下不可用。
- **怎么改**：Java 里先拼好真实键 `String rk = join(":", "rate", "limit", key);`，sampleKey 和 KEYS[1] 都用 rk。一行改动。

### 【P0-3】HASH.time()：keyCount=0 导致随机路由，自愈也救不回来

- **现象**：集群下取 Redis 服务器时间戳，大概率报 NOSCRIPT（偶尔成功，取决于随机到哪个节点），行为不稳定。
- **原因**：JedisUtils.java:2261 用 `evalLua("hash.lua", "hash.lua", 0, ...)`，keyCount=0。Jedis 对无路由键的命令走 `ClusterConnectionProvider.getConnection()` **随机挑节点**（含从节点）；而脚本只被 `scriptLoad(script, "hash.lua")` 加载到了 `"hash.lua"` 这个字符串算出来的那一个槽的主节点。随机命中的节点约 5/6 没有脚本。失败后自愈重新加载，加载点不变，重试仍是随机路由——救不回来。
- **影响**：依赖 `HASH.time()` 的调用方在集群下拿到的是随机异常。
- **怎么改**（二选一）：
  1. `evalsha(sha, sampleKey)` 这个带路由键的形态本来就存在（JedisClusterOperations.java:424-426），`time()` 改用它，加载和执行都用同一个固定样例键；
  2. 给 `JedisClusterOperations` 补一个"广播加载"的 `scriptLoad(String script)` 覆盖（委托 Jedis 的广播版本，实测源码：不带 sampleKey 的 `scriptLoad` 会向所有节点加载，各节点返回同一 SHA），TIME 这种无键脚本就哪个节点都能执行。

### 【P1-4】keys(pattern)：不带 tag 直接抛异常，带 tag 也只查一个分区

- **现象**：`JedisUtils.keys("user:*")` 在集群下抛 `IllegalArgumentException`；`keys("{auth}*")` 能执行但只返回 `{auth}` 那个槽里的键。
- **原因**：Jedis 5.0.2 源码 `ClusterCommandObjects.keys()` 硬性要求 pattern 含非空 `{...}`（客户端本地校验，见源码 37 行），有 tag 时也仅路由到该 tag 的单一槽。**这是 Jedis 的行为，不是集群不支持 KEYS**——封装层 `JedisClusterOperations.keys()` 只是把 Set 转 List，没有屏蔽也没有说明这个差异。
- **影响**：把单节点时代的"按模式捞全库键"用法直接搬到集群会要么异常要么结果残缺。
- **怎么改**：要么文档写明限制并引导调用方换 SCAN 方案，要么给集群实现版：遍历所有主节点分别执行 KEYS 再合并（运维语义，慎用，大库会阻塞节点）。

### 【P1-5】subscribe / psubscribe：集群下是空实现，只打一行日志

- **现象**：`JedisClusterOperations.subscribe()` 里只有一句 `log.info("Not implemented yet!")`（444-451 行），调用方完全无感知——不抛异常、不订阅。
- **影响链**：
  - `BlockingLock`（阻塞锁）：等待者以为订阅了 `:channel`，实际没人订阅，解锁后发的通知没人收到，只能靠 park 超时（一个租期，默认 30 秒）醒来重试。互斥性不受影响（加锁仍由 setnx.lua 原子保证），**代价是集群下等锁的唤醒延迟最坏为整个租期**。BlockingLock 类头注释 22-24 行已声明这点，属于"已知退化"，但空实现不抛异常这个做法值得商榷——调用方（比如想订阅登录/登出消息的业务）会误以为订阅成功。
  - `AuthUtils` 的在线状态监听（登录/登出频道订阅）在集群下收不到任何消息。
- **怎么改**：至少把这两个方法改成抛 `OperationNotSupportedException`，让误用在第一时间暴露；有余力再补真实现：Jedis 5 的 `JedisCluster` 没有集群订阅封装，可行做法是维护一条到某个节点的专用连接执行 SUBSCRIBE（Redis 普通 pubsub 走集群总线转发，连哪个节点都能收到全集群消息，实测 UnifiedJedis.subscribe 就是取随机连接做的）。

### 【P1-6】blpop / brpop 阻塞命令与默认 socketTimeout=1000ms 矛盾

- **现象**：`JedisUtils` 的阻塞弹出（`blpop(timeout, key)`）在集群配置下，只要 timeout >= 1 秒就会抛连接读超时异常，而且 Jedis 会把这条连接标记为坏连接销毁。
- **原因**：`JedisClusterPoolFactory.java:48` 命令超时默认 1000 毫秒；阻塞命令的等待时间超过读超时，客户端先于服务端放弃。这不是集群独有，但集群工厂的默认值把它变成了"开箱即坏"。
- **怎么改**：文档强调 `redis.socketTimeout` 必须大于业务最长阻塞时间；或为阻塞命令单独走更长超时的配置。

### 【P2-7】AuthUtils 在类加载时就加载脚本，集群不可达会让应用起不来

`private static final String sha1 = JedisUtils.scriptLoad(...)`（AuthUtils.java:105）在类初始化时执行，此时若集群那个分片的主节点不可达，抛 `ExceptionInInitializerError`，且该类此后永远不可用。建议改成懒加载 + NOSCRIPT 自愈路径复用现有的 `currentSha()/reloadSha()`。

### 【P2-8】集群连接池默认值偏激进的组合

`connectionTimeout` 默认 50000ms（JedisClusterPoolFactory.java:46）：节点挂了以后建连要等 50 秒才失败，配合 `maxWaitMillis=60000` 容易让线程在故障期间长时间等待。建议默认降到 5 秒内，与单节点工厂一致。

---

## 三、多键命令清单（不算 bug，但要写进使用须知）

以下方法在集群下要求**所有键同槽**，否则 Jedis 本地直接抛 `Keys must belong to same hashslot`，这是集群的固有约束，封装层不做预处理是合理选择，但调用方必须知情：

- `SET.sinter(key1, key2)`
- `bitAnd/bitOr/pfmerge/pfcount` 的多键版本
- `brpop(timeout, keys...)` / `blpop(timeout, byte[]... keys)` 多键版本
- `eval(script, keyCount, params)` 中 keyCount>1 的自写脚本

建议：要么约定派生键统一用 `{原键}:后缀` 的 tag 写法（HASH 模块已经是这个范式），要么在 javadoc 逐个标注入选条件。

## 四、现有测试覆盖情况

- `RedisClusterCompatTest`：4 个用例，覆盖了单键读写、锁、HASH 双键脚本、AuthUtils，集群不可达时整体跳过（assumeTrue 写法正确，不误报）。
- `JedisUitlsClusterTest`：仅 3 个用例（set/setnx/sinter）。
- **上面 P0-1 / P0-2 / P0-3 三条问题路径都没有集群测试覆盖**——P0-1 的 slidingWindow 和 P0-2 的 rateLimit 连单节点测试都在，恰恰证明这两个的 bug 只在集群语义下才暴露。修复时请在 `RedisClusterCompatTest` 里补对应用例。

## 五、总结对账表

| # | 功能 | 集群可用性 | 结论 |
|---|------|-----------|------|
| 1 | 常规单键读写 | ✅ 可用 | 委托 JedisCluster 自动路由重试 |
| 2 | setnx/renew/unlock.lua 锁链路 | ✅ 可用 | 加载点=执行点，NOSCRIPT 可自愈 |
| 3 | HASH field 过期（hash.lua） | ✅ 可用（畸形空 tag 键除外） | 同槽派生实测通过 |
| 4 | AuthUtils 登录（multi-auth.lua） | ✅ 可用 | {auth} tag + KEYS[1] 路由正确 |
| 5 | publish 发布消息 | ✅ 可用 | 集群总线转发，不要求同槽 |
| 6 | slidingWindows 限流 | ❌ 每次失败 | 脚本操作未声明的跨槽键（P0-1） |
| 7 | rateLimit 限流 | ❌ 几乎每次失败 | 加载/执行路由键不一致（P0-2） |
| 8 | HASH.time() | ❌ 随机失败 | keyCount=0 随机路由（P0-3） |
| 9 | keys(pattern) | ⚠️ 受限 | 需 tag 且单槽结果（P1-4） |
| 10 | subscribe/psubscribe | ⚠️ 空实现 | 阻塞锁唤醒退化为一租期（P1-5） |
| 11 | blpop/brpop | ⚠️ 默认超时冲突 | socketTimeout 需显式调大（P1-6） |
| 12 | pipeline()/execute() | ❌ 抛不支持 | 集群下本就难做，行为明确可接受 |

## 附录：离线槽位探针实测输出

探针：`JedisClusterCRC16.getSlot()`（Jedis 5.0.2 自带实现），逐键计算。

```text
hash普通键      [user:cart:1] slot=990   [{user:cart:1}:jedis_utils:__timeout__set] slot=990                 SAME
hash已含tag     [{auth}:token:login:info] slot=13741 [{auth}:jedis_utils:__timeout__set:{auth}:token:login:info] slot=13741  SAME
auth路由键vs键群 [{auth}:token:username] slot=13741 [{auth}:token:ttl:zset] slot=13741                        SAME
auth路由键vsSET [{auth}:token:username] slot=13741 [{auth}:alice:token] slot=13741                            SAME
锁键vs通知频道  [copilot:blk:order:lock] slot=12213 [copilot:blk:order:lock:channel] slot=11008               DIFFERENT(无碍，pubsub不走槽)
rateLimit路由vs实际键 [api:x] slot=8643  [rate:limit:api:x] slot=618                                           DIFFERENT ← P0-2
a{}.b派生       [a{}.b] slot=7347        [{a{}.b}:jedis_utils:__timeout__set] slot=14311                      DIFFERENT ← 已声明的限制
slidingWindow路由vs实际键 [order] slot=16025 [slading_window:zset:order] slot=3001                             DIFFERENT ← P0-1
keys pattern [user:*]      jedis集群校验: 抛IllegalArgumentException
keys pattern [{auth}*]     jedis集群校验: 通过(仅路由到该tag所在slot)
keys pattern [copilot:blk:*:lock] jedis集群校验: 抛IllegalArgumentException
```
