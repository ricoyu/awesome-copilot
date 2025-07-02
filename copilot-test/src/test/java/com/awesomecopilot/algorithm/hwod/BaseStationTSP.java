package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 基站维护工程师数 - 动态规划
 * <p>
 * 一、题目描述
 * <p>
 * 小王是一名基站维护工程师，负责某区域的基站维护。某地方有 n 个基站 (1 < n < 10)，已知各基站之间的距离 s (0 <= s < 500)，并且基站 x 到基站 y 的距离，与基站 y 到基站 x 的距离并不一定会相同。
 * <p>
 * 小王从基站 1 出发，途经每个基站 1 次，然后返回基站 1。需要请你为他选择一条距离最短的路径。
 * <p>
 * 二、输入描述
 * <p>
 * 站点数 n 和各站点之间的距离 (均为整数)。
 * <p>
 * 三、输出描述
 * <p>
 * 最短路径的数值。
 * <p>
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 3
 * 0 2 1
 * 1 0 2
 * 2 1 0
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 4
 * 0 10 15 20
 * 10 0 35 25
 * 15 35 0 30
 * 20 25 30 0
 *
 * 2、输出
 * 80
 * </pre>
 * <p>
 * 这是一个 4x4 的距离矩阵，代表基站之间的距离：
 * <pre>
 *         基站1	  基站2	基站3  基站4
 * 基站1(0) 0      10    15    20
 * 基站2(1) 10	  0	    35	  25
 * 基站3(2) 15	  35	0	  30
 * 基站4(3) 20	  25	30	  0
 * </pre>
 * 3、说明
 * 路径 1 → 2 → 4 → 3 → 1，总距离为10 + 25 + 30 + 15 = 80。 <p>
 * 由于这题是非对称图（不一定满足 dist[i][j] == dist[j][i]），所以千万别随意对调索引。每一项都要根据行号表示出发点，列号表示目的地来读取。
 * <p>
 * 这是一个典型的旅行商问题(TSP)，我们需要找到从基站1出发，经过所有基站一次并返回基站1的最短路径。由于基站数量n<10，我们可以使用动态规划来解决。
 * <ul>位掩码表示访问状态：使用整数的二进制位来表示哪些基站已经被访问过。例如，对于3个基站：
 *     <li/>001 表示只访问过基站1
 *     <li/>011 表示访问过基站1和2
 *     <li/>111 表示访问过所有基站
 * </ul>
 * DP状态转移：对于每个状态mask和当前位置u，尝试转移到所有未访问的基站v，更新新的状态newMask和位置v的最短路径。
 * <p>
 * 最终路径计算：在所有基站都被访问后（mask=111...1），需要加上从最后一个基站返回基站1的距离。
 * <ul>动态规划状态定义
 *     <li/>dp[mask][u]：表示已经访问过的基站集合为mask，当前位于基站u时的最短路径长度
 *     <li/>mask是一个位掩码，第i位为1表示基站i已经被访问过
 *     <li/>初始状态：dp[1][0] = 0（从基站1出发，只访问过基站1）
 *     <li/>目标状态：访问所有基站后返回基站1的最短路径
 * </ul>
 * <ul>算法步骤
 *     <li/>初始化DP表，所有值设为无穷大，除了dp[1][0] = 0
 *     <li/>遍历所有可能的mask状态
 *     <li/>对于每个mask，遍历所有可能的当前基站u
 *     <li/>对于每个未被访问的基站v，更新dp[mask | (1<<v)][v]
 *     <li/>最后在所有基站都被访问后，加上返回基站1的距离
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-23 8:16
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BaseStationTSP {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入站点数n: ");
		int n = scanner.nextInt();
		scanner.nextLine();
		int[][] dist = new int[n][n];
		for (int i = 0; i < n; i++) {
			System.out.print("请输入站点之间距离: ");
			int[] arr = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
			dist[i] = arr;
		}
		System.out.println(findShortestRoute(n, dist));
	}

	/**
	 * 计算从基站1出发，经过所有基站一次并返回基站1的最短路径
	 *
	 * @param n         基站数量
	 * @param distances 基站之间的距离矩阵
	 * @return 最短路径长度
	 */
	public static int findShortestRoute(int n, int[][] distances) {
		/*
		 * 表示所有基站都被访问过的状态, 比如有3个基站, 那么:
		 * (1 << 3) - 1 = 0111
		 */
		int finalState = (1 << n) - 1;
		/*
		 * dp表，dp[mask][u]表示访问过mask中的基站，当前在u基站的最短路径
		 * dp[1<<1][0]表示访问过基站2, 当前在基站1
		 */
		int[][] dp = new int[1 << n][n]; //dp数组的第二维表示当前在哪个基站

		// 初始化DP表，所有值设为最大值
		for (int i = 0; i < 1 << n; i++) {
			Arrays.fill(dp[i], Integer.MAX_VALUE / 2);
		}

		// 初始状态：从基站1出发（索引0），只访问过基站1（mask=000...001）
		dp[1][0] = 0;

		// 遍历所有可能的mask状态
		for (int mask = 0; mask < 1 << n; mask++) {
			// 遍历所有可能的当前基站
			for (int u = 0; u < n; u++) {
				/*
				 * 如果当前mask不包含u基站，跳过
				 * 这段代码的作用是 检查当前状态 mask 是否包含基站 u。如果不包含，则跳过后续处理，因为动态规划要求路径必须连续，不能“跳跃”到未访问的基站。
				 */
				if ((mask & (1 << u)) == 0) {
					continue;
				}
				// 遍历所有可能的下一基站v
				for (int v = 0; v < n; v++) {
					/*
					 * 如果v已经被访问过，跳过
					 * 这段代码的作用是 检查下一个目标基站 v 是否已经被访问过。
					 * 如果 v 已经在当前状态 mask 中被访问过，则跳过该基站（因为TSP要求每个基站只能访问一次）。
					 */
					if ((mask & (1 << v)) != 0) {
						continue;
					}
					// 更新状态：从u到v
					int newMask = mask | (1 << v);
					dp[newMask][v] = Math.min(dp[newMask][v], dp[mask][u] + distances[u][v]);
				}
			}
		}

		// 最后需要返回基站1，遍历所有可能最后访问的基站，加上返回基站1的距离
		int minDistance = Integer.MAX_VALUE;
		for (int u = 0; u < n; u++) {
		    minDistance = Math.min(minDistance, dp[finalState][u] + distances[u][0]);
		}
		return minDistance;
	}
}
