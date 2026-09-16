package com.awesomecopilot.common.lang.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * volatile 只保证可见性与有序性, 不保证原子性: {@code count++} 是"读-改-写"三步复合操作,
 * 两个线程交叉执行时会发生写覆盖(lost update).
 * <p>
 * 结论: 2 个线程各自增 100 次, 最终值不确定, 上界是 200, 但**没有贴近的下界**
 * (实测 8000 轮最低只到 59), 而且偶发会正好等于 200.
 * <p>
 * 为什么下界不是 100? 常见的错误直觉是"线程自己第 k 次写入前, volatile 读到的值
 * 必然不早于它自己第 k-1 次写入, 故最终值 >= 100". 这个推理隐含假设了"变量值只增不减",
 * 而并发自增恰恰会打破它: 线程 B 在早期读到 19 后被调度出去, 线程 A 继续把值推到 80,
 * B 回来后把 19+1=20 写回去 —— 变量值从 80 **倒退** 到 20. 因此最后一笔写入
 * 完全可能是一个很小的数, 上述"下界 100"的推理不成立.
 * <p>
 * 因为单次运行结果非确定(甚至偶发会正好等于 200), 所以这里跑多轮, 断言"至少有一轮丢更新",
 * 而不是"每轮都丢更新", 避免测试 flaky.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class VolatileIncrementTest {
	
	private static final int THREADS = 2;
	private static final int TIMES = 100;
	/** 多轮重复, 让"至少一轮丢更新"成为统计学上必然成立的事实 */
	private static final int ROUNDS = 500;
	/** 预热轮数, 让 JIT 先把自增路径编译成机器码, 排除解释执行的干扰 */
	private static final int WARMUP = 2000;
	
	/** 被两个线程并发自增的 volatile 变量 */
	private static volatile int volatileCount = 0;
	
	/**
	 * 起跑门闸. 这里刻意不用 {@link CountDownLatch#await()} 同步起跑:
	 * await 会把 worker 线程 park, 而唤醒一个被挂起的线程要几十微秒,
	 * 100 次 volatile 自增只需约 1 微秒 —— 第一个线程早就跑完了, 第二个还没被调度起来,
	 * 两个线程实际是串行的, 每轮都会"恰好"得到 200, 竞争根本没发生过.
	 * 改成让两个 worker 在同一个 volatile 标志上热自旋, 释放延迟才降到纳秒级.
	 */
	private static volatile boolean go;
	
	@Test
	public void volatileIncrementLosesUpdates() throws Exception {
		int upperBound = THREADS * TIMES;
		int roundsWithLoss = 0;
		int lowest = upperBound;
		int highest = 0;
		
		try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
			for (int i = 0; i < WARMUP; i++) {
				volatileCount = 0;
				race(pool, THREADS, () -> volatileCount++, () -> volatileCount);
			}
			
			for (int round = 0; round < ROUNDS; round++) {
				volatileCount = 0;
				int actual = race(pool, THREADS, () -> volatileCount++, () -> volatileCount);
				
				// 200 是两线程完全不互相覆盖时的理想上限, 实际不可能超过它;
				// 下限只能取 0 —— 见类上注释, 变量值会因慢线程把旧值写回而倒退
				assertTrue(actual >= 0 && actual <= upperBound,
						"结果超出了 [0, 200] 的范围, 测例框架有问题? 本轮=" + actual);
				lowest = Math.min(lowest, actual);
				highest = Math.max(highest, actual);
				if (actual < upperBound) {
					roundsWithLoss++;
				}
			}
		}
		
		System.out.printf("volatile: %d/%d 轮出现丢更新, 实测区间=[%d, %d], 理论最大值=%d%n",
				roundsWithLoss, ROUNDS, lowest, highest, upperBound);
		assertTrue(roundsWithLoss > 0,
				"volatile 自增被证明是原子的? 这与 JMM 语义矛盾, 请检查测试是否退化成串行");
	}
	
	@Test
	public void atomicIntegerIncrementIsExact() throws Exception {
		int upperBound = THREADS * TIMES;
		
		try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
			for (int round = 0; round < ROUNDS; round++) {
				AtomicInteger atomicCount = new AtomicInteger();
				int actual = race(pool, THREADS, atomicCount::incrementAndGet, atomicCount::get);
				
				assertEquals(upperBound, actual,
						"CAS 自增在轮次 " + round + " 上丢更新了?");
			}
		}
	}
	
	@Test
	public void singleThreadIncrementIsExact() throws Exception {
		volatileCount = 0;
		
		try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
			int actual = race(pool, 1, () -> volatileCount++, () -> volatileCount);
			
			assertEquals(TIMES, actual, "无竞争时 volatile 自增结果就应该等于自增次数");
		}
	}
	
	/**
	 * 让 {@code threads} 个线程在同一个 volatile 门闸上自旋就位, 再统一放行, 把竞争窗口压到最大.
	 *
	 * @param pool      执行线程池
	 * @param threads   参与竞争的线程数
	 * @param increment 单次自增动作
	 * @param counter   读取最终值的动作
	 * @return 全部线程跑完后的变量值
	 */
	private int race(ExecutorService pool, int threads, Runnable increment, IntSupplier counter)
			throws InterruptedException {
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch done = new CountDownLatch(threads);
		go = false;
		for (int i = 0; i < threads; i++) {
			pool.execute(() -> {
				ready.countDown();
				while (!go) {
					Thread.onSpinWait();
				}
				try {
					for (int j = 0; j < TIMES; j++) {
						increment.run();
					}
				} finally {
					done.countDown();
				}
			});
		}
		ready.await();
		// 此刻两个 worker 都已在门闸上热自旋, 放行的起跑偏差只有纳秒级
		go = true;
		boolean finished = done.await(30, TimeUnit.SECONDS);
		assertTrue(finished, "自增线程未在 30 秒内结束");
		return counter.getAsInt();
	}
}
