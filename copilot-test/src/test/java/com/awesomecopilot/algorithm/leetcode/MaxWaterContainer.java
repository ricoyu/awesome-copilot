package com.awesomecopilot.algorithm.leetcode;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 盛最多水的容器
 * <p>
 * 给定一个长度为 n 的整数数组 height 。有 n 条垂线，第 i 条线的两个端点是 (i, 0) 和 (i, height[i]) 。
 * <p>
 * 找出其中的两条线，使得它们与 x 轴共同构成的容器可以容纳最多的水。
 * <p>
 * 返回容器可以储存的最大水量。
 * <p>
 * 示例1:
 * <image src="images/water.png"/>
 * <pre>
 * 输入：[1,8,6,2,5,4,8,3,7]
 * 输出：49
 * 解释：图中垂直线代表输入数组 [1,8,6,2,5,4,8,3,7]。在此情况下，容器能够容纳水（表示为蓝色部分）的最大值为 49。
 * </pre>
 * <pre>
 * 示例 2：
 *
 * 输入：height = [1,1]
 * 输出：1
 * </pre>
 * <p>
 * 说明：你不能倾斜容器。
 * <p>
 * <ul>核心采用双指针法：
 *     <li/>初始时，左右指针分别指向数组两端
 *     <li/>计算当前指针位置形成的容器水量（宽 × 高，宽为指针距离，高为较短垂线高度）
 *     <li/>移动高度较小的指针（因为移动较高指针只会使水量减少或不变）
 *     <li/>重复步骤 2-3，直到两指针相遇，记录过程中最大水量
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-12 9:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxWaterContainer {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组: ");
		int[] nums = Arrays.stream(scanner.nextLine().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(maxArea(nums));
	}

	/**
	 * 计算容器能容纳的最大水量
	 *
	 * @param height 表示垂线高度的数组
	 * @return 最大水量
	 */
	private static int maxArea(int[] height) {
		// 初始化最大水量为0
		int maxWater = 0;
		// 左指针，起始位置为数组第一个元素
		int left = 0;
		// 右指针，起始位置为数组最后一个元素
		int right = height.length - 1;

		// 当左右指针未相遇时继续循环
		while (left < right) {
			// 容器的高度由较短的垂线决定
			int currentHeight = Math.min(height[left], height[right]);
			// 计算当前指针位置形成的容器宽度（左右指针之间的距离）
			int width = right - left;
			// 计算当前容器的水量并更新最大水量
			int currentWater = width * currentHeight;
			maxWater = Math.max(maxWater, currentWater);

			// 移动高度较小的指针，寻找可能更大的水量
			if (height[left] < height[right]) {
				left++; // 左指针右移
			} else {
				right--; // 右指针左移
			}
		}
		return maxWater;
	}
}
