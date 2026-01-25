package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * Excel 表列序号
 * <p>
 * 给你一个字符串 columnTitle ，表示 Excel 表格中的列名称。返回 该列名称对应的列序号 。
 * <p>
 * 例如：
 *
 * <pre>
 * A -> 1
 * B -> 2
 * C -> 3
 * ...
 * Z -> 26
 * AA -> 27
 * AB -> 28
 * </pre>
 * ...
 *
 *
 * <pre>
 * 示例 1:
 *
 * 输入: columnTitle = "A"
 * 输出: 1
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: columnTitle = "AB"
 * 输出: 28
 * </pre>
 *
 * <pre>
 * 示例 3:
 *
 * 输入: columnTitle = "ZY"
 * 输出: 701
 * </pre>
 *
 * <ul>解题思路
 *     <li/>这个问题的核心逻辑和进制转换一致，但有个关键区别：常规 26 进制是 0-25，而 Excel 列名是 1-26（A-Z）。
 *     <li/>从字符串的最左侧（最高位）开始遍历每个字符
 *     <li/>每遍历一个字符，先将当前结果乘以 26（进位），再加上当前字符对应的数值
 *     <li/>字符转数值的公式：字符的ASCII码 - 'A'的ASCII码 + 1（比如 'A'->65-65+1=1，'B'->66-65+1=2）
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-20 9:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ExcelColumnNumberConverter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter column name: ");
		String columnName = scanner.nextLine().trim();
		System.out.println(columnNumber(columnName));
	}

	private static int columnNumber(String columnName) {
		int result = 0;
		for (int i = 0; i < columnName.length(); i++) {
			result = result * 26 + (columnName.charAt(i) - 'A' + 1);
		}
		return result;
	}
}
