package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 寻找数组的中心下标
 * <p>
 * 给你一个整数数组 nums ，请计算数组的 中心下标 。
 * <p>
 * 数组 中心下标 是数组的一个下标，其左侧所有元素相加的和等于右侧所有元素相加的和。
 * <p>
 * 如果中心下标位于数组最左端，那么左侧数之和视为 0 ，因为在下标的左侧不存在元素。这一点对于中心下标位于数组最右端同样适用。
 * <p>
 * 如果数组有多个中心下标，应该返回 最靠近左边 的那一个。如果数组不存在中心下标，返回 -1 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1, 7, 3, 6, 5, 6]
 * 输出：3
 * 解释：
 * 中心下标是 3 。
 * 左侧数之和 sum = nums[0] + nums[1] + nums[2] = 1 + 7 + 3 = 11 ，
 * 右侧数之和 sum = nums[4] + nums[5] = 5 + 6 = 11 ，二者相等。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [1, 2, 3]
 * 输出：-1
 * 解释：
 * 数组中不存在满足此条件的中心下标。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [2, 1, -1]
 * 输出：0
 * 解释：
 * 中心下标是 0 。
 * 左侧数之和 sum = 0 ，（下标 0 左侧不存在元素），
 * 右侧数之和 sum = nums[1] + nums[2] = 1 + -1 = 0 。
 * </pre>
 *
 * <ul>核心思路是前缀和优化
 *     <li/>先计算数组的总和 totalSum；
 *     <li/>遍历数组时维护左侧元素的累加和 leftSum；
 *     <li/>对于当前下标 i，右侧元素和 = 总和 - 左侧和 - 当前元素值；
 *     <li/>若左侧和等于右侧和，当前下标即为中心下标（优先返回最左侧的）；
 *     <li/>遍历结束未找到则返回 -1。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-11 8:58
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ArrayCenterIndex {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().split("\\s*,\\s*")).mapToInt(Integer::parseInt).toArray();
		System.out.println(pivotIndex(nums));
	}
	
	/**
	 * 寻找数组的中心下标
	 * @param nums 整数数组
	 * @return 最左侧的中心下标，无则返回-1
	 */
	public static int pivotIndex(int[] nums) {
		// 1. 计算数组所有元素的总和
		int totalSum = 0;
		for (int num : nums) {
			totalSum += num;
		}
		
		// 2. 维护左侧元素的累加和，初始为0（中心下标在最左端时左侧和为0）
		int leftSum = 0;
		for (int i = 0; i < nums.length; i++) {
			// 3. 右侧和 = 总和 - 左侧和 - 当前元素（当前元素不属于左侧/右侧）
			int rightSum = totalSum - leftSum - nums[i];
			// 4. 若左侧和等于右侧和，返回当前下标（优先返回最左侧）
			if (leftSum == rightSum) {
				return i;
			}
			
			// 5. 否则，将当前元素加入左侧和，继续遍历下一个下标
			leftSum += nums[i];
		}
		// 6. 遍历结束未找到中心下标，返回-1
		return -1;
	}
}