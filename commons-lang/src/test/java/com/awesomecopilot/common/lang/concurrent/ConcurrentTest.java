package com.awesomecopilot.common.lang.concurrent;

import com.awesomecopilot.common.lang.context.ThreadContext;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Copyright: (C), 2021-05-18 11:13
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ConcurrentTest {
	
	public static void main(String[] args) {
		ThreadContext.put("main", "来自主线程的问候");
		Concurrent.execute(() -> {
			System.out.println("第一个任务开始执行...");
			try {
				TimeUnit.SECONDS.sleep(3);
				System.out.println("第一个任务收到: " + (String) ThreadContext.get("main"));

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("第一个任务执行完成...");
		});
		Concurrent.execute(() -> {
			System.out.println("第二个任务开始执行...");
				System.out.println("第二个任务收到: " + (String) ThreadContext.get("main"));
			if (true) {
				throw new RuntimeException("第二个任务执行失败");
			}
			System.out.println("第二个任务执行完成...");
		});
		// 给线程池一点启动时间（只为测试，生产环境不要加）
		try {
			Thread.sleep(100);  // 或 200
		} catch (InterruptedException ignored) {}
		Concurrent.await();
		System.out.println("所有任务执行完成");
	}
	
}
