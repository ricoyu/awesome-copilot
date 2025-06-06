package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 字符串划分
 * <p>
 * 一、题目描述 <p>
 * 给定一个小写字母组成的字符串 s，请找出字符串中两个不同位置的字符作为分割点，使得字符串分成三个连续子串且子串权重相等，注意子串不包含分割点。
 * <p>
 * 若能找到满足条件的两个分割点，请输出这两个分割点在字符串中的位置下标，若不能找到满足条件的分割点请返回0,0。
 * <p>
 * 子串权重计算方式为：子串所有字符的ASCII码数值之和。
 * <p>
 * 二、输入描述 <p>
 * 输入为一个字符串，字符串由a~z，26个小写字母组成，5 ≤ 字符串长度 ≤ 200。
 * <p>
 * 三、输出描述 <p>
 * 输出为两个分割点在字符串中的位置下标，以逗号分隔
 * <p>
 * 备注
 * <p>
 * 只考虑唯一解，不存在一个输入多种输出解的情况
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * acdbbbca
 *
 * 2、输出
 * 2,5
 * </pre>
 * <p>
 * 3、说明 <p>
 * 以位置2和5作为分割点，将字符串分割为ac, db, bb, ca三个子串，每一个的子串权重都为196，输出为：2,5
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * abcabc
 *
 * 2、输出
 * 0,0
 * </pre>
 *
 * <ul>定义分割点规则：
 *     <li/>设两个分割点为 i 和 j，需满足 0 < i < j < s.length() - 1
 *     <li/>将字符串分为三段：s[0, i-1]，s[i+1, j-1]，s[j+1, end]
 * </ul>
 * <ul>判断权重相等
 *     <li/>用前缀和数组 prefixSum 快速计算任意子串的ASCII和
 *     <li/>枚举所有 (i, j) 组合，判断三段的ASCII和是否相等
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-04 8:35
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class EqualWeightSplitter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串s: ");
		String s = scanner.nextLine();

		String result = findSplitPoints(s);
		System.out.println(result);
	}

	/**
	 * 寻找两个分割点，使得分成三段的子串权重相等
	 *
	 * @param s
	 * @return 分割点下标，格式为x,y，若无解则返回0,0
	 */
	public static String findSplitPoints(String s) {
		int n = s.length();
		int[] prefixSum = new int[n];

		// 构造前缀和数组：prefixSum[i]表示s[0..i]的ASCII和
		prefixSum[0] = s.charAt(0);
		for (int i = 1; i < n; i++) {
			prefixSum[i] = prefixSum[i - 1] + s.charAt(i);
		}

		// 枚举两个分割点i和j
		for (int i = 1; i < n - 2; i++) {
			for (int j = i + 2; j < n; j++) {
				int sum1 = prefixSum[i - 1]; // 第1段：s[0..i-1]
				int sum2 = prefixSum[j - 1] - prefixSum[i]; // 第2段：s[i+1..j-1]
				int sum3 = prefixSum[n - 1] - prefixSum[j];// 第3段：s[j+1..n-1]

				if (sum1 == sum2 && sum2 == sum3) {
					return i + "," + j;
				}
			}
		}
		return "0,0";
	}
}
