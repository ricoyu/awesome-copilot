package com.awesomecopilot.algorithm.leetcode;

/**
 * K 个一组翻转链表
 * <p>
 * 给你链表的头节点 head ，每 k 个节点一组进行翻转，请你返回修改后的链表。
 * <p>
 * k 是一个正整数，它的值小于或等于链表的长度。如果节点总数不是 k 的整数倍，那么请将最后剩余的节点保持原有顺序。
 * <p>
 * 你不能只是单纯的改变节点内部的值，而是需要实际进行节点交换。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：head = [1,2,3,4,5], k = 2
 * 输出：[2,1,4,3,5]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *  *
 *  * 输入：head = [1,2,3,4,5], k = 3
 *  * 输出：[3,2,1,4,5]
* </pre>
 *
 * 提示：
 * 链表中的节点数目为 n
 * 1 <= k <= n <= 5000
 * 0 <= Node.val <= 1000
 * <p>
 * 进阶：你可以设计一个只用 O(1) 额外内存空间的算法解决此问题吗？
 * <p/>
 * Copyright: Copyright (c) 2025-04-05 10:18
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ReverseKGroup {

	public ListNode reverseKGroup(ListNode head, int k) {
		// 如果链表为空或k为1，直接返回原链表
		if (head == null || k == 1) {
			return head;
		}

		// 创建一个虚拟头节点，方便处理头节点的翻转
		ListNode dummy = new ListNode(0);

		return null;
	}


}
