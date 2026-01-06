package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 加一
 * <p>
 * 给定一个表示 大整数 的整数数组 digits，其中 digits[i] 是整数的第 i 位数字。这些数字按从左到右，从最高位到最低位排列。这个大整数不包含任何前导 0。
 * <p>
 * 将大整数加 1，并返回结果的数字数组。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：digits = [1,2,3]
 * 输出：[1,2,4]
 * 解释：输入数组表示数字 123。
 * 加 1 后得到 123 + 1 = 124。
 * 因此，结果应该是 [1,2,4]。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：digits = [4,3,2,1]
 * 输出：[4,3,2,2]
 * 解释：输入数组表示数字 4321。
 * 加 1 后得到 4321 + 1 = 4322。
 * 因此，结果应该是 [4,3,2,2]。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：digits = [9]
 * 输出：[1,0]
 * 解释：输入数组表示数字 9。
 * 加 1 得到了 9 + 1 = 10。
 * 因此，结果应该是 [1,0]。
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>倒序遍历：从数组最低位（最后一个元素） 开始处理，符合整数加 1 的运算规则；
 *     <li/>进位处理：初始进位为 1（题目要求加 1），每位计算规则为 当前值 = 原数字 + 进位，新数字取 当前值 % 10，新进位取 当前值 / 10；
 *     <li/>终止条件：遍历完成 或 进位变为 0（无进位时后续位无需处理）；
 *     <li/>边界兜底：遍历结束后若仍有进位（如 [9,9] 加 1），则在数组头部插入进位 1。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-03 8:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BigNumberPlusOne {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字数组digits, 以逗号分隔: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(Arrays.toString(plusOne(nums)));
	}

	private static int[] plusOne(int[] nums) {
		// 初始化进位为1，因为核心需求是给数字加1
		int carry = 1;
		for (int i = nums.length - 1; i >= 0; i--) {
			int sum = nums[i] + carry;
			nums[i] = sum % 10;
			carry = sum / 10;
		}

		// 遍历结束后若进位仍为1，说明所有位都进位（如[9]->[1,0]、[9,9]->[1,0,0]）
		if (carry == 1) {
			int[] result = new int[nums.length + 1];
			result[0] = 1;
			System.arraycopy(nums, 0, result, 1, nums.length);
			return result;
		}

		// 无剩余进位，直接返回原数组（已完成修改）
		return nums;
	}
}
