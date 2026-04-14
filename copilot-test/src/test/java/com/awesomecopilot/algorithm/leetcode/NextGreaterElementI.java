package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 下一个更大元素 I
 * <p>
 * nums1 中数字 x 的 下一个更大元素 是指 x 在 nums2 中对应位置 右侧 的 第一个 比 x 大的元素。
 * <p>
 * 给你两个 没有重复元素 的数组 nums1 和 nums2 ，下标从 0 开始计数，其中nums1 是 nums2 的子集。
 * <p>
 * 对于每个 0 <= i < nums1.length ，找出满足 nums1[i] == nums2[j] 的下标 j ，并且在 nums2 确定 nums2[j] 的 下一个更大元素 。如果不存在下一个更大元素，那么本次查询的答案是 -1 。
 * <p>
 * 返回一个长度为 nums1.length 的数组 ans 作为答案，满足 ans[i] 是如上所述的 下一个更大元素 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums1 = [4,1,2], nums2 = [1,3,4,2].
 * 输出：[-1,3,-1]
 * 解释：nums1 中每个值的下一个更大元素如下所述：
 * 4 ，nums2 = [1,3,4,2]。不存在下一个更大元素，所以答案是 -1 。
 * 1 ，nums2 = [1,3,4,2]。下一个更大元素是 3 。
 * 2 ，nums2 = [1,3,4,2]。不存在下一个更大元素，所以答案是 -1 。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums1 = [2,4], nums2 = [1,2,3,4].
 * 输出：[3,-1]
 * 解释：nums1 中每个值的下一个更大元素如下所述：
 * 2 ，nums2 = [1,2,3,4]。下一个更大元素是 3 。
 * 4 ，nums2 = [1,2,3,4]。不存在下一个更大元素，所以答案是 -1 。
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>核心目标：先一次性求出nums2中所有元素的下一个更大元素，存入哈希表，再遍历nums1直接查表得到结果，时间复杂度最优（O (n)）。
 *     <li/>核心算法：单调栈（递减栈），专门高效解决「下一个更大元素」问题。
 *     <ul>
 *          <li/>遍历nums2，用栈维护未找到更大元素的数值；
 *          <li/>遇到更大元素时，栈中所有比它小的元素的「下一个更大元素」就是当前值，存入哈希表；
 *          <li/>最后遍历nums1，从哈希表中取值，无值则填 - 1。
 *     </ul>
 * </ul>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-18 9:47
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class NextGreaterElementI {
	
	public static int[] nextGreaterElement(int[] nums1, int[] num2) {
		// 存储 nums2 中每个元素 对应的 下一个更大元素（key=元素值，value=下一个更大值）
		Map<Integer, Integer> map = new HashMap<>();
		// 单调栈：存储未找到下一个更大元素的元素，保持栈内元素 递减
		Deque<Integer> stack = new ArrayDeque<>();
		return new int[0];
	}
}