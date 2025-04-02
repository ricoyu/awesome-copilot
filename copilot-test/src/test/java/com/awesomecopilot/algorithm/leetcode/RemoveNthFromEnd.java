package com.awesomecopilot.algorithm.leetcode;

/**
 * 删除链表的倒数第 N 个结点
 * <p/>
 * 给你一个链表，删除链表的倒数第 n 个结点，并且返回链表的头结点。
 * <p/>
 * <pre>
 * 示例 1：
 *
 * 输入：head = [1,2,3,4,5], n = 2
 * 输出：[1,2,3,5]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：head = [1], n = 1
 * 输出：[]
 * </pre>
 * <pre>
 * 示例 3：
 *
 * 输入：head = [1,2], n = 1
 * 输出：[1]
 * </pre>
 * <p>
 * 进阶：你能尝试使用一趟扫描实现吗？
 * <p/>
 * 假设链表长度是L，那么倒数第N个节点就是正数第L-N+1个节点。
 * <p>
 * 那有没有办法在一次遍历中找到倒数第N个节点呢？哦，对了，可以用双指针的方法。比如，快慢指针。
 * 让快指针先走N步，然后快慢指针一起走，当快指针到达末尾时，慢指针所在的位置就是倒数第N个节点的前一个节点？
 * <p>
 * 比如示例1中的链表1->2->3->4->5，n是2。那么倒数第二个节点是4，要删除它。这时候，如果让快指针先走两步，然后快慢一起走，
 * 当快指针到末尾时，慢指针应该在3的位置，这样就可以删除下一个节点。
 * <p>
 * Copyright: Copyright (c) 2025-04-02 9:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RemoveNthFromEnd {

	public ListNode removeNthFromEnd(ListNode head, int n) {
		ListNode dummy = new ListNode(0);
		dummy.next = head;
		ListNode fast = dummy;
		ListNode slow = dummy;

		for (int i = 0; i < n; i++) {
			if (fast.next != null) {
				fast = fast.next;
			}
		}

		while (fast.next != null) {
			fast = fast.next;
			slow = slow.next;
		}

		if (slow.next != null) {
			slow.next = slow.next.next;
		}

		return dummy.next;
	}
}
