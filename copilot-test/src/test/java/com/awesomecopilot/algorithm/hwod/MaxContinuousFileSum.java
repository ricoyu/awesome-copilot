package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 最大连续文件之和
 * <p>
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
 * 1、输入
 * 1000
 * 100 300 500 400 400 150 100
 *
 * 2、输出
 * 950
 * </pre>
 * <ol>这是一个典型的滑动窗口（Sliding Window）问题。滑动窗口技术可以用来高效地解决需要遍历连续子数组的问题。具体步骤如下：
 *     <li/>初始化指针和变量：使用两个指针left和right来表示窗口的左右边界，初始时都指向数组的起始位置。
 *          current_sum记录当前窗口内文件的大小之和，max_sum记录满足条件的最大和。
 *     <li/>滑动窗口:
 *     <li/>移动right指针，将F[right]加到current_sum中
 *     <li/>如果current_sum超过M，则移动left指针，从current_sum中减去F[left]，直到current_sum小于或等于M。
 *     <li/>在每次调整窗口后，如果current_sum <= M，则更新max_sum
 *     <li/>终止条件：当right指针遍历完整个数组时，max_sum即为所求
 * </ol>
 * <p>
 * 请用Java实现并给出核心的解题思路, 分析过程要精简, 不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-04-23 12:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxContinuousFileSum {

	public static void main(String[] args) {
		System.out.print("请输入SATA盘容量M:");
		Scanner scanner = new Scanner(System.in);
		int M = scanner.nextInt();
		System.out.print("请输入区块链文件大小序列[空格隔开]:");
		scanner.nextLine();
		String input = scanner.nextLine().trim();
		String[] arr = input.split(" ");
		int[] files = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
		    files[i] = Integer.parseInt(arr[i].trim());
		}

		int maxSum = findMaxContinuousFileSum(M, files);
		System.out.println("最大连续文件大小之和: " + maxSum);
	}

	public static int findMaxContinuousFileSum(int M, int[] files) {
		int left = 0;// 窗口左边界
		int currentSum = 0; // 当前窗口内文件大小之和
		int maxSum = 0; // 满足条件的最大和
		for (int right = 0; right < files.length; right++) {
			currentSum += files[right];// 将右边界文件加入当前和

			// 如果当前和超过M，移动左边界直到当前和<=M
			while (currentSum > M && left <= right) {
				currentSum -= files[left];
				left++;
			}

			// 更新最大和
			if (currentSum <= M && currentSum >= maxSum) {
				maxSum = currentSum;
			}
		}

		return maxSum;
	}
}
