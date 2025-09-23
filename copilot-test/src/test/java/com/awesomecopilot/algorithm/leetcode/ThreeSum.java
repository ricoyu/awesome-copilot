package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 三数之和
 * <p>
 * 给你一个整数数组nums ，判断是否存在三元组 [nums[i], nums[j], nums[k]] 满足 i != j、i != k 且 j != k ，同时还满足 nums[i] + nums[j] + nums[k]
 * == 0 。请你返回所有和为 0 且不重复的三元组。
 * <p>
 * 注意：答案中不可以包含重复的三元组。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [-1,0,1,2,-1,-4]
 * 输出：[[-1,-1,2],[-1,0,1]]
 * 解释：
 * nums[0] + nums[1] + nums[2] = (-1) + 0 + 1 = 0 。
 * nums[1] + nums[2] + nums[4] = 0 + 1 + (-1) = 0 。
 * nums[0] + nums[3] + nums[4] = (-1) + 2 + (-1) = 0 。
 * 不同的三元组是 [-1,0,1] 和 [-1,-1,2] 。
 * 注意，输出的顺序和三元组的顺序并不重要。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [0,1,1]
 * 输出：[]
 * 解释：唯一可能的三元组和不为 0 。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [0,0,0]
 * 输出：[[0,0,0]]
 * 解释：唯一可能的三元组和为 0 。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-16 9:18
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ThreeSum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums, 以逗号分隔: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		scanner.close();
		System.out.println(threeSum(nums));
	}

	private static List<List<Integer>> threeSum(int[] nums) {
		// 创建结果列表
		List<List<Integer>> result = new ArrayList<>();
		// 边界条件判断：数组长度小于3时，直接返回空列表
		if (nums.length < 3) {
			return result;
		}

		// 对数组进行排序，为后续去重和双指针操作做准备
		Arrays.sort(nums);

		// 遍历数组，固定三元组的第一个元素
		for (int i = 0; i < nums.length; i++) {
			// 排序后，如果当前元素大于0，则后续元素都大于0，不可能形成和为0的三元组
			if (nums[i] > 0) {
				break;
			}

			// 去重：如果当前元素与前一个元素相同，跳过当前元素
			if (i > 0 && nums[i] == nums[i - 1]) {
				continue;
			}

			// 左指针，从i+1开始
			int left = i + 1;
			// 右指针，从数组末尾开始
			int right = nums.length - 1;

			// 双指针查找过程
			while (left < right) {
				// 计算三数之和
				int sum = nums[i] + nums[left] + nums[right];
				if (sum == 0) {
					// 找到符合条件的三元组，添加到结果列表
					result.add(Arrays.asList(nums[i], nums[left], nums[right]));

					// 去重：跳过左指针指向的重复元素
					while (left < right && nums[left] == nums[left + 1]) {
						left++;
					}

					// 去重：跳过右指针指向的重复元素
					while (left < right && nums[right] == nums[right - 1]) {
						right--;
					}

					// 移动双指针，继续查找
					left++;
					right--;
				} else if (sum < 0) {
					// 和小于0，需要增大总和，左指针右移
					left++;
				} else {
					// 和大于0，需要减小总和，右指针左移
					right--;
				}
			}
		}

		return result;
	}
}
