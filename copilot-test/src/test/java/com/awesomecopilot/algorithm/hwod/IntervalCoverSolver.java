package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * 区间交叠问题 - 贪心算法
 * <p>
 * 一、题目描述
 * <p>
 * 给定 坐标轴上的一组线段，线段的起点和终点均为整数并且长度不小于1，请你从中找到最少数量的线段，这些线段可以覆盖住所有线段。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入为所有线段的数量，不超过10000，后面每行表示一条线段，格式为“x,y”，x和y分别表示起点和终点，取值范围是[-105, 105]。
 * <p>
 * 三、输出描述
 * <p>
 * 最少线段数量，为正整数。
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 3
 * 1,4
 * 2,5
 * 3,6
 *
 * 2、输出
 * 2
 * </pre>
 *
 * 3、说明
 * <p>
 * 选择线段 [1,4] 和 [3,6]，它们的并集覆盖了所有原始线段的区域 [1,6]。
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 4
 * 1,2
 * 2,3
 * 3,4
 * 4,5
 *
 * 2、输出
 * 4
 * </pre>
 *
 * 3、说明
 * 需要选择所有线段 [1,2], [2,3], [3,4], [4,5] 来覆盖整个区间 [1,5]。
 *
 * <ul>核心解题思路（贪心）
 *     <li/>将所有线段按右端点从小到大排序。
 *     <li/>初始化一个集合 selectedSegments 用于存储选择的线段
 *     <li/>每次选择一个“能尽可能早覆盖当前未被覆盖线段的右端点”的线段加入结果集合。
 *     <li/>只要当前线段没有被任何已选线段覆盖，就必须选一条新线段来覆盖它
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-24 9:40
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IntervalCoverSolver {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入线段数量: ");
		int n = scanner.nextInt();
		scanner.nextLine();
		List<int[]> segments = new ArrayList<int[]>();
		for (int i = 0; i < n; i++) {
		    System.out.print("请输入第"+(i+1)+"条线段的起点和终点: ");
			int[] line = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
			segments.add(line);
		}

		int result = getMinCoverSegments(segments);
		System.out.println(result);
		scanner.close();
	}

	/**
	 * 贪心算法核心逻辑：选择最少的线段使得所有线段被覆盖
	 * @param segments
	 * @return
	 */
	public static int getMinCoverSegments(List<int[]> segments) {
		// 按照线段的右端点升序排列（贪心关键）
		segments.sort(Comparator.comparingInt(a -> a[1]));

		int count = 0;
		int converEnd = Integer.MIN_VALUE;

		for (int[] segment : segments) {
			int start = segment[0];
			int end = segment[1];

			// 如果当前线段未被上一个选中线段覆盖，则必须选择一条线段来覆盖它
			if (start > converEnd) {
				count++;
				converEnd = end;
			}
		}

		return count;
	}
}
