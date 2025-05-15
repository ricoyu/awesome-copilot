package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 统计友好度最大值
 * <p>
 * 一、题目描述
 * <p>
 * 工位由序列F1,F2…Fn组成，Fi值为0、1或2。其中0代表空置，1代表有人，2代表障碍物。
 * <p>
 * 1、某一空位的友好度为左右连续老员工数之和 <p>
 * 2、为方便新员工学习求助，优先安排友好度高的空位 <p>
 * <p>
 * 给出工位序列，求所有空位中友好度的最大值。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为工位序列：F1,F2…Fn组成，1<=n<=100000，Fi值为0、1或2。其中0代表空置，1代码有人，2代表障碍物 <p>
 * 其中0代表空置，1代码有人，2代表障碍物。
 * <p>
 * 三、输出描述
 * <p>
 * 所有空位中友好度的最大值。如果没有空位，返回0。
 * <p>
 * <pre>
 * 1、输入
 * 1 1 0 1 2 1 0
 *
 * 2、输出
 * 3
 * </pre>
 * <ul>核心算法思路
 *     <li/>从左到右遍历数组。
 *     <li/>对每个空位（即值为 0 的位置）：
 *     <ul>
 *          <li/>向左统计连续的 1（遇到 2 或边界就停）；
 *          <li/>向右统计连续的 1；
 *          <li/>总和即为该空位的友好度。
 *      </ul>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-04-30 8:25
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxFriendlinessFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入工位序列, 空格隔开:");
		String workstation = scanner.nextLine().trim();
		String[] parts = workstation.split(" ");
		int[] seates = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			seates[i] = Integer.parseInt(parts[i]);
		}
		System.out.println(findMaxFriendliness(seates));
	}

	/**
	 * 计算工位序列中所有空位的最大友好度
	 * @param seates 工位序列数组（0=空位, 1=有人, 2=障碍物）
	 * @return 所有空位中的最大友好度
	 */
	private static int findMaxFriendliness(int[] seates) {
		int maxFriendliness = 0;
		int n = seates.length;

		for (int i = 0; i < n; i++) {
		    if (seates[i] == 0) {
			    // 向左统计连续的1
			    int left = 0;
				int j = i-1;
				while (j>=0 && seates[j] == 1) {
					left++;
					j--;
				}

			    // 向右统计连续的1
			    int right = 0;
				j=i+1;
				while (j<n && seates[i] == 1) {
					right++;
					j++;
				}
				maxFriendliness = Math.max(maxFriendliness, left + right);
		    }
		}
		return maxFriendliness;
	}
}
