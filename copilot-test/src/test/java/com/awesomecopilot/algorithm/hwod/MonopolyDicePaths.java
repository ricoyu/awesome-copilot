package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 大富翁
 * <p>
 * 一、问题描述
 * <p>
 * 玩家根据骰子的点数决定走的步数，即骰子点数为1时可以走一步，点数为2时可以走两步，点数为n时可以走n步。求玩家走到第n步（n小于等于骰子最大点数且投骰子方法唯一）时总共有多少种投骰子的方法。
 * <p>
 * 二、输入描述
 * <p>
 * 输入一个1至6之间的整数
 * <p>
 * 三、输出描述
 * <p>
 * 输出一个整数，表示投骰子的方法数例如，输入为6时，输出为32.
 * <p>
 * 设 dp[i] 表示走到第 i 步的方法总数。 <p>
 * 玩家每次可以投出点数 1~6，即可以从 i-1、i-2……i-6 这些位置跳到 i。 <p>
 * 所以状态转移方程为：dp[i] = dp[i-1] + dp[i-2] + ... + dp[i-6]（前提是i-1~i-6 ≥ 0） <p>
 * 初始状态：dp[0] = 1，表示从第0步出发，有1种方式（即不动）
 * <p/>
 * Copyright: Copyright (c) 2025-07-04 10:24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MonopolyDicePaths {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入目标步数: ");
		int n = scanner.nextInt();

		// dp[i]表示走到第i步的方法数
		int[] dp = new int[n+1];

		// 初始条件：走到第0步只有1种方式（起点）
		dp[0] = 1;

		// 逐步计算从1步到n步的方法数
		for (int i = 1; i <= n; i++) {
			// 从前6步中跳跃一步到当前步（前提是步数不能小于0）
			for (int j = 1; j <= 6; j++) {
			    if (i-j>=0) {
					dp[i] += dp[i-j];
			    }
			}
		}

		System.out.println(dp[n]);
	}
}
