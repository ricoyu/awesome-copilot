package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 无向图染色
 * <p>
 * 一、题目描述
 * <p>
 * 给一个无向图染色，可以填红黑两种颜色，必须保证相邻的两个节点不能同时为红色，输出有多少种不同的染色方案？
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入M(图中节点数) N(边数)
 * <p>
 * 后续N行格式为：V1 V2表示一个V1到V2的边。
 * <p>
 * 数据范围：1 <= M <= 15, 0 <= N <= M * 3, 不能保证所有节点都是连通的。
 * <p>
 * 三、输出描述
 * <p>
 * 输出一个 数字 表示染色方案的个数。
 * <p>
 * 说明
 * <pre>
 * 0 < N < 15
 * 0 <= M <= N * 3
 * 0 <= s, t < n
 * </pre>
 *
 * 不保证图连通 <p>
 * 保证没有重边和自环
 * <p>
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 4 4
 * 1 2
 * 2 4
 * 3 4
 * 1 3
 *
 * 2、输出
 * 7
 * </pre>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 3 0
 *
 * 2、输出
 * 8
 * </pre>
 * 以测试用例1为例：
 * <pre>
 * 4个节点，4条边：
 * 1-2
 * 2-4
 * 3-4
 * 1-3
 * </pre>
 * <p/>
 * 染色方案总数为2的四次方=16，我们枚举 0~15 的二进制：
 * <ul>比如 0101 表示节点1红，2黑，3红，4黑 (从低位往高位看)
 *     <li/>检查边1-2：节点1红(1)，2黑(0) ✅
 *     <li/>检查边2-4：2黑，4黑 ✅
 *     <li/>检查边3-4：3红，4黑 ✅
 *     <li/>检查边1-3：1红，3红 ❌，不合法
 *     <li/>比如 0000 全黑 ✅ 合法
 *     <li/>比如 1111 全红 ❌ 不合法，因为所有边都违反
 * </ul>
 * <ul>技巧总结
 *     <li/>使用位运算 高效表达状态（如 state >> i & 1 判断节点i是否为红色）
 *     <li/>由于节点数较小（最多15个），可以放心使用暴力枚举
 * </ul>
 * Copyright: Copyright (c) 2025-05-27 8:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GraphColoring {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入节点个数：");
		int M = scanner.nextInt();
		System.out.print("请输入边数：");
		int N = scanner.nextInt();
		System.out.println("节点个数：" + M + " 边数：" + N);
		scanner.nextLine();

		// 用邻接表存图
		List<int[]> edges = new ArrayList<int[]>();
		for (int i = 0; i < N; i++) {
			System.out.print("请输入第"+(i+1)+"条边的起点和终点：");
			String[] parts = scanner.nextLine().trim().split(" ");
			int[] edge = new int[2];
			edge[0] = Integer.parseInt(parts[0]);
			edge[1] = Integer.parseInt(parts[1]);
			edges.add(edge);
		}

		int totalValid = 0;
		int totalStates = 1<<M;// 枚举所有染色方式，总共2^M种

		for (int state = 0; state < totalStates; state++) {
		    boolean valid = true;
			for (int[] edge : edges) {
				int u = edge[0];
				int v = edge[1];
				// 检查是否违反：相邻节点不能同时为红色（即二进制位为1）
				if (((state >> u) & 1) == 1 && ((state >> v) & 1) ==1) {
					valid = false;
					break;
				}
			}
			if (valid) {
				totalValid++;
			}
		}

		System.out.print(totalValid);
	}
}
