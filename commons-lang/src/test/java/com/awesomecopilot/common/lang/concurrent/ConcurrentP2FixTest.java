package com.awesomecopilot.common.lang.concurrent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.awesomecopilot.common.lang.exception.AsyncExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-1 / P2-2 / P2-34 / P2-37 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P2-1 AbortWithReportPolicy 的线程 dump 文本被 SLF4J 丢弃：
 * 修复前 log.error("thread dump info:", sb.toString()) 走 error(String, Object)，
 * 格式串没有 {} 占位符时第二个参数根本不输出，实测日志只剩 "[thread dump info:]"；</li>
 * <li>P2-2 Concurrent.ioConcentratedFixedThreadPool 无界队列：
 * 修复前 LinkedBlockingQueue 无参构造容量 Integer.MAX_VALUE，
 * maximumPoolSize 与拒绝策略全部失效，积压只会放大到内存耗尽；</li>
 * <li>P2-34 CopilotThreadExecutor.beforeExecute 每任务一条 INFO 监控日志（高 QPS 下刷屏）
 * 与 shutdownNow 异常分支返回 null；CopilotExecutors 的 javadoc/校验问题；</li>
 * <li>P2-37 FutureResult 把 InterruptedException 与业务异常混在一起处理，
 * 中断标记被丢弃不恢复，线程池的取消信号在这里断链。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ConcurrentP2FixTest {

	/**
	 * 把 ListAppender 挂到指定类的 logback logger 上并放开级别，返回 appender 供断言。
	 * 调用方负责 finally 里 detach/还原。
	 */
	private static ListAppender<ILoggingEvent> attach(Class<?> owner, Level level) {
		ch.qos.logback.classic.Logger log =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(owner);
		log.setLevel(level);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		log.addAppender(appender);
		return appender;
	}

	// ---------------- P2-1 dump 内容必须进日志 ----------------

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testThreadDumpContentReachesLogMessage() throws Exception {
		AbortWithReportPolicy policy = new AbortWithReportPolicy();
		ListAppender<ILoggingEvent> appender = attach(AbortWithReportPolicy.class, Level.DEBUG);
		try {
			//修复前没有 doThreadDump 方法(dump 文本构建藏在 DUMP_EXECUTOR 的 lambda 里)，
			//这里反射获取会抛 NoSuchMethodException —— RED 形态即"方法未提取"；
			//提取出来还顺带让本用例不依赖静态执行器的状态(AbortWithReportPolicyGuardTest
			//的提交失败用例会把 DUMP_EXECUTOR 永久关停)。
			Method m = AbortWithReportPolicy.class.getDeclaredMethod("doThreadDump");
			m.setAccessible(true);
			m.invoke(policy);

			//dump 文本由 getThreadDumpString 生成, 必含 " Id=" 片段; 修复前 formattedMessage
			//只有 "thread dump info:"(占位符缺失导致第二参数被 SLF4J 丢弃), 断言在这里红。
			boolean dumpBodyDelivered = appender.list.stream()
					.filter(e -> e.getLevel() == Level.ERROR)
					.anyMatch(e -> e.getFormattedMessage().contains("thread dump info")
							&& e.getFormattedMessage().contains(" Id="));
			assertThat(dumpBodyDelivered)
					.as("线程 dump 正文必须出现在日志消息里(修复前被 SLF4J 丢掉)")
					.isTrue();
		} finally {
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AbortWithReportPolicy.class))
					.detachAppender(appender);
		}
	}

	// ---------------- P2-2 IO 池必须有界 + 可报警的拒绝策略 ----------------

	@Test
	public void testIoConcentratedPoolBoundedQueueAndReportPolicy() {
		ExecutorService es = Concurrent.ioConcentratedFixedThreadPool();
		ThreadPoolExecutor pool = (ThreadPoolExecutor) es;
		try {
			assertThat(pool.getQueue().remainingCapacity())
					.as("IO 池队列必须有上限(修复前是无界 LinkedBlockingQueue)")
					.isNotEqualTo(Integer.MAX_VALUE);
			assertThat(pool.getQueue().remainingCapacity()).isEqualTo(2600);
			assertThat(pool.getRejectedExecutionHandler())
					.as("队列满时要用 AbortWithReportPolicy 留下现场(修复前是默认 AbortPolicy)")
					.isInstanceOf(AbortWithReportPolicy.class);
		} finally {
			pool.shutdownNow();
		}
	}

	// ---------------- P2-34 CopilotThreadExecutor ----------------

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testPerTaskMonitorLogDowngradedToDebug() throws Exception {
		CopilotThreadExecutor executor = new CopilotThreadExecutor(1, 1, 1L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new CopilotThreadFactory(),
				new ThreadPoolExecutor.AbortPolicy());
		ListAppender<ILoggingEvent> appender = attach(CopilotThreadExecutor.class, Level.DEBUG);
		try {
			int n = 30;
			CountDownLatch done = new CountDownLatch(n);
			for (int i = 0; i < n; i++) {
				executor.execute(done::countDown);
			}
			assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();

			long infoMonitor = appender.list.stream()
					.filter(e -> e.getFormattedMessage().contains("ThreadPool monitor data"))
					.filter(e -> e.getLevel() == Level.INFO)
					.count();
			assertThat(infoMonitor)
					.as("每个任务一条 INFO 监控日志在高 QPS 下会刷屏, 必须降到 DEBUG")
					.isZero();
			long debugMonitor = appender.list.stream()
					.filter(e -> e.getFormattedMessage().contains("ThreadPool monitor data"))
					.filter(e -> e.getLevel() == Level.DEBUG)
					.count();
			assertThat(debugMonitor).as("监控信息仍要能开到 DEBUG 时看到").isPositive();
		} finally {
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CopilotThreadExecutor.class))
					.detachAppender(appender);
			executor.shutdownNow();
		}
	}

	@Test
	public void testShutdownNowNeverReturnsNull() {
		//ThreadPoolExecutor.shutdownNow() 契约是非 null; 修复前 catch 分支返回 null。
		//异常分支在真实执行器上无法稳定构造, 本用例 pin 正常路径 + 关停后重复调用的路径都不为 null。
		CopilotThreadExecutor executor = new CopilotThreadExecutor(1, 1, 1L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new CopilotThreadFactory(),
				new ThreadPoolExecutor.AbortPolicy());
		for (int i = 0; i < 5; i++) {
			executor.execute(() -> {
			});
		}
		List<Runnable> dropped = executor.shutdownNow();
		assertThat(dropped).as("shutdownNow 必须返回非 null 列表").isNotNull();
		assertThat(executor.shutdownNow()).isNotNull();
	}

	@Test
	public void testCopilotExecutorsRejectsNullMaxPoolSizeAtSetter() {
		//修复前 maxPoolSize(null) 当时不报错, 到 build() 拆箱才抛无主语 NPE——犯错点与报错点隔整条链
		CopilotExecutors builders = CopilotExecutors.of("p2-34");
		assertThatThrownBy(() -> builders.maxPoolSize(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("maximumPoolSize");
	}

	@Test
	public void testCopilotExecutorsBuildRejectsCoreGreaterThanMaxWithMessage() {
		CopilotExecutors b = CopilotExecutors.of("p2-34")
				.maxPoolSizeToCorePoolSize()   // max = 当时的 core
				.corePoolSize(64)              // 链式顺序颠倒: core 反超 max
				.queueSize(10);
		assertThatThrownBy(b::build)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("corePoolSize");
		//修复前这里也抛 IAE, 但 message 为 null(JDK 构造器内部抛出), hasMessageContaining 先红后绿
	}

	// ---------------- P2-37 FutureResult 中断信号 ----------------

	@Test
	public void testGetRestoresInterruptFlag() {
		CompletableFuture<String> future = new CompletableFuture();   // 永不完成
		FutureResult<String> fr = new FutureResult<>(future);
		Thread.currentThread().interrupt();
		try {
			assertThatThrownBy(fr::get).isInstanceOf(AsyncExecutionException.class);
			assertThat(Thread.currentThread().isInterrupted())
					.as("InterruptedException 被包装前必须恢复中断标记, 否则上层线程池的取消信号断链")
					.isTrue();
		} finally {
			Thread.interrupted(); //清掉, 不污染后续用例
		}
	}

	@Test
	public void testGetStillWrapsCancellationException() {
		//契约(pin 既有语义): FutureResult 配合 Concurrent.submit 使用, 调用方 catch
		//AsyncExecutionException 要兜住一切失败场景——future 被 cancel() 后 get() 抛的
		//CancellationException(运行时异常)也走统一包装, P2-37 重构不能放宽这个口径
		CompletableFuture<String> future = new CompletableFuture();
		FutureResult<String> fr = new FutureResult<>(future);
		future.cancel(true);
		assertThatThrownBy(fr::get)
				.isInstanceOf(AsyncExecutionException.class)
				.hasCauseInstanceOf(java.util.concurrent.CancellationException.class);
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testSubmitThenGetRoundTripThroughConcurrent() {
		//端到端: FutureResult 的实际用法是包在 Concurrent.submit 后面, P2-37 改动后
		//正常路径(取值)与失败路径(异常包成 AsyncExecutionException 且原始异常在因果链里)都要照旧
		FutureResult<String> ok = Concurrent.submit(() -> "value-42");
		assertThat(ok.get()).isEqualTo("value-42");

		FutureResult<String> boom = Concurrent.submit(() -> {
			throw new IllegalStateException("task exploded intentionally");
		});
		assertThatThrownBy(boom::get)
				.isInstanceOf(AsyncExecutionException.class)
				.rootCause().isInstanceOf(IllegalStateException.class)
				.hasMessage("task exploded intentionally");
		Concurrent.await(); //清掉 ThreadLocal 里登记的两个 future, 不污染其它用例
		System.out.println("...");
		String result = ok.get();
		assertThat(result).isEqualTo("value-42");
	}

	@Test
	public void testOrElseGetKeepsFallbackButRestoresInterruptFlag() {
		CompletableFuture<String> future = new CompletableFuture();
		FutureResult<String> fr = new FutureResult<>(future);
		Thread.currentThread().interrupt();
		try {
			assertThat(fr.orElseGet("fallback")).isEqualTo("fallback");
			assertThat(Thread.currentThread().isInterrupted())
					.as("走回落分支也要保留中断标记, 让调用方有机会感知")
					.isTrue();
		} finally {
			Thread.interrupted();
		}
	}
}
