package com.awesomecopilot.algorithm.leetcode;

/**
 * 完全平方数
 * <p/>
 * 给你一个整数 n ，返回 和为 n 的完全平方数的最少数量 。<p/>
 * 完全平方数 是一个整数，其值等于另一个整数的平方；换句话说，其值等于一个整数自乘的积。例如，1、4、9 和 16 都是完全平方数，而 3 和 11 不是。
 * <p>
 * 这个问题可以使用动态规划来解决。我们需要找到和为 n 的完全平方数的最少数量。我们可以定义一个数组 dp，其中 dp[i] 表示和为 i 的完全平方数的最少数量。
 *
 * <ul>我们可以通过以下方式填充 dp 数组：
 *     <li/>初始化 dp[0] = 0，因为和为 0 的完全平方数的数量是 0。
 *     <li/>对于每个 i 从 1 到 n，我们遍历所有小于等于 i 的完全平方数 j*j，然后更新 dp[i] 为 dp[i - j*j] + 1 的最小值。
 * </ul>
 * <p>
 * 动态规划方程可以表示为：dp[i]=min(dp[i],dp[i−j∗j]+1) <p/>
 * <p>
 * 示例 1：
 * <p/>
 * 输入：n = 12 <br/>
 * 输出：3 <br/>
 * 解释：12 = 4 + 4 + 4
 * <p/>
 * 示例 2：
 * <p/>
 * 输入：n = 13 <br/>
 * 输出：2 <br/>
 * 解释：13 = 4 + 9
 * <p/>
 * Copyright: Copyright (c) 2025-03-18 9:25
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PerfectSquares {

	public int numSquares(int n) {
		// 创建一个数组 dp，其中 dp[i] 表示和为 i 的完全平方数的最少数量
		int[] dp = new int[n + 1];

		// 初始化 dp 数组，初始值为最大值
		for (int i = 0; i <= n; i++) {
			dp[i] = Integer.MAX_VALUE;
		}
		// 初始化 dp[0] = 0，因为和为 0 的完全平方数的数量是 0
		dp[0] = 0;

		// 遍历从 1 到 n 的所有数字
		for (int i = 1; i <= n; i++) {
			// 遍历所有小于等于 i 的完全平方数
			for (int j = 1; j * j <= i; j++) {
				// 更新 dp 数组
				dp[i] = Math.min(dp[i], dp[i - j * j] + 1);
			}
		}

		// 返回 dp[n]，即和为 n 的完全平方数的最少数量
		return dp[n];
	}

	public static void main(String[] args) {
		PerfectSquares solution = new PerfectSquares();
		System.out.println(solution.numSquares(12)); // 输出: 3
		System.out.println(solution.numSquares(13)); // 输出: 2
	}
}
