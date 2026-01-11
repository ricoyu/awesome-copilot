package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 二叉树的中序遍历
 * <p>
 * 给定一个二叉树的根节点 root ，返回 它的 中序 遍历 。
 * <p>
 * <pre>
 * 示例 1：
 * 输入：root = [1,null,2,3]
 * 输出：[1,3,2]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：root = []
 * 输出：[]
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：root = [1]
 * 输出：[1]
 * </pre>
 * <ul>解题思路
 *     <li/>递归法：利用递归的栈特性，先递归遍历左子树，再记录当前节点值，最后递归遍历右子树，逻辑简单直观。
 *     <li/>迭代法：手动借助栈模拟递归过程，先将所有左子节点入栈，弹出栈顶节点并记录值后，再处理其右子树，避免递归的栈溢出风险。
 *     <li/>
 *     <li/>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-09 8:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BinaryTreeInorderTraversal {

	// 测试示例
	public static void main(String[] args) {
		BinaryTreeInorderTraversal solution = new BinaryTreeInorderTraversal();

		// 示例1：构建树 [1,null,2,3]
		TreeNode root1 = new TreeNode(1);
		root1.right = new TreeNode(2);
		root1.right.left = new TreeNode(3);
		// 递归遍历结果
		System.out.println("递归遍历示例1结果：" + solution.inorderTraversalRecursive(root1)); // 输出 [1,3,2]
		// 迭代遍历结果
		//System.out.println("迭代遍历示例1结果：" + solution.inorderTraversalIterative(root1)); // 输出 [1,3,2]

		// 示例2：空树
		TreeNode root2 = null;
		System.out.println("递归遍历示例2结果：" + solution.inorderTraversalRecursive(root2)); // 输出 []

		// 示例3：单节点树 [1]
		TreeNode root3 = new TreeNode(1);
		System.out.println("递归遍历示例3结果：" + solution.inorderTraversalRecursive(root3)); // 输出 [1]
	}

	/**
	 * 递归辅助方法，封装遍历逻辑
	 * @param root
	 * @return
	 */
	private List<Integer> inorderTraversalRecursive(TreeNode root) {
		List<Integer> result = new ArrayList<>();
		inorderRecursiveHelper(root, result);
		return result;
	}

	/**
	 * 递归辅助方法，封装遍历逻辑
	 * @param node
	 * @param result
	 */
	private void inorderRecursiveHelper(TreeNode node, List<Integer> result) {
		// 递归终止条件：节点为空时直接返回
		if (node == null) {
			return;
		}

		//1. 先递归遍历左子树（左）
		inorderRecursiveHelper(node.left, result);
		// 2. 访问当前根节点，将值加入结果集（根）
		result.add(node.val);
		// 3. 递归遍历右子树（右）
		inorderRecursiveHelper(node.right, result);
	}
}
