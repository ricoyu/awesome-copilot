package com.awesomecopilot.algorithm.leetcode;

import com.awesomecopilot.common.lang.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 189. 轮转数组
 * <p>
 * 给定一个整数数组 nums，将数组中的元素向右轮转 k 个位置，其中 k 是非负数。
 *
 * <pre>
 * 示例 1:
 *
 * 输入: nums = [1,2,3,4,5,6,7], k = 3
 * 输出: [5,6,7,1,2,3,4]
 * 解释:
 * 向右轮转 1 步: [7,1,2,3,4,5,6]
 * 向右轮转 2 步: [6,7,1,2,3,4,5]
 * 向右轮转 3 步: [5,6,7,1,2,3,4]
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入：nums = [-1,-100,3,99], k = 2
 * 输出：[3,99,-1,-100]
 * 解释:
 * 向右轮转 1 步: [99,-1,-100,3]
 * 向右轮转 2 步: [3,99,-1,-100]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-08-16 6:57
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ArrayRotator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入整数k: ");
		int k = scanner.nextInt();
		rotate(nums, 0, nums.length-1);
		rotate(nums, 0, k-1);
		rotate(nums, k, nums.length-1);
		ArrayUtils.print(nums);
	}

	private static void rotate(int[] nums, int start, int end) {
		while (start < end) {
			nums[start] = nums[start] ^ nums[end];
			nums[end] = nums[start] ^ nums[end];
			nums[start] = nums[start] ^ nums[end];
			start++;
			end--;
		}
	}
}
