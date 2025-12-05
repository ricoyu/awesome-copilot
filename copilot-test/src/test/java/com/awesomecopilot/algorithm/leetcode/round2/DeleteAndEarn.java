package com.awesomecopilot.algorithm.leetcode.round2;

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
 * <p>
 * 只有你「主动选择删除」的那个元素能拿分,「被动连带删除」的 nums[i]-1 和 nums[i]+1 元素, 是没有点数的！
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
 * Copyright: Copyright (c) 2025-11-27 16:23
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

	private static int deleteAndEarn(int[] nums) {
		return 0;

	}
}
