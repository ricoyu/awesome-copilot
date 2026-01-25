package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 颠倒二进制位
 * <p>
 * 颠倒给定的 32 位有符号整数的二进制位。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：n = 43261596
 * 输出：964176192
 *
 * 解释：
 *
 * 整数  二进制
 * 43261596  00000010100101000001111010011100
 * 964176192 00111001011110000010100101000000
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 2147483644
 * 输出：1073741822
 *
 * 解释：
 *
 * 整数  二进制
 * 2147483644  01111111111111111111111111111100
 * 1073741822  00111111111111111111111111111110
 * </pre>
 *
 * <p/>
 * Copyright: Copyright (c) 2026-01-22 9:22
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ReverseBits {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入数字：");
		int n = scanner.nextInt();
		System.out.println(reverseBits(n));
		scanner.close();
	}

	private static int reverseBits(int n) {
		// 初始化结果为0，存储颠倒后的二进制数
		int result = 0;

		// 循环32次，处理每一位（因为是32位整数）
		for (int i = 0; i < 32; i++) {
			// 1. 将result左移1位，为新的位腾出最低位空间
			// 例如：result=010（十进制2），左移后变为100（十进制4）
			result <<= 1;

			// 2. 取出n的最低位（n&1），并加到result的最低位
			// n&1：如果n的最低位是1，结果为1；否则为0
			// 例如：n=101（十进制5），n&1=1；n=100（十进制4），n&1=0
			result |= (n & 1);

			// 3. 将n右移1位，去掉已经处理过的最低位
			// 例如：n=101（十进制5），右移后变为10（十进制2）
			n >>>=1; // 使用无符号右移，避免符号位干扰（负数高位补0）
		}

		return result;
	}
}
