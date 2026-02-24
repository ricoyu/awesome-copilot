package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 有效的字母异位词
 * <p>
 * 给定两个字符串 s 和 t ，编写一个函数来判断 t 是否是 s 的 字母异位词。
 * <p>
 * 字母异位词是通过重新排列不同单词或短语的字母而形成的单词或短语，并使用所有原字母一次。
 * <p>
 *
 * <pre>
 * 示例 1:
 *
 * 输入: s = "anagram", t = "nagaram"
 * 输出: true
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: s = "rat", t = "car"
 * 输出: false
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>长度校验：如果两个字符串长度不同，直接返回 false（字母数量不同，不可能是异位词）。
 *     <li/>字符计数：利用 ASCII 码特性，创建一个长度为 26 的数组（对应 26 个小写英文字母），遍历第一个字符串时对字符出现次数做加法，遍历第二个字符串时做减法。
 *     <li/>遍历计数数组，若所有元素都为 0，说明字符出现次数完全一致，返回 true；否则返回 false。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-02-11 8:23
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class AnagramChecker {
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s: ");
		String s = scanner.nextLine();
		System.out.print("请输入字符串t: ");
		String t = scanner.nextLine();
		System.out.println(isAnagram(s, t));
		scanner.close();
	}
	
	private static boolean isAnagram(String s, String t) {
		if (s.length() != t.length()) {
			return false;
		}
		
		int[] counterArr = new int[26];
		s = s.toLowerCase();
		t = t.toLowerCase();
		for (int i = 0; i < s.length(); i++) {
			counterArr[s.charAt(i) - 'a']++;
			counterArr[t.charAt(i) - 'a']--;
		}
		
		for (int i = 0; i < counterArr.length; i++) {
			if (counterArr[i] != 0) {
				return false;
			}
		}
		return true;
	}
}