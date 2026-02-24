package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 各位相加
 * <p>
 * 给定一个非负整数 num，反复将各个位上的数字相加，直到结果为一位数。返回这个结果。
 *
 * <pre>
 * 示例 1:
 *
 * 输入: num = 38
 * 输出: 2
 * 解释: 各位相加的过程为：
 * 38 --> 3 + 8 --> 11
 * 11 --> 1 + 1 --> 2
 * 由于 2 是一位数，所以返回 2。
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: num = 0
 * 输出: 0
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-02-23 10:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DigitAdder {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.println(addDigits(n));
		scanner.close();
	}
	
	private static int addDigits(int n) {
		while (n > 9) {
			int num = 0;
			char[] array = String.valueOf(n).toCharArray();
			int i = 0;
			int j = array.length - 1;
			while (i < j) {
				num += array[i] - '0' + array[j] - '0';
				i++;
				j--;
			}
			if (i == j) {
				num += array[i] - '0';
			}
			n = num;
		}
		return n;
	}
}