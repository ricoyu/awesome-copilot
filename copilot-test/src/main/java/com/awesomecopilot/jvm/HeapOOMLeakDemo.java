package com.awesomecopilot.jvm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 堆内存溢出(Heap OOM)演示程序 —— 模拟「本地缓存只 put 不 evict」这一线上最常见的内存泄漏场景。
 * <p>
 * 泄漏链(也是 MAT 里 Leak Suspects 报告给出的引用链)：
 * <pre>
 *   com.awesomecopilot.jvm.HeapOOMLeakDemo.USER_TRACE_CACHE (static 字段, 本身就是 GC Root)
 *     -> java.util.HashMap$Node[]
 *       -> com.awesomecopilot.jvm.HeapOOMLeakDemo$UserTrace
 *         -> java.util.ArrayList -> java.util.HashMap$Node[] -> byte[32768]
 * </pre>
 * 因为是 static Map 强引用, 这些对象全程「可达」, GC 一次都回收不掉, 所以堆只涨不落, 直到抛
 * java.lang.OutOfMemoryError: Java heap space。
 * <p>
 * 启动参数(heapdump 由 JVM 在抛 OOM 的那一刻自动落盘, 无需 kill -3 / jmap):
 * <pre>
 * -Xms64m -Xmx64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target/heapdump/heap-oom-leak.hprof
 * </pre>
 * 跑起来(IDEA 里直接 Run 本类, 把上面参数填到 Modify options -> Add VM options; 命令行则在 copilot-test 目录下):
 * <pre>
 * mvn -q -pl copilot-test -am compile
 * java -Xms64m -Xmx64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target/heapdump/heap-oom-leak.hprof -cp target/classes com.awesomecopilot.jvm.HeapOOMLeakDemo
 * </pre>
 * 生成的 .hprof 用 Memory Analyzer (MAT) 打开, 跑 Leak Suspects 即可定位到本类的 static 字段。
 * <p>
 * Copyright: (C), 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HeapOOMLeakDemo {

	/**
	 * 每个请求往缓存里塞多少个快照分片, 每片 32KB, 即单次请求约 256KB
	 */
	private static final int SNIPPETS_PER_TRACE = 8;

	private static final int SNIPPET_SIZE = 32 * 1024;

	/**
	 * 问题根源: static 缓存, 有入无出, 没有任何淘汰策略(不像 Caffeine 有 maximumSize / expireAfterWrite)
	 */
	private static final Map<String, UserTrace> USER_TRACE_CACHE = new HashMap<>();

	public static void main(String[] args) {
		System.out.println("最大堆: " + toMb(Runtime.getRuntime().maxMemory()) + " MB, 开始压入带缓存的请求...");

		int round = 0;
		// 注意: 这里故意不 catch OutOfMemoryError。一是 catch 了也救不回来(分配失败即已无力回天),
		// 二是让 OOM 原样抛出, JVM 才会在抛出的那一刻自动写 heapdump, 栈轨迹也最干净
		while (true) {
			round++;
			serve("user-" + round);

			if (round % 20 == 0) {
				System.out.printf("已处理 %d 个请求, 缓存条目 %d 个, 已用堆 %d MB%n",
						round, USER_TRACE_CACHE.size(), toMb(usedHeap()));
			}
		}
	}

	/**
	 * 模拟一次请求: 查出用户的行为轨迹快照并「顺手」放进本地缓存, 但永远没有人清理它
	 */
	private static void serve(String userId) {
		UserTrace trace = new UserTrace(userId, loadTraceSnippets(userId));
		USER_TRACE_CACHE.put(userId, trace);
	}

	/**
	 * 模拟从 DB / ES 读出来的大字段(报文、图片二进制、导出中间结果等)
	 */
	private static List<byte[]> loadTraceSnippets(String userId) {
		byte[] seed = userId.getBytes(StandardCharsets.UTF_8);
		List<byte[]> snippets = new ArrayList<>(SNIPPETS_PER_TRACE);
		for (int i = 0; i < SNIPPETS_PER_TRACE; i++) {
			byte[] snippet = new byte[SNIPPET_SIZE];
			System.arraycopy(seed, 0, snippet, 0, Math.min(seed.length, snippet.length));
			snippets.add(snippet);
		}
		return snippets;
	}

	private static long usedHeap() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	private static long toMb(long bytes) {
		return bytes >> 20;
	}

	/**
	 * 业务对象本身很小, 真正吃内存的是它持有的 snippets
	 */
	static class UserTrace {

		private final String userId;

		private final List<byte[]> snippets;

		UserTrace(String userId, List<byte[]> snippets) {
			this.userId = userId;
			this.snippets = snippets;
		}

		@Override
		public String toString() {
			return "UserTrace{userId=" + userId + ", snippets=" + snippets.size() + "}";
		}
	}
}
