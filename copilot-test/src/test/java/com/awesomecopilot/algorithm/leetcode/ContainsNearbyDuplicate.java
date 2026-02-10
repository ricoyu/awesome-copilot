package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 存在重复元素 II
 * <p>
 * 给你一个整数数组 nums 和一个整数 k ，判断数组中是否存在两个 不同的索引 i 和 j ，满足 nums[i] == nums[j] 且 abs(i - j) <= k 。如果存在，返回 true ；否则，返回 false 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,2,3,1], k = 3
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [1,0,1,1], k = 1
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [1,2,3,1,2,3], k = 2
 * 输出：false
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-02-03 9:12
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ContainsNearbyDuplicate {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入k: ");
		int k = scanner.nextInt();
		System.out.println(containsNearbyDuplicate(nums, k));
		scanner.close();
	}

	private static boolean containsNearbyDuplicate(int[] nums, int k) {
		Map<Integer, Integer> map = new HashMap<>();
		for (int i=0; i<nums.length; i++) {
			int num = nums[i];
			if (map.containsKey(num)) {
				int index = map.get(num);
				if (Math.abs(i - index) <=k) {
					return true;
				}
			}
			map.put(num, i);
		}
		return false;
	}
}
