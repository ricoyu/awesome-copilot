package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 字符串序列判定 - 双指针
 * <p>
 * 一、题目描述 <p>
 * 输入两个字符串a和b，都只包含英文小写字母。a长度<=100，b长度<=500,000。
 * <p>
 * 判定a是否是b的有效子串。
 * <p>
 * 判定规则：
 * <p>
 * a中的每个字符在b中都能找到（可以不连续），且a在b中字符的前后顺序与a中顺序要保持一致。
 * <p>
 * （例如，a=”qwt”是b=”qwerty”的一个子序列且有效字符是q、w、t）。
 * <p>
 * 二、输入描述 <p>
 * 输入两个字符串a和b，都只包含英文小写字母。a长度<=100，b长度<=500,000。 <p>
 * 先输入a，再输入b，每个字符串占一行。
 * <p>
 * 三、输出描述 <p>
 * a串最后一个有效字符在b中的位置。（首位从0开始计算，无有效字符返回-1）
 *
 * <pre>
 * 测试用例1
 * 1、输入
 * abc
 * xyzabc
 *
 * 2、输出
 * 5
 * </pre>
 *
 * <pre>
 * 测试用例2
 * 1、输入
 * xyz
 * zyxwxyz
 *
 * 2、输出
 * 6
 * </pre>
 * <ul>使用双指针法：一个指针 i 遍历字符串 a，另一个指针 j 遍历字符串 b。
 *     <li/>如果 a[i] == b[j], 则 i 和 j 都向后移动, 表示匹配到一个有效字符。
 *     <li/>如果 a[i] != b[j]，则 j 向后移动, 继续在 b 中寻找匹配的字符。
 *     <li/>如果 i 遍历完 a，说明 a 是 b 的有效子串，此时 j-1 就是 a 最后一个有效字符在 b 中的位置。
 *     <li/>如果 j 遍历完 b 但 i 未遍历完 a，说明匹配失败，返回 -1。
 * </ul>
 * <pre>
 * 测试用例1：
 * a = "abc", b = "xyzabc"
 * i=0（'a'），j=0（'x'）→ 不匹配，j++（j=1）
 * i=0（'a'），j=1（'y'）→ 不匹配，j++（j=2）
 * i=0（'a'），j=2（'z'）→ 不匹配，j++（j=3）
 * i=0（'a'），j=3（'a'）→ 匹配，i++（i=1），j++（j=4）
 * i=1（'b'），j=4（'b'）→ 匹配，i++（i=2），j++（j=5）
 * i=2（'c'），j=5（'c'）→ 匹配，i++（i=3，遍历完 a），返回 j-1=5。
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-17 9:16
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SubsequenceChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串a, 输入exit退出: ");
		String a = scanner.nextLine().trim();
		System.out.print("请输入字符串b, 输入exit退出: ");
		String b = scanner.nextLine().trim();
		System.out.println(findLastValidIndex(a, b));
	}

	public static int findLastValidIndex(String a, String b) {
		int i = 0; // 指向a的指针
		int j = 0; // 指向b的指针
		int lastValidIndex = -1; // 记录最后一个有效字符的位置

		while (i < a.length() && j < b.length()) {
			if (a.charAt(i) == b.charAt(j)) {
				lastValidIndex = j;
				i++; // 匹配成功，移动a的指针
			}
			j++; // 无论是否匹配，都要移动b的指针
		}

		// 如果a的所有字符都匹配成功，返回最后一个有效位置，否则返回-1
		return (i == a.length()) ? lastValidIndex : -1;
	}
}
