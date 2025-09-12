package com.awesomecopilot.algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;

/**
 * 两数之和
 * <p>
 * 给定一个整数数组nums和一个整数目标值 target，请你在该数组中找出和为目标值 target  的那两个整数，并返回它们的数组下标。
 * <p>
 * 你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。
 * <p>
 * 你可以按任意顺序返回答案。
 * <p>
 * <pre>
 * 示例1
 * 输入：nums = [2,7,11,15], target = 9
 * 输出：[0,1]
 * 解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。
 * </pre>
 * <pre>
 * 示例 2：
 *
 * 输入：nums = [3,2,4], target = 6
 * 输出：[1,2]
 * </pre>
 * <p>
 * <pre>
 * 示例 3：
 *
 * 输入：nums = [3,3], target = 6
 * 输出：[0,1]
 * </pre>
 * 利用哈希表。因为哈希表可以存储已经遍历过的元素的值和对应的下标，这样对于当前元素num，我们只需要检查哈希表中是否存在target - num的值。
 *  <p>
 * 如果存在，就找到了这两个数，返回它们的下标。这种方法的时间复杂度是O(n)，因为只需要遍历一次数组，每次查找哈希表的时间是O(1)。
 * <p>
 * 那具体怎么实现呢？在Java中，可以用HashMap来存储。遍历数组，对于每一个元素，先计算补数（target - current），然后检查补数是否在map中存在。
 *  <p>
 * 如果存在，则返回当前索引和补数的索引。如果不存在，就将当前元素的值作为key，索引作为value存入map中。这样就可以保证在后续的元素中能够快速找到对应的补数。
 * <p/>
 * Copyright: Copyright (c) 2025-03-29 8:21
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TwoSumSolution {

	public int[] twoSum(int[] nums, int target) {
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
