package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.Scanner;

/**
 * 两数相除
 * <p>
 * 给你两个整数，被除数 dividend 和除数 divisor。将两数相除，要求不使用乘法、除法和取余运算。
 * 整数除法应该向零截断，也就是截去（truncate）其小数部分。例如，8.345 将被截断为 8 ，-2.7335 将被截断至 -2
 * 返回被除数 dividend 除以除数 divisor 得到的 商
 * 注意：假设我们的环境只能存储 32 位 有符号整数，其数值范围是 [−231,  231 − 1] 。本题中，如果商 严格大于 231 − 1 ，则返回 231 − 1 ；如果商 严格小于 -231 ，则返回 -231 。
 * <p>
 * 示例 1:
 * <p>
 * 输入: dividend = 10, divisor = 3
 * 输出: 3
 * 解释: 10/3 = 3.33333.. ，向零截断后得到 3 。
 * 示例 2:
 * <p>
 * 输入: dividend = 7, divisor = -3
 * 输出: -2
 * 解释: 7/-3 = -2.33333.. ，向零截断后得到 -2 。
 * <p/>
 * Copyright: Copyright (c) 2025-10-10 10:15
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DivideTwoIntegers {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入被除数dividend: ");
		int dividend = scanner.nextInt();
		System.out.print("请输入除数divisor: ");
		int divisor = scanner.nextInt();
		System.out.println(divide(dividend, divisor));
		scanner.close();
	}

	/**
	 * 两数相除，不使用乘法、除法和取余运算
	 *
	 * @param dividend 被除数
	 * @param divisor  除数
	 * @return 商（向零截断）
	 */
	private static int divide(int dividend, int divisor) {
		// 处理溢出情况：最小32位整数除以-1会溢出
		if (dividend == Integer.MIN_VALUE && divisor == -1) {
			return Integer.MAX_VALUE;
		}

		// 确定结果的符号
		int sign = 1;

		if ((dividend <  0 && divisor > 0) || (dividend > 0 && divisor < 0)) {
			sign = -1;
		}

		// 将被除数和除数都转为负数（避免溢出问题）
		int dividendNeg = dividend < 0 ? dividend : -dividend;
		int divisorNeg = divisor < 0 ? divisor : -divisor;

		// 计算商
		int result = divideNegative(dividendNeg, divisorNeg);

		return sign * result;
	}

	/**
	 * 计算两个负数的商（结果为正数）
	 *
	 * @param dividend 被除数（负数）
	 * @param divisor  除数（负数）
	 * @return 商（正数）
	 */
	private static int divideNegative(int dividend, int divisor) {
		// 当被除数的绝对值小于除数的绝对值时，商为0
		if (Math.abs(dividend) < Math.abs(divisor)) {
			return 0;
		}

		int result = 0;
		int currentDivisor = divisor;
		int currentMUltiplier = 1;

		// 倍增法加速计算
		// 每次将除数翻倍，同时记录倍数，直到除数的绝对值超过被除数的绝对值
		while (dividend <= currentDivisor && currentDivisor >= Integer.MAX_VALUE / 2) {
			// 将除数翻倍（因为是负数，所以是加上自身）
			currentDivisor += currentDivisor;
			// 将倍数翻倍
			currentMUltiplier += currentMUltiplier;
		}

		// 递归计算剩余部分，并加上当前倍数
		result = currentMUltiplier + divideNegative(dividend - currentDivisor, divisor);
		return result;
	}
}
