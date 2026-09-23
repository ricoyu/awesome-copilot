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
     * 每个 1ms tick 的漏出逻辑，把"真实流逝的时间"换算成"应漏出的请求数"：
     * <ol>
     * <li>累计流逝纳秒进 pendingNanos（配额池）；</li>
     * <li>应漏数 due = 配额池 ÷ 单请求配额（1e9/leakRate 纳秒），本轮配额全额扣走；</li>
     * <li>due 封顶 capacity 后逐个 poll + 执行。队列先空则清零剩余配额并退出——
     * 空闲时间攒出来的配额不允许在"队列刚有货"时一次性连发，
     * 保证输出速率恒定（漏桶的匀速语义）。</li>
     * </ol>
     * <b>为什么不直接"每 tick 漏固定个数"</b>：调度间隔不精确（GC、任务占用、线程竞争），
     * 固定个数会让长期速率偏离配置。按流逝时间换算则无论 tick 怎么抖动，
     * 单位时间内漏出的请求数恒等于 leakRate。
     * <p>
     * <b>异常纪律</b>：任务体内必须不向外抛异常——scheduleAtFixedRate 的任务一旦抛出
     * 未捕获异常，后续所有执行会被取消，桶从此只进不出。runRequest 内部已自行兜住
     * 请求异常，外层再包一道 try/catch 兜住框架异常，双保险。
     */
    private void leakTick() {
        try {
            long now = System.nanoTime();
            if (lastNanos == 0) {
                lastNanos = now; //首次 tick 把基准设为当前, 不把"对象创建到首次执行"的空档记成配额
            }
            pendingNanos += now - lastNanos;
            lastNanos = now;
            //单个请求占用的漏出时间配额, 纳秒
            long perTokenNanos = Math.max(1, 1_000_000_000L / leakRate);
            long due = pendingNanos / perTokenNanos;
            if (due <= 0) {
                return; //不足漏 1 个的配额, 全部结转
            }
            //配额全额扣掉, 超出本轮处理能力(capacity)的部分作废——慢任务执行很久之后不做补偿式连续放行
            pendingNanos -= due * perTokenNanos;
            due = Math.min(due, capacity);
            for (long i = 0; i < due; i++) {
                Runnable request = queue.poll();
                if (request == null) {
                    pendingNanos = 0; //队列空了, 攒下的配额作废, 不允许下个 tick 一次性连发
                    return;
                }
                runRequest(request); //在漏出线程内同步执行
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

    // 测试用例
    public static void main(String[] args) throws InterruptedException {
        // 容量10，每秒处理2个请求
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(10, 2);

        // 模拟突发20个请求
        for (int i = 1; i <= 20; i++) {
            final int requestId = i;
            boolean accepted = limiter.submitRequest(() -> {
                System.out.println("处理请求 " + requestId);
                try {
                    // 模拟处理耗时
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            System.out.println("请求 " + i + ": " + (accepted ? "已加入队列" : "被拒绝"));
        }

        // 等待所有请求处理完成
        Thread.sleep(10000);
        limiter.shutdown();
    }
}
