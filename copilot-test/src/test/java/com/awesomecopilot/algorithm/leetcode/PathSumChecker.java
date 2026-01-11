package com.awesomecopilot.algorithm.leetcode;

/**
 * 路径总和
 * <p>
 * 给你二叉树的根节点 root 和一个表示目标和的整数 targetSum 。判断该树中是否存在 根节点到叶子节点 的路径，这条路径上所有节点值相加等于目标和 targetSum 。如果存在，返回 true ；否则，返回 false 。
 * <p>
 * <pre>
 * 示例 1：
 * 输入：root = [5,4,8,11,null,13,4,7,2,null,null,null,1], targetSum = 22
 * 输出：true
 * 解释：等于目标和的根节点到叶节点路径如上图所示。
 * </pre>
 * <image src="images/demo-tree1.png" width=200, height=180/>
 * <pre>
 * 示例 2：
 * 输入：root = [1,2,3], targetSum = 5
 * 输出：false
 * 解释：树中存在两条根节点到叶子节点的路径：
 * (1 --> 2): 和为 3
 * (1 --> 3): 和为 4
 * 不存在 sum = 5 的根节点到叶子节点的路径。
 * </pre>
 * <image src="images/demo-tree2.png" width=200, height=180/>
 *
 * <ul>核心思路是使用深度优先搜索（DFS） 递归遍历二叉树：
 *     <li/>从根节点开始，每访问一个节点，就用目标值减去当前节点的值，得到剩余需要满足的和。
 *     <li/>递归遍历当前节点的左、右子节点，将剩余和传递下去。
 *     <ul>当遍历到叶子节点时（左、右子节点均为 null），判断剩余和是否等于 0：
 *       <li/>等于 0 说明找到符合条件的路径，返回 true；
 *       <li/>不等于 0 则返回 false。
 *     </ul>
 *     <li/>递归终止条件：若当前节点为 null，说明路径不存在，返回 false。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-07 9:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PathSumChecker {
	public static void main(String[] args) {
		PathSumChecker checker = new PathSumChecker();

		// 示例1构建：root = [5,4,8,11,null,13,4,7,2,null,null,null,1], targetSum = 22
		TreeNode node7 = new TreeNode(7);
		TreeNode node2 = new TreeNode(2);
		TreeNode node11 = new TreeNode(11, node7, node2);
		TreeNode node4Left = new TreeNode(4, node11, null);

		TreeNode node13 = new TreeNode(13);
		TreeNode node1 = new TreeNode(1);
		TreeNode node4Right = new TreeNode(4, null, node1);
		TreeNode node8 = new TreeNode(8, node13, node4Right);

		TreeNode root1 = new TreeNode(5, node4Left, node8);
		System.out.println(checker.hasPathSum(root1, 22)); // 输出：true

		// 示例2构建：root = [1,2,3], targetSum = 5
		TreeNode node2_2 = new TreeNode(2);
		TreeNode node3_2 = new TreeNode(3);
		TreeNode root2 = new TreeNode(1, node2_2, node3_2);
		System.out.println(checker.hasPathSum(root2, 5)); // 输出：false
	}

	public boolean hasPathSum(TreeNode root, int targetSum) {
		// 递归终止条件1：当前节点为null，无路径，返回false
		if (root == null) {
			return false;
		}

		// 递归终止条件2：当前节点是叶子节点（左右子节点都为null）
		if (root.left == null && root.right == null) {
			// 剩余需要满足的和等于当前节点值，说明路径和匹配
			return targetSum == root.val;
		}

		// 递归遍历左子树：目标和减去当前节点值，传递给左子树
		boolean left = hasPathSum(root.left, targetSum - root.val);
		// 递归遍历右子树：目标和减去当前节点值，传递给右子树
		boolean right = hasPathSum(root.right, targetSum - root.val);

		// 左/右子树任意一个存在符合条件的路径，整体就存在
		return left || right;
	}
}
