package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;
import com.awesomecopilot.cache.exception.OperationNotSupportedException;
import com.awesomecopilot.cache.utils.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Redis 分布式锁的公共底座: BlockingLock(阻塞锁)与 NonBlockingLock(非阻塞锁)共享的状态与动作.
 * <p>
 * 共同约定:
 * <ul>
 *     <li>锁的状态(值/是否持有/看门狗)按线程存放在 ThreadLocal 中, 因此同一个锁实例可以被多个
 *     线程(比如多个 Spring bean)安全共用</li>
 *     <li>不支持重入: 同一线程对同一实例重复加锁会直接抛异常, 而不是等待自己的锁过期</li>
 *     <li>加锁成功后由 WatchDog 每 租期/3 秒续期一次; 解锁流程固定为:
 *     检查持锁 -> 停看门狗 -> unlock.lua 比对 value 删除 -> 清理线程状态</li>
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
abstract class AbstractLock implements Lock, AutoCloseable {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected static final int NCPUS = Runtime.getRuntime().availableProcessors();

	/**
	 * The number of times to spin before blocking in timed waits.
	 * The value is empirically derived -- it works well across a
	 * variety of processors and OSes. Empirically, the best value
	 * seems not to vary with number of CPUs (beyond 2) so is just
	 * a constant.
	 */
	protected static final int MAX_TIMED_SPINS = (NCPUS < 2) ? 0 : 32;

	/**
	 * 分布式锁的key
	 */
	protected final String key;

	/**
	 * 解锁后在该channel上通知等待线程可以获取锁了(仅 BlockingLock 使用, NonBlockingLock 拿不到锁直接返回, 没有等待者)
	 */
	protected final String notifyChannel;

	/**
	 * 锁租期(秒)
	 */
	protected final int defaultTimeout;

	/**
	 * 锁的值, 解锁要用到; 解锁时提供的value跟redis中的匹配才可以解锁.
	 * <p>
	 * 将lockValue放到ThreadLocal中是为了支持锁实例被多个线程共享使用的场景,
	 * 比如被Springboot注入到多个bean中使用: 只有加锁的那个线程才能拿到自己写入的 lockValue 用来解锁
	 */
	protected final ThreadLocal<String> valueThreadLocal = new ThreadLocal<>();

	/**
	 * 当前线程是否持有这把锁
	 */
	protected final ThreadLocal<Boolean> lockedThreadLocal = ThreadLocal.withInitial(() -> false);

	/**
	 * 当前线程这一轮持锁的看门狗任务, unlock/停止时 stop;
	 * 按线程隔离, 实例被多线程复用互不干扰(修掉旧实现 watchDogStopped 实例字段置 true 后永不复位的缺陷)
	 */
	protected final ThreadLocal<WatchDog> renewTaskThreadLocal = new ThreadLocal<>();

	AbstractLock(String lockKeyFormat, String rawKey, int leaseSeconds) {
		KeyUtils.requireNonBlank(rawKey);
		if (leaseSeconds < 2) {
			throw new IllegalArgumentException("leaseSeconds 至少为 2, 实际: " + leaseSeconds);
		}
		this.key = String.format(lockKeyFormat, rawKey);
		this.notifyChannel = this.key + ":channel";
		this.defaultTimeout = leaseSeconds;
	}

	/**
	 * 开始一次加锁动作: 检查中断标记、拒绝同线程重复加锁(不支持重入),
	 * 生成本轮锁的 value(threadName-UUID) 并写入 ThreadLocal
	 * <p>
	 * 中断检查用 isInterrupted() 而不是 Thread.interrupted(): 后者会清除标记,
	 * 抛出自定义异常后上层若不按该类型判断, 线程池/框架就再也看不到这个中断信号了。
	 * 这里保留标记, 中断语义交回给调用方。
	 *
	 * @return 本轮锁的 value
	 */
	protected final String beginLock() {
		if (Thread.currentThread().isInterrupted()) {
			throw new LockThreadInterruptedException("线程被中断了");
		}
		if (lockedThreadLocal.get()) {
			throw new OperationNotSupportedException(
					getClass().getSimpleName() + " 不支持重入, 当前线程已持有锁 " + key + ", 请检查是否重复加锁");
		}
		String lockValue = Thread.currentThread().getName() + "-" + UUID.randomUUID();
		valueThreadLocal.set(lockValue);
		return lockValue;
	}

	/**
	 * 加锁成功后的公共处理: 置持有状态并启动看门狗续期
	 */
	protected final void holdLock(String lockValue) {
		lockedThreadLocal.set(true);
		WatchDog renewTask = new WatchDog(key, lockValue, defaultTimeout);
		renewTaskThreadLocal.set(renewTask);
		renewTask.schedule();
	}

	/**
	 * 停止当前线程本轮持锁的看门狗
	 */
	protected final void stopWatchDog() {
		WatchDog renewTask = renewTaskThreadLocal.get();
		if (renewTask != null) {
			renewTaskThreadLocal.remove();
			renewTask.stop();
		}
	}

	/**
	 * 清理当前线程的持锁状态(避免线程池线程复用时残留)
	 */
	protected final void clearHoldState() {
		lockedThreadLocal.remove();
		valueThreadLocal.remove();
	}

	/**
	 * 解锁的公共流程: 检查持锁 -> 停看门狗 -> unlock.lua 比对 value 删除 -> 清理状态.
	 * 锁在本线程持有期间已过期或被别的客户端获取时(value 不匹配), 抛异常让调用方感知"锁已丢失"。
	 * <p>
	 * 无论解锁是否成功, 线程持锁状态(locked/value)一定会被清理:
	 * 否则 Redis 抖动导致 unlock 抛异常后, 该线程在这把锁上会被永久判定为"仍持有",
	 * 以后每次 lock() 都撞上重入检测抛异常。
	 */
	protected final void doUnlock() {
		if (!lockedThreadLocal.get()) {
			throw new OperationNotSupportedException("当前线程并未持有锁 " + key);
		}
		//先停看门狗再删锁: 反过来会有一个"锁已删除、续期任务与删除交错"的乱序窗口
		stopWatchDog();

		String threadName = Thread.currentThread().getName();
		String lockValue = valueThreadLocal.get();
		try {
			boolean unlockSuccess = JedisUtils.unlock(key, lockValue);
			if (!unlockSuccess) {
				// value 不匹配: 锁已过期或已易主, 持有期间互斥可能已被破坏, 属于锁丢失
				throw new OperationNotSupportedException(
						"解锁失败: 锁已过期或已易主, 持有期间互斥可能已被破坏, key=" + key + ", value=" + lockValue);
			}
			log.debug(">>>>>> {} 解锁成功, key={}, value={} <<<<<<", threadName, key, lockValue);
		} finally {
			clearHoldState();
		}
	}

	@Override
	public boolean locked() {
		return lockedThreadLocal.get();
	}

	@Override
	public void close() {
		if (lockedThreadLocal.get()) {
			unlock();
		}
	}
}
