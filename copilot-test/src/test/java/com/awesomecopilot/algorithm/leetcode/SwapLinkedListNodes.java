package com.awesomecopilot.algorithm.leetcode;

/**
 * 两两交换链表中的节点
 * <p>
 * 给你一个链表，两两交换其中相邻的节点，并返回交换后链表的头节点。你必须在不修改节点内部的值的情况下完成本题（即，只能进行节点交换）。
 *
 * <pre>
 * 示例1
 * 输入：head = [1,2,3,4]
 * 输出：[2,1,4,3]
 * </pre>
 * <image src="images/exchange list.png" ></image>
 * <pre>
 * 示例 2：
 *
 * 输入：head = []
 * 输出：[]
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：head = [1]
 * 输出：[1]
 * </pre>
 *
 * <ul>两两交换链表节点的核心思路是使用迭代法：
 *     <li/>创建一个虚拟头节点 (dummy) 简化边界处理
 *     <li/>使用 prev 指针指针指针指向需要交换的两个节点的前一个节点
 *     <li/>每次交换 prev 后的两个节点，并更新指针位置
 *     <li/>重复直到没有可交换的节点对
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-10-01 8:18
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SwapLinkedListNodes {

	public static ListNode swapPairs(ListNode head) {
		// 创建虚拟头节点，简化边界处理
		ListNode dummy = new ListNode(0);
		dummy.next = head;

		// prev指针指向需要交换的两个节点的前一个节点
		ListNode prev = dummy;

		// 当有两个及以上节点可交换时
		while (prev.next != null && prev.next.next != null) {
			// 第一个节点
			ListNode first = prev.next;
			// 第二个节点
			ListNode second = prev.next.next;

			// 交换节点
			first.next = second.next;
			second.next = first;
			prev.next = second;

			// 更新prev指针位置，准备下一次交换
			prev = first;
		}

		// 返回交换后的链表头节点
		return dummy.next;
	}
}
