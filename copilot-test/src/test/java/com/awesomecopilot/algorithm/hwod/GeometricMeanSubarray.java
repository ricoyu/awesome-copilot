package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 几何平均值最大子数
 * <p>
 * 一、题目描述 <p>
 * 从一个长度为N的正整数数组numbers中找出长度至少为L且几何平均值最大的子数组，并输出其位置和大小。（K个数的几何平均值为K个数的乘积的K次方根）
 * <p>
 * 若有多个子数组的几何平均值均为最大值，则输出长度最小的子数组。
 * <p>
 * 若有多个长度最小的子数组的几何平均值均为最大值，则输出最前面的子数组。
 * <p>
 * 二、输入描述 <p>
 * 第1行输入N、L：
 * <p>
 * N表示numbers的大小（1 <= N <= 100000）
 * <p>
 * L表示子数组的最小长度（1 <= L <= N）
 * <p>
 * 之后N行表示numbers中的N个数，每行一个（1 <= numbers[i] <= 10^9）
 * <p>
 * 三、输出描述 <p>
 * 输出子数组的位置（从0开始计数）和大小，中间用一个空格隔开。
 * <p>
 * 备注 <p>
 * 用例保证除几何平均值为最大值的子数组外，其他子数组的几何平均值至少比最大值小10^-10倍。
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * 3 2
 * 2
 * 2
 * 3
 *
 * 2、输出
 * 1 2
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 10 2
 * 0.2
 * 0.2
 * 0.1
 * 0.2
 * 0.2
 * 0.2
 * 0.1
 * 0.2
 * 0.2
 * 0.2
 *
 * 2、输出
 * 0 2
 * </pre>
 *
 * 3、说明
 * <pre>
 * 所有长度为2的子数组及其几何平均值：
 *
 * 索引0-1: [0.2, 0.2]，GM = 0.2
 * 索引1-2: [0.2, 0.1]，GM ≈ 0.141
 * 索引2-3: [0.1, 0.2]，GM ≈ 0.141
 * 索引3-4: [0.2, 0.2]，GM = 0.2
 * 索引4-5: [0.2, 0.2]，GM = 0.2
 * 索引5-6: [0.2, 0.1]，GM ≈ 0.141
 * 索引6-7: [0.1, 0.2]，GM ≈ 0.141
 * 索引7-8: [0.2, 0.2]，GM = 0.2
 * 索引8-9: [0.2, 0.2]，GM = 0.2
 * </pre>
 *
 * 最大几何平均值为0.2，对应多个子数组。选择最早的子数组，即从索引0开始的子数组，因此输出0 2。
 *
 * <ul>核心算法步骤
 *     <li/>预处理：对数组中的每个元素取自然对数，得到一个新的数组logNumbers。
 *     <li/>二分搜索：在可能的最小和最大几何平均值之间进行二分搜索。对于每个中间值mid，检查是否存在一个长度至少为L的子数组，其算术平均值（对数值的）大于等于mid。
 *     <li/>检查条件：对于每个mid，计算logNumbers中每个元素减去mid后的前缀和。然后使用滑动窗口技术找到是否存在一个子数组，其前缀和之差非负。
 *     <li/>更新结果：如果找到满足条件的子数组，则更新二分搜索的下界；否则更新上界。
 * </ul>
 *
 * <p/>
 * Copyright: Copyright (c) 2025-04-29 9:56
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GeometricMeanSubarray {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入N L: ");
		String input = scanner.nextLine().trim();
		String[] arr = input.split(" ");
		int N = Integer.parseInt(arr[0]);
		int L = Integer.parseInt(arr[1]);

		double[] numbers = new double[N];
		scanner.nextLine();
		for (int i = 0; i <= N; i++) {
			System.out.print("请输入第 " + i + " 个数：");
		    numbers[i] = scanner.nextDouble();
		}

		// 初始化二分搜索的左右边界
		double left = 0;
		double right = 1;

		for (double num : numbers) {
			if (num > right) {
				right = num;
			}
		}
		double[] logNumbers = new double[N];
		for (int i = 0; i < N; i++) {
			logNumbers[i] = Math.log(numbers[i]);
		}
	}
}
