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
		scanner .close();
	}

	private static List<Integer> getRow(int numRows) {
		// 初始化结果列表，第rowIndex行有rowIndex+1个元素，初始值都为1
		List<List<Integer>> pascalTriangle = new ArrayList<>();

		// 边界条件：如果输入行数为0，直接返回空列表
		if (numRows == 0) {
			return new ArrayList<>();
		}

		// 初始化第一行（索引0），杨辉三角第一行固定为[1]
		List<Integer> firstRow = new ArrayList<>();
		firstRow.add(1);
		pascalTriangle.add(firstRow);

		// 从第二行开始生成（索引从1到numRows-1）
		for (int i = 1; i < numRows; i++) {
		    List<Integer> prevRow = pascalTriangle.get(i - 1);
			List<Integer> currentRow = new ArrayList<>();
			currentRow.add(1);
			for (int j = 1; j < i; j++) {
				int element = prevRow.get(j - 1) + prevRow.get(j);
				currentRow.add(element);
			}
			// 每行的最后一个元素固定为1
			currentRow.add(1);

			// 将当前行添加到杨辉三角列表中
			pascalTriangle.add(currentRow);
		}

		return pascalTriangle.get(numRows - 1);
	}
}
