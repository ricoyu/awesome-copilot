package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 子集
 * <p>
 * 给你一个整数数组 nums, 数组中的元素互不相同 。返回该数组所有可能的子集（幂集）。
 * <p>
 * 解集不能包含重复的子集。你可以按任意顺序返回解集。
 * <p>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,2,3]
 * 输出：[[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
 * </pre>
 *
 * <pre>
 * 示例 2：
 * 输入：nums = [0]
 * 输出：[[],[0]]
 * <p/>
 * </pre>
 * Copyright: Copyright (c) 2025-11-17 9:28
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class Subsets {

	// 存储所有子集（最终结果）
	private static List<List<Integer>> result = new ArrayList<>();
	// 存储当前正在构建的子集（临时状态）
	private static List<Integer> path = new ArrayList<>();

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter numbers: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		subset(nums);
		System.out.println(result);
	}

	public static void subset(int[] nums) {
		// 从索引0开始递归探索（控制不回头选）
		backtrack(nums, 0);
	}

	/**
	 * // start：当前选择的起始索引（关键：避免重复子集）
	 * @param nums
	 * @param start
	 */
	private static void backtrack(int[] nums, int start) {
		// 终止条件：每一步的临时子集都是一个有效解，直接收集
		result.add(new ArrayList<>(path));

		// 遍历当前阶段的所有选择（从start开始，不回头）
		for (int i = start; i < nums.length; i++) {
			// 做出选择：将当前元素加入临时子集
			path.add(nums[i]);
			// 递归探索下一个阶段（起始索引+1，避免重复选之前的元素）
			backtrack(nums, i + 1);
			// 回溯：撤销选择，恢复状态
			path.remove(path.size() - 1);
		}
	}
}
