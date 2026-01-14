package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 杨辉三角 II
 * <p>
 * 给定一个非负索引 rowIndex，返回「杨辉三角」的第 rowIndex 行。 <p>
 * 在「杨辉三角」中，每个数是它左上方和右上方的数的和。
 * <p>
 * <image src="images/yanghui.gif"/>
 * <pre>
 * 示例 1:
 *
 * 输入: rowIndex = 3
 * 输出: [1,3,3,1]
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: rowIndex = 0
 * 输出: [1]
 * </pre>
 *
 * <pre>
 * 示例 3:
 *
 * 输入: rowIndex = 1
 * 输出: [1,1]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-01-11 8:03
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PascalTriangleII {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入行数: ");
		int numRows = scanner.nextInt();
		List<Integer> result = getRow(numRows);
		System.out.println(result);
		scanner .close();
	}

	private static List<Integer> getRow(int rowIndex) {
		// 初始化结果列表，第rowIndex行有rowIndex+1个元素，初始值都为1
		List<Integer> row = new ArrayList<>(rowIndex + 1);
		for (int i = 0; i <= rowIndex; i++) {
			row.add(1);
		}

		// 从第2行开始计算（索引从0开始，i=2对应第3行），因为前两行[1]、[1,1]无需计算
		for (int i = 2; i <= rowIndex; i++) {
			// 从后往前更新，避免覆盖未使用的上一行数据
			// j从i-1开始（倒数第二个元素），到1结束（第一个元素是1，无需更新）
			for (int j = i - 1; j >= 1; j--) {
				// 当前元素 = 上一行的当前元素（未更新前的值） + 上一行前一个元素（未更新前的值）
				row.set(j, row.get(j) + row.get(j - 1));
			}
		}

		return row;
	}
}
