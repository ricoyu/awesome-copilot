package com.awesomecopilot.algorithm.leetcode.round2;

import java.util.Scanner;

/**
 * 找出字符串中第一个匹配项的下标
 * <p>
 * 给你两个字符串 haystack 和 needle ，请你在 haystack 字符串中找出 needle 字符串的第一个匹配项的下标（下标从 0 开始）。如果 needle 不是 haystack 的一部分，则返回  -1 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：haystack = "sadbutsad", needle = "sad"
 * 输出：0
 * 解释："sad" 在下标 0 和 6 处匹配。
 * 第一个匹配项的下标是 0 ，所以返回 0 。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：haystack = "leetcode", needle = "leeto"
 * 输出：-1
 * 解释："leeto" 没有在 "leetcode" 中出现，所以返回 -1 。
 * </pre>
 *
 * <p/>
 * Copyright: Copyright (c) 2025-10-08 9:11
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FirstOccurrenceFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串haystack: ");
		String haystack = scanner.nextLine().trim();
		System.out.print("请输入字符串needle: ");
		String needle = scanner.nextLine().trim();

		System.out.println(findFirstOccurrenceIndex(haystack, needle));
	}

	private static int findFirstOccurrenceIndex(String haystack, String needle) {
		// 处理needle为空的情况
		if (needle == null || needle.length() == 0) {
			return 0;
		}

		// 获取两个字符串的长度
		int haystackLen = haystack.length();
		int needleLen = needle.length();

		// 如果needle比haystack长，直接返回-1
		if (needleLen > haystackLen) {
			return -1;
		}

		// 遍历haystack，尝试从每个可能的位置开始匹配
		// 最后一个可能的起始位置是haystackLen - needleLen
		for (int i = 0; i <= haystackLen - needleLen; i++) {
		    boolean match = true;

			// 检查从i开始的子串是否与needle匹配
			for (int j = 0; j < needleLen; j++) {
				// 如果有任何字符不匹配，标记为不匹配并跳出循环
				if (haystack.charAt(i+j) != needle.charAt(j)) {
					match = false;
					break;
				}
			}

			// 如果找到匹配，返回起始下标i
			if (match) {
				return i;
			}
		}

		// 没有找到匹配项
		return -1;
	}
}
