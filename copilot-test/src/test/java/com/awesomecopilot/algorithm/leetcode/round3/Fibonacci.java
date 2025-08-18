package com.awesomecopilot.algorithm.leetcode.round3;

import java.util.Scanner;

/**
 * 斐波那契数 <p/>
 * <p>
 * 斐波那契数 （通常用 F(n) 表示）形成的序列称为 斐波那契数列 。该数列由 0 和 1 开始，后面的每一项数字都是前面两项数字的和。也就是：
 * <p/>
 * F(0) = 0，F(1) = 1 <br/>
 * F(n) = F(n - 1) + F(n - 2)，其中 n > 1 <p/>
 * 给定 n ，请计算 F(n) 。
 * <p/>
 * <p>
 * 示例 1：
 * <p/>
 * 输入：n = 2 <br/>
 * 输出：1 <br/>
 * 解释：F(2) = F(1) + F(0) = 1 + 0 = 1 <p/>
 * <p>
 * 示例 2：
 * <p/>
 * 输入：n = 3 <br/>
 * 输出：2 <br/>
 * 解释：F(3) = F(2) + F(1) = 1 + 1 = 2
 * <p/>
 * 示例 3：
 * <p/>
 * 输入：n = 4 <br/>
 * 输出：3 <br/>
 * 解释：F(4) = F(3) + F(2) = 2 + 1 = 3  <br/>
 * <p/>
 * 为了计算斐波那契数列中的第 n 个数字，有几种常见的方法，包括递归、动态规划和空间优化的动态规划。
 * 在这里，我们将使用空间优化的动态规划方法，这种方法时间复杂度是 O(n)，但空间复杂度减少到 O(1)。
 * <p/>
 * 空间优化动态规划的原理:
 * <ol>
 *     <li/>初始化前两个斐波那契数：F(0) = 0 和 F(1) = 1。
 *     <li/>使用一个循环从 2 到 n 计算每个斐波那契数。
 *     <li/>每次循环更新两个变量来存储最新的两个斐波那契数，这两个变量在每次循环结束时向前移动。
 *     <li/>最终的斐波那契数即为所求。
 * </ol>
 * 实现步骤:
 * <ol>
 *     <li/>初始化前两个斐波那契数：F(0) = 0 和 F(1) = 1。
 *     <li/>使用一个循环从 2 到 n 计算每个斐波那契数。
 *     <li/>使用一个循环从 2 到 n 计算每个斐波那契数。
 *     <li/>每次循环更新两个变量来存储最新的两个斐波那契数，这两个变量在每次循环结束时向前移动。
 *     <li/>最终的斐波那契数即为所求。
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-07-31 9:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class Fibonacci {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		for (int i = 0; i < 5; i++) {
			System.out.print("请输入n: ");
			int n = scanner.nextInt();
			System.out.println(fib(n));
		}
		scanner.close();
	}

	public static int fib(int n) {
		if (n <= 1) {
			return n;
		}
		int prev1 = 0;
		int prev2 = 1;
		int current = 0;
		for (int i = 2; i <= n; i++) {
			current = prev1 + prev2;
			prev1 = prev2;
			prev2 = current;
		}
		return current;
	}
}
