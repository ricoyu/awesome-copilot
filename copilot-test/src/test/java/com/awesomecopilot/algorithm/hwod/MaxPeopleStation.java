package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 人数最多的站点
 * <p>
 * 一、题目描述
 * <p>
 * 公园园区提供小火车单向通行，从园区站点编号最小到最大通行（如1→2→3→4→1），然后供员工在各个办公园区穿梭。通过对公司N个员工调研统计到每个员工的坐车区间，包含前后站点，请设计一个程序计算出小火车在哪个园区站点时人数最多。
 * <p>
 * 二、输入描述
 * <p>
 * 第1行：为调研员工人数。
 * <p>
 * 第2行开始：为每个员工的上车站点和下车站点。
 * <p>
 * 使用数字代表每个园区且为整数分割，如3表示从第3个园区上车，在第5个园区下车。
 * <p>
 * 三、输出描述
 * <p>
 * 人数最多时的园区站点编号，最多人数同时出现在的园区站点最小的园区站点。
 * <p>
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 3
 * 1 3
 * 2 4
 * 3 1
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <ul>示例1处理流程
 *     <li/>1 → 3：加1于 1, 2
 *     <li/>2 → 4：加1于 2, 3
 *     <li/>3 → 1：加1于 3, 4
 * </ul>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 5
 * 2 5
 * 3 6
 * 1 4
 * 5 2
 * 4 1
 *
 * 2、输出
 * 4
 * </pre>
 *
 * <ul>核心思路
 *     <li/>站点为环形结构（如 1 → 2 → 3 → 4 → 1），不能简单地认为下车点大于上车点。
 *     <li/>使用一个数组 cnt[]，记录每个站点变化的人数。
 *     <li/>对每个员工的上下车站点进行区间+1标记（注意环形）
 *     <li/>最后统计每个站点经过多少人，找到最多的，并返回最小编号的站点。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-05-10 10:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxPeopleStation {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入调研员工人数: ");
		int n = scanner.nextInt();
		scanner.nextLine(); //情况输入缓存区
		int[][] data = new int[n][2];
		for (int i = 0; i < n; i++) {
			System.out.print("请输入第" + (i + 1) + "员工上下车站点: ");
			String input = scanner.nextLine().trim();
			int[] station = new int[2];
			station[0] = Integer.parseInt(input.split(" ")[0]);
			station[1] = Integer.parseInt(input.split(" ")[1]);
			data[i] = station;
		}

		//找出最大的站点
		int maxStation = 0;
		for (int i = 0; i < n; i++) {
			int[] station = data[i];
			if (station[0] > maxStation) {
				maxStation = station[0];
			}
			if (station[1] > maxStation) {
				maxStation = station[1];
			}
		}

		int[] count = new int[maxStation + 2];
		for (int i = 0; i < n; i++) {
			int[] station = data[i];
			int start = station[0];
			int end = station[1];

			if (start < end) {
				for (int j = start; j < end; j++) {
					count[j]++;
				}
			} else if (start == end) {
				continue;
			} else {
				for (int j = start; j <= maxStation; j++) {
					count[j]++;
				}
				for (int j = 1; j < end; j++) {
					count[j]++;
				}
			}
		}

		int maxPeople = 0;
		int stationNum = 0;
		for (int i = 1; i < count.length; i++) {
		    if (count[i] > maxPeople) {
				maxPeople = count[i];
				stationNum = i;
		    }
		}

		System.out.println("最多人数同时出现在的园区站点最小的园区站点: " + stationNum);
	}
}
