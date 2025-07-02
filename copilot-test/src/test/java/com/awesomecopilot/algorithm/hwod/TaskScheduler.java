package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * 任务总执行时长 - 枚举
 * <p>
 * 一、题目描述
 * <p>
 * 任务调排服务负责对任务进行组合调度。
 * <p>
 * 参与编排的任务有两种类型，其中一种执行时长为 taskA，另一种执行时长为 taskB。
 * <p>
 * 任务一旦开始执行不能被打断，且任务可连续执行。
 * <p>
 * 服务每次可以编排 num 个任务。
 * <p>
 * 请写一个方法，生成编排后的任务所有可能的总执行时长。
 * <p>
 * 二、输入描述
 * <p>
 * 第 1 行输入分别为 第 1 种任务执行时长 taskA，第 2 种任务执行时长 taskB，这次要编排的任务个数 num，以逗号分隔。
 * <p>
 * 注：每种任务的数量都大于本次可以编排的任务数量。
 * <p>
 * 0 < taskA
 * 0 < taskB
 * 0 <= num <= 100000
 * <p>
 * 三、输出描述
 * <p>
 * 数组形式返回所有总执行时长， 需要按从小到大排列。
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 1,2,3
 *
 * 2、输出
 * [3,4,5,6]
 * </pre>
 *
 * 3、说明
 * 所有可能的任务组合及其总执行时长如下：
 * <p>
 * 0个任务A，3个任务B：01 + 32 = 6 <br/>
 * 1个任务A，2个任务B：11 + 22 = 5 <br/>
 * 2个任务A，1个任务B：21 + 12 = 4 <br/>
 * 3个任务A，0个任务B：31 + 02 = 3 <br/>
 *
 * 排序后为 [3,4,5,6]
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 2,3,2
 *
 * 2、输出
 * [4,5,6]
 * </pre>
 *
 * 3、说明
 * 所有可能的任务组合及其总执行时长如下：
 * <p>
 * 0个任务A，2个任务B：02 + 23 = 6 <br/>
 * 1个任务A，1个任务B：12 + 13 = 5 <br/>
 * 2个任务A，0个任务B：22 + 03 = 4 <br/>
 * <p>
 * 排序后为 [4,5,6]
 *
 * <ul>核心解题思路：
 *     <li/>枚举所有可能使用的 taskA 数量：i = 0 到 num。
 *     <li/>每种组合的总时长为：i * taskA + (num - i) * taskB
 *     <li/>最终将所有结果去重（如果 taskA == taskB 会有重复），然后排序返回即可。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-06-24 9:01
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TaskScheduler {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入任务时长以及任务个数: ");
		int[] parts = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		int taskA = parts[0];
		int taskB = parts[1];
		int nums = parts[2];

		System.out.println(generateTotalDurations(taskA, taskB, nums));
		scanner.close();
	}

	/**
	 * 生成所有可能的总执行时长
	 * @param taskA 第1种任务的执行时长
	 * @param taskB 第2种任务的执行时长
	 * @param nums  本次要编排的任务数量
	 * @return      所有不同的总执行时长，升序排列
	 */
	public static List<Integer> generateTotalDurations(int taskA, int taskB, int nums) {
		Set<Integer> durations = new HashSet<>();

		// 枚举使用了多少个 taskA，其余的用 taskB
		for (int i = 0; i <= nums; i++) {
		    int countA = i;
			int countB = nums - i;

			// 计算当前组合的总时长
			int duration = countA * taskA + countB * taskB;
			durations.add(duration);
		}

		// 转换为列表并排序
		List<Integer> result = durations.stream().sorted().toList();
		return result;
	}
}
