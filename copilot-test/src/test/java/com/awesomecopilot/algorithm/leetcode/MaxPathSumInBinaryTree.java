package com.awesomecopilot.algorithm.leetcode;

/**
 * 二叉树中的最大路径和
 * <p/>
 * 二叉树中的 路径 被定义为一条节点序列，序列中每对相邻节点之间都存在一条边。同一个节点在一条路径序列中 至多出现一次 。该路径 至少包含一个 节点，且不一定经过根节点。
 * <p/>
 * 路径和 是路径中各节点值的总和。
 * <p/>
 * 给你一个二叉树的根节点 root ，返回其 最大路径和 。
 * <p/>
 * 示例 1：<br/>
 * 输入：root = [1,2,3]：<br/>
 * 输出：6：<br/>
 * 解释：最优路径是 2 -> 1 -> 3 ，路径和为 2 + 1 + 3 = 6：
 * <p/>
 * 示例 2：：<br/>
 * 输入：root = [-10,9,20,null,null,15,7]：<br/>
 * 输出：42：<br/>
 * 解释：最优路径是 15 -> 20 -> 7 ，路径和为 15 + 20 + 7 = 42
 * <p/>
 * <ul>我们可以使用递归的方法来解决这个问题。对于每个节点，我们需要计算以下两个值：
 *     <li/>以当前节点为起点的最大路径和：这个路径可以向左子树或右子树延伸，但不能同时向左右子树延伸（因为路径不能分叉）。
 *     <li/>经过当前节点的最大路径和：这个路径可以经过当前节点，并向左右子树延伸。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-03-15 15:10
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxPathSumInBinaryTree {

	private int maxSum = Integer.MIN_VALUE;

	// 主函数，返回最大路径和
	public int maxPathSum(TreeNode root) {
		// 调用递归函数
		maxGain(root);
		// 返回最大路径和
		return maxSum;
	}

	// 递归函数，返回以当前节点为起点的最大路径和
	private int maxGain(TreeNode root) {
		// 如果节点为空，返回0
		if (root == null) {
			return 0;
		}

		// 递归计算左子树的最大路径和，如果为负数则取0（表示不选择左子树）
		int leftGain = Math.max(maxGain(root.left), 0);

		// 递归计算右子树的最大路径和，如果为负数则取0（表示不选择右子树）
		int rightGain = Math.max(maxGain(root.right), 0);

		// 计算经过当前节点的最大路径和
		int priceNewPath = root.val + leftGain + rightGain;

		// 更新全局最大路径和
		maxSum = Math.max(maxSum, priceNewPath);

		// 返回以当前节点为起点的最大路径和（只能选择左子树或右子树中的一个）
		return root.val + Math.max(leftGain, rightGain);
	}

	public static void main(String[] args) {
		TreeNode root = new TreeNode(1);
		root.left = new TreeNode(2);
		root.right = new TreeNode(3);
		MaxPathSumInBinaryTree solution = new MaxPathSumInBinaryTree();
		int maxPathSum = solution.maxPathSum(root);
		System.out.println(maxPathSum); // 6

		// 构建示例2的二叉树
		TreeNode root2 = new TreeNode(-10);
		root2.left = new TreeNode(9);
		root2.right = new TreeNode(20);
		root2.right.left = new TreeNode(15);
		root2.right.right = new TreeNode(7);

		System.out.println(solution.maxPathSum(root2)); // 输出: 42
	}
}
