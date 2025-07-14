package com.awesomecopilot.common.lang.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 最短木板长度
 * <p>
 * 一、题目描述
 * <p>
 * 小明有 n 块木板，第 i ( 1 ≤ i ≤ n ) 块木板长度为 ai。 小明买了一块长度为 m 的木料，这块木料可以切割成任意块，拼接到已有的木板上，用来加长木板。 小明想让最短的木板尽量长。 请问小明加长木板后，最短木板的长度可以为多少？
 * <p>
 * 二、输入描述
 * <p>
 * 输入的第一行包含两个正整数，n(1≤n≤103)、m(1≤m≤106)；
 * <p>
 * n表示木板数，m表示木板长度。输入的第二行包含n个正整数，a1,a2,…an(1≤ai≤106)。
 * <p>
 * 三、输出描述
 * <p>
 * 输出的唯一一行包含一个正整数，表示加长木板后，最短木板的长度最大可以为多少？
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 1 10
 * 5
 *
 * 2、输出
 * 15
 *
 * 3、说明
 * 只有一块木板，初始长度 5，加上所有 10 的木料可达到 15。
 * </pre>
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 4 8
 * 4 4 4 4
 *
 * 2、输出
 * 6
 *
 * 3、说明
 * 所有木板初始均为 4，要达到 6 需要每块补 2，总共需要 8 恰好等于 m；若要求 7，则需要 4×3=12，超过 m。
 * 注意不能直接把长度8的木料加到第一块长度为4的木料上, 这样最短的木料还是4, 所以得均匀拼接
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-13 7:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class MaxMinBoardLength {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n和m: ");
		int[] arr = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
		int n = arr[0];
		int m = arr[1];

		System.out.print("请输入木板长度数组: ");
		int[] boards = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();

		// 二分查找答案：最短木板最大可能长度
		int maxLength = Arrays.stream(boards).max().orElseThrow();
		int left = 0, right = maxLength+m;// 上限可以是最大木板长度 + 所有木料长度
		int answer = 0;
		while(left <= right) {
			int mid = left + (right - left)/2;
			// 判断是否可以把所有木板补到至少 mid 长
			if (canAchieve(boards, m, mid)) {
				answer = mid; // 可以达到，更新答案
				left = mid +1; // 尝试更大的最小长度
			}else {
				right = mid-1; // 尝试更小的最大长度
			}
		}


		System.out.println(answer);
	}

	/**
	 * 判断是否可以通过最多 m 的木料，使所有木板长度 ≥ target
	 * @param boards
	 * @param m
	 * @param target
	 * @return
	 */
	private static boolean canAchieve(int[] boards, int m, int target) {
		long required = 0;  // 记录总共需要补多少木料
		for (int length : boards) {
			if (length < target) {
				required += (target - length);// 补足到目标长度所需木料
			}
		}
		return required <= m; // 如果总需求在可用范围内，则可行
	}
}
