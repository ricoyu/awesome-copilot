package com.awesomecopilot.algorithm.leetcode;

/**
 * K 个一组翻转链表
 * <p>
 * 给你链表的头节点 head, 每 k 个节点一组进行翻转，请你返回修改后的链表。
 * <p>
 * k 是一个正整数，它的值小于或等于链表的长度。如果节点总数不是 k 的整数倍，那么请将最后剩余的节点保持原有顺序。
 * <p>
 * 你不能只是单纯的改变节点内部的值，而是需要实际进行节点交换。
 * <p>
 * <pre>
 * 示例 1：
 * <image src="images/k-reverse.png" />
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
 * <p>
 * 提示：
 * 链表中的节点数目为 n
 * 1 <= k <= n <= 5000
 * 0 <= Node.val <= 1000
 * <p>
 * 进阶：你可以设计一个只用 O(1) 额外内存空间的算法解决此问题吗？
 *
 * <ul>要解决 K 个一组翻转链表的问题，我们可以采用迭代的方法，主要思路如下：
 *     <li/>创建一个虚拟头节点 (dummy)，简化边界情况的处理
 *     <li/>遍历链表，每次检查剩余节点是否有 k 个
 *     <li/>如果有 k 个节点，就将这 k 个节点进行翻转
 *     <li/>将翻转后的子链表与原链表的前后部分连接起来
 *     <li/>继续处理剩余的节点，直到所有节点都被处理完毕
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-04-05 10:18
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ReverseKGroup {

	public static void main(String[] args) {
		// 示例1：输入: [1,2,3,4,5], k = 2
		int[] arr1 = {1, 2, 3, 4, 5};
		ListNode head1 = arrayToList(arr1);
		System.out.print("示例1 原始链表: ");
		printList(head1);
		ListNode result1 = reverseKGroup(head1, 2);
		System.out.print("示例1 翻转后链表: ");
		printList(result1);  // 预期输出: 2 -> 1 -> 4 -> 3 -> 5

		// 示例2：输入: [1,2,3,4,5], k = 3
		int[] arr2 = {1, 2, 3, 4, 5};
		ListNode head2 = arrayToList(arr2);
		System.out.print("示例2 原始链表: ");
		printList(head2);
		ListNode result2 = reverseKGroup(head2, 3);
		System.out.print("示例2 翻转后链表: ");
		printList(result2);  // 预期输出: 3 -> 2 -> 1 -> 4 -> 5
	}
	/**
	 * K个一组翻转链表的主方法
	 *
	 * @param head 链表的头节点
	 * @param k    每组节点的数量
	 * @return 翻转后的链表头节点
	 */
	public static ListNode reverseKGroup(ListNode head, int k) {
		// 创建虚拟头节点，简化边界情况处理
		ListNode dummy = new ListNode(-1);
		dummy.next = head;

		// 前驱节点，初始指向虚拟头节点
		ListNode prev = dummy;

		while (true) {
			// 检查剩余节点是否有k个
			ListNode check = prev;
			for (int i = 0; i < k; i++) {
				check = check.next;
				// 如果剩余节点不足k个，直接返回结果
				if (check == null) {
					return dummy.next;
				}
			}

			// 翻转k个节点，并获取翻转后的头节点和尾节点
			ListNode[] reversed = reverse(prev.next, k);
			ListNode newHead = reversed[0]; // 翻转后的头节点
			ListNode newTail = reversed[1]; // 翻转后的尾节点

			// 将翻转后的子链表与前驱节点连接
			prev.next = newHead;
			// 更新前驱节点为当前子链表的尾节点，为下一次循环做准备
			prev = newTail;
		}
	}

	/**
	 * 翻转指定数量的节点
	 *
	 * @param head 要翻转的子链表的头节点
	 * @param k    要翻转的节点数量
	 * @return 一个数组，包含两个元素：翻转后的头节点和尾节点
	 */
	private static ListNode[] reverse(ListNode head, int k) {
		ListNode prev = null;
		ListNode curr = head;
		ListNode next = null;

		// 翻转k个节点
		for (int i = 0; i < k; i++) {
			next = curr.next; // 保存下一个节点
			curr.next = prev; // 翻转指针
			prev = curr;      // 移动prev指针
			curr = next;      // 移动curr指针
		}

		// 将翻转后的尾节点(原头节点)与剩余节点连接
		head.next = curr;

		// 返回翻转后的头节点(原尾节点)和尾节点(原头节点)
		return new ListNode[]{prev, head};
	}

	/**
	 * 辅助方法：将数组转换为链表
	 * @param arr 输入的数组
	 * @return 转换后的链表头节点
	 */
	public static ListNode arrayToList(int[] arr) {
		if (arr == null || arr.length == 0) {
			return null;
		}

		ListNode head = new ListNode(arr[0]);
		ListNode curr = head;

		for (int i = 1; i < arr.length; i++) {
			curr.next = new ListNode(arr[i]);
			curr = curr.next;
		}

		return head;
	}

	/**
	 * 辅助方法：打印链表
	 * @param head 链表的头节点
	 */
	public static void printList(ListNode head) {
		ListNode curr = head;
		while (curr != null) {
			System.out.print(curr.val);
			if (curr.next != null) {
				System.out.print(" -> ");
			}
			curr = curr.next;
		}
		System.out.println();
	}


}
