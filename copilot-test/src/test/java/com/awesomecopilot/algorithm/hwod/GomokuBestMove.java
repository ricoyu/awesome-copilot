package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 五子棋迷 - 贪心算法
 * <p>
 * 一、题目描述
 * <p>
 * 张兵和王武是五子棋迷，工作之余经常切磋棋艺。这不，这会儿又下起来了。走了一会儿，轮张兵了，对着一条线思考起来了，这条线上的棋子分布如下：
 * <p>
 * 用数组表示：-101110101-1
 * <p>
 * 棋子分布说明：
 * <p>
 * -1代表白子，0代表空位，1代表黑子
 * 数组长度L，满足 1 < L < 40，且L为奇数
 * <p>
 * 你得帮他写一个程序，算出最有利的出子位置。最有利定义：
 * <p>
 * 找到一个空位(0)，用棋子(1/-1)填充该位置，可以使得当前子的最大连续长度变大； <p>
 * 如果存在多个位置，返回最靠近中间的较小的那个坐标； <p>
 * 如果不存在可行位置，直接返回-1； <p>
 * 连续长度不能超过5个(五子棋约束)； <p>
 * <p>
 * 二、输入描述
 * <p>
 * 第一行：当前出子颜色
 * <p>
 * 第二行：当前的棋局状态
 * <p>
 * 三、输出描述
 * <p>
 * 1个整数，表示出子位置的数组下标
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 1
 * -1 0 1 1 1 0 1 0 1 -1 1
 *
 * 2、输出
 * 5
 *
 * 3、说明
 * 当前为黑子（1），放置在下标为5的位置，黑子的最大连续长度，可以由3到5
 * </pre>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * -1
 * -1 0 1 1 1 0 1 0 1 -1 1
 *
 * 2、输出
 * 1
 *
 * 3、说明
 * 当前为白子，唯一可以放置的位置下标为1，白子的最大长度，由1变为2
 * </pre>
 * <ul>核心解题思路
 *     <li/>枚举每一个空位 0
 *     <li/>模拟落子（放当前颜色），然后扫描这条线，计算最长连续当前颜色的棋子数；
 *     <li/>记录最大长度增长的坐标（长度不能超过 5）；
 *     <li/>如果多个位置长度相同，选择最靠近数组中间且下标较小的。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-27 8:52
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GomokuBestMove {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入当前出子颜色: ");
		int color = scanner.nextInt();
		System.out.print("输入当前棋盘状态: ");
		scanner.nextLine();
		int[] board = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.println(bestMove(color, board));
	}

	public static int bestMove(int color, int[] board) {
		int maxLen = 0; // 当前能达到的最长连续长度
		int bestIndex = -1; // 最优下标位置
		int mid = board.length / 2; // 中间位置，用于比较距离

		// 遍历所有可能落子的位置
		for (int i = 0; i < board.length; i++) {
			if (board[i] != 0) {
				continue;// 不是空位则跳过
			}
			board[i] = color;// 假设当前颜色落子在此

			// 计算当前落子后，最大的连续长度
			int curMaxLen = getMaxLen(board, color);

			// 满足连续长度增加并且不超过5
			if (curMaxLen > maxLen && curMaxLen <= 5) {
				maxLen = curMaxLen;
				bestIndex = i;
			} else if (curMaxLen == maxLen && curMaxLen <= 5 && bestIndex != -1) {
				// 如果长度相同，则选择更靠近中间且下标小的
				int prevDist = Math.abs(bestIndex - mid);
				int newDist = Math.abs(i - mid);
				if (newDist < prevDist || (newDist == prevDist && i < bestIndex)) {
					bestIndex = i;
				}
			}

			board[i] = 0; // 回溯
		}

		return bestIndex;
	}

	/**
	 * 取某颜色在当前棋盘上的最大连续长度
	 *
	 * @param board
	 * @param color
	 * @return
	 */
	public static int getMaxLen(int[] board, int color) {
		int max = 0;
		int count = 0;

		for (int num : board) {
			if (num == color) {
				count++;
				max = Math.max(max, count);
			} else {
				count = 0; // 连续被打断
			}
		}

		return max;
	}
}
