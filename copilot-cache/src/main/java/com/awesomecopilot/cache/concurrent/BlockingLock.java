package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;
import com.awesomecopilot.cache.listeners.MessageListener;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 阻塞锁
 * <p>
 * 加锁流程: setnx 尝试 → 失败自旋 32 次(每次间隔 1ms) → 仍失败则 SUBSCRIBE 通知频道并 park(一个租期时长),
 * 被解锁消息唤醒或超时醒来后重试 setnx, 直到获取到锁.
 * <p>
 * 看门狗: 加锁成功后由 WatchDog 每 租期/3 秒续期一次(renew.lua, value 匹配才刷新过期时间).
 * <p>
 * 注意:
 * <ul>
 *     <li>不支持重入: 同一线程对同一实例重复 lock() 直接抛 OperationNotSupportedException,
 *         否则 setnx 要等自己写的 key 过期, 等于自己等待自己释放锁</li>
 *     <li>Redis Cluster 部署时订阅通知不可用(JedisClusterOperations.subscribe 未实现),
 *         等待者收不到解锁消息, 只能靠 park 超时醒来的重试, 获取锁的延迟最坏为一个租期</li>
 * </ul>
 * <p>
 * Copyright: (C), 2020/3/28 17:57
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class BlockingLock extends AbstractLock {

	/**
	 * 锁的模板
	 */
	private static final String LOCK_FORMAT = "copilot:blk:%s:lock";

	/**
	 * 当前线程自旋获取锁失败后, 会先订阅notifyChannel, 然后进入阻塞状态;
	 * 如果拿到锁的线程解锁, 会发布一条消息, 此时本线程被唤醒再次尝试获取锁
	 */
	private final ThreadLocal<JedisPubSub> subscribeThreadLocal = new ThreadLocal<>();

	public BlockingLock(String key) {
		this(key, 30);
	}

	/**
	 * 指定锁租期(秒)的构造器, 主要供测试与短任务使用
	 *
	 * @param key          锁业务名
	 * @param leaseSeconds 锁租期, 至少 2 秒(看门狗按 租期/3 的间隔调度续期)
	 */
	public BlockingLock(String key, int leaseSeconds) {
		super(LOCK_FORMAT, key, leaseSeconds);
	}

	/**
	 * 加锁, 阻塞直到获取到锁; 锁有效期为租期时长(默认30秒), 持有期间看门狗自动续期,
	 * 业务执行完必须调用 unlock() 释放
	 */
	@Override
	public void lock() {
		String lockValue = beginLock();
		String threadName = Thread.currentThread().getName();
		int spinCount = 0;
		try {
			/*
			 * 尝试第一次加锁, 加锁成功则置状态、取消订阅、启动看门狗并返回
			 */
			if (JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS)) {
				log.debug(">>>>>> {} 获取锁成功, key={}, value={} <<<<<<", threadName, key, lockValue);
				onLockAcquired(lockValue);
				return;
			}

			log.debug(">>>>>> {} 第一次没能成功获取锁, 开始自旋获取锁 <<<<<<", threadName);
			/**
			 * 尝试MAX_TIMED_SPINS次自旋获取锁, 加锁成功则置状态、取消订阅、启动看门狗并返回
			 */
			while (spinCount++ < MAX_TIMED_SPINS) {
				if (JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS)) {
					log.debug(">>>>>> {} 自旋{}次获取锁成功 <<<<<<", threadName, spinCount);
					onLockAcquired(lockValue);
					return;
				}
				TimeUnit.MILLISECONDS.sleep(1);
			}

			log.debug(">>>>>> {} 自旋失败, 进入阻塞等待 <<<<<<", threadName);
			/**
			 * 循环获取锁, 获取加锁成功则置状态、取消订阅、启动看门狗并返回
			 * 加锁失败挂起线程
			 */
			for ( ; ; ) {
				startListener();
				/**
				 * 阻塞一个租期时长后自动醒来
				 * 期间如果Listener收到通知, 则提前唤醒本线程
				 * 如果获取锁的线程一直没有解锁, 或者那个线程被杀死了, 也就是锁一直没有被释放,
				 * 本线程过完租期也会自动醒来(锁最迟在租期后过期), 防止死锁
				 */
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(defaultTimeout));
				if (Thread.interrupted()) {
					throw new LockThreadInterruptedException("线程被中断了");
				}
				if (JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS)) {
					log.debug(">>>>>> {} 醒来后终获成功, key={}, value={} <<<<<<", threadName, key, lockValue);
					onLockAcquired(lockValue);
					return;
				}
				log.debug("{} 醒来后仍然没有获取到锁, 准备再次进入阻塞状态, key={}", threadName, key);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LockThreadInterruptedException("线程在自旋等待锁时被中断");
		} finally {
			// 如果没拿到锁, 清理ThreadLocal状态, 避免线程池线程复用时的残留与订阅泄漏
			if (!lockedThreadLocal.get()) {
				clearHoldState();
				stopListener();
			}
		}
	}

	/**
	 * 获取锁成功后的处理: 公共部分(置状态+看门狗)之上, 额外取消解锁通知的订阅
	 */
	private void onLockAcquired(String lockValue) {
		holdLock(lockValue);
		// 取消订阅失败不影响锁本身(顶多订阅线程多存活一会儿), 捕获后只记日志
		try {
			stopListener();
		} catch (Exception e) {
			log.warn("取消锁通知订阅失败, key={}", key, e);
		}
	}

	@Override
	public void unlock() {
		doUnlock();
		/**
		 * 通知其他线程可以重新获取锁了, 把当前线程名作为消息发出去, 方便记log
		 */
		JedisUtils.publish(notifyChannel, Thread.currentThread().getName());
		log.debug(">>>>>> {} 发布消息, 现在其他线程可以重新获取锁, key={} <<<<<<",
				Thread.currentThread().getName(), key);
	}

	/**
	 * 订阅通知channel, 只会订阅一次
	 * <p>
	 * 通过判断subscribeThreadLocal里面是否已经有JedisPubSub来判断是否已经订阅了
	 * <p>
	 * 已经订阅就不再订阅
	 */
	public void startListener() {
		if (subscribeThreadLocal.get() == null) {
			/*
			 * 因为当前线程自旋获取锁失败, 所以在startListener之后当前线程会被阻塞,
			 * 这里把当前线程传给NotifyListener, 这个在监听到事件后, 才可以知道要唤醒的线程是哪个
			 * 订阅本身是交给JedisPoolOperations.THREAD_POOL线程池去执行的
			 */
			subscribeThreadLocal.set(JedisUtils.subscribe(new NotifyListener(Thread.currentThread()),
					notifyChannel));
		}
	}

	public void stopListener() {
		JedisPubSub jedisPubSub = subscribeThreadLocal.get();
		if (jedisPubSub != null) {
			//先清引用再 unsubscribe: 即使 unsubscribe 抛异常(订阅线程尚未进入 SUBSCRIBE 的极端时序),
			//也不会留下"已取消但引用还在"的状态被重复 unsubscribe
			subscribeThreadLocal.remove();
			JedisUtils.unsubscribe(jedisPubSub, notifyChannel);
		}
	}

	private class NotifyListener implements MessageListener {

		private final Thread thread;

		public NotifyListener(Thread thread) {
			this.thread = thread;
		}

		@Override
		public void onMessage(String channel, String message) {
			log.debug("收到 {} 发来的消息, 准备唤醒线程: {}, channel: {}", message, thread.getName(), channel);
			LockSupport.unpark(thread);
		}
	}
}
