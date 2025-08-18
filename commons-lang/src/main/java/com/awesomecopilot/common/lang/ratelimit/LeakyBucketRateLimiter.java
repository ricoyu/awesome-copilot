package com.awesomecopilot.common.lang.ratelimit;

import java.util.concurrent.*;
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
 * 特点：请求先进入队列，然后按固定速率从队列中取出处理
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    private final int capacity;                  // 桶的容量(最大队列长度)
    private final BlockingQueue<Runnable> queue; // 请求队列
    private final int leakRate;                  // 漏出速率(每秒处理的请求数)
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;

    /**
     * @param capacity 桶的容量(最大队列长度)
     * @param leakRate 漏出速率(每秒处理的请求数)
     */
    public LeakyBucketRateLimiter(int capacity, int leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.scheduler = Executors.newScheduledThreadPool(2); // 1个用于定时漏出，1个用于处理任务
        this.isRunning = new AtomicBoolean(true);
        
        // 启动漏桶的定时漏出任务
        // 计算每次处理的间隔时间(毫秒)
        long interval = 1000 / leakRate;
        scheduler.scheduleAtFixedRate(this::processRequests, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试提交请求到漏桶
     * @param request 需要处理的请求
     * @return 是否成功加入队列
     */
    public boolean submitRequest(Runnable request) {
        if (!isRunning.get()) {
            return false;
        }
        // 尝试将请求加入队列，队列满则返回false
        return queue.offer(request);
    }

    /**
     * 按固定速率从队列中取出请求并处理
     */
    private void processRequests() {
        if (!isRunning.get()) {
            return;
        }
        try {
            // 从队列中取出一个请求处理g
            Runnable request = queue.poll();
            if (request != null) {
                // 异步处理请求，不阻塞定时任务
                scheduler.execute(() -> {
                    try {
                        request.run();
                    } catch (Exception e) {
                        // 处理请求时的异常
                        System.err.println("处理请求出错: " + e.getMessage());
                    }
                });
                System.out.println("处理请求，当前队列剩余: " + queue.size());
            }
        } catch (Exception e) {
            System.err.println("漏桶处理出错: " + e.getMessage());
        }
    }

    @Override
    public boolean canPass() {
        // 兼容RateLimiter接口，仅判断是否能加入队列
        return queue.size() < capacity;
    }

    /**
     * 关闭漏桶，不再接受新请求，处理完队列中剩余请求
     */
    public void shutdown() {
        isRunning.set(false);
        // 等待队列处理完成后关闭线程池
        scheduler.schedule(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            System.out.println("漏桶已关闭");
        }, 1, TimeUnit.SECONDS);
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
