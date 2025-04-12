package com.awesomecopilot.algorithm.leetcode;

/**
 * 旋转链表
 * <p>
 * 给你一个链表的头节点 head ，旋转链表，将链表每个节点向右移动 k 个位置。
 * <p>
 * 示例 1：
 * <p>
 * 输入：head = [1,2,3,4,5], k = 2
 * 输出：[4,5,1,2,3]
 * <p>
 * 示例 2：
 * <p>
 * 输入：head = [0,1,2], k = 4
 * 输出：[2,0,1]
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2025-04-07 8:53
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LinkedListRotator {

	public ListNode rotateRight(ListNode head, int k) {
		if (head == null ||k==0) {
			return head;
		}

		int n = 1;
		ListNode tail = head;
		while (tail.next != null) {
			tail = tail.next;
			n++;
		}

		tail.next = head;

		k = k % n;
		if (k == 0) {
			tail.next = null;
			return head;
		}

		ListNode newTail = head;
		for (int i = 0; i < n - k - 1; i++) {
			newTail = newTail.next;
		}

		ListNode newHead = newTail.next;
		newTail.next = null;

		return newHead;
	}
}
