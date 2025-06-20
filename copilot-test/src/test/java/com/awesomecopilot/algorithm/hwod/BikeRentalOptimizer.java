package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 租车骑绿道 - 双指针
 * <p>
 * 一、题目描述
 * <p>
 * 部门组织绿岛骑行团建活动，租用公共双人自行车骑行，每辆自行车最多坐两人、最大载重 M。
 * <p>
 * 给出部门每个人的体重，请问最多需要租用多少双人自行车。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行两个数字 m、n，自行车限重 m，代表部门总人数 n。
 * <p>
 * 第二行，n 个数字，代表每个人的体重。体重都小于等于自行车限重 m。
 * <p>
 * 0<m <= 200
 * <p>
 * 0 < n <= 1000000
 * <p>
 * 三、输出描述 <p>
 * 最小需要的双人自行车数量。
 * <pre>
 * 测试用例1：
 * 1、输入
 * 10 4
 * 9 8 6 5
 *
 * 2、输出
 * 4
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 10 5
 * 5 5 5 5 5
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <pre>
 * 测试用例3：
 * 1、输入
 * 7 6
 * 3 4 5 3 3 7
 *
 * 2、输出
 * 4
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-18 9:02
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BikeRentalOptimizer {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入自行车限重: ");
		int m = scanner.nextInt();
		System.out.print("请输入部门人数: ");
		int n = scanner.nextInt();
		int[] weights = new int[n];
		for (int i = 0; i < n; i++) {
		    System.out.print("请输入第" +(i+1)+" 个人的体重: ");
			weights[i] = scanner.nextInt();
		}

		System.out.println(minBikesNeeded(m, weights));
	}

	public static int minBikesNeeded(int m, int[] weights) {
		Arrays.sort(weights);
		int i = 0; // 最轻的人索引
		int j = weights.length - 1; // 最重的人索引
		int count = 0; // 自行车数量
		while (i<=j) {
			// 如果最轻和最重的可以一起骑，则两人一车
			if (weights[i] + weights[j] < m) {
				i++; // 配对成功，轻的人向右移
			}
			j--; // 无论配对成功与否，重的人都要骑车（单独或配对）
			count++;
		}

		return count;
	}
}
