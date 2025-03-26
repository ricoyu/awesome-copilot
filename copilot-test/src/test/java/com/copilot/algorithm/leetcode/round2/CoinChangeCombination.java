package com.copilot.algorithm.leetcode.round2;

/**
 * 零钱兑换
 * <p>
 * 给你一个整数数组 coins 表示不同面额的硬币，另给一个整数 amount 表示总金额。
 * <p>
 * 请你计算并返回可以凑成总金额的硬币组合数。如果任何硬币组合都无法凑出总金额，返回 0 。
 * <p>
 * 假设每一种面额的硬币有无限个。
 * <p>
 * 题目数据保证结果符合 32 位带符号整数。
 * <p>
 * 这个问题是一个经典的“零钱兑换”问题，属于动态规划的范畴。我们需要计算用给定的硬币面额凑成指定金额的组合数。由于每种硬币的数量是无限的，因此这是一个完全背包问题。
 * <p>
 * 动态规划思路
 * 我们可以使用动态规划来解决这个问题。定义一个数组 dp，其中 dp[i] 表示凑成金额 i 的组合数。我们的目标是计算 dp[amount]。
 * <p>
 * 动态规划方程
 *
 * <ul>对于每个金额 i，我们可以通过以下方式更新 dp[i]：
 *     <li/>对于每个硬币面额 coin，如果 i >= coin，那么 dp[i] 可以加上 dp[i - coin] 的值。这是因为如果我们可以用 coin 来凑成 i - coin 的金额，那么加上这个 coin
 *     就可以凑成 i 的金额。
 *     <li/>因此，动态规划方程为：dp[i] = dp[i] + dp[i - coin]  (对于每个 coin 且 i >= coin)
 *     <li/>这个方程的意思是，凑成金额 i 的组合数等于不使用当前硬币的组合数 dp[i] 加上使用当前硬币的组合数 dp[i - coin]。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-03-25 14:49
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CoinChangeCombination {

	public int coinChange(int[] coins, int amount) {
		// dp[i] 表示凑成金额 i 的组合数
		int[] dp = new int[amount + 1];

		// 初始化：凑成金额 0 的组合数为 1（即不使用任何硬币）
		dp[0] = 1;

		for (int coin : coins) {
			// 对于每个硬币，更新 dp 数组
			for (int i = coin; i <= amount; i++) {
				dp[i] += dp[i - coin];
			}
		}

		return dp[amount];
	}

	public static void main(String[] args) {
		CoinChangeCombination solution = new CoinChangeCombination();
		int[] coins = {1, 2, 5};
		int amount = 11;
		int result = solution.coinChange(coins, amount);
		System.out.println("凑成金额 " + amount + " 的硬币组合数为: " + result);
	}
}
