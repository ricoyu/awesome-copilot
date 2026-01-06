package com.awesomecopilot.algorithm.leetcode;

/**
 * 二叉树的最小深度
 * <p>
 * 给定一个二叉树，找出其最小深度。 <br/>
 * 最小深度是从根节点到最近叶子节点的最短路径上的节点数量。 <br/>
 * 说明：叶子节点是指没有子节点的节点。 <br/>
 *
 * <pre>
 * 示例 1：
 * 输入：root = [3,9,20,null,null,15,7]
 * 输出：2
 * </pre>
 * <image src="images/ex_depth.jpg" />
 * 示例 2：
 *
 * 输入：root = [2,null,3,null,4,null,5,null,6]
 * 输出：5
 * <p/>
 * Copyright: Copyright (c) 2026-01-06 9:23
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BinaryTreeMinDepth {

	public static void main(String[] args) {
		BinaryTreeMinDepth solution = new BinaryTreeMinDepth();

		// 示例1：root = [3,9,20,null,null,15,7] → 输出2
		TreeNode node9 = new TreeNode(9);
		TreeNode node15 = new TreeNode(15);
		TreeNode node7 = new TreeNode(7);
		TreeNode node20 = new TreeNode(20, node15, node7);
		TreeNode root1 = new TreeNode(3, node9, node20);
		System.out.println("示例1输出：" + solution.minDepth(root1)); // 2

		// 示例2：root = [2,null,3,null,4,null,5,null,6] → 输出5
		TreeNode node6 = new TreeNode(6);
		TreeNode node5 = new TreeNode(5, null, node6);
		TreeNode node4 = new TreeNode(4, null, node5);
		TreeNode node3 = new TreeNode(3, null, node4);
		TreeNode root2 = new TreeNode(2, null, node3);
		System.out.println("示例2输出：" + solution.minDepth(root2)); //5
	}

	/**
	 * 计算二叉树的最小深度
	 * @param root
	 * @return
	 */
	public int minDepth(TreeNode root) {
		// 边界条件1：空树，深度为0
		if (root == null) {
			return 0;
		}
		// 递归计算左右子树的深度
		int leftDepth = minDepth(root.left);
		int rightDepth = minDepth(root.right);

		// 关键逻辑1：左子树为空 → 只能走右子树，深度=右子树深度+当前节点
		if (root.left == null) {
			return rightDepth + 1;
		}

		// 关键逻辑2：右子树为空 → 只能走左子树，深度=左子树深度+当前节点
		if (root.right == null) {
			return leftDepth + 1;
		}

		// 关键逻辑3：左右子树都不为空 → 取最小值+当前节点
		return Math.min(leftDepth, rightDepth) + 1;
	}
}
