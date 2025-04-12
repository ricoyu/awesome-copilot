package com.awesomecopilot.algorithm.leetcode;

/**
 * 反转链表
 * <p>
 * 给定单链表的头节点 head ，请反转链表，并返回反转后的链表的头节点。
 * <pre>
 * 示例 1：
 *
 * 输入：head = [1,2,3,4,5]
 * 输出：[5,4,3,2,1]
 * </pre>
 * <pre>
 * 示例 2：
 *
 * 输入：head = [1,2]
 * 输出：[2,1]
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：head = []
 * 输出：[]
 * </pre>
 * <p>
 * 反转链表是一个经典的链表操作问题，可以通过迭代或递归两种方式实现。
 * <p/>
 * 迭代法思路:
 * <ul>初始化三个指针
 *     <li/>prev：指向已反转部分的头节点，初始为null
 *     <li/>指向当前待反转的节点，初始为head
 *     <li/>临时保存current的下一个节点
 * </ul>
 *
 * <ul>遍历链表
 *     <li/>在每次迭代中，先保存current的下一个节点到next
 *     <li/>然后将current的next指向prev（反转操作）
 *     <li/>移动prev和current指针向前
 * </ul>
 *
 * <ul>终止条件
 *     <li/>当current为null时，说明已遍历完整个链表
 *     <li/>此时prev指向新的头节点ka
 * </ul>
 * <p>
 * 以链表 1→2→3→4→5 为例：
 * 初始状态：
 * <pre>
 * prev = null
 * current = 1→2→3→4→5
 * </pre>
 * <ul>第一次迭代：
 *     <li/>保存next = current.next = 2
 *     <li/>current.next = prev → 1→null
 *     <li/>prev = current = 1
 *     <li/>current = next = 2
 * </ul>
 * 状态：
 * <pre>
 * prev = 1→null
 * current = 2→3→4→5
 * </pre>
 *
 * <ul>第二次迭代：
 *     <li/>next = 3
 *     <li/>2→1→null
 *     <li/>prev = 2
 *     <li/>current = 3
 * </ul>
 * 状态
 * <pre>
 * prev = 2→1→null
 * current = 3→4→5
 * </pre>
 * <p>
 * 继续这个过程，最终会得到：
 * <pre>
 * prev = 5→4→3→2→1→null
 * current = null
 * </pre>
 * <p>
 * Copyright: Copyright (c) 2025-04-03 8:59
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LinkedListReverser {

	public ListNode reverseList(ListNode head) {
		ListNode prev = null;
		ListNode curr = head;
		ListNode next = null;

		while (curr != null) {
			next = curr.next;
			curr.next = prev;
			prev = curr;
			curr = next;
		}

		return prev;
	}
}
