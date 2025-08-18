package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 叠积木1
 * <p>
 * 一、题目描述
 * <p>
 * 有一堆长方体积木，它们的高度和宽度都相同，但长度不一。
 * <p>
 * 小橙想把这堆积木叠成一面墙，墙的每层可以放一个积木，或将两个积木拼接起来，要求每层的长度相同。若必须用完这些积木，叠成的墙最多为多少层?
 * <p>
 * 二、输入描述
 * <p>
 * 输入为一行，为各个积木的长度，数字为正整数，并由空格分隔。积木的数量和长度都不超过5000。
 * <p>
 * 三、输出描述
 * <p>
 * 输出一个数字，为墙的最大层数，如果无法按要求叠成每层长度一致的墙，则输出-1。
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 4 4 4 4
 *
 * 2、输出
 * 4
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 3 3 2
 *
 * 2、输出
 * -1
 * </pre>
 *
 * <ul>核心思路简述：
 *     <li/>最多能叠多少层 = 从 1 到 N（积木数量）中找出最大的层数，使得：
 *     <li/>积木可以被分为这么多组；
 *     <li/>每组和相等；
 *     <li/>每组最多两个积木。
 * </ul>
 * 关键技巧：先计算所有积木的总长度，然后枚举所有可能的层数（从大到小），验证是否可行。
 *
 * <ul>示例解释（举例说明算法逻辑）
 *     <li/>输入：1 2 3 4 5 6
 *     <li/>总和 = 21。其因数包括 1, 3, 7, 21。
 *     <li/>尝试从 6 层开始到 1 层，依次尝试是否能分成这些层数，每层和 = 总和 / 层数。
 *     <li/>例如尝试 3 层：每层和为 7，尝试用最多两个数组合为 7。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-29 7:53
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BlockWallBuilder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入各个积木的长度, 空格隔开: ");
		int[] bricks = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		System.out.println(maxLayers(bricks));
	}

	public static int maxLayers(int[] bricks) {
		int totalLength = Arrays.stream(bricks).sum();
		int n = bricks.length;
		Arrays.sort(bricks); // 先排序，便于剪枝

		// 从最大层数开始尝试（即最多n层）
		for (int layers = n; layers >= 1; layers--) {
			if (totalLength % layers != 0) {
				continue; // 如果不能整除，跳过
			}
			int targetLength = totalLength / layers;

			boolean[] used = new boolean[n]; // 标记积木是否使用
			if (canBuildLayers(bricks, used, layers, 0, 0, targetLength)) {
				return layers;
			}
		}

		return -1; // 没有任何有效的层数组合
	}

	/**
	 * 回溯判断是否能构建指定层数，每层长度为targetLength
	 *
	 * @param bricks  积木长度数组
	 * @param used    每个积木是否已被使用
	 * @param layers  剩余层数
	 * @param start   搜索起始位置
	 * @param currSum 当前这层已累积的长度
	 * @param target  每层的目标长度
	 * @return
	 */
	public static boolean canBuildLayers(int[] bricks, boolean[] used, int layers, int start, int currSum,
	                                     int target) {
		if (layers == 0) {// 所有层都构建完了
			return true;
		}

		// 当前层构建完成，递归构建下一层
		if (currSum == target) {
			return canBuildLayers(bricks, used, layers - 1, 0, 0, target);
		}

		int prev = -1;
		for (int i = start; i < bricks.length; i++) {
			if (used[i] || currSum + bricks[i] > target || bricks[i] == prev) {
				continue;
			}

			used[i] = true;
			if (canBuildLayers(bricks, used, layers, i + 1, currSum + bricks[i], target)) {
				return true;
			}
			used[i] = false;
			prev = bricks[i];

			// 剪枝：如果当前是新一层的第一个积木，且失败了，则不再尝试该层
			if (currSum == 0) {
				break;
			}
		}

		return false;
	}
}
