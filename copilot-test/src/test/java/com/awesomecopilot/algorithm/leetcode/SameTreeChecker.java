package com.awesomecopilot.algorithm.leetcode;

/**
 * 相同的树
 * <p>
 * 给你两棵二叉树的根节点 p 和 q ，编写一个函数来检验这两棵树是否相同。
 * <p>
 * 如果两个树在结构上相同，并且节点具有相同的值，则认为它们是相同的。
 *
 * <p/>
 * Copyright: Copyright (c) 2025-12-10 9:21
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SameTreeChecker {

	// 定义二叉树节点结构
	static class TreeNode {
		int val;
		TreeNode left;
		TreeNode right;
		TreeNode() {}
		TreeNode(int val) { this.val = val; }
		TreeNode(int val, TreeNode left, TreeNode right) {
			this.val = val;
			this.left = left;
			this.right = right;
		}
	}

	public boolean isSameTree(TreeNode p, TreeNode q) {
		// 终止条件1：两个节点都为空 → 结构相同
		if (p == null && q == null) {
			return true;
		}

		// 终止条件2：一个为空、一个非空 → 结构不同
		if (p == null || q == null) {
			return false;
		}

		// 终止条件3：节点值不同 → 内容不同
		if (p.val != q.val) {
			return false;
		}

		// 递归比较左子树 && 递归比较右子树（需同时满足）
		return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
	}


	public static void main(String[] args) {
		SameTreeChecker checker = new SameTreeChecker();

		// 示例1：两棵完全相同的树
		TreeNode p1 = new TreeNode(1, new TreeNode(2), new TreeNode(3));
		TreeNode q1 = new TreeNode(1, new TreeNode(2), new TreeNode(3));
		System.out.println("示例1结果：" + checker.isSameTree(p1, q1)); // 输出 true

		// 示例2：结构相同但值不同
		TreeNode p2 = new TreeNode(1, new TreeNode(2), null);
		TreeNode q2 = new TreeNode(1, new TreeNode(3), null);
		System.out.println("示例2结果：" + checker.isSameTree(p2, q2)); // 输出 false

		// 示例3：结构不同
		TreeNode p3 = new TreeNode(1, new TreeNode(2), null);
		TreeNode q3 = new TreeNode(1, null, new TreeNode(2));
		System.out.println("示例3结果：" + checker.isSameTree(p3, q3)); // 输出 false
	}
}
