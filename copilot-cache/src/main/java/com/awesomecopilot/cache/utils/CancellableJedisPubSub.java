package com.awesomecopilot.cache.utils;

import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 可取消的 JedisPubSub: 解决"订阅还没真正建立就取消"的竞态。
 * <p>
 * 背景: JedisUtils.subscribe 把订阅动作放到线程池排队执行, 调用方拿到本对象时订阅任务
 * 可能还没轮到运行。此时父类 unsubscribe() 会因内部 client 尚未赋值直接抛 JedisException;
 * 更麻烦的是排队中的订阅任务随后照常建立订阅, 没人取消它, 那条连接将一直被占用。
 * <p>
 * 本类的约定:
 * <ul>
 *     <li>{@link #cancel()}: 标记取消。订阅任务尚未开始时, 任务在建立订阅前检查到此标记会直接放弃;
 *     订阅已建立时, 行为与父类 unsubscribe 一致(发送 UNSUBSCRIBE 退出)</li>
 *     <li>{@link #onSubscribe}: 若取消标记已置位(取消发生在 SUBSCRIBE 命令刚发出、
 *     确认消息尚未被本对象处理的窗口内), 收到订阅确认后立即补发 UNSUBSCRIBE, 不会把连接晾在订阅状态</li>
 * </ul>
 * <p>
 * Copyright: (C), 2026/9/22
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class CancellableJedisPubSub extends JedisPubSub {

	/**
	 * 取消标记; cancel() 由业务线程调用, 读取方是订阅线程池线程, volatile 保证可见性
	 */
	private volatile boolean cancelled;

	/**
	 * 订阅是否已真正建立(proceed 已开始运行并处理过 SUBSCRIBE 响应).
	 * 由订阅线程置位, cancel() 用它决定"发 UNSUBSCRIBE"还是"只放弃、连接归还"
	 */
	private volatile boolean subscribed;

	/**
	 * 标记取消, 并在订阅确已建立时尝试发送 UNSUBSCRIBE。
	 * <p>
	 * cancel 后调用方即可安全归还/丢弃自己这一侧的资源:
	 * 未建立的订阅根本不会建立, 已建立的要么 unsubscribe 成功, 要么由 onSubscribe 回调补发退出。
	 *
	 * @return true 表示 UNSUBSCRIBE 已发出; false 表示订阅未建立(或发送失败), 调用方无需等待退订确认
	 */
	public boolean cancel() {
		cancelled = true;
		if (!subscribed) {
			return false;
		}
		try {
			unsubscribe();
			return true;
		} catch (JedisException e) {
			// subscribed 与 proceed 结束之间无同步屏障, client 可能刚好被并发置空; 此时订阅循环已退出, 忽略
			return false;
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isSubscriptionActive() {
		return subscribed;
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		subscribed = true;
		// 取消发生在 SUBSCRIBE 已发出、确认刚到达的窗口: 立即补发 UNSUBSCRIBE, 不晾着连接
		if (cancelled) {
			try {
				unsubscribe();
			} catch (JedisException ignored) {
				// 订阅循环即将退出, 无需处理
			}
		}
	}

	@Override
	public void onPSubscribe(String pattern, int subscribedChannels) {
		subscribed = true;
		if (cancelled) {
			try {
				punsubscribe();
			} catch (JedisException ignored) {
				// 同上
			}
		}
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		subscribed = subscribedChannels > 0;
	}

	@Override
	public void onPUnsubscribe(String pattern, int subscribedChannels) {
		subscribed = subscribedChannels > 0;
	}
}
