package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 二叉树的所有路径
 * <p>
 * 给你一个二叉树的根节点 root ，按 任意顺序 ，返回所有从根节点到叶子节点的路径。
 * <p>
 * 叶子节点 是指没有子节点的节点。
 * <p>
 *
 * <pre>
 * 示例 1：
 * <image src="images/paths-tree.jpg"/>
 *
 * 输入：root = [1,2,3,null,5]
 * 输出：["1->2->5","1->3"]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：root = [1]
 * 输出：["1"]
 * </pre>
 *
 * 提示：
 *
 * 树中节点的数目在范围 [1, 100] 内
 * -100 <= Node.val <= 100
 * <p/>
 * Copyright: Copyright (c) 2026-02-13 9:21
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BinaryTreePaths {
	
	public static void main(String[] args) {
		TreeNode root = new TreeNode(1);
		root.left = new TreeNode(2);
		root.right = new TreeNode(3);
		root.left.right = new TreeNode(5);
		
		System.out.println(binaryTreePaths(root));
	}
	
	/**
	 * 对外提供的接口，返回所有路径
	 * @param root
	 * @return
	 */
	private static List<String> binaryTreePaths(TreeNode root) {
		// 存储最终结果的列表
		List<String> result = new ArrayList<>();
		// 如果根节点为空，直接返回空列表（题目中节点数>=1，此判断为鲁棒性设计）
		if (root == null) {
			return result;
		}
		// 用于临时存储当前路径的字符串构建器（效率高于String拼接）
		StringBuilder path = new StringBuilder();
		// 调用深度优先搜索方法
		dfs(root, path, result);
		return result;
	}
	
	/**
	 * 深度优先搜索核心方法
	 * @param root  当前遍历的节点
	 * @param path  从根节点到当前节点的路径（拼接中的临时值）
	 * @param result 存储所有完整路径的结果列表
	 */
	private static void dfs(TreeNode root, StringBuilder path, List<String> result) {
		// 记录当前路径长度（用于回溯）
		int currentLength = path.length();
		
		// 将当前节点值添加到路径中
		if (currentLength == 0) {
			// 根节点：直接添加值（无前置"->"）
			path.append(root.val);
		}else {
			// 非根节点：先加"->"再加值
			path.append("->").append(root.val);
		}
		
		// 判断是否是叶子节点（左右子节点都为空）
		if (root.left == null && root.right == null) {
			// 叶子节点：将完整路径加入结果列表
			result.add(path.toString());
		}else {
			// 非叶子节点：递归处理左右子节点
			if (root.left != null) {
				dfs(root.left, path, result);
			}
			if (root.right != null) {
				dfs(root.right, path, result);
			}
		}
		
		// 回溯：撤销当前节点的路径记录（恢复到递归前的状态）
		path.setLength(currentLength);
	}
}