package com.copilot.algorithm.leetcode;

/**
 * 一和零 <p/>
 * 给你一个二进制字符串数组 strs 和两个整数 m 和 n 。
 * <p/>
 * 请你找出并返回 strs 的最大子集的长度，该子集中 最多 有 m 个 0 和 n 个 1 。
 * <p/>
 * 如果 x 的所有元素也是 y 的元素，集合 x 是集合 y 的 子集 。
 * <p/>
 * 示例 1：
 * <p/>
 * 输入：strs = ["10", "0001", "111001", "1", "0"], m = 5, n = 3 <br/>
 * 输出：4 <br/>
 * 解释：最多有 5 个 0 和 3 个 1 的最大子集是 {"10","0001","1","0"} ，因此答案是 4 。 <br/>
 * 其他满足题意但较小的子集包括 {"0001","1"} 和 {"10","1","0"} 。{"111001"} 不满足题意，因为它含 4 个 1 ，大于 n 的值 3 。 <br/>
 * <p/>
 * 示例 2：
 * <p/>
 * 输入：strs = ["10", "0", "1"], m = 1, n = 1 <br/>
 * 输出：2 <br/>
 * 解释：最大的子集是 {"0", "1"} ，所以答案是 2 。 <br/>
 * <p/>
 * Copyright: Copyright (c) 2025-03-21 9:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxSubsetWithZerosAndOnes {

	public int findMaxForm(String[] strs, int m, int n) {
		// dp[i][j] 表示最多有 i 个 0 和 j 个 1 的情况下，能够组成的最大子集的长度
		int[][] dp = new int[m + 1][n + 1];

		// 遍历每个字符串
		for (String s : strs) {
			// 计算当前字符串中 0 和 1 的数量
			int zeros = 0, ones = 0;
			for (char c : s.toCharArray()) {
				if (c == '0') {
					zeros++;
				} else {
					ones++;
				}
			}

			// 更新 dp 数组
			// 从后向前更新，避免覆盖
			for (int i = m; i >= zeros ; i--) {
			    for (int j = n; j >=ones ; j--) {
			        dp[i][j] = Math.max(dp[i][j], dp[i - zeros][j - ones] + 1);
			    }
			}
		}

		// 返回最终结果
		return dp[m][n];
	}

	public static void main(String[] args) {
		MaxSubsetWithZerosAndOnes maxSubsetWithZerosAndOnes = new MaxSubsetWithZerosAndOnes();
		System.out.println(maxSubsetWithZerosAndOnes.findMaxForm(new String[]{"10", "0001", "111001", "1", "0"}, 5, 3));
		System.out.println(maxSubsetWithZerosAndOnes.findMaxForm(new String[]{"10", "0", "1"}, 1, 1));
	}
}
