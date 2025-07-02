package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 字符串分割(二) - 双指针
 * <p>
 * 一、题目描述
 * <p>
 * 给定一个非空字符串S，其被N个'-'分隔成N+1的子串，给定正整数K，要求除第一个子串外，其余的子串每K个字符组成新的子串，并用‘-’分隔。
 * <p>
 * 对于新组成的每一个子串，
 * <p>
 * 如果它含有的小写字母比大写字母多，则将这个子串的所有大写字母转换为小写字母；<br/>
 * 反之，如果它含有的大写字母比小写字母多，则将这个子串的所有小写字母转换为大写字母； <br/>
 * 大小写字母的数量相等时，不做转换。 <br/>
 * <p>
 * 二、输入描述
 * <p>
 * 输入为两行，第一行为参数K，第二行为字符串S。
 * <p>
 * 三、输出描述
 * <p>
 * 输出转换后的字符串。
 *
 * <pre>
 * 测试用例1
 * 1、输入
 * 3
 * 12abc-abcABC-4aB@
 *
 * 2、输出
 * 12abc-abc-ABC-4aB-@
 * </pre>
 *
 * <pre>
 * 测试用例2
 * 1、输入
 * 3
 * Test-aaBBcc-CCddEE
 *
 * 2、输出
 * Test-aab-bcc-CCD-DEE
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-22 11:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringFormatter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入整数K: ");
		int k = scanner.nextInt();
		System.out.print("请输入字符串S: ");
		scanner.nextLine();
		String s = scanner.nextLine().trim();

		System.out.println(formatString(k, s));
	}

	/**
	 * 将字符串按照题目要求格式化
	 *
	 * @param k 每组子串长度
	 * @param s 原始输入字符串
	 * @return 格式化后的字符串
	 */
	public static String formatString(int k, String s) {
		String[] parts = s.split("-");

		// 保留第一个子串
		StringBuilder result = new StringBuilder(parts[0]);

		StringBuilder merged = new StringBuilder();
		// 收集其余子串字符拼接为一个字符串
		for (int i = 1; i < parts.length; i++) {
			merged.append(parts[i]);
		}

		// 双指针按K长度分组处理
		for (int i = 0; i < merged.length(); i += k) {
			// 当前子串长度不一定为k（如最后不足k个字符）
			int end = Math.min(i + k, merged.length());
			String group = merged.substring(i, end);
			result.append("-").append(transformGroup(group));
		}

		return result.toString();
	}

	/**
	 * 按规则转换子串中大小写
	 *
	 * @param group 子串
	 * @return 转换后的子串
	 */
	public static String transformGroup(String group) {
		int upper = 0, lower = 0;
		for (char c : group.toCharArray()) {
			if (Character.isUpperCase(c)) {
				upper++;
			} else {
				lower++;
			}
		}

		// 大小写数量不同，根据规则进行转换
		if (lower > upper) {
			return group.toLowerCase();
		} else if (upper > lower) {
			return group.toUpperCase();
		} else {
			return group;
		}
	}
}
