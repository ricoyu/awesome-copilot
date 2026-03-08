package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 回溯法最基础例子
 * <p>
 * 回溯法的核心思想可以总结为：“尝试 - 回退 - 再尝试”，就像走迷宫，选择一条路走到底，走不通就退回来换另一条路，直到找到所有解。
 * <p>
 * 给定一个无重复元素的数组 nums = [1,2,3]，返回所有可能的子集（包括空集和自身）。
 *  <p>
 * 输出：[[],[1],[2],[3],[1,2],[1,3],[2,3],[1,2,3]]
 *
 * <ul>核心思路
 *     <li/>每个元素有 “选” 和 “不选” 两种状态；
 *     <li/>用回溯遍历所有状态：选当前元素 → 递归处理下一个 → 回溯（不选当前元素）→ 处理下一个。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-08 12:32
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SubsetGenerator {
	
	private List<List<Integer>> result;
	private List<Integer> path; // 记录当前选中的元素
	
	public List<List<Integer>> subsets(int[] nums) {
		result = new ArrayList<>();
		path = new ArrayList<>();
		backtrack(nums, 0);// 从第0个元素开始选择
		return result;
	}
	
	/**
	 * 回溯函数
	 * @param nums 原数组
	 * @param start 当前处理的元素下标（避免重复选，比如选了2就不再回头选1）
	 */
	private void backtrack(int[] nums, int start) {
		// 关键：每一步都要把当前路径加入结果（因为子集包含所有中间状态）
		result.add(new ArrayList<>(path));
		
		// 遍历从start开始的元素（避免重复组合）
		for (int i = start; i < nums.length; i++) {
			// 选择当前元素
			path.add(nums[i]);
			// 递归：处理下一个元素
			backtrack(nums, i + 1);
			// 回溯：撤销选择（把当前元素从路径中移除）
			path.remove(path.size() - 1);
		}
	}
	
	public static void main(String[] args) {
		SubsetGenerator sg = new SubsetGenerator();
		//System.out.println(sg.subsets(new int[]{1,2,3}));
		System.out.println(sg.subsets(new int[]{1,2}));
	}
}