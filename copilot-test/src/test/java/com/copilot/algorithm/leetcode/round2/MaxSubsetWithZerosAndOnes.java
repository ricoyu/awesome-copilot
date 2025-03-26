package com.copilot.algorithm.leetcode.round2;

/**
 * 一和零
 * <p/>
 * Copyright: Copyright (c) 2025-03-23 12:10
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxSubsetWithZerosAndOnes {

	public int findMaxForm(String[] strs, int m, int n) {
		int[][] dp = new int[m + 1][n + 1];

		for (String s : strs) {
			int zeros = 0, ones = 0;
			for (char c : s.toCharArray()) {
				if (c == '0') {
					zeros++;
				} else {
					ones++;
				}
			}

			for (int i = m; i >= zeros; i--) {
				for (int j = n; j >= ones; j--) {
					dp[i][j] = Math.max(dp[i][j], dp[i - zeros][j - ones] + 1);
				}
			}

		}
		return dp[m][n];
	}
}
