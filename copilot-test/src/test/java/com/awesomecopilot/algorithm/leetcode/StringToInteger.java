package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 字符串转换整数 (atoi)
 * <p>
 * 请你来实现一个 myAtoi(string s) 函数，使其能将字符串转换成一个 32 位有符号整数 <p>
 * 函数 myAtoi(string s) 的算法如下： <p>
 * <p>
 * 空格：读入字符串并丢弃无用的前导空格（" "） <p>
 * 符号：检查下一个字符（假设还未到字符末尾）为 '-' 还是 '+'。如果两者都不存在，则假定结果为正。 <p>
 * 转换：通过跳过前置零来读取该整数，直到遇到非数字字符或到达字符串的结尾。如果没有读取数字，则结果为0。 <p>
 * 舍入：如果整数数超过 32 位有符号整数范围 [−231,  231 − 1] ，需要截断这个整数，使其保持在这个范围内。具体来说，小于 −231 的整数应该被舍入为 −231 ，大于 231 − 1 的整数应该被舍入为 231
 * − 1 。
 * 返回整数作为最终结果。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：s = "42"
 * 输出：42
 * 解释：加粗的字符串为已经读入的字符，插入符号是当前读取的字符。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = " -042"
 * 输出：-42
 * 解释：
 * 第 1 步："   -042"（读入前导空格，但忽视掉）
 * 第 2 步："   -042"（读入 '-' 字符，所以结果应该是负数）
 * 第 3 步："   -042"（读入 "042"，在结果中忽略前导零）
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = "1337c0d3"
 * 输出：1337
 * 解释：
 * 第 1 步："1337c0d3"（当前没有读入字符，因为没有前导空格）
 * 第 2 步："1337c0d3"（当前没有读入字符，因为这里不存在 '-' 或者 '+'）
 * 第 3 步："1337c0d3"（读入 "1337"；由于下一个字符不是一个数字，所以读入停止）
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：s = "0-1"
 * 输出：0
 * 解释：
 * 第 1 步："0-1" (当前没有读入字符，因为没有前导空格)
 * 第 2 步："0-1" (当前没有读入字符，因为这里不存在 '-' 或者 '+')
 * 第 3 步："0-1" (读入 "0"；由于下一个字符不是一个数字，所以读入停止)
 * </pre>
 *
 * <pre>
 * 示例 5：
 * 输入：s = "words and 987"
 * 输出：0
 * 解释：
 * 读取在第一个非数字字符“w”处停止。
 * </pre>
 * <p>
 * <p>
 * 请用Java实现并给出核心的解题思路, 不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-09-07 7:25
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringToInteger {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s: ");
		String s = scanner.nextLine();
		System.out.println(myAtoi(s));
	}

	private static int myAtoi(String s) {
		if (s == null || s.length() == 0) {
			return 0;
		}
		int n = s.length();
		int index = 0; // 当前处理的字符索引
		int sign = 1; // 符号，1表示正数，-1表示负数
		long result = 0; // 使用long存储中间结果，避免溢出
		while (index < n && s.charAt(index) == ' ') {
			index++;
		}
		// 如果已经到达字符串末尾，返回0
		if (index == n) {
			return 0;
		}

		// 2. 处理符号
		if (s.charAt(index) == '+' || s.charAt(index) == '-') {
			sign = s.charAt(index) == '+' ? 1 : -1;
			index++;
		}

		//读取数字
		while (index<n) {
			char c = s.charAt(index);
			// 检查是否为数字字符
			//if (c < '0' || c > '9') {
			if (!Character.isDigit(c)) {
				break;
			}

			// 将字符转换为数字并累加到结果中
			int digit = c - '0';

			// 4. 检查是否超出32位有符号整数范围
			// 提前判断是否会溢出，避免后续计算溢出
			if (result > Integer.MAX_VALUE / 10 ||
					/*
					 * 这行代码用于检查当结果接近Integer.MAX_VALUE（2147483647）时，添加下一个数字是否会导致溢出。
					 * Integer.MAX_VALUE / 10 的结果是 214748364
					 * Integer.MAX_VALUE % 10 的结果是 7
					 * 假设当前result = 214748364（刚好等于Integer.MAX_VALUE / 10）
					 * 若下一个数字digit = 8
					 * 因为 8 > 7（Integer.MAX_VALUE % 10）
					 * 计算结果会是 2147483648，超过Integer.MAX_VALUE
					 * 触发溢出条件，返回Integer.MAX_VALUE
					 */
					(result == Integer.MAX_VALUE/10 && digit > Integer.MAX_VALUE % 10)) {
				return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}

			result = result * 10 + digit;
			index++;
		}

		// 应用符号并转换为int返回
		return (int) result * sign;
	}
}
