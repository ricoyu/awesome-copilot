package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 一、题目描述
 * <p>
 * 一个有N个选手参加的比赛，选手编号为1~N（3 <= N <= 100），有M（3 <= M <= 10）个评委对选手进行打分。
 * <p>
 * 打分规则为每个评委对选手打分，最高为10分，最低为1分。
 * <p>
 * 请计算得分最多的3位选手的编号。
 * <p>
 * 如果得分相同，得分高分值最多的选手排名靠前。
 * <p>
 * （10分数量相同，则比较9分的数量，以此类推，用例中不会出现多个选手得分完全相同的情况。）
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为半角逗号分割的两个正整数，第一个数字表示M（3 <= M <= 10）个评委，第二个数字表示N（3 <= N <= 100）个选手。
 * <p>
 * 第2到M+1行为半角逗号分割的整数数组，表示评委对每个选手的打分，0号下标数字表示1号选手分数，1号下标数字表示2号选手分数，依次类推。
 * <p>
 * 三、输出描述 <p>
 * 选手前3名的编号。
 * <p>
 * 注意：
 * <p>
 * 若输入为异常，输出-1，如M、N、打分不在生范围内。
 * <p>
 * 测试用例1：
 * <pre>
 * 1、输入
 * 4,5
 * 10,6,9,7,6
 * 9,10,6,7,5
 * 8,10,6,5,10
 * 9,10,8,4,9
 *
 * 2、输出
 * 2,1,5
 * </pre>
 *
 * <ul>代码逻辑详解
 *     <li/>首先按总分降序排序。
 *     <li/>如果总分相同，则从高分到低分（10分到1分）依次比较数量。例如：
 *     <li/>选手A总分36，分数统计：[..., 1, 2, 1]（10分1次，9分2次，8分1次）。
 *     <li/>选手B总分36，分数统计：[..., 2, 1, 1]（10分2次，9分1次，8分1次）。
 *     <li/>比较10分数量：选手B的10分更多，因此选手B排名更高。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-09 8:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CompetitionRanking {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		// 读取第一行输入，解析M和N
		System.out.print("输入M和N:");
		String[] input = scanner.nextLine().trim().split(",");
		int m = Integer.parseInt(input[0]);
		int n = Integer.parseInt(input[1]);

		// 读取评委的打分
		int[][] scores = new int[m][n];
		for (int i = 0; i < m; i++) {
			System.out.print("输入评委" + (i + 1) + "的打分: ");
			String[] parts = scanner.nextLine().trim().split(",");
			for (int j = 0; j < n; j++) {
				scores[i][j] = Integer.parseInt(parts[j]);
			}
		}

		// 处理并输出结果
		String result = getTop3Players(m, n, scores);
		System.out.println(result);
	}

	private static String getTop3Players(int m, int n, int[][] scores) {
		// 为每个选手创建分数统计对象
		List<PlayerScore> playerScores = new ArrayList<>();
		for (int player = 0; player < n; player++) {
		    int total = 0;
			int[] scoreCounts = new int[11]; //索引0不用，1~10分别表示1~10分的数量
			for (int judge = 0; judge < m; judge++) {
				int score = scores[judge][player];
				total+=score;
				scoreCounts[score]++;
			}
			playerScores.add(new PlayerScore(player, total, scoreCounts));
		}

		// 自定义排序
		Collections.sort(playerScores, (p1, p2) -> {
			if (p1.total == p2.total) {
				return p2.total - p1.total;
			}else {
				// 比较高分数量，从10分到1分依次比较
				for (int score = 10; score >= 1; score--) {
					if (p1.scoreCounts[score] != p2.scoreCounts[score]) {
						return p2.scoreCounts[score] - p1.scoreCounts[score];
					}
				}
				return 0; // 理论上不会走到这里，因为题目保证没有完全相同的选手
			}
		});

		// 提取前三名的编号
		return playerScores.stream()
				.limit(3)
				.map(ps -> ps.playerId + "")
				.collect(Collectors.joining(","));
	}

	static class PlayerScore {
		int playerId; // 选手编号
		int total; // 总分
		int[] scoreCounts; // 各分数（1~10）出现的次数

		public PlayerScore(int playerId, int total, int[] scoreCounts) {
			this.playerId = playerId;
			this.total = total;
			this.scoreCounts = scoreCounts;
		}
	}

}
