package com.awesomecopilot.algorithm.leetcode;

/**
 * x 的平方根
 * 给你一个非负整数 x ，计算并返回 x 的 算术平方根 。
 * <p>
 * 由于返回类型是整数，结果只保留整数部分, 小数部分将被舍去 。
 * 注意：不允许使用任何内置指数函数和算符, 例如 pow(x, 0.5) 或者 x ** 0.5 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：x = 4
 * 输出：2
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：x = 8
 * 输出：2
 * 解释：8 的算术平方根是 2.82842..., 由于返回类型是整数，小数部分将被舍去。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-08-26 18:00
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SqrtCalculator {

	public static void main(String[] args) {
		System.out.println(mySqrt(2147395600));
	}

	public static int mySqrt(int x) {
		int result = 0;
		int pow = 0;
		System.out.println(pow * pow);
		while (pow * pow <= x) {
			result = (int)pow;
			pow++;
		}
		return result;
	}
}
