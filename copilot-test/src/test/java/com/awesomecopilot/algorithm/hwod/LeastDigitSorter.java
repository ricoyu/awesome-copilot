package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * 整型数组按个位值排序 <p>
 * 题目描述
 * <p>
 * 给定一个非空数组(列表)，其元素数据类型为整型，请按照数组元素十进制最低位从小到大进行排序，十进制最低位相同的元素，相对位置保持不变。
 * <p>
 * 当数组元素为负值时，十进制最低位等同于去除符号位后对应十进制值最低位。
 * <p>
 * 二、输入描述
 * <p>
 * 给定一个非空数组，其元素数据类型为32位有符号整数，数组长度[1,1000]
 * <p>
 * 三、输出描述
 * <p>
 * 输出排序后的数组
 * <pre>
 * 1、输入
 * 1,2,5,-21,22,11,55,-101,42,8,7,32
 *
 * 2、输出
 * 1,-21,11,-101,2,22,42,32,5,55,7,8
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-11 11:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LeastDigitSorter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入数组, 数字之间逗号隔开:");
		String line = scanner.nextLine();
		String[] parts = line.split(",");
		int[] array = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			array[i] = Integer.parseInt(parts[i].trim());
		}

		List<Integer> nums = Arrays.stream(array).boxed().collect(Collectors.toList());
		nums.sort(Comparator.comparingInt(num -> Math.abs(num % 10)));

		String result = nums.stream().map(String::valueOf).collect(joining(",", "[", "]"));
		System.out.println(result);
	}

}
