package com.awesomecopilot.common.lang.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 漏桶算法
 * <ul>
 *     <li/>原理: 请求以恒定速率从桶中漏出 (处理), 若桶满 (请求积压超过容量)则拒绝新请求。
 *     <li/>特点: 平滑流量输出, 但无法应对突发流量 (严格按固定速率处理)。
 *     <li/>类比: 类似水龙头滴水, 无论输入多快, 输出速率恒定。
 * </ul>
 * <ul>适用场景
 *     <li/>流量整形: 确保下游服务接收到的请求速率稳定。
 *     <li/>防止过载: 例如限制MQ消费者的消息拉取速度。
 * </ul>
 * 缺点是无法应对突发流量
 * <p>
 * <b>本实现是"漏桶 + 请求队列"的工作模型</b>（区别于令牌桶的"计数放行"模型）：
 * 请求（Runnable）先提交进桶（有界队列），一个后台线程按固定速率从桶里取出请求
 * 并<b>在漏出线程内同步执行</b>。因此它不只是"判断能不能过"，而是真正把请求接过来
 * 排队、按整形后的节奏逐个处理。两点推论：
 * <ul>
 * <li>在途量 = 队列中等待的 + 正在执行的 1 个，队列容量 capacity 就是真实的在途上界
 * （有背压）——早期版本把请求转投另一个线程池异步执行，容量约束会失真，见构造器注释；</li>
 * <li>漏出线程是单线程：一个慢请求会占住整个桶（后续请求排队等待），
 * 这是"恒定速率处理"的固有代价，也是与令牌桶的本质分工——令牌桶允许攒配额打突发，
 * 漏桶的输出永远是匀速的。</li>
 * </ul>
 * <p>
 * 带请求队列的漏桶算法实现
 * <p>
 * 特点：请求先进入队列，然后按固定速率从队列中取出，由漏出线程直接执行
 * （在途量 = 队列 + 正在执行的 1 个，桶容量就是实际在途上界）
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LeakyBucketRateLimiter.class);

    /**
     * 桶容量 = 队列的最大长度，即允许积压的等待请求数上界。
     * 队列满时新请求 submitRequest 直接失败（拒绝，不排队等待），
     * 这是"背压"的落点：上游感知到 false 就该减速或降级。
     */
    private final int capacity;                  // 桶的容量(最大队列长度)

    /**
     * 请求队列（桶本体）。ArrayBlockingQueue 有界，put 语义不用——
     * 本类只 offer（满则拒），保证提交方永不阻塞。
     * 队列里排的是"等待被漏出线程执行"的 Runnable。
     */
    private final BlockingQueue<Runnable> queue; // 请求队列

    /**
     * 漏出速率：每秒处理多少个请求。决定输出侧的匀速节奏，
     * 与输入侧多快无关（输入过快只会把桶灌满然后被拒）。
     * 换算关系：每个请求占用 1e9/leakRate 纳秒的"漏出配额"（见 leakTick）。
     */
    private final int leakRate;                  // 漏出速率(每秒处理的请求数)

    /**
     * 漏出线程：单线程调度器，跑固定 1ms 一次的 leakTick 与关闭排空任务 drainAll。
     * daemon 线程（P2-3：忘调 shutdown 也不挂进程），命名 leaky-bucket-drain。
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 是否仍接受新请求。shutdown() 第一步把它翻成 false：此后 submitRequest 恒 false，
     * 已入队的由排空任务处理完。用 AtomicBoolean 是因为写入方（shutdown 调用线程）
     * 和读取方（各业务提交线程）不同，需要可见性保证。
     */
    private final AtomicBoolean accepting = new AtomicBoolean(true);

    //lastNanos/pendingNanos 只被单线程 scheduler 访问(漏出任务与 drainAll 在同一线程排队执行), 无需同步
    /**
     * 上一次 leakTick 的时刻（纳秒）。首次 tick 为 0 时先把基准设为当前时刻，
     * 之后每 tick 用 now - lastNanos 得到"真实流逝时间"——
     * 定时器抖动/任务占用导致的延迟都被流逝时间自然吸收，速率不漂。
     */
    private long lastNanos;
    /**
     * 纳秒配额池（"存钱"机制的余额）：流逝的时间先攒在这里，
     * 每攒够 1e9/leakRate 纳秒就够漏出 1 个请求。与令牌桶的 pendingRefillMillis
     * 同型，只是单位是纳秒、兑换的是"处理次数"。
     */
    private long pendingNanos;

    /**
     * @param capacity 桶的容量(最大队列长度)，须 &gt; 0——桶大小为 0 意味着拒绝一切请求
     * @param leakRate 漏出速率(每秒处理的请求数)，须 &gt; 0——速率 0 意味着桶只进不出
     */
    public LeakyBucketRateLimiter(int capacity, int leakRate) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (leakRate <= 0) {
            throw new IllegalArgumentException("leakRate must be positive");
        }
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.queue = new ArrayBlockingQueue<>(capacity);
        //P2-33: 修复前用 2 线程池, 漏出的请求被转投到同一个池的无界工作队列异步执行——
        //capacity 不再是实际在途上界(无背压)。改为单线程: 漏出线程直接执行请求,
        //一个请求没跑完就不会取下一个, 在途量由桶容量真实约束。
        //线程设为 daemon: 使用者忘了 shutdown 也不会挂住进程退出。
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "leaky-bucket-drain");
            t.setDaemon(true);
            return t;
        });

        // 启动漏桶的定时漏出任务
        //P2-33: 修复前 interval = 1000L/leakRate——leakRate>1000 时被 Math.max(1,...) 压成
        //每 1ms 漏 1 个(实际封顶 1000/s, 配置再高也没用), 且整数截断让实际速率略高于配置
        //(如 leakRate=3 → 每 333ms 漏 1 个 ≈ 3.003/s)。改为固定 1ms tick + 纳秒配额累计:
        //应漏个数由真实流逝时间换算, 长时间平均速率精确等于 leakRate。
        scheduler.scheduleAtFixedRate(this::leakTick, 1, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试提交请求到漏桶（主用法）。
     * <p>
     * 判定只有两个条件：① 桶还开着（accepting）；② 队列未满（offer 成功）。
     * 两个条件任一不满足立即返回 false，调用方自行决定重试/降级/丢弃——
     * 方法本身不阻塞，这也是"有界队列 + offer"取代"无界队列"的意义。
     *
     * @param request 需要处理的请求（将由漏出线程同步执行，执行期间桶的其他漏出暂停）
     * @return 是否成功加入队列（true=已接管，后续会被处理；false=桶满或已关闭，请求未入队）
     */
    public boolean submitRequest(Runnable request) {
        if (!accepting.get()) {
            return false;
        }
        // 尝试将请求加入队列，队列满则返回false
        return queue.offer(request);
    }

    /**
     * leakTick —— 漏出线程每 1ms 跑一次的匀速漏出逻辑。
     * <p>
     * <h2>要解决的问题</h2>
     * 需求是"每秒处理 leakRate 个请求"，但定时任务是固定 1ms 一跳。两个数字对不上：
     * leakRate=3 时，平均 333,333,333 纳秒才该漏 1 个，一个 1ms 的 tick 里往往
     * "还不够漏一个"；反过来若某次 tick 被 GC 或慢请求耽误了 100ms，这 100ms 里
     * 该漏的 0.3 个也不能就这样丢掉。所以需要一套记账办法，把"任意长的真实时间"
     * 换算成"应漏多少个"，误差跨 tick 结转。
     * <p>
     * <h2>记账模型：把时间当钱</h2>
     * 想象每个请求的漏出要"付"一笔时间：<b>漏 1 个请求的价格
     * perTokenNanos = 10亿纳秒 ÷ leakRate</b>（leakRate=3 → 每个约 3.33 亿纳秒）。
     * 然后：
     * <ul>
     * <li><b>攒钱</b>：每次 tick 把距上次 tick 真实流逝的纳秒数存进 pendingNanos
     * （配额池，就是"存款"）；</li>
     * <li><b>算能买几个</b>：应漏数 due = 存款 ÷ 单价。存了 7 亿、单价 3.33 亿
     * → due = 2，还余 3,333,334 纳秒零头；</li>
     * <li><b>整笔花掉</b>：due × 单价从存款里扣走，注意扣的是"全部应漏数"
     * 的钱——哪怕下面真正漏出的少于 due（队空了、被 capacity 截断），多付的钱也不退。
     * 为什么不退：漏桶是"匀速"语义，错过的水滴就是错过了，不存在攒余额补漏；
     * 只有除不尽的零头才留在池里滚存（见攒钱例 2）。</li>
     * </ul>
     * <h2>数字例子（leakRate=3，单价 333,333,333ns，容量 10）</h2>
     * <ol>
     * <li><b>攒</b>：tick0 存款 0；tick1 流逝 1ms → 存 1,000,000ns，due = 1,000,000
     * ÷ 333,333,333 = 0 → 直接返回，一个都不漏，存款留着。约 334 个 tick（333ms）
     * 后存款凑够一个单价，才漏出第 1 个——宏观上就是 0.33 秒漏 1 个 = 每秒 3 个；</li>
     * <li><b>结转</b>：凑够后存款 333,334,000，due=1，扣 333,333,333，剩 667ns
     * 滚存到下一轮——长期平均速率因此精确等于 leakRate，不向下取整吃亏；</li>
     * <li><b>队空清零</b>：桶空闲 10 秒后只有 1 个请求入队。此刻存款已高达 10ms
     * = 30 个单价，due=30 但只有 1 个可漏。循环第 2 次 poll 拿到 null → 把剩余
     * 存款清零、退出。若不清零，这 29 个"空闲攒出来的钱"会在下个 tick 起继续
     * 兑换，把随后到达的请求成串放行——突发流量被瞬时消化，等于放弃了匀速。
     * 这就是字段注释里"队列空了不允许下个 tick 一次性连发"的落点。</li>
     * </ol>
     * <h2>两个易忽略细节</h2>
     * <ul>
     * <li>花掉 due×单价 发生在 min(due, capacity) 截断<b>之前</b>：截断只限制
     * 本轮最多漏 capacity 个（防一个极慢请求执行了 10 分钟后，下个 tick 因攒了
     * 海量配额而连发数千个），但账要一次结清，逻辑更简单、也不留"补偿"入口；</li>
     * <li>Math.max(1, ...) 防泄漏：leakRate 若配到超过 10 亿，1e9/leakRate 整除
     * 得 0，后面除法直接除零崩溃，下限压到 1ns；</li>
     * <li>首次 tick 的 lastNanos=0 特殊处理：System.nanoTime() 是 JVM 启动以来的
     * 纳秒值（通常几十亿），不先把基准设为 now 的话，第一次就"存"进几十秒的配额，
     * 开局桶被连发打满。</li>
     * </ul>
     * <p>
     * <b>异常纪律</b>：任务体内必须不向外抛异常——scheduleAtFixedRate 的任务一旦抛出
     * 未捕获异常，后续所有执行会被取消，桶从此只进不出。runRequest 内部已自行兜住
     * 请求异常，外层再包一道 try/catch 兜住框架异常，双保险。
     */
    private void leakTick() {
        try {
            //① 攒钱: 把距上次 tick 真实流逝的纳秒存进配额池
            long now = System.nanoTime();
            if (lastNanos == 0) {
                lastNanos = now; //首次 tick 把基准设为当前, 不把"对象创建到首次执行"的空档记成配额
            }
            pendingNanos += now - lastNanos;
            lastNanos = now;
            //② 单价: 漏 1 个请求要付的时间 = 1秒 ÷ leakRate; max(1,..) 防 leakRate>10亿时整除为 0 导致除零
            long perTokenNanos = Math.max(1, 1_000_000_000L / leakRate);
            //③ 存款能买几个漏出名额(除不尽的零头留在池里滚存, 保证长期速率精确)
            long due = pendingNanos / perTokenNanos;
            if (due <= 0) {
                return; //还不够漏 1 个的配额, 全部结转, 本 tick 什么都不做
            }
            //④ 整笔付账: 扣全部 due 个名额的钱, 哪怕实际漏出更少也不退——匀速语义,
            //错过的时间不补; 也发生在 capacity 截断之前, 慢任务跑完回来不做补偿式连发
            pendingNanos -= due * perTokenNanos;
            due = Math.min(due, capacity); //本轮最多漏 capacity 个
            for (long i = 0; i < due; i++) {
                Runnable request = queue.poll();
                if (request == null) {
                    pendingNanos = 0; //队列空了, 空闲攒下的余额清零作废, 防下个 tick 连发(见 javadoc 例3)
                    return;
                }
                runRequest(request); //在漏出线程内同步执行, 执行期间其余 tick 排队等待
            }
        } catch (Exception e) {
            //P2-33: 修复前用 System.err, 库代码统一走 slf4j
            log.error("漏桶处理出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行一个漏出的请求。异常在此兜住：单个业务请求抛异常不能让漏出线程死亡、
     * 不能阻断后续请求的处理（只记 ERROR 日志），也不向 leakTick 传播。
     */
    private void runRequest(Runnable request) {
        try {
            request.run();
            if (log.isDebugEnabled()) {
                //P2-33: 修复前每个漏出的请求打一条 System.out, 永不停; 降为 debug 级别
                log.debug("处理请求，当前队列剩余: {}", queue.size());
            }
        } catch (Exception e) {
            log.error("处理请求出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 纯限流语义的"过闸"（RateLimiter 接口方法）：不携带任务的计数型放行。
     * 实现是往队列塞一个空 Runnable 占位——占一个在途名额，等下一个 tick 漏掉。
     * offer 的原子性同时完成"未满判定 + 入队"，避免先查 size 再 offer 的
     * check-then-act 竞态（两个线程同时看到 size=capacity-1 会双双挤进）。
     *
     * @return true=放行（已占位在途配额）；false=桶满或已关闭
     */
    @Override
    public boolean canPass() {
        // 通过原子入队占位，避免 size 检查的 TOCTOU 竞态
        return submitRequest(() -> {});
    }

    /**
     * 关闭漏桶，不再接受新请求，处理完队列中剩余请求。
     * 三步：① accepting=false 拒新单；② 向漏出线程排队一个 drainAll 排空任务
     * （排空在漏出线程上执行，与正常漏出串行，无双线程抢队列）；
     * ③ 关调度器并阻塞等待——排空任务跑完线程才退出。
     * 若队列里有慢任务导致等待超过 60 秒，会中断等待并把未处理数量以 WARN 日志报出，
     * 不再无提示丢弃。
     */
    public void shutdown() {
        //P2-33: 修复前 javadoc 承诺"处理完队列中剩余请求", 实现却是 isRunning=false 后
        //漏出循环每 tick 直接 return——对外已承诺接收(submitRequest 已返回 true)的任务被丢弃:
        //不执行、不计数、无日志。现在真正排空: 停止接收 → 排队一个排空任务 → 等它执行完。
        accepting.set(false);
        scheduler.execute(this::drainAll);
        scheduler.shutdown(); //不再接受新任务; 已排队的 tick 与 drainAll 照常执行完
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        int leftover = queue.size();
        if (leftover > 0) {
            //只有超时/被中断才会剩, 剩余数如实报出
            log.warn("漏桶关闭超时, 队列中仍有 {} 个请求未处理", leftover);
        } else if (log.isDebugEnabled()) {
            log.debug("漏桶已关闭, 队列已排空");
        }
    }

    /**
     * 与 shutdown() 同义, 让 try-with-resources 可用（P2-3: RateLimiter 接口已继承 AutoCloseable）。
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * 关闭排空任务：忽略漏出速率，把队列里剩余请求全部执行完。
     * 由 shutdown() 排在漏出线程上执行，因此执行期间不可能再有新请求提交
     * （accepting 已为 false），poll 到 null 即真正排空。
     * 注意：排空阶段不再限速——语义是"已承诺的请求必须交付"，
     * 若队列积压很深、下游经不起连发，应在提交侧控制总入队量而不是指望这里匀速。
     */
    private void drainAll() {
        Runnable request;
        while ((request = queue.poll()) != null) {
            runRequest(request);
        }
    }
}
