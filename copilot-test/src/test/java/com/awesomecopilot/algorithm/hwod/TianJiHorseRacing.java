package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 田忌赛马 - 贪心思维
 * <p>
 * 一、题目描述
 * <p>
 * 给定两个只包含数字的数组a、b，调整数组a里面数字的排列顺序，使得尽可能多的a[i] > b[i]。数组a和b中的数字各不相同。
 * <p>
 * 输出所有可以达到最优结果的a数组组数量
 * <p>
 * 二、输入描述
 * <p>
 * 输入的第一行是数组a中的数字，其中只包含数字，每两个数字之间相隔一个空格，a数组大小不超过10。
 * <p>
 * 输入的第二行是数组b中的数字，其中只包含数字，每两个数字之间相隔一个空格，b数组大小不超过10。
 * <p>
 * 三、输出描述
 * <p>
 * 输出所有可以达到最优结果的a数组数量
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 11 8 20
 * 10 13 7
 *
 * 2、输出
 * 1
 * </pre>
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 11 12 20
 * 10 13 7
 *
 * 2、输出
 * 2
 * </pre>
 *
 * <ul>核心思路（贪心 + 回溯）
 *     <li/>数组长度较短（≤10），可以枚举所有a的排列。
 *     <li/>对每种排列，统计有多少个位置满足 a[i] > b[i]。
 *     <li/>记录最大胜利次数 maxWin，并统计达到该胜利次数的排列数量。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-20 9:44
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TianJiHorseRacing {

	// 记录最大胜利次数
	private static int maxWin = 0;

	// 记录达到最大胜利次数的排列数量
	private static int count = 0;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组a: ");
		int[] a = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入数组b: ");
		int[] b = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();

		// 使用visited数组标记哪些数已使用
		boolean[] visited = new boolean[a.length];
		// 存储当前排列路径
		List<Integer> path = new ArrayList<>();

		// 回溯法枚举所有a的排列
		dfs(a, b, visited, path);

		// 输出满足最优胜利场数的排列个数
		System.out.println(count);
	}

	/**
	 * 回溯枚举所有a数组的排列，统计胜利场数
	 *
	 * @param a
	 * @param b
	 * @param visited 标记哪些数已使用
	 * @param path    当前排列路径
	 */
	private static void dfs(int[] a, int[] b, boolean[] visited, List<Integer> path) {
		// 如果当前排列完成
		if (path.size() == a.length) {
			int winCount = 0;
			for (int i = 0; i < a.length; i++) {
				if (path.get(i) > b[i]) {
					winCount++;
				}
			}

			// 更新最大胜利场数和对应排列个数
			if (winCount > maxWin) {
				maxWin = winCount;
				count = 1; // 重置为1
			} else if (winCount == maxWin) {
				count++;
			}
			return;
		}

		// 尝试每一个未被使用的数字
		for (int i = 0; i < a.length; i++) {
			if (!visited[i]) {
				visited[i] = true;
				path.add(a[i]);
				dfs(a, b, visited, path);
				path.remove(path.size() - 1);
				visited[i] = false;
				
			}
		}
	}

}
