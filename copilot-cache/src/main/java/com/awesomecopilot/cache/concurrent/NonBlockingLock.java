package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;

import java.util.concurrent.TimeUnit;

/**
 * 非阻塞锁（尝试锁 / 快速获取锁）
 * <p>
 * 特点：
 * - 非阻塞：lock() 会有限自旋尝试(每次间隔 1ms)，不进入 park / 订阅 / 等待
 * - 适合"能拿就拿，拿不到就走其他逻辑"的场景
 * - 支持 AutoCloseable，推荐 try-with-resources 使用
 * - 看门狗(WatchDog)在持锁线程存活期间自动续期；忘记 unlock 时续期有硬上限，
 *   达到上限后锁在一个租期后自然过期
 * - ThreadLocal 存储 value 和 locked 状态，支持对象被多个线程复用
 * <p>
 * 使用建议：
 * try (NonBlockingLock lock = new NonBlockingLock("myKey")) {
 *     if (lock.tryLock()) {
 *         // 业务逻辑
 *     } else {
 *         // 获取锁失败，走降级/排队/抛异常等
 *     }
 * }  // 自动 unlock
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class NonBlockingLock extends AbstractLock {

	private static final String LOCK_FORMAT = "copilot:nblk:%s:lock";

	public NonBlockingLock(String key) {
		this(key, 30);
	}

	/**
	 * 指定锁租期(秒)的构造器, 主要供测试与短任务使用
	 *
	 * @param key          锁业务名
	 * @param leaseSeconds 锁租期, 至少 2 秒(看门狗按 租期/3 的间隔调度续期)
	 */
	public NonBlockingLock(String key, int leaseSeconds) {
		super(LOCK_FORMAT, key, leaseSeconds);
	}

	/**
	 * 尝试获取锁（非阻塞，立即返回）
	 *
	 * @return 是否成功获取锁
	 */
	public boolean tryLock() {
		String lockValue = beginLock();

		// setnx.lua 原子写入 key+过期时间, 返回 1 即代表锁归属本请求, 无需再 GET 验证
		boolean acquired = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
		if (acquired) {
			log.debug(">>>>>> {} 尝试获取锁成功, key={}, value={} <<<<<<",
					Thread.currentThread().getName(), key, lockValue);
			holdLock(lockValue);
			return true;
		}

		// 清理（失败时）
		clearHoldState();
		return false;
	}

	/**
	 * 阻塞式获取锁（有限自旋，不进入 park）
	 * 非阻塞锁的 lock() 只是多尝试几次，仍然是非阻塞语义
	 * <p>
	 * 加锁失败不会抛异常, 后续需要通过locked()方法来判断是否加锁成功
	 */
	@Override
	public void lock() {
		int spins = 0;
		while (spins++ < MAX_TIMED_SPINS) {
			if (tryLock()) {
				log.debug("自旋{}次后获取锁成功, key={}", spins, key);
				return;
			}
			try {
				// 1ms 间隔: 给持有者留出解锁时间, 也让 Redis 网络抖动有机会恢复
				TimeUnit.MILLISECONDS.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new LockThreadInterruptedException("线程在自旋等待锁时被中断");
			}
		}
		// 自旋失败，不再阻塞，直接返回（符合 NonBlockingLock 语义）
		log.debug("自旋 {} 次后仍未获取到锁，放弃阻塞等待，key={}", spins - 1, key);
	}

	@Override
	public void unlock() {
		doUnlock();
	}

	/**
	 * 如果加锁成功, 则执行task, task会在try/catch块中执行,
	 * 无论task执行成功与否, 最后锁都会释放
	 *
	 * @param task 要执行的代码
	 */
	@Override
	public void ifLocked(Runnable task) {
		if (locked()) {
			log.debug("加锁成功, 执行task");
			try {
				task.run();
			} catch (Exception e) {
				log.error("task执行异常", e);
				throw e;
			} finally {
				unlock();
			}
		} else {
			log.warn("加锁失败, task未执行");
		}
	}
}
