package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 重新分装苹果
 * <p>
 * 给你一个长度为n的数组apple和另一个长度为m的数组capacity 。
 * <p>
 * 一共有n个包裹，其中第i个包裹中装着 apple[i]个苹果。同时，还有m个箱子，第i个箱子的容量为capacity[i]个苹果。
 * <p>
 * 请你选择一些箱子来将这 n 个包裹中的苹果重新分装到箱子中，返回你需要选择的箱子的最小数量。
 * <p>
 * 注意，同一个包裹中的苹果可以分装到不同的箱子中。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：apple = [1,3,2], capacity = [4,3,1,5,2]
 * 输出：2
 * 解释：使用容量为 4 和 5 的箱子。
 * 总容量大于或等于苹果的总数，所以可以完成重新分装。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：apple = [5,5,5], capacity = [2,4,2,7]
 * 输出：4
 * 解释：需要使用所有箱子。
 * </pre>
 *
 * 你需要解决的问题是：给定装苹果的包裹数组和箱子容量数组，计算能装下所有苹果所需的最少箱子数量。核心逻辑是先统计苹果总数，再优先使用容量大的箱子，直到总容量≥苹果总数。
 * <ul>解题思路
 *     <li/>计算苹果总数：遍历apple数组，累加得到所有苹果的总数量。
 *     <li/>排序箱子容量：将capacity数组降序排序，优先使用大箱子能最小化箱子数量。
 *     <li/>贪心选取箱子：从最大的箱子开始累加容量，直到累加值≥苹果总数，此时累加的箱子数即为答案
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-12-24 8:53
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class AppleRepackaging {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入苹果数组: ");
		int[] apple = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入箱子容量数组: ");
		int[] capacity = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println("最少箱子数量为: " + repackaging(apple, capacity));
	}

	private static int repackaging(int[] apple, int[] capacity) {
		Integer[] array = Arrays.stream(capacity).boxed().sorted((a, b) -> b - a).toArray(Integer[]::new);
		int totalApples = Arrays.stream(apple).sum();

		int totalCapacity = 0;
		int count = 0;
		for (Integer i : array) {
			totalCapacity+= i;
			count++;
			if (totalCapacity >= totalApples) {
				return count ;
			}
		}
		return -1;
	}
}
