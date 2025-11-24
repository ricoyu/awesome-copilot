package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 寻找两个正序数组的中位数
 * <p>
 * 给定两个大小分别为 m 和 n 的正序（从小到大）数组 nums1 和 nums2。请你找出并返回这两个正序数组的 中位数 。
 * <p>
 * 算法的时间复杂度应该为 O(log (m+n)) 。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：nums1 = [1,3], nums2 = [2]
 * 输出：2.00000
 * 解释：合并数组 = [1,2,3] ，中位数 2
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums1 = [1,2], nums2 = [3,4]
 * 输出：2.50000
 * 解释：合并数组 = [1,2,3,4] ，中位数 (2 + 3) / 2 = 2.5
 * </pre>
 *
 * <ul>中位数的定义
 *     <li/>如果 (m+n) 是奇数，中位数就是第 (m+n)/2 + 1 个数
 *     <li/>如果 (m+n) 是偶数，中位数就是第 (m+n)/2 和 (m+n)/2 + 1 个数的平均值。
 *     <li/>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-02 8:40
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MedianOfTwoSortedArrays {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组1: ");
		int[] num1 = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入数组2: ");
		int[] num2 = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(findMedianSortedArrays(num1, num2));
	}

	public static double findMedianSortedArrays(int[] nums1, int[] nums2) {
		// 确保 nums1 是较短的数组（便于二分查找）
		if (nums1.length > nums2.length) {
			return findMedianSortedArrays(nums2, nums1);
		}

		int m = nums1.length;
		int n = nums2.length;

		// 二分查找的左右边界
		int left = 0, right = m;

		while (left <= right) {
			// i 表示 nums1 中切分的位置
			// i：表示在 nums1 中，分割点右侧的第一个元素索引（即 nums1 中前 i 个元素属于左半部分）
			int i = (left + right) / 2;
			// j 根据 i 推导出来，保证左半部分和右半部分元素数量尽可能相等
			int j = (m + n + 1) / 2 - i;

			// 边界处理：如果 i=0，表示左边没有元素，设为最小值
			int maxLeftA = (i==0) ? Integer.MIN_VALUE : nums1[i-1];
			int minRightA = (i==m) ? Integer.MAX_VALUE : nums1[i];

			// 边界处理：如果 j=0，表示左边没有元素，设为最小值
			int maxLeftB = (j==0) ? Integer.MIN_VALUE : nums2[j-1];
			int minRightB = (j==n) ? Integer.MAX_VALUE : nums2[j];

			// 满足有效划分条件
			if (maxLeftA <= minRightB && maxLeftB <= minRightA) {
				// 总长度为奇数，中位数是左半部分最大值
				if ((m + n) % 2 == 1) {
					return Math.max(maxLeftA, maxLeftB);
				}else {
					// 总长度为偶数，中位数是左半部分最大值和右半部分最小值的平均
					return (Math.max(maxLeftA, maxLeftB) + Math.min(minRightA, minRightB)) / 2.0;
				}
			} else if (maxLeftA >minRightB) {
				// i 切得太大了，往左收缩
				right = i - 1;
			}else {
				// i 切得太小了，往右收缩
				left = i + 1;
			}
		}

		throw new IllegalArgumentException("输入的数组不合法");
	}
}
