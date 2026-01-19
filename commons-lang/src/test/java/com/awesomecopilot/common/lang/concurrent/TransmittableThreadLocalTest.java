package com.awesomecopilot.common.lang.concurrent;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransmittableThreadLocalTest {

	static ThreadLocal tl = new ThreadLocal(); //单个线程之间的数据传递

	static InheritableThreadLocal itl = new InheritableThreadLocal(); //父子线程之间的数据传递

	static TransmittableThreadLocal ttl = new TransmittableThreadLocal();

	public static void main(String[] args) {
		method1();
	}


	private static void method1() {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		new Thread(() -> {
			executor.execute(() -> {
				System.out.println("初始化线程池里的工作线程");
			});
		}).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		//tl.set("tl");
		//itl.set("itl");
		ttl.set("ttl");

		System.out.println("当前线程 " + ttl.get());

		//子线程
		Thread t = new Thread(() -> {
			System.out.println("子线程 " + ttl.get());
		});
		t.start();

		//线程池线程
		executor.execute(() -> {
			//第一个工作线程是main线程创建的, 存在父子关系, 所以可以拿到itl中的数据
			System.out.println("线程池线程1 " + ttl.get());
		});

		//线程池线程
		executor.execute(() -> {
			//这样拿不到ttl中的数据
			System.out.println("线程池线程2 " + ttl.get());
		});

		//线程池线程
		executor.execute(TtlRunnable.get(() -> {
			//用TtlRunnable包装一层就能拿到ttl中的数据
			System.out.println("线程池线程3 " + ttl.get());
		}));

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
