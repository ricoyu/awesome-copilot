package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;
import java.util.Stack;

/**
 * 最长有效括号
 * <p>
 * <image src="images/stack.png" />
 * <p>
 * 给你一个只包含 '(' 和 ')' 的字符串，找出最长有效（格式正确且连续）括号子串的长度。
 * <p>
 * 左右括号匹配，即每个左括号都有对应的右括号将其闭合的字符串是格式正确的，比如 "(()())"。
 * <p>
 * <pre>
 * 示例 1：
 *
 * 输入：s = "(()"
 * 输出：2
 * </pre>
 * 解释：最长有效括号子串是 "()"
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = ")()())"
 * 输出：4
 * </pre>
 * 解释：最长有效括号子串是 "()()"
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = ""
 * 输出：0
 * </pre>
 * <p>
 * 核心解题思路：<p>
 * 使用栈存储未匹配左括号的索引（初始推入-1作为哨兵，便于计算边界长度）。<br/>
 * 遍历字符串，遇到'('推入索引，遇到')'弹出栈顶； <br/>
 * 若栈空则推入当前索引作为新哨兵，否则计算当前有效子串长度（当前索引减栈顶索引）并更新最大值。<br/>
 * 该方法时间复杂度O(n)，空间O(n)。
 * <p/>
 * Copyright: Copyright (c) 2025-12-13 12:44
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestValidParentheses {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter string: ");
		String s = scanner.nextLine().trim();
		System.out.println(longestValidParentheses(s));
	}

	/**
	 * 计算字符串中最长有效括号子串的长度。
	 *
	 * @param s 输入字符串，只包含'('和')'
	 * @return 最长有效括号子串的长度
	 */
	private static int longestValidParentheses(String s) {
		// 处理空字符串或null的情况, 直接返回0
		if (s == null || s.length() == 0) {
			return 0;
		}
		// 初始化栈, 用于存储未匹配的左括号索引, 初始推入-1作为哨兵 (便于计算从字符串开头开始的有效长度)
		Stack<Integer> stack = new Stack<>();
		stack.push(-1);

		// 记录最大有效长度
		int maxLength = 0;

		for (int i = 0; i < s.length(); i++) {
			// 如果当前字符是左括号'('，将其索引推入栈（等待匹配）
			if (s.charAt(i) == '(') {
				stack.push(i);
			} else {
				// 如果是右括号')'，弹出栈顶（尝试匹配最近的左括号或哨兵）
				/*
				 * 详细解释为什么 pop() 就代表成功匹配
				 * 算法的核心假设是：
				 *   我们只在遇到 '(' 时才把索引压入栈。
				 *   所以栈里存储的索引 永远对应位置上的字符是 '('（除了最底层的 -1 哨兵）。
				 *
				 * 因此，当我们遇到一个 ')' 时：
				 * 执行 stack.pop()
				 *     如果栈顶本来就是一个未匹配的 '(' 的索引，弹出它就相当于 把这个左括号和当前的右括号配对成功。
				 *     这时候栈不为空（至少还有哨兵或其他更外层的左括号），我们就可以安全地计算当前已匹配的有效长度：i - stack.peek()
				 *
				 * 如果弹出后栈为空，说明：
				 *     刚才弹出的其实是哨兵 -1，或者之前已经把所有左括号都匹配完了。
				 *     当前这个 ')' 是多余的、无法匹配的，它破坏了从字符串开头到这里的有效性。
				 *     所以我们把当前索引 i 重新压入栈，作为新的“无效起点”（新哨兵），后续计算长度时就不会跨越这个多余的右括号。
				 */
				stack.pop();

				// 如果栈为空，说明这个右括号无法匹配（多余的右括号），推入当前索引作为新的无效起始点（新哨兵）
				if (stack.isEmpty()) {
					stack.push(i);
				} else {
					// 栈不为空，说明匹配成功，计算当前有效子串长度：当前索引 i 减去栈顶索引（栈顶是最近未匹配的左括号或哨兵）
					// 更新最大长度
					maxLength = Math.max(maxLength, i - stack.peek());
				}
			}
		}
		return maxLength;
	}
}
