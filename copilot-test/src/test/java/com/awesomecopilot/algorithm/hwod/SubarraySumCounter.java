package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 寻找连续区间
 * <p>
 * 一、题目描述
 * <p>
 * 给定一个含有N个正整数的数组，求出有多少连续区间（包括单个正整数），它们的和大于等于 x。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为两个整数 N,x。(0<N≤100000, 0≤x≤10000000)
 * <p>
 * 第二行有 N 个正整数 （每个正整数小于等于 100）。
 * <p>
 * 三、输出描述
 * <p>
 * 输出一个整数，表示所求的个数
 *
 * <ul>使用 双指针（滑动窗口）
 *     <li/>定义两个指针 left 和 right 表示子数组的起始和结束位置
 *     <li/>从前往后遍历数组，每次右指针向右扩展，使窗口内的和 sum ≥ x
 *     <li/>一旦当前窗口 [left, right] 的和 sum >= x，则从 left 开始的所有子数组 [left, right], [left, right+1], ..., [left, n-1]
 *     的和一定都大于等于 x（因为全是正整数，后续只会更大），这类子数组数量为 n - right。
 *     <li/>记录该数量后，左指针右移，收缩窗口，继续判断。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-02 9:44
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SubarraySumCounter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入学生目前排队情况：");
		String[] str = scanner.nextLine().split(" ");
		int N = Integer.parseInt(str[0]);
		int x = Integer.parseInt(str[1]);
		System.out.print("请输入" + N + " 个正整数, 空格隔开: ");
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.println(countSubarrays(nums, x));
	}

	/**
	 * 统计和大于等于x的连续子数组个数
	 *
	 * @param nums
	 * @param x
	 * @return
	 */
	public static int countSubarrays(int[] nums, int x) {
		int n = nums.length;
		int left = 0, right = 0; // 滑动窗口左右指针
		int count = 0;
		int sum = 0;

		while (left < n) {
			// 扩展右指针直到窗口内的和 sum >= x
			while (right < n && sum < x) {
				sum += nums[right];
				right++;
			}

			// 如果此时 sum >= x，则说明从 left 开始的所有子数组都合法
			if (sum >= x) {
				count += n - right + 1; // 包括 [left, right], [left, right+1], ..., [left, n-1]
			}

			// 移动左指针，缩小窗口
			sum -= nums[left];
			left++;
		}

		return count;
	}
}
