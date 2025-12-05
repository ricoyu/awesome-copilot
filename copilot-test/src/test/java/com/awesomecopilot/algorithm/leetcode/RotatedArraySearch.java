package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 搜索旋转排序数组
 * <p>
 * 整数数组 nums 按升序排列，数组中的值互不相同 。
 * <p>
 * 在传递给函数之前，nums 在预先未知的某个下标 k（0 <= k < nums.length）上进行了 向左旋转，使数组变为 [nums[k], nums[k+1], ..., nums[n-1], nums[0],
 * nums[1], ..., nums[k-1]]（下标 从 0 开始 计数）。例如， [0,1,2,4,5,6,7] 下标 3 上向左旋转后可能变为 [4,5,6,7,0,1,2] 。
 * <p>
 * 给你 旋转后 的数组 nums 和一个整数 target ，如果 nums 中存在这个目标值 target ，则返回它的下标，否则返回 -1 。
 * <p>
 * 你必须设计一个时间复杂度为 O(log n) 的算法解决此问题。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [4,5,6,7,0,1,2], target = 0
 * 输出：4
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [4,5,6,7,0,1,2], target = 3
 * 输出：-1
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [1], target = 0
 * 输出：-1
 * </pre>
 * <p>
 * 解题思路
 *  <p>
 *
 * <ul>利用二分查找，旋转后的数组可分为两部分有序子数组。每次二分确定 mid 后，判断左半部分或右半部分是否有序：
 *     <li/>若左半部分有序，检查 target 是否在左半区间，调整指针；
 *     <li/>若右半部分有序，检查 target 是否在右半区间，调整指针；
 *     <li/>重复直至找到 target 或指针越界。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-11-28 14:28
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RotatedArraySearch {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组, 逗号分隔: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入目标值: ");
		int target = Integer.parseInt(scanner.next());
		System.out.println(search(nums, target));
		scanner.close();
	}

	private static int search(int[] nums, int target) {
		int left = 0;
		int right = nums.length - 1;

		while (left <= right) {
			int mid = left + (right - left) / 2;
			if (nums[mid] == target) {
				return mid;
			}

			// 判断左半部分是否有序
			/*
			 * 当 nums[left] <= nums[mid] 时，说明从 left 到 mid 之间没有出现 “断崖式下降”（即没有跨越两段升序子数组的分界点），
			 * 因此这一段必然是连续的升序区间，也就是左半部分有序。
			 *
			 * 举个例子：
			 * 对于数组 [4,5,6,7,0,1,2]，若 left=0，mid=3，则 nums[0]=4 <= nums[3]=7，[4,5,6,7] 是有序的左半部分；
			 * 若 left=4，mid=5，则 nums[4]=0 <= nums[5]=1，[0,1] 也是有序的区间。
			 * 反之，如果 nums[left] > nums[mid]，说明 left 到 mid 之间跨越了分界点（比如 left=3，mid=5 时，nums[3]=7 > nums[5]=1），此时左半部分无序，而右半部分必然有序。
			 */
			if (nums[left] < nums[mid]) {
				// 目标值在左半部分有序区间内
				if (nums[mid] < nums[0] && target >= nums[0]) {
					right = mid - 1;
				} else {
					left = mid + 1;
				}
			} else {// 右半部分有序
				// 目标值在右半部分有序区间内
				if (nums[mid] < target && target <= nums[right]) {
					left = mid + 1;
				} else {
					right = mid - 1;
				}
			}
		}

		return -1;
	}
}