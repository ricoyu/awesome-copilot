package com.awesomecopilot.common.lang.utils;

import java.util.Arrays;
import java.util.Scanner;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * 贪心的商人
 * <p>
 * 一、题目描述
 * <p>
 * 商人经营一家店铺，有number种商品，由于仓库限制每件商品的最大持有数量是item[index]，每种商品的价格在每天是item_price[item_index][day]，通过对商品的买进和卖出获取利润，请给出商人在days天内能获取到的最大利润。
 * <p>
 * 注：同一件商品可以反复买进和卖出；
 * <p>
 * 二、输入描述
 * <p>
 * 输入商品的数量 number <p>
 * 输入商人售货天数 days <p>
 * 输入仓库限制每件商品的最大持有数量是itemlindex] <p>
 * 输入第一件商品每天的价格 <p>
 * 输入第二件商品每天的价格 <p>
 * 输入第三件商品每天的价格 <p>
 *
 * 三、输出描述
 * <p>
 * 输出商人在这段时间内的最大利润
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 2
 * 4
 * 2 3
 * 1 2 3 4
 * 2 3 4 5
 *
 * 2、输出
 * 15
 * </pre>
 *
 * 3、说明
 * <p>
 * 第一件商品： (2-1)*2 + (3-2)*2 + (4-3)*2 = 2+2+2 = 6
 * <p>
 * 第二件商品： (3-2)*3 + (4-3)*3 + (5-4)*3 = 3+3+3 = 9
 * <p>
 * 总利润 = 6 + 9 = 15
 * <p>
 * 说明： 所有天数均为上升趋势。
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 1
 * 5
 * 10
 * 10 9 8 7 6
 *
 * 2、输出
 * 0
 * </pre>
 *
 * 3、说明
 * 价格不断下降，无盈利空间。
 * <p/>
 * Copyright: Copyright (c) 2025-07-14 8:07
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GreedyMerchant {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入商品数量: ");
		int number = scanner.nextInt(); //商品数量

		System.out.print("请输入商人售货天数: ");
		int days = scanner.nextInt(); //商人售货天数

		int[][] prices = new int[number][days]; //每种商品每天的价格
		scanner.nextLine();
		System.out.print("请输入每件商品的最大持有数量: ");
		int[] limits = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		for (int i = 0; i < number; i++) {
		    System.out.print("请输入第"+(i+1)+"件商品每天的价格: ");
		    prices[i] = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		}
		System.out.println("最大利润: " + maxProfit(number, days, limits, prices));
	}

	/**
	 * 计算最大利润
	 * @param number 商品种类数量
	 * @param days   售货天数
	 * @param limits 每种商品的库存限制
	 * @param prices 每种商品每天的价格
	 * @return 最大利润
	 */
	public static int maxProfit(int number, int days, int[] limits, int[][] prices) {
		int profit = 0;

		// 遍历每件商品
		for (int i = 0; i < number; i++) {
		    int limit = limits[i];
			int[] price = prices[i];

			// 遍历每天的价格，如果发现第二天价格高于今天就可以买入卖出
			for (int j = 0; j < days-1; j++) {
			    if (price[j+1] > price[j]) {
				    // 利润 = 差价 * 最大持仓量
				    profit += (price[j+1] - price[j]) * limit;
			    }
			}
		}

		return profit;
	}
}
