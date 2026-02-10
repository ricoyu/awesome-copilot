package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 2 的幂
 * <p>
 * 给你一个整数 n，请你判断该整数是否是 2 的幂次方。如果是，返回 true ；否则，返回 false 。
 * <p>
 * 如果存在一个整数 x 使得 n == 2x ，则认为 n 是 2 的幂次方。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 1
 * 输出：true
 * 解释：20 = 1
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 16
 * 输出：true
 * 解释：24 = 16
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：n = 3
 * 输出：false
 * </pre>
 * <p>
 * 提示：
 * <p>
 * -231 <= n <= 231 - 1
 * <p>
 * <p>
 * 进阶：你能够不使用循环/递归解决此问题吗？
 *
 * <ul>核心解题思路
 *     <li/>2 的幂次方的整数在二进制表示中有一个显著特征：只有 1 个二进制位是 1，其余都是 0（例如：1 (0b1)、2 (0b10)、4 (0b100)、8 (0b1000)）。
 *     <li/>利用这个特征可以推导出位运算技巧：对于正整数 n，若 n 是 2 的幂，则 n & (n - 1) = 0。
 *     <li/>例：16 (0b10000)，16-1=15 (0b01111)，16 & 15 = 0；
 *     <li/>反例：3 (0b11)，3-1=2 (0b10)，3 & 2 = 2 ≠ 0。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-02-10 9:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PowerOfTwoChecker {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入整数n: ");
		int n = scanner.nextInt();
		System.out.println(isPowerOfTwo(n));
		scanner.close();
	}
	
	private static boolean isPowerOfTwo(int n) {
		if (n < 0) {
			return false;
		}
		if (n == 0) {
			return false;
		}
		return (n & (n - 1)) == 0;
	}
}