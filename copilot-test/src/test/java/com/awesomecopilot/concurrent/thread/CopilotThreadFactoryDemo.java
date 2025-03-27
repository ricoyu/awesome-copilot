package com.awesomecopilot.concurrent.thread;

/**
 * <p>
 * Copyright: (C), 2020-09-30 14:24
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class CopilotThreadFactoryDemo {
	
	public static void main(String[] args) {
		CopilotThreadFactory threadFactory = new CopilotThreadFactory("copilot");
		for (int i = 0; i < 10; i++) {
			Thread t = threadFactory.newThread(() -> System.out.println("hello" + Thread.currentThread().getName()));
			t.start();
		}
	}
}
