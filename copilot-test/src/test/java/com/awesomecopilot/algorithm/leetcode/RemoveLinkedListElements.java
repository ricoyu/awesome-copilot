package com.awesomecopilot.algorithm.leetcode;

/**
 * 移除链表元素
 *
 * 给你一个链表的头节点 head 和一个整数 val ，请你删除链表中所有满足 Node.val == val 的节点，并返回 新的头节点 。
 *
 * 示例 1：
 * <image src="images/removelinked-list.jpg"/>
 * 输入：head = [1,2,6,3,4,5,6], val = 6
 * 输出：[1,2,3,4,5]
 *
 * 示例 2：
 *
 * 输入：head = [], val = 1
 * 输出：[]
 *
 * 示例 3：
 *
 * 输入：head = [7,7,7,7], val = 7
 * 输出：[]
 * <p/>
 * Copyright: Copyright (c) 2026-01-27 8:52
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RemoveLinkedListElements {

	public static void main(String[] args) {
		// 示例1：构建链表 [1,2,6,3,4,5,6]
		ListNode head1 = new ListNode(1);
		head1.next = new ListNode(2);
		head1.next.next = new ListNode(6);
		head1.next.next.next = new ListNode(3);
		head1.next.next.next.next = new ListNode(4);
		head1.next.next.next.next.next = new ListNode(5);
		head1.next.next.next.next.next.next = new ListNode(6);

		ListNode result1 = removeElements(head1, 6);
		printList(result1); // 输出：1 2 3 4 5

		// 示例3：构建链表 [7,7,7,7]
		ListNode head3 = new ListNode(7);
		head3.next = new ListNode(7);
		head3.next.next = new ListNode(7);
		head3.next.next.next = new ListNode(7);
		ListNode result3 = removeElements(head3, 7);
		printList(result3); // 输出：（空）
	}

	private static ListNode removeElements(ListNode head1, int val) {
		ListNode dummy = new ListNode(-1);
		dummy.next = head1;
		ListNode current = dummy;
		while (current.next != null) {
			if (current.next.val == val) {
				current.next = current.next.next;
			} else {
				// 不是要删除的节点，指针向后移动
				current = current.next;
			}

		}

		return dummy.next;
	}
	/*
	private static ListNode removeElements(ListNode head1, int val) {
		ListNode dummy = new ListNode(-1);
		dummy.next = head1;
		ListNode current = dummy;
		while (current.next != null) {
			if (current.next.val == val) {
				current.next = current.next.next;
			} else {
				// 不是要删除的节点，指针向后移动
				current = current.next;
			}

		}

		return dummy.next;
	}
	*/

	// 辅助打印链表的方法
	private static void printList(ListNode head) {
		ListNode curr = head;
		while (curr != null) {
			System.out.print(curr.val + " ");
			curr = curr.next;
		}
		System.out.println();
	}
}
