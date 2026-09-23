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
 *
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
	private long pendingRefillMillis;     // 累积未兑换成令牌的毫秒配额（定时任务单线程访问）

	/**
	 * 令牌桶限流器
	 *
	 * @param capacity         桶的最大容量 (令牌数)
	 * @param refillRate       每秒补充的令牌数
	 * @param refillIntervalMs 补充令牌的时间间隔(毫秒)
	 */
	public TokenBucketRateLimiter(long capacity, long refillRate, long refillIntervalMs) {
		//P2-33: 修复前零参数校验——refillRate<=0 时令牌只出不进, 桶空后全量拒绝且无任何告警;
		//capacity<=0 时永远拒绝。孪生类 LeakyBucketRateLimiter 有同款校验, 这里补齐。
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		if (refillRate <= 0) {
			throw new IllegalArgumentException("refillRate must be positive");
		}
		if (refillIntervalMs <= 0) {
			throw new IllegalArgumentException("refillIntervalMs must be positive");
		}
		this.capacity = capacity;
		this.tokens = new AtomicLong(capacity); // 初始时桶是满的
		this.refillRate = refillRate;
		this.refillIntervalMs = refillIntervalMs;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			//P2-3: 修复前默认线程工厂是非 daemon, 忘记 shutdown() 会挂住 JVM 退不出去
			Thread t = new Thread(r, "token-bucket-refill");
			t.setDaemon(true);
			return t;
		});

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
		pendingRefillMillis += refillRate * refillIntervalMs;
		long tokensToAdd = pendingRefillMillis / 1000;
		if (tokensToAdd <= 0) {
			return;
		}
		//只扣除真正兑换成令牌的那部分毫秒配额(保持整除关系), 尾数留在池子里下次再兑
		pendingRefillMillis -= tokensToAdd * 1000;
		//P2-3: 修复前是 get -> 算新值 -> compareAndSet, CAS 失败(说明有并发 acquire)就直接返回,
		//而 tokensToAdd 已经从 pendingRefillMillis 扣掉了——本轮增量作废。
		//实测(高水位 8 消费者压测)46 万注入丢失 1~2 万令牌, 实际放行速率低于配置值。
		//改为 accumulateAndGet 原子累加并封顶: 无论并发怎么扣减, 增量都完整入账。
		tokens.accumulateAndGet(tokensToAdd, (current, add) -> {
			long updated = current + add;
			//current<=capacity 恒成立(入口满桶且每次累加都封顶), capacity 受构造校验为正数,
			//相加溢出时结果必然远超 capacity, 收敛到上限即可, 不需要单独判溢出
			return updated > capacity ? capacity : updated;
		});
		if (log.isDebugEnabled()) {
			log.debug("[Refill] Tokens: {}", tokens.get()); // 调试日志
		}
	}

	/**
	 * 尝试获取一个令牌
	 *
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

	/**
	 * 释放补令牌用的调度线程; 与 shutdown() 同义, 让 try-with-resources 可用（P2-3）。
	 */
	@Override
	public void close() {
		shutdown();
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