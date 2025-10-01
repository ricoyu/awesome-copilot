package com.awesomecopilot.algorithm.leetcode;

import java.util.PriorityQueue;

/**
 * 合并 K 个升序链表
 * <p>
 * 给你一个链表数组，每个链表都已经按升序排列。
 * <p>
 * 请你将所有链表合并到一个升序链表中，返回合并后的链表。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：lists = [[1,4,5],[1,3,4],[2,6]]
 * 输出：[1,1,2,3,4,4,5,6]
 * 解释：链表数组如下：
 * [
 *   1->4->5,
 *   1->3->4,
 *   2->6
 * ]
 * 将它们合并到一个有序链表中得到。
 * 1->1->2->3->4->4->5->6
 * </pre>
 *
 *
 * <pre>
 * 示例 2：
 *
 * 输入：lists = []
 * 输出：[]
 * 示例 3：
 *
 * 输入：lists = [[]]
 * 输出：[]
 * </pre>
 *
 * <ul>合并 K 个升序链表的核心思路是利用最小堆（优先队列）来高效获取每个链表当前的最小值：
 *     <li/>先将所有链表的头节点加入最小堆
 *     <li/>每次从堆中取出最小值节点，加入结果链表
 *     <li/>然后将该节点的下一个节点（如果存在）加入堆中
 *     <li/>重复步骤 2-3，直到堆为空
 * </ul>
 * 这种方法的时间复杂度为 O (NlogK)，其中 N 是所有节点总数，K 是链表数量，每次堆操作的时间复杂度是 O (logK)。
 * <p/>
 * Copyright: Copyright (c) 2025-09-30 8:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MergeKSortedLists {

	public static ListNode mergeKLists(ListNode[] lists) {
		// 边界情况处理
		if (lists == null || lists.length == 0) {
			return null;
		}

		// 创建最小堆，使用节点值进行比较
		// 这里使用lambda表达式定义比较器，比较两个节点的值
		PriorityQueue<ListNode> minHeap = new PriorityQueue<>(lists.length, (a, b) -> a.val - b.val);

		// 将所有非空链表的头节点加入堆
		for (ListNode node : lists) {
			if (node != null) {
				minHeap.add(node);
			}
		}

		// 创建结果链表的虚拟头节点，方便操作
		ListNode dummy = new ListNode(-1);
		ListNode cur = dummy;

		// 当堆不为空时，持续取出最小节点
		while (!minHeap.isEmpty()) {
			ListNode smallest = minHeap.poll();
			cur.next = smallest;
			cur = cur.next;

			// 如果当前节点有下一个节点，则将其加入堆
			if (smallest.next != null) {
				minHeap.add(smallest.next);
			}
		}

		// 返回合并后的链表（跳过虚拟头节点）
		return dummy.next;
	}
}
