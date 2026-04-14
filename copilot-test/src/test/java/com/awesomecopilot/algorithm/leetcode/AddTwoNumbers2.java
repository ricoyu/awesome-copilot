package com.awesomecopilot.algorithm.leetcode;

/**
 * 两数相加
 * <p>
 * 给你两个非空的链表，表示两个非负的整数。它们每位数字都是按照逆序的方式存储的，并且每个节点只能存储 一位数字。
 * <p>
 * 请你将两个数相加，并以相同形式返回一个表示和的链表。
 * <p>
 * 你可以假设除了数字 0 之外，这两个数都不会以0开头。
 * <p>
 * <image src="images/addtwonumber1.jpg"></image>
 *
 * <pre>
 * 输入：l1 = [2,4,3], l2 = [5,6,4]
 * 输出：[7,0,8]
 * 解释：342 + 465 = 807.
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：l1 = [0], l2 = [0]
 * 输出：[0]
 * </pre>
 * <p>
 * <pre>
 * 示例 3：
 *
 * 输入：l1 = [9,9,9,9,9,9,9], l2 = [9,9,9,9]
 * 输出：[8,9,9,9,0,0,0,1]
 * </pre>
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
public class AddTwoNumbers2 {
	
	public static void main(String[] args) {
		ListNode l1 = new ListNode(2);
		ListNode l1_2 = new ListNode(4);
		ListNode l1_3 = new ListNode(3);
		l1.next = l1_2;
		l1_2.next = l1_3;
		
		ListNode l2 = new ListNode(5);
		ListNode l2_2 = new ListNode(6);
		ListNode l2_3 = new ListNode(4);
		l2.next = l2_2;
		l2_2.next = l2_3;
		
		ListNode node = addTwoNumbers(l1, l2);
		while (node != null) {
			System.out.print(node.val + " ");
			node = node.next;
		}
		
	}
	
	private static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
		ListNode dummy = new ListNode(0);
		int carry = 0;
		while (l1.next != null && l2.next != null) {
			int sum = l1.val + l2.val + carry;
			int value = sum % 10;
			carry = sum / 10;
			ListNode node = new ListNode(value);
			dummy.next = node;
			dummy = dummy.next;
			carry = sum / 10;
			l1 = l1.next;
			l2 = l2.next;
		}
		
		return dummy.next;
	}
	
}

class ListNode2 {
	int val;
	ListNode next;
	
	ListNode2() {
	}
	
	ListNode2(int val) {
		this.val = val;
	}
	
	ListNode2(int val, ListNode next) {
		this.val = val;
		this.next = next;
	}
}