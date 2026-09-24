package com.awesomecopilot.cache.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 看门狗对"续期异常"的容错回归测试(评估报告 P0-2)。
 * <p>
 * 原实现: run() 的 catch(Exception) 与"锁已过期/已易主"走同一个 stop() 出口——
 * 一次网络抖动(socket 超时默认 1000ms、主从切换、连接池借不到连接)即永久停止续期,
 * 锁在一个租期后自然过期, 第二个客户端加锁成功, 两方同时进临界区。
 * <p>
 * 修复后语义:
 * <ul>
 *   <li>续期返回 -1/0(锁已过期/已易主): 立即停止, 不重试——锁确实没了;</li>
 *   <li>续期抛异常(连接/超时类): 连续失败达到阈值才停止, 单次抖动不影响;</li>
 *   <li>异常后再次成功: 连续失败计数清零。</li>
 * </ul>
 * Copyright: (C), 2026/9/24
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WatchDogErrorToleranceTest {

	/**
	 * 单次续期抛异常(模拟网络抖动)后, 下一次仍继续续期且成功后一切正常
	 */
	@Test
	public void testTransientErrorDoesNotStopRenewing() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		WatchDog dog = new WatchDog("wd:jitter", "req-1", 3, (k, v, lease) -> {
			if (calls.incrementAndGet() == 1) {
				throw new RuntimeException("模拟: socket read timeout");
			}
			return 1;
		});
		dog.schedule();
		// 租期3秒 → 间隔1秒; 等到第2、3轮: 第1轮抛异常, 第2轮成功
		Thread.sleep(2500);
		assertFalse(dog.isStopped(), "单次续期异常就永久停止续期(P0-2)");
		assertTrue(calls.get() >= 2, "抖动后应继续尝试续期, 实际调用次数: " + calls.get());
		dog.stop();
	}

	/**
	 * 连续失败达到阈值(3次)后停止, 之后的周期不再发起续期; 停止前锁靠租期自然过期来结束持有
	 */
	@Test
	public void testConsecutiveErrorsStopAfterThreshold() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		WatchDog dog = new WatchDog("wd:down", "req-1", 3, (k, v, lease) -> {
			calls.incrementAndGet();
			throw new RuntimeException("模拟: Redis 持续不可达");
		});
		dog.schedule();
		// 间隔1秒: 等 4.5 秒应累计 >=3 次失败并停止
		Thread.sleep(4500);
		assertTrue(dog.isStopped(), "连续失败达到阈值后应停止(避免无限重试已经丢锁的任务)");
		int atStop = calls.get();
		assertTrue(atStop >= 3 && atStop <= 4, "应在 3~4 次连续失败后停止, 实际: " + atStop);
		Thread.sleep(2500);
		assertEquals(atStop, calls.get(), "停止后不应再发起续期");
	}

	/**
	 * 续期返回 -1(锁已过期)时立即停止: 这不是可重试异常, 重试没有意义
	 */
	@Test
	public void testLockExpiredStopsImmediately() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		WatchDog dog = new WatchDog("wd:expired", "req-1", 3, (k, v, lease) -> {
			calls.incrementAndGet();
			return -1;
		});
		dog.schedule();
		Thread.sleep(2500);
		assertTrue(dog.isStopped(), "锁已过期应立即停止续期");
		assertEquals(1, calls.get(), "返回 -1 后不应再重试");
	}

	/**
	 * 先失败2次再成功, 然后又开始失败: 连续计数在成功时清零, 需要重新累计满阈值才停止
	 */
	@Test
	public void testSuccessResetsConsecutiveErrorCount() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		WatchDog dog = new WatchDog("wd:reset", "req-1", 3, (k, v, lease) -> {
			int n = calls.incrementAndGet();
			// 第1、2、4次失败, 第3、5次起成功
			if (n == 1 || n == 2 || n == 4) {
				throw new RuntimeException("模拟抖动 " + n);
			}
			return 1;
		});
		dog.schedule();
		Thread.sleep(5500);
		assertFalse(dog.isStopped(), "失败没有连续满3次(第3、5次成功打了折扣), 不应停止");
		dog.stop();
	}
}
