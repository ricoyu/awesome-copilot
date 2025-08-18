package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 多数元素
 * <p>
 * 给定一个大小为n的数组nums, 返回其中的多数元素。多数元素是指在数组中出现次数大于 ⌊ n/2 ⌋ 的元素。
 * <p>
 * 你可以假设数组是非空的，并且给定的数组总是存在多数元素。
 * <pre>
 * 示例 1：
 * 输入：nums = [3,2,3] <br/>
 * 输出：3
 * </pre>
 * <pre>
 * 示例 2：
 * 输入：nums = [2,2,1,1,1,2,2]
 * 输出：2
 * </pre>
 *
 * 这道题可以使用几种不同的方法来解决，例如排序、哈希表统计或者摩尔投票法。 <p>
 * 在这里，我将选择摩尔投票法（Boyer-Moore Voting Algorithm）, 因为它的时间复杂度是O(n), 空间复杂度是O(1), 非常高效。
 * <p>
 * <ul>摩尔投票法的基本思想：
 *     <li/>维护一个候选元素和一个计数器
 *     <li/>遍历数组时，如果遇到与候选元素相同的元素，计数器加 1
 *     <li/>如果遇到不同的元素，计数器减 1
 *     <li/>当计数器为 0 时，更换候选元素为当前元素，并将计数器重置为 1
 *     <li/>最终剩下的候选元素就是多数元素
 * </ul>
 * 经过一轮遍历后，candidate就是出现次数超过半数的元素。
 *
 * <p/>
 * Copyright: Copyright (c) 2025-08-15 8:21
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MajorityElementFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(majorityElement(nums));
		scanner.close();
	}

	private static int majorityElement(int[] nums) {
		if (nums == null || nums.length == 0) {
			return -1 ;
		}
		int candidate = nums[0];
		int count = 1;

		for (int i = 1; i < nums.length; i++) {
		    if (nums[i] == candidate) {
				count++;
		    } else {
				count--;
				if (count == 0) {
					candidate = nums[i];
					count = 1;
				}
		    }
		}

		return candidate;
	}
}
