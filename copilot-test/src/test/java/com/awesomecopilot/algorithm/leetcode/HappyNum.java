package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 快乐数
 * <p>
 * 编写一个算法来判断一个数 n 是不是快乐数。
 * <p>
 * <ul>「快乐数」 定义为:
 *     <li/>对于一个正整数，每一次将该数替换为它每个位置上的数字的平方和。
 *     <li/>然后重复这个过程直到这个数变为 1，也可能是 无限循环 但始终变不到 1。
 *     <li/>如果这个过程 结果为 1，那么这个数就是快乐数。
 *     <li/>如果 n 是 快乐数 就返回 true ；不是，则返回 false 。
 * </ul>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 19
 * 输出：true
 * 解释：
 * 1^2 + 9^2 = 82
 * 8^2 + 2^2 = 68
 * 6^2 + 8^2 = 100
 * 1^2 + 0^2 + 0^2 = 1
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 2
 * 输出：false
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-01-24 9:52
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HappyNum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.println(isHappy(n));
		scanner.close();
	}

	private static boolean isHappy(int n) {
		int temp = 0;
		while (n / 10 != 0) {
			int n1 = n % 10;
			temp += n1 * n1;
			n = n / 10;
			if (n < 10) {
				temp += n * n;
			}
			n = temp;
		}
		return (temp == 1);
	}
}
