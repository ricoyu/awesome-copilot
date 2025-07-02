package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 学生重新排队
 * <p>
 * 一、题目描述
 * <p>
 * m个学生排成一排，学生编号分别是1到m，m为3的整倍数。
 * <p>
 * 老师随机抽签决定将所有学生分成n个3人的小组，m=3*n为了便于同组学生交流，老师决定将小组成员安排到一起，也就是同组成员彼此相连，同组任意两个成员之间无其它组的成员。
 * <p>
 * 因此老师决定调整队伍，老师每次可以调整任何一名学生到队伍的任意位置，计为调整了一次，请计算最少调整多少次可以达到目标。
 * <p>
 * 注意：对于小组之间没有顺序要求，同组学生之间没有顺序要求
 * <p>
 * 二、输入描述
 * <p>
 * 两行字符串，空格分隔表示不同的学生编号。
 * <p>
 * 第一行是学生目前排队情况，第二行是随机抽签分组情况，从左开始每3个元素为一组n为学生的数量，m的范围为[3，900]，m一定为3的整数倍。第一行和第二行的元素个数一定相同。
 * <p>
 * 三、输出描述
 * <p>
 * 老师调整学生达到同组彼此相连的最小次数。
 *
 * <pre>
 * 1、输入
 * 7 9 8 5 6 4 2 1 3
 * 7 8 9 4 2 1 3 5 6
 *
 * 2、输出
 * 1
 * </pre>
 * <p>
 * 3、说明 <p>
 * 学生目前排队情况：7 9 8 5 6 4 2 1 3
 * <p>
 * 学生分组情况：[7 8 9]、[4 2 1]、[3 5 6]
 * <p>
 * 将3调整到4之前，队列调整为7 9 8 5 6 3 4 2 1
 * <p>
 * 那么三个小组成员均彼此相连[7 9 8]、[5 6 3]、[4 2 1]
 * <p/>
 * Copyright: Copyright (c) 2025-06-30 8:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StudentGroupReorder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入学生目前排队情况：");
		String[] current = scanner.nextLine().split(" ");
		System.out.print("输入学生分组情况：");
		String[] target = scanner.nextLine().split(" ");

		System.out.println(minAdjustments(current, target));
	}

	/**
	 * 计算最小的调整次数使得每组学生相邻
	 *
	 * @param current
	 * @param target
	 * @return
	 */
	public static int minAdjustments(String[] current, String[] target) {
		int m = current.length;
		int n = m / 3;

		// 第一步：建立学生编号 -> 小组编号 的映射
		Map<String, Integer> groupMap = new HashMap<>();
		for (int i = 0; i < n; i++) {
			//每3个为一组
			for (int j = 0; j < 3; j++) {
				groupMap.put(target[n * 3 + j], i);
			}
		}

		// 第二步：将当前队列转换为组号序列
		int[] groupSeq = new int[m];
		for (int i = 0; i < m; i++) {
			groupSeq[i] = groupMap.get(current[i]);
		}

		// 第三步：使用贪心 + 滑动窗口找出最多的合法块（连续3个相同组号）
		boolean[] used = new boolean[m];
		int validBlocks = 0;

		for (int i = 0; i < m - 3; i++) {
			// 尝试每个起点，是否能形成合法块，且位置未被占用
			if (!used[i] && !used[i + 1] && !used[i + 2]) {
				int g1 = groupSeq[i], g2 = groupSeq[i + 1], g3 = groupSeq[i + 2];
				if (g1 == g2 && g2 == g3) {
					validBlocks++;
					used[i] = used[i + 1] = used[i + 2] = true; // 标记这3个位置被使用
				}
			}
		}

		// 总共有n个组，已形成validBlocks个合法块，剩下的都需要移动
		return n - validBlocks;
	}
}
