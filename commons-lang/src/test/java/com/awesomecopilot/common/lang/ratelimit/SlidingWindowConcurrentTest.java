package com.awesomecopilot.common.lang.ratelimit;

import com.awesomecopilot.common.lang.ratelimit.SlidingWindow;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowConcurrentTest {

    // 测试配置
    private static final int THREAD_COUNT = 50;      // 并发线程数
    private static final int REQUESTS_PER_THREAD = 100; // 每线程请求数
    private static final long WINDOW_SIZE_SEC = 1;    // 时间窗口大小(秒)
    private static final int MAX_REQUESTS = 200;      // 窗口内允许的最大请求数

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        // 初始化限流器（1秒窗口，最多200请求）
        SlidingWindow slidingWindow = new SlidingWindow(
                WINDOW_SIZE_SEC, TimeUnit.SECONDS, MAX_REQUESTS);

        // 线程安全的计数器
        AtomicInteger passedRequests = new AtomicInteger(0);
        AtomicInteger rejectedRequests = new AtomicInteger(0);

        // 使用CountDownLatch协调线程
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // 记录测试开始时间
        long startTime = System.currentTimeMillis();

        // 提交所有任务
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始

                    for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                        if (slidingWindow.canPass()) {
                            passedRequests.incrementAndGet();
                        } else {
                            rejectedRequests.incrementAndGet();
                        }
                        // 随机间隔0-5ms模拟网络延迟
                        Thread.sleep(ThreadLocalRandom.current().nextInt(5));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 触发所有线程开始执行
        startLatch.countDown();
        // 等待所有线程完成
        endLatch.await();

        // 计算测试耗时（秒）
        double elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0;

        // 输出测试结果
        System.out.println("\n=== 测试结果 ===");
        System.out.println("并发线程数: " + THREAD_COUNT);
        System.out.println("每线程请求数: " + REQUESTS_PER_THREAD);
        System.out.println("总请求数: " + (THREAD_COUNT * REQUESTS_PER_THREAD));
        System.out.println("实际通过数: " + passedRequests.get());
        System.out.println("实际拒绝数: " + rejectedRequests.get());
        System.out.printf("测试耗时: %.2f秒\n", elapsedSec);
        System.out.printf("平均QPS: %.1f\n", passedRequests.get() / elapsedSec);

        // 验证1：总通过数不超过理论最大值
        int expectedMaxPassed = (int) (MAX_REQUESTS * Math.ceil(elapsedSec));
        assertTrue(passedRequests.get() <= expectedMaxPassed,
                "实际通过数(" + passedRequests.get() + ")超过理论最大值(" + expectedMaxPassed + ")");

        // 验证2：拒绝数应该存在（因为总请求数远大于单窗口容量）
        assertTrue(rejectedRequests.get() > 0, "高并发场景下应该有请求被拒绝");

        // 验证3：瞬时QPS不超过限制
        assertTrue((passedRequests.get() / elapsedSec) <= MAX_REQUESTS * 1.1,
                "实际QPS超过限制的10%容差范围");

        executor.shutdown();
    }
}