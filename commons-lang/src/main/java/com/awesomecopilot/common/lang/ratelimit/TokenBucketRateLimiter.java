package com.awesomecopilot.common.lang.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 令牌桶限流器
 * <p>
 * 以固定速率向桶中添加令牌, 请求需获取令牌才能通过
 * <p/>
 * Copyright: Copyright (c) 2025-07-25 9:02
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);
    private final long capacity;          // 桶的最大容量 (令牌数)
    private final AtomicLong tokens;      // 当前桶中的令牌数量
    private final long refillRate;        // 每秒补充的令牌数
    private final long refillIntervalMs;  // 补充令牌的时间间隔(毫秒)
    private final ScheduledExecutorService scheduler;

    /**
     * 令牌桶限流器
     * @param capacity   桶的最大容量 (令牌数)
     * @param refillRate 每秒补充的令牌数
     * @param refillIntervalMs 补充令牌的时间间隔(毫秒)
     */
    public TokenBucketRateLimiter(long capacity, long refillRate, long refillIntervalMs) {
        this.capacity = capacity;
        this.tokens = new AtomicLong(capacity); // 初始时桶是满的
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // 启动定时任务：按固定速率补充令牌
        scheduler.scheduleAtFixedRate(
            this::refillTokens,
            this.refillIntervalMs,
            this.refillIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 补充令牌（由定时任务调用）
     */
    private void refillTokens() {
        long currentTokens = tokens.get();
        long newTokens = Math.min(capacity, currentTokens + refillRate); // 不超过容量
        tokens.compareAndSet(currentTokens, newTokens);
        if (log.isDebugEnabled()) {
            log.debug("[Refill] Tokens: " + newTokens); // 调试日志
        }
    }

    /**
     * 尝试获取一个令牌
     * @return true表示获取成功（请求允许通过），false表示失败（被限流）
     */
    public boolean canPass() {
        while (true) {
            long currentTokens = tokens.get();
            if (currentTokens <= 0) {
                return false; // 令牌不足，拒绝请求
            }
            // 尝试扣减令牌（CAS操作保证线程安全）
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
            // 如果CAS失败，说明其他线程修改了令牌数，重试; 重试是通过上面这个while循环实现的
        }
    }

    /**
     * 关闭限流器（释放资源）
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    // 测试用例
    public static void main(String[] args) throws InterruptedException {
        // 创建一个令牌桶：容量=10，每秒补充5个令牌
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 5, 1000);

        // 模拟20个请求，间隔200毫秒
        for (int i = 1; i <= 20; i++) {
            boolean acquired = limiter.canPass();
            System.out.printf("Request %2d: %s (Tokens: %d)%n",
                i,
                acquired ? "Accepted" : "Rejected",
                limiter.tokens.get()
            );
            Thread.sleep(200);
        }

        limiter.shutdown();
    }
}