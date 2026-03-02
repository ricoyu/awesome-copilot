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
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数字n: ");
		int n = scanner.nextInt();
		System.out.print("请输入数字bad: ");
		int bad = scanner.nextInt();
		System.out.println(firstBadVersion(n, bad));
		scanner.close();
	}
	
	/**
	 * 查找第一个错误的版本
	 * @param n   版本总数
	 * @param bad 第一个错误的版本号
	 * @return
	 */
	private static int firstBadVersion(int n, int bad) {
		// 左边界：初始为第一个版本
		int left = 1;
		// 右边界：初始为最后一个版本
		int right = n;
		
		// 二分查找核心循环：当左边界小于右边界时继续查找
		while (left < right) {
			// 中间位置：取左边界和右边界中间位置
			int mid = left + (right - left) / 2;
			
			// 判断中间位置的版本是否为错误版本
			if (isBadVersion(mid, bad)) {
				// 中间版本是错误的：第一个错误版本在[left, mid]区间，调整右边界
				right = mid;
			}else {
				// 中间版本是正确的：第一个错误版本在[mid+1, right]区间，调整左边界
				left = mid + 1;
			}
		}
		// 循环结束时left == right，即为第一个错误版本
		return left;
	}
	
	private static boolean isBadVersion(int version, int badVersion) {
		return version >= badVersion;
	}
}