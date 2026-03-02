package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 丑数
 * <p>
 * 丑数 就是只包含质因数 2、3 和 5 的 正 整数。
 * <p>
 * 给你一个整数 n ，请你判断 n 是否为 丑数 。如果是，返回 true ；否则，返回 false 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 6
 * 输出：true
 * 解释：6 = 2 × 3
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 1
 * 输出：true
 * 解释：1 没有质因数。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：n = 14
 * 输出：false
 * 解释：14 不是丑数，因为它包含了另外一个质因数 7 。
 * </pre>
 *
 * <p/>
 * Copyright: Copyright (c) 2026-02-25 9:25
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class UglyNumberChecker {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.println(isUgly(n));
		scanner.close();
	}
	
	/**
	 * 判断一个整数是否为丑数
	 * @param n 待判断的整数
	 * @return 是丑数返回true，否则返回false
	 */
	private static boolean isUgly(int n) {
		// 丑数必须是正整数，小于等于0直接返回false
		if (n <= 0) {
			return false;
		}
		
		// 1是特殊的丑数，没有质因数，直接返回true
		if (n == 1) {
			return true;
		}
		
		// 依次用2、3、5去除n，直到无法整除
		// 先处理质因数2
		while (n % 2 == 0) {
			n /= 2;
		}
		
		// 再处理质因数3
		while (n % 3 == 0) {
			n /= 3;
		}
		
		// 最后处理质因数5
		while (n % 5 == 0) {
			n /= 5;
		}
		
		// 如果最终n等于1，说明所有质因数只有2、3、5，是丑数
		return n == 1;
	}
}