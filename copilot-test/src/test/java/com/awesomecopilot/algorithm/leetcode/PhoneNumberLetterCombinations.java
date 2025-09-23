package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 电话号码的字母组合
 * <p>
 * 给定一个仅包含数字 2-9 的字符串，返回所有它能表示的字母组合。答案可以按 任意顺序 返回。
 * <p>
 * 给出数字到字母的映射如下（与电话按键相同）。注意 1 不对应任何字母。
 *
 * <image src="images/phone.png" </image>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：digits = "23"
 * 输出：["ad","ae","af","bd","be","bf","cd","ce","cf"]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：digits = ""
 * 输出：[]
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：digits = "2"
 * 输出：["a","b","c"]
 * </pre>
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2025-09-18 9:11
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PhoneNumberLetterCombinations {

	public static final String[] LETTER_MAP = {
			"",     // 0
			"",     // 1
			"abc",  // 2
			"def",  // 3
			"ghi",  // 4
			"jkl",  // 5
			"mno",  // 6
			"pqrs", // 7
			"tuv",  // 8
			"wxyz"  // 9
	};

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter digits: ");
		String digits = scanner.nextLine().trim();
		System.out.println(letterCombinations(digits));
	}
	public static List<String> letterCombinations(String digits) {
		List<String> result = new ArrayList<>();
		// 处理空输入的情况
		if (digits == null || digits.length() == 0) {
			return result;
		}
		// 调用回溯方法
		backtrack(result, new StringBuilder(), digits, 0);
		return result;
	}

	/**
	 * 回溯法生成所有字母组合
	 *
	 * @param result  存储最终结果的集合
	 * @param current 存储当前组合的字符串
	 * @param digits  输入的数字字符串
	 * @param index   当前处理的数字索引
	 */
	private static void backtrack(List<String> result, StringBuilder current, String digits, int index) {
		// 递归终止条件：已经处理完所有数字
		if (index == digits.length()) {
			result.add(current.toString());
			return;
		}
		// 获取当前数字对应的字母
		String letters = LETTER_MAP[digits.charAt(index) - '0'];

		// 遍历当前数字对应的所有字母
		for (int i = 0; i < letters.length(); i++) {
			// 添加当前字母到组合中
			current.append(letters.charAt(i));
			// 递归处理下一个数字
			backtrack(result, current, digits, index + 1);
			// 回溯：移除最后添加的字母，尝试其他可能性
			current.deleteCharAt(current.length() - 1);
		}
	}
}
