package com.awesomecopilot.common.lang.concurrent;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;
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
	
	@SneakyThrows
	@Test
	public void testCreateThreadPool() {
		ThreadPoolExecutor executor = CopilotExecutors.of("Copilot-Pool")
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
	
	@SneakyThrows
	@Test
	public void testTaskCorePoolAndQueue() {
		ThreadPoolExecutor executor = CopilotExecutors.of("屌丝Pool")
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
	
	@SneakyThrows
	@Test
	public void testTaskCorePoolAndQueue2() {
		ThreadPoolExecutor executor = CopilotExecutors.of("屌丝Pool")
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
