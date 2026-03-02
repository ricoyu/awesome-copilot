package com.awesomecopilot.algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 单词规律
 * <p>
 * 给定一种规律 pattern 和一个字符串 s ，判断 s 是否遵循相同的规律。
 * <p>
 * 这里的 遵循 指完全匹配，例如， pattern 里的每个字母和字符串 s 中的每个非空单词之间存在着双向连接的对应规律。具体来说：
 * <p>
 * pattern 中的每个字母都 恰好 映射到 s 中的一个唯一单词。
 * <p>
 * s 中的每个唯一单词都 恰好 映射到 pattern 中的一个字母。
 * <p>
 * 没有两个字母映射到同一个单词，也没有两个单词映射到同一个字母。
 *
 * <pre>
 * 示例1:
 *
 * 输入: pattern = "abba", s = "dog cat cat dog"
 * 输出: true
 * 示例 2:
 * </pre>
 *
 * <pre>
 * 输入:pattern = "abba", s = "dog cat cat fish"
 * 输出: false
 * 示例 3:
 * </pre>
 *
 * <pre>
 * 输入: pattern = "aaaa", s = "dog cat cat dog"
 * 输出: false
 * </pre>
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2026-03-02 9:39
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WordPatternValidator {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入pattern: ");
		String pattern = scanner.nextLine();
		System.out.print("请输入s: ");
		String s = scanner.nextLine();
		System.out.println(wordPattern(pattern, s));
		scanner.close();
	}
	
	private static boolean wordPattern(String pattern, String s) {
		// 1. 将字符串s按空格拆分为单词数组
		String[] words = s.split(" ");
		if (pattern.length() != words.length) {
			return false;
		}
		char[] chars = pattern.toCharArray();
		
		// 2. 定义双向映射的HashMap
		Map<Character, String> charToWord = new HashMap<>();
		Map<String, Character> wordToChar = new HashMap<>();
		
		// 3. 遍历每个字符和对应的单词，校验双向映射
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			String word = words[i];
			// 情况1：当前字符已存在映射，但映射的单词与当前单词不一致
			if (charToWord.containsKey(c)) {
				if (!charToWord.get(c).equals(word)) {
					return false;
				}
			} else {
				// 情况2：当前字符无映射，但当前单词已映射到其他字符（双向冲突）
				if (wordToChar.containsKey(word)) {
					return false;
				}
				// 情况3：双向都无映射，建立新的映射关系
				charToWord.put(c, word);
				wordToChar.put(word, c);
			}
		}
		
		// 4. 遍历完成无冲突，返回true
		return true;
	}
}