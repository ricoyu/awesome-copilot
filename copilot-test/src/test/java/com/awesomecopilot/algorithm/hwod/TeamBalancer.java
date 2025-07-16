package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 英雄联盟 - 递归
 * <p>
 * 一、题目描述
 * <p>
 * 部门准备举办一场王者荣耀表演赛，有 10 名游戏爱好者参与，分为两队，每队 5 人。
 * <p>
 * 每位参与者都有一个评分，代表着他的游戏水平。
 * <p>
 * 为了表演赛尽可能精彩，我们需要把 10 名参赛者分为实力尽量相近的两队。一队的实力可以表示为这一队 5 名队员的评分总和。
 * <p>
 * 现在给你 10 名参与者的游戏水平评分，请你根据上述要求分队最后输出这两组的实力差绝对值。
 * <p>
 * 例: 10 名参赛者的评分分别为 5 1 8 3 4 6 7 10 9 2，分组为 (1 3 5 8 10) (2 4 6 7 9)，两组实力差最小，差值为 1。有多种分法，但实力差的绝对值最小为 1。
 * <p>
 * 二、输入描述
 * <p>
 * 10 个整数，表示 10 名参与者的游戏水平评分。范围在[1,10000]之间
 * <p>
 * 三、输出描述
 * <p>
 * 1 个整数，表示分组后两组实力差绝对值的最小值。
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 1 2 3 4 5 6 7 8 9 10
 *
 * 2、输出
 * 1
 * </pre>
 *
 * 3、说明 <p>
 * 10 名队员分成两组，两组实力差绝对值最小为 1。
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 3 3 3 3 3 3 3 3 3 9
 *
 * 2、输出
 * 6
 * </pre>
 * 代码逻辑说明:
 * <ul>递归搜索
 *     <li/>findMinDifference函数递归生成所有可能的5人组合。
 *     <li/>递归终止条件
 *     <li/>已选5人时，计算差值Math.abs(totalSum - 2 * currentSum)并更新最小值
 *     <li/>处理完所有评分时直接返回。
 *     <li/>递归过程：
 *     <li/>选择当前评分：增加selectedCount和currentSum，处理下一个评分。
 *     <li/>不选择当前评分：直接处理下一个评分。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-15 9:47
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TeamBalancer {
	private static int minDiff = Integer.MAX_VALUE; // 记录最小差值

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入10个评分: ");
		int[] scores = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		int totalSum = Arrays.stream(scores).sum();

		// 递归搜索所有可能的5人组合
		findMinDifference(scores, 0, 0, 0, totalSum);

		System.out.println(minDiff);
		scanner.close();
	}

	/**
	 * 递归搜索所有可能的5人组合，并计算最小差值
	 * @param scores 所有评分数组
	 * @param index 当前处理的评分索引
	 * @param selectedCount 已选的人数
	 * @param currentSum 当前已选评分的和
	 * @param totalSum 所有评分的总和
	 */
	private static void findMinDifference(int[] scores, int index, int selectedCount, int currentSum, int totalSum) {
		// 如果已经选了5人，计算差值并更新最小值
		if (selectedCount == 5) {
			int diff = Math.abs(totalSum - 2*currentSum);
			if (diff < minDiff) {
				minDiff = diff;
			}
			return;
		}

		// 如果已经处理完所有评分，直接返回
		if (index == 10) {
			return;
		}

		// 选择当前评分
		findMinDifference(scores, index + 1, selectedCount+1, currentSum+scores[index], totalSum);

		// 不选择当前评分
		findMinDifference(scores, index + 1, selectedCount, currentSum, totalSum);
	}
}
