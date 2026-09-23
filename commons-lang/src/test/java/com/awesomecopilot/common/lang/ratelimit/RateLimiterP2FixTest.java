package com.awesomecopilot.common.lang.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-3 / P2-33 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P2-3 TokenBucketRateLimiter.refillTokens 先扣 pendingRefillMillis 再做 CAS，
 * CAS 失败（有并发 acquire）时本轮增量作废——修复前实测（高水位 8 消费者压测）
 * 46 万注入丢失 1~2 万令牌，实际放行速率低于配置值；</li>
 * <li>P2-33 LeakyBucketRateLimiter.shutdown() javadoc 写"处理完队列中剩余请求"，
 * 修复前 isRunning=false 后漏出循环直接 return，桶里已承诺接收的任务被丢弃；</li>
 * <li>P2-33 TokenBucket 构造器零参数校验（refillRate&lt;=0 令牌只出不进）；</li>
 * <li>P2-3 接口层面：RateLimiter 继承 AutoCloseable，持有调度线程的实现必须能统一释放。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class RateLimiterP2FixTest {

	// ---------------- P2-3 补令牌守恒 ----------------

	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	public void testRefillNeverLosesTokensUnderConcurrentAcquire() throws Exception {
		//复刻探针场景: 高容量 + 10000/秒补充, 8 个消费者线程狂抢, 手动驱动 refill 制造
		//get->CAS 窗口竞争。守恒式: 桶里剩余 + 已消费 == 总注入, 任何差额都是被丢弃的增量。
		TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(Long.MAX_VALUE / 4, 10000L, 1000L);
		Field sf = TokenBucketRateLimiter.class.getDeclaredField("scheduler");
		sf.setAccessible(true);
		((java.util.concurrent.ScheduledExecutorService) sf.get(limiter)).shutdownNow();

		Field tf = TokenBucketRateLimiter.class.getDeclaredField("tokens");
		tf.setAccessible(true);
		((AtomicLong) tf.get(limiter)).set(0);
		Field pf = TokenBucketRateLimiter.class.getDeclaredField("pendingRefillMillis");
		pf.setAccessible(true);
		pf.setLong(limiter, 0);

		Method refill = TokenBucketRateLimiter.class.getDeclaredMethod("refillTokens");
		refill.setAccessible(true);
		refill.invoke(limiter);  //先垫 10000 高水位, 保证消费者在 refill 的 CAS 窗口里有令牌可抢

		int consumers = 8;
		AtomicInteger consumed = new AtomicInteger();
		boolean[] run = {true};
		Thread[] ts = new Thread[consumers];
		for (int i = 0; i < consumers; i++) {
			ts[i] = new Thread(() -> {
				while (run[0]) {
					if (limiter.canPass()) {
						consumed.incrementAndGet();
					}
				}
			}, "consumer-" + i);
			ts[i].setDaemon(true);
			ts[i].start();
		}

		int ticks = 40;
		for (int i = 0; i < ticks; i++) {
			refill.invoke(limiter);
			Thread.sleep(2);
		}
		Thread.sleep(50);
		run[0] = false;
		for (Thread t : ts) t.join(5000);
		for (int i = 0; i < 5; i++) {
			refill.invoke(limiter); //清掉 pending 尾数(此时无并发, 必成功)
		}

		long expected = (long) (ticks + 6) * 10000;
		long inBucket = ((AtomicLong) tf.get(limiter)).get();
		long lost = expected - inBucket - consumed.get();
		limiter.close();
		assertThat(lost)
				.as("修复前实测丢 1~2 万令牌(CAS 失败批次被作废), 必须零丢失")
				.isZero();
	}

	// ---------------- P2-33 TokenBucket 构造参数校验 ----------------

	@Test
	public void testTokenBucketRejectsNonPositiveRefillRate() {
		//修复前: refillRate<=0 不报错直接通过, 令牌只出不进, 桶空后全量拒绝且无任何告警
		assertThatThrownBy(() -> new TokenBucketRateLimiter(10, 0, 100))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("refillRate");
		assertThatThrownBy(() -> new TokenBucketRateLimiter(10, -5, 100))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TokenBucketRateLimiter(0, 10, 100))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("capacity");
		assertThatCode(() -> {
			TokenBucketRateLimiter ok = new TokenBucketRateLimiter(10, 10, 100);
			ok.close();
		}).doesNotThrowAnyException();
	}

	// ---------------- P2-33 LeakyBucket shutdown 排空 ----------------

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testLeakyBucketShutdownDrainsAcceptedRequests() throws Exception {
		//漏出速率 2/秒: 4 个请求入桶, 等 1 个漏出后 shutdown(), 承诺接收的其余请求必须执行完
		LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(10, 2);
		AtomicInteger executed = new AtomicInteger();
		for (int i = 0; i < 4; i++) {
			assertThat(limiter.submitRequest(executed::incrementAndGet)).isTrue();
		}
		Thread.sleep(700);      //让定时器漏出第 1 个
		limiter.close();        //修复前: 剩余 3 个被无提示丢弃(不执行、不计数、无日志)

		//close() 返回时排空已完成(修复实现: 关停定时器后在调用线程 drain 队列)
		assertThat(executed.get())
				.as("shutdown 前已入队的请求必须全部执行, 不允许无提示丢弃")
				.isEqualTo(4);

		//关闭后不再接收新请求
		assertThat(limiter.submitRequest(() -> {
		})).isFalse();
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testLeakyBucketRequestRunsOnCallerPoolThreadWithoutRelay() throws Exception {
		//修复前请求被转投同一个 scheduler 的无界队列, capacity 不是实际在途上界(无背压)。
		//修复后请求直接在漏出线程执行: 阻塞一个在途任务时, 队列满即拒绝, 不会"全部接收后再排队"。
		LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(2, 1);
		CountDownLatch block = new CountDownLatch(1);
		CountDownLatch started = new CountDownLatch(1);
		try {
			assertThat(limiter.submitRequest(() -> {
				started.countDown();
				try {
					block.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			})).isTrue();
			assertThat(limiter.submitRequest(() -> {
			})).isTrue();
			assertThat(limiter.submitRequest(() -> {
			})).as("桶容量 2, 第 3 个必须被拒绝").isFalse();
			assertThat(started.await(5, TimeUnit.SECONDS))
					.as("第 1 个请求应在漏出速率 1/秒内开始执行")
					.isTrue();
		} finally {
			block.countDown();
			limiter.close();
		}
	}

	// ---------------- P2-3 接口层面 AutoCloseable ----------------

	@Test
	public void testRateLimiterIsAutoCloseable() {
		//接口可放进 try-with-resources; 无后台资源的实现(SlidingWindow)继承默认空 close()
		assertThat(AutoCloseable.class).isAssignableFrom(RateLimiter.class);
		SlidingWindow sw = new SlidingWindow(1L, TimeUnit.SECONDS, 5);
		assertThatCode(() -> {
			try (RateLimiter r = sw) {
				assertThat(r.canPass()).isTrue();
			}
		}).doesNotThrowAnyException();
	}
}
