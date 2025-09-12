package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 回文数
 * <p>
 * 给你一个整数x ，如果 x 是一个回文整数，返回 true ；否则，返回 false 。
 * <p>
 * 回文数是指正序（从左向右）和倒序（从右向左）读都是一样的整数。 <p>
 * 例如，121 是回文，而 123 不是
 *
 * <pre>
 * 示例 1：
 *
 * 输入：x = 121
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：x = -121
 * 输出：false
 * 解释：从左向右读, 为 -121 。 从右向左读, 为 121- 。因此它不是一个回文数。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：x = 10
 * 输出：false
 * 解释：从右向左读, 为 01 。因此它不是一个回文数。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-09 9:24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PalindromeChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter number: ");
		int num = scanner.nextInt();
		System.out.println(isPalindrome(num));
	}

	/**
	 * 判断一个整数是否为回文数
	 *
	 * @param x 待判断的整数
	 * @return 如果是回文数返回true，否则返回false
	 */
	private static boolean isPalindrome(int x) {
		// 负数不是回文数，末尾为0且本身不为0的数不是回文数
		if (x < 0 || (x % 10 == 0 && x != 0)) {
			return false;
		}

		int reversedHalf = 0;
		// 当原数大于反转得到的数时，继续反转（处理一半数字）
		while (x > reversedHalf) {
			// 取出x的最后一位，加到反转数的末尾
			reversedHalf = reversedHalf * 10 + x % 10;
			// 移除x的最后一位
			x /= 10;
		}

		// 当数字长度为偶数时，x应该等于reversedHalf
		// 当数字长度为奇数时，x应该等于reversedHalf / 10（去掉中间位）
		return x == reversedHalf || x == reversedHalf / 10;
	}
}
