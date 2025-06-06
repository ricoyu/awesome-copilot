package com.awesomecopilot.algorithm.hwod.round2;

/**
 * 最大连续文件之和
 * <p>
 * 一、题目描述 <p>
 * 区块链底层存储是一个链式文件系统，由顺序的N个文件组成，每个文件的大小不一，依次为F1,F2…Fn。
 * <p>
 * 随着时间的推移，所占存储会越来越大。
 * <p>
 * 云平台考虑将区块链按文件转储到廉价的SATA盘，只有连续的区块链文件才能转储到SATA盘上，且转储的文件之和不能超过SATA盘的容量。
 * <p>
 * 假设每块SATA盘容量为M，求能转储的最大连续文件大小之和。
 * <p>
 * 二、输入描述 <p>
 * 第一行为SATA盘容量M，1000<=M<=1000000
 * <p>
 * 第二行为区块链文件大小序列F1,F2…Fn。其中 1<=n<=100000， 1<=Fi<=500
 * <p>
 * 三、输出描述 <p>
 * 求能转储的最大连续文件大小之和
 * <p>
 * <pre>
 * 示例1
 * 1、输入
 * 1000
 * 100 300 500 400 400 150 100
 *
 * 2、输出
 * 950
 * </pre>
 * <p>
 * 使用双指针（滑动窗口）的方法：
 * <ul>
 *     <li/>维护一个当前窗口 [left, right]，窗口内的文件大小总和 sum。
 *     <li/>向右移动 right 指针，将文件加入窗口。
 *     <li/>如果 sum 超过了 M，则左指针 left 向右缩小窗口，直到 sum <= M。
 *     <li/>每次更新符合条件的最大和。
 * </ul>
 * <p>
 * 该算法时间复杂度为 O(n)，适合处理文件数较多（如 10 万）的场景。
 * <p/>
 * Copyright: Copyright (c) 2025-05-24 9:20
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxContinuousFileSum {

	public static void main(String[] args) {
		// 示例测试
		int M = 1000;
		int[] files = {100, 300, 500, 400, 400, 150, 100};

		System.out.println(getMaxSum(M, files));  // 输出: 950
	}

	public static int getMaxSum(int M, int[] files) {
		int left = 0;   // 滑动窗口左边界
		int sum = 0;    // 当前窗口内的文件大小之和
		int maxSum = 0; // 满足条件的最大连续和

		for (int right = 0; right < files.length; right++) {
		    sum+=files[right]; // 向窗口添加当前文件

			// 若当前窗口总和超过容量M，则缩小窗口
			while (sum>M) {
				sum-=files[left++]; // 缩小窗口
			}

			// 更新符合条件的最大和
			maxSum = Math.max(maxSum, sum);
		}

		return maxSum;
	}
}
