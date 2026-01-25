package com.awesomecopilot.juc;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreTryAcquireExample {
	// 2 个许可的资源（比如 2 台打印机）
	private static final Semaphore PRINTER_SEMAPHORE = new Semaphore(2);

	public static void main(String[] args) {
		for (int i = 1; i <= 5; i++) {
			int taskId = i;
			new Thread(() -> {
				try {
					// 尝试在 1 秒内获取许可，超时则放弃
					boolean acquired = PRINTER_SEMAPHORE.tryAcquire(1, TimeUnit.SECONDS);
					if (acquired) {
						System.out.println("任务" + taskId + "获取到打印机，开始打印...");
						TimeUnit.SECONDS.sleep(2);
						System.out.println("任务" + taskId + "打印完成，释放打印机");
						PRINTER_SEMAPHORE.release();
					} else {
						System.out.println("任务" + taskId + "等待 1 秒后仍未获取到打印机，放弃打印");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.out.println("任务" + taskId + "被中断");
				}
			}).start();
		}
	}
}