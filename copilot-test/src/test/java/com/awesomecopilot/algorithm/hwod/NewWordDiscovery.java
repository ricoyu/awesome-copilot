package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 发现新词的数量
 * <p>
 * 一、题目描述
 * <p>
 * 小华负责公司知识图谱产品，现在要通过新词挖掘完善知识图谱。
 * 新词挖掘: 给出一个待挖掘文本内容字符串Content和一个词的字符串word，找到content中所有word的新词。
 * 新词：使用词word的字符排列形成的字符串。
 * 请帮小华实现新词挖掘，返回发现的新词的数量。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入为待挖掘的文本内容content
 * 第二行输入为词word
 * <p>
 * 三、输出描述
 * <p>
 * 在content中找到的所有word的新词的数量
 *
 * <ul>核心思路
 *     <li/>新词定义：用词 word 的字符重排构成的新词（即“排列组合”变形）。
 *     <li/>
 *     <ul>使用滑动窗口法
 *          <li/>窗口长度为 word.length()
 *          <li/>判断每个窗口内的子串是否为 word 的排列。
 *     </ul>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-05-17 9:20
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class NewWordDiscovery {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入待挖掘的文本内容: ");
		String content = scanner.nextLine();
		System.out.print("请输入词word: ");
		String word = scanner.nextLine();
		int count = countNewWords(content, word);
		System.out.println(count);

	}

	public static int countNewWords(String content, String word) {
		if (content == null || word == null || content.length() < word.length()) {
			return 0;
		}

		int wordLen = word.length();
		int count = 0;

		// 构建 word 的字符频次映射
		Map<Character, Integer> wordFreq = buildFrequencyMap(word);

		Map<Character, Integer> windowFreq = new HashMap<>();
		// 滑动窗口中当前子串的字符频次
		for (int i = 0; i < wordLen; i++) {
			char c = content.charAt(i);
			windowFreq.put(c, windowFreq.getOrDefault(c, 0) + 1);
		}

		// 比较第一个窗口是否匹配
		if (windowFreq.equals(wordFreq)) {
			count++;
		}

		// 滑动窗口：从左到右滑动一位
		for (int i = wordLen; i < content.length(); i++) {
			char outChar = content.charAt(i - wordLen); // 移除窗口最左边的字符
			char inChar = content.charAt(i);// 加入窗口右边的新字符

			// 更新窗口频次
			windowFreq.put(outChar, windowFreq.get(outChar) - 1);
			if (windowFreq.get(outChar) <= 0) {
				windowFreq.remove(outChar); // 保持map精简
			}

			windowFreq.put(inChar, windowFreq.getOrDefault(inChar, 0) + 1);

			// 判断当前窗口是否匹配
			if (windowFreq.equals(wordFreq)) {
				count++;
			}
		}

		return count;
	}

	private static Map<Character, Integer> buildFrequencyMap(String s) {
		Map<Character, Integer> freq = new HashMap<>();
		for (char c : s.toCharArray()) {
			freq.put(c, freq.getOrDefault(c, 0) + 1);
		}
		return freq;
	}
}
