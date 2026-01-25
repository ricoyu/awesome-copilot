package com.awesomecopilot.juc;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreBasicExample {
    // 初始化信号量，设置 5 个许可（5 个停车位），公平模式（按等待顺序获取许可）
    private static final Semaphore PARKING_SEMAPHORE = new Semaphore(5, true);

    public static void main(String[] args) {
        // 模拟 10 辆车要停车
        for (int i = 1; i <= 10; i++) {
            int carId = i;
            new Thread(() -> {
                try {
                    System.out.println("车辆" + carId + "准备进入停车场...");
                    // 获取许可（停车），如果没有可用许可则等待
                    PARKING_SEMAPHORE.acquire();
                    System.out.println("车辆" + carId + "成功停车，占用1个停车位");
                    
                    // 模拟停车时长（1-3 秒）
                    TimeUnit.SECONDS.sleep((long) (Math.random() * 3 + 1));
                    
                    System.out.println("车辆" + carId + "离开停车场，释放停车位");
                    // 释放许可（离开）
                    PARKING_SEMAPHORE.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("车辆" + carId + "停车过程被中断");
                }
            }, "Car-" + i).start();
        }
    }
}