package com.awesomecopilot.algorithm.leetcode.round2;

import com.awesomecopilot.common.lang.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 删除有序数组中的重复项 II
 * <p>
 * 给你一个有序数组 nums ，请你原地删除重复出现的元素，使得出现次数超过两次的元素只出现两次 ，返回删除后数组的新长度。
 * <p>
 * 不要使用额外的数组空间，你必须在原地修改输入数组并在使用 O(1) 额外空间的条件下完成。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：nums = [1,1,1,2,2,3]
 * 输出：5, nums = [1,1,2,2,3]
 * 解释：函数应返回新长度 length = 5, 并且原数组的前五个元素被修改为 1, 1, 2, 2, 3。 不需要考虑数组中超出新长度后面的元素。
 * </pre>
 * <p>
 * 示例 2：
 * <pre>
 * 输入：nums = [0,0,1,1,1,1,2,3,3] <br/>
 * 输出：7, nums = [0,0,1,1,2,3,3] <br/>
 * 解释：函数应返回新长度 length = 7, 并且原数组的前七个元素被修改为 0, 0, 1, 1, 2, 3, 3。不需要考虑数组中超出新长度后面的元素。
 * </pre>
 * <p>
 * 这个问题可以使用一个双指针技巧来解决。我们使用两个指针：一个用于遍历数组（i），另一个用于标记可以插入元素的位置（j）。 <p>
 * 核心思想是当我们遍历数组时，如果一个数字的出现次数不超过两次，我们就将其复制到j指向的位置，然后增加j。如果一个数字出现超过两次，我们就跳过这个数字的后续出现。
 *
 * <ol>下面是具体步骤：
 *     <li/>如果数组长度小于等于2，直接返回数组长度。
 *     <li/>初始化两个指针：j（插入位置指针）初始化为1，因为第一个元素总是保留的；i（遍历数组的指针）从1开始。
 *     <li/>遍历数组，如果nums[i]和nums[j-1]相同，且nums[j]和nums[j-1]也相同，则跳过nums[i]因为这意味着这个数字已经出现了至少三次。
 *     <li/>否则，复制nums[i]到nums[j]，然后j++。
 *     <li/>遍历完成后，j就是新数组的长度。
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-08-15 8:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RemoveDuplicatesFromSortedArrayII {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组nums: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(removeDuplicates(nums));
		ArrayUtils.print(nums);
	}

	private static int removeDuplicates(int[] nums) {
		if (nums.length <=2) {
			return nums.length;
		}

		int j = 2;
		for (int i = 2; i < nums.length; i++) {
			if (nums[i] != nums[j - 2]) {
				nums[j++] = nums[i];
			}
		}
		return j;
	}
}
