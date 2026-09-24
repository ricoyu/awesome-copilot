package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式锁的看门狗续期任务: 一个实例对应"某线程持有一把锁"的这一轮续期.
 * <p>
 * 所有锁的续期共用一个守护调度线程池(续期是每把锁每 租期/3 秒一次的轻量 Redis 命令,
 * 不值得每次加锁新建线程池). 续期只通过 renew.lua 在"锁的 value 仍是本轮写入的 requestId"
 * 时才刷新过期时间, 不会给其他客户端的锁续期.
 * <p>
 * 停止续期只有这几种情况: 持锁线程调用 stop()(典型是 unlock)、持锁线程死亡、
 * 锁已过期或已易主(续期返回 -1/0)、续期请求连续失败达到阈值(网络持续不通).
 * 单次续期异常(网络抖动/超时)不停止, 下一周期继续尝试——这与 Redisson 看门狗行为一致:
 * 只有确认"锁没了"或"一直联系不上 Redis"才放弃; 一次抖动不会停止续期, 避免锁在一个租期后过期、
 * 第二个客户端加锁成功导致两方同时进临界区(评审报告 P0-2).
 * 另有续期次数硬上限: 达到上限打 error 日志并停止,
 * 锁将在一个租期后自然过期, 防止业务线程泄漏后锁被永久续期.
 * <p>
 * Copyright: (C), 2026/9/22
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
final class WatchDog implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(WatchDog.class);

	private static final int NCPUS = Runtime.getRuntime().availableProcessors();

	/**
	 * 全部看门狗共用一个调度线程池, 线程为守护线程, 不阻止 JVM 退出
	 */
	private static final ScheduledExecutorService POOL =
			Executors.newScheduledThreadPool(Math.max(2, NCPUS / 2), new ThreadFactory() {
				private final AtomicInteger seq = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "lock-watchdog-" + seq.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * 续期次数硬上限(JVM 参数 -Dcopilot.cache.lock.watchdog.max-renew-count=N, 默认 360 次):
	 * 按 租期/3 的间隔, 默认值相当于租期 30 秒时最多续 1 小时
	 */
	private static final int MAX_RENEW_COUNT =
			Integer.getInteger("copilot.cache.lock.watchdog.max-renew-count", 360);

	/**
	 * 续期连续失败多少次才放弃(JVM 参数 -Dcopilot.cache.lock.watchdog.max-consecutive-errors=N, 默认 3).
	 * 续期间隔是 租期/3, 连续 3 次失败约等于容忍 1 个租期时长的 Redis 不可用(30 秒租期即约 30 秒),
	 * 期间锁靠上一轮续期写入的 TTL 维持; 超过阈值则认为 Redis 长时间不可达, 停止续期, 让锁按租期自然过期.
	 */
	private static final int MAX_CONSECUTIVE_ERRORS = Math.max(1,
			Integer.getInteger("copilot.cache.lock.watchdog.max-consecutive-errors", 3));

	/**
	 * 续期动作的注入点: 测试可替换成抛异常/返回指定值的桩, 生产走 JedisUtils.renewLock
	 */
	@FunctionalInterface
	interface Renewer {
		int renew(String key, String lockValue, int leaseSeconds) throws Exception;
	}

	private final String key;
	private final String lockValue;
	private final int leaseSeconds;
	private final Thread holderThread;
	private final Renewer renewer;
	private final AtomicInteger counter = new AtomicInteger();

	/**
	 * 连续续期异常计数: 续期成功一次即清零; 达到 MAX_CONSECUTIVE_ERRORS 才停止本轮看护
	 */
	private final AtomicInteger consecutiveErrors = new AtomicInteger();

	/**
	 * stop() 可能由持锁线程或看门狗任务线程自己调用, volatile 保证任务线程及时可见
	 */
	private volatile boolean stopped;
	private volatile ScheduledFuture<?> future;

	/**
	 * 必须在持锁线程上构造: 构造时捕获当前线程作为被看护线程
	 */
	WatchDog(String key, String lockValue, int leaseSeconds) {
		this(key, lockValue, leaseSeconds,
				(k, v, lease) -> JedisUtils.renewLock(k, v, lease, TimeUnit.SECONDS));
	}

	/**
	 * 测试用的续期动作注入构造
	 */
	WatchDog(String key, String lockValue, int leaseSeconds, Renewer renewer) {
		this.key = key;
		this.lockValue = lockValue;
		this.leaseSeconds = leaseSeconds;
		this.holderThread = Thread.currentThread();
		this.renewer = renewer;
	}

	/**
	 * 按 租期/3 秒的间隔安排周期续期
	 */
	void schedule() {
		long intervalMillis = TimeUnit.SECONDS.toMillis(leaseSeconds) / 3;
		future = POOL.scheduleWithFixedDelay(this, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
		// 赋值与 stop() 竞态的补漏: 若安排期间已被停止(future 当时还是 null, 没能取消),
		// 这里补一次取消, 避免一个每轮进来直接 return 的空转任务永久留在调度池里
		if (stopped) {
			future.cancel(false);
		}
	}

	@Override
	public void run() {
		//本轮续期已停止(unlock 或上一轮自我停止): 不再做任何续期动作
		if (stopped) {
			return;
		}
		try {
			if (!holderThread.isAlive()) {
				log.warn("持锁线程已死亡, 停止续期 {}", key);
				stop();
				return;
			}
			if (counter.incrementAndGet() > MAX_RENEW_COUNT) {
				log.error("锁 {} 续期达到上限 {} 次, 停止续期(锁将在一个租期后过期), 请检查业务时长或是否遗漏 unlock",
						key, MAX_RENEW_COUNT);
				stop();
				return;
			}
			// renew.lua 区分三种结果: 1=续期成功; -1=key 已不存在(锁已过期); 0=被别的客户端持有(已易主)
			int renewed = renewer.renew(key, lockValue, leaseSeconds);
			consecutiveErrors.set(0);
			if (renewed == -1) {
				log.warn("续期失败: 锁已过期(key 不存在了), 停止续期, key={}, 本轮任务value={}", key, lockValue);
				stop();
			} else if (renewed == 0) {
				log.warn("续期失败: 锁已被别的客户端持有(已易主), 停止续期, key={}, 本轮任务value={}", key, lockValue);
				stop();
			} else if (log.isDebugEnabled()) {
				log.debug("看门狗第[{}]次续期成功, 将锁 {} 过期时间刷新为 {} 秒, 任务value={}",
						counter.get(), key, leaseSeconds, lockValue);
			}
		} catch (Exception e) {
			// 可重试路径(P0-2): 网络抖动/socket 超时/连接池暂时借不到连接等异常不代表锁已丢失。
			// 原实现在这里直接 stop(), 一次抖动就让锁在一个租期后自然过期、被第二个客户端抢走。
			// 修复: 连续失败达到阈值(默认3次, 约等于 2 个租期的不可用时长)才停止;
			// 单次异常只记日志, 锁靠上一轮续期写入的 TTL 继续维持, 下一周期再试。
			int errors = consecutiveErrors.incrementAndGet();
			if (errors >= MAX_CONSECUTIVE_ERRORS) {
				log.error("看门狗连续 {} 次续期异常, 判定 Redis 长时间不可达, 停止续期(锁将在一个租期后过期), key={}",
						errors, key, e);
				stop();
			} else {
				log.warn("看门狗续期异常(第 {} 次, 连续失败达 {} 次才停止), 下一周期继续尝试, key={}: {}",
						errors, MAX_CONSECUTIVE_ERRORS, key, e.getMessage());
			}
		}
	}

	/**
	 * 停止本轮续期并取消调度任务(幂等).
	 * 重复调用时不再打"取消续期任务"日志: 任务自我停止(锁过期/易主)后再来的 unlock 调用
	 * cancel 必然返回 false(任务已不在队列), 打出来容易被误读成"取消失败"
	 */
	void stop() {
		if (stopped) {
			return;
		}
		stopped = true;
		ScheduledFuture<?> f = future;
		if (f != null) {
			boolean cancelled = f.cancel(false);
			log.debug("取消续期任务 key={}, cancel返回={}", key, cancelled);
		}
	}

	/**
	 * 本轮看护是否已停止(供测试与诊断使用)
	 */
	boolean isStopped() {
		return stopped;
	}
}
