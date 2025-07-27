package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 最长连续子序列
 * <p>
 * 一、题目描述
 * <p> <p>
 * 有N个正整数组成的一个序列。给定整数sum，求长度最长的连续子序列，使他们的和等于sum，返回此子序列的长度，
 * <p>
 * 如果没有满足要求的序列，返回-1。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入是：N个正整数组成的一个序列
 * <p>
 * 第二行输入是：给定整数sum
 * <p>
 * 三、输出描述
 * <p>
 * 最长的连续子序列的长度
 * <p>
 * 备注
 * <p>
 * 输入序列仅由数字和英文逗号构成，数字之间采用英文逗号分隔 <br/>
 * 序列长度：1 <= N <= 200 <br/>
 * 输入序列不考虑异常情况 <br/>
 * <pre>
 * 示例1
 * 1、输入
 * 1,3,5,7,2
 * 9
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <ul>本题的核心思想是 滑动窗口
 *     <li/>序列是正整数，因此只要当前窗口内的和大于目标 sum，就可以收缩左边界
 *     <li/>若当前窗口和等于 sum，则记录该窗口长度并尝试继续扩大右边界，寻找更长的解。
 *     <li/>如果小于 sum，就扩大窗口（右边界右移）
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-22 7:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestSubsequenceSum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组a中的数字: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入整数sum: ");
		int sum = scanner.nextInt();
		System.out.println(findLongestSubsequenceLength(nums, sum));
		scanner.close();
	}

	/**
	 * 使用滑动窗口求解和为 targetSum 的最长连续子序列长度
	 * @param nums
	 * @param targetSum
	 * @return
	 */
	public static int findLongestSubsequenceLength(int[] nums, int targetSum) {
		int left = 0; //窗口左边界
		int right = 0; //窗口右边界
		int sum = 0; //当前窗口内元素的和
		int maxLen = -1; // 记录最大长度，初始为-1，表示没找到

		while (right < nums.length) {
			sum += nums[right]; // 扩大右边界
			// 如果当前和超过了目标值，则收缩左边界
			while (sum > targetSum) {
				sum -= nums[left];
				left++;
			}
			// 如果和正好等于目标值，更新最大长度
			if (sum == targetSum) {
				int length = right - left + 1;
				maxLen = Math.max(maxLen, length);
			}

			right++; // 继续扩大窗口
		}

		return maxLen;
	}
}
