package com.awesomecopilot.common.lang.ratelimit;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * 滑动时间窗口
 * <p>
 * Copyright: (C), 2022-11-18 14:53
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SlidingWindow implements RateLimiter {
	private final ConcurrentLinkedDeque<Long> requestTimestamps = new ConcurrentLinkedDeque<>();
	private final long windowSizeMillis;  //多长时间内
	private final int maxRequests;        //最多允许多少个请求

	public SlidingWindow(long windowSize, TimeUnit timeUnit, int maxRequests) {
		this.windowSizeMillis = timeUnit.toMillis(windowSize);
		this.maxRequests = maxRequests;
	}

	public synchronized boolean canPass() {
		long currentTime = System.currentTimeMillis();
		// 移除过期的请求记录
		while (!requestTimestamps.isEmpty() &&
				currentTime - requestTimestamps.peekFirst() > windowSizeMillis) {
			requestTimestamps.pollFirst();
		}
		// 检查当前窗口是否超限
		if (requestTimestamps.size() < maxRequests) {
			requestTimestamps.addLast(currentTime);
			return true;
		}
		return false;
	}
}
