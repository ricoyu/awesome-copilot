package com.awesomecopilot.algorithm.leetcode.round4;

import java.util.Scanner;

/**
 * 爬楼梯 <p>
 * <p>
 * 假设你正在爬楼梯。需要 n 阶你才能到达楼顶。 <p>
 * <p>
 * 每次你可以爬 1 或 2 个台阶。你有多少种不同的方法可以爬到楼顶呢？ <p>
 * <p>
 * 示例 1：
 * <p>
 * 输入：n = 2 <br/>
 * 输出：2 <p>
 * 解释：有两种方法可以爬到楼顶。 <p>
 * 1 阶 + 1 阶 <br/>
 * 2 阶 <p>
 * 示例 2：
 * <br/>
 * 输入：n = 3 <br/>
 * 输出：3 <p>
 * 解释：有三种方法可以爬到楼顶。 <p>
 * 1 阶 + 1 阶 + 1 阶 <br/>
 * 1 阶 + 2 阶 <br/>
 * 2 阶 + 1 阶  <br/>
 * <p/>
 * Copyright: Copyright (c) 2025-07-31 9:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ClimbStairs {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入阶数: ");
		int n = scanner.nextInt();
		System.out.println(climb(n));
		scanner.close();
	}

	private static int climb(int n) {
		if (n ==1) {
			return 1;
		}
		if (n ==2) {
			return 2;
		}

		int[] dp = new int[n+1];
		dp[1] = 1;
		dp[2] = 2;
		for (int i = 3; i <= n; i++) {
		    dp[i] = dp[i-1] + dp[i-2];
		}

		return dp[n];
	}
}
