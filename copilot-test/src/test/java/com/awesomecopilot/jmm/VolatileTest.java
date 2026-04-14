package com.awesomecopilot.jmm;

public class VolatileTest {
    //  volatile 修饰int变量
    private static volatile int i = 0;

    public static void main(String[] args) throws InterruptedException {
        // 线程1：累加100次
        Thread t1 = new Thread(() -> {
            for (int j = 0; j < 100; j++) {
                i++;
            }
        });

        // 线程2：累加100次
        Thread t2 = new Thread(() -> {
            for (int j = 0; j < 100; j++) {
                i++;
            }
        });

        t1.start();
        t2.start();
        // 等待两个线程执行完毕
        t1.join();
        t2.join();

        // 输出结果：绝大概率不是200
        System.out.println("最终i的值：" + i);
    }
}