package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * 无重复字符的最长子串
 * <p>
 * 给定一个字符串 s ，请你找出其中不含有重复字符的 最长 子串 的长度。
 * <p>
 * <pre>
 * 示例 1:
 *
 * 输入: s = "abcabcbb"
 * 输出: 3
 * 解释: 因为无重复字符的最长子串是 "abc"，所以其长度为 3。
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: s = "bbbbb"
 * 输出: 1
 * 解释: 因为无重复字符的最长子串是 "b"，所以其长度为 1。
 * </pre>
 *
 * <pre>
 * 示例 3:
 *
 * 输入: s = "pwwkew"
 * 输出: 3
 * 解释: 因为无重复字符的最长子串是 "wke"，所以其长度为 3。
 *      请注意，你的答案必须是 子串 的长度，"pwke" 是一个子序列，不是子串。
 * </pre>
 * 这个题就是经典的 滑动窗口（Sliding Window） 问题。
 * <ol>核心思路：
 *     <li/>用两个指针（left 和 right）表示一个滑动窗口，保证窗口内的子串没有重复字符。
 *     <li/>使用 HashSet 或 HashMap 来记录窗口内出现的字符。
 *     <li/>每次遇到重复字符时，移动 left 指针缩小窗口，直到窗口再次满足条件。
 *     <li/>在遍历过程中更新最大子串长度。
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-10-16 9:59
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestSubstringWithoutRepeating {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter string: ");
		String s = scanner.nextLine().trim();
		System.out.println(lengthOfLongestSubstring(s));

	}

	public static int lengthOfLongestSubstring(String s) {
		Set<Character> window = new HashSet<>();
		int left = 0;
		int right = 0;
		int maxLength = 0;

		while (right < s.length()) {
			char c = s.charAt(right);
			while (window.contains(c)) {
				window.remove(s.charAt(left));
				left++;
			}
			window.add(c);
			maxLength = Math.max(maxLength, right - left +1);
			right++;
		}

		return maxLength;
	}
}
