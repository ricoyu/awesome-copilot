package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全排列 II
 * <p>
 * 给定一个可包含重复数字的序列 nums ，按任意顺序 返回所有不重复的全排列。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,1,2]
 * 输出：
 * [[1,1,2],
 *  [1,2,1],
 *  [2,1,1]]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [1,2,3]
 * 输出：[[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
 * </pre>
 *
 * <ul>全排列 II 的核心是回溯法 + 去重，与普通全排列的区别在于处理重复元素：
 *     <li/>排序：先对数组排序，让重复元素相邻，便于后续去重判断；
 *     <li/>回溯：通过递归尝试所有排列组合，用 used 数组标记元素是否已被使用；
 *     <li/>去重关键：若当前元素与前一个元素相同，且前一个元素未被使用（说明是同一层递归的重复选择），则跳过当前元素，避免生成重复排列。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-08 12:01
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PermutationII {
	
	// 存储最终结果的列表
	private List<List<Integer>> result;
	
	// 存储当前路径的临时列表
	private List<Integer> path;
	
	// 标记元素是否被使用过，避免重复选择同一位置元素
	private boolean[] used;
	
	public static void main(String[] args) {
		PermutationII solution = new PermutationII();
		// 测试示例1：含重复元素
		int[] nums1 = {1, 1, 2};
		List<List<Integer>> res1 = solution.permuteUnique(nums1);
		System.out.println("示例1输出：" + res1); // 输出 [[1,1,2],[1,2,1],[2,1,1]]
		
		// 测试示例2：无重复元素
		int[] nums2 = {1, 2, 3};
		List<List<Integer>> res2 = solution.permuteUnique(nums2);
		System.out.println("示例2输出：" + res2); // 输出所有6种排列
	}
	
	public List<List<Integer>> permuteUnique(int[] nums) {
		result = new ArrayList<>();
		path = new ArrayList<>();
		used = new boolean[nums.length];
		
		// 排序：让重复元素相邻，是去重的前提
		Arrays.sort(nums);
		
		// 启动回溯
		backtrack(nums);
		
		return result;
	}
	
	/**
	 * 回溯核心方法：递归生成所有不重复排列
	 *
	 * @param nums 输入的排序后数组
	 */
	private void backtrack(int[] nums) {
		// 终止条件：当前路径长度等于数组长度，说明生成了一个完整排列
		if (path.size() == nums.length) {
			// 注意：需要新建列表，否则后续修改path会影响已存入的结果
			result.add(new ArrayList<>(path));
			return;
		}
		
		// 遍历数组，尝试选择每个未被使用的元素
		for (int i = 0; i < nums.length; i++) {
			// 去重关键逻辑：
			// 1. used[i] 为 true：当前元素已被使用，跳过
			// 2. i > 0 且 nums[i] == nums[i-1] 且 !used[i-1]：
			// 同一层递归中，前一个重复元素未被使用（说明是同一层的重复选择），跳过
			if (used[i] || (i > 0 && nums[i] == nums[i - 1] && !used[i - 1])) {
				continue;
			}
			// 选择当前元素：标记为已使用，加入路径
			used[i] = true;
			path.add(nums[i]);
			
			// 递归：继续选择下一个元素
			backtrack(nums);
			
			// 回溯：撤销选择，恢复状态（标记为未使用，移出路径）
			used[i] = false;
			path.remove(path.size() - 1);
		}
	}
}