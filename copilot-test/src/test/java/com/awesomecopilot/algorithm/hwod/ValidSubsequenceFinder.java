package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 有效子字符串 - 双指针
 * <p>
 * 一、题目描述
 * <p>
 * 输入两个字符串S和L，都只包含小写字母，len(S) <= 100，len(L) <= 500000。判断S是否是L的有效子字符串。
 * <p>
 * 判定规则：S中的每个字符在L中都能找到（可以不连续），且S在L中字符的前后顺序与S中顺序要保持一致。 <p>
 * 例如：
 * <p>
 * S = "ace"是L = "abcde"的一个子序列，且有效字符是a、c、e，而"aec"不是有效子序列，且有效字符只有a、e（因为相对位置不同）。
 * <p>
 * 二、输入描述
 * <p>
 * 输入两个字符串S和L，都只包含小写字母，len(S) <= 100，len(L) <= 500000，先输入S再输入L 每个字符串占一行。
 * <p>
 * 三、输出描述 <p>
 * S串最后一个有效字符在L中的位置，首位从0开始计算。无有效字符返回 -1
 * <pre>
 * 1、输入
 * ace
 * abcde
 *
 * 2、输出
 * 4
 * </pre>
 * <ul>核心解题思路（双指针法）
 *     <li/>一个指针 i 遍历子串 S。
 *     <li/>一个指针 j 遍历主串 L。
 *     <li/>若 S.charAt(i) == L.charAt(j)，说明当前字符匹配，i++。
 *     <li/>不管匹不匹配，j++ 始终前进。
 *     <li/>
 *     <li/>
 * </ul>
 * <pre>
 * 示例1
 * S = "ace"
 * L = "abcde"
 * </pre>
 * <ol>执行过程
 *     <li/>i=0, j=0: 'a'=='a' → match ✅ → i=1, j=1
 *     <li/>i=1, j=1: 'c'!='b' → j++
 *     <li/>i=1, j=2: 'c'=='c' → match ✅ → i=2, j=3
 *     <li/>i=2, j=3: 'e'!='d' → j++
 *     <li/>i=2, j=4: 'e'=='e' → match ✅ → i=3
 * </ol>
 * <pre>
 * 示例2 顺序不一致
 * S = "aec"
 * L = "abcde"
 * </pre>
 * <ol>执行过程：
 *     <li/>'a' → match at j=0 ✅
 *     <li/>'e' → match at j=4 ✅
 *     <li/>'c' → 再找 'c'，但 j 已到末尾 ❌
 *     <li/>因为 'c' 出现在 'e' 之后，不满足顺序一致性，匹配失败，返回 -1。
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-06-21 9:36
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ValidSubsequenceFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s: ");
		String s = scanner.nextLine().trim();
		System.out.print("请输入字符串l: ");
		String l = scanner.nextLine().trim();
		System.out.println(findLastMatchedIndex(s, l));
	}

	/**
	 * 判断 S 是否是 L 的有效子序列，并返回 S 中最后一个匹配字符在 L 中的位置
	 *
	 * @param s 子序列字符串
	 * @param l 主字符串
	 * @return 如果是有效子序列，则返回 S 最后一个匹配字符在 L 中的索引；否则返回 -1。
	 */
	public static int findLastMatchedIndex(String s, String l) {
		int i = 0; //指向s的指针
		int j = 0; //指向l的指针
		int lastMatchIndex = -1;

		// 开始遍历 L，寻找与 S 匹配的字符
		while (i < s.length() && j < l.length()) {
			if (s.charAt(i) == l.charAt(j)) {
				lastMatchIndex = j; // 记录当前匹配成功的索引
				i++;// 移动 S 的指针
			}
			j++; // 每次都要移动 L 的指针
		}

		// 如果 i 没有遍历完 S，说明 S 不是 L 的有效子序列
		return i == s.length() ? lastMatchIndex : -1;
	}
}
