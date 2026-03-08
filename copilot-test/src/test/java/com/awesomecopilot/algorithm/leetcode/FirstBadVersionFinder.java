package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 第一个错误的版本
 * <p>
 * 你是产品经理，目前正在带领一个团队开发新的产品。不幸的是，你的产品的最新版本没有通过质量检测。由于每个版本都是基于之前的版本开发的，所以错误的版本之后的所有版本都是错的。
 * <p>
 * 假设你有 n 个版本 [1, 2, ..., n]，你想找出导致之后所有版本出错的第一个错误的版本。
 * <p>
 * 你可以通过调用 bool isBadVersion(version) 接口来判断版本号 version 是否在单元测试中出错。实现一个函数来查找第一个错误的版本。你应该尽量减少对调用 API 的次数。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：n = 5, bad = 4
 * 输出：4
 * 解释：
 * 调用 isBadVersion(3) -> false
 * 调用 isBadVersion(5) -> true
 * 调用 isBadVersion(4) -> true
 * 所以，4 是第一个错误的版本。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 1, bad = 1
 * 输出：1
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2026-03-01 12:09
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FirstBadVersionFinder {
	
	private static int badVersion;
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.print("请输入数字bad: ");
		int bad = scanner.nextInt();
		badVersion = bad;
		System.out.println(firstBadVersion(n));
		scanner.close();
	}
	
	/**
	 * 查找第一个错误的版本
	 * @param n   版本总数
	 * @param bad 第一个错误的版本号
	 * @return
	 */
	/**
	 * 查找第一个错误的版本
	 * @param n 版本总数
	 * @return 第一个错误的版本号
	 */
	public static int firstBadVersion(int n) {
		// 左边界：初始为第一个版本
		int left = 1;
		// 右边界：初始为最后一个版本
		int right = n;
		
		// 二分查找核心循环：当左边界小于右边界时继续查找
		while (left < right) {
			// 计算中间版本，避免(left + right)溢出（例如n=2^31-1时，left+right会超出int范围）
			int mid = left + (right - left) / 2;
			
			if (isBadVersion(mid)) {
				// 中间版本是错误的：第一个错误版本在[left, mid]区间，调整右边界
				right = mid;
			} else {
				// 中间版本是正确的：第一个错误版本在[mid+1, right]区间，调整左边界
				left = mid + 1;
			}
		}
		
		// 循环结束时left == right，即为第一个错误版本
		return left;
	}
	
	// 模拟判断版本是否错误的接口
	private static boolean isBadVersion(int version) {
		return version >= badVersion;
	}
}