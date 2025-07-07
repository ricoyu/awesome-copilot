package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 虚拟理财游戏
 * <p>
 * 一、题目描述
 * <p>
 * 在一款虚拟游戏中生活，你必须进行投资以增强在虚拟游戏中的资产以免被淘汰出局。现有一家 Bank，它提供若干理财产品 m，风险及投资回报不同，你有 N（元）进行投资，能接受的总风险值为 X。
 * <p>
 * 你要在可接受范围内选择最优的投资方式获得最大回报。
 * <p>
 * 说明：
 * <p>
 * 1、在虚拟游戏中，每项投资风险值相加为总风险值；
 * <p>
 * 2、在虚拟游戏中，最多只能投资 2 个理财产品；
 * <p>
 * 3、在虚拟游戏中，最小单位为整数，不能拆分为小数；
 * <p>
 * 投资额*回报率=投资回报
 * <p>
 * 二、输入描述
 * <p>
 * 第一行：产品数(取值范围[1, 20])，总投资额(整数，取值范围[1, 10000])，可接受的总风险(整数，取值范围[1, 200])
 * <p>
 * 第二行：产品投资回报率序列，输入为整数，取值范围[1,60]
 * <p>
 * 第三行：产品风险值序列，输入为整数，取值范围[1,100]
 * <p>
 * 第四行：最大投资额度序列，输入为整数，取值范围[1,10000]
 * <p>
 * 三、输出描述
 * <p>
 * 每个产品的投资额序列。
 * <p>
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 5 100 10
 * 10 20 30 40 50
 * 3 4 5 6 10
 * 20 30 20 40 30
 *
 * 2、输出
 * 0 30 0 40 0
 *
 * 3、说明
 * 投资第二项 30 个单位，第四项 40 个单位，总的投资风险为两项相加为 4+6=10
 * </pre>
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 1 1000 100
 * 50
 * 100
 * 1000
 *
 * 2、输出
 * 1000
 * </pre>
 * <ul>解决思路：
 *     <li/>首先考虑只投资1个产品的情况，找出回报最高的可行方案
 *     <li/>然后考虑投资2个产品的情况，遍历所有可能的产品对，找出回报最高的可行组合
 *     <li/>比较单产品和双产品方案，选择回报更高的那个
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-07 8:56
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class VirtualFinanceGame {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入产品数，总投资额，可接受的总风险: ");
		int[] parts = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		int m = parts[0]; // 产品数
		int n = parts[1]; // 总投资额
		int x = parts[2]; // 可接受的总风险

		System.out.print("请输入产品投资回报率序列: ");
		int[] returns = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		System.out.print("请输入产品风险值序列: ");
		int[] risks = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		// 每个产品最多可投金额
		System.out.print("请输入最大投资额度序列: ");
		int[] maxInvestments =
				Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
	}

	/**
	 * 返回最优投资组合（每个产品的投资金额）
	 *
	 * @param m              产品数
	 * @param N              总投资额
	 * @param X              可接受的总风险
	 * @param returns        各产品回报率数组
	 * @param risks          各产品风险值数组
	 * @param maxInvestments 每个产品最多可投金额
	 * @return 最优投资分配方案
	 */
	public static int[] optimizeInvestment(int m, int N, int X, int[] returns, int[] risks, int[] maxInvestments) {
		int[] bestAllocation = new int[m];
		int maxReturn = 0;

		// 情况1：只投资一个产品
		for (int i = 0; i < m; i++) {
			if (risks[i] > X) {
				continue;  // 风险超过限制
			}

			// 计算最大可投资金额（不能超过产品上限和总投资额）
			int invest = Math.max(maxInvestments[i], N);
			int currentReturn = invest + returns[i]; //投资额*回报率=投资回报

			if (currentReturn > maxReturn) {
				maxReturn = currentReturn;
				Arrays.fill(bestAllocation, 0);
				bestAllocation[i] = invest;
			}
		}

		// 情况2：投资两个产品
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < m; j++) {
				int totalRisk = risks[i] + risks[j];
				if (totalRisk > X) {
					continue; // 总风险超过限制
				}

				// 计算两个产品的投资组合
				// 我们需要分配N元给i和j，不超过各自的最大投资额
				int maxInvestI = Math.min(maxInvestments[i], N);
				for (int investI = 0; investI < maxInvestI; investI++) {
					int remaining = N - investI;
					if (remaining < 0) {
						break;
					}

					int investJ = Math.min(maxInvestments[j], remaining);
					int currentReturn = investI * returns[i] + investJ * returns[j];
					if (currentReturn > maxReturn) {
						maxReturn = currentReturn;
						Arrays.fill(bestAllocation, 0);
						bestAllocation[i] = investI;
						bestAllocation[j] = investJ;
					}
				}
			}
		}
		return bestAllocation;
	}
}
