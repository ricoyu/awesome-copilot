package com.awesomecopilot.atomic;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Copyright: (C), 2019/11/22 9:05
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class AtomicIntegerTest {
	
	private static AtomicInteger atomicInteger = new AtomicInteger();
	
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				atomicInteger.incrementAndGet();
			}).start();
		}
		
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("自加10次数值：--->" + atomicInteger.get());
	}
	
	@Test
	public void test1() {
		Integer i = new Integer(127);
		Integer j = new Integer(127);
		System.out.println(i == j); //false
		Integer num = Integer.valueOf(127);
		Integer num2 = Integer.valueOf(127);
		System.out.println(num == num2); //true
		System.out.println(i == 127); //true
	}
}