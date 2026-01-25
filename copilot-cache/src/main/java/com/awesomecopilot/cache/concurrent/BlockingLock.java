package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;
import com.awesomecopilot.cache.exception.OperationNotSupportedException;
import com.awesomecopilot.cache.listeners.MessageListener;
import com.awesomecopilot.common.lang.concurrent.CopilotThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 阻塞锁
 * <p>
 * Copyright: (C), 2020/3/28 17:57
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class BlockingLock implements Lock, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(BlockingLock.class);

	private static final int NCPUS = Runtime.getRuntime().availableProcessors();

	/**
	 * The number of times to spin before blocking in timed waits.
	 * The value is empirically derived -- it works well across a
	 * variety of processors and OSes. Empirically, the best value
	 * seems not to vary with number of CPUs (beyond 2) so is just
	 * a constant.
	 */
	private static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

	/**
	 * 锁的模板
	 */
	private static final String LOCK_FORMAT = "copilot:blk:%s:lock";

	/**
	 * 解锁channel模板
	 */
	private static final String NOTIFY_CHANNEL_FORMAT = "copilot:blk:%s:lock:channel";

	// 看门狗终止标记（volatile保证多线程可见性）
	private volatile boolean watchDogStopped = false;
	// 单次执行锁（防止任务堆积导致日志重复）
	private final Object renewLock = new Object();

	/**
	 * 解锁后在该channel上通知等待线程可以获取锁了
	 */
	private String notifyChannel;

	/**
	 * 分布式锁的key
	 */
	private String key;

	/**
	 * 锁的值, 解锁要用到; 解锁时提供的value跟redis中的匹配才可以解锁
	 */
	private ThreadLocal<String> valueThreadLocal = new ThreadLocal<>();

	/**
	 * 是否加锁成功
	 */
	private ThreadLocal<Boolean> lockedThreadLocal = ThreadLocal.withInitial(() -> false);

	/**
	 * 负责定时刷新锁过期时间
	 */
	private ThreadLocal<ScheduledExecutorService> watchDogThreadLocal = new ThreadLocal<>();

	/**
	 * 当前线程自旋获取锁失败后, 会先订阅notifyChannel, 然后进入阻塞状态;
	 * 如果拿到锁的线程解锁, 会发布一条消息, 此时本线程被唤醒再次尝试获取锁
	 */
	private ThreadLocal<JedisPubSub> subscribeThreadLocal = new ThreadLocal<>();

	/**
	 * 锁默认30秒过期
	 */
	private int defaultTimeout = 30;

	public BlockingLock(String key) {
		this.key = String.format(LOCK_FORMAT, key);
		this.notifyChannel = String.format(NOTIFY_CHANNEL_FORMAT, key);
	}

	/**
	 * 加锁, 锁有效期默认30秒
	 * 如果本线程被杀死, 30秒后自动释放锁
	 * 如果本线程一直在执行并且没有释放锁, 会有一条watchDog定时刷新锁的过期时间, 防止被其他线程获取锁
	 */
	@Override
	public void lock() {
		if (Thread.interrupted()) {
			throw new LockThreadInterruptedException("线程被中断了");
		}
		//自旋计数
		int i = 0;
		String threadName = Thread.currentThread().getName();

		/*
		 * 将lockValue放到threadLocal中是为了支持BlockingLock被作为多个线程共享使用的场景
		 * 比如被Springboot注入到多个bean中使用, 那么A加的锁是会被B给解锁的
		 *
		 * 而把lockValue放到ThreadLocal中, 只有加锁的那个线程才能拿到lockValue, 用来解锁
		 */
		String lockValue = Thread.currentThread().getName() + UUID.randomUUID().toString();
		this.valueThreadLocal.set(lockValue);
		try {
			/*
			 * 尝试第一次加锁, 加锁成功则启动watchDog并返回
			 */
			boolean lockedFlag = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
			if (lockedFlag && verifyLock(lockValue)) {
				log.info(">>>>>> {} 获取锁成功, key={}, value={} <<<<<<", threadName, key, lockValue);
				this.lockedThreadLocal.set(true); //为了支持多线程环境使用
				startWatchDog();
				return;
			}

			log.info(">>>>>> {} 第一次没能成功获取锁, 开始自旋获取锁 <<<<<<", threadName);
			/**
			 * 尝试maxTimedSpins次自旋获取锁, 加锁成功则启动watchDog并返回
			 */
			while (i++ < maxTimedSpins) {
				lockedFlag = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
				if (lockedFlag && verifyLock(lockValue)) {
					log.info(">>>>>> {} 自旋{}次获取锁成功 <<<<<<", threadName, i);
					this.lockedThreadLocal.set(true); //为了支持多线程环境使用
					startWatchDog();
					return;
				}
				log.info(">>>>>> {} 自旋{}次获取锁失败 <<<<<<", threadName, i);
			}

			log.info(">>>>>> {} 自旋失败, 进入阻塞等待 <<<<<<", threadName);
			/**
			 * 循环获取锁, 获取加锁成功则启动watchDog并返回
			 * 加锁失败挂起线程
			 */
			for (; ; ) {
				startListener();
				/**
				 * 阻塞30秒后自动醒来
				 * 期间如果Listener收到通知, 则提前唤醒本线程
				 * 如果获取锁的线程一直没有解锁, 或者那个线程被杀死了, 也就是锁一直没有被释放, 本线程过30秒也会自动醒来, 防止死锁
				 */
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(defaultTimeout));
				if (Thread.interrupted()) {
					throw new LockThreadInterruptedException("线程被中断了");
				}
				lockedFlag = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
				if (lockedFlag && verifyLock(lockValue)) {
					log.info(">>>>>> {} 醒来后终获成功, key={}, value={} <<<<<<", threadName, key, lockValue);
					this.lockedThreadLocal.set(true); //为了支持多线程环境使用
					stopListener();
					startWatchDog();
					return;
				}
				log.info("{} 醒来后仍然没有获取到锁, 准备再次进入阻塞状态, key={}", threadName, key);
			}
		} finally {
			// 如果失败，清理ThreadLocal，避免泄漏
			if (!lockedThreadLocal.get()) {
				valueThreadLocal.remove();
				lockedThreadLocal.remove();
				stopListener(); // 如果订阅了，清理
			}
		}
	}

	private boolean verifyLock(String lockValue) {
		String currentValue = JedisUtils.get(key);
		if (!lockValue.equals(currentValue)) {
			log.warn("锁验证失败: 预期value={}, 实际={}, key={}", lockValue, currentValue, key);
			JedisUtils.unlock(key, lockValue); // 尝试清理（幂等）
			return false;
		}
		return true;
	}

	@Override
	public void unlock() {
		if (!this.lockedThreadLocal.get()) {
			throw new OperationNotSupportedException("你还没获取到锁哦");
		}

		String threadName = Thread.currentThread().getName();
		//解锁
		boolean unlockSuccess = JedisUtils.unlock(key, valueThreadLocal.get());
		if (!unlockSuccess) {
			throw new OperationNotSupportedException("解锁失败了哟");
		}
		log.info(">>>>>> {} 解锁成功, key={}, value={} <<<<<<", threadName, key, valueThreadLocal.get());
		lockedThreadLocal.set(false);
		lockedThreadLocal.remove();
		valueThreadLocal.remove();

		/**
		 * 通知其他线程可以重新获取锁了, 把当前线程名作为消息发出去, 方便记log
		 */
		JedisUtils.publish(notifyChannel, Thread.currentThread().getName());
		log.info(">>>>>> {} 发布消息, 现在其他线程可以重新获取锁, key={} <<<<<<", threadName, key);
		/*
		 * 关掉看门狗
		 */
		stopWatchDog();
		log.info(">>>>>> {} shutdown Watch dog, key={} <<<<<<", threadName, key);
	}

	@Override
	public boolean locked() {
		return lockedThreadLocal.get();
	}

	/**
	 * 订阅通知channel, 只会订阅一次
	 * <p>
	 * 通过判断subscribeThreadLocal里面是否已经有JedisPubSub来判断是否已经订阅了
	 * <p>
	 * 已经订阅就不再订阅量
	 */
	public void startListener() {
		if (this.subscribeThreadLocal.get() == null) {
			/*
			 * 因为当前线程自旋获取锁失败, 所以在startListener之后当前线程会被阻塞,
			 * 这里把当前线程传给NotifyListener, 这个在监听到事件后, 才可以知道要唤醒的线程是哪个
			 * 订阅本身是交给JedisPoolOperations.THREAD_POOL线程池去执行的
			 */
			this.subscribeThreadLocal.set(JedisUtils.subscribe(new NotifyListener(Thread.currentThread()),
					notifyChannel));
		}
	}

	public void stopListener() {
		if (this.subscribeThreadLocal.get() != null) {
			JedisUtils.unsubscribe(subscribeThreadLocal.get(), notifyChannel);
			this.subscribeThreadLocal.set(null);
			this.subscribeThreadLocal.remove();
		}
	}

	private class NotifyListener implements MessageListener {

		private Thread thread;

		public NotifyListener(Thread thread) {
			this.thread = thread;
		}

		@Override
		public void onMessage(String channel, String message) {
			log.info("收到 {} 发来的消息, 准备唤醒线程: {}, channel: {}", message, thread.getName(), channel);
			LockSupport.unpark(thread);
		}
	}

	/**
	 * 定时刷新锁的过期时间, 看门狗自动检测线程是否还在处理业务，忘记解锁也会自动停止续期
	 * 注意通过Idea debug的时候, 断点Suspend要设为Thread级别, 不然watchDog线程不会运行, 导致锁一会就失效了
	 */
	private void startWatchDog() {
		if (watchDogThreadLocal.get() == null) {
			ScheduledThreadPoolExecutor watchDog = new ScheduledThreadPoolExecutor(1, new CopilotThreadFactory("分布式锁看门狗"));
			watchDogThreadLocal.set(watchDog);

			// 保存持有锁的线程 + 加锁时的栈轨迹（仅记录和当前锁相关的特征）
			Thread lockHolderThread = Thread.currentThread();
			// 最大续期次数兜底（双重保险）
			AtomicInteger counter = new AtomicInteger(1);
			AtomicInteger renewCount = new AtomicInteger(0);
			AtomicInteger successRenewCount = new AtomicInteger(1);
			final int MAX_RENEW_COUNT = 10;


			// 定义续期逻辑（Runnable）
			Runnable renewTask = new Runnable() {
				@Override
				public void run() {
					log.info("看门狗定时任务第[{}]次运行...", counter.getAndIncrement());
					if (watchDogStopped || watchDog.isShutdown()) {
						return;
					}

					synchronized (renewLock) {
						try {
							if (!lockHolderThread.isAlive()) {
								log.warn("持有线程已死亡，停止续期 {}", key);
								stopWatchDog();
								return;
							}

							if (!isThreadProcessingLockRelatedBusiness(lockHolderThread)) {
								log.info("线程已空闲，停止看门狗续期 {}", key);
								stopWatchDog();
								return;
							}

							if (renewCount.incrementAndGet() > MAX_RENEW_COUNT) {
								log.warn("续期超过最大次数，停止 {}", key);
								stopWatchDog();
								return;
							}

							boolean success = JedisUtils.expire(key, defaultTimeout, TimeUnit.SECONDS);
							if (success) {
								log.info("看门狗第[{}]次续期成功, 将锁 {} 过期时间刷新为 {} 秒", successRenewCount.getAndIncrement() ,key, defaultTimeout);
								// 续期成功 → 重新调度下一次
								watchDog.schedule(this, defaultTimeout / 3, TimeUnit.SECONDS);
							} else {
								log.info("key 已不存在或过期，停止看门狗 {}", key);
								stopWatchDog();
							}
						} catch (Exception e) {
							log.error("看门狗异常，key={}", key, e);
							stopWatchDog();
						}
					}
				}
			};

			// 第一次调度
			watchDog.schedule(renewTask, defaultTimeout / 3, TimeUnit.SECONDS);

			watchDog.setKeepAliveTime(10, TimeUnit.SECONDS);
			watchDog.allowCoreThreadTimeOut(true);
		}
	}

	// ========== 修正后的stopWatchDog方法（完整可编译） ==========
	private void stopWatchDog() {
		// 优先级1：立即标记终止（先置位，再处理线程池，杜绝新任务执行）
		this.watchDogStopped = true;

		// 优先级2：获取线程池（先拿引用，再清空ThreadLocal，防止时序漏洞）
		ScheduledExecutorService watchDog = watchDogThreadLocal.get();
		if (watchDog == null || watchDog.isShutdown()) {
			watchDogThreadLocal.remove(); // 仅当线程池为空时清空
			return;
		}

		try {
			// 1. 强制停止线程池（核心：先停止，再清空ThreadLocal）
			List<Runnable> remaining = watchDog.shutdownNow();
			log.info("锁[{}]看门狗停止，清理待执行任务数：{}", key, remaining.size());

			// 2. 强制中断当前执行的任务（新增：主动中断线程池工作线程）
			interruptExecutorThread(watchDog);

			// 3. 等待线程池终止（延长等待时间到2秒，确保终止）
			if (!watchDog.awaitTermination(2, TimeUnit.SECONDS)) {
				log.warn("锁[{}]看门狗线程池终止超时，已强制中断所有线程", key);
			}

			// 4. 最后清空ThreadLocal（时序优化）
			watchDogThreadLocal.remove();
			log.info("锁[{}]看门狗已彻底终止", key);
		} catch (InterruptedException e) {
			log.info("停止看门狗中断", e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 核心新增：强制中断线程池的工作线程（直接终止正在执行的任务）
	 */
	private void interruptExecutorThread(ScheduledExecutorService executor) {
		if (executor instanceof ScheduledThreadPoolExecutor) {
			ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) executor;
			// 反射获取线程池的工作线程（JDK8/JDK11通用）
			try {
				Field workerField = ThreadPoolExecutor.class.getDeclaredField("workers");
				workerField.setAccessible(true);
				Set<?> workers = (Set<?>) workerField.get(stpe);
				for (Object worker : workers) {
					Field threadField = worker.getClass().getDeclaredField("thread");
					threadField.setAccessible(true);
					Thread thread = (Thread) threadField.get(worker);
					if (thread.isAlive()) {
						thread.interrupt(); // 强制中断正在执行的任务线程
						log.info("锁[{}]强制中断看门狗线程：{}", key, thread.getName());
					}
				}
			} catch (Exception e) {
				log.warn("反射中断线程失败（非关键）", e);
			}
		}
	}

	@Override
	public void close() {
		if (lockedThreadLocal.get()) {
			unlock();
		}
	}

	/**
	 * 通用化检测：记录加锁时栈中是否包含当前锁类的调用（无业务依赖）
	 */
	private boolean checkLockStackInCurrentThread() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String lockClassName = this.getClass().getName(); // 当前锁类的全限定名（通用）
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().equals(lockClassName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 核心通用检测：线程是否还在处理加锁后的业务（移除hasLockStack参数）
	 */
	private boolean isThreadProcessingLockRelatedBusiness(Thread thread) {
		try {
			// 新增：结合线程状态检查（NIO worker空闲时通常WAITING on park）
			Thread.State state = thread.getState();
			if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
				log.info("锁[{}]持有线程状态为{}，判断为空闲", key, state);
				return false;
			}

			StackTraceElement[] stackTrace = thread.getStackTrace();
			if (stackTrace == null || stackTrace.length <= 3) {
				return false;
			}

			// 如果栈深度小，视为空闲（Tomcat空闲线程通常<10）
			if (stackTrace.length < 10) {
				return false;
			}

			// 扩展框架前缀，覆盖更多Tomcat/JDK底层（基于典型栈迹）
			String[] frameworkPrefixes = {
					"org.apache.tomcat.",    // Tomcat核心
					"org.apache.coyote.",    // Coyote HTTP处理器
					"org.apache.catalina.",  // Catalina容器
					"java.lang.",            // JDK线程/反射
					"java.util.concurrent.", // 线程池/锁
					"sun.nio.ch.",           // NIO通道
					"java.net.",             // Socket/BIO
					"java.nio.",             // NIO
					"sun.misc.",             // Unsafe park
					"jdk.internal."          // JDK内部（Java 9+）
			};

			int frameworkStackCount = 0;
			for (StackTraceElement element : stackTrace) {
				String className = element.getClassName();
				for (String prefix : frameworkPrefixes) {
					if (className.startsWith(prefix)) {
						frameworkStackCount++;
						break;
					}
				}
			}

			// 如果框架栈 >= 90%，视为空闲（非框架栈 <=10%）
			boolean isProcessing = frameworkStackCount < stackTrace.length * 0.9;

			// 新增：日志打印简要栈迹，便于debug（可选，生产可移除）
			if (!isProcessing) {
				StringBuilder sb = new StringBuilder("空闲栈迹样本: ");
				for (int i = 0; i < Math.min(5, stackTrace.length); i++) { // 只打印前5
					sb.append(stackTrace[i]).append("; ");
				}
				log.info("锁[{}]判断为空闲，栈深度={}, 框架栈比例={}/{}，样本: {}",
						key, stackTrace.length, frameworkStackCount, stackTrace.length, sb);
			}

			return isProcessing;
		} catch (Exception e) {
			log.warn("检测线程业务状态失败，默认认为还在处理业务", e);
			return true;
		}
	}
}
