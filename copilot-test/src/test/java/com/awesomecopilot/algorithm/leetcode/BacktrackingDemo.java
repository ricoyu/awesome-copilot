package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 最简单的 Java 示例：输出 [1,2] 的全排列
 *
 * 目标：给数组 [1,2]，输出所有排列
 * <p/>
 * Copyright: Copyright (c) 2026-04-01 8:01
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BacktrackingDemo {
	
	public static void main(String[] args) {
		int[] nums = {1, 2};
		List<List<Integer>> result = permute(nums);
		System.out.println(result);
	}
	
	private static List<List<Integer>> permute(int[] nums) {
		List<List<Integer>> result = new ArrayList<>();
		backtrack(nums, new ArrayList<>(), result);
		return result;
	}
	
	private static void backtrack(int[] nums, List<Integer> path, List<List<Integer>> result) {
		// 1. 终止条件：路径长度 == 数组长度，说明找到一个排列
		if (path.size() == nums.length) {
			result.add(new ArrayList<Integer>(path));
			return;
		}
		
		// 2. 遍历所有选择
		for (int num : nums) {
			// 已经选过的跳过
			if (path.contains(num)) {
				continue;
			}
			path.add(num);// 选择：把数字加入路径
			backtrack(nums, path, result); // 递归：继续往下走
			path.remove(path.size() - 1);// 撤销：移除最后一个元素 → 回溯！
		}
	
	}
}