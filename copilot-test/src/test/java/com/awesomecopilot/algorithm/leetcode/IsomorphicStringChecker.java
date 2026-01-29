package com.awesomecopilot.algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 同构字符串
 * <p>
 * 给定两个字符串 s 和 t ，判断它们是否是同构的。
 * <p>
 * 如果 s 中的字符可以按某种映射关系替换得到 t ，那么这两个字符串是同构的。
 * <p>
 * 每个出现的字符都应当映射到另一个字符，同时不改变字符的顺序。不同字符不能映射到同一个字符上，相同字符只能映射到同一个字符上，字符可以映射到自己本身。
 *
 * <pre>
 * 示例 1:
 *
 * 输入：s = "egg", t = "add"
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = "foo", t = "bar"
 * 输出：false
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = "paper", t = "title"
 * 输出：true
 * </pre>
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2026-01-29 10:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IsomorphicStringChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s: ");
		String s = scanner.nextLine();
		System.out.print("请输入字符串t: ");
		String t = scanner.nextLine();
		System.out.println(isIsomorphic(s, t));
	}

	/**
	 * 核心方法：判断两个字符串是否同构
	 *
	 * @param s 源字符串
	 * @param t 目标字符串
	 * @return true-同构，false-不同构
	 */
	private static boolean isIsomorphic(String s, String t) {
		// 边界条件：长度不同直接返回false
		if (s.length() != t.length()) {
			return false;
		}

		// 维护s字符到t字符的映射
		Map<Character, Character> s2t = new HashMap<>();
		// 维护t字符到s字符的映射（防止不同字符映射到同一个字符）
		Map<Character, Character> t2s = new HashMap<>();

		// 遍历每个字符位置
		for (int i = 0; i < s.length(); i++) {
			char sChar = s.charAt(i); // 当前s的字符
			char tChar = t.charAt(i); // 当前t的字符

			// 情况1：sChar已有映射，但映射值不等于当前tChar → 映射冲突
			if (s2t.containsKey(sChar)) {
				if (s2t.get(sChar) != tChar) {
					return false;
				}
			} else {
				// 情况2：sChar没有映射，但tChar已有映射，映射值不等于当前sChar → 映射冲突
				if (t2s.containsKey(tChar)) {
					return false;
				}
			}

			// 情况3：双向无映射，建立新的双向映射
			s2t.put(sChar, tChar);
			t2s.put(tChar, sChar);
		}

		// 所有字符校验通过，返回true
		return true;
	}
}
