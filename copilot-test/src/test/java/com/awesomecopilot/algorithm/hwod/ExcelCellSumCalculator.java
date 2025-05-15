package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Excel单元格数值统计
 * <p>
 * 一、题目描述
 * <p>
 * Excel工作表中对选定区域的数值进行统计的功能非常实用。仿照Excel的这个功能，请你给定表格中区域中的单元格进行求和统计，并输出统计结果。
 * <p>
 * 为简化计算，假设当前输入中每个单元格内容仅为数字或公式两种。
 * <p>
 * 如果为数字，则是一个非负整数，形如3，77。 <p>
 * 如果为公式，则固定以“=”开头，且包含以下三种情况： <p>
 * <p>
 * 等于某单元格的值，例如=B12； <p>
 * 两个单元格的双目运算（仅为+或-），形如=C1-C2，C3+B2； <p>
 * 单元格和数字的双目运算（仅为+或-），形如=B1+1，100-B2。 <p>
 * <p>
 * 注意：
 * <p>
 * 公式内容都是合法的，例如不存在=，=C+1，=C1-C2+B3=5，=3+5； <p>
 * 不存在循环引用，例如A1=B1+C1，C1=A1+B2； <p>
 * 内容中不存在空格、括号。 <p>
 * <p>
 * 二、输入描述
 * <p>
 * 第一行两个整数rows、cols，表示给定表格区域的行数和列数，1<=rows<=20，1<=cols<=26。 <p>
 * 接下来rows行，每行cols个以空格分隔的字符串values，表示表格values的单元格内容。 <p>
 * 最后一行给出一个字符串，表示给定的统计区域，形如A1 <p>
 * <p>
 * 三、输出描述
 * <p>
 * 一个整数，表示给定选定区域中单元格数字的累加总和，范围为-2,147,483,648 ~ 2,147,483,647。
 * <p>
 * 四、测试用例
 * <pre>
 * 1、输入
 * 1 3
 * 1 =A1+C1 3
 * A1:C1
 *
 * 2、输出
 * 8
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-13 11:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ExcelCellSumCalculator {

	static int rows, cols;
	static String[][] cells; // 原始内容（数字或公式）
	static Map<String, Integer> cache = new HashMap<>(); // 记忆化缓存避免重复计算

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入行数: ");
		rows = scanner.nextInt();
		System.out.print("请输入列数: ");
		cols = scanner.nextInt();
		cells = new String[rows][cols];
		System.out.println("请输入表格内容: ");
		scanner.nextLine();
		for (int i = 0; i < rows; i++) {
			System.out.print("请输入第" + (i + 1) + "行数据: ");
			String[] values = scanner.nextLine().trim().split(" ");
			for (int j = 0; j < cols; j++) {
				cells[i][j] = values[j];
			}
		}

		System.out.print("请输入统计区域: ");
		String region = scanner.nextLine();

		System.out.println(computeSum(region));
	}

	/**
	 * 计算区域内所有单元格值的总和
	 *
	 * @param region
	 * @return int
	 */
	private static int computeSum(String region) {
		int sum = 0;

		// 支持单格和区域两种形式
		String[] parts = region.split(":");
		int[] start = parseCell(parts[0]);
		int[] end = parts.length == 2 ? parseCell(parts[1]) : start;

		for (int r = start[0]; r <= end[0]; r++) {
			for (int c = start[1]; c <= end[1]; c++) {
				String cellName = getCellName(r, c);
				sum += eval(cellName);
			}
		}
		return sum;
	}

	/**
	 * 计算单元格值（数字或公式）
	 *
	 * @param cellName
	 * @return
	 */
	private static int eval(String cellName) {
		if (cache.containsKey(cellName)) {
			return cache.get(cellName);
		}

		int[] pos = parseCell(cellName);
		String content = cells[pos[0]][pos[1]];

		int result = 0;
		if (!content.startsWith("=")) {
			result = Integer.parseInt(content);
		} else {
			// 是公式，去掉等号
			String expr = content.substring(1);

			// 处理 + 和 - 运算
			if (expr.contains("+")) {
				String[] parts = expr.split("\\+");
				result = evalOperand(parts[0]) + evalOperand(parts[1]);
			} else if (expr.contains("-")) {
				String[] parts = expr.split("-");
				result = evalOperand(parts[0]) - evalOperand(parts[1]);
			} else {
				result = evalOperand(expr);
			}
		}

		cache.put(cellName, result);
		return result;
	}

	private static int evalOperand(String op) {
		if (Character.isLetter(op.charAt(0))) {
			return eval(op);  // 是单元格
		} else {
			return Integer.parseInt(op);  // 是数字
		}
	}

	/**
	 * 把单元格名（如 A1）转成行列坐标
	 *
	 * @param name
	 * @return
	 */
	private static int[] parseCell(String name) {
		int col = name.charAt(0) - 'A';
		int row = Integer.parseInt(name.substring(1)) - 1;
		return new int[]{row, col};
	}

	private static String getCellName(int row, int col) {
		return (char) ('A' + col) + Integer.toString(row);
	}
}
