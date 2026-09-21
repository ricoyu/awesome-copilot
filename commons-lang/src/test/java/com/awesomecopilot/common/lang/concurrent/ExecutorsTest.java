package com.awesomecopilot.common.lang.concurrent;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.awesomecopilot.common.lang.concurrent.Policy.ABORT_WITH_REPORT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * <p>
 * Copyright: (C), 2021-07-10 12:35
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class ExecutorsTest {

	/**
	 * 手工演示线程池参数效果, 不标 @Test: 方法结尾用 Thread.currentThread().join()
	 * 让当前线程一直等待, 方便在 IDE 里边看日志边观察; mvn test 若执行该方法会永远不结束,
	 * 所以只保留 @SneakyThrows, 由人在 IDE 里手动运行. 需要自动化断言的并发测试见 VolatileIncrementTest.
	 */
	@SneakyThrows
	public void testCreateThreadPool() {
		ExecutorService executor = CopilotExecutors.of("Copilot-Pool")
				.rejectPolicy(ABORT_WITH_REPORT)
				.maxPoolSize(500)
				.keepAliveTime(1, TimeUnit.MINUTES)
				.queueSize(200)
				.build();

		executor.execute(() -> {
			int i = 0;
			while (i++ < 100) {
				log.info(i+"");
			}
		});

		Thread.currentThread().join();
	}

	/**
	 * 手工演示核心线程数+队列满了之后的表现, 不标 @Test(原因同 testCreateThreadPool:
	 * 结尾 Thread.currentThread().join() + 任务里 while(true) 不会自己结束)
	 */
	@SneakyThrows
	public void testTaskCorePoolAndQueue() {
		ExecutorService executor = CopilotExecutors.of("屌丝Pool")
				.corePoolSize(1)
				.allowCoreThreadTimeout(false)
				.queueSize(1)
				.maxPoolSize(2)
				.keepAliveTime(4, SECONDS)
				.rejectPolicy(ABORT_WITH_REPORT)
				.build();

		executor.execute(() -> {
			int i = 1;
			while (true) {
				log.info("线程{}执行第一个任务 {}", Thread.currentThread().getId(), i++);
				try {
					MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		});

		SECONDS.sleep(1);

		executor.execute(() -> {
			int i = 1;
			while (true) {
				log.info("线程{}执行第二个任务 {}",Thread.currentThread().getId(),  i++);
				try {
					MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		});

		Thread.currentThread().join();
	}

	/**
	 * 手工演示 prestartAllCoreThreads 的效果, 不标 @Test(原因同 testCreateThreadPool)
	 */
	@SneakyThrows
	public void testTaskCorePoolAndQueue2() {
		ExecutorService executor = CopilotExecutors.of("屌丝Pool")
				.corePoolSize(1)
				.allowCoreThreadTimeout(false)
				.queueSize(1)
				.maxPoolSize(2)
				.keepAliveTime(4, SECONDS)
				.rejectPolicy(ABORT_WITH_REPORT)
				.prestartAllCoreThreads()
				.build();

		executor.execute(() -> {
			int i = 1;
			while (true) {
				log.info("线程{}执行第{}次任务", Thread.currentThread().getId(), i++);
				try {
					MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		});

		SECONDS.sleep(1);

		executor.execute(() -> {
			int i = 1;
			while (true) {
				log.info("线程{}执行第{}次任务",Thread.currentThread().getId(),  i++);
				try {
					MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		});

		SECONDS.sleep(1);

		executor.execute(() -> {
			int i = 1;
			while (true) {
				log.info("线程{}执行第{}次任务",Thread.currentThread().getId(),  i++);
				try {
					MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		});

		Thread.currentThread().join();
	}
}
