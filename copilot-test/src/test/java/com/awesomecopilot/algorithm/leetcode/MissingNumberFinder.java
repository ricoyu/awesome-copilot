package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 丢失的数字
 * <p>
 * 给定一个包含 [0, n] 中 n 个数的数组 nums ，找出 [0, n] 这个范围内没有出现在数组中的那个数。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [3,0,1]
 *
 * 输出：2
 *
 * 解释：n = 3，因为有 3 个数字，所以所有的数字都在范围 [0,3] 内。2 是丢失的数字，因为它没有出现在 nums 中。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [0,1]
 *
 * 输出：2
 *
 * 解释：n = 2，因为有 2 个数字，所以所有的数字都在范围 [0,2] 内。2 是丢失的数字，因为它没有出现在 nums 中。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [9,6,4,2,3,5,7,0,1]
 *
 * 输出：8
 *
 * 解释：n = 9，因为有 9 个数字，所以所有的数字都在范围 [0,9] 内。8 是丢失的数字，因为它没有出现在 nums 中。
 * </pre>
 *
 * <ul>解题思路
 *     <li/>计算 [0, n] 范围内所有数字的理论总和：总和 = n * (n + 1) / 2（等差数列求和公式）
 *     <li/>计算数组中所有数字的实际总和
 *     <li/>理论总和 - 实际总和 = 缺失的数字
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-02-26 9:34
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MissingNumberFinder {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(findMissingNumber(nums));
		scanner.close();
	}
	
	private static int findMissingNumber(int[] nums) {
		int n = nums.length;
		long logicalSum = n * (n + 1) / 2;
		long actualSum = Arrays.stream(nums).sum();
		return (int) (logicalSum - actualSum);
	}
}