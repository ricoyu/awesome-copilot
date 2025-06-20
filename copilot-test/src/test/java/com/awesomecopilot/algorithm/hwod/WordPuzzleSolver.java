package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * 猜字谜 - 双指针
 * <p>
 * 一、题目描述 <p>
 * 小王设计了一个简单的猜字谜游戏，游戏的迷面是一人错误的单词，比如nesw，玩家需要猜出谈底库中正确的单词。猜中的要求如· <p>
 * 对于某个谜面和谜底单词，满足下面任一条件都表示猜中：
 * <p>
 * 变换顺序以后一样的，比如通过变换w和e的顺序，“nwes”跟“news”是可以完全对应的； <p>
 * 字母去重以后是一样的，比如“woood”和“wood”是一样的，它们去重后都是“wod”请你写一个程序帮忙在谜底库中找到正确的谜底。迷面是多个单词，都需要找到对应的谜底，如果找不到的话，返"not found"。
 * <p>
 * 二、输入描述 <p>
 * 1、谜面单词列表，以",“分隔 <p>
 * 2、谜底库单词列表，以”,"分隔 <p>
 *
 * 三、输出描述 <p>
 * 匹配到的正确单词列表，以","分隔；
 * <p>
 * 如果找不到，返回"not found"。
 * <p>
 * 谜面为 nwes，谜底为 news <br/>
 * 排序后：nwes → ensw，news → ensw → 匹配 ✅
 * <p>
 * 谜面为 woood，谜底为 wood <br/>
 * 去重后：woood → wod，wood → wod → 匹配 ✅
 * <p>
 * 谜面为 abc，谜底为 bca <br/>
 * 排序后：abc → abc，bca → abc → 匹配 ✅
 * 
 * <pre>
 * 测试用例 1：顺序不同，应该匹配
 * 谜面： nwes
 * 谜底库： news,wood,cat
 * 期望输出： news
 * </pre>
 * 
 * <pre>
 * 测试用例 2：字母重复但字符集合相同，应该匹配
 * 谜面： woood
 * 谜底库： news,wood,cat
 * 期望输出： wood
 * </pre>
 * 
 * <pre>
 * 测试用例 3：完全一样的单词
 * 谜面： cat
 * 谜底库： news,wood,cat
 * 期望输出： cat
 * </pre>
 * 
 * <pre>
 * 测试用例 4：顺序和去重都不一致，找不到
 * 谜面： xyz
 * 谜底库： news,wood,cat
 * 期望输出： not found
 * </pre>
 *
 * <pre>
 * 测试用例 5：多个谜面混合
 * 谜面： nwes,woood,cat,xyz
 * 谜底库： news,wood,cat
 * 期望输出： news,wood,cat,not found
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-18 9:53
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WordPuzzleSolver {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入谜面单词列表: ");
		String puzzles = scanner.nextLine().trim();
		System.out.print("请输入谜底库单词列表: ");
		String words = scanner.nextLine().trim();
		System.out.println(solve(puzzles, words));
		scanner.close();
	}

	/**
	 * 核心方法：给定谜面单词列表和谜底库，返回匹配到的正确谜底单词列表
	 * @param puzzleWordsStr
	 * @param answerWordsStr
	 * @return
	 */
	public static String solve(String puzzleWordsStr, String answerWordsStr) {
		String[] puzzles = puzzleWordsStr.split(",");
		String[] answers = answerWordsStr.split(",");

		// 创建两个Map，一个用来按字符排序比较，一个用来按字符集合（去重后）比较
		Map<String,String> sortedMap = new HashMap<>();
		Map<String, String> setMap = new HashMap<>();

		for (String answer : answers) {
			// key1: 排序后的字符串（用于判断是否只是顺序不同）
			String key1 = sortString(answer);
			sortedMap.put(key1, answer);
			//key2: 去重并排序后的字符串（用于判断字符集合是否相同）
			String key2 = sortAndDup(answer);
			setMap.put(key2, answer);
		}

		List<String> results = new ArrayList<>();
		for (String puzzle : puzzles) {
			String key1 = sortString(puzzle);
			if (sortedMap.containsKey(key1)) {
				results.add(sortedMap.get(key1));
				continue;
			}
			String key2 = sortAndDup(puzzle);
			if (setMap.containsKey(key2)) {
				results.add(setMap.get(key2));
			} else {
				results.add("not found");
			}
		}
		return String.join(",", results);
	}

	/**
	 * 将字符串按字母排序
	 * 例如: "nwes" -> "ensw"
	 * @param word
	 * @return
	 */
	public static String sortString(String word) {
		char[] chars = word.toCharArray();
		Arrays.sort(chars);
		return new String(chars);
	}

	public static String sortAndDup(String word) {
		Set<Character> set = new HashSet<Character>();
		for(Character c : word.toCharArray()) {
			set.add(c);
		}

		// 转成字符数组并排序
		char[] chars = new char[set.size()];
		int idx = 0;
		for (char c : set) {
			chars[idx++] = c;
		}
		Arrays.sort(chars);
		return new String(chars);
	}
}
