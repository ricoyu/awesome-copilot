package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * 三阶积幻方
 * <p>
 * 一、题目描述 <p>
 * 九宫格是一款广为流传的游戏，起源于河图洛书。
 * <p>
 * 游戏规则 Q 是：1到9的九个数字放在3×3的格子中，要求每行、每列以及两个对角线上的三数之和都等于15。
 * <p>
 * 在金庸名著《射雕英雄传》中黄蓉曾给 九宫格 Q 的一种解法，口诀：戴九履一，左三右七，二四肩背，八六为足，五居中央。解法如图所示。
 * <p>
 * 现在有一种新的玩法，给九个不同的数字，将这九个数字放在3×3的格子中，要求每行、每列以及两个对角线上的三数之积相等（三阶积幻方）。其中一个三阶幻方如图：
 * <pre>
 * 2   9   12
 * 36  6   1
 * 3   4   18
 * </pre>
 * 解释：每行、每列以及两个对角线上的三数之积相等，都为216。请设计一种算法，将给定的九个数宇重新排列后，使其满足三阶积幻方的要求。
 * 排列后的九个数宇中：第1-3个数字为方格的第一行，第4-6个数宇为方格的第二行，第7-9个数字为方格的第三行。
 * <p>
 * 二、输入描述 <p>
 * 九个不同的数字，每个数字之间用空格分开。
 * <p>
 * 0 < 数字 < 107。
 * <p>
 * 三、输出描述 <p>
 * 九个数字所有满足要求的排列，每个数字之间用空格分开。
 * <p>
 * 每行输出一个满足要求的排列。
 * <p>
 * 要求输出的排列升序排列，即：对于排列 A(A1,A2,A3…A9) 和排列 B(B1,B2,B3…B9)，从排列的第 1 个数字开始，遇到 Ai < Bi，则排列 A < 排列 B (1 <= i <= 9)。
 * <p>
 * 说明：用例保证至少有一种排列组合满足条件。
 * <pre>
 * 测试用例1：
 * 1、输入
 * 75 36 10 4 30 225 90 25 12
 *
 * 2、输出
 * 10 12 225 36 25 30 75 90 4
 * 10 12 225 75 90 4 36 25 30
 * 10 225 12 36 30 25 75 4 90
 * 10 225 12 75 4 90 36 30 25
 * 10 36 75 12 25 90 225 30 4
 * 10 36 75 225 30 4 12 25 90
 * 10 75 36 12 90 25 225 4 30
 * 10 75 36 225 4 30 12 90 25
 * 12 10 225 25 36 30 90 75 4
 * 12 10 225 90 75 4 25 36 30
 * 12 225 10 25 30 36 90 4 75
 * 12 225 10 90 4 75 25 30 36
 * 12 25 90 10 36 75 225 30 4
 * 12 25 90 225 30 4 10 36 75
 * 12 90 25 10 75 36 225 4 30
 * 12 90 25 225 4 30 10 75 36
 * 225 10 12 30 36 25 4 75 90
 * 225 10 12 4 75 90 30 36 25
 * 225 12 10 30 25 36 4 90 75
 * 225 12 10 4 90 75 30 25 36
 * 225 30 4 10 36 75 12 25 90
 * 225 30 4 12 25 90 10 36 75
 * 225 4 30 10 75 36 12 90 25
 * 225 4 30 12 90 25 10 75 36
 * 25 12 90 30 225 4 36 10 75
 * 25 12 90 36 10 75 30 225 4
 * 25 30 36 12 225 10 90 4 75
 * 25 30 36 90 4 75 12 225 10
 * 25 36 30 12 10 225 90 75 4
 * 25 36 30 90 75 4 12 10 225
 * 25 90 12 30 4 225 36 75 10
 * 25 90 12 36 75 10 30 4 225
 * 30 225 4 25 12 90 36 10 75
 * 30 225 4 36 10 75 25 12 90
 * 30 25 36 225 12 10 4 90 75
 * 30 25 36 4 90 75 225 12 10
 * 30 36 25 225 10 12 4 75 90
 * 30 36 25 4 75 90 225 10 12
 * 30 4 225 25 90 12 36 75 10
 * 30 4 225 36 75 10 25 90 12
 * 36 10 75 25 12 90 30 225 4
 * 36 10 75 30 225 4 25 12 90
 * 36 25 30 10 12 225 75 90 4
 * 36 25 30 75 90 4 10 12 225
 * 36 30 25 10 225 12 75 4 90
 * 36 30 25 75 4 90 10 225 12
 * 36 75 10 25 90 12 30 4 225
 * 36 75 10 30 4 225 25 90 12
 * 4 225 30 75 10 36 90 12 25
 * 4 225 30 90 12 25 75 10 36
 * 4 30 225 75 36 10 90 25 12
 * 4 30 225 90 25 12 75 36 10
 * 4 75 90 225 10 12 30 36 25
 * 4 75 90 30 36 25 225 10 12
 * 4 90 75 225 12 10 30 25 36
 * 4 90 75 30 25 36 225 12 10
 * 75 10 36 4 225 30 90 12 25
 * 75 10 36 90 12 25 4 225 30
 * 75 36 10 4 30 225 90 25 12
 * 75 36 10 90 25 12 4 30 225
 * 75 4 90 10 225 12 36 30 25
 * 75 4 90 36 30 25 10 225 12
 * 75 90 4 10 12 225 36 25 30
 * 75 90 4 36 25 30 10 12 225
 * 90 12 25 4 225 30 75 10 36
 * 90 12 25 75 10 36 4 225 30
 * 90 25 12 4 30 225 75 36 10
 * 90 25 12 75 36 10 4 30 225
 * 90 4 75 12 225 10 25 30 36
 * 90 4 75 25 30 36 12 225 10
 * 90 75 4 12 10 225 25 36 30
 * 90 75 4 25 36 30 12 10 225
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-29 8:54
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MagicProductSquare {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		int[] nums = new int[9];
		System.out.print("请输入9个不同的数字, 空格隔开: ");
		String[] parts = scanner.nextLine().trim().split(" ");
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Integer.parseInt(parts[i]);
		}
		scanner.close();

		List<String> solutions = new ArrayList<String>();
		// 使用全排列方法生成所有可能排列
		permute(nums, 0, solutions);
		// 排序所有解
		Collections.sort(solutions);
		for (String solution : solutions) {
			System.out.println(solution);
		}
	}

	private static void permute(int[] nums, int start, List<String> solutions) {
		if (start == nums.length) {
			if (isMagicSquare(nums)) {
				solutions.add(arrayToString(nums));
			}
			return;
		}
		for (int i = start; i < nums.length; i++) {
		    swap(nums, start, i); //生成新的排列起点。
			permute(nums, start + 1, solutions);
			swap(nums, start, i); //恢复数组状态，确保后续排列生成正确。
		}

	}

	private static void swap(int[] nums, int i, int j) {
		int temp = nums[i];
		nums[i] = nums[j];
		nums[j] = temp;
	}

	private static String arrayToString(int[] nums) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nums.length; i++) {
			sb.append(nums[i]);
			if (i < nums.length - 1) sb.append(" ");
		}
		return sb.toString();
	}

	private static boolean isMagicSquare(int[] nums) {
		// 计算第一行的乘积
		int p = nums[0] *nums[1]* nums[2];
		// 检查其他行
		if (nums[3] * nums[4] *nums[5] != p) {
			return false;
		}
		if (nums[6] * nums[7] *nums[8] != p) {
			return false;
		}
		// 检查列
		if (nums[0] * nums[3] *nums[6] != p) return false;
		if (nums[1] * nums[4] *nums[7] != p) return false;
		if (nums[2] * nums[5] *nums[8] != p) return false;

		if(nums[0] * nums[4] * nums[8] !=p) return false; // 主对角线
		if(nums[2] * nums[4] * nums[6] !=p) return false; // 副对角线
		return true;
	}
}