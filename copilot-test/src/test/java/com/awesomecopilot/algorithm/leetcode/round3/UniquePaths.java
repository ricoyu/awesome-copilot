package com.awesomecopilot.algorithm.leetcode.round3;

import java.util.Scanner;

/**
 * 不同路径
 * <p>
 * 一个机器人位于一个 m x n 网格的左上角 （起始点在下图中标记为 “Start” ）。<br/>
 * 机器人每次只能向下或者向右移动一步。机器人试图达到网格的右下角（在下图中标记为 “Finish” ）。<br/>
 * <image src="../images/uniquepaths.png" />
 * 问总共有多少条不同的路径？
 * <p>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：m = 3, n = 7
 * 输出：28
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：m = 3, n = 2
 * 输出：3
 * </pre>
 * 解释：
 * 从左上角开始，总共有 3 条路径可以到达右下角。
 * 1. 向右 -> 向下 -> 向下
 * 2. 向下 -> 向下 -> 向右
 * 3. 向下 -> 向右 -> 向下
 *
 * <pre>
 * 示例 3：
 *
 * 输入：m = 7, n = 3
 * 输出：28
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：m = 3, n = 3
 * 输出：6
 * </pre>
 *
 * 提示：
 *
 * 1 <= m, n <= 100
 * 题目数据保证答案小于等于 2 * 109
 * <p/>
 * Copyright: Copyright (c) 2025-12-02 9:08
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class UniquePaths {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入m: ");
		int m = scanner.nextInt();
		System.out.print("请输入n: ");
		int n = scanner.nextInt();
		System.out.println(uniquePaths(m, n));
	}

	private static int uniquePaths(int m, int n) {
		// 优化空间：用一维数组代替二维数组，节省空间复杂度至O(n)
		int[] dp = new int[n];

		// 初始化第一行：到达第一行任意位置的路径数都是1（只能从左向右走）
		for (int j = 0; j < n; j++) {
		    dp[j] = 1;
		}

		// 从第二行开始遍历每一行（i从1到m-1）
		for (int i = 1; i < m; i++) {
			// 从第二列开始遍历每一列（j从1到n-1）
			for (int j = 1; j < n; j++) {
				// dp[j] = 上方路径数（原dp[j]） + 左方路径数（dp[j-1]）
				dp[j] = dp[j] + dp[j - 1];
			}
		}
		// 最后一个元素即为到达右下角的路径数
		return dp[n-1];
	}
}
