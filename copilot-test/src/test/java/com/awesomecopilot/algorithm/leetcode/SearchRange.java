package com.awesomecopilot.algorithm.leetcode;

/**
 * 在排序数组中查找元素的第一个和最后一个位置
 * <p>
 * 给你一个按照非递减顺序排列的整数数组 nums，和一个目标值 target。请你找出给定目标值在数组中的开始位置和结束位置。
 * <p>
 * 如果数组中不存在目标值 target，返回 [-1, -1]。
 * <p>
 * 你必须设计并实现时间复杂度为 O(log n) 的算法解决此问题。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [5,7,7,8,8,10], target = 8
 * 输出：[3,4]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [5,7,7,8,8,10], target = 6
 * 输出：[-1,-1]
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [], target = 0
 * 输出：[-1,-1]
 * </pre>
 *
 * 核心解题思路：
 * <p>
 * 使用两次二分搜索分别查找目标值的左边界（第一个位置）和右边界（最后一个位置）。 <br/>
 * 左边界二分通过在 nums[mid] >= target 时压缩右边界来实现； <br/>
 * 右边界二分通过在 nums[mid] <= target 时压缩左边界来实现。确保处理空数组和不存在目标的情况。 <br/>
 * <ul> 解题思路
 *     <li/>二分查找左边界：找到第一个大于等于 target 的位置，验证该位置是否等于 target，是则为左边界，否则不存在。
 *     <li/>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-12-21 10:49
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SearchRange {

	public static int[] searchRange(int[] nums, int target) {
		// 初始化结果数组为[-1,-1]
		int[] result = {-1, -1};
		// 处理空数组的特殊情况
		if (nums == null || nums.length == 0) {
			return result;
		}

		// 1. 查找左边界：第一个等于target的位置
		int left = findLeftBound(nums, target);
		// 如果左边界不存在，直接返回[-1,-1]
		if (left == -1) {
			return result;
		}

		// 2. 查找右边界：第一个等于target的位置
		int right = findRightBound(nums, target);
		result[0] = left;
		result[1] = right;

		return result;
	}

	/**
	 * 查找目标值的左边界（第一个等于target的位置）
	 * @param nums   排序数组
	 * @param target 目标值
	 * @return 左边界索引，不存在返回-1
	 */
	private static int findLeftBound(int[] nums, int target) {
		int left = 0;
		int right = nums.length - 1;
		int leftBound = -1;

		while (left <= right) {
			int mid = left + (right-left)/2;
			if (nums[mid] == target) {
				// 找到目标值，记录当前位置，继续向左找更早的位置
				leftBound = mid;
				right = mid - 1;
			} else if (nums[mid] < target) {
				// 目标值在右侧，移动左指针
				left = mid + 1;
			} else {
				// 目标值在左侧，移动右指针
				right = mid - 1;
			}
		}

		return leftBound;
	}

	/**
	 * 查找目标值的右边界（最后一个等于target的位置）
	 * @param nums   排序数组
	 * @param target 目标值
	 * @return 右边界索引，不存在返回-1
	 */
	private static int findRightBound(int[] nums, int target) {
		int left = 0;
		int right = nums.length - 1;
		int rightBound = -1;

		while (left <= right) {
			int mid = left + (right-left)/2;
			if (nums[mid] == target) {
				// 找到目标值，记录当前位置，继续向右找更晚的位置
				rightBound = mid;
				left = mid + 1;
			} else if (nums[mid] < target) {
				// 目标值在右侧，移动左指针
				left = mid + 1;
			} else {
				// 目标值在左侧，移动右指针
				right = mid - 1;
			}
		}
		return rightBound;
	}
}
