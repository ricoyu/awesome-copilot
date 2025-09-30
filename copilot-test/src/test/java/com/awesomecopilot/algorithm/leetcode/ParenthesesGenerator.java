package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 括号生成
 * <p>
 * 数字 n 代表生成括号的对数，请你设计一个函数，用于能够生成所有可能的并且有效的 括号组合。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 3
 * 输出：["((()))","(()())","(())()","()(())","()()()"]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 1
 * 输出：["()"]
 * </pre>
 *
 * <ul>核心采用回溯算法：
 *     <li/>维护两个计数器，分别记录已使用的左括号数和右括号数
 *     <li/>当左括号数小于 n 时，可添加左括号
 *     <li/>当右括号数小于左括号数时，可添加右括号
 *     <li/>当左右括号和右括号总数达到 2n 时，生成一个有效组合
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-24 8:50
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ParenthesesGenerator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter n: ");
		int n = scanner.nextInt();
		List<String> result = new ArrayList<>();
		// 调用回溯方法，初始状态：空字符串，0个左括号，0个右括号
		backtrack(result, new StringBuilder(), 0, 0, n);
		scanner.close();
	}

	/**
	 * 回溯算法生成有效括号组合
	 * @param result  存储结果的列表
	 * @param current 当前构建的括号字符串
	 * @param open    已使用的左括号数量
	 * @param close   已使用的右括号数量
	 * @param max     最大括号对数（n）
	 * @return
	 */
	private static void backtrack(List<String> result,
	                                          StringBuilder current,
	                                          int open,
	                                          int close,
	                                          int max) {
		// 终止条件：当前字符串长度达到2n（括号对数的2倍）
		if (current.length() == max * 2) {
			result.add(current.toString());
			return;
		}

		// 如果左括号数量小于n，可以添加左括号
		if (open < max) {
			current.append('(');
			backtrack(result, current, open + 1, close, max);
			// 回溯：移除最后添加的左括号
			current.deleteCharAt(current.length() - 1);
		}

		// 如果右括号数量小于左括号数量，可以添加右括号
		if (close < open) {
			current.append(')');
			backtrack(result, current, open, close + 1, max);
			// 回溯：移除最后添加的右括号
			current.deleteCharAt(current.length() - 1);
		}
	}

}
