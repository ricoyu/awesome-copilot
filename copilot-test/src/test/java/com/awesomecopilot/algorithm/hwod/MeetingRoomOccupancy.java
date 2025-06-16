package com.awesomecopilot.algorithm.hwod;

import com.awesomecopilot.common.lang.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * 会议室占用时间段
 * <p>
 * 一、题目描述 <p>
 * 现有若干个会议，所有会议共享一个会议室，用数组表示各个会议的开始时间和结束时间，
 * <p>
 * 格式为: [[会议1开始时间，会议1结束时间]，[会议2开始时间，会议2结束时间]] 请计算会议室占用时间段。
 * <p>
 * 二、输入描述
 * <p>
 * [[会议1开始时间，会议1结束时间]，[会议2开始时间，会议2结束时间] ]
 * <p>
 * 备注:
 * <p>
 * 会议个数范围: [1,100]
 * <p>
 * 会议室时间段: [1,24]
 * <p>
 * 三、输出描述
 * <p>
 * 输出格式与输入一致,具体请看用例。
 * <p>
 * [[会议开始时间，会议结束时间]，[会议开始时间，会议结束时间]
 * <pre>
 * 1、输入
 * [[1 ,4],[4,5]]
 *
 * 2、输出
 * [[1,5]]
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-13 9:24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MeetingRoomOccupancy {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入会议开始结束时间段: ");
		String input = scanner.nextLine().trim();
		//input = input.substring(1, input.length() - 1);
		//String[] parts = input.split(",");
		int[][] meetings = ArrayUtils.parseTwoDimensionArray(input);
		//for (int i = 0; i < parts.length; i++) {
		//	String[] subParts = parts[i].trim().substring(1, parts[i].trim().length() - 1).split(",");
		//	int[] meeting = new int[subParts.length];
		//	meeting[0] = Integer.parseInt(subParts[0]);
		//	meeting[1] = Integer.parseInt(subParts[1]);
		//	meetings[i] = meeting;
		//}

		List<int[]> result = mergeMeetings(meetings);
		for (int[] ints : result) {
			System.out.println(Arrays.toString(ints));
		}
	}

	/**
	 * 合并会议时间段
	 * @param meetings 输入的会议时间段
	 * @return 合并后的会议时间段
	 */
	public static List<int[]> mergeMeetings(int[][] meetings) {
		// 如果会议数量为0或1，直接返回
		if (meetings == null ||meetings.length == 0) {
			return Arrays.asList(meetings);
		}

		// 第一步：按会议开始时间升序排序
		Arrays.sort(meetings, Comparator.comparingInt(a -> a[0]));
		List<int[]> result = new ArrayList<>();

		// 初始化第一个会议区间
		int start = meetings[0][0];
		int end = meetings[0][1];

		// 遍历剩下的会议时间段
		for (int i = 1; i < meetings.length; i++) {
		    int[] current = meetings[i];

			// 如果当前会议的开始时间 <= 上一个合并区间的结束时间，说明有重叠或连接
			if (current[0] <= end) {
				// 更新结束时间为两者中的较大值，表示合并
				end = Math.max(current[1], end);
			} else {
				// 如果没有重叠，先把前一个合并区间加入结果集
				result.add(new int[]{start, end});
				// 然后更新当前区间
				start = current[0];
				end = current[1];
			}
		}
		// 最后一个合并区间也要加入结果集
		result.add(new int[]{start, end});
		return result;
	}
}
