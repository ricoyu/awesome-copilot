package com.copilot.algorithm.leetcode;

/**
 * 组合总和 Ⅳ
 * <p/>
 * 给你一个由 不同 整数组成的数组 nums ，和一个目标整数 target 。请你从 nums 中找出并返回总和为 target 的元素组合的个数。
 * 题目数据保证答案符合 32 位整数范围。
 * <p/>
 * 示例 1：
 * <p/>
 * 输入：nums = [1,2,3], target = 4 <br/>
 * 输出：7 <br/>
 * 解释： <p/>
 * 所有可能的组合为：
 * (1, 1, 1, 1) <br/>
 * (1, 1, 2) <br/>
 * (1, 2, 1) <br/>
 * (1, 3) <br/>
 * (2, 1, 1) <br/>
 * (2, 2) <br/>
 * (3, 1) <br/>
 * 请注意，顺序不同的序列被视作不同的组合。
 * <p/>
 * 示例 2：
 * <p/>
 * 输入：nums = [9], target = 3 <br/>
 * 输出：0
 * <p/>
 *
 * 这个问题可以使用动态规划（Dynamic Programming, DP）来解决。我们可以定义一个数组 dp，其中 dp[i] 表示和为 i 的组合数。我们的目标是计算 dp[target]。
 *
 * <ul>动态规划思路
 *     <ul>定义状态：
 *       <li/>dp[i] 表示和为 i 的组合数。
 *     </ul>
 *     <ul>状态转移方程：
 *      <li/>对于每一个 i，我们可以通过遍历数组 nums 中的每一个数 num，如果 i - num >= 0，那么 dp[i] += dp[i - num]。
 *      <li/>这个方程的意思是，如果我们已经知道和为 i - num 的组合数，那么在这些组合的末尾加上 num，就可以得到和为 i 的组合。
 *     </ul>
 *     <ul>初始条件：
 *      <li/>dp[0] = 1，因为和为 0 的组合只有一种，即空组合。
 *     </ul>
 *     <ul>最终结果：
 *          <li/>dp[target] 就是我们要求的结果。
 *     </ul>
 * </ul>
 * <ul>举例说明
 *     <li/>假设 nums = [1, 2, 3]，target = 4。
 *     <ul>假设 nums = [1, 2, 3]，target = 4。
 *         <li/>dp[0] = 1（空组合）
 *         <li/>dp[1] = dp[0] = 1（只有 [1]）
 *         <li/>dp[2] = dp[1] + dp[0] = 1 + 1 = 2（[1,1] 和 [2]）
 *         <li/>dp[3] = dp[2] + dp[1] + dp[0] = 2 + 1 + 1 = 4（[1,1,1], [1,2], [2,1], [3]）
 *         <li/>dp[4] = dp[3] + dp[2] + dp[1] = 4 + 2 + 1 = 7（[1,1,1,1], [1,1,2], [1,2,1], [2,1,1],[2,2], [1,3], [3,1]）
 *     </ul>
 * </ul>
 * Copyright: Copyright (c) 2025-03-20 8:31
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CombinationSumIV {

	public int combinationSum4(int[] nums, int target) {
		// 创建一个大小为 target + 1 的数组 dp
		int[] dp = new int[target + 1];

		// 初始条件：和为 0 的组合数为 1（空组合）
		dp[0] = 1;

		// 遍历从 1 到 target 的所有可能和
		for (int i = 1; i <= target; i++) {
			// 对于每一个和 i，遍历数组中的每一个数 num
			for (int num : nums) {
				// 如果 i - num >= 0，说明可以通过在 dp[i - num] 的组合末尾加上 num 得到 dp[i]
				if (i-num >=0) {
					dp[i] += dp[i-num];
				}
			}
		}

		return dp[target];
	}

	public static void main(String[] args) {
		CombinationSumIV solution = new CombinationSumIV();
		int[] nums = { 1, 2, 3 };
		int target = 4;
		System.out.println(solution.combinationSum4(nums, target));
	}
}
