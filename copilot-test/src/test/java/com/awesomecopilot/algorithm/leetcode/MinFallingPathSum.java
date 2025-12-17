package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 下降路径最小和
 * <p>
 * <image src="images/MinFallingPathSum.png" />
 * <p>
 * 给你一个 n x n 的方形整数数组 matrix ，请你找出并返回通过 matrix 的下降路径的最小和 。
 * <p>
 * 下降路径可以从第一行中的任何元素开始，并从每一行中选择一个元素。在下一行选择的元素和当前行所选元素最多相隔一列（即位于正下方或者沿对角线向左或者向右的第一个元素）。
 * 具体来说，位置 (row, col) 的下一个元素应当是 (row + 1, col - 1)、(row + 1, col) 或者 (row + 1, col + 1) 。
 * <p>
 * 示例 1：<br/>
 * 输入：matrix = [[2,1,3],[6,5,4],[7,8,9]] <br/>
 * 输出：13 <br/>
 * <p>
 * <p>
 * 示例 2： <br/>
 * 输入：matrix = [[-19,57],[-40,-5]] <br/>
 * 输出：-59 <br/>
 *
 * <ol>解题思路:
 *     <li/>初始化状态: 我们将一个和输入相同尺寸的dp数组用于存储到当前元素为止的最小下降路径和。
 *     <li/>从矩阵第一行开始，每一行的每个元素的最小下降路径和等于其自身加上上一行中可到达它的三个位置（正上方、左上方、右上方）的最小路径和
 *     <li/>边界处理：第一列元素只能从正上方或右上方到达，最后一列元素只能从正上方或左上方到达
 *     <li/>状态转移: 从倒数第二行开始向上计算每个元素的最小下降路径和。对于dp数组中的每个元素dp[i][j]，其值由下一行的三个可能位置的元素决定（注意边界条件）：
 *     <li/>dp[i+1][j-1]（左下角元素，如果存在的话）
 *     <li/>dp[i+1][j]（正下方元素）
 *     <li/>dp[i+1][j+1]（右下角元素，如果存在的话）
 *     <li/>对于每个元素，我们将它的值更新为它自己的值加上上述三个元素中的最小值。
 *     <li/>最终结果为最后一行所有元素的最小路径和中的最小值
 * </ol>
 * <p>
 * Copyright: Copyright (c) 2024-10-27 12:08
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MinFallingPathSum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入矩阵行列数: ");
		int n = scanner.nextInt();
		scanner.nextLine();
		int[][] matrix = new int[n][n];
		for (int i = 0; i < n; i++) {
			System.out.print("请输入第" + (i + 1) + "行元素: ");
			String input = scanner.nextLine().trim();
			String[] parts = input.split(",");
			int[] row = new int[n];
			for (int j = 0; j < n; j++) {
				row[j] = Integer.parseInt(parts[j].trim());
				matrix[i] = row;
			}
		}

		System.out.println(minFallingPathSum(matrix));
	}

	public static int minFallingPathSum(int[][] matrix) {
		int n = matrix.length;

		// 创建dp数组并初始化最后一行
		int[][] dp = new int[n][n];
		for (int i = 0; i < n; i++) {
			dp[n - 1][i] = matrix[n - 1][i];
		}

		// 从倒数第二行开始向上计算每个元素的最小下降路径和
		for (int i = n - 2; i >= 0; i--) {
			for (int j = 0; j < n; j++) {
				// 正下方元素
				int min = dp[i + 1][j];
				// 左下方元素
				if (j > 0) {
					min = Math.min(min, dp[i + 1][j - 1]);
				}
				// 右下方元素
				if (j < n - 1) {
					min = Math.min(min, dp[i + 1][j + 1]);
				}
				// 更新当前位置的最小路径和
				dp[i][j] = matrix[i][j] + min;
			}
		}

		// 在dp数组的第一行找到最小值
		int minSum = Integer.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			minSum = Math.min(minSum, dp[0][i]);
		}

		return minSum;
	}
}
