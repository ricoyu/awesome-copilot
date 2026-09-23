package com.awesomecopilot.common.lang.concurrent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-23 回归测试（CODE_REVIEW_REPORT 2026-09-22）：AbortWithReportPolicy 信号量许可泄漏，
 * 线程 dump 能力从此永久失效且无任何提示。
 * <p>
 * 泄漏路径（报告直读结论）：B 通过第一道 10 分钟时间检查后、执行 tryAcquire 前的窗口里，
 * A 完成了自己的 dump（更新 lastPrintTime 并归还许可）——B 随后 tryAcquire 拿到唯一许可、
 * 进第二道检查判定"10 分钟内已 dump 过"直接 return：许可既没被任务归还（任务没提交），
 * 也没在方法内归还。Semaphore(1) 从此归零，之后的 reject→threadDump 全部无提示失效。
 * <p>
 * 时间窗口是纳秒级竞态，单线程无法确定性复现（第二道检查与第一道同表达式，
 * 只有并发交错才让两者判据不一致）。本用例用多线程压测放大命中概率：
 * 修复前实测任一交错踩中即 permits 永远回不到 1（RED 由 stash 回验）；
 * 修复后（未提交任务必在 finally 归还）压测结束 permits 必回到 1。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class AbortWithReportPolicyGuardTest {

	private static Field field(String name) throws Exception {
		Field f = AbortWithReportPolicy.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static Semaphore guard() throws Exception {
		return (Semaphore) field("guard").get(null);
	}

	private static void resetLastPrintTime(long v) throws Exception {
		field("lastPrintTime").setLong(null, v);
	}

	private static void invokeThreadDump(AbortWithReportPolicy policy) throws Exception {
		Method m = AbortWithReportPolicy.class.getDeclaredMethod("threadDump");
		m.setAccessible(true);
		m.invoke(policy);
	}

	@Test
	public void testConcurrentDumpCallsNeverStrandSemaphorePermit() throws Exception {
		AbortWithReportPolicy policy = new AbortWithReportPolicy();
		AtomicInteger errors = new AtomicInteger();

		//反复把 lastPrintTime 推回 10 分钟前, 让多批调用有机会同时穿过第一道检查互相竞争
		Thread[] workers = new Thread[4];
		for (int t = 0; t < workers.length; t++) {
			workers[t] = new Thread(() -> {
				try {
					for (int round = 0; round < 200; round++) {
						if (round % 20 == 0) {
							resetLastPrintTime(System.currentTimeMillis() - 10 * 60 * 1000 - 1);
						}
						invokeThreadDump(policy);
					}
				} catch (Throwable e) {
					errors.incrementAndGet();
				}
			}, "guard-stress-" + t);
		}
		for (Thread w : workers) w.start();
		for (Thread w : workers) w.join(30_000);
		assertThat(errors.get()).isZero();

		//等 dump 任务排空(DUMP_EXECUTOR 是单线程); 修复前任一次竞态踩中"第二道检查 return",
		//唯一许可就永久丢失, 这里 30 秒内 permits 回不到 1
		long deadline = System.currentTimeMillis() + 30_000;
		while (guard().availablePermits() == 0 && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(guard().availablePermits())
				.as("并发拒绝风暴后 dump 能力必须仍可用(许可已归还)")
				.isEqualTo(1);
	}

	@Test
	public void testSubmitFailureReturnsPermitAndKeepsTimestamp() throws Exception {
		//评审修复(2026-09-23)后新增: 修复前的两个问题——(1)时间戳在 execute 之前推进, 提交抛异常后
		//10分钟窗口被无谓吃掉; (2)catch 才 release, 若 execute 没抛但走了第二道检查 return 分支许可照样丢。
		//这里用反射关停单线程执行器, 让 execute 必抛 RejectedExecutionException, 得到确定性验证。
		AbortWithReportPolicy policy = new AbortWithReportPolicy();
		java.util.concurrent.ExecutorService dumpExecutor =
				(java.util.concurrent.ExecutorService) field("DUMP_EXECUTOR").get(null);
		dumpExecutor.shutdown();
		boolean rejectObserved = false;
		for (int i = 0; i < 5 && !rejectObserved; i++) {
			resetLastPrintTime(System.currentTimeMillis() - 10 * 60 * 1000 - 1); //保证穿过两道时间检查
			invokeThreadDump(policy);
			rejectObserved = true; //shutdown 后 execute 必抛, 走到 catch
		}
		assertThat(guard().availablePermits())
				.as("提交失败必须归还唯一许可(否则 dump 能力从此丢失)").isEqualTo(1);
		long lpt = field("lastPrintTime").getLong(null);
		assertThat(lpt)
			.as("提交失败时不应推进 lastPrintTime(评审修复: 时间戳移到提交成功之后)")
			.isLessThan(System.currentTimeMillis() - 10 * 60 * 1000);
		//执行器已永久关停属于极端场景, 本用例结束后类加载期内不再恢复; 后续用例不依赖 DUMP_EXECUTOR 可用
	}
	@Test
	public void testRateLimitedReturnDoesNotConsumePermit() throws Exception {
		AbortWithReportPolicy policy = new AbortWithReportPolicy();
		resetLastPrintTime(System.currentTimeMillis()); //10 分钟窗口内

		invokeThreadDump(policy); //第一道检查就返回, 不碰许可

		assertThat(guard().availablePermits()).isEqualTo(1);
	}
}
