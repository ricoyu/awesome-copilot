package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 体育场找座位
 * <p>
 * 一、题目描述
 * <p>
 * 在一个大型体育场内举办了一场大型活动，由于疫情防控的需要，要求每位观众的必须间隔至少一个空位才允许落座。现在给出一排观众座位分布图，座位中存在已落座的观众，请计算出，在不移动现有观众座位的情况下，最多还能坐下多少名观众。
 * <p>
 * 二、输入描述
 * <p>
 * 一个数组，用来标识某一排座位中，每个座位是否已经坐人。0表示该座位没有坐人，1表示该座位已经坐人。
 * <p>
 * 三、输出描述
 * <p>
 * 整数，在不移动现有观众座位的情况下，最多还能坐下多少名观众。
 *
 * <pre>
 * 1、输入
 * 10001
 *
 * 2、输出
 * 1
 * </pre>
 * <ul> 解题核心思路
 *     <li/>遍历座位数组，只要发现一个为0的位置，判断其左右两边是否都为0（或边界），如果满足条件则可以在该位置安排一位观众落座，并将其标记为1（代表已坐人），以防后续判断时重复使用。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-08 10:20
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StadiumSeating {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入座位排布图:");
		String input = scanner.nextLine().trim();
		int n = input.length();
		int[] seats = new int[n];
		for (int i = 0; i < n; i++) {
			seats[i] = input.charAt(i) - '0';
		}

		System.out.println(maxAdditionalPeople(seats));
	}

	/**
	 * 计算在当前座位分布下，最多还能坐下多少名观众
	 * @param seats 一个数组表示一排座位，0表示空位，1表示有人
	 * @return 最多还能坐下的人数
	 */
	public static int maxAdditionalPeople(int[] seats) {
		int count = 0;
		int n = seats.length;

		for (int i = 0; i < n; i++) {
			// 只有当前座位是空的，才考虑能不能坐人
			if (seats[i] == 0) {
				// 判断当前座位左边是否安全（i==0表示最左边）
				boolean leftSafe = (i == 0 || seats[i - 1] == 0);
				// 判断当前座位右边是否安全（i==n-1表示最右边）
				boolean rightSafe = (i == n - 1 || seats[i + 1] == 0);

				// 如果左右都为空或是边界，就可以坐人
				if (leftSafe && rightSafe) {
					seats[i] = 1; // 安排观众坐下
					count++;      // 统计一个人
				}
			}
		}

		return count;
	}
}
