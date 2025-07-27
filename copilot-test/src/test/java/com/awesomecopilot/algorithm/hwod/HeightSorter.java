package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 寻找身高相近的小朋友
 * <p>
 * 一、题目描述
 * <p>
 * 小明今年升学到了小学1年级来到新班级后，发现其他小朋友身高参差不齐，然后就想基于各小朋友和自己的身高差，对他们进行排序，请帮他实现排序。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为正整数h和n，0<h<200为小明的身高，0<n<50为新班级其他小朋友个数；
 * <p>
 * 第二行为n个正整数，h1~hn分别是其他小朋友的身高，取值范围0<hi<200，且n个正整数；各不相同。
 * <p>
 * 三、输出描述 <p>
 * 输出排序结果，各正整数以空格分割和小明身高差绝对值最小的小朋友排在前面，
 * <p>
 * 和小明身高差绝对值最大的小朋友排在后面。
 * <p>
 * 如果两个小朋友和小明身高差一样，则个子较小的小朋友排在前面。
 * <pre>
 * 测试用例1：
 * 1、输入
 * 150 5
 * 140 155 160 145 150
 *
 * 2、输出
 * 150 145 155 140 160
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 120 4
 * 130 110 125 115
 *
 * 2、输出
 * 115 125 110 130
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-20 9:18
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HeightSorter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入小明的身高和新班级其他小朋友个数: ");
		String[] parts = scanner.nextLine().trim().split(" ");
		int xiaomingHeight = Integer.parseInt(parts[0]);
		int n = Integer.valueOf(parts[1]);
		System.out.print("请输入其他小朋友的身高: ");
		int[] heights = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		List<Integer> heightList = Arrays.stream(heights).boxed().collect(Collectors.toList());
		heightList.sort((h1, h2) -> {
			int diff1 = Math.abs(h1 - xiaomingHeight);
			int diff2 = Math.abs(h2 - xiaomingHeight);

			if (diff1 != diff2) {
				return diff1 - diff2;
			} else {
				return h1 - h2;
			}
		});

		for (Integer i : heightList) {
			System.out.print(i + " ");
		}
		System.out.println("");
		scanner.close();
	}
}
