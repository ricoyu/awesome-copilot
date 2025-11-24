package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 合并两个有序链表
 * <p>
 * 将两个升序链表合并为一个新的 升序 链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。
 *
 * <pre>
 * 示例 1：
 * 输入：l1 = [1,2,4], l2 = [1,3,4]
 * 输出：[1,1,2,3,4,4]
 * </pre>
 * <image src="images/ListNodeMerge.png" /
 *
 * <pre>
 * 示例 2：
 * 输入：l1 = [], l2 = []
 * 输出：[]
 * </pre>
 *
 * <pre>
 * 示例 3：
 * 输入：l1 = [], l2 = [0]
 * 输出：[0]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-09-23 8:26
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MergeTwoSortedLists {

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

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		ListNode dummyNode = new ListNode(-1);
		System.out.print("请输入链表l1: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		ListNode l1 = new ListNode(nums[0]);
		ListNode curr = l1;
		for (int i = 1; i < nums.length; i++) {
			curr.next = new ListNode(nums[i]);
			curr = curr.next;
		}
		System.out.print("请输入链表l2: ");
		int[] nums2 = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		ListNode l2 = new ListNode(nums[0]);
		ListNode curr2 = l2;
		for (int i = 1; i < nums2.length; i++) {
			curr2.next = new ListNode(nums2[i]);
			curr2 = curr2.next;
		}

		ListNode mergedList = mergeTwoLists(l1, l2);
		while (mergedList != null) {
			System.out.print(mergedList.val + " ");
			mergedList = mergedList.next;
		}
	}

	/**
	 * 合并两个升序链表
	 * @param l1 第一个升序链表
	 * @param l2 第二个升序链表
	 * @return 合并后的升序链表
	 */
	private static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
		// 创建虚拟头节点，简化处理
		ListNode dummyNode = new ListNode(-1);
		// 当前指针，用于构建结果链表
		ListNode curr = dummyNode;

		// 当两个链表都有节点时，比较并连接较小的节点
		while (l1 != null && l2 != null) {
			if (l1.val < l2.val) {
				// 连接l1的当前节点
				curr.next = l1;
				// l1指针后移
				l1 = l1.next;
			} else {
				// 连接l2的当前节点
				curr.next = l2;
				// l2指针后移
				l2 = l2.next;
			}
			// 当前指针后移
			curr = curr.next;
		}

		// 连接剩余的节点（其中一个链表可能已经遍历完毕）
		// 连接剩余节点：当其中一个链表遍历完成后，将另一个链表的剩余部分直接连接到结果链表末尾
		// 因为两个链表都是有序的，所以剩余的部分也必然是有序的
		curr.next = l1 != null ? l1 : l2;

		// 返回合并后链表的头节点（虚拟头节点的下一个节点）
		return dummyNode.next;
	}
}
