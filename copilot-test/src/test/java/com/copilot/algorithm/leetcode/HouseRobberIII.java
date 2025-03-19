package com.copilot.algorithm.leetcode;

/**
 * 小偷又发现了一个新的可行窃的地区。这个地区只有一个入口，我们称之为 root 。
 * <p/>
 * 除了 root 之外，每栋房子有且只有一个“父“房子与之相连。一番侦察之后，聪明的小偷意识到“这个地方的所有房屋的排列类似于一棵二叉树”。 如果 两个直接相连的房子在同一天晚上被打劫 ，房屋将自动报警。
 * <p/>
 * 给定二叉树的 root 。返回 在不触动警报的情况下 ，小偷能够盗取的最高金额 。
 * <p/>
 * <ul>示例 1:
 *     <li/>输入: root = [3,2,3,null,3,null,1]
 *     <li/>输出: 7
 *     <li/>解释: 小偷一晚能够盗取的最高金额 3 + 3 + 1 = 7
 * </ul>
 * <ul>示例 2:
 *     <li/>输入: root = [3,4,5,1,3,null,1]
 *     <li/>输出: 9
 *     <li/>解释: 小偷一晚能够盗取的最高金额 4 + 5 = 9
 * </ul>
 *
 * 这个问题可以看作是一个树形结构的动态规划问题。我们需要在二叉树中选择一些节点，使得这些节点之间没有直接相连的边（即不能同时选择父节点和子节点），并且这些节点的值之和最大。
 *
 * <b>动态规划思路:</b>
 * <ul>我们可以使用动态规划来解决这个问题。对于每个节点，我们有两个选择：
 *     <li/>选择当前节点：那么我们不能选择它的子节点，但可以选择它的孙子节点。
 *     <li/>不选择当前节点：那么我们可以选择它的子节点。
 * </ul>
 * <ul>我们可以为每个节点维护两个值：
 *     <li/>dp[0]：表示不选择当前节点时，以当前节点为根的子树能够获得的最大金额。
 *     <li/>dp[1]：表示选择当前节点时，以当前节点为根的子树能够获得的最大金额。
 * </ul>
 * <p/>
 * <ul>对于每个节点，我们可以得到以下动态规划方程：
 *     <li/>如果选择当前节点，那么最大金额为当前节点的值加上不选择其左右子节点的最大金额。 dp[1] = node.val + left[0] + right[0];
 *     <li/>如果不选择当前节点，那么最大金额为选择或不选择其左右子节点的最大值之和。dp[0] = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);
 * </ul>
 * Copyright: Copyright (c) 2025-03-15 11:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HouseRobberIII {

		public int rob(TreeNode root) {
			int[] result = robHelper(root);
			return Math.max(result[0], result[1]);
		}

		// 辅助函数，返回一个数组，数组的第一个元素表示不选择当前节点的最大金额，第二个元素表示选择当前节点的最大金额
		private int[] robHelper(TreeNode root) {
			if (root == null) {
				return new int[]{0, 0};
			}

			// 递归计算左右子节点的最大金额
			int[] left = robHelper(root.left);
			int[] right = robHelper(root.right);

			// 不选择当前节点的情况
			int notRobbed = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);
			// 选择当前节点的情况
			int robbed = root.val + left[0] + right[0];

			return new int[]{notRobbed, robbed};
		}
}
