package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 整数反转
 * <p>
 * 给你一个 32 位的有符号整数x ，返回将x中的数字部分反转后的结果。
 * <p>
 * 如果反转后整数超过32位的有符号整数的范围 [−231,  231 − 1] ，就返回 0。
 * <p>
 * 假设环境不允许存储 64 位整数（有符号或无符号）。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：x = 123
 * 输出：321
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：x = -123
 * 输出：-321
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：x = 120
 * 输出：21
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：x = 0
 * 输出：0
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-06 10:36
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IntegerReverser {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入一个整数: ");
		int x = scanner.nextInt();
		System.out.println(reverse(x));
	}

	/**
	 * 反转整数并处理溢出情况
	 * @param x 待反转的32位有符号整数
	 * @return 反转后的整数，若溢出则返回0
	 */
	private static int reverse(int x) {
		// 记录符号，正数为1，负数为-1
		int sign = 1;
		if (x < 0) {
			sign = -1;
			// 将负数转为正数处理，注意：-2^31转为正数会溢出，所以用long暂存
			long temp = x;
			temp = -temp;
			x = (int) temp;
		}

		int reversed = 0;
		while (x >0) {
			// 取出最后一位数字
			int digit = x % 10;

			// 检查是否会溢出
			// 情况1：反转数已经大于最大整数的1/10，乘以10后必溢出
			// 情况2：反转数等于最大整数的1/10，且最后一位大于7（2^31-1的最后一位是7）
			if (reversed > Integer.MAX_VALUE / 10 || (reversed == Integer.MAX_VALUE / 10 && digit > 7)) {
				return 0;
			}

			reversed = reversed * 10 + digit;

			/*
			 * 移除原数的最后一位
			 * 初始值：x = 123
			 * 第一次循环处理后，我们已经获取了最后一位数字 3
			 * 执行 x = x / 10 → 123 / 10 = 12（整数除法自动舍弃小数部分）
			 * 此时 x 变为 12，相当于移除了最后一位的 3
			 */
			x /= 10;
		}

		// 恢复符号并返回
		return sign * reversed;
	}
}
