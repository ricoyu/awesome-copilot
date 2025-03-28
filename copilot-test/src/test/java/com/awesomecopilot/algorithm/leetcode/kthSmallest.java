package com.awesomecopilot.algorithm.leetcode;

/**
 * 二叉搜索树中第 K 小的元素
 * <p>
 * 给定一个二叉搜索树的根节点 root ，和一个整数 k ，请你设计一个算法查找其中第 k 小的元素（从 1 开始计数）。
 * <p>
 *
 * <p/>
 * Copyright: Copyright (c) 2025-03-28 8:36
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class kthSmallest {

	private int count;

	private int result;

	public int kthSmallest(TreeNode root, int k) {
		count=k;
		result=0;
		inorderTraversal(root);
		return result;
	}

	public void inorderTraversal(TreeNode node) {
		// 当前节点为空或已找到结果时，直接返回
		if (node == null || result != 0) {
			return;
		}
		inorderTraversal(node.left);
		// 处理当前节点：减少计数，若计数为0则记录结果
		count--;
		if (count==0) {
			result = node.val;
			return;// 提前终止后续遍历
		}

		// 遍历右子树
		inorderTraversal(node.right);
	}
}
