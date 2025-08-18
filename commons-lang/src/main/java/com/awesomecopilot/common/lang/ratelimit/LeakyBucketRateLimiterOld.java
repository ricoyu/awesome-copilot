package com.awesomecopilot.common.lang.ratelimit;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
 * <p/>
 * Copyright: Copyright (c) 2025-07-25 8:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LeakyBucketRateLimiterOld implements RateLimiter {
	private final int capacity;          // 桶的容量 (最大积压请求数)
	private final AtomicInteger water;   // 当前桶中的水量 (当前积压请求数)
	private final int leakRate;          // 漏出速率 (每秒漏出的请求数)
	private final ScheduledExecutorService scheduler;

	/**
	 * @param capacity 桶的容量 (最大积压请求数)
	 * @param leakRate 漏出速率 (每秒漏出的请求数)
	 */
	public LeakyBucketRateLimiterOld(int capacity, int leakRate) {
		this.capacity = capacity;
		this.water = new AtomicInteger(0);
		this.leakRate = leakRate;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();

		// 启动漏桶的定时漏出任务
		scheduler.scheduleAtFixedRate(this::leak, 1, 1, TimeUnit.SECONDS);
	}

	// 漏出请求（固定速率调用）, 不断降低当前桶中的水量
	private void leak() {
		int current = water.get();
		if (current > 0) {
			int newWater = Math.max(0, current - leakRate); // 漏出leakRate个请求
			water.compareAndSet(current, newWater);
			System.out.println("[Leak] Current water: " + newWater);
		}
	}

	// 尝试通过漏桶, 不断增加当前桶中的水量
	@Override
	public boolean canPass() {
		int retryCount = 0;
		int maxRetries = 3; // 最大重试次数
		while (retryCount < maxRetries) {
			int current = water.get();
			if (current >= capacity) {
				return false; // 桶已满，拒绝请求
			}
			// 尝试增加水量
			if (water.compareAndSet(current, current + 1)) {
				return true;
			}
			// CAS失败，重试前短暂休眠
			try {
				MILLISECONDS.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
			retryCount++;
		}
		return false; // 达到最大重试次数仍失败
	}

	// 关闭限流器
	public void shutdown() {
		scheduler.shutdown();
	}

	// 测试用例
	public static void main(String[] args) throws InterruptedException {
		LeakyBucketRateLimiterOld limiter = new LeakyBucketRateLimiterOld(10, 2); // 容量10, 每秒漏出2个

		// 模拟20个请求
		for (int i = 1; i <= 20; i++) {
			boolean acquired = limiter.canPass();
			System.out.println("Request " + i + ": " + (acquired ? "Accepted" : "Rejected"));
			//Thread.sleep(50); // 模拟请求间隔
		}

		limiter.shutdown();
	}

}