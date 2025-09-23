package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 四数之和
 * <p>
 * 给你一个由 n 个整数组成的数组 nums, 和一个目标值target。
 * 请你找出并返回满足下述全部条件且不重复的四元组 [nums[a], nums[b], nums[c], nums[d]] （若两个四元组元素一一对应，则认为两个四元组重复）：
 * <p>
 * 0 <= a, b, c, d < n <p>
 * a、b、c 和 d 互不相同 <p>
 * nums[a] + nums[b] + nums[c] + nums[d] == target <p>
 * 你可以按 任意顺序 返回答案 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,0,-1,0,-2,2], target = 0
 * 输出：[[-2,-1,1,2],[-2,0,0,2],[-1,0,0,1]]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [2,2,2,2,2], target = 8
 * 输出：[[2,2,2,2]]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-19 9:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FourSum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter numbers: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("Enter target: ");
		int target = scanner.nextInt();
		System.out.println(fourSum(nums, target));
	}

	private static List<List<Integer>> fourSum(int[] nums, int target) {
		List<List<Integer>> result = new ArrayList<>();
		// 数组长度小于4时，直接返回空结果
		if (nums.length < 4) {
			return result;
		}

		// 对数组进行排序，为后续去重和双指针做准备
		Arrays.sort(nums);
		// 第一层循环：固定第一个数
		for (int i = 0; i < nums.length - 3; i++) {
			// 去重：如果当前元素与前一个元素相同，跳过当前元素
			if (i > 0 && nums[i] == nums[i - 1]) {
				continue;
			}

			// 第二层循环：固定第二个数
			for (int j = i + 1; j < nums.length - 2; j++) {
				// 去重：如果当前元素与前一个元素相同，跳过当前元素
				if (j > i + 1 && nums[j] == nums[j - 1]) {
					continue;
				}

				// 定义双指针，left从j+1开始，right从数组末尾开始
				int left = j+1;
				int right = nums.length-1;

				// 双指针遍历，寻找满足条件的两个数
				while (left < right) {
					// 计算四数之和
					// 使用long避免int溢出
					long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];
					if (sum == target) {
						// 找到符合条件的四元组，加入结果集
						result.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
						// 去重：跳过left指针指向的重复元素
						while (left < right && nums[left] == nums[left+1]) {
							left++;
						}
						// 去重：跳过right指针指向的重复元素
						while (left < right && nums[right] == nums[right-1]) {
							right--;
						}

						// 移动双指针
						left++;
						right--;
					} else if (sum < target) {
						// 总和小于目标值，左指针右移，增大总和
						left++;
					}else {
						// 总和大于目标值，右指针左移，减小总和
						right--;
					}
				}
			}
		}
		return result;
	}
}
