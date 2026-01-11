package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 杨辉三角
 * <p>
 * 给定一个非负整数 numRows，生成「杨辉三角」的前 numRows 行。
 * <p>
 * 在「杨辉三角」中，每个数是它左上方和右上方的数的和。
 * <p>
 * <pre>
 * 示例 1:
 *
 * 输入: numRows = 5
 * 输出: [[1],[1,1],[1,2,1],[1,3,3,1],[1,4,6,4,1]]
 * </pre>
 * <image src="images/yanghui.gif"/>
 * <pre>
 * 示例 2:
 *
 * 输入: numRows = 1
 * 输出: [[1]]
 * </pre>
 *
 * <ul>核心解题思路
 *     <li/>初始化结构：杨辉三角的每一行都是一个列表，整体用二维列表List<List<Integer>>存储。
 *     <li/>第 0 行（首行）固定为[1]；若 numRows 为 0 则返回空列表，numRows 为 1 则直接返回仅包含首行的列表。
 *     <li/>从第 2 行（索引 1）开始，每行的首尾元素固定为 1，中间元素等于上一行中对应位置 “左上方” 和 “右上方” 元素之和（即上一行的j-1和j位置元素相加）。
 *     <li/>循环填充：通过双层循环，外层控制行数，内层控制每行的元素，逐步填充每一行的数值。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-08 9:09
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PascalTriangleGenerator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入行数: ");
		int numRows = scanner.nextInt();
		List<List<Integer>> result = generate(numRows);
		scanner .close();
	}

	/**
	 * 生成杨辉三角前numRows行的核心方法
	 *
	 * @param numRows 非负整数，指定生成的行数
	 * @return 二维列表，每个子列表代表杨辉三角的一行
	 */
	private static List<List<Integer>> generate(int numRows) {
		// 定义最终返回的二维列表
		List<List<Integer>> pascalTriangle = new ArrayList<>();

		// 边界条件：如果输入行数为0，直接返回空列表
		if (numRows == 0) {
			return pascalTriangle;
		}

		// 初始化第一行（索引0），杨辉三角第一行固定为[1]
		List<Integer> firstRow = new ArrayList<>();
		firstRow.add(1);
		pascalTriangle.add(firstRow);

		// 从第二行开始生成（索引从1到numRows-1）
		for (int i = 1; i < numRows; i++) {
			// 获取上一行的数据
			List<Integer> previousRow = pascalTriangle.get(i - 1);
			// 初始化当前行
			List<Integer> currentRow = new ArrayList<>();

			// 每行的第一个元素固定为1
			currentRow.add(1);
			// 生成中间元素：当前元素 = 上一行的j-1位置 + 上一行的j位置
			// 中间元素的数量为 i-1 个（比如第3行索引2，中间有1个元素；第4行索引3，中间有2个元素）
			for (int j = 1; j < i; j++) {
				int currentElement = previousRow.get(j - 1) + previousRow.get(j);
				currentRow.add(currentElement);
			}

			// 每行的最后一个元素固定为1
			currentRow.add(1);

			// 将当前行添加到杨辉三角列表中
			pascalTriangle.add(currentRow);
		}

		return pascalTriangle;
	}
}
