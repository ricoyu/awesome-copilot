package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * 存在重复元素
 * <p>
 * 给你一个整数数组 nums 。如果任一值在数组中出现 至少两次 ，返回 true ；如果数组中每个元素互不相同，返回 false 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,2,3,1]
 *
 * 输出：true
 *
 * 解释：
 *
 * 元素 1 在下标 0 和 3 出现。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [1,2,3,4]
 *
 * 输出：false
 *
 * 解释：
 *
 * 所有元素都不同。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [1,1,1,3,3,4,3,2,4,2]
 *
 * 输出：true
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-02-02 9:28
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DuplicateElementChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字数组: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(containsDuplicate(nums));
		scanner.close();
	}

	private static boolean containsDuplicate(int[] nums) {
		Set<Integer> seenElements = new HashSet<>();
		for (Integer num : nums) {
			if (!seenElements.contains( num)) {
				seenElements.add(num);
			}else {
				return true;
			}
		}
		return false;
	}
}
