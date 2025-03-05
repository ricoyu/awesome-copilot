package com.copilot.common.lang.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * <p>
 * Copyright: (C), 2020-12-07 14:08
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class LoserThreadExecutorTest {
	
	public static void main(String[] args) {
		LoserThreadExecutor executor = new LoserThreadExecutor(2, 3, 1, TimeUnit.MINUTES);
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
	
	@Test
	public void test() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					
				}
			}
		});
		boolean alive = t.isAlive();
		System.out.println(alive);
		t.start();
		System.out.println(t.isAlive());
	}
}
