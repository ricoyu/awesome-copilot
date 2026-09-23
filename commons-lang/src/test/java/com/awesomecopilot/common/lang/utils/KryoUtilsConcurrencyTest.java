package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-1 回归测试：KryoUtils 多线程共用同一个 Kryo 实例导致序列化结果错乱。
 * <p>
 * 修复前现象（CODE_REVIEW_REPORT P0-1 实测）：8 线程并发各 300 次往返，
 * 出现 KryoException: Unable to find class / 反序列化结果与源对象不等。
 * Kryo 官方声明实例非线程安全（内部复用 class 注册表与输出缓冲）。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class KryoUtilsConcurrencyTest {

	private static final int THREADS = 8;
	private static final int ROUNDS = 300;

	@Test
	public void testConcurrentRoundTripKeepsDataIntact() throws InterruptedException {
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(THREADS);
		AtomicInteger failures = new AtomicInteger();
		List<Throwable> samples = new ArrayList<>();

		for (int t = 0; t < THREADS; t++) {
			final int tid = t;
			Thread thread = new Thread(() -> {
				try {
					startLatch.await();
					for (int i = 0; i < ROUNDS; i++) {
						ArrayList<String> source = new ArrayList<>();
						for (int k = 0; k < 5; k++) {
							source.add("item-" + tid + "-" + i + "-" + k);
						}
						try {
							byte[] bytes = KryoUtils.toBytes(source);
							@SuppressWarnings("unchecked")
							ArrayList<String> restored = (ArrayList<String>) KryoUtils.toObject(bytes);
							if (!source.equals(restored)) {
								failures.incrementAndGet();
							}
						} catch (Throwable e) {
							failures.incrementAndGet();
							synchronized (samples) {
								if (samples.size() < 3) samples.add(e);
							}
						}
					}
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			}, "kryo-probe-" + tid);
			//守护线程：若缺陷重现导致线程挂死，不阻止 JVM 退出（与 spliterator 回归测试同法）
			thread.setDaemon(true);
			thread.start();
		}

		startLatch.countDown();
		// await 返回 false 即测试失败，避免共享实例引发的内部死循环把整个测试挂死
		// （修复前实测：jstack 显示 8 个线程在 Kryo 的 IdentityObjectIntMap.locateKey 处空转）
		assertThat(doneLatch.await(60, java.util.concurrent.TimeUnit.SECONDS))
				.as("60 秒内全部线程应完成——共享 Kryo 实例会把内部引用表改坏导致死循环")
				.isTrue();

		// 8线程 x 300次 x 2个方向 = 4800 次使用，任何一次异常或数据不符都说明实例被并发共享
		assertThat(samples).isEmpty();
		assertThat(failures.get())
				.as("并发往返失败次数（修复前实测 2400 次里 651 次异常或数据不符）")
				.isZero();
	}
}
