package com.awesomecopilot.algorithm.leetcode;

/**
 * 两数相加
 * <p>
 * 给你两个 非空 的链表，表示两个非负的整数。它们每位数字都是按照 逆序 的方式存储的，并且每个节点只能存储 一位数字。
 * <p>
 * 请你将两个数相加，并以相同形式返回一个表示和的链表。
 * <p>
 * 你可以假设除了数字 0 之外，这两个数都不会以 0 开头。
 * <p>
 * 示例 1：
 * <p>
 * 输入：l1 = [2,4,3], l2 = [5,6,4]
 * 输出：[7,0,8]
 * 解释：342 + 465 = 807.
 * <p>
 * <p>
 * 示例 2：
 * <p>
 * 输入：l1 = [0], l2 = [0]
 * 输出：[0]
 * <p>
 * 示例 3：
 * <p>
 * 输入：l1 = [9,9,9,9,9,9,9], l2 = [9,9,9,9]
 * 输出：[8,9,9,9,0,0,0,1]
 * <p>
 * 提示：
 * <p>
 * 每个链表中的节点数在范围 [1, 100] 内
 * 0 <= Node.val <= 9
 * 题目数据保证列表表示的数字不含前导零
 * <p>
 * 那具体怎么做呢？可能需要同时遍历两个链表，把对应的节点相加，再加上进位值。然后，每个新的节点保存相加后的个位数，而进位则保存十位数，参与下一次的计算。
 * 比如，比如某次相加是2+5=7，进位是0，所以新节点是7。而如果是9+9=18，那么新节点是8，进位是1。
 *
 * <ol>算法步骤
 *     <li/>初始化一个哑节点(dummy)作为结果链表的头部，以及一个当前节点(current)用于构建结果链表
 *     <li/>初始化进位(carry)为0
 *     <li/>同时遍历两个链表，直到两个链表都到达末尾
 *     <ol>
 *        <li/>获取当前两个节点的值（如果节点存在则取值，否则为0）
 *        <li/>计算当前位的和：sum = val1 + val2 + carry
 *        <li/>计算当前位的值：sum % 10
 *        <li/>计算新的进位：carry = sum / 10
 *        <li/>创建新节点存储当前位的值，并连接到结果链表
 *        <li/>
 *     </ol>
 *     <li/>如果遍历结束后仍有进位，创建新节点存储进位
 *     <li/>返回哑节点的下一个节点（即结果链表的真正头部）
 * </ol>
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-04-01 6:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class AddTwoNumbers {

	public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
		// 使用哑结点简化链表操作，避免处理头节点为空的特殊情况
		ListNode dummy = new ListNode(0);
		ListNode current = dummy; // 当前节点指针，用于构建结果链表
		int carry = 0; // 进位值，初始为0

		// 当任一链表未遍历完或仍有进位时，继续循环
		while (l1 != null || l2 != null || carry != 0) {
			// 获取当前两个节点的值（如果节点存在则取值，否则为0）
			int val1 = l1 == null ? 0 : l1.val;
			int val2 = l2 == null ? 0 : l2.val;

			// 计算当前位的总和（包括进位）
			int sum = val1 + val2 + carry;
			carry = sum / 10; // 更新进位值

			// 创建新节点存储当前位的计算结果，并移动当前指针
			current.next = new ListNode(sum % 10);
			current = current.next;

			// 移动链表指针（如果未到末尾）
			l1 = l1 == null ? null : l1.next;
			l2 = l2 == null ? null : l2.next;
		}

		return dummy.next;
	}
}

class ListNode {
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
