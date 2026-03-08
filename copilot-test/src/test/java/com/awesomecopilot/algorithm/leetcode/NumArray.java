package com.awesomecopilot.algorithm.leetcode;

/**
 * 区域和检索 - 数组不可变
 * <p>
 * 给定一个整数数组  nums，处理以下类型的多个查询:
 * <p>
 * 计算索引 left 和 right （包含 left 和 right）之间的 nums 元素的 和 ，其中 left <= right <p>
 * 实现 NumArray 类：
 * <p>
 * NumArray(int[] nums) 使用数组 nums 初始化对象 <br/>
 * int sumRange(int left, int right) 返回数组 nums 中索引 left 和 right 之间的元素的 总和 ，包含 left 和 right 两点（也就是 nums[left] + nums[left + 1] + ... + nums[right] )
 *
 * <pre>
 * 示例 1：
 *
 * 输入：
 * ["NumArray", "sumRange", "sumRange", "sumRange"]
 * [[[-2, 0, 3, -5, 2, -1]], [0, 2], [2, 5], [0, 5]]
 * 输出：
 * [null, 1, -1, -3]
 * </pre>
 *
 * 解释：
 * NumArray numArray = new NumArray([-2, 0, 3, -5, 2, -1]);
 * numArray.sumRange(0, 2); // return 1 ((-2) + 0 + 3)
 * numArray.sumRange(2, 5); // return -1 (3 + (-5) + 2 + (-1))
 * numArray.sumRange(0, 5); // return -3 ((-2) + 0 + 3 + (-5) + 2 + (-1))
 *
 * <ul>解题思路: 要高效解决多次区间和查询的问题，核心思路是前缀和预处理：
 *     <li/>初始化时计算前缀和数组 prefixSum，其中 prefixSum[i] 表示数组前 i 个元素的和（prefixSum[0] = 0，prefixSum[1] = nums[0]，prefixSum[2] = nums[0]+nums[1]，以此类推）。
 *     <li/>查询区间 [left, right] 时，直接用 prefixSum[right+1] - prefixSum[left] 得到结果，时间复杂度为 O (1)，避免每次查询都遍历区间（O (n)）。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-06 9:01
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class NumArray {
	// 前缀和数组：prefixSum[0] = 0，prefixSum[1] = nums[0]，prefixSum[2] = nums[0]+nums[1]...
	private int[] prefixSum;
	
	//初始化前缀和数组
	public NumArray(int[] nums) {
		prefixSum = new int[nums.length + 1];
		for (int i = 1; i <= nums.length; i++) {
			prefixSum[i] = prefixSum[i - 1] + nums[i - 1];
		}
	}
	
	/**
	 * 计算[left, right]区间内元素的和（包含两端）
	 * @param left
	 * @param right
	 * @return
	 */
	public int sumRange(int left, int right) {
		// 核心公式：prefixSum[right+1]是前right+1个元素和（即nums[0]到nums[right]），
		// prefixSum[left]是前left个元素和（即nums[0]到nums[left-1]），
		// 两者的差就是nums[left]到nums[right]的和
		return prefixSum[right + 1] - prefixSum[left];
	}
	
	public static void main(String[] args) {
		int[] nums = {-2, 0, 3, -5, 2, -1};
		NumArray numArray = new NumArray(nums);
		System.out.println(numArray.sumRange(0, 2));  // 输出1
		System.out.println(numArray.sumRange(2, 5));  // 输出-1
		System.out.println(numArray.sumRange(0, 5));  // 输出-3
	}
}