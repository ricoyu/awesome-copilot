package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * 在长度 2N 的数组中找出重复 N 次的元素
 * <p>
 * 给你一个整数数组 nums ，该数组具有以下属性：
 *  <p>
 * <ol>
 *     <li/>nums.length == 2 * n.
 *     <li/>nums 包含 n + 1 个 不同的 元素
 *     <li/>nums 中恰有一个元素重复 n 次
 * </ol>
 *
 * 找出并返回重复了 n 次的那个元素。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,2,3,3]
 * 输出：3
 * 示例 2：
 * </pre>
 *
 * <pre>
 * 输入：nums = [2,1,2,5,3,2]
 * 输出：2
 * 示例 3：
 * </pre>
 *
 * <pre>
 * 输入：nums = [5,1,5,2,5,3,5,4]
 * 输出：5
 * </pre>
 * 数组满足 长度 = 2n、含 n+1 个不同元素、仅 1 个元素重复 n 次 → 其余 n 个元素均为唯一出现。
 * <p>
 * 核心逻辑：遍历数组，第一个出现重复的元素，就是目标元素（因为唯一重复 n 次的元素是首个重复项，其余元素永不重复）。
 * <p/>
 * Copyright: Copyright (c) 2026-01-02 17:10
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RepeatedElementFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字数组: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(findRepeatedElement(nums));
	}

	private static int findRepeatedElement(int[] nums) {
		// 哈希集合：存储已遍历过的元素，利用其元素唯一性特性判断重复
		Set<Integer> existedElements = new HashSet<>();
		for (int num : nums) {
			if (!existedElements.add(num)) {
				return num;
			}
		}
		return -1;
	}
}
