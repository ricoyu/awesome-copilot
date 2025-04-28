package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 开放日活动、取出尽量少的球
 * <p>
 * 一、题目描述
 * <p>
 * 某部门开展 Family Day 开放日活动，其中有个从桶里取球的游戏。
 * <p>
 * 游戏规则如下：
 * <p>
 * 有 N 个容量一样的小桶等距排开，且每个小桶都默认装了数量不等的小球，每个小桶装的小球数量记录在数组 bucketBallNums 中。
 * <p>
 * 游戏开始时，要求所有桶的小球总数不能超过 SUM，如果小球总数超过 SUM，则需对所有的小桶统一设置一个容量最大值 maxCapacity 并需将超过容量最大值的小球拿出来，直至小桶里的小球数量小于 maxCapacity。
 * <p>
 * 限制规则一:
 * <p>
 * 所有小桶的小球总和小于SUM，则无需设置容量值maxCapacity，并且无需从小桶中拿球出来，返回结果[]
 * <p>
 * 限制规则二:
 * <p>
 * 如果所有小桶的小球总和大于SUM，则需设置容量最大值maxCapacity，并且需从桶中拿球出来，返回从每个桶中取出的小球教量组成的数组;
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入 2 个正整数，数字之间使用空格隔开，其中第一个数字表示 SUM，第二个数字表示 bucketBallNums 数组长度；
 * <p>
 * 第二行输入 N 个正整数，数字之间使用空格隔开，表示 bucketBallNums 的每一项；
 * <p>
 * 三、输出描述
 * <p>
 * 找到一个 maxCapacity，来保证取出尽量少的球，并返回从每个小桶拿出的小球数量组成的数组。
 *
 * <pre>
 * 1、输入
 * 3 3
 * 1 2 3
 *
 * 2、输出
 * [0, 1, 2]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-04-25 9:13
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BucketBallGame {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入两个正整数，第一个数字表示 SUM，第二个数字表示 bucketBallNums 数组长度：");
		String[] nums = scanner.nextLine().trim().split(" ");
		int SUM = Integer.parseInt(nums[0].trim());
		int length = Integer.parseInt(nums[1].trim());
		System.out.print("请输入 " + length + " 个正整数，数字之间使用空格隔开，表示 bucketBallNums 的每一项：");
		nums = scanner.nextLine().trim().split(" ");
		int[] bucketBallNums = new int[length];
		for (int i = 0; i < length; i++) {
			bucketBallNums[i] = Integer.parseInt(nums[i].trim());
		}

		int[] result = findMinBallsToRemove(SUM, bucketBallNums);
		System.out.println(Arrays.toString(result)); // 输出
		scanner.close();
	}

	/**
	 * 计算需要从每个桶中取出的小球数量
	 *
	 * @param SUM            小球总数上限
	 * @param bucketBallNums 每个桶中小球的初始数量
	 * @return 从每个桶中取出的小球数量数组
	 */
	public static int[] findMinBallsToRemove(int SUM, int[] bucketBallNums) {
		// 计算初始总和
		int total = Arrays.stream(bucketBallNums).sum();

		if (total <= SUM) {
			return new int[0]; // 规则一：无需取球
		}

		// 初始化二分查找的左右边界
		int left = 0;
		int right = Arrays.stream(bucketBallNums).max().getAsInt();

		// 二分查找寻找最大的maxCapacity，使得截断后的总和 <= SUM
		while (left < right) {
			int mid = left + (right - left) / 2;
			int sum = calculateSum(bucketBallNums, mid);
			if (sum > SUM) {
				right = mid;
			} else {
				left = mid + 1;
			}
		}

		// 找到的 maxCapacity 是 left - 1
		int maxCapacity = left - 1;
		// 计算每个桶需要取出的小球数量
		int[] removedBalls = new int[bucketBallNums.length];
		for (int i = 0; i < bucketBallNums.length; i++) {
			removedBalls[i] = Math.max(0, bucketBallNums[i] - maxCapacity);
		}

		return removedBalls;
	}

	/**
	 * 计算所有桶中小球数量不超过 maxCapacity 时的总和
	 *
	 * @param bucketBallNums 每个桶中小球的初始数量
	 * @param maxCapacity    当前尝试的maxCapacity
	 * @return 截断后的总和
	 */
	private static int calculateSum(int[] bucketBallNums, int maxCapacity) {
		int sum = 0;
		for (int num : bucketBallNums) {
			sum += Math.min(num, maxCapacity);
		}
		return sum;
	}
}
