package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 最后一个单词的长度
 * <p>
 * 给你一个字符串 s，由若干单词组成，单词前后用一些空格字符隔开。返回字符串中 最后一个单词的长度。
 * <p>
 * 单词 是指仅由字母组成、不包含任何空格字符的最大子字符串。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：s = "Hello World"
 * 输出：5
 * 解释：最后一个单词是“World”，长度为 5。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = "   fly me   to   the moon  "
 * 输出：4
 * 解释：最后一个单词是“moon”，长度为 4。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = "luffy is still joyboy"
 * 输出：6
 * 解释：最后一个单词是长度为 6 的“joyboy”。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-12-24 8:32
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LastWordLengthCalculator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String s = scanner.nextLine();
		System.out.println(lengthOfLastWord(s));
		scanner.close();
	}

	private static int lengthOfLastWord(String s) {
		if (s == null || s.length() == 0) {
			return 0;
		}
		int len = 0;
		int index = s.length() - 1;
		while (index >=0) {
			if (s.charAt(index) == ' ' && len > 0) {
				return len;
			} else if (s.charAt(index) != ' ') {
				len++;
				index--;
			} else if (s.charAt(index) == ' ' && len == 0) {
				index --;
			}
		}
		return len;
	}
}
