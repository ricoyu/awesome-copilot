package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 阿里巴巴找黄金宝箱
 * <p>
 * 一、题目描述
 * <p>
 * 一贫如洗的樵夫阿里巴巴在去砍柴的路上，无意中发现了强盗集团的藏宝地，藏宝地有编号从0~N的箱子，每个箱子上面贴有一个数字，箱子中可能有一个黄金宝箱。
 * <p>
 * 黄金宝箱满足排在它之前的所有箱子数字和等于排在它之后的所有箱子数字和；第一个箱子左边部分的数字和定义为0；最后一个宝箱右边部分的数字和定义为0。
 * <p>
 * 请帮阿里巴巴找到黄金宝箱，输出第一个满足条件的黄金宝箱编号，如果不存在黄金宝箱，请返回-1。
 * <p>
 * 二、输入描述
 * <p>
 * 箱子上贴的数字列表，使用逗号分隔，例如1，-1，0。
 * <p>
 * 宝箱的数量不小于1个，不超过10000
 * <p>
 * 宝箱上贴的数值范围不低于-1000，不超过1000
 * <p>
 * 三、输出描述 <p>
 * 第一个黄金宝箱的编号。
 *
 * <pre>
 * 1、输入
 * 1,5,9,10,10,2,3
 *
 * 2、输出
 * 3
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-16 9:26
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class GoldenTreasureBoxFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入箱子上贴的数字列表，使用逗号分隔: ");
		int[] boxes = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();

		System.out.println(findGoldenBox(boxes));
	}

	public static int findGoldenBox(int[] boxes) {
		int totalSum = Arrays.stream(boxes).sum(); // 先求整个数组的总和

		int leftSum = 0; // 初始化左边部分的和为0
		for (int i = 0; i < boxes.length; i++) {
			// 当前下标i的右边部分和 = 总和 - 左边部分和 - 当前元素
			int rightSum = totalSum - boxes[i] - leftSum;

			// 如果左右相等，当前下标就是黄金宝箱编号
			if (leftSum == rightSum) {
				return i;
			}

			// 更新左边部分和
			leftSum +=boxes[i];
		}
		// 遍历完都没有找到
		return -1;
	}
}
