package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 计算最多能观看几场演出
 * <p>
 * 一、题目描述
 * <p>
 * 为了庆祝中国共产党成立100周年，某公园将举行多场文艺表演，很多演出都是同时进行，一个人只能同时观看一场演出，且不能迟到早退，由于演出分布在不同的演出场地，所以连续观看的演出最少有15分钟的时间间隔。
 * <p>
 * 小明是一个狂热的文艺迷，想观看尽可能多的演出，现给出演出时间表，请帮小明计算他最多能观看几场演出。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为一个数N,表示演出场数，1 <= N<=1000,接下来N行，每行有被空格分割的两个整数；<br/>
 * 第一个整数T表示演出的开始时间，第二个整数L表示演出的持续时间；<br/>
 *
 * T和L的单位为分钟，0 <=T <=1440, 0 <= L <= 100
 * <p>
 * 三、输出描述
 * <p>
 * 输出最多能观看的演出场数。
 *
 * <pre>
 * 示例 1
 *
 * 1、输入
 * 3
 * 100 120
 * 240 150
 * 400 100
 *
 * 2、输出
 * 2
 * </pre>
 * <ul>核心思路
 *     <li/>将所有演出按结束时间排序（结束时间 = 开始时间 + 持续时间）
 *     <li/>遍历每个演出：如果当前演出的开始时间 ≥ 上一个观看演出结束时间 + 15分钟，就选择这个演出
 *     <li/>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-05-29 12:17
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MaxShowsScheduler {

	// 演出对象，包含开始时间和结束时间（用于排序）
	static class Show {
		int start;
		int end;

		Show(int start, int duration) {
			this.start = start;
			this.end = start + duration;
		}
	}

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入演出场数: ");
		int N = scanner.nextInt();
		scanner.nextLine();
		List<Show> shows = new ArrayList<Show>();
		for (int i = 0; i < N; i++) {
		    System.out.print("请输入演出开始时间和持续时间: ");
			String[] parts = scanner.nextLine().trim().split(" ");
			shows.add(new Show(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		}
		scanner.close();
		// 将演出按结束时间从小到大排序（贪心策略的关键）
		shows.sort((show1, show2) -> {
			return Integer.compare(show1.end, show2.end); // 按结束时间从小到大排序
		});

		int count = 0; //最多能观看的演出数
		int lastShowEnd = -1; //上一个观看演出结束时间，初始为-15，便于第一场判断

		for (Show show : shows) {
			if (show.start >=lastShowEnd+15) {
				count++;
				lastShowEnd = show.end;
			}
		}

		System.out.println(count);
	}
}
