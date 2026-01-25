package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;
import com.awesomecopilot.cache.exception.OperationNotSupportedException;
import com.awesomecopilot.cache.utils.KeyUtils;
import com.awesomecopilot.common.lang.concurrent.CopilotThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 非阻塞锁（尝试锁 / 快速获取锁）
 * <p>
 * 特点：
 * - 非阻塞：lock() 会有限自旋尝试，不进入 park / 订阅 / 等待
 * - 适合“能拿就拿，拿不到就走其他逻辑”的场景
 * - 支持 AutoCloseable，推荐 try-with-resources 使用
 * - 看门狗会自动检测线程是否空闲/死亡，忘记 unlock 也不会无限续期
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
public class NonBlockingLock implements Lock, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(NonBlockingLock.class);

	private static final int NCPUS = Runtime.getRuntime().availableProcessors();
	private static final int MAX_TIMED_SPINS = (NCPUS < 2) ? 0 : 32;

	private static final String LOCK_FORMAT = "copilot:nblk:%s:lock";

	/**
	 * 解锁channel模板
	 */
	private static final String NOTIFY_CHANNEL_FORMAT = "copilot:blk:%s:lock:channel";

	private final String key;
	private final String notifyChannel; // 虽然本锁不订阅，但保留字段兼容性

	// 使用 ThreadLocal 支持同一个实例被多个线程使用
	private final ThreadLocal<String> valueThreadLocal = new ThreadLocal<>();
	private final ThreadLocal<Boolean> lockedThreadLocal = ThreadLocal.withInitial(() -> false);

	private final ThreadLocal<ScheduledExecutorService> watchDogThreadLocal = new ThreadLocal<>();
	private volatile boolean watchDogStopped = false;
	private final Object renewLock = new Object();

	private final int defaultTimeout = 30; // 秒

	public NonBlockingLock(String key) {
		KeyUtils.requireNonBlank(key);
		this.key = String.format(LOCK_FORMAT, key);
		this.notifyChannel = String.format(NOTIFY_CHANNEL_FORMAT, key);
	}

	/**
	 * 尝试获取锁（非阻塞，立即返回）
	 *
	 * @return 是否成功获取锁
	 */
	public boolean tryLock() {
		if (Thread.interrupted()) {
			throw new LockThreadInterruptedException("线程被中断");
		}

		String threadName = Thread.currentThread().getName();
		String lockValue = threadName + "-" + UUID.randomUUID();

		valueThreadLocal.set(lockValue);

		boolean acquired = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
		if (acquired && verifyLock(lockValue)) {
			log.debug(">>>>>> {} 尝试获取锁成功, key={}, value={} <<<<<<", threadName, key, lockValue);
			lockedThreadLocal.set(true);
			startWatchDog();
			return true;
		}

		// 清理（失败时）
		valueThreadLocal.remove();
		return false;
	}

	/**
	 * 阻塞式获取锁（有限自旋，不进入 park）
	 * 非阻塞锁的 lock() 只是多尝试几次，仍然是非阻塞语义
	 */
	@Override
	public void lock() {
		int spins = 0;
		while (spins++ < MAX_TIMED_SPINS) {
			if (tryLock()) {
				return;
			}
			// 可选：Thread.yield(); 但通常不加，避免过度让出 CPU
		}
		// 自旋失败，不再阻塞，直接返回（符合 NonBlockingLock 语义）
		log.debug("自旋 {} 次后仍未获取到锁，放弃阻塞等待，key={}", spins - 1, key);
	}

	@Override
	public void unlock() {
		if (!lockedThreadLocal.get()) {
			throw new OperationNotSupportedException("当前线程并未持有锁");
		}

		String threadName = Thread.currentThread().getName();
		String value = valueThreadLocal.get();

		boolean success = JedisUtils.unlock(key, value);
		if (!success) {
			log.warn("解锁失败，可能已被其他线程持有或已过期，key={}, value={}", key, value);
		} else {
			log.debug(">>>>>> {} 解锁成功, key={}, value={} <<<<<<", threadName, key, value);
		}

		lockedThreadLocal.set(false);
		lockedThreadLocal.remove();
		valueThreadLocal.remove();

		stopWatchDog();
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

	// ────────────────────────────────────────────────
	//  看门狗相关（与 BlockingLock 最新版本保持一致风格）
	// ────────────────────────────────────────────────

	private void startWatchDog() {
		if (watchDogThreadLocal.get() != null) {
			return;
		}

		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,
				new CopilotThreadFactory("nblk-watchdog-" + key.replaceAll("[^a-zA-Z0-9]", "-")));

		watchDogThreadLocal.set(scheduler);

		final Thread holderThread = Thread.currentThread();
		final AtomicInteger renewCount = new AtomicInteger(0);
		final int MAX_RENEW_COUNT = 10;

		Runnable renewTask = new Runnable() {
			@Override
			public void run() {
				if (watchDogStopped || scheduler.isShutdown()) {
					return;
				}

				synchronized (renewLock) {
					try {
						if (!holderThread.isAlive()) {
							log.warn("持有线程已死亡，停止续期 {}", key);
							stopWatchDog();
							return;
						}

						if (!isThreadProcessingBusiness(holderThread)) {
							log.info("持有线程已回到空闲/框架状态，停止看门狗续期 {}", key);
							stopWatchDog();
							return;
						}

						if (renewCount.incrementAndGet() > MAX_RENEW_COUNT) {
							log.warn("续期次数超过上限({})，强制停止看门狗 {}", MAX_RENEW_COUNT, key);
							stopWatchDog();
							return;
						}

						boolean success = JedisUtils.expire(key, defaultTimeout, TimeUnit.SECONDS);
						if (success) {
							log.debug("看门狗续期成功，key={}，剩余续期次数上限：{}", key, MAX_RENEW_COUNT - renewCount.get());
							// 成功续期 → 重新调度下一次
							scheduler.schedule(this, defaultTimeout / 3, TimeUnit.SECONDS);
						} else {
							log.info("key 已过期或不存在，停止看门狗 {}", key);
							stopWatchDog();
						}
					} catch (Exception e) {
						log.error("看门狗续期异常，key={}", key, e);
						stopWatchDog();
					}
				}
			}
		};

		// 第一次调度（延迟 defaultTimeout/3 秒开始第一次检查+续期）
		scheduler.schedule(renewTask, defaultTimeout / 3, TimeUnit.SECONDS);

		scheduler.setKeepAliveTime(10, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);
	}

	private void stopWatchDog() {
		watchDogStopped = true;

		ScheduledExecutorService executor = watchDogThreadLocal.get();
		if (executor == null || executor.isShutdown()) {
			watchDogThreadLocal.remove();
			return;
		}

		try {
			List<Runnable> pending = executor.shutdownNow();
			log.debug("看门狗停止，丢弃待执行任务数：{}，key={}", pending.size(), key);

			interruptExecutorThreads(executor);

			if (!executor.awaitTermination(1500, TimeUnit.MILLISECONDS)) {
				log.warn("看门狗线程池未能在1.5秒内完全终止，key={}", key);
			}

			log.info("看门狗已彻底停止，key={}", key);
		} catch (InterruptedException e) {
			log.warn("等待看门狗终止被中断", e);
			Thread.currentThread().interrupt();
		} finally {
			watchDogThreadLocal.remove();
		}
	}

	/**
	 * 强制中断线程池的工作线程
	 */
	private void interruptExecutorThreads(ScheduledExecutorService executor) {
		if (!(executor instanceof ScheduledThreadPoolExecutor)) {
			return;
		}

		ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) executor;

		try {
			// 获取 workers 字段（类型是 HashSet<Worker>）
			Field workersField = ThreadPoolExecutor.class.getDeclaredField("workers");
			workersField.setAccessible(true);

			// 注意：这里用 Set<?> 或 Set<Object>，**不要**写 Set<ThreadPoolExecutor.Worker>
			@SuppressWarnings("unchecked")
			Set<?> workers = (Set<?>) workersField.get(stpe);

			for (Object workerObj : workers) {
				// workerObj 实际是 Worker 实例
				// 获取 Worker 的 'thread' 字段（private final Thread thread）
				Field threadField = workerObj.getClass().getDeclaredField("thread");
				threadField.setAccessible(true);

				Thread thread = (Thread) threadField.get(workerObj);
				if (thread != null && thread.isAlive()) {
					thread.interrupt();
					log.debug("强制中断看门狗线程：{} (key={})", thread.getName(), key);
				}
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.warn("无法通过反射中断看门狗线程（非致命），key={}", key, e);
		}
	}

	/**
	 * 判断线程是否仍在处理业务（非空闲状态）
	 * 逻辑与 BlockingLock 保持一致
	 */
	private boolean isThreadProcessingBusiness(Thread thread) {
		try {
			Thread.State state = thread.getState();
			if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
				return false;
			}

			StackTraceElement[] stack = thread.getStackTrace();
			if (stack == null || stack.length < 10) {
				return false;
			}

			String[] frameworkPrefixes = {
					"org.apache.tomcat.", "org.apache.coyote.", "org.apache.catalina.",
					"java.lang.", "java.util.concurrent.", "sun.nio.ch.", "java.net.",
					"java.nio.", "sun.misc.", "jdk.internal."
			};

			int frameworkCount = 0;
			for (StackTraceElement e : stack) {
				String cls = e.getClassName();
				for (String prefix : frameworkPrefixes) {
					if (cls.startsWith(prefix)) {
						frameworkCount++;
						break;
					}
				}
			}

			// 框架栈占比 >= 90% → 视为空闲
			return frameworkCount < stack.length * 0.9;
		} catch (Exception e) {
			log.warn("无法获取线程栈信息，默认认为仍在处理业务", e);
			return true;
		}
	}

	private boolean verifyLock(String expectedValue) {
		String current = JedisUtils.get(key);
		if (!expectedValue.equals(current)) {
			log.warn("锁验证失败，预期={}，实际={}，key={}", expectedValue, current, key);
			JedisUtils.unlock(key, expectedValue); // 尝试清理
			return false;
		}
		return true;
	}
}