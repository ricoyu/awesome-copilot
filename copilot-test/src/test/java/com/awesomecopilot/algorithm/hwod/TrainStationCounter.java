package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 小火车最多人时所在园区站点
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
 * <pre>
 * 测试用例1：
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
 * <ul>
 *     <li/>使用一个数组count来记录每个站点的当前人数。
 *     <li/>对于每个员工，根据其上车站点和下车站点，更新count数组。
 *     <li/>如果下车站点小于上车站点，说明小火车已经绕了一圈，需要分别处理从上车站点到最大站点和从1到下车站点的部分。
 *     <li/>
 * </ul>
 *
 * <ol>以测试用例1为例, 处理过程如下：
 *     <li/>第一个员工：1→3，count[1]++, count[2]++, count[3]++
 *     <li/>第二个员工：2→4，count[2]++, count[3]++, count[4]++
 *     <li/>第三个员工：3→1，由于1 < 3，且假设最大站点为4，所以需要处理3→4和1→（假设最大站点为4，1→4不处理，因为1→4与3→1不冲突）
 * </ol>
 *
 * 由于小火车是单向行驶，且站点编号是连续的，我们可以假设站点编号最大为maxStation，如果下车站点小于上车站点，说明小火车已经绕了一圈。
 * <p>
 * 但为了简化问题，我们可以假设站点编号最大为1000，且小火车不会绕多圈。如果题目中站点编号范围更大，可以动态调整。
 * <p/>
 * Copyright: Copyright (c) 2025-05-08 8:53
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TrainStationCounter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		System.out.print("员工人数: ");
		int n = scanner.nextInt();

		// 记录所有上下车信息，并确定最大站点编号
		int[][] trips = new int[n][2];
		int maxStation = 0; //最大的站点编号

		for (int i = 0; i < n; i++) {
			System.out.print("请输入第 " + (i + 1) + " 个员工的上车站点: ");
			trips[i][0] = scanner.nextInt();
			System.out.print("请输入第 " + (i + 1) + " 个员工的下车站点: ");
			trips[i][1] = scanner.nextInt();
			maxStation = Math.max(maxStation, Math.max(trips[i][0], trips[i][1]));
		}

		// 初始化一个数组来记录每个站点的当前人数
		int[] count = new int[maxStation + 1];// 站点从1开始

		// 读取每个员工的上车站点和下车站点，并更新count数组
		for (int i = 0; i < n; i++) {
			int start = trips[i][0];
			int end = trips[i][1];

			if (end < start) {
				// 处理绕圈的情况
				for (int j = start; j <= maxStation; j++) {
				    count[j]++;
				}

				for (int j = 1; j <= end; j++) {
				    count[j]++;
				}
			}else {
				for (int j = start; j <= end; j++) {
				    count[j]++;
				}
			}
		}

		// 找到人数最多的站点
		int maxCount = 0;
		int resultStation = 0;
		for (int i = 1; i <= maxStation; i++) {
		   if (count[i] > maxCount) {
			   maxCount = count[i];
			   resultStation = i;
		   }
		}

		// 输出结果
		System.out.println(resultStation);
	}
}
