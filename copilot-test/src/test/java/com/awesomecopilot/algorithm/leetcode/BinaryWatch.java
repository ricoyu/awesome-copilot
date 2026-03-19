package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 二进制手表
 * <p>
 * 二进制手表顶部有 4 个 LED 代表 小时（0-11），底部的 6 个 LED 代表 分钟（0-59）。每个 LED 代表一个 0 或 1，最低位在右侧。
 * <p>
 * 例如，下面的二进制手表读取 "4:51" 。
 * <image src="images/binarywatch.png"/>
 * <p>
 * 给你一个整数 turnedOn ，表示当前亮着的 LED 的数量，返回二进制手表可以表示的所有可能时间。你可以 按任意顺序 返回答案。
 * <p>
 * 小时不会以零开头：
 * <p>
 * 例如，"01:00" 是无效的时间，正确的写法应该是 "1:00" 。
 * <p>
 * 分钟必须由两位数组成，可能会以零开头：
 * <p>
 * 例如，"10:2" 是无效的时间，正确的写法应该是 "10:02" 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：turnedOn = 1
 * 输出：["0:01","0:02","0:04","0:08","0:16","0:32","1:00","2:00","4:00","8:00"]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：turnedOn = 9
 * 输出：[]
 * </pre>
 *
 * <ul>核心思路是枚举所有合法时间 + 统计二进制中 1 的个数：
 *     <li/>哪个位置上的灯亮就是1
 *     <li/>枚举小时（0-11）和分钟（0-59）的所有组合；
 *     <li/>对每个组合，计算小时的二进制中 1 的个数 + 分钟的二进制中 1 的个数；
 *     <li/>若总数等于turnedOn，则按格式拼接成时间字符串并加入结果集。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-09 8:37
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BinaryWatch {
	
	// 测试方法
	public static void main(String[] args) {
		BinaryWatch binaryWatch = new BinaryWatch();
		// 测试示例1：turnedOn=1
		System.out.println(binaryWatch.readBinaryWatch(1));
		// 测试示例2：turnedOn=9
		System.out.println(binaryWatch.readBinaryWatch(9));
	}
	
	/**
	 * 获取所有亮灯数为turnedOn的合法时间
	 * @param turnedOn   亮着的LED数量
	 * @return  所有合法时间的字符串列表
	 */
	public List<String> readBinaryWatch(int turnedOn) {
		// 存储最终结果
		List<String> result = new ArrayList<>();
		
		// 枚举所有可能的小时（0-11）
		for (int hour = 0; hour < 12; hour++) {
			// 枚举所有可能的分钟（0-59）
			for (int minute = 0; minute < 60; minute++) {
				// 统计小时的二进制中1的个数 + 分钟的二进制中1的个数
				int totalOn = countOneBits(hour) + countOneBits(minute);
				// 如果总数等于目标值，拼接成合法格式的时间字符串
				if (totalOn == turnedOn) {
					result.add(String.format("%d:%02d", hour, minute));
				}
			}
		}
		return result;
	}
	
	/**
	 * 统计一个整数的二进制表示中1的个数（核心工具方法）
	 * @param num 待统计的整数
	 * @return    二进制中1的个数
	 */
	private int countOneBits(int num) {
		int count = 0;
		// 循环直到num变为0
		while (num >0) {
			// 按位与运算：仅保留最低位的1，其余位为0
			num -= num & -num;
			count++;
		}
		return count;
	}
}