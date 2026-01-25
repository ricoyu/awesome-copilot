package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 快乐数
 * <p>
 * 编写一个算法来判断一个数 n 是不是快乐数。
 * <p>
 * <ul>「快乐数」 定义为:
 *     <li/>对于一个正整数，每一次将该数替换为它每个位置上的数字的平方和。
 *     <li/>然后重复这个过程直到这个数变为 1，也可能是 无限循环 但始终变不到 1。
 *     <li/>如果这个过程 结果为 1，那么这个数就是快乐数。
 *     <li/>如果 n 是 快乐数 就返回 true ；不是，则返回 false 。
 * </ul>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 19
 * 输出：true
 * 解释：
 * 1^2 + 9^2 = 82
 * 8^2 + 2^2 = 68
 * 6^2 + 8^2 = 100
 * 1^2 + 0^2 + 0^2 = 1
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 2
 * 输出：false
 * </pre>
 *
 * <ul>判断快乐数的关键在于检测循环：
 *     <li/>计算数字各位的平方和（核心辅助逻辑）。
 *     <ul>使用「快慢指针法（弗洛伊德循环检测算法）」检测是否存在循环：
 *       <li/>慢指针每次计算 1 次平方和，快指针每次计算 2 次平方和。
 *       <li/>如果是快乐数，快指针会先到达 1；如果存在循环，快慢指针最终会相遇（且值不为 1）
 *       <li/>这种方法无需额外的集合存储历史值，空间复杂度更低（O (1)）。
 *     </ul>
 * </ul>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-24 9:52
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HappyNumberChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.println(isHappy(n));
	}

	private static boolean isHappy(int n) {
		// 使用快慢指针法检测循环
		// 慢指针：每次计算一次平方和
		int slow = n;
		// 快指针：每次计算两次平方和
		int fast = n;
		
		// 当快指针不等于1（即还没找到快乐数）并且快慢指针还未相遇（即还没发现循环）时继续循环
		do {
			slow = calculateSquareSum(slow);        // 慢指针前进一步
			fast = calculateSquareSum(fast);       // 快指针第一步
			fast = calculateSquareSum(fast);       // 快指针第二步，总共前进了两步
			
			// 如果快指针已经到达1，说明是快乐数，直接跳出循环
			if (fast == 1) {
				return true;
			}
		} while (slow != fast);  // 当快慢指针相遇时停止，说明进入了一个循环
		
		// 如果退出循环是因为快慢指针相遇（而不是因为找到1），则不是快乐数
		// 此时快指针等于慢指针，如果它们都等于1则为快乐数，否则不是
		return fast == 1;
	}

	/**
	 * 计算一个数各位数字的平方和
	 * 例如：输入19，返回1*1+9*9=82
	 */
	private static int calculateSquareSum(int num) {
		int sum = 0;
		while (num > 0) {
			int digit = num % 10;    // 获取个位数
			sum += digit * digit;    // 累加个位数的平方
			num /= 10;               // 去掉个位数，获取剩余部分
		}
		return sum;
	}
}
