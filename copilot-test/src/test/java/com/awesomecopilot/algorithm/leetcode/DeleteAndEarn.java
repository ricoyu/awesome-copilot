package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 删除并获得点数
 * <p>
 * 给你一个整数数组 nums, 你可以对它进行一些操作。
 * <p>
 * 每次操作中, 选择任意一个 nums[i], 删除它并获得 nums[i] 的点数。之后, 你必须删除 所有 等于 nums[i] - 1 和 nums[i] + 1 的元素。
 * <p>
 * 开始你拥有 0 个点数。返回你能通过这些操作获得的最大点数。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [3,4,2]
 * 输出：6
 * </pre>
 * 解释： <p>
 * 删除 4 获得 4 个点数，因此 3 也被删除。 <p>
 * 之后，删除 2 获得 2 个点数。总共获得 6 个点数。
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [2,2,3,3,3,4]
 * 输出：9
 * </pre>
 * 解释： <p>
 * 删除 3 获得 3 个点数，接着要删除两个 2 和 4 。 <p>
 * 之后，再次删除 3 获得 3 个点数，再次删除 3 获得 3 个点数。 <p>
 * 总共获得 9 个点数。
 * <p/>
 * <ul>核心思路：
 *     <li/>统计每个数字的总点数：将数组转化为每个数字对应的总得分。例如，如果数组中有三个 3，那么 earn[3] = 9。
 *     <li/>转化为“不能选相邻元素的最大和”问题：如果选了 x，就不能选 x - 1 和 x + 1，所以这是典型的“不能选择相邻数字的最大奖励”问题。
 *     <li/>dp[i] = max(dp[i - 1], dp[i - 2] + earn[i]) 不是dp[i + 2] + earn[i]的原因是动态规划都是从前往后算, 所以不能从后往前遍历。
 *     <li/>
 * </ul>
 * 题目说“删除所有 x - 1 和 x + 1”，但你并不会获得它们的点数，只是被迫移除它们。所以：
 * 等价于：选了 x，就不能选 x - 1 和 x + 1
 * 这就变成了一个选择互不相邻数字的最大得分问题 —— 这正是打家劫舍问题的本质！
 * Copyright: Copyright (c) 2025-08-02 9:35
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DeleteAndEarn {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组, 逗号分隔: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(deleteAndEarn(nums));
	}

	public static int deleteAndEarn(int[] nums) {
		// 边界条件处理
		if (nums == null || nums.length == 0) return 0;

		// 找出数组中的最大值，用来定义 earn 数组的大小
		int max = Arrays.stream(nums).max().getAsInt();

		// earn[i] 表示数字 i 总共能获得的点数（i 出现了多少次 * i）
		int[] earn = new int[max + 1];
		for (int num : nums) {
			earn[num] += num;
		}

		// 动态规划数组 dp[i] 表示考虑前 i 个数字时，能获得的最大点数
		int[] dp = new int[max + 1];
		dp[0] = 0;
		dp[1] = earn[1];

		// 从2开始遍历，套用打家劫舍的状态转移公式
		for (int i = 2; i <= max; i++) {
		    dp[i] = Math.max(dp[i - 1], dp[i - 2] + earn[i]);
		}

		return dp[max];
	}
}
