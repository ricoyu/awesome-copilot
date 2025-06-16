package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * 围棋的气
 * <p>
 * 题目描述
 * <p>
 * 围棋棋盘由纵横各19条线垂直相交组成, 棋盘上一共19x19=361个交点, 对弈双方一方执白棋, 一方执黑棋, 落子时只能将棋子置于交点上。
 * <p>
 * “气”是围棋中很重要的一个概念, 某个棋子有几口气, 是指其上下左右方向四个相邻的交叉点中, 有几个交叉点没有棋子, 由此可知:
 * <p>
 * 1. 在棋盘的边缘上的棋子最多有3口气（黑1）, 在棋盘角点的棋子最多有2口气（黑2）, 其它情况最多有4口气（白1）
 * <p>
 * 2. 所有同色棋子的气之和叫作该色棋子的气, 需要注意的是, 同色棋子重合的气点, 对于该颜色棋子来说, 只能计算一次气, 比如下图中, 黑棋一共4口气, 而不是5口气, 因为黑1和黑2中间红色三角标出的气是两个黑棋共有的,
 * 对于黑棋整体来说只能算一个气。
 * <p>
 * 3. 本题目只计算气, 对于眼也按气计算, 如果您不清楚“眼”的概念, 可忽略, 按照前面描述的规则计算即可现在, 请根据输入的黑棋和白棋的坐标位置, 计算黑棋和白起一共各有多少气?
 * <p>
 * 输入描述
 * <p>
 * 输入包括两行数据, 如:
 * <p>
 * 0 5 8 9 9 10
 * <p>
 * 5 0 9 9 9 8
 * <p>
 * 1、每行数据以空格分隔，数据个数是2的整数倍，每两个数是一组,代表棋子在棋盘上的坐标；
 * <p>
 * 2、坐标的原点在棋盘左上角点，第一个值是行号，范围从0到18;第二个值是列号，范围从0到18；
 * <p>
 * 3、举例说明: 第一行数据表示三个坐标 (0，5)、 (8，9)、 (9,10)；
 * <p>
 * 4、第一行表示黑棋的坐标，第二行表示白棋的坐标。
 * <p>
 * 5、题目保证输入两行数据，无空行且每行按前文要求是偶数个，每个坐标不会超出棋盘范围。
 * <p>
 * 输出描述
 * <p>
 * 8 7
 * <p>
 * 两个数字以空格分隔，第一个数代表黑棋的气数，第二个数代表白棋的气数。
 *
 * <pre>
 * 示例1:
 *
 *
 * 输入：
 * 0 5 8 9 9 10
 * 5 0 9 9 9 8
 *
 * 输出：
 * 8 7
 * </pre>
 * <p>
 * 请用Java实现并给出核心的解题思路, 不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-06-12 8:41
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GoGameLibertyCalculator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入黑棋坐标: ");
		String[] blankParts = scanner.nextLine().trim().split(" ");
		System.out.print("请输入白棋坐标: ");
		String[] whiteParts = scanner.nextLine().trim().split(" ");

		int[][] board = new int[19][19];  // 0=空, 1=黑, 2=白

		// 记录黑棋坐标
		List<int[]> blackStones = new ArrayList<>();
		for (int i = 0; i < blankParts.length; i += 2) {
			int x = Integer.parseInt(blankParts[i]);
			int y = Integer.parseInt(blankParts[i + 1]);
			board[x][y] = 1;
			blackStones.add(new int[]{x, y});
		}

		// 记录白棋坐标
		List<int[]> whiteStones = new ArrayList<>();
		for (int i = 0; i < whiteParts.length; i += 2) {
			int x = Integer.parseInt(whiteParts[i]);
			int y = Integer.parseInt(whiteParts[i + 1]);
			board[x][y] = 2;
			whiteStones.add(new int[]{x, y});
		}

		// 计算黑棋和白棋的气
		int blackQi = countQi(board, blackStones, 1);
		int whiteQi = countQi(board, whiteStones, 2);

		System.out.println(blackQi + " " + whiteQi);
	}

	/**
	 * 计算给定棋子颜色的所有气的数量
	 *
	 * @param board  棋盘状态
	 * @param stones 当前颜色的所有棋子坐标
	 * @param color  当前棋子颜色（1=黑，2=白）
	 * @return 该颜色所有棋子的气总数（去重后的）
	 */
	private static int countQi(int[][] board, List<int[]> stones, int color) {
		Set<String> qies = new HashSet<>();
		int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; //上下左右方向

		for (int[] stone : stones) {
			int x = stone[0];
			int y = stone[1];
			for (int[] direction : directions) {
				int nx = x + direction[0];
				int ny = y + direction[1];

				// 如果在棋盘范围内且该位置为空，则是“气”
				if (nx >= 0 && nx < 19 && ny >= 0 && ny < 19 && board[nx][ny] == 0) {
					qies.add(nx + " " + ny);// 用字符串表示气点，自动去重
				}
			}
		}

		return qies.size();
	}
}
