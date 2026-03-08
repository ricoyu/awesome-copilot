package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * Nim 游戏
 * <p>
 * 你和你的朋友，两个人一起玩 Nim 游戏：
 *
 * <p>
 * 桌子上有一堆石头。 <br/>
 * 你们轮流进行自己的回合， 你作为先手 。 <br/>
 * 每一回合，轮到的人拿掉 1 - 3 块石头。 <br/>
 * 拿掉最后一块石头的人就是获胜者。 <br/>
 * 假设你们每一步都是最优解。请编写一个函数，来判断你是否可以在给定石头数量为 n 的情况下赢得游戏。如果可以赢，返回 true；否则，返回 false 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 4
 * 输出：false
 * 解释：以下是可能的结果:
 * 1. 移除1颗石头。你的朋友移走了3块石头，包括最后一块。你的朋友赢了。
 * 2. 移除2个石子。你的朋友移走2块石头，包括最后一块。你的朋友赢了。
 * 3.你移走3颗石子。你的朋友移走了最后一块石头。你的朋友赢了。
 * 在所有结果中，你的朋友是赢家。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 1
 * 输出：true
 * 示例 3：
 *
 * 输入：n = 2
 * 输出：true
 * </pre>
 *
 * <ul>解题思路
 *     <li/>当石头数 n % 4 == 0 时，无论你拿 1/2/3 块，对手都可以拿 3/2/1 块，使得每轮总共拿走 4 块，最终对手会拿走最后一块，你必败。
 *     <li/>当石头数 n % 4 != 0 时，你可以先拿走 n % 4 块，让剩余石头数变成 4 的倍数，此时对手进入必败态，你必胜。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-04 9:00
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class NimGame {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.println(canWinNim(n));
		scanner.close();
	}
	
	/**
	 * 判断先手是否能赢得Nim游戏
	 * @param n
	 * @return
	 */
	private static boolean canWinNim(int n) {
		// 核心逻辑：当n不是4的倍数时，先手必胜；是4的倍数时必败
		// 原理：每轮双方最优策略下，总能凑出拿走4块石头
		return n % 4 != 0;
	}
}