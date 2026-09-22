# copilot-cache 分布式锁分析报告

> 分析日期：2026-09-22
> 分析范围：`JedisUtils#lock` / `#blockingLock` / `#nonBlockingLock` 三个入口，及其背后的
> `BlockingLock`、`NonBlockingLock`、`setnx.lua`、`unlock.lua`、`JedisPoolOperations`、`JedisClusterOperations`、`PoolFactory`。
> 环境事实：本机 Redis 未启动（127.0.0.1:6379 探测不通），本报告全部结论来自代码静态分析，未经过运行时验证；涉及行为推断的地方都标注了依据。

## 一、结论速览

| 入口 | 正确性 | 性能 | 死锁风险 |
|---|---|---|---|
| `lock(key, requestId, leaseTime, unit)` | 基本正确，但有 3 个问题（见 §2.1） | 最好：加锁 1 次往返、解锁 1 次往返 | 无死锁，但租期一到锁自动消失，业务超时后互斥失效 |
| `blockingLock(key)` | **有严重缺陷，不建议在复用场景使用**（watchDogStopped 永不复原、订阅连接泄漏、"线程空闲"判断会错误停止续期） | 最差：无竞争加锁 2 次往返 + 每次新建线程；竞争时 32 次自旋 + 每个等待者占 1 个线程和 1 个 Redis 连接 | 无线程意义上的永久死锁，但存在"连接池耗尽导致全局卡死"和"同线程重复加锁互相阻塞几十秒"两类问题 |
| `nonBlockingLock(key)` | 中等：watchDogStopped 同样永不复原；续期上限 100 秒会让长任务执行中途失去锁 | 中等：拿到锁要 2 次往返，拿不到时仍然自旋 32 次后才返回 | 同 blockingLock 的续期类问题，无等待逻辑所以没有连接泄漏问题 |

**一句话总结：这套锁不会"永远锁住"（TTL + 30 秒定时醒两道保护在），真正的问题是反过来的——多条路径会让锁"提前丢失"，互斥被破坏；另外 blockingLock 有一个连接泄漏问题：等待者占满连接池后，整个应用卡死。**

---

## 二、逐个入口分析

### 2.1 `JedisUtils#lock(String key, String requestId, long leaseTime, TimeUnit unit)`（JedisUtils.java:3014）

实现是一行 `setnx(key, requestId, leaseTime, timeUnit)`，底层走 `setnx.lua`：

```lua
local set = redis.call("setnx", KEYS[1], ARGV[1])
if tonumber(set) == 1 then
  redis.call('expire', KEYS[1], ARGV[2])
end
return set
```

**做对的地方：**
- setnx 和 expire 在同一个 Lua 脚本里执行，服务端单线程跑完，中途不会被打断——加锁和设过期是原子的，不会出现"加了锁没设上过期就宕机"的死锁。
- 解锁走 `unlock.lua`（JedisUtils.java:3065）：`GET` 比对 value 再 `DEL`，也在 Lua 里原子完成，不会误删别人的锁。value 用调用方自带的 `requestId`，满足"解铃还须系铃人"。
- 全程只有调用方自己掌握 requestId，不依赖任何本地状态，语义简单可预测。

**三个问题：**

1. **亚秒租期直接把锁删掉**（JedisUtils.java:435）。`timeUnit.toSeconds(expires)` 是向下取整：传 `500ms` 得 `0`，`EXPIRE key 0` 在 Redis 里的语义是立即删除该键。结果是 `setnx.lua` 返回 1（"加锁成功"），但锁瞬间消失，任何后续 `lock()` 都能成功——**互斥完全失效，且调用方拿到的返回值说"成功"**。应加校验：`toSeconds` 后为 0 就拒绝或改用 `PEXPIRE`/`SET key val NX PX ms` 一条命令完成。
2. **超时后 unlock 返回 false 是正常现象，但没有任何区分手段**。业务跑超租期后锁过期、被 B 拿走，A 调 `unlock` 时 value 对不上返回 false——这只能说明"你手里的锁已经不是你的了"，A  critical section 里做的写操作可能已经和 B 并发执行了。接口上没有任何机制告诉调用方"你的锁中途丢过"，全靠调用方自己保证业务时长 << 租期。
3. **无续期、无重入**。同一线程用同一 requestId 重复 `lock()`，第二次 setnx 返回 false（key 还在），但此时锁仍然归你持有——返回值"失败"和返回值"被别人占了"无法区分。

另外注释本身有个错位：javadoc（JedisUtils.java:3010）写"key过期时间, 单位秒"，但实际参数是 `leaseTime + timeUnit` 任意单位；上面 2951 行还有一段被注释掉的旧版 `lock(key, requestId)`（无过期时间的 setnx，会死锁），建议直接删掉注释代码，留着只会误导。

### 2.2 `JedisUtils#blockingLock(key)` → `BlockingLock`

流程：setnx 抢锁 → 失败自旋 32 次 → 再失败则 SUBSCRIBE 通知频道 + `LockSupport.parkNanos(30秒)` → 被唤醒或超时自动醒来后重试 setnx → 成功则启动看门狗。解锁：Lua 校验后删除 → PUBLISH 通知 → 停看门狗。

**正确性问题（按严重程度排序）：**

#### （1）watchDogStopped 置 true 后永不复原 —— 实例复用时锁的续期永久失效（BlockingLock.java:60, 351）

`watchDogStopped` 是**实例字段**。`stopWatchDog()` 把它置 true（:351），`startWatchDog()`（:280）**从不把它复位**。看门狗任务第一件事就是 `if (watchDogStopped || ...) return;`（:299）直接退出。

后果：同一个 `BlockingLock` 实例（典型场景：`JedisUtils.blockingLock()` 返回的对象被存成字段、或每次流程复用同一实例；CLAUDE.md 已知问题里也记了这一条）第一次解锁后，**之后每次加锁后看门狗都是空转：任务在第一个判断处直接退出，不再续期**，锁固定 30 秒过期。业务只要超过 30 秒，锁就在持有者手里丢了——这正是"即使有客户端崩溃也不会死锁"的反面：活着的客户端丢了锁。

而且它是 `volatile` 的实例字段却配合 **ThreadLocal 的看门狗线程池**（:87）使用，语义上是错的：A 线程 stopWatchDog 会把 B 线程看门狗也关掉（B 的后续续期在 :299 直接 return）。**这个状态必须按线程隔离**（放进 ThreadLocal，或 `ConcurrentHashMap<Thread, Boolean>`）。

**这是全套代码里最需要先修的 bug。当前唯一安全的用法是"每次加锁都 `new BlockingLock`/调一次 `JedisUtils.blockingLock`"，但代码和文档都没有强制或提示这一点。**

#### （2）订阅连接泄漏 → 连接池耗尽 → 全应用卡死（JedisPoolOperations.java:450-453）

```java
public void subscribe(JedisPubSub jedisPubSub, String... channels) {
    THREAD_POOL.execute(() -> pool.getResource().subscribe(jedisPubSub, channels));
}
```

`pool.getResource()` 借出的 Jedis 连接**从头到尾没有 close()**。SUBSCRIBE 是阻塞命令，线程在池里一直占用这条连接；即使 `unsubscribe()` 让 subscribe 返回了，这个局部 Jedis 对象也没人归还——commons-pool2 的连接一旦借出不还就是永久泄漏（PoolFactory 没有开启 removeAbandoned）。

叠加后果链：`maxTotal` 默认 50（PoolFactory.java:60）。**每个进入阻塞等待的线程泄漏 1 条连接**，等待者一多：
- 第 51 个等待者 `getResource()` 阻塞 `maxWaitMillis`=60 秒（PoolFactory.java:79）后抛异常；
- 更糟的是**持锁线程的看门狗续期、业务自己的 Redis 读写也拿不到连接**——续期失败 → 锁 30 秒后过期 → 互斥丢失；或业务操作整体阻塞 60 秒。

这是本题"有没有死锁风险"的最实质答案：**锁本身不会永久死锁，但 blockingLock 的等待机制会把连接池的连接全部占用完，让整个进程所有 Redis 操作排队 60 秒以上——对外表现和死锁几乎一样。**

修法：订阅 lambda 里 `try (Jedis jedis = pool.getResource()) { jedis.subscribe(...); }`，注意 Jedis 在 subscribe 被 unsubscribe 正常结束后 close 是安全的（返回池）。

#### （3）"线程空闲检测"会错误停止续期，破坏互斥（BlockingLock.java:280-346, 429-491）

看门狗每 10 秒续期一次（`defaultTimeout/3`，:341），但每次续期前先跑 `isThreadProcessingLockRelatedBusiness()`（:429）判断持锁线程"是否仍在执行业务"，判断依据是一组启发式规则：

- 线程状态是 WAITING / TIMED_WAITING → 判空闲（:433）；
- 栈深度 < 10 → 判空闲（:443）；
- 框架类前缀占比 ≥ 90% → 判空闲（:474）。

问题：**业务线程等待任何下游响应时都处于 WAITING/TIMED_WAITING**——JDBC 驱动多数实现、RPC 回调、`BlockingQueue.take()`、`Thread.sleep`、锁等待……都会被误判为空闲。锁明明还在被积极使用，看门狗却停止续期，30 秒后锁过期，第二个持有者进入临界区，然后原持有者业务执行完成后 `unlock()`，value 已经不是自己的 → 解锁失败 → **BlockingLock.unlock 直接抛异常**（:209-211 `throw new OperationNotSupportedException("解锁失败了哟")`），让一个逻辑上正常走完的业务方法对外抛出异常。

栈深度和类名前缀这两条规则更不可靠：一个 8 层的业务调用栈会被判"空闲"，一个 `WAITING` 在 `pool.getResource()` 上的线程是"空闲"但它其实只是因为问题（2）的连接泄漏处于阻塞状态。

**建议删掉整个判断逻辑**，续期只认两件事：调用方是否 unlock、持锁线程是否死亡（`isAlive()`）。"怕忘记解锁导致永久续期"的担忧应该用"续期次数上限 + 告警日志"解决——下面的 MAX_RENEW_COUNT 就是这个上限，但它被设成了 10 次 ×10 秒 = **约 100 秒硬上限**（:291, 317），任何合法的 2 分钟批处理任务都会在执行中途失去锁。上限应该配成可配置项，并且到上限时的行为是"继续持有剩余 TTL、停止续期"而不是抛异常，同时打 error 级日志。

#### （4）verifyLock 多余且引入新竞态（BlockingLock.java:190-198）

`setnx.lua` 返回 1 的那一刻，value 一定是本次写入的（Lua 原子性保证）。之后的 `GET` 比对是多余的第二次往返，还把一次加锁变成 2 次往返。而且它自身不原子：GET 之后到判断之间锁仍可能过期。verify 失败时还会额外调用一次 `unlock(key, lockValue)` 再返回 false——然后外层继续自旋。这段代码唯一能"防"的是主从切换后数据丢失，但那种场景它同样防不住。**删掉它，加锁从 2 次往返降回 1 次。**

#### （5）同线程重入 = 自己等自己（BlockingLock.java:111-188）

`lock()` 开头不检查 `lockedThreadLocal`，同一线程对同一实例再次 `lock()`，setnx 必然失败（key 是自己写的，但 setnx 只看 key 存在与否），然后进入自旋 + 30 秒 park 循环——**等待的对象是它自己，它不可能自己给自己解锁**。只能等第一次加的锁因为看门狗续期被"空闲检测"错误停掉、或达到 100 秒上限而过期后才能抢到。表现为几十秒到一分多钟的无响应。javadoc 和 Lock 接口（Lock.java:37）都没说"不支持重入"，必须补文档，或 lock() 开头 `if (lockedThreadLocal.get()) throw` 快速失败。

#### （6）stopListener 与异步订阅的时序竞态（BlockingLock.java:159-177 + JedisPoolOperations.java:450）

`startListener()` 将订阅任务提交给线程池后立即返回；超时醒来并抢到锁后立刻 `stopListener()` → `jedisPubSub.unsubscribe(channel)`。如果此刻订阅线程还没真正执行到 `jedis.subscribe()`，JedisPubSub 内部 client 尚未就绪，`unsubscribe` 会抛 `IllegalStateException`。时序：抛在 :174（stopListener），而 :173 已把 `lockedThreadLocal` 置 true——finally 里的清理条件（:182 `if (!lockedThreadLocal.get())`）不成立，**锁状态留着、看门狗没启动、调用方收到一个与业务无关的异常**。获取锁成功的路径应把 stopListener 挪到 return 后或单独 try-catch 捕获后仅记录日志（订阅晚点取消不影响锁本身）。

#### （7）cluster 模式下通知机制整体失效（JedisClusterOperations.java:432-434）

集群版 `subscribe()` 只打一行 "Not implemented yet!"。blockingLock 在集群部署时：能加锁能解锁，但等待者收不到任何唤醒，完全靠 30 秒超时重试——功能可用但退化成高延迟轮询，且每个等待者照样会在 THREAD_POOL 里提交一个立即返回的空任务。javadoc 必须注明。

#### （8）细节问题

- javadoc 复制错误：JedisUtils.java:3021 说 blockingLock 的 key 模板是 `copilot:nblk:%s:lock`，实际（BlockingLock.java:52）是 `copilot:blk:%s:lock`；NonBlockingLock 的通知频道模板（NonBlockingLock.java:51）用了 `blk` 前缀与 blockingLock 的频道命名相同，好在该类根本不发布消息，纯遗留垃圾字段。
- 死代码：`interruptExecutorThread`（BlockingLock.java:382，反射访问 JDK 内部字段，CLAUDE.md 已记录，现无任何调用点）和 `checkLockStackInCurrentThread`（:415）都没被调用，删。
- `WatchDog.java` 是个只有 `ttlMillis` 字段的空壳类，从未使用，删或真正实现。
- NOSCRIPT 无恢复：`shaHashs`（JedisUtils.java:427, 3066）缓存脚本 SHA，Redis 重启或主从切换后脚本缓存清空，evalsha 永远抛 NOSCRIPT，**锁模块整体瘫痪，直到应用重启**。应在 catch NOSCRIPT 时 scriptLoad 后重试一次。这是所有 Lua 调用点的共性问题。

**BlockingLock 死锁风险小结：**

| 场景 | 结果 |
|---|---|
| 持有者进程崩溃 | 看门狗随进程消失，锁 ≤30 秒过期。✔ 无死锁 |
| 持有者线程处于 WAITING 状态（等待下游响应） | "空闲检测"停续期 → 锁 30 秒过期，其他线程可进。✔ 无死锁（代价是互斥已丢） |
| 等待者收不到通知（pubsub 消息丢失/订阅断） | park 带 30 秒超时（:166），必然醒来重试。✔ 无死锁，最坏延迟 30 秒 |
| 持有者业务超 100 秒 | 续期上限强制到点，锁过期。**互斥丢失，不是死锁**，但最终 unlock 抛异常 |
| 连接池被订阅泄漏耗尽 | 所有 Redis 操作（含续期、解锁本身）排队 60 秒后异常。**全局性长时间阻塞，事实上的服务级死锁**，这是最高风险 |
| 同线程重入 | 自己等待自己释放锁 30~100 秒。局部长时间无响应 |

### 2.3 `JedisUtils#nonBlockingLock(key)` → `NonBlockingLock`

定位正确：tryLock 语义，拿不到则执行降级逻辑，不 park 不订阅，没有 blockingLock 的连接泄漏问题。`try-with-resources` + `ifLocked(task)`（NonBlockingLock.java:313）的使用姿势在类注释里写得很清楚，这是三者里工程化最好的一个。

保留的问题：

1. **watchDogStopped 永不复原**（NonBlockingLock.java:61, 225）：与 BlockingLock 完全相同的 bug，同类复用（比如 `new NonBlockingLock` 存字段）第二次加锁后不续期。且它是实例字段配 ThreadLocal 线程池，跨线程互相干扰的问题同样存在。
2. **续期硬上限约 100 秒**（:171, 194）：`isThreadProcessingBusiness` 已被简化成 `thread.isAlive()`（:255-257）——这个方向是对的——但 MAX_RENEW_COUNT=10 × 每 10 秒一次 = 100 秒后强制停续期。业务超过 100 秒照样丢锁。上限应配置化 + 告警。
3. **lock() 拿不到锁时静默返回**（:107-118）：自旋 32 次失败后什么都不抛，调用方必须记得 `locked()` 判断——Lock 接口契约（Lock.java:31）确实这么写了，junit 示例也这么用了，但"32 次毫无间隔的立即自旋"（一次 EVALSHA 约 1ms，整个自旋窗口 <50ms）对一次 Redis 抖动（默认 socketTimeout 见 redis.properties 是 1000ms）毫无容忍度，抖动场景下等于必然拿不到锁。至少给自旋之间加 1~2ms 退避。
4. **unlock 语义与 BlockingLock 不一致**：BlockingLock 解锁失败抛异常（BlockingLock.java:210），NonBlockingLock 解锁失败只 warn（NonBlockingLock.java:130-131）。两个类应统一为"解锁失败=锁已丢失，抛专用异常"，让调用方能感知。
5. 失败路径 `tryLock` 清理了 valueThreadLocal（:96）但没 remove `lockedThreadLocal`——因为有 `withInitial(false)` 所以逻辑上无害，不过在线程池复用场景这两个 ThreadLocal 永远留着条目，建议完整 remove。
6. 同样有 verifyLock 多余往返问题（:288-306），同 2.2(4)。

### 2.4 测试现状

仓库内锁相关测试只有 `JedisLockTest`、`JedisUtilsLockTest` 两个仅验证基本流程的示例测试：无线程并发、无"两个线程抢同一把锁"的断言、无"持锁超时后锁被抢走"的用例、无 watchDogStopped 复用回归用例，且都依赖真实 Redis。按"测试验证行为后果"的标准，上面 §2.2(1)(2)(3) 三个致命 bug 全部处于零覆盖状态——这也是它们能长期存活的原因。修复时每个 bug 应先补失败用例再改实现。

---

## 三、性能分析

以单实例 Redis、内网 RTT≈0.3ms 估算（数字为理论推算，非实测）：

| 操作 | 命令/往返 | 额外开销 |
|---|---|---|
| `lock()` 加锁 / 解锁 | 1 次 evalsha / 1 次 evalsha | 无 |
| `nonBlockingLock` 加锁 / 解锁 | 2 次（setnx+GET，verify 删掉后回到 1）/ 1 次 | **每次成功加锁新建一个 1 线程池**；解锁时 `awaitTermination(1.5s)` |
| `blockingLock` 无竞争加锁 / 解锁 | 2 次 / 2 次（+1 次 publish） | 同上，且解锁的 `stopWatchDog` 里 `awaitTermination(2s)` **同步等待**在 unlock 调用线程上——解锁路径额外增加最多 2 秒 |
| `blockingLock` 竞争等待 | 32 次自旋 evalsha + 每 30 秒 1 次 | 每个等待者：THREAD_POOL 1 线程 + **1 条永久泄漏的 Redis 连接** + 1 次 SUBSCRIBE；失败期日志每自旋打 2 行 info，高竞争时日志本身就是瓶颈 |
| 看门狗续期 | 每 10 秒 1 次 expire | 持锁期间 `thread.getStackTrace()`（BlockingLock.java:438）——该调用会让目标线程走到安全点，高频调用对繁忙业务线程有可观察的停顿影响 |

**核心性能结论：**

1. 看门狗线程池"每次加锁 new、解锁 shutdown"是最大浪费。正解：一个共享的静态 `ScheduledThreadPoolExecutor`（1~2 线程足够），每把锁一个 `ScheduledFuture` 句柄，解锁只 `future.cancel(false)`——省去线程创建销毁，同时消除 awaitTermination 同步等待。
2. `unlock()` 里同步 `awaitTermination(2s)`、`stopWatchDog` 在 await 超时后还遗留中断标记处理，都会使解锁耗时从 1ms 级增加到秒级。
3. 无竞争路径删掉 verifyLock 的 GET 后，blockingLock 加锁成本与 lock() 持平；竞争路径的连接泄漏是唯一会导致整个应用不可用的项。
4. BlockingLock 每个自旋/失败回合 4~6 行 `log.info`（BlockingLock.java:139-158），32 次自旋 = 每次抢锁失败约 200 行日志，高 QPS 锁竞争下日志 IO 会反压业务线程，应降 debug 并合并。

---

## 四、`JedisUtils#lock` 还有没有必要存在？

用户的问题：Lock 抽象已有 `locked()`，lock() 是否多余？

**先说结论：lock()/unlock() 手工对和 blockingLock/nonBlockingLock 不是包含关系，它们覆盖了一个 Lock 抽象给不了的独有场景；但在"普通同线程加解锁"场景里，它确实应该让位给 nonBlockingLock。**

`blockingLock`/`nonBlockingLock` 的 lockValue 由构造函数内部生成并只存在 `valueThreadLocal` 里（BlockingLock.java:125-126），**没有任何 getter**。这意味着：锁的加与解必须发生在同一进程、同一线程。而 `lock(key, requestId, ttl)` + `unlock(key, requestId)` 的 requestId 由业务方自己生成、自己保管，因此可以做：

1. **跨请求/跨进程解锁**：HTTP 请求 A 用订单号作为 requestId 锁定资源，MQ 消费者/回调线程 B 用同一个 requestId 解锁。`blockingLock` 体系做不到，除非给它补一个"自定义 lockValue"和"unlock(key, value) 静态出口"。
2. **一次性 tryLock 语义的最小实现**：定时任务多实例防重复执行（抢到就执行，抢不到就跳过），1 次往返、无 ThreadLocal、无订阅、无线程池，是三者里资源和延迟开销最小的。
3. 与 `Lock` 接口的 `locked()` 模型正交：lock() 直接返回 boolean，调用链更短，不需要对象生命周期管理。

**建议的处理：**

- 保留，但把定位在 javadoc 里写死："仅用于业务方自行保管 requestId 的跨线程/跨请求锁；同线程场景请改用 nonBlockingLock()。不提供续期，租期必须显著大于业务最长耗时。"
- 立刻修：拒绝/修正亚秒租期（§2.1 问题 1）、删除 2951 行注释掉的废弃代码、给 unlock 补 NOSCRIPT 重试。
- 如果决定不再保留，先把 `NonBlockingLock` 扩展成支持"外部传入 requestId + 静态 unlock(key, requestId) 出口"再废弃 lock()，否则等于失去跨请求解锁能力。

---

## 五、修复清单（按优先级）

**P0（会造成锁失效或全局卡死，改动小）**
1. `watchDogStopped` 改为按线程隔离的状态，并在 `startWatchDog()` 里重置为 false（BlockingLock.java:60/280、NonBlockingLock.java:61/159）。
2. `JedisPoolOperations.subscribe/psubscribe` 用 try-with-resources 归还订阅 Jedis（JedisPoolOperations.java:450-466），消除连接泄漏。
3. 删除 `isThreadProcessingLockRelatedBusiness` 全部启发式判断，续期条件只保留"未解锁 && 线程存活"；MAX_RENEW 上限配置化（默认放宽到小时级）且到限时只记 error 不改锁行为。
4. `setnx` 过期换算后为 0 时抛 IllegalArgumentException（JedisUtils.java:435）。

**P1（正确性加固）**
5. 删除 verifyLock 的 GET 比对（两个类），加锁回到 1 次往返；若坚持保留，必须在文档说明它不能防任何东西。
6. 看门狗改共享静态调度线程池 + 每锁一个 ScheduledFuture 句柄；去掉 unlock 路径上的 awaitTermination 同步等待。
7. lock() 入口检查 `lockedThreadLocal` 已为 true 时直接抛"不支持重入"异常（BlockingLock.java:111）。
8. evalsha 全部调用点补 NOSCRIPT→scriptLoad→重试一次的包装（JedisUtils.java:427/3066 及所有 shaHashs 使用处）。
9. 统一两个类的解锁失败语义（都抛专用异常），BlockingLock 抢锁成功路径把 stopListener 移出异常传播链。

**P2（清理与文档）**
10. 删死代码：`interruptExecutorThread`、`checkLockStackInCurrentThread`、`WatchDog.java`、被注释的旧 lock()、NonBlockingLock 的 notifyChannel 字段；修正 blockingLock javadoc 的 nblk/blk 复制错误。
11. 集群模式 subscribe 未实现 → 在 `blockingLock` javadoc 标注，或实现 cluster 版订阅。
12. 补并发测试：双线程互斥断言、实例复用二次加锁续期断言（针对 P0-1）、等待者数量>连接池大小时不卡死断言（针对 P0-2）、重入抛异常断言。

---

## 附：本次分析未覆盖

- 未连接真实 Redis 运行任何用例（本机 6379 不通），所有"行为"论断基于代码与 Redis/Lua/Jedis 的公开语义；P0 修复落地前建议先在有 Redis 的环境跑一轮双进程互斥实证。
- `ReadWriteLock` 接口（readWriteLock() 无实现类）不在本次三个入口范围内，未展开。
