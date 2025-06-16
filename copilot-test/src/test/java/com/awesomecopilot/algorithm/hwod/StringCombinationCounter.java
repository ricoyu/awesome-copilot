package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 构成指定长度字符串的个数
 * <p>
 * 一、题目描述 <p>
 * 给定 M 个字符( a-z ) ，从中取出任意字符(每个字符只能用一次)拼接成长度为 N 的字符串，要求相同的字符不能相邻。
 * <p>
 * 计算出给定的字符列表能拼接出多少种满足条件的字符串，输入非法或者无法拼接出满足条件的字符串则返回 0 。
 * <p>
 * 二、输入描述 <p>
 * 给定长度为 M 的字符列表和结果字符串的长度 N ，中间使用空格(" ")拼接。
 * <p>
 * 0 < M < 30 <p>
 * 0 < N ≤ 5
 * <p>
 * 三、输出描述 <p>
 * 输出满足条件的字符串个数。
 * <p>
 * 1、输入 <p>
 * dde 2
 * <p>
 * 2、输出 <p>
 * 2
 * <p>
 * 3、说明 <p>
 * 给定的字符为 dde ，果字符串长度为 2 ，可以拼接成 de、ed， 共 2 种。 <p>
 * <ul>核心解题思路
 *     <li/>从字符列表中选 N 个字符，位置唯一（即从字符数组中选择不同索引的 N 个字符）
 *     <li/>回溯生成所有长度为 N 的排列：每个字符最多只能使用一次，且相邻字符不能相同。
 *     <li/>过滤掉含有相邻相同字符的排列
 *     <li/>计数
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-07 9:23
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringCombinationCounter {

	private static int count = 0;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入字符串和结果字符串长度: ");
		String input = scanner.nextLine().trim();
		String[] parts = input.split(" ");
		scanner.close();

		String s = parts[0];
		int n = Integer.parseInt(parts[1]);
		System.out.println(countValidStrings(s, n));
	}

	public static int countValidStrings(String s, int n) {
		if (s == null || s.trim().isEmpty()) {
			return 0;
		}

		char[] chars = s.toCharArray();
		boolean[] visited = new boolean[chars.length];
		backtrack(chars, visited, n, new ArrayList<>());
		return count;
	}

	/**
	 * 回溯生成所有长度为n的排列组合（位置不同，字符可重复）
	 *
	 * @param chars   源字符数组
	 * @param visited 记录源字符数组中每个字符是否被使用过
	 * @param n       结果字符串的长度
	 * @param path    存储结果字符串
	 */
	private static void backtrack(char[] chars, boolean[] visited, int n, List<Character> path) {
		if (path.size() == n) {
			// 检查是否有相邻字符相同
			for (int i = 1; i < path.size(); i++) {
				if (path.get(i).equals(path.get(i - 1))) {
					return; //不合法
				}
			}
			count++;
			return;
		}
		
		for (int i = 0; i < chars.length; i++) {
		    if (visited[i]) {
				continue;
		    }
			visited[i] = true;
			path.add(chars[i]);

			backtrack(chars, visited, n, path);
			// 回溯
			path.remove(path.size() - 1);
			visited[i] = false;
		}
	}
}
