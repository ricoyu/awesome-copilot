package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归: DateConstants.SDT_GMT 改为 ThreadLocal 包装后, 并发解析HTTP头日期格式(RFC1123)
 * 必须全部拿到正确结果.
 * <p>
 * 旧实现是 public static final SimpleDateFormat 直接共享, 8线程并发解析实测12万次错921次
 * (错的是日期值本身, 不抛异常). 见 copilot-json 审计报告 P0-1.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SdtGmtConcurrencyTest {

	@Test
	public void concurrentParseOfRfc1123DatesMustBeExact() throws Exception {
		List<String> inputs = new ArrayList<>();
		List<Long> expects = new ArrayList<>();
		long base = 784111777000L; // "Sun, 06 Nov 1994 08:49:37 GMT"
		for (int i = 0; i < 500; i++) {
			Date d = new Date(base + i * 1000L);
			inputs.add(DateUtils.formatToRfc(d));
			expects.add(d.getTime());
		}

		int threads = 8;
		int iterationsPerThread = 15000;
		AtomicLong wrong = new AtomicLong();
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < threads; t++) {
			futures.add(pool.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < iterationsPerThread; i++) {
						int idx = i % inputs.size();
						Date parsed = DateUtils.parse(inputs.get(idx));
						if (parsed == null || parsed.getTime() != expects.get(idx)) {
							wrong.incrementAndGet();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}));
		}
		start.countDown();
		for (Future<?> f : futures) {
			f.get(120, TimeUnit.SECONDS);
		}
		pool.shutdown();

		assertEquals(0L, wrong.get(), "并发解析RFC1123日期出现错值, SDT_GMT线程隔离失效?");
		// 兜底分支的另一半: toLocalDateTime 内部同样走 SDT_GMT, 串行验证一次
		assertEquals(784111777000L, DateUtils.toLocalDateTime(inputs.get(0)) == null ? -1L
				: DateUtils.toDate(DateUtils.toLocalDateTime(inputs.get(0))).getTime(),
				"toLocalDateTime(RFC串) 结果错误");
	}
}
