package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.HashMap;
import java.util.Map;

public class TwoSumSolution {

	public static int[] twoSum(int[] nums, int target) {
		Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < nums.length; i++) {
		    if (map.containsKey(target - nums[i])) {
				return new int[]{map.get(target - nums[i]), i};
		    } else {
				map.put(nums[i], i);
			}
		}
		// 根据题目假设，此处应不会执行，抛出异常以处理意外情况
		throw new IllegalArgumentException("No two sum solution found");
	}
}
