package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * 分班问题
 * <p>
 * 一、题目描述
 * <p>
 * 幼儿园两个班的小朋友在排队时混在了一起，每位小朋友都知道自己是否与前面一位小朋友同班，请你帮忙把同班的小朋友找出来。 <br/>
 * 小朋友的编号是整数，与前一位小朋友同班用Y表示，不同班用N表示。 <br/>
 * 学生序号范围[0,999]，如果输入不合法则打印ERROR。 <br/>
 * <p>
 * 二、输入描述 <p>
 * 输入为空格分开的朋友编号和是否同班标志。
 * <p>
 * 三、输出描述
 * <p>
 * 输出为两行，每一行记录一个班小朋友的编号，编号用空格分开，且：
 * <p>
 * 编号需要按升序排列。
 * 若只有一个班的小朋友，第二行为空行。
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 1/N 2/Y 3/N 4/Y
 *
 * 2、输出
 * 1 2
 * 3 4
 * </pre>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 1/N 2/Y 3/N 4/Y 5/Y
 *
 * 2、输出
 * 1 2
 * 3 4 5
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-28 8:44
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ClassDivider {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入班级小朋友的编号和是否同班: ");
		String input = scanner.nextLine().trim();

		if (input.isEmpty()) {
			System.out.println("ERROR");
			return;
		}

		String[] parts = input.split(" ");
		List<Integer> class1 = new ArrayList<>();
		List<Integer> class2 = new ArrayList<>();

		// 当前班级标记，true 表示 class1，false 表示 class2
		boolean currentClass = true;

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			String[] tokens = part.split("/");

			// 输入格式错误校验
			if (tokens.length != 2 ||
					(!tokens[1].equalsIgnoreCase("Y") && !tokens[1].equalsIgnoreCase("N"))) {
				System.out.println("ERROR");
				return;
			}

			int studentId;
			try {
				studentId = Integer.parseInt(tokens[0]);
				if (studentId < 0 || studentId > 999) {
					System.out.println("ERROR");
					return;
				}
			} catch (NumberFormatException e) {
				System.out.println("ERROR");
				return;
			}

			// 第一个学生，默认分到 class1
			if (i == 0) {
				class1.add(studentId);
			} else {
				// 如果是不同班，则切换当前班级
				String flag = tokens[1];
				// 如果是不同班，则切换当前班级
				if (flag.equalsIgnoreCase("N")) {
					currentClass = !currentClass;
				}
				if (currentClass) {
					class1.add(studentId);
				} else {
					class2.add(studentId);
				}
			}
		}

		Collections.sort(class1);
		Collections.sort(class2);

		// 输出
		System.out.println(listToString(class1));
		System.out.println(listToString(class2));
	}

	/**
	 * 将列表转为用空格分隔的字符串
	 */
	private static String listToString(List<Integer> list) {
		StringBuilder sb = new StringBuilder();
		for (int num : list) {
			sb.append(num).append(" ");
		}
		return sb.toString().trim();
	}
}
