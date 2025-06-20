package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 贪吃的猴子 - 滑动窗口
 * <p>
 * 一、题目描述
 * <p>
 * 一只贪吃的猴子，来到一个果园，发现许多串香蕉排成一行，每串香蕉上有若干根香蕉。每串香蕉的根数由数组numbers给出。猴子获取香蕉，每次都只能从行的开头或者末尾获取，并且只能获取N次，求猴子最多能获取多少根香蕉。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行为数组numbers的长度
 * <p>
 * 第二行为数组numbers的值每个数字通过空格分开
 * <p>
 * 第三行输入为N，表示获取的次数
 * <p>
 * 三、输出描述
 * <p>
 * 按照题目要求能获取的最大数值。
 * <p>
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 4
 * 4 2 2 3
 * 2
 *
 * 2、输出
 * 7
 * </pre>
 *
 * <pre>
 * 测试用例2
 * 8
 * 3 5 9 3 3 4 6 6
 * 4
 *
 * 5、输出
 * 23
 * </pre>
 * <p>
 * 这个问题可以通过滑动窗口技巧来解决。我们需要找到从数组开头取x次和从末尾取N-x次的最大和组合，其中x的范围是0到N。
 * <ul>算法步骤
 *     <li/>首先计算前N个元素的和作为初始最大值
 *     <li/>然后使用滑动窗口方法，每次从窗口左侧移除一个元素，从右侧添加一个元素
 *     <li/>比较并记录所有可能组合中的最大值
 * </ul>
 *
 * <pre>
 * 以测试用例1为例：
 * numbers = [4, 2, 2, 3], N=2
 *
 * 可能的组合：
 *
 * 取左0次，右2次：2+3=5
 *
 * 取左1次，右1次：4+3=7
 *
 * 取左2次，右0次：4+2=6
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-20 9:08
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GreedyMonkey {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组numbers的长度: ");
		int length = scanner.nextInt();
		System.out.print("请输入数组numbers的值: ");
		scanner.nextLine();
		String[] parts = scanner.nextLine().trim().split(" ");
		int[] numbers = new int[length];
		for (int i = 0; i < parts.length; i++) {
			numbers[i] = Integer.parseInt(parts[i]);
		}
		System.out.print("请输入N: ");
		int n = scanner.nextInt();

		System.out.println(getMaxBananas(numbers, n));
	}

	/**
	 * 使用滑动窗口计算猴子能获取的最大香蕉数
	 *
	 * @param numbers 香蕉数组
	 * @param n       可以获取的次数
	 * @return 最大香蕉数
	 */
	public static int getMaxBananas(int[] numbers, int n) {
		int length = numbers.length;
		// 如果N大于等于数组长度，直接返回所有元素的和
		if (n >= length) {
			int sum = Arrays.stream(numbers).sum();
			return sum;
		}

		// 初始窗口：全部从左边取
		int windowSum = 0;
		for (int i = 0; i < n; i++) {
			windowSum += numbers[i];
		}
		int maxSum = windowSum;

		// 滑动窗口：每次从左边移除一个元素，从右边添加一个元素
		int left = n - 1; // 左边窗口的右边界
		int right = length - 1; // 右边窗口的左边界

		for (int i = 0; i < n; i++) {
			windowSum = windowSum - numbers[left] + numbers[right];
			maxSum = Math.max(maxSum, windowSum);
			right--;
			left--;
		}

		return maxSum;
	}
}
