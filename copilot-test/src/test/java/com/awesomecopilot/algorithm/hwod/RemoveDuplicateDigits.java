package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;
import java.util.Stack;

/**
 * 删除重复数字后的最大数字 - 贪心算法
 * <p>
 * 一、题目描述
 * <p>
 * 给定一个由纯数字组成以字符串表示的数值，现要求字符串中的每个数字最多只能出现 2 次，超过的需要进行删除；
 * <p>
 * 删除某个重复的数字后，其它数字相对位置保持不变。
 * <p>
 * 如"34533"，数字 3 重复超过 2 次，需要删除其中一个 3，删除第一个 3 后获得最大数值 “4533”。
 * <p>
 * 请返回经过删除操作后的最大值，以字符串表示。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为一个纯数字组成的字符串，长度范围：[1, 100000]
 * <p>
 * 三、输出描述
 * <p>
 * 输出经过删除操作后的最大数值
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 34533
 *
 * 2、输出
 * 4533
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 5445795045
 *
 * 2、输出
 * 5479504
 * </pre>
 * <ul>算法思路:
 *     <li/>使用一个计数器数组记录每个数字已经出现的次数
 *     <li/>使用一个栈来构建最终结果
 *     <ul>遍历每个数字时:
 *       <li/>如果当前数字已经在栈中出现过2次，直接跳过
 *       <li/>否则，考虑是否可以移除栈顶较小的数字（前提是后面还有该数字可以补充），以得到更大的数字
 *       <li/>将当前数字压入栈中，并更新计数器
 *     </ul>
 * </ul>
 * <ul>关键点
 *     <li/>维护每个数字的剩余可用次数（总次数减去已使用次数）
 *     <li/>在决定是否移除栈顶元素时，要确保后面还有该数字可以补充
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-25 9:38
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RemoveDuplicateDigits {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字: ");
		String input = scanner.nextLine().trim();

		String result = removeDuplicates(input);
		System.out.println(result);
	}

	public static String removeDuplicates(String nums) {
		// 统计每个数字出现的总次数, 因为就0~9十个数字，所以长度为10
		int[] totalCount = new int[10];
		for (char c : nums.toCharArray()) {
			totalCount[c - '0']++;
		}

		// 记录栈中每个数字的当前计数
		int[] stackCount = new int[10];
		Stack<Character> stack = new Stack<>();

		for (char c : nums.toCharArray()) {
			int digit = c - '0';
			// 如果栈中已经有2个该数字，跳过
			if (stackCount[digit] >= 2) {
				continue;
			}

			// 尝试移除栈顶较小的数字（如果后面还有该数字可以补充）
			/*
			 * <ul>核心逻辑解释
			 *     <li/>stack.peek() - '0'  获取当前栈顶数字对应的整数值（比如栈顶是'3'，得到数字3）
			 *     <li/>stackCount[digit]   表示这个数字已经在栈中出现了多少次
			 *     <li/>totalCount[digit]   表示这个数字在剩余未处理的字符串部分中还有多少个
			 *     <li/>stackCount + totalCount = 这个数字最终可能出现的总次数
			 * </ul>
			 * <ul>如果 stackCount + totalCount >= 2：
			 *     <li/>说明即使现在弹出这个栈顶数字，后面还有足够多的该数字可以补充进来
			 *     <li/>不会导致这个数字最终出现次数不足2次
			 * </ul>
			 */
			while (!stack.isEmpty() &&
					stack.peek() < c &&
					stackCount[stack.peek() - '0'] + totalCount[stack.peek() - '0'] -1>= 2) {
				char top = stack.pop();
				stackCount[top - '0']--;
			}

			// 将当前数字压入栈中
			stack.push(c);
			stackCount[digit]++;
			totalCount[digit]--; // 减少剩余可用计数
		}

		// 将栈中字符转换为字符串
		StringBuilder sb = new StringBuilder();
		for (char c : stack) {
			sb.append(c);
		}

		return sb.toString();
	}
}
