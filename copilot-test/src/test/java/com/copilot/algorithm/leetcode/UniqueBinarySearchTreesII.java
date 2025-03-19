package com.copilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 虽然这个问题可以用递归解决，但为了优化性能，我们可以使用动态规划（DP）来避免重复计算。我们可以使用一个数组 dp，其中 dp[i] 存储所有由 i 个节点组成的二叉搜索树的列表。
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2025-03-15 9:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */

class TreeNode {
	int val;
	TreeNode left;
	TreeNode right;

	TreeNode() {
	}

	TreeNode(int val) {
		this.val = val;
	}

	TreeNode(int val, TreeNode left, TreeNode right) {
		this.val = val;
		this.left = left;
		this.right = right;
	}
}

public class UniqueBinarySearchTreesII {

	public List<TreeNode> generateTrees(int n) {
		if (n == 0) {
			return new ArrayList<>();
		}
		return generateTrees(1, n);
	}

	// 递归生成从 start 到 end 的所有可能的二叉搜索树
	private List<TreeNode> generateTrees(int start, int end) {
		List<TreeNode> allTrees = new ArrayList<>();
		if (start > end) {
			allTrees.add(null);
			return allTrees;
		}
		// 遍历每个可能的根节点
		for (int i = start; i <= end; i++) {
			// 生成左子树
			List<TreeNode> leftTrees = generateTrees(start, i - 1);
			// 递归生成所有可能的右子树
			List<TreeNode> rightTrees = generateTrees(i + 1, end);

			// 将左子树和右子树组合在一起
			for (TreeNode leftTree : leftTrees) {
				for (TreeNode rightTree : rightTrees) {
					TreeNode root = new TreeNode(i);
					root.left = leftTree;
					root.right = rightTree;
					allTrees.add(root);
				}
			}
		}

		return allTrees;
	}

	public static void main(String[] args) {
		UniqueBinarySearchTreesII solution = new UniqueBinarySearchTreesII();
		int n = 3;
		List<TreeNode> trees = solution.generateTrees(n);
		for (TreeNode tree : trees) {
			printTree(tree);
		}
	}

	// 辅助方法：打印树结构
	private static void printTree(TreeNode root) {
		if (root == null) {
			System.out.print("null ");
			return;
		}
		System.out.print(root.val + " ");
		printTree(root.left);
		printTree(root.right);
	}
}
