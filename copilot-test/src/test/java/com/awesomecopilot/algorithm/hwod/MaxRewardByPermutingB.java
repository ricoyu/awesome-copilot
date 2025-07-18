package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 在规定时间内获得的最大报酬
 * <p>
 * 一、题目描述
 * <p>
 * 给定两个只包含数字的数组a、b，调整数组b里面数字的排列顺序，使得尽可能多的a[i] > b[i]。数组a和b中的数字各不相同。
 * <p>
 * 输出所有可以达到最优结果的a数组组数量
 * <p>
 * 二、输入描述
 * <p>
 * 输入的第一行是数组a中的数字，其中只包含数字，每两个数字之间相隔一个空格，a数组大小不超过10。
 * <p>
 * 输入的第二行是数组b中的数字，其中只包含数字，每两个数字之间相隔一个空格，b数组大小不超过10。
 * <p>
 * 三、输出描述
 * <p>
 * 输出所有可以达到最优结果的a数组数量
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 11 8 20
 * 10 13 7
 *
 * 2、输出
 * 1
 * </pre>
 *
 * 3、说明
 * 最优结果只有一个，a = [11, 20, 8]，故输出1
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 11 12 20
 * 10 13 7
 *
 * 2、输出
 * 2
 * </pre>
 *
 * 3、说明
 * 有两个a数组的排列可以达到最优结果，[12, 20, 11]和[11, 20, 12]，故输出2
 * <p/>
 * Copyright: Copyright (c) 2025-07-17 8:13
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxRewardByPermutingB {

	private static int maxWinCount = 0; // 最大胜利次数
	private static int count = 0;       // 达到最大胜利次数的 b 排列个数

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组a中的数字: ");
		int[] a = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入数组b中的数字: ");
		int[] b = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		scanner.close();

		// 使用回溯法枚举 b 的所有排列
		permuteAndEvaluate(a, b);
		System.out.println(count);
	}

	/**
	 * 回溯生成 b 的全排列并统计胜利次数
	 * @param a
	 * @param b
	 * @return
	 */
	private static void permuteAndEvaluate(int[] a, int[] b) {
		boolean[] used = new boolean[b.length];
		List<Integer> current = new ArrayList<>();
		backtrack(a,b,used, current);
	}

	/**
	 * 回溯生成 b 的全排列并统计胜利次数 <p>
	 * 回溯的整体目标 <p>
	 * 构造出 b 的每一个可能排列 <p>
	 * 每构造完一个排列 current，就和固定的 a 数组比较，看有多少个位置满足 a[i] > b[i]
	 * @param a        固定的数组
	 * @param b        要全排列的数组
	 * @param used     标记数组 b[i] 是否被用过
	 * @param current  当前构造出来的 b 的排列结果
	 */
	private static void backtrack(int[] a, int[] b, boolean[] used, List<Integer> current) {
		if (current.size() == b.length) {
			int winCount = 0;
			// 与固定 a 数组逐一比较
			for (int i = 0; i < a.length; i++) {
			    if (a[i] > current.get(i)) {
					winCount++;
			    }
			}

			// 记录最大胜利次数和对应排列数量
			if (winCount > maxWinCount) {
				maxWinCount = winCount;
				count = 1;
			} else if (winCount == maxWinCount) {
				count++;
			}

			return;
		}

		for (int i = 0; i < b.length; i++) {
		    if (!used[i]) {
				used[i] = true;
				current.add(b[i]);
				backtrack(a, b, used, current);
				used[i] = false;
				current.remove(current.size() - 1);
		    }
		}
	}
}
