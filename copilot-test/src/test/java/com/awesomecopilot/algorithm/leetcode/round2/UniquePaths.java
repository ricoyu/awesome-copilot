package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 不同路径
 * <p/>
 * 一个机器人位于一个 m x n 网格的左上角 （起始点在下图中标记为 “Start” ）。
 * <p/>
 * 机器人每次只能向下或者向右移动一步。机器人试图达到网格的右下角（在下图中标记为 “Finish” ）。
 * <p/>
 * 问总共有多少条不同的路径？
 * <p/>
 * 示例 1：<br/>
 * 输入：m = 3, n = 7 <br/>
 * 输出：28
 * <p>
 * 示例 2： <br/>
 * 输入：m = 3, n = 2 <br/>
 * 输出：3 <p/>
 * <p>
 * 解释： <br/>
 * 从左上角开始，总共有 3 条路径可以到达右下角。 <br/>
 * 1. 向右 -> 向下 -> 向下 <br/>
 * 2. 向下 -> 向下 -> 向右 <br/>
 * 3. 向下 -> 向右 -> 向下 <br/>
 * <p/>
 * 示例 3： <br/>
 * 输入：m = 7, n = 3 <br/>
 * 输出：28 <p/>
 * <p>
 * 示例 4： <br/>
 * 输入：m = 3, n = 3 <br/>
 * 输出：6
 * <p/>
 * 这个问题可以通过动态规划的方法来解决。我们可以定义一个二维数组 dp，其中 dp[i][j] 表示到达网格中的第 i 行第 j 列的不同路径数量。
 * 从起点（左上角）到任意点 (i, j) 的路径数量可以从其上方的格子 (i-1, j) 和左侧的格子 (i, j-1) 获得，因为机器人只能从这两个方向来到 (i, j)。
 * <ol>初始化：
 *     <li/>第一行的所有格子只能从左边一直向右移动，因此路径数都为1。
 *     <li/>第一列的所有格子只能从上边一直向下移动，因此路径数也都为1。
 * </ol>
 * 状态转移方程：对于非边缘的格子，dp[i][j] = dp[i-1][j] + dp[i][j-1]。 <p/>
 * 最后，dp[m-1][n-1] 就是从起点到终点的不同路径总数。
 * <p/>
 * Copyright: Copyright (c) 2025-08-03 8:25
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class UniquePaths {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入行列: ");
		int[] arr = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		int m = arr[0];
		int n = arr[1];

		int[][] dp = new int[m][n];
		for (int i = 0; i < m; i++) {
			dp[i][0] = 1;
		}
		for (int i = 0; i < n; i++) {
			dp[0][i] = 1;
		}

		for (int i = 1; i < m; i++) {
		    for (int j = 1; j < n; j++) {
		        dp[i][j] = dp[i-1][j] + dp[i][j-1];
		    }
		}

		System.out.println(dp[m-1][n-1]);
	}
}
