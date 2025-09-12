package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * Z 字形变换
 * <p>
 * 将一个给定字符串s根据给定的行数numRows ，以从上往下、从左到右进行 Z 字形排列。
 * <p>
 * 比如输入字符串为 "PAYPALISHIRING" 行数为 3 时，排列如下：
 * <pre>
 * P   A   H   N
 * A P L S I I G
 * Y   I   R
 * </pre>
 *
 * 之后，你的输出需要从左往右逐行读取，产生出一个新的字符串，比如："PAHNAPLSIIGYIR"。
 *
 * 请你实现这个将字符串进行指定行数变换的函数：
 *
 * string convert(string s, int numRows);
 * <pre>
 * 示例 1：
 *
 * 输入：s = "PAYPALISHIRING", numRows = 3
 * 输出："PAHNAPLSIIGYIR"
 * </pre>
 *
 * <pre>
 * 示例 2：
 * 输入：s = "PAYPALISHIRING", numRows = 4
 * 输出："PINALSIGYAHRPI"
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = "A", numRows = 1
 * 输出："A"
 * </pre>
 *
 * <ul>Z 字形变换的核心是确定每个字符在变换后所处的行位置。观察规律可知：
 *     <li/>字符按 "之" 字形排列，行数为 numRows
 *     <li/>当 numRows=1 时，直接返回原字符串
 *     <li/>其余情况，字符先从上到下填充行，再从下到上（除首尾行）填充中间行
 *     <li/>每个周期的字符数为 2*(numRows-1)
 * </ul>
 * <ul>以示例 1 s = "PAYPALISHIRING", numRows = 3 为例：
 *     <li/>初始化 3 个 StringBuilder 分别对应 3 行
 *     <li/>P：currentRow=0 → 行 0，方向改为向下
 *     <li/>A：currentRow=1 → 行 1
 *     <li/>Y：currentRow=2 → 行 2，方向改为向上
 *     <li/>P：currentRow=1 → 行 1
 *     <li/>A：currentRow=0 → 行 0，方向改为向下
 *     <li/>L：currentRow=1 → 行 1
 *     <li/>... 以此类推
 *     <ul>最终各行内容
 *       <li/>行 0: P A H N
 *       <li/>行 1: A P L S I I G
 *       <li/>行 2: Y I R
 *       <li/>拼接后得到结果 "PAHNAPLSIIGYIR"
 *     </ul>
 * </ul>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-05 8:50
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ZigzagConverter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String s = scanner.nextLine().trim();
		System.out.print("请输入行数: ");
		int numRows = scanner.nextInt();
		System.out.println(convert(s, numRows));
		scanner.close();
	}

	private static String convert(String s, int numRows) {
		// 特殊情况：行数为1或字符串长度小于等于行数，直接返回原字符串
		if (numRows == 1 || s.length() <= numRows) {
			return s;
		}

		// 创建numRows个StringBuilder, 分别存储每行的字符
		StringBuilder[] rows = new StringBuilder[numRows];
		for (int i = 0; i < numRows; i++) {
			rows[i] = new StringBuilder();
		}

		// 当前行索引, 初始为0
		int currentRow = 0;
		// 方向标志：-1表示向上，1表示向下
		int direction = 1;

		// 遍历每个字符，放入对应的行
		for (char c : s.toCharArray()) {
			rows[currentRow].append(c);
			// 到达第一行，方向改为向下
			if (currentRow == 0) {
				direction = 1;
			}
			// 到达最后一行，方向改为向上
			else if (currentRow == numRows - 1) {
				direction = -1;
			}
			currentRow += direction;
		}

		// 拼接所有行的字符，得到结果
		StringBuilder result = new StringBuilder();
		for (StringBuilder row : rows) {
			result.append(row);
		}
		return result.toString();
	}
}
