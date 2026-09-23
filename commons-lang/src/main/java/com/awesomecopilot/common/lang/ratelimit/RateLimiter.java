package com.awesomecopilot.common.lang.ratelimit;

/**
 * <p>
 * Copyright: (C), 2022-11-18 15:11
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public interface RateLimiter extends AutoCloseable {
	
	/**
	 * 判断是否放行, 如果放行, 计数器+1
	 * @return
	 */
	public boolean canPass();
	
	/**
	 * 释放限流器占用的后台资源(如补令牌/漏出用的调度线程)。
	 * 无后台资源的实现(如 SlidingWindow)沿用默认空实现。
	 * <p>
	 * P2-3: 声明为 AutoCloseable 后, 持有线程池的限流器可以放进 try-with-resources,
	 * 用完必须显式释放, 否则调度线程会让限流器实例与它引用的对象永远无法回收。
	 */
	@Override
	default void close() {
	}
}
