package com.awesomecopilot.algorithm.leetcode;

/**
 * 将有序数组转换为二叉搜索树
 * <p>
 * 给你一个整数数组 nums ，其中元素已经按 升序 排列，请你将其转换为一棵 平衡 二叉搜索树。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [-10,-3,0,5,9]
 * 输出：[0,-3,9,-10,null,5]
 * 解释：[0,-10,5,null,-3,null,9] 也将被视为正确答案：
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [1,3]
 * 输出：[3,1]
 * 解释：[1,null,3] 和 [3,1] 都是高度平衡二叉搜索树。
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>二叉搜索树特性：升序数组的中间元素作为根节点，左半数组是左子树、右半数组是右子树；
 *     <li/>平衡树保证：每次选中间元素为根，左右子树节点数量差≤1，天然满足高度平衡；
 *     <li/>递归实现：分治思想，对左右子数组重复选中点、建节点、连父子的操作，直到子数组为空。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-06 8:58
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SortedArrayToBSTConverter {

	/**
	 * 对外暴露的核心方法：升序数组 → 平衡二叉搜索树
	 *
	 * @param nums 升序排列的整数数组
	 * @return 平衡BST的根节点
	 */
	public TreeNode sortedArrayToBST(int[] nums) {
		// 空数组直接返回空树
		if (nums == null || nums.length == 0) {
			return null;
		}
		// 调用递归方法，初始处理区间：整个数组[0, nums.length-1]
		return buildBST(nums, 0, nums.length - 1);
	}

	/**
	 * 递归构建平衡BST的核心方法（分治）
	 *
	 * @param nums 升序数组
	 * @return
	 */
	private TreeNode buildBST(int[] nums, int left, int right) {
		// 递归终止条件：左边界 > 右边界，说明无元素，返回空节点
		if (left > right) {
			return null;
		}

		// ✅ 关键技巧：选中间位置为根，保证左右子树平衡
		// 计算中间索引（用(left+right)/2也可，此写法避免int溢出）
		int mid = left + (right - left) / 2;

		// 1. 中间元素作为当前子树的根节点
		TreeNode root = new TreeNode(nums[mid]);
		// 2. 递归构建左子树：处理左半区间 [left, mid-1]
		root.left = buildBST(nums, left, mid - 1);

		// 3. 递归构建右子树：处理右半区间 [mid+1, right]
		root.right = buildBST(nums, mid + 1, right);

		// 返回当前子树的根节点，供父节点连接
		return root;
	}
}
