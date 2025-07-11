package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 数字序列比大小
 * <p>
 * 一、题目描述 <p>
 * A，B两个人玩一个数字比大小的游戏，在游戏前，两个人会拿到相同长度的两个数字序列，两个数字序列不相同且其中的数字是随机的。
 * <p>
 * A，B各自从数字序列中挑选出一个数字进行大小比较，赢的人得1分，输的人扣1分，相等则各自的分数不变，用过的数字需要丢弃。
 * <p>
 * 求A可能赢B的最大分数。
 * <p>
 * 二、输入描述
 * <p>
 * 输入数据的第一个数字表示数字序列的长度N，后面紧跟着两个长度为N的数字序列。
 * <p>
 * 三、输出描述
 * <p>
 * A可能赢B的最大分数。
 * <p>
 * 这里要求计算A可能赢B的最大分数，不妨假设，A知道B的数字序列，且总是B先挑选数字并明示；
 * <p>
 * 可以采用贪心策略，能赢的一定要赢，要输的尽量减少损失。
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 3
 * 1 3 5
 * 2 4 6
 *
 * 2、输出
 * 1
 * </pre>
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 3
 * 7 8 9
 * 1 2 3
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <pre>
 * 测试用例3
 *
 * 1、输入
 * 5
 * 3 8 2 6 4
 * 5 1 9 7 3
 *
 * 2、输出
 * 3
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-11 9:24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxScoreGame {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字序列的长度N: ");
		int n = scanner.nextInt();
		scanner.nextLine();
		System.out.print("请输入数字序列A: ");
		int[] numsA = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入数字序列B: ");
		int[] numsB = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		scanner.close();

		System.out.println(getMaxScore(numsA, numsB));
	}

	public static int getMaxScore(int[] a, int[] b) {
		Arrays.sort(a); // 排序后便于贪心策略应用
		Arrays.sort(b);

		int score = 0;
		int left = 0;             // A的左指针（最小牌）
		int right = a.length - 1; // A的右指针（最大牌）

		// 遍历B的牌，从小到大出
		for (int i = 0; i < b.length; i++) {
			/*
			 * b.length - 1 - i
			 * 这是在遍历 B的数字序列时，对B使用了倒序遍历。也就是说，i 从 0 开始递增，但 b[b.length - 1 - i] 实际上是从 B 的最大值开始逐个往前看的。
			 */
			if (a[right] > b[b.length - 1 - i]) {
				// 如果A最大牌可以赢B最大牌，用它赢，得+1
				score++;
				right--;
			} else if (a[left] > b[i]) {
				// 如果A最小牌就能赢B当前牌，用它赢，得+1
				score++;
				left++;
			} else if (a[left] < b[i]) {
				// 如果A最小牌还比B小，送掉，扣1分
				score--;
				left++;
			} else {
				// 否则平局，不加不减
				left++;
			}
		}

		return score;
	}
}
