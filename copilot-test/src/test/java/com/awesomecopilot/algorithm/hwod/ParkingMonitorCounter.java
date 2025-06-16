package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 需要打开多少监控器
 * <p>
 * 一、题目描述
 * <p>
 * 在一长方形停车场内，每个车位上方都有对应监控器，当且仅当在当前车位或者前后左右四个方向任意一个车位范围停车时，监控器才需要打开。给出某一时刻停车场的停车分布，请统计最少需要打开多少个监控器。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入 m，n 表示长宽，满足 1 < m, n <= 20；
 * <p>
 * 后面输入 m 行，每行有 n 个 0 或 1 的整数，整数间使用一个空格隔开，表示该行停车情况，其中 0 表示空位，1 表示已停。
 * <p>
 * 三、输出描述
 * <p>
 * 最少需要打开监控器的数量。
 * <pre>
 * 1、输入
 * 3 3
 * 0 0 0
 * 0 1 0
 * 0 0 0
 *
 * 2、输出
 * 5
 * </pre>
 *
 * <ul>解题思路
 *     <li/>遍历整个二维停车场矩阵。
 *     <li/>记录每一个为 1 的格子。
 *     <li/>对于每一个 1，把它自己和上下左右五个位置对应的监控器标记为“需要开启”。
 *     <li/>最后统计一共需要开启多少个监控器（不重复统计）
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-08 7:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ParkingMonitorCounter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入长宽:");
		String input = scanner.nextLine().trim();
		String[] parts = input.split(" ");
		int m = Integer.parseInt(parts[0]);
		int n = Integer.parseInt(parts[1]);

		int[][] parking = new int[m][n];
		for (int i = 0; i < m; i++) {
			System.out.print("请输入第 " + (i + 1) + " 行数据:");
			input = scanner.nextLine().trim();
			parts = input.split(" ");
			for (int j = 0; j < n; j++) {
				parking[i][j] = Integer.parseInt(parts[j]);
			}
		}

		// 用来标记哪些监控器需要开启
		boolean[][] monitorOn = new boolean[m][n];
		// 四个方向：上、下、左、右
		int[][] directions = new int[][]{
				{0, 0}, // 自己
				{-1, 0},// 上
				{1, 0}, // 下
				{0, -1}, // 左
				{0, 1}};// 右

		// 遍历停车场，遇到1就激活自己和相邻位置的监控器
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				if (parking[i][j] == 1) {
					for (int[] direction : directions) {
						int x = i + direction[0];
						int y = j + direction[1];
						// 判断坐标是否在停车场范围内
						if (x >= 0 && x < m && y >= 0 && y < n) {
							monitorOn[x][y] = true;// 该位置的监控器需要开启
						}
					}
				}
			}
		}

		// 统计需要开启的监控器数量
		int count = 0;
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				if (monitorOn[i][j]) {
					count++;
				}
			}
		}

		System.out.println(count);
	}
}
