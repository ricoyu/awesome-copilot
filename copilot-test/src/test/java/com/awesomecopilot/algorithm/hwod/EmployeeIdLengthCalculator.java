package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 工号不够用了怎么办
 * <p>
 * 一、题目描述
 * <p>
 * 3020年，空间通信集团的员工人数突破20亿人，即将遇到现有工号不够用的窘境。
 * <p>
 * 现在，请你负责调研新工号系统。
 * <p>
 * 继承历史传统，新的工号系统由小写英文字母(a-z)和数字(0-9)两部分构成。新工号由一段英文字母开头，之后跟随一段数字。
 * <p>
 * 比如”aaahw0001"、”a12345"、“abcd1”、”a00"。
 * <p>
 * 注意新工号不能全为字母或者数字，允许数字部分有前导0或者全为0。
 * <p>
 * 但是过长的工号会增加同事们的记忆成本，现在给出新工号至少需要分配的人数X和新工号中字母的长度Y，求新工号中数字的最短长度Z。
 * <p>
 * 二、输入描述 <p>
 * 一行两个非负整数XY，数字用单个空格分隔
 * <p>
 * 0<X<=250-1
 * <p>
 * 0<Y<=5
 * <p>
 * 三、输出描述
 * <p>
 * 输出新工号中数字的最短长度Z。
 *
 * <pre>
 * 示例1
 * 1、输入
 * 260 1
 *
 * 2、输出
 * 1
 * </pre>
 *
 * <ul>核心思路：
 *     <li/>字母部分可以构成 26^Y 种组合；
 *     <li/>数字部分可以构成 10^Z 种组合；
 *     <li/>总组合数为 26^Y * 10^Z；
 *     <li/>我们要找到最小的 Z 使得组合总数 ≥ X
 * </ul>
 * 换句话说，我们要找最小的 Z，使得：26^Y * 10^Z >= X。
 *
 * <ul>示例说明
 *     <li/>假设：X = 260（需要260个工号）
 *     <li/>Y = 1（字母部分长度为1）→ 可组成 26^1 = 26 个字母组合
 *     <li/>则需要找到最小的 Z 使得 26 * 10^Z >= 260
 *     <li/>尝试：Z=0：26 * 1 = 26 → 不够
 *     <li/>Z=1：26 * 10 = 260 → 刚好够 ✅
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-18 9:00
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class EmployeeIdLengthCalculator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("至少需要分配的人数X和新工号中字母的长度Y: ");
		int[] nums = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
		int x = nums[0];
		int y = nums[1];
		scanner.close();

		// 计算字母部分最多能组合多少种前缀
		long letterCombos = (long)Math.pow(26, y);

		// 初始化数字部分长度 z
		int z = 0;

		// 计算总工号数直到满足要求
		while (letterCombos * Math.pow(10, z) < x) {
			z++;
		}

		System.out.println(z);
	}
}
