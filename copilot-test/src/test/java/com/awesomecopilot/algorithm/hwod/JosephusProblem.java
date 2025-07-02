package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 报数问题 - 约瑟夫环
 * <p>
 * 一、题目描述 <p>
 * 有n个人围成一圈，顺序排号为1~n。
 * <p>
 * 从第一个人开始报数（从1到3报数），凡报到3的人退出圈子，问最后留下的是原来第几号的那位。
 * <p>
 * 二、输入描述 <p>
 * 输入人数n（n < 1000）
 * <p>
 * 三、输出描述 <p>
 * 输出最后留下的是原来第几号
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * 2
 *
 * 2、输出
 * 2
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 5
 *
 * 2、输出
 * 4
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-26 8:41
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class JosephusProblem {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入人数: ");
		int n = scanner.nextInt();
		scanner.close();
		System.out.println(getLastRemaining(n));
	}

	public static int getLastRemaining(int n) {
		List<Integer> peoples = new ArrayList<Integer>();
		// 初始化编号为 1 到 n 的人
		for (int i = 1; i <= n; i++) {
		    peoples.add(i);
		}

		int index = 0; // 当前报数起点索引
		int count = 0; // 报数计数器

		// 不断移除报到 3 的人，直到只剩下 1 人
		while (peoples.size() > 1) {
			count++;
			// 报数到3的人出圈
			if (count == 3) {
				peoples.remove(index);
				count = 0; // 报数重新从1开始
				// 注意：删除后列表缩短了，index 不需要前进
			} else {
				index++; // 没有人被删除，报数从下一个人开始
			}

			if (index >= peoples.size()) {
				index = 0;
			}
		}

		// 返回最后剩下的那个人的编号
		return peoples.get(0);
	}
}
