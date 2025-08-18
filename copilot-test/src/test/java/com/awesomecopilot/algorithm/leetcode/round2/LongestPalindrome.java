package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.Scanner;

/**
 * 最长回文子串
 * 给你一个字符串 s，找到 s 中最长的回文子串
 * 示例 1：
 * 输入：s = "babad"
 * 输出："bab"
 * 解释："aba" 同样是符合题意的答案。
 * 示例 2：
 * 输入：s = "cbbd"
 * 输出："bb"
 * <p/>
 * <ul>最长回文子串问题可以通过 "中心扩展法" 高效解决，其核心思路是：
 *     <li/>回文串具有中心对称性（奇数长度回文以字符为中心，偶数长度以两个字符中间为中心）
 *     <li/>遍历字符串每个可能的中心，向两侧扩展寻找最长回文
 *     <li/>记录扩展过程中发现的最长回文子串
 * </ul>
 * <ul>以输入 "babad" 为例，说明中心扩展过程：
 *     <ul>当 i=0 (字符 'b')
 *       <li/>奇数扩展：left=0, right=0 → 扩展后 left=-1, right=1 → 回文 "b" (长度 1)
 *       <li/>偶数扩展：left=0, right=1 → 字符不同，不扩展 → 无有效回文
 *     </ul>
 *     <ul>当 i=1 (字符 'a')
 *       <li/>奇数扩展：left=1, right=1 → 扩展到 left=0, right=2 (字符 'b' 和 'b')→ 再扩展失败
 *       <li/>偶数扩展：left=1, right=2 → 字符不同，不扩展
 *     </ul>
 *     <ul>当 i=2 (字符 'b')
 *       <li/>奇数扩展：left=2, right=2 → 扩展到 left=1, right=3（字符 'a' 和 'a'）→ 再扩展失败
 *       <li/>偶数扩展：left=2, right=3 → 字符不同，不扩展
 *     </ul>
 * </ul>
 * Copyright: Copyright (c) 2025-08-08 9:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestPalindrome {
	// 存储最长回文子串的起始索引和长度
	static int start = 0, maxLength = 1;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入原始字符串: ");
		String s = scanner.nextLine();
		String result = longestPalindrome(s);
		System.out.println(result);
	}

	private static String longestPalindrome(String s) {
		for (int i = 0; i < s.length(); i++) {
			// 处理奇数长度的回文（以当前字符为中心）
			expandAroundCenter(s, i, i);
			// 处理偶数长度的回文（以当前字符和下一个字符之间为中心）
			expandAroundCenter(s, i, i + 1);
		}

		// 根据记录的起始索引和长度返回最长回文子串
		return s.substring(start, start + maxLength);
	}

	/**
	 * 从中心向两侧扩展，寻找最长回文
	 * @param s 输入字符串
	 * @param left left 左指针，起始中心的左位置
	 * @param right right 右指针，起始中心的右位置
	 */
	private static void expandAroundCenter(String s, int left, int right) {
		// 当左右指针不越界且指向的字符相同时，继续向两侧扩展
		while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
			left--;
			right++;
		}

		// 计算当前找到的回文长度
		// 退出循环时左右指针已不满足条件，所以实际长度是(right-1)-(left+1)+1 = right-left-1
		int currLength = right - left - 1;

		// 如果当前回文长度大于已记录的最大长度，则更新记录
		if (currLength > maxLength) {
			// 更新最大长度
			maxLength = currLength;
			// 计算起始索引（退出循环时left已减1，所以实际起始索引是left+1）
			start = left + 1;
		}
	}
}
