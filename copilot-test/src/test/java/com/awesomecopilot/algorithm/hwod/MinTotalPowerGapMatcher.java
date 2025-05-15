package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 实力差距最小总和
 * <p>
 * 一、题目描述
 * <p>
 * 游戏里面，队伍通过匹配实力相近的对手进行对战。
 * <p>
 * 但是如果匹配的队伍实力相差太大，对于双方游戏体验都不会太好。
 * <p>
 * 给定n个队伍的实力值，对其进行两两实力匹配，两支队伍实例差距在允许的最大差距d内，则可以匹配。
 * <p>
 * 要求在匹配队伍最多的情况下，匹配出的各组实力差距的总和最小。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行，n，d。队伍个数 n。允许的最大实力差距 d。(2<= n <=50, 0<= d <=100)。
 * <p>
 * 第二行，n个队伍的实力值，空格分割。(0<=各队伍实力值<=100)。
 * <p>
 * 三、输出描述
 * <p>
 * 匹配后，各组对战的实力差值的总和。若没有队伍可以匹配，则输出-1。
 * <p>
 * <pre>
 * 测试用例1
 * 1、输入
 * 6 20
 * 19 57 42 66 82 32
 *
 * 2、输出
 * 19
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>排序所有队伍的实力值。
 *     <li/>从前往后贪心匹配相邻两个队伍，只要差值在 d 以内就配对。
 *     <li/>匹配成功则记录差值并跳过这两个队伍，继续处理下一个。
 *     <li/>最终输出所有匹配的差值总和，若无任何一组匹配，返回 -1。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-05-06 8:47
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MinTotalPowerGapMatcher {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("队伍个数 n: ");
		int n = scanner.nextInt();
		System.out.print("允许的最大实力差距 d: ");
		int d = scanner.nextInt();
		int[] power = new int[n];
		for (int i = 0; i < n; i++) {
			System.out.print("请输入第 " + (i + 1) + " 个队伍实力值: ");
			power[i] = scanner.nextInt();
		}

		// 输出最小总差值
		System.out.println(minTotalPowerGap(power, d));
	}

	/**
	 * 计算最大匹配数量下的最小实力差值总和
	 *
	 * @param powers 队伍实力数组
	 * @param d      最大允许的差距
	 * @return 总差值和，若无法配对返回-1
	 */
	private static int minTotalPowerGap(int[] powers, int d) {
		Arrays.sort(powers); // 实力从小到大排序
		int totalDiff = 0;
		int i = 0;
		int pairCount = 0;

		while (i < powers.length - 1) {
			// 判断当前和下一个队伍能否匹配
			if (powers[i + 1] - powers[i] <= d) {
				// 成功匹配
				totalDiff += powers[i + 1] - powers[i];
				pairCount++;
				i += 2; // 跳过已经匹配的两个
			} else {
				i++; // 当前无法匹配，尝试下一个
			}
		}
		return pairCount > 0 ? totalDiff : -1;
	}
}
