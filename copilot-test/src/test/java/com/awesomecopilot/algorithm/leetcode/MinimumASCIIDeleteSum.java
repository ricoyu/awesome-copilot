package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 两个字符串的最小ASCII删除和
 * <p>
 * 给定两个字符串s1 和 s2，返回 使两个字符串相等所需删除字符的 ASCII 值的最小和 。
 * <p>
 * <pre>
 * 示例 1:
 *
 * 输入: s1 = "sea", s2 = "eat"
 * 输出: 231
 * 解释: 在 "sea" 中删除 "s" 并将 "s" 的值(115)加入总和。
 * 在 "eat" 中删除 "t" 并将 116 加入总和。
 * 结束时，两个字符串相等，115 + 116 = 231 就是符合条件的最小和。
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: s1 = "delete", s2 = "leet"
 * 输出: 403
 * 解释: 在 "delete" 中删除 "dee" 字符串变成 "let"，
 * 将 100[d]+101[e]+101[e] 加入总和。在 "leet" 中删除 "e" 将 101[e] 加入总和。
 * 结束时，两个字符串都等于 "let"，结果即为 100+101+101+101 = 403 。
 * 如果改为将两个字符串转换为 "lee" 或 "eet"，我们会得到 433 或 417 的结果，比答案更大。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-08-12 7:49
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MinimumASCIIDeleteSum {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s1: ");
		String s1 = scanner.nextLine();
		System.out.print("请输入字符串s2: ");
		String s2 = scanner.nextLine();
		System.out.println(minimumDeleteSum(s1, s2));
	}

	/**
	 * 计算使两个字符串相等所需删除字符的最小ASCII值和
	 *
	 * @param s1 第一个字符串
	 * @param s2 第二个字符串
	 * @return 最小ASCII删除和
	 */
	private static int minimumDeleteSum(String s1, String s2) {
		int m = s1.length(), n = s2.length();

		// 创建dp数组，dp[i][j]表示s1前i个字符和s2前j个字符的最小删除和
		int[][] dp = new int[m + 1][n + 1];

		// 边界条件：s2为空字符串，需要删除s1所有字符
		for (int i = 1; i <= m; i++) {
			dp[i][0] = dp[i - 1][0] + s1.charAt(i - 1);
		}

		// 边界条件：s1为空字符串，需要删除s2所有字符
		for (int j = 1; j <= n; j++) {
			dp[0][j] = dp[0][j - 1] + s2.charAt(j - 1);
		}

		// 填充dp数组
		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				// 当前字符相等，不需要删除
				if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					dp[i][j] = dp[i - 1][j - 1];
				} else {
					// 当前字符不等，选择删除s1的字符或s2的字符，取较小值
					dp[i][j] = Math.min(
							dp[i - 1][j] + s1.charAt(i - 1), // 删除s1的当前字符
							dp[i][j - 1] + s2.charAt(j - 1)  // 删除s2的当前字符
					);

				}
			}
		}

		return dp[m][n];
	}
}
