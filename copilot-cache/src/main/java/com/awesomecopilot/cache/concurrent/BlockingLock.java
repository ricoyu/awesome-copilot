package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.LockThreadInterruptedException;
import com.awesomecopilot.cache.exception.OperationNotSupportedException;
import com.awesomecopilot.cache.listeners.MessageListener;
import com.awesomecopilot.common.lang.concurrent.CopilotThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
				log.debug(">>>>>> {} 获取锁成功, key={}, value={} <<<<<<", threadName, key, lockValue);
				this.lockedThreadLocal.set(true); //为了支持多线程环境使用
				startWatchDog();
				return;
			}

			log.debug(">>>>>> {} 第一次没能成功获取锁, 开始自旋获取锁 <<<<<<", threadName);
			/**
			 * 尝试maxTimedSpins次自旋获取锁, 加锁成功则启动watchDog并返回
			 */
			while (i++ < maxTimedSpins) {
				lockedFlag = JedisUtils.setnx(key, lockValue, defaultTimeout, TimeUnit.SECONDS);
				if (lockedFlag && verifyLock(lockValue)) {
					log.debug(">>>>>> {} 自旋{}次获取锁成功 <<<<<<", threadName, i);
					this.lockedThreadLocal.set(true); //为了支持多线程环境使用
					startWatchDog();
					return;
				}
				log.debug(">>>>>> {} 自旋{}次获取锁失败 <<<<<<", threadName, i);
			}

			log.debug(">>>>>> {} 自旋失败, 进入阻塞等待 <<<<<<", threadName);
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
					log.debug(">>>>>> {} 醒来后终获成功, key={}, value={} <<<<<<", threadName, key, lockValue);
					this.lockedThreadLocal.set(true); //为了支持多线程环境使用
					stopListener();
					startWatchDog();
					return;
				}
				log.debug("{} 醒来后仍然没有获取到锁, 准备再次进入阻塞状态, key={}", threadName, key);
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
		log.debug(">>>>>> {} 解锁成功, key={}, value={} <<<<<<", threadName, key, valueThreadLocal.get());
		lockedThreadLocal.set(false);
		lockedThreadLocal.remove();
		valueThreadLocal.remove();

		/**
		 * 通知其他线程可以重新获取锁了, 把当前线程名作为消息发出去, 方便记log
		 */
		JedisUtils.publish(notifyChannel, Thread.currentThread().getName());
		log.debug(">>>>>> {} 发布消息, 现在其他线程可以重新获取锁, key={} <<<<<<", threadName, key);
		/*
		 * 关掉看门狗
		 */
		stopWatchDog();
		log.debug(">>>>>> {} shutdown Watch dog, key={} <<<<<<", threadName, key);
	}

	@Override
	public boolean locked() {
		return lockedThreadLocal.get();
	}

	@Override
	public void unlockAnyway() {
		JedisUtils.del(key); //无视value
		log.warn("强制解锁 key={}", key);
		// 清理状态
		lockedThreadLocal.set(false);
		lockedThreadLocal.remove();
		valueThreadLocal.remove();
		stopWatchDog();
	}

	/**
	 * 订阅通知channel, 只会订阅一次
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
			log.debug("收到 {} 发来的消息, 准备唤醒 {}, channel={}", message, thread.getName(), channel);
			LockSupport.unpark(thread);
		}
	}

	/**
	 * 定时刷新锁的过期时间
	 * 注意通过Idea debug的时候, 断点Suspend要设为Thread级别, 不然watchDog线程不会运行, 导致锁一会就失效了
	 */
	private void startWatchDog() {
		if (watchDogThreadLocal.get() == null) {
			ScheduledThreadPoolExecutor watchDog =
					new ScheduledThreadPoolExecutor(1, new CopilotThreadFactory("copilot Cache key renewval watch " +
							"dog"));
			watchDogThreadLocal.set(watchDog);
		}
		watchDogThreadLocal.get().scheduleAtFixedRate(() -> {
			//如果key已经过期了, 那么watchDog就不用再去刷新key过期时间了
			boolean isSuccess = false;
			for (int retry = 0; retry < 2; retry++) {
				isSuccess = JedisUtils.expire(key, defaultTimeout, TimeUnit.SECONDS);
				if (isSuccess) {
					break;
				}
				try {
					Thread.sleep(100); // 短暂重试间隔
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (!isSuccess) {
				log.debug("Key {} already expired after retries, Watch dog stop refresh", key);
				stopWatchDog();
			} else {
				log.debug("Watch dog refresh lock {} timeout to default {} seconds", key, defaultTimeout);
			}
		}, defaultTimeout / 3, defaultTimeout / 3, TimeUnit.SECONDS); // 初始延迟设为间隔，防刚设就跑
	}

	private void stopWatchDog() {
		if (watchDogThreadLocal.get() != null && !watchDogThreadLocal.get().isShutdown()) {
			watchDogThreadLocal.get().shutdown();
			watchDogThreadLocal.set(null);
			watchDogThreadLocal.remove();  // 清理
		}
	}

	@Override
	public void close() {
		if (lockedThreadLocal.get()) {
			unlock();
		}
	}
}
