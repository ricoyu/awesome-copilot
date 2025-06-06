package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 新员工座位安排系统
 * <p>
 * 一、题目描述 <p>
 * 工位由序列F1,F2…Fn组成，Fi值为0、1或2。其中0代表空置，1代表有人，2代表障碍物。
 * <p>
 * 1、某一空位的友好度为左右连续老员工数之和 <p>
 * 2、为方便新员工学习求助，优先安排友好度高的空位 <p>
 *
 * 给出工位序列，求所有空位中友好度的最大值。 <p>
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为工位序列：F1,F2…Fn组成，1<=n<=100000，Fi值为0、1或2。其中0代表空置，1代表有人，2代表障碍物
 * <p>
 * 三、输出描述 <p>
 * 所有空位中友好度的最大值。如果没有空位，返回0。
 * <p>
 * 核心解题思路
 * <ul>遍历数组，对每一个空位（值为 0）：
 *     <li/>向左和向右延伸，统计连续的老员工（值为 1）个数，遇到障碍物（2）或空位（0）停止。
 *     <li/>空位的友好度 = 左连续老员工数 + 右连续老员工数。
 *     <li/>
 * </ul>
 * 记录所有空位中的最大友好度，返回。
 * <p/>
 * Copyright: Copyright (c) 2025-05-30 8:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxFriendlinessSeat {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入工位序列, 逗号隔开:");
		String[] parts = scanner.nextLine().trim().split(",");
		int[] seats = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
		    			seats[i] = Integer.parseInt(parts[i]);
		}

		int maxFriendliness = 0;

		for (int i = 0; i < seats.length; i++) {
			// 如果当前位置是空位（0），计算其友好度
			int count = 0;
		    if (seats[i] ==0) {
			    // 向左计算连续的1（老员工）
				int left = i-1;
				while (left >= 0 && seats[left] == 1) {
					left--;
					count++;
				}
				int right = i+1;
				while (right < seats.length && seats[right] == 1) {
					count++;
					right++;
				}
		    }
			maxFriendliness = Math.max(maxFriendliness, count);
		}

		System.out.println("最大友好度是: " + maxFriendliness);
	}
}
