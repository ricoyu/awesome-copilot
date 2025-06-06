package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * <p/>
 * Copyright: Copyright (c) 2025-05-23 8:39
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestVowelSubstring {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入瑕疵度: ");
		int flaw = scanner.nextInt();
		scanner.nextLine();
		System.out.print("字符串: ");
		String s = scanner.nextLine();
		System.out.println(findLongestVowelSubstring(flaw, s));
	}

	public static int findLongestVowelSubstring(int flaw, String s) {
		int maxLen = 0;
		int left = 0;
		int nonVowelCount = 0;

		for (int right = 0; right < s.length(); right++) {
			// 如果右边不是元音，瑕疵度加1
			if (!isVowel(s.charAt(right))) {
				nonVowelCount++;
			}

			// 缩小窗口直到瑕疵度符合条件
			while (nonVowelCount > flaw) {
				if (!isVowel(s.charAt(left))) {
					nonVowelCount--;
				}
				left++;
			}

			// 窗口满足瑕疵度要求，判断头尾是否为元音
			if (left <= right && isVowel(s.charAt(left)) && isVowel(s.charAt(right))) {
				maxLen = Math.max(maxLen, right - left + 1);
			}
		}

		return maxLen;
	}

	/**
	 * 判断是否是元音字母
	 *
	 * @param ch
	 * @return
	 */
	private static boolean isVowel(char ch) {
		return "aeiouAEIOU".indexOf(ch) != -1;
	}
}
