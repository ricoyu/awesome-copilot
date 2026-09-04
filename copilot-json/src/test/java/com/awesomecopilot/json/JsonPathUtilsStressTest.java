package com.awesomecopilot.json;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonPathUtils 压力测试。
 * <p>
 * 覆盖三个维度:
 * <ol>
 *   <li>并发正确性: 多线程读同一 path, 结果必须与串行结果一致, 无异常;</li>
 *   <li>热点路径吞吐: 多线程重复读同一 path, 观察 {@code PATH_CACHE} 命中下的 ops/s;</li>
 *   <li>多路径吞吐: 大量不同 path 轮转, 观察缓存 miss 填充 + 命中的混合吞吐。</li>
 * </ol>
 * 注: {@link JsonPathUtils#readNode(String, String)} 每次调用都会重新解析 JSON 串,
 * 因此这里测量的是「JSON 解析 + JsonPath 读取」的完整链路, 而非单纯的 compile 缓存收益。
 */
@Slf4j
public class JsonPathUtilsStressTest {

	private static final String JSON = """
			{
			  "store": {
			    "book": [
			      {"category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95},
			      {"category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99},
			      {"category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "isbn": "0-553-21311-3", "price": 8.99},
			      {"category": "fiction", "author": "J. R. R. Tolkien", "title": "The Lord of the Rings", "isbn": "0-395-19395-8", "price": 22.99}
			    ],
			    "bicycle": {"color": "red", "price": 19.95}
			  }
			}""";

	private static final String HOT_PATH = "$.store.book[*].title";

	/**
	 * 并发正确性: 32 线程各读 2000 次同一 path, 结果集合必须只有一个唯一值(与串行结果一致), 且无异常。
	 */
	@Test
	public void testConcurrentReadSamePathCorrectness() throws InterruptedException {
		int threads = 32;
		int iterationsPerThread = 2000;

		Object expected = JsonPathUtils.readNode(JSON, HOT_PATH);
		String expectedKey = String.valueOf(expected);

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		ConcurrentHashMap<String, AtomicLong> resultCounts = new ConcurrentHashMap<>();
		AtomicLong failures = new AtomicLong();

		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					start.await();
					for (int j = 0; j < iterationsPerThread; j++) {
						try {
							Object result = JsonPathUtils.readNode(JSON, HOT_PATH);
							System.out.println(JacksonUtils.toJson( result));
							resultCounts.computeIfAbsent(String.valueOf(result), k -> new AtomicLong()).incrementAndGet();
						} catch (Exception e) {
							log.error("[异常]", e);
							failures.incrementAndGet();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		start.countDown();
		assertThat(done.await(120, TimeUnit.SECONDS)).isTrue();
		pool.shutdown();

		assertThat(failures.get()).isZero();
		assertThat(resultCounts).hasSize(1);
		assertThat(resultCounts.containsKey(expectedKey)).isTrue();
		assertThat(resultCounts.get(expectedKey).get()).isEqualTo((long) threads * iterationsPerThread);
	}

	/**
	 * 热点路径吞吐: 预热后 8 线程各读 50000 次同一 path, 打印 ops/s。
	 */
	@Test
	public void testHotPathThroughput() throws InterruptedException {
		// 预热, 触发 JIT + 填充缓存
		for (int i = 0; i < 5000; i++) {
			JsonPathUtils.readNode(JSON, HOT_PATH);
		}

		int threads = 8;
		int iterationsPerThread = 50_000;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicLong total = new AtomicLong();

		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					start.await();
					long local = 0;
					for (int j = 0; j < iterationsPerThread; j++) {
						JsonPathUtils.readNode(JSON, HOT_PATH);
						local++;
					}
					total.addAndGet(local);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		long begin = System.nanoTime();
		start.countDown();
		assertThat(done.await(180, TimeUnit.SECONDS)).isTrue();
		long elapsedNanos = System.nanoTime() - begin;
		pool.shutdown();

		long ops = total.get();
		double seconds = elapsedNanos / 1_000_000_000.0;
		log.info("[热点路径] {} 次操作 / {}s = {} ops/s (线程数={})",
				ops, String.format("%.3f", seconds), String.format("%,.0f", ops / seconds), threads);
		assertThat(ops).isEqualTo((long) threads * iterationsPerThread);
	}

	/**
	 * 多路径吞吐: 500 个不同 path 轮转, 覆盖缓存 miss 填充 + 命中混合场景。
	 */
	@Test
	public void testManyDistinctPathsThroughput() throws InterruptedException {
		int pathCount = 500;
		List<String> paths = new ArrayList<>(pathCount);
		for (int i = 0; i < pathCount; i++) {
			paths.add("$.store.book[0].field_" + i);
		}

		// 预热一次, 让全部 path 进入缓存
		for (String path : paths) {
			JsonPathUtils.readNode(JSON, path);
		}

		int threads = 8;
		int rounds = 200;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicLong total = new AtomicLong();

		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					start.await();
					long local = 0;
					for (int r = 0; r < rounds; r++) {
						for (String path : paths) {
							JsonPathUtils.readNode(JSON, path);
							local++;
						}
					}
					total.addAndGet(local);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		long begin = System.nanoTime();
		start.countDown();
		assertThat(done.await(180, TimeUnit.SECONDS)).isTrue();
		long elapsedNanos = System.nanoTime() - begin;
		pool.shutdown();

		long ops = total.get();
		double seconds = elapsedNanos / 1_000_000_000.0;
		log.info("[多路径] {} 个不同 path, {} 次操作 / {}s = {} ops/s (线程数={})",
				pathCount, ops, String.format("%.3f", seconds), String.format("%,.0f", ops / seconds), threads);
		assertThat(ops).isEqualTo((long) threads * rounds * pathCount);
	}
}
