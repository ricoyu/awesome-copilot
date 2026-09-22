package com.awesomecopilot.cache.concurrent;

import com.awesomecopilot.cache.JedisUtils;
import com.awesomecopilot.cache.exception.OperationNotSupportedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分布式锁修复批次的回归测试（对应 DISTRIBUTED_LOCK_REVIEW.md 的 P0/P1 条目）
 * <p>
 * 需要一个真实 Redis: 配置在 src/test/resources/redis.properties
 * <p>
 * Copyright: (C), 2026/9/22
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class LockFixTest {

	/**
	 * 每个 JVM 用独立的 key 命名空间: IDE 调试和命令行同时跑这些测试时,
	 * 两边都用固定 key 会互相删除对方的锁, 让测试随机失败; 加上 pid 后各跑各的
	 */
	private static final String NS = "fix-" + ProcessHandle.current().pid();

	/**
	 * renewLock 的返回值契约(WatchDog 停止续期的判断依据):
	 * 1=value 匹配且刷新成功; -1=key 不存在(锁已过期); 0=key 被别的客户端持有(已易主)。
	 * 三种结果必须可区分: 运维排查"为什么不再续期"时, 日志要能说出是过期还是易主。
	 */
	@Test
	public void testRenewLockReturnsDistinguishableResults() {
		JedisUtils.del(NS + ":renew3");
		// key 不存在 -> -1
		assertEquals(-1, JedisUtils.renewLock(NS + ":renew3", "req-a", 3, TimeUnit.SECONDS));
		// 自己持有 -> 1, 且 TTL 被刷新回 3 秒附近
		assertTrue(JedisUtils.lock(NS + ":renew3", "req-a", 3, TimeUnit.SECONDS));
		assertEquals(1, JedisUtils.renewLock(NS + ":renew3", "req-a", 3, TimeUnit.SECONDS));
		// 被别的 requestId 持有 -> 0, 且不能给对方锁刷新过期时间
		assertEquals(0, JedisUtils.renewLock(NS + ":renew3", "req-b", 3, TimeUnit.SECONDS));
		assertTrue(JedisUtils.unlock(NS + ":renew3", "req-a"));
	}

	/**
	 * 报告 P0-4: setnx/lock 的租期经过 TimeUnit 换算后为 0 秒时, EXPIRE key 0 会立即删除刚写入的 key,
	 * 方法返回"加锁成功"但锁已不存在, 互斥完全失效。修复后应直接拒绝这种入参。
	 */
	@Test
	public void testSubSecondLeaseIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> JedisUtils.lock(NS + ":subsecond", "req-1", 500, TimeUnit.MILLISECONDS));
		assertThrows(IllegalArgumentException.class,
				() -> JedisUtils.setnx(NS + ":subsecond", "req-1", 1, TimeUnit.MILLISECONDS));
		// 正常租期不受影响
		assertTrue(JedisUtils.lock(NS + ":subsecond", "req-2", 3, TimeUnit.SECONDS));
		assertTrue(JedisUtils.unlock(NS + ":subsecond", "req-2"));
	}

	/**
	 * 报告 P1-8: shaHashs 缓存的脚本 SHA 在 Redis 执行 SCRIPT FLUSH(或重启)后失效,
	 * 之后所有 evalsha 都会报 NOSCRIPT, 锁模块不可用直到应用重启。修复后应自动重载脚本并重试一次。
	 */
	@Test
	public void testLockWorksAfterScriptCacheFlushed() {
		// 预热: 让 setnx.lua / unlock.lua 的 SHA 进入缓存
		assertTrue(JedisUtils.lock(NS + ":noscript", "warm", 3, TimeUnit.SECONDS));
		assertTrue(JedisUtils.unlock(NS + ":noscript", "warm"));

		// 清空 Redis 侧脚本缓存, 本地 shaHashs 仍是旧值
		JedisUtils.execute(jedis -> jedis.scriptFlush());

		// 加锁/解锁都必须照常工作(内部捕获 NOSCRIPT 后 scriptLoad 重试)
		assertTrue(JedisUtils.lock(NS + ":noscript", "after-flush", 3, TimeUnit.SECONDS));
		assertEquals("after-flush", JedisUtils.get(NS + ":noscript"));
		assertTrue(JedisUtils.unlock(NS + ":noscript", "after-flush"));
	}

	/**
	 * 报告 P0-2: JedisPoolOperations.subscribe 借出的 Jedis 从不归还, 每个"等待过锁"的线程永久泄漏 1 条连接。
	 * 测试资源里连接池 maxTotal=10: 两轮各 5 个等待者, 每轮等全部等待者结束(订阅连接取消)再进入下一轮。
	 * 修复前第二轮结束时池内已有 10 条连接被永久占用, 主线程最后的探测写入借不到连接、2 秒后抛异常;
	 * 修复后订阅连接随 unsubscribe 归还, 探测写入正常成功。
	 */
	@Test
	public void testWaitingSubscribersReturnConnectionsToPool() throws Exception {
		String biz = NS + ":leak:" + System.nanoTime();

		for (int round = 1; round <= 2; round++) {
			Lock holder = JedisUtils.blockingLock(biz);
			holder.lock();

			CountDownLatch roundDone = new CountDownLatch(5);
			for (int i = 0; i < 5; i++) {
				new Thread(() -> {
					try {
						Lock l = JedisUtils.blockingLock(biz);
						l.lock();
						l.unlock();
					} finally {
						roundDone.countDown();
					}
				}, "waiter-" + round + "-" + i).start();
			}

			// 留出时间让 5 个等待者完成订阅(自旋 32 次失败后进入 park, 之前先 SUBSCRIBE)
			TimeUnit.SECONDS.sleep(3);
			holder.unlock();
			// 等本轮等待者全部完成"醒来→加锁→解锁→取消订阅", 下一轮开始前连接已全部归还(修复后)
			assertTrue(roundDone.await(60, TimeUnit.SECONDS), "第" + round + "轮等待者没有全部完成");
		}

		// 关键断言: 此刻若连接已被泄漏完(修复前), 这行会阻塞 2 秒后抛 JedisException
		JedisUtils.set(NS + ":leak:probe", "1");
		assertEquals("1", JedisUtils.get(NS + ":leak:probe"));
	}

	/**
	 * 报告 P0-1 + P0-3 的核心行为: 业务线程在持锁期间处于 TIMED_WAITING(sleep/等下游响应/等锁都是这个状态),
	 * 旧代码的"空闲检测"据此停止续期, 锁在租期(此处 2 秒)后过期。修复后只要持锁线程存活且未解锁就必须续期。
	 * <p>
	 * 使用短租期 2 秒(新增构造器), 持有 6 秒: 第 3~6 秒期间锁必须仍然存在(ttl > 0)。
	 */
	@Test
	public void testWatchdogRenewsWhileHolderThreadIsSleeping() {
		//清理前序测试(preempt 超时机制)可能残留的中断标记, 否则 sleep(6) 会被立即打断导致假失败
		Thread.interrupted();
		Lock lock = new BlockingLock(NS + ":renew", 2);
		lock.lock();
		try {
			TimeUnit.SECONDS.sleep(6);
			// 若续期被"空闲检测"错误停止, 锁在 2 秒时已过期, ttl 返回 -2(key 不存在)
			assertTrue(JedisUtils.ttl("copilot:blk:" + NS + ":renew:lock") > 0,
					"持锁线程 sleep 期间看门狗没有续期, 锁已过期(旧'空闲检测'误判)");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 报告 P0-1: watchDogStopped 是实例字段且 stopWatchDog 置 true 后 startWatchDog 从不复位,
	 * 同一实例第二次加锁后看门狗直接退出不再续期。修复后每次加锁的续期状态独立生效。
	 */
	@Test
	public void testReusedInstanceRenewsOnSecondAcquire() {
		//清理前序测试(preempt 超时机制)可能残留的中断标记, 否则 sleep(6) 会被立即打断导致假失败
		Thread.interrupted();
		BlockingLock lock = new BlockingLock(NS + ":reuse", 2);

		lock.lock();
		lock.unlock();

		lock.lock();
		try {
			TimeUnit.SECONDS.sleep(6);
			assertTrue(JedisUtils.ttl("copilot:blk:" + NS + ":reuse:lock") > 0,
					"实例复用后第二次加锁没有续期(watchDogStopped 未复位)");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 报告 P1-7: 同一线程用同一实例重复 lock(), setnx 必然失败(锁是自己写的),
	 * 于是自己等待自己释放锁, 表现为 30~100 秒无响应。修复后应立即抛出明确异常。
	 * <p>
	 * 必须在同一线程上调用第二次 lock()(重入检测按线程判断); 租期故意用 2 秒:
	 * 若将来误删了重入保护, 阻塞版本会在锁过期后"自己等到自己", 本断言会在几秒内失败而不是挂死套件
	 */
	@Test
	public void testSameThreadReentrantLockFailsFast() {
		Lock lock = new BlockingLock(NS + ":reentrant", 2);
		lock.lock();
		try {
			assertThrows(OperationNotSupportedException.class, lock::lock,
					"同线程重入 lock() 应立即抛异常而不是自行等待");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 报告 P2-12 的行为底线: 8 个线程并发竞争同一把 blockingLock, 临界区内绝不能重叠。
	 * 每个线程抢锁后检查"是否已有其他线程在临界区内", 有则计一次违规。
	 */
	@Test
	public void testConcurrentThreadsGetLockExclusively() throws Exception {
		String biz = NS + ":mutex:" + System.nanoTime();
		int threads = 8;
		AtomicInteger violations = new AtomicInteger();
		AtomicInteger completed = new AtomicInteger();
		Map<Integer, Boolean> inside = new ConcurrentHashMap<>();
		AtomicBoolean stopCheck = new AtomicBoolean();

		List<Thread> ts = new java.util.ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final int id = i;
			Thread t = new Thread(() -> {
				Lock l = JedisUtils.blockingLock(biz);
				l.lock();
				try {
					// 记录进入临界区的线程集合, 同时检查是否有其他线程也在内
					inside.put(id, true);
					if (stopCheck.get() && inside.size() > 1) {
						violations.incrementAndGet();
					}
					TimeUnit.MILLISECONDS.sleep(200);
					stopCheck.set(true);
					inside.remove(id);
					completed.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					l.unlock();
				}
			}, "mutex-" + i);
			ts.add(t);
			t.start();
		}
		for (Thread t : ts) {
			t.join(90_000);
		}
		assertEquals(threads, completed.get(), "所有线程都应最终获得并完成锁");
		assertEquals(0, violations.get(), "临界区出现重叠, 互斥被破坏");
	}
}
