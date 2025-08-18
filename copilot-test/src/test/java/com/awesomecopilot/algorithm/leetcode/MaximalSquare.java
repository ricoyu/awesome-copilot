package com.awesomecopilot.algorithm.leetcode;

/**
 * 最大正方形
 * <p>
 * 在一个由 '0' 和 '1' 组成的二维矩阵内，找到只包含 '1' 的最大正方形，并返回其面积。
 * <img src="max1grid.jpg" alt="最大正方形算法示意图" width="400">
 * <p>
 * 示例 1：
 *  <p>
 * 输入：matrix = [["1","0","1","0","0"],["1","0","1","1","1"],["1","1","1","1","1"],["1","0","0","1","0"]] <br/>
 * 输出：4
 * <p>
 * 示例 2：
 *  <p>
 * 输入：matrix = [["0","1"],["1","0"]] <br/>
 * 输出：1
 * <p>
 * 示例 3：
 * <p>
 * 输入：matrix = [["0"]] <br/>
 * 输出：0
 * <p>
 * 解题思路: <br/>
 * 这个问题可以通过动态规划来解决。思路是建立一个二维的动态规划数组 dp，其中 dp[i][j] 表示以 (i, j) 为右下角的只包含 '1' 的最大正方形的边长。
 * 通过填充这个 dp 数组，可以找出所有可能的最大正方形，并计算出最大的面积。
 *  <p>
 * <ul>
 *     <li/>定义dp[i][j]表示以matrix[i][j]为右下角的最大正方形的边长
 *     <li/>状态转移方程：dp[i][j] = min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1（当matrix[i][j] = '1'时）
 *     <li/>边界条件：第一行和第一列的dp值与原矩阵值相同
 *     <li/>
 * </ul>
 * <p>
 * Copyright: Copyright (c) 2024-10-27 15:10
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaximalSquare {

	public static int maximalSquare(char[][] matrix) {
		if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
			return 0;
		}

		int rows = matrix.length, cols = matrix[0].length;
		int maxSide = 0;

		// 创建dp数组，dp[i][j]表示以matrix[i][j]为右下角的最大正方形的边长
		int[][] dp = new int[rows][cols];

		// 遍历每一个单元格，计算dp值
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (matrix[i][j] == '1') {
					/*
					 * if (i == 0 || j == 0)：这个条件判断当前元素是否在矩阵的边界（第一行或第一列）
					 * dp[i][j] = 1：如果当前元素在边界上，且该元素的值为1，那么以它为右下角的最大正方形边长就是 1。
					 */
					if (i == 0 || j == 0) {
						dp[i][j] = 1; // 在边界上，最大正方形就是它自己
					} else {
						// 状态转移方程：当前位置的最大边长 = 左上角、上方、左方三个位置的最小值 + 1
						// 这是因为正方形需要三个方向都能形成同样大小的正方形才能扩展
						dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1])+1;
					}

					// 更新最大边长
					maxSide = Math.max(maxSide, dp[i][j]);
				}
			}
		}

		return maxSide * maxSide;
	}
}
