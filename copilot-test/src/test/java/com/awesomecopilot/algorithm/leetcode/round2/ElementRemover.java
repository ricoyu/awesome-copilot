package com.awesomecopilot.algorithm.leetcode.round2;

import com.awesomecopilot.common.lang.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 移除元素
 * <p>
 * 给你一个数组 nums 和一个值 val，你需要原地移除所有数值等于 val 的元素。元素的顺序可能发生改变。然后返回 nums 中与 val 不同的元素的数量。 <p>
 * 假设 nums 中不等于 val 的元素数量为 k，要通过此题，您需要执行以下操作： <p>
 * 更改 nums 数组，使 nums 的前 k 个元素包含不等于 val 的元素。nums 的其余元素和 nums 的大小并不重要。<br/>
 * 返回 k。<br/>
 * <pre>
 * 示例 1：
 * 输入：nums = [3,2,2,3], val = 3
 * 输出：2, nums = [2,2,_,_]
 * 解释：你的函数函数应该返回 k = 2, 并且 nums 中的前两个元素均为 2。
 * 你在返回的 k 个元素之外留下了什么并不重要（因此它们并不计入评测）。
 * </pre>
 * <pre>
 * 示例 2：
 * 输入：nums = [0,1,2,2,3,0,4,2], val = 2
 * 输出：5, nums = [0,1,4,0,3,_,_,_]
 * 解释：你的函数应该返回 k = 5，并且 nums 中的前五个元素为 0,0,1,3,4。
 * 注意这五个元素可以任意顺序返回。
 * 你在返回的 k 个元素之外留下了什么并不重要（因此它们并不计入评测）。
 * </pre>
 * <p>
 * <ol>我们可以采用双指针（快慢指针）的方法来解决：
 *     <li/>定义一个慢指针 i，用于指向处理后数组的末尾位置
 *     <li/>定义一个快指针 j，用于遍历遍历整个数组
 *     <li/>当快指针指向的元素不等于 val 时，将该元素复制到慢指针位置，然后同时慢指针向前移动一步
 *     <li/>最终慢指针的值就是不等于 val 的元素数量
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-08-14 7:48
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ElementRemover {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入整数val: ");
		int val = scanner.nextInt();
		System.out.println(removeElement(nums, val));
		ArrayUtils.print(nums);
	}

	public static int removeElement(int[] nums, int val) {
		if (nums == null || nums.length == 0) {
			return 0;
		}

		// 慢指针i，指向处理后数组的最后一个有效元素
		int slow = 0;
		for (int fast = 0; fast < nums.length; fast++) {
			// 当快指针指向的元素不等于val时, 将该元素复制到慢指针位置
			if (nums[fast] != val) {
				nums[slow] = nums[fast];
				slow++;
			}
			// 当快指针指向的元素等于val时，不做任何操作，直接跳过
		}
		// 慢指针i的值就是不等于val的元素数量
		return slow;
	}
}
