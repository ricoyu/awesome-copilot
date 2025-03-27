package com.awesomecopilot.algorithm.leetcode;

/**
 * 零钱兑换
 * <p>
 * 给你一个整数数组 coins ，表示不同面额的硬币；以及一个整数 amount ，表示总金额。
 * 计算并返回可以凑成总金额所需的 最少的硬币个数 。如果没有任何一种硬币组合能组成总金额，返回 -1 。
 * 你可以认为每种硬币的数量是无限的。
 * <p>
 * 示例 1：
 * <p>
 * 输入：coins = [1, 2, 5], amount = 11
 * 输出：3
 * 解释：11 = 5 + 5 + 1
 * <p>
 * 示例 2：
 * <p>
 * 输入：coins = [2], amount = 3
 * 输出：-1
 * <p>
 * 示例 3：
 * <p>
 * 输入：coins = [1], amount = 0
 * 输出：0
 * <p>
 * 动态规划定义
 * 我们定义 dp[i] 表示凑成金额 i 所需的最少硬币数。我们的目标是求 dp[amount]。
 * <p>
 * 状态转移方程
 * 对于每个金额 i，我们可以通过以下方式来计算 dp[i]：
 * 遍历所有硬币面额 coin，如果 i - coin >= 0，那么 dp[i] = min(dp[i], dp[i - coin] + 1)。
 * <p>
 * 这个方程的意思是：如果我们有一个硬币面额为 coin，那么凑成金额 i 所需的最少硬币数，就是凑成金额 i - coin 所需的最少硬币数加上 1（表示使用了这个硬币）。
 *
 * <ul>初始条件
 *     <li/>dp[0] = 0：凑成金额 0 所需的硬币数为 0。
 *     <li/>对于其他金额 i，初始时 dp[i] = amount + 1，表示一个不可能的值（因为最多需要 amount 个硬币，即全部使用面额为 1 的硬币）。
 * </ul>
 * <p>
 * 最终结果
 * 如果 dp[amount] 仍然等于 amount + 1，说明无法凑成该金额，返回 -1；否则返回 dp[amount]。
 * <p/>
 * Copyright: Copyright (c) 2025-03-25 15:46
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CoinChange {

	public int coinChange(int[] coins, int amount) {
		// 创建一个数组 dp，其中 dp[i] 表示凑成金额 i 所需的最少硬币数
		int[] dp = new int[amount + 1];

		// 初始化 dp 数组，除了 dp[0] = 0，其他都设置为 amount + 1（表示不可能的值）
		for (int i = 1; i < dp.length; i++) {
			dp[i] = amount + 1;
		}

		// 遍历每个金额，从 1 到 amount
		for (int i = 0; i <= amount; i++) {
			// 遍历每个硬币面额
			for (int coin : coins) {
				// 如果 i - coin >= 0，那么 dp[i] = min(dp[i], dp[i - coin] + 1)
				if (i - coin >= 0) {
					dp[i] = Math.min(dp[i], dp[i - coin] + 1);
				}
			}
		}

		// 如果 dp[amount] 仍然等于 amount + 1，说明无法凑成该金额，返回 -1
		// 否则返回 dp[amount]
		return dp[amount] == amount + 1 ? -1 : dp[amount];
	}
}
