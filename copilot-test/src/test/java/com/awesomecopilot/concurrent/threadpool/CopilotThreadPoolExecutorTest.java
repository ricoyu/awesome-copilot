package com.awesomecopilot.concurrent.threadpool;

import com.awesomecopilot.concurrent.blockingQueue.CopilotArrayBlockingQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * <p>
 * Copyright: (C), 2021-07-03 9:58
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class CopilotThreadPoolExecutorTest {
	
	public static void main(String[] args) {
		CopilotThreadPoolExecutor executor = new CopilotThreadPoolExecutor(8, 1000, 10, TimeUnit.SECONDS, new CopilotArrayBlockingQueue<>(100));
		executor.execute(() -> {
			while (true) {
				log.info("...执行任务中");
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
			}
		});
		executor.execute(() -> {
			while (true) {
				log.info("...执行任务中");
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
			}
		});
		executor.execute(() -> {
			while (true) {
				log.info("...执行任务中");
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
			}
		});
		executor.execute(() -> {
			while (true) {
				log.info("...执行任务中");
				LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
			}
		});
	}
}
