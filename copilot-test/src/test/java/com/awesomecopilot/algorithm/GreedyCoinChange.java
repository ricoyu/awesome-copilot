package com.awesomecopilot.algorithm;

/**
 * 贪心算法经典场景: 货币兑换 <p>
 * 你是一个货币兑换员, 用户希望用尽可能少的硬币来支付某笔金额。给你一些硬币面额, 求如何用最少数量的硬币凑出目标金额。
 * <p>
 * 比如: 硬币面额为 [1, 5, 10], 目标金额是 18。
 * <p/>
 * Copyright: Copyright (c) 2025-05-24 8:21
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GreedyCoinChange {

	public static void main(String[] args) {
		int[] coins = { 10, 5, 1}; //从大到小排序(贪心策略)
		int amount  = 18;

		int count = 0;
		for (int coin : coins) {
			int use = amount / coin; //当前最多能用的硬币数量
			count += use;
			amount -= use * coin;

			System.out.println("使用面额为 " + coin +" 的硬币 " + use + " 次");
		}
		System.out.println("总共使用了 " + count + " 个硬币");
	}
}
