package com.awesomecopilot.algorithm.exam_egroup;

import java.util.Scanner;

/**
 * 求满足条件的最长子串的长度
 * <p>
 * 一、题目描述
 * <p>
 * 给定一个字符串，只包含字母和数字，按要求找出字符串中的最长（连续）子串的长度，字符串本身是其最长的子串，子串要求：
 * <p>
 * 只包含1个字母(a-z, A-Z)，其余必须是数字； <p>
 * 字母可以在子串中的任意位置； <p>
 * <p>
 * 如果找不到满足要求的子串，如全是字母或全是数字，则返回-1。
 * <p>
 * 二、输入描述 <p>
 * 字符串(只包含字母和数字)。
 * <p>
 * 三、输出描述 <p>
 * 子串的长度。
 * <p/>
 * Copyright: Copyright (c) 2025-07-23 9:13
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestValidSubstringFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String s = scanner.nextLine().trim();
		System.out.println(longestValidSubstring(s));
	}

	public static int longestValidSubstring(String s) {
		int maxLen = -1;// 记录满足条件的最长子串长度，初始化为 -1 表示找不到
		int left = 0; // 滑动窗口左指针
		int letterCount = 0; // 当前窗口内字母数量
		int digitCount = 0;  // 当前窗口中数字个数

		for (int right = 0; right < s.length(); right++) {
			char c = s.charAt(right);

			// 如果当前字符是字母，则字母数量加1
			if (Character.isLetter(c)) {
				letterCount++;
			} else if (Character.isDigit(c)) {
				digitCount++;
			}

			// 如果字母数量超过1，移动左指针直到只有1个字母
			while (letterCount > 1) {
				char leftChar = s.charAt(left);
				if (Character.isLetter(leftChar)) {
					letterCount--;
				} else if (Character.isDigit(leftChar)) {
					digitCount--;
				}
				left++;
			}

			// 只包含一个字母，其余全是数字，更新最大长度
			int windowLen = right - left + 1;
			// 如果窗口中恰好有一个字母，更新最大长度
			if (letterCount == 1 && digitCount >= 1 && digitCount == windowLen - 1) {
				maxLen = Math.max(maxLen, windowLen);
			}
		}

		return maxLen;
	}
}
