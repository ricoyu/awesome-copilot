package com.awesomecopilot.algorithm.leetcode;

import com.awesomecopilot.common.lang.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 除自身以外数组的乘积
 * <p>
 * 给你一个整数数组 nums，返回 数组 answer ，其中 answer[i] 等于 nums 中除 nums[i] 之外其余各元素的乘积 。 <p>
 * 题目数据 保证 数组 nums之中任意元素的全部前缀元素和后缀的乘积都在  32 位 整数范围内。
 * <p>
 * 请 不要使用除法，且在 O(n) 时间复杂度内完成此题。
 * <p>
 * <pre>
 * 示例 1:
 * 输入: nums = [1,2,3,4]
 * 输出: [24,12,8,6]
 * </pre>
 * <p>
 * <pre>
 * 示例 2:
 * 输入: nums = [-1,1,0,-3,3]
 * 输出: [0,0,9,0,0]
 * </pre>
 * <p>
 * 为了实现这个问题，我们可以使用“前缀积”和“后缀积”的方法。这种方法可以避免使用除法，并且可以在O(n) 的时间复杂度内解决问题。
 *
 * <ol>具体思路如下：
 *     <li/>前缀乘积数组：prefix [i] 表示 nums [0] 到 nums [i-1] 的乘积
 *     <li/>后缀乘积数组：suffix [i] 表示 nums [i+1] 到 nums [n-1] 的乘积
 *     <li/>结果 answer [i] = prefix [i] * suffix [i]
 * </ol>
 * <p>
 * Copyright: Copyright (c) 2024-11-12 9:17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ProductOfArrayExceptSelf {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter numbers: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		ArrayUtils.print(productExceptSelf(nums));
	}

	public static int[] productExceptSelf(int[] nums) {
		int n = nums.length;

		// 用于存储结果的数组 answer
		int[] answer = new int[n];

		// 前缀乘积数组：prefix[i]表示nums[0]到nums[i-1]的乘积
		int[] prefix = new int[n];
		// 初始值：第一个元素的前缀乘积为1（没有元素）
		prefix[0] = 1;
		for (int i = 1; i < n; i++) {
		    prefix[i] = prefix[i - 1] * nums[i - 1];
		}

		// 后缀乘积数组：suffix[i]表示nums[i+1]到nums[n-1]的乘积
		int[] suffix = new int[n];
		// 初始值：最后一个元素的后缀乘积为1（没有元素）
		suffix[n - 1] = 1;
		for (int i = n-2; i >= 0; i--) {
		    suffix[i] = suffix[i + 1] * nums[i + 1];
		}

		// 计算结果：每个位置的结果等于前缀乘积乘以后缀乘积
		for (int i = 0; i < n; i++) {
			answer[i] = prefix[i] * suffix[i];
		}

		return answer;
	}
}
