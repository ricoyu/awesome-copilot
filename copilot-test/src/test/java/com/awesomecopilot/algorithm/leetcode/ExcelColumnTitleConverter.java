package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * Excel 表列名称
 * <p>
 * 给你一个整数 columnNumber ，返回它在 Excel 表中相对应的列名称。
 * <p>
 * <pre>
 * 例如：
 *
 * A -> 1
 * B -> 2
 * C -> 3
 * ...
 * Z -> 26
 * AA -> 27
 * AB -> 28
 * ...
 * </pre>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：columnNumber = 1
 * 输出："A"
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：columnNumber = 28
 * 输出："AB"
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：columnNumber = 701
 * 输出："ZY"
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：columnNumber = 2147483647
 * 输出："FXSHRXW"
 * </pre>
 * <ul>核心解题思路
 *     <li/>Excel 列名转换不是标准的 26 进制（标准 26 进制是 0-25），而是 1-26 对应 A-Z，因此核心技巧是：
 *     <li/>每次转换前先将 columnNumber 减 1，把数值映射到 0-25 的范围（对应 A-Z）；
 *     <li/>对 26 取余得到当前位的字符（0→A，1→B…25→Z）；
 *     <li/>将 columnNumber 除以 26，重复上述步骤直到数值为 0；
 *     <li/>最后反转拼接的字符，得到最终结果。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-12 9:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ExcelColumnTitleConverter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入列数: ");
		int columnNumber = scanner.nextInt();
		System.out.println(convertToTitle(columnNumber));
	}

	/**
	 * 将整数列号转换为Excel列名称
	 * @param columnNumber
	 * @return
	 */
	private static String convertToTitle(int columnNumber) {
		// 用于拼接结果的字符串构建器
		StringBuilder sb = new StringBuilder();

		// 循环处理，直到columnNumber变为0
		while (columnNumber > 0) {
			// 核心技巧：减1操作，将1-26映射为0-25，适配ASCII码计算
			columnNumber--;
			// 取余得到当前位的偏移量（0-25），加上'A'的ASCII码得到对应字符
			char ch = (char) ('A' + columnNumber % 26);
			// 将当前字符添加到字符串构建器（此时是逆序的）
			sb.append(ch);
			// 除以26，处理下一位
			columnNumber /= 26;
		}

		// 反转字符串得到正确顺序
		return sb.reverse().toString();
	}
}
