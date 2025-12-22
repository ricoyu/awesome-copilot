package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 搜索插入位置
 * <p>
 * 给定一个排序数组和一个目标值，在数组中找到目标值，并返回其索引。如果目标值不存在于数组中，返回它将会被按顺序插入的位置。
 * <p>
 * 请必须使用时间复杂度为 O(log n) 的算法。
 *
 * <pre>
 * 示例 1:
 *
 * 输入: nums = [1,3,5,6], target = 5
 * 输出: 2
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: nums = [1,3,5,6], target = 2
 * 输出: 1
 * </pre>
 *
 * <pre>
 * 示例 3:
 *
 * 输入: nums = [1,3,5,6], target = 7
 * 输出: 4
 * </pre>
 *
 * <ul>这是典型的二分查找变种问题，核心逻辑是：
 *     <li/>初始化左右指针 left（起始为 0）和 right（起始为数组长度 - 1）。
 *     <ul>在 left <= right 的循环中，计算中间索引 mid，比较 nums[mid] 与目标值：
 *         <li/>若 nums[mid] == target，直接返回 mid（找到目标值）
 *         <li/>若 nums[mid] < target，说明目标值在右半区，将 left 移到 mid + 1。
 *         <li/>若 nums[mid] > target，说明目标值在左半区，将 right 移到 mid - 1。
 *     </ul>
 *     <li/>循环结束时，left 即为目标值应插入的位置（因为此时 left > right，left 恰好是第一个大于目标值的位置）。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-12-22 9:03
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SearchInsertPosition {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入有序数组: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入目标值: ");
		int target = scanner.nextInt();

		System.out.println(searchInsert(nums, target));
	}

	/**
	 * 查找目标值在有序数组中的索引，或返回插入位置
	 * @param nums   升序排列的整数数组
	 * @param target 要查找/插入的目标值
	 * @return       目标值索引或插入位置
	 */
	public static int searchInsert(int[] nums, int target) {
		if (nums == null ||nums.length==0) {
			return -1;
		}
		// 左指针，初始指向数组第一个元素
		int left = 0;
		// 右指针，初始指向数组最后一个元素
		int right = nums.length - 1;

		// 二分查找核心循环：当左指针不超过右指针时继续
		while (left <= right) {
			int mid = left + (right - left) / 2;
			if (nums[mid] == target) {
				return mid;
			} else if (nums[mid] < target) {
				left = mid + 1;
			} else {
				right = mid - 1;
			}
		}

		// 循环结束未找到目标值，left 即为插入位置
		return left;
	}
}
