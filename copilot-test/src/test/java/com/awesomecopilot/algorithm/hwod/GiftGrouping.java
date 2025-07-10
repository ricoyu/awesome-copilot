package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 计算礼品发放的最小分组数目
 * <p>
 * 一、题目描述
 * <p>
 * 又到了一年的末尾，项目组让小明负责新年晚会的小礼品发放工作。
 * <p>
 * 为使得参加晚会的同事所获得的小礼品价值相对平衡，需要把小礼品根据价格进行分组，但每组最多只能包括两件小礼品，并且每个分组的价格总和不能超过一个价格上限。
 * <p>
 * 为了保证发放小礼品的效率，小明需要找到分组数目最少的方案。
 * <p>
 * 你的任务是写一个程序，找出分组数最少的分组方案，并输出最少的分组数目。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行数据为分组礼品价格之和的上限
 * <p>
 * 第二行数据为每个小礼品的价格，按照空格隔开，每个礼品价格不超过分组价格和的上限。
 * <p>
 * 10
 * 5 5 10 2 7
 * <p>
 * 三、输出描述
 * <p>
 * 输出最小分组数量。
 *
 * <pre>
 * 测试用例1
 * 1、输入
 * 8
 * 3 3 4 5 2
 *
 * 2、输出
 * 3
 *
 * 3、说明
 * 价格为3和5可以配成一组。
 *
 * 价格为4和3可以配成一组。
 *
 * 价格为2只能单独一组。
 *
 * 因此，最少需要3组。
 * </pre>
 *
 * <pre>
 * 测试用例2
 * 1、输入
 * 6
 * 2 3 4 5
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>排序所有礼品价格，从小到大
 *     <li/>使用双指针：left 指向最便宜的礼品，right 指向最贵的礼品
 *     <li/>如果 prices[left] + prices[right] <= limit，两者配对，left++，right--
 *     <li/>否则，right-- 单独配对一个最贵的
 *     <li/>每次循环，都形成一个分组，计数器 groups++
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-10 9:59
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GiftGrouping {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入分组礼品价格之和的上限: ");
		int limit = scanner.nextInt(); //输入分组礼品价格之和的上限
		scanner.nextLine();
		System.out.print("输入每个小礼品的价格: ");
		int[] prices = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		System.out.println(minGroupCount(limit, prices));
		scanner.close();
	}

	public static int minGroupCount(int limit, int[] prices) {
		Arrays.sort(prices);

		int left = 0; // 指向最便宜的礼品
		int right = prices.length - 1; // 指向最贵的礼品
		int groups = 0; // 记录分组数量
		while (left <= right) {
			// 如果当前最便宜的和最贵的可以组成一组
			if (prices[left] + prices[right] <= limit) {
				left++; // 搭配成功，最便宜的礼品指针前移
			}
			// 最贵的礼品总是要分组，无论是否配对成功
			right--;// 最贵礼品指针后移
			groups++;// 成立一个分组
		}
		return groups;
	}
}
