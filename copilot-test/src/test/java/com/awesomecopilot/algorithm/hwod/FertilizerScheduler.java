package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 农场施肥
 * <p>
 * 一、题目描述
 * <p>
 * 某农场主管理了一大片果园, fields[i]表示不同果园的面积, 单位m2, 现在要为所有的果园施肥且必须在n天之内完成, 否则影响收成。
 * 小布是果园的工作人员, 他每次选择一片果园进行施肥, 且一片国园施肥完后当天不再进行施肥作业。
 * <p>
 * 假设施肥机的能效为K, 单位：m2/day, 请问至少租赁能效K为多少的施肥机才能确保不影响收成？如果无法完成施肥任务, 则返回-1。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入为m和n, m表示fields中的元素个数, n表示施肥任务必须在n天内（含n天）完成；
 * <p>
 * 第二行输入为fields，fields[i]表示果林i的面积, 单位：m2。
 * <p>
 * 三、输出描述
 * <p>
 * 对于每组数据, 输出最小施肥机的能效k, 无多余空格。
 * <p>
 * 示例1:
 * <pre>
 * 1、输入
 * 5 9
 * 5 6 7 8 9
 *
 * 2、输出
 * 5
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-26 9:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FertilizerScheduler {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入果园数量m: ");
		int m = scanner.nextInt();
		System.out.print("请输入施肥天数n: ");
		int n = scanner.nextInt();
		scanner.nextLine();
		System.out.print("请输入每个果园的面积: ");
		String[] parts = scanner.nextLine().split(" ");
		scanner.close();
		int[] fields = new int[m];
		for (int i = 0; i < m; i++) {
			fields[i] = Integer.parseInt(parts[i]);
		}

		int result = minFertilizerRate(fields, n);
		System.out.println("最小施肥机效率: " + result);
	}

	/**
	 * 二分查找最小可行的施肥效率 K
	 *
	 * @param fields 每片果园的面积
	 * @param n      可用天数
	 * @return 最小施肥机效率 K, 若不能完成任务返回 -1
	 */
	public static int minFertilizerRate(int[] fields, int n) {
		// 如果果园数 > 天数，必定不可能（每片至少要花一天）
		if (fields.length > n) {
			return -1;
		}

		int left = 0;
		int right = Arrays.stream(fields).max().getAsInt();
		int ans = -1;

		while (left <= right) {
			int k = (right - left) / 2 + left; // 当前尝试的效率 K
			if (canFinish(fields, n, k)) {
				ans = k; // mid 足够，记录并尝试更小
				right = k - 1;
			} else {
				left = k + 1; // mid 不够，提高效率
			}
		}
		return ans;
	}

	/**
	 * 判断给定效率 k 下，能否在 n 天内完成所有果园施肥
	 *
	 * @param fields 果园面积数组
	 * @param n      可用天数
	 * @param k      施肥机效率（m2/天）
	 * @return 如果可以完成返回 true, 否则 false
	 */
	private static boolean canFinish(int[] fields, int n, int k) {
		int daysNeeded = 0;
		for (int area : fields) {
			// 每块果园需要 ceil(area / k) 天
			// (area + k - 1) / k 正好完成向上取整
			daysNeeded += (area + k - 1) / k;
			if (daysNeeded > n) {
				// 一旦超出 n 天就可以提前返回
				return false;
			}
		}
		return daysNeeded <= n;
	}
}
