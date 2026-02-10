package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 汇总区间
 * <p>
 * 给定一个 无重复元素的 有序 整数数组 nums 。
 * <p>
 * 区间 [a,b] 是从 a 到 b（包含）的所有整数的集合。
 * <p>
 * 返回 恰好覆盖数组中所有数字 的 最小有序 区间范围列表 。也就是说，nums 的每个元素都恰好被某个区间范围所覆盖，并且不存在属于某个区间但不属于 nums 的数字 x 。
 *
 * <ul>列表中的每个区间范围 [a,b] 应该按如下格式输出：
 *     <li/>"a->b" ，如果 a != b
 *     <li/>"a" ，如果 a == b
 * </ul>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [0,1,2,4,5,7]
 * 输出：["0->2","4->5","7"]
 * 解释：区间范围是：
 * [0,2] --> "0->2"
 * [4,5] --> "4->5"
 * [7,7] --> "7"
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [0,2,3,4,6,8,9]
 * 输出：["0","2->4","6","8->9"]
 * 解释：区间范围是：
 * [0,0] --> "0"
 * [2,4] --> "2->4"
 * [6,6] --> "6"
 * [8,9] --> "8->9"
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-02-08 12:30
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IntervalSummary {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入整数数组 nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(summaryRanges(nums));
		scanner.close();
	}

	/**
	 * 核心方法：生成区间汇总列表
	 * @param nums
	 * @return
	 */
	public static List<String> summaryRanges(int[] nums) {
		// 初始化结果列表
		List<String> result = new ArrayList<>();
		// 处理空数组的边界情况
		if (nums == null || nums.length == 0) {
			return result;
		}
		// start标记当前区间的起始位置，end标记当前区间的结束位置
		int start = nums[0];
		int end = nums[0];

		// 从第二个元素开始遍历数组
		for (int i = 1; i < nums.length; i++) {
			if (nums[i] == end + 1) {
				end = nums[i];
			}else {
				// 否则，将当前区间转化为指定格式并加入结果列表
				result.add(convertToRange(start, end));
				// 重置start和end为当前元素，开始新的区间
				start = nums[i];
				end = nums[i];
			}
		}

		// 处理最后一个未加入的区间（遍历结束后，最后一个区间还未添加）
		result.add(convertToRange(start, end));
		return result;
	}

	/**
	 * 辅助方法：将[start, end]转换为指定格式的字符串
	 * @param start 区间起始值
	 * @param end 区间结束值
	 * @return 格式为"a"或"a->b"的字符串
	 */
	private static String convertToRange(int start, int end) {
		if (start == end) {
			// 起始和结束相等，直接返回单个数字的字符串
			return String.valueOf(start);
		} else {
			// 起始和结束不等，返回"a->b"格式
			return start + "->" + end;
		}
	}
}
