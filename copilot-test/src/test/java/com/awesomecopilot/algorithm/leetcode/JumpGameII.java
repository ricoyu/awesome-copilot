package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;


/**
 * 跳跃游戏 II
 * <p>
 * 给定一个长度为 n 的 0 索引整数数组 nums。初始位置为 nums[0]。
 * <p>
 * 每个元素 nums[i] 表示从索引 i 向前跳转的最大长度。换句话说，如果你在 nums[i] 处，你可以跳转到任意 nums[i + j] 处:
 * <p>
 * 0 <= j <= nums[i] <br/>
 * i + j < n <br/>
 * 返回到达 nums[n - 1] 的最小跳跃次数。生成的测试用例可以到达 nums[n - 1]。
 * <p>
 * 示例 1:
 * <p>
 * 输入: nums = [2,3,1,1,4] <br/>
 * 输出: 2 <br/>
 * 解释: 跳到最后一个位置的最小跳跃数是 2。 <br/>
 * 从下标为 0 跳到下标为 1 的位置，跳 1 步，然后跳 3 步到达数组的最后一个位置。
 * <p>
 * 示例 2:
 * <p>
 * 输入: nums = [2,3,0,1,4] <br/>
 * 输出: 2 <br/>
 * <p>
 * 贪心算法的核心思路是：
 * <ol>
 *     <li/>每次跳跃时，选择能让我们到达最远位置的下一步
 *     <li/>当到达当前跳跃的边界时，必须进行一次新的跳跃，并更新边界为当前能到达的最远位置
 * </ol>
 * <ul>具体实现步骤：
 *     <li/>维护三个变量：当前能到达的最远位置、当前跳跃的边界、跳跃次数
 *     <li/>遍历数组，不断更新当前能到达的最远位置
 *     <li/>当到达当前跳跃边界时，增加跳跃次数并更新边界
 *     <li/>当边界已经覆盖最后一个元素时，即可返回跳跃次数
 * </ul>
 * "当前跳跃的边界" 指的是在当前跳跃步数下，能够到达的最远位置。当我们到达这个边界时，意味着必须进行一次新的跳跃才能继续前进。
 * <p>
 * 我们结合示例 1 来具体解释：
 * 输入: nums = [2,3,1,1,4]
 * <pre>
 * 初始状态：
 * 跳跃次数 = 0
 * 当前边界 (currentEnd) = 0（初始位置，还未进行任何跳跃）
 * 最远可达 (maxReach) = 0 + 2 = 2
 * </pre>
 * <pre>
 * 当 i=0 时：
 * 此时 i 等于当前边界 (0=0)，说明我们已到达当前跳跃的边界
 * 必须进行一次跳跃，跳跃次数变为 1
 * 更新当前边界为最远可达位置 2
 * 现在的含义是：通过 1 次跳跃，我们最远能到达位置 2
 * </pre>
 * <pre>
 * 当 i=1 时：
 * 计算新的最远可达：max (2, 1+3)=4
 * i 不等于当前边界 (1≠2)，不需要跳跃
 * </pre>
 * <pre>
 * 当 i=2 时：
 * i 等于当前边界 (2=2)，到达了第一次跳跃的边界
 * 必须进行第二次跳跃，跳跃次数变为 2
 * 更新当前边界为最远可达位置 4
 * 此时当前边界 (4) 已经等于数组最后一个索引，循环可以结束
 * </pre>
 * <p>
 * Copyright: Copyright (c) 2024-11-09 19:32
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class JumpGameII {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		for (int j = 0; j < 3; j++) {
			System.out.print("请输入数组: ");
			String input = scanner.nextLine().trim();
			String[] parts = input.split(",");
			int[] nums = new int[parts.length];
			for (int i = 0; i < parts.length; i++) {
				nums[i] = Integer.parseInt(parts[i].trim());
			}
			int jumps = jumps(nums);
			System.out.println(jumps);
		}
		scanner.close();
	}

	public static int jumps(int[] nums) {
		// 数组长度为1时，无需跳跃
		if (nums.length == 1) {
			return 0;
		}

		// 当前能到达的最远位置
		int maxReach = 0;
		// 当前跳跃的边界，到达此位置必须进行下一次跳跃
		int currentEnd = 0;
		// 跳跃次数
		int jumps = 0;

		// 遍历数组，注意不需要检查最后一个元素
		for (int i = 0; i < nums.length - 1; i++) {
			// 更新当前能到达的最远位置
			maxReach = Math.max(maxReach, i + nums[i]);

			// 到达当前跳跃的边界，必须进行一次新的跳跃
			if (i == currentEnd) {
				jumps++; // 跳跃次数加1
				currentEnd = maxReach; // 更新边界为当前能到达的最远位置

				// 如果当前边界已经超过或等于最后一个元素，提前结束
				if (currentEnd >= nums.length - 1) {
					break;
				}
			}
		}
		return jumps;
	}
}
