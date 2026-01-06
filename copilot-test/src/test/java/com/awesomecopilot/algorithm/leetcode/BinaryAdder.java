package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 二进制求和
 * <p>
 * 给你两个二进制字符串 a 和 b ，以二进制字符串的形式返回它们的和
 *
 * <pre>
 * 示例 1：
 *
 * 输入:a = "11", b = "1"
 * 输出："100"
 * </pre>
 * <pre>
 * 示例 2：
 *
 * 输入：a = "1010", b = "1011"
 * 输出："10101"
 * </pre>
 *
 * <ul>采用二进制竖式加法的核心逻辑：
 *     <li/>双指针从两个二进制字符串末尾开始遍历，模拟从低位到高位相加；
 *     <li/>维护进位值 carry，每次计算「a 当前位 + b 当前位 + 进位 carry」的总和；
 *     <li/>当前位结果 = 总和 % 2，新进位 = 总和 / 2；
 *     <li/>遍历结束后，若仍有进位，需追加到结果最前面；
 *     <li/>结果是逆序拼接的，最终反转得到正确二进制和。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-04 9:57
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BinaryAdder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入二进制字符串: ");
		String a = scanner.nextLine();
		System.out.print("请输入二进制字符串: ");
		String b = scanner.nextLine();
		System.out.println(addBinary(a, b));
	}

	/**
	 * 计算两个二进制字符串的和，返回二进制结果字符串
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	private static String addBinary(String a, String b) {
		// 定义StringBuilder高效拼接结果（尾部追加，最后反转）
		StringBuilder result = new StringBuilder();

		// 双指针：分别指向a、b字符串的最后一位（最低位）
		int i = a.length() - 1;
		int j = b.length() - 1;
		// 进位值，初始为0（加法初始无进位）
		int carry = 0;

		// 循环条件：任意字符串未遍历完 或 仍有进位，三者满足其一则继续计算
		while (i >= 0 || j >= 0 || carry != 0) {
			// 取出a的当前位：指针有效则转数字，否则补0（短字符串高位补0）
			int aDigit = i >= 0 ? a.charAt(i) - '0' : 0;
			// 取出b的当前位：规则同a的当前位
			int bDigit = j >= 0 ? b.charAt(j) - '0' : 0;
			// 计算当前位总和 = a位 + b位 + 上一轮进位
			int sum = aDigit + bDigit + carry;
			// 更新进位 = 总和/2（二进制逢2进1）
			carry = sum / 2;
			// 当前位最终值 = 总和%2（二进制逢2取余）
			result.append(sum % 2);
			// 指针左移，处理高位
			i--;
			j--;
		}
		// 结果是逆序拼接的，反转后得到正确的二进制和
		return result.reverse().toString();
	}
}
