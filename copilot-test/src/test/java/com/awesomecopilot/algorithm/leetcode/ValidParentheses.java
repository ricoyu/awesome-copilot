package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;
import java.util.Stack;

/**
 * 有效的括号
 * <p>
 * 给定一个只包括 '('，')'，'{'，'}'，'['，']' 的字符串 s ，判断字符串是否有效。
 * <p>
 * 有效字符串需满足：
 * <p>
 * 左括号必须用相同类型的右括号闭合。 <br/>
 * 左括号必须以正确的顺序闭合。 <br/>
 * 每个右括号都有一个对应的相同类型的左括号。 <br/>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：s = "()"
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = "()[]{}"
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = "(]"
 * 输出：false
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：s = "([])"
 * 输出：true
 * </pre>
 *
 * <pre>
 * 示例 5：
 *
 * 输入：s = "([)]"
 * 输出：false
 * </pre>
 *
 * <ul>判断有效的括号可以使用栈（Stack）这种数据结构来解决，核心思路如下：
 *     <li/>遍历字符串中的每个字符
 *     <li/>遇到左括号（'(', '{', '['）时，将其入栈
 *     <ul>遇到右括号时，检查栈顶元素是否为对应的左括号：
 *         <li/>若匹配，则弹出栈顶元素
 *         <li/>若不匹配或栈为空，则字符串无效
 *     </ul>
 *     <li/>遍历结束后，若栈为空则有效，否则无效
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-22 8:15
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ValidParentheses {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter string: ");
		String s = scanner.nextLine().trim();
		System.out.println(isValid(s));
	}

	/**
	 * 判断括号字符串是否有效
	 * @param s 待判断的括号字符串
	 * @return 有效返回true，否则返回false
	 */
	private static boolean isValid(String s) {
		// 创建栈用于存储左括号
		Stack<Character> stack = new Stack<>();

		// 遍历字符串中的每个字符
		for (int i = 0; i < s.length(); i++) {
		    char c = s.charAt(i);
			// 如果是左括号，入栈
			if (c == '(' || c == '{' || c == '[') {
				stack.push(c);
			}else {
				// 如果是右括号，但栈为空，说明没有匹配的左括号，直接返回false
				if (stack.isEmpty()) {
					return false;
				}

				// 弹出栈顶元素，检查是否匹配
				char top = stack.pop();
				if (c == ')' && top != '(' || c == '}' && top != '{' || c == ']' && top != '[') {
					return false;
				}
			}
		}

		// 遍历结束后，栈必须为空才是有效的（所有左括号都有匹配的右括号）
		return stack.isEmpty();
	}
}
