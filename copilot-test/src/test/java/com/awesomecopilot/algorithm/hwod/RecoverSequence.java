package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 恢复数字序列 - 贪心算法
 * <p>
 * 一、题目描述
 * <p>
 * 对于一个连续正整数组成的序列，可以将其拼接成一个字符串，再将字符串里的部分字符打乱顺序。如序列8 9 10 11 12，拼接成的字符串为89101112，打乱部分字符后得到90811211, 原来的正整数序列10就被拆成了0和1。
 * <p>
 * 现在给定一个按如上规则得到的并打乱字符串的字符串，请将其还原成原来的连续正整数序列，并输出序列中最小的数字。
 * <p>
 * 二、输入描述
 * <p>
 * 输入一行，为打乱字符的字符串和正整数序列的长度，两者间用空格分隔。字符串长度不超过200，正整数不超过1000，保证输入可以还原成唯一序列。
 * <p>
 * 三、输出描述
 * <p>
 * 输出一个数字，为序列中最小的数字。
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 19801211 5
 *
 * 2、输出
 * 8
 *
 * 3、说明
 * 输入的打乱字符串是19801211，序列长度为5。
 * 将其还原为19, 8, 0, 12, 11，其中最小数字是8。
 * </pre>
 *
 * 我们要从一个被打乱的字符串中找出原始的连续数字序列，并输出其最小值。由于字符串中的字符顺序是打乱的，我们需要从这些字符中恢复出一个连续的正整数序列，
 * 这等价于从字符中挑选出 n 个整数，使得这些整数构成连续的正整数序列，且它们的拼接是输入字符串的一个排列。
 *
 * <p/>
 * Copyright: Copyright (c) 2025-06-28 10:01
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RecoverSequence {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入打乱字符的字符串和正整数序列的长度，两者间用空格分隔: ");
		String[] str = scanner.nextLine().split(" ");
		String shuffled = str[0]; // 打乱的字符串
		int length = Integer.parseInt(str[1]); // 连续正整数个数
	}

	/**
	 * 找出恢复后连续整数序列的最小起始值
	 * 举个完整例子:
	 * 输入： 19801211 5
	 * 表示：字符串是 "19801211"，要还原的连续正整数序列长度是 5。
	 * 执行逻辑：
	 * <ol>我们从 start = 1 开始枚举：
	 *     <li/>start = 1 → 拼接序列 1,2,3,4,5 → 得到字符串 "12345" → 不匹配
	 *     <li/>start = 2 → 2,3,4,5,6 → "23456" → 不匹配
	 *     <li/>......
	 *     <li/>start = 8 → 8,9,10,11,12 → 拼接为 "89101112" → 和 "19801211" 频次一样 → ✅找到
	 * </ol>
	 * 所以输出：8
	 * @param shuffled
	 * @param count
	 * @return int
	 */
	public static int findMinStart(String shuffled, int count) {
		// 将打乱的字符串中的字符转为频次映射，用于后续比较
		Map<Character, Integer> shuffledFreq = getCharFrequency(shuffled);

		/*
		 * 我们要恢复一个连续的正整数序列，假设它是从某个 start 开始的，长度为 count。 <p>
		 * 那么这个序列将是： start, start+1, start+2, ..., start+(count-1) <p>
		 * 最后一个数是 start + count - 1，它不能超过 1000（题目说最大数字不超过 1000），所以： <p>
		 * start + count - 1 <= 1000 ⇒ start <= 1000 - count + 1 <p>
		 * <p>
		 * 所以 start 的取值范围是：1 ≤ start ≤ 1000 - count + 1
		 */
		for (int start = 1; start <= 1000-count+1; start++) {
		    StringBuilder sb = new StringBuilder();
			/*
			 * 当 start = 8，count = 5，会拼接出： "8" + "9" + "10" + "11" + "12" = "89101112"
			 */
			for (int i = 0; i < count; i++) {
			    sb.append(start + 1);
			}

			// 判断拼接后字符串和原始打乱字符串是否为字符重排
			if (sameCharFrequency(sb.toString(), shuffledFreq)) {
				return start;
			}
		}

		return -1;
	}

	private static boolean sameCharFrequency(String s, Map<Character, Integer> targetFreq) {
		Map<Character, Integer> currentFreq = getCharFrequency(s);
		return currentFreq.equals(targetFreq);
	}

	/**
	 * 将字符串转为字符频率映射
	 * @param s
	 * @return Map<Character, Integer>
	 */
	private static Map<Character, Integer> getCharFrequency(String s) {
		Map<Character, Integer> map = new HashMap<>();
		for (char c : s.toCharArray()) {
			map.put(c, map.getOrDefault(c, 0) + 1);
		}
		return map;
	}
}
