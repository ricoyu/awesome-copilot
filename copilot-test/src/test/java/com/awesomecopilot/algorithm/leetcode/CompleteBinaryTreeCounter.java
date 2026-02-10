package com.awesomecopilot.algorithm.leetcode;

/**
 * 完全二叉树的节点个数
 * <p>
 * 给你一棵 完全二叉树 的根节点 root ，求出该树的节点个数。
 * <p>
 * 完全二叉树 的定义如下：在完全二叉树中，除了最底层节点可能没填满外，其余每层节点数都达到最大值，并且最下面一层的节点都集中在该层最左边的若干位置。
 * 若最底层为第 h 层（从第 0 层开始），则该层包含 1~ 2h 个节点。
 * <image src="images/complete.jpg" width=200, height=180/>
 * <pre>
 * 输入：root = [1,2,3,4,5,6]
 * 输出：6
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：root = []
 * 输出：0
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：root = [1]
 * 输出：1
 * </pre>
 *
 * <p/>
 * Copyright: Copyright (c) 2026-02-05 8:35
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CompleteBinaryTreeCounter {

	/**
	 * 核心方法：计算完全二叉树的节点个数
	 * @param root 完全二叉树的根节点
	 * @return 节点总数
	 */
	public static int countNodes(TreeNode root) {
		// 递归终止条件：节点为空，返回0
		if (root == null) {
			return 0;
		}

		// 计算当前节点的左深度（仅向左遍历）
		int leftDepth = getLeftDepth(root);
		// 获取当前节点的右深度
		 int rightDepth = getRightDepth(root);

		// 如果左右深度相等，说明是满二叉树，直接用公式计算
		if (leftDepth == rightDepth) {
			// 2^depth - 1：用位运算代替幂运算，效率更高（1 << depth 等价于 2^depth）
			return (1 << leftDepth) - 1;
		} else {
			// 否则递归计算左子树 + 右子树 + 当前节点（1）
			return 1 + countNodes(root.left) + countNodes(root.right);
		}
	}

	/**
	 * 计算节点的左深度（从当前节点一直向左走，统计层数）
	 * @param root 起始节点
	 * @return 左深度
	 */
	private static int getLeftDepth(TreeNode root) {
		int depth = 0;
		while (root != null) {
			depth++;
			root = root.left;
		}
		return depth;
	}

	/**
	 * 计算节点的右深度（从当前节点一直向右走，统计层数）
	 * @param root 起始节点
	 * @return 右深度
	 */
	private static int getRightDepth(TreeNode root) {
		int depth = 0;
		while (root != null) {
			depth++;
			root = root.right;
		}
		return depth;
	}
}
