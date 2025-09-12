package com.awesomecopilot.algorithm.dafuweng;

import java.util.Random;

public class Dice {
    private static final Random random = new Random();

    // 生成1到max之间的随机数
    public static int roll(int max) {
        if (max < 1 || max > 6) {
            throw new IllegalArgumentException("骰子最大值必须在1到6之间");
        }
        return random.nextInt(max) + 1;
    }
}
