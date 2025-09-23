package com.awesomecopilot.algorithm.leetcode.round2;

/**
 * 删除链表的倒数第 N 个结点
 * <p>
 * 给你一个链表，删除链表的倒数第 n 个结点，并且返回链表的头结点。
 * <p>
 * 示例 1：
 * <image src="../images/ListNode.png" ></image>
 * <pre>
 * 输入：head = [1,2,3,4,5], n = 2
 * 输出：[1,2,3,5]
 * </pre>
 *
 * <pre>
 * 示例 2：
 * 输入：head = [1], n = 1
 * 输出：[]
 * </pre>
 *
 * <pre>
 * 示例 3：
 * 输入：head = [1,2], n = 1
 * 输出：[1]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-20 10:16
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RemoveNthFromEnd {

	// 链表节点定义
	static class ListNode {
		int val;
		ListNode next;

		ListNode() {
		}

		ListNode(int val) {
			this.val = val;
		}

		ListNode(int val, ListNode next) {
			this.val = val;
			this.next = next;
		}
	}

	public ListNode removeNthFromEnd(ListNode head, int n) {
		// 创建哑节点，指向头节点，处理边界情况（如删除第一个节点）
		ListNode dummy = new ListNode(0);
		dummy.next = head;
		// 定义快慢指针，都从哑节点开始
		ListNode fast = dummy;
		ListNode slow = dummy;

		// 快指针先向前移动n+1步
		// 这样当快指针到达末尾时，慢指针正好在倒数第n+1个节点
		for (int i = 0; i <= n; i++) {
			fast = fast.next;
		}

		// 快慢指针同时移动，直到快指针到达链表末尾
		while (fast != null) {
			fast = fast.next;
			slow = slow.next;
		}

		// 此时slow的下一个节点就是要删除的节点
		slow.next = slow.next.next;
		// 返回头节点（注意：不能直接返回head，因为head可能被删除）
		return dummy.next;
	}
}
