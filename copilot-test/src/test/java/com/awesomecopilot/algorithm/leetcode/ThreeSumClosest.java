package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 最接近的三数之和
 * <p>
 * 给你一个长度为 n 的整数数组 nums 和 一个目标值 target。请你从 nums 中选出三个整数，使它们的和与 target 最接近。 <p>
 * 返回这三个数的和。
 * <p>
 * 假定每组输入只存在恰好一个解。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [-1,2,1,-4], target = 1
 * 输出：2
 * 解释：与 target 最接近的和是 2 (-1 + 2 + 1 = 2)。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [0,0,0], target = 1
 * 输出：0
 * 解释：与 target 最接近的和是 0（0 + 0 + 0 = 0）。
 * </pre>
 *
 * <ul>解题思路
 *     <li/>先对数组进行排序，这是使用双指针法的前提
 *     <li/>固定一个基准元素，然后使用双指针寻找另外两个元素
 *     <li/>通过比较当前三数之和与目标值的差值，更新最接近的和
 *     <li/>根据当前和与目标值的大小关系，移动双指针调整和的大小
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-17 8:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ThreeSumClosest {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter numbers: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("Enter target: ");
		int target = scanner.nextInt();
		System.out.println(threeSumClosest(nums, target));
	}

	/**
	 * 寻找数组中三个数的和最接近目标值
	 *
	 * @param nums   整数数组
	 * @param target 目标值
	 * @return 最接近目标值的三数之和
	 */
	private static int threeSumClosest(int[] nums, int target) {
		// 先对数组排序，为双指针法做准备
		Arrays.sort(nums);

		// 初始化最接近的和为前三个数的和
		int closestSum = nums[0] + nums[1] + nums[2];
		// 计算初始差值的绝对值
		int minDiff = Math.abs(closestSum - target);

		// 固定第一个数，遍历数组
		for (int i = 0; i < nums.length - 2; i++) {
			// 左指针从i+1开始
			int left = i + 1;
			// 右指针从数组末尾开始
			int right = nums.length - 1;

			// 双指针遍历
			while (left < right) {
				// 计算当前三数之和
				int currentSum = nums[i] + nums[left] + nums[right];
				// 计算当前差值的绝对值
				int currentDiff = Math.abs(currentSum - target);

				// 如果当前差值更小，更新最接近的和和最小差值
				if (currentDiff < minDiff) {
					closestSum = currentSum;
					minDiff = currentDiff;

					// 如果差值为0，说明找到了最接近的和，直接返回
					if (currentDiff == 0) {
						return closestSum;
					}
				}

				// 根据当前和与目标值的大小关系，移动指针
				if (currentSum < target) {
					// 当前和小于目标值，左指针右移增大和
					left++;
				}else {
					// 当前和大于等于目标值，右指针左移减小和
					right--;
				}
			}
		}
		return closestSum;
	}
}
