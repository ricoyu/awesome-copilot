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

        // 等待限流窗口重置。注意：canPass 判断过期用的是"距今严格大于窗口长度"，
        // 而 currentTimeMillis 按毫秒取整——只睡 1000ms 时差值可能恰好等于 1000，
        // 旧记录不算过期、配额没释放，全量套件里实测会随机失败。多等 100ms 留边界余量。
        Thread.sleep(1100);
        assertTrue(slidingWindow.canPass());
    }
}