package com.awesomecopilot.common.lang.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlidingWindowTest {

    @Test
    public void testBasicRateLimiting() throws InterruptedException {
        // 限流配置：1秒内最多允许 5 个请求，分成 5 个子窗口（每个子窗口 200ms）
        SlidingWindow slidingWindow = new SlidingWindow(1L, TimeUnit.SECONDS, 5);

        // 模拟 10 个请求，应该只有前 5 个通过
        for (int i = 0; i < 10; i++) {
            if (i < 5) {
                assertTrue(slidingWindow.canPass());
            } else {
                assertFalse(slidingWindow.canPass());
            }
        }

        // 等待 1 秒后，限流窗口应重置，允许新的请求
        Thread.sleep(1000);
        assertTrue(slidingWindow.canPass());
    }
}