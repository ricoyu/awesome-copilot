package com.awesomecopilot.common.lang.ratelimit;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * 滑动时间窗口限流器（精确计数：滑动窗口日志法）
 * <p>
 * <b>算法思想</b>：不像固定窗口算法那样把时间切成一格一格（例如"1 分钟"切成
 * 0~59 秒两个窗口，窗口结束计数清零），而是为<b>每一个放行的请求</b>记一条到达时间戳。
 * "窗口"不是一个真实存在的容器，而是一个随时间向前移动的区间 [now - windowSizeMillis, now]：
 * 每次有请求进来，先把早于区间左端点的旧记录从队头逐条丢弃，剩下的记录数就是
 * "最近 windowSizeMillis 内已经放行了多少请求"，没超过 maxRequests 就放行并补记一条。
 * <p>
 * <b>相比固定窗口解决了什么</b>：固定窗口在两个窗口的交界处会出问题——窗口 N 的最后
 * 一瞬间和窗口 N+1 的最前一瞬各放满 maxRequests，合起来在一个 windowSize 时长内
 * 实际通过了 2 倍量（边界突发）。本实现把窗口随每一毫秒向前滑，任意一个
 * windowSizeMillis 时长片段内通过的请求数都严格不超过 maxRequests。
 * <p>
 * <b>代价</b>：内存与放行请求数成正比——每个通过的请求存一个 Long 时间戳。
 * 适合窗口时长较短、QPS 不高的场景；高 QPS 场景要换"把窗口切成小格、每格只记计数"
 * 的近似滑动窗口（内存恒定，但换回一点边界误差）。
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
	/*
	 * 管道左边（head / first端）◄───────管道───────► 管道右边（tail / last端）
	 * firstElement                                  lastElement
	 * <p>
	 * 存的是每个放行请求的到达时间戳（毫秒），入队顺序即时间先后，所以队列
	 * 天然按时间升序：队头永远是最老的记录。判断"哪些记录过期"只需要从队头
	 * 往后看——一旦队头没过期，它后面的必然也没过期，清理循环即可停止。
	 * <p>
	 * ConcurrentLinkedDeque 自身虽是线程安全的，但本类的正确性靠的是 canPass()
	 * 上的 synchronized："清理过期 → 数个数 → 入队"必须作为一个整体执行。
	 * 若不加锁，两个线程可能同时读到 size < maxRequests 然后都入队，
	 * 窗口内实际放行数会超过 maxRequests。
	 */
	private final ConcurrentLinkedDeque<Long> requestTimestamps = new ConcurrentLinkedDeque<>();

	/**
	 * 窗口长度（毫秒）。一条放行记录只有在 [当前时刻 - windowSizeMillis, 当前时刻]
	 * 这个区间内才算"数"，比左端点更早的一律作废清理掉。
	 * 由构造参数 windowSize + timeUnit 换算而来，是"多长时间内"这个配置的单一刻度。
	 */
	private final long windowSizeMillis;  //多长时间内

	/**
	 * 窗口配额：任意滑动窗口内最多允许通过的请求数。
	 * 放行条件即"未过期的记录数 &lt; maxRequests"。
	 */
	private final int maxRequests;        //最多允许多少个请求

	/**
	 * @param windowSize 窗口长度，配合 timeUnit 使用（例如 1 + MINUTES = 每分钟）
	 * @param timeUnit   windowSize 的时间单位，构造时统一换算成毫秒，避免热路径重复换算
	 * @param maxRequests 窗口内最多放行的请求数
	 */
	public SlidingWindow(long windowSize, TimeUnit timeUnit, int maxRequests) {
		this.windowSizeMillis = timeUnit.toMillis(windowSize);
		this.maxRequests = maxRequests;
	}

	/**
	 * 尝试放行一次请求，整体分三步（synchronized 保证三步之间没有其他线程插队）：
	 * <ol>
	 * <li><b>清理过期</b>：从队头开始，凡是距今已超过 windowSizeMillis 的记录
	 * （即已滑出窗口左端点）逐条弹出。队列按时间升序，队头未过期则后面必然未过期，循环停止；</li>
	 * <li><b>判断配额</b>：剩余（未过期）记录数 &lt; maxRequests 才允许通过；</li>
	 * <li><b>记账</b>：通过则把当前时间戳追加到队尾，成为下一次判断的"窗口内请求"之一。
	 * 被拒绝的请求不留记录——没放行的请求不消耗配额。</li>
	 * </ol>
	 * 注意"过期"用的是严格大于（{@code > windowSizeMillis}）：恰好等于窗口长度的
	 * 边界记录仍算窗口内，配额含义是"任意 windowSizeMillis 时长（含两端）内不超过 maxRequests"。
	 *
	 * @return true=放行；false=窗口内已达配额，拒绝
	 */
	public synchronized boolean canPass() {
		long currentTime = System.currentTimeMillis();
		// 第一步：移除过期的请求记录（窗口整体向前滑）
		while (!requestTimestamps.isEmpty() &&
				currentTime - requestTimestamps.peekFirst() > windowSizeMillis) {
			requestTimestamps.pollFirst();
		}
		// 第二步：检查当前窗口是否超限
		if (requestTimestamps.size() < maxRequests) {
			// 第三步：记录本次放行（入队尾）
			requestTimestamps.addLast(currentTime);
			return true;
		}
		return false;
	}
}
