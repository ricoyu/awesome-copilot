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
 * 带请求队列的漏桶算法实现
 * <p>
 * 特点：请求先进入队列，然后按固定速率从队列中取出，由漏出线程直接执行
 * （在途量 = 队列 + 正在执行的 1 个，桶容量就是实际在途上界）
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LeakyBucketRateLimiter.class);

    private final int capacity;                  // 桶的容量(最大队列长度)
    private final BlockingQueue<Runnable> queue; // 请求队列
    private final int leakRate;                  // 漏出速率(每秒处理的请求数)
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean accepting = new AtomicBoolean(true);

    //lastNanos/pendingNanos 只被单线程 scheduler 访问(漏出任务与 drainAll 在同一线程排队执行), 无需同步
    private long lastNanos;
    private long pendingNanos;

    /**
     * @param capacity 桶的容量(最大队列长度)
     * @param leakRate 漏出速率(每秒处理的请求数)
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
     * 尝试提交请求到漏桶
     * @param request 需要处理的请求
     * @return 是否成功加入队列
     */
    public boolean submitRequest(Runnable request) {
        if (!accepting.get()) {
            return false;
        }
        // 尝试将请求加入队列，队列满则返回false
        return queue.offer(request);
    }

    /**
     * 按固定速率从队列中取出请求并在本(漏出)线程执行
     */
    private void leakTick() {
        try {
            long now = System.nanoTime();
            if (lastNanos == 0) {
                lastNanos = now;
            }
            pendingNanos += now - lastNanos;
            lastNanos = now;
            long perTokenNanos = Math.max(1, 1_000_000_000L / leakRate);
            long due = pendingNanos / perTokenNanos;
            if (due <= 0) {
                return;
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
                runRequest(request);
            }
        } catch (Exception e) {
            //P2-33: 修复前用 System.err, 库代码统一走 slf4j
            log.error("漏桶处理出错: {}", e.getMessage(), e);
        }
    }

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

    @Override
    public boolean canPass() {
        // 通过原子入队占位，避免 size 检查的 TOCTOU 竞态
        return submitRequest(() -> {});
    }

    /**
     * 关闭漏桶，不再接受新请求，处理完队列中剩余请求。
     * 本方法会阻塞到排空完成；若队列里有慢任务导致等待超过 60 秒，
     * 会中断等待并把未处理数量以 WARN 日志报出，不再无提示丢弃。
     */
    public void shutdown() {
        //P2-33: 修复前 javadoc 承诺"处理完队列中剩余请求", 实现却是 isRunning=false 后
        //漏出循环每 tick 直接 return——对外已承诺接收(submitRequest 已返回 true)的任务被丢弃:
        //不执行、不计数、无日志。现在真正排空: 停止接收 → 排队一个排空任务 → 等它执行完。
        accepting.set(false);
        scheduler.execute(this::drainAll);
        scheduler.shutdown();
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
