package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

/**
 * 项目排期
 * <p>
 * 一、题目描述
 * <p>
 * 项目组共有N个开发人员，项目经理接到了M个独立的需求，每个需求的工作量不同，且每个需求只能由一个开发人员独立完成，不能多人合作。
 * <p>
 * 假定各个需求直接无任何先后依赖关系，请设计算法帮助项目经理进行工作安排，使整个项目能用最少的时间交付。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行输入为M个需求的工作量，单位为天，用逗号隔开。 例如：X1 X2 X3 … Xm 表示共有M个需求，每个需求的工作量分别为X1天，X2天…Xm天。其中0<M<30；0<Xm<200 第二行输入为项目组人员数量N 例如： 5
 * 表示共有5名员工，其中0<N<10
 * <p>
 * 三、输出描述
 * <p>
 * 最快完成所有工作的天数 例如： 25 表示最短需要25天能完成所有工作。
 *
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * 8 8 8 8 8 1
 * 3
 *
 * 2、输出
 * 16
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 10 10 10 10
 * 2
 *
 * 2、输出
 * 20
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-09 9:54
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ProjectScheduler {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入M个需求的工作量, 空格隔开: ");
		int[] tasks =
				Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.print("请输入项目组人员数量: ");
		int n = scanner.nextInt();
		scanner.close();

		System.out.println(minCompletionTime(tasks, n));
	}

	public static int minCompletionTime(int[] tasks, int workers) {
		// 边界情况处理
		if (tasks.length == 0) {
			return 0;
		}
		if (workers <= 0) {
			return -1;
		}

		// 将任务按从大到小排序，便于贪心分配
		List<Integer> taskList = Arrays.stream(tasks).boxed().collect(toList());
		Collections.sort(taskList, Collections.reverseOrder());
		/*
		 * 即使有N个开发人员，最大的任务也必须由一个开发人员单独完成
		 * 例如：任务[8,5,3]，3个开发人员 → 最少需要8天（因为最大的8天任务必须由一个人完成）
		 */
		int left = taskList.get(0); // 最小可能时间（最大单个任务时间）
		/*
		 * 如果只有一个开发人员（N=1），那么所有任务都必须由这一个人完成
		 * 完成时间就是所有任务工作量的总和
		 * 例如：任务[8,5,3]，1个开发人员 → 需要8+5+3=16天
		 */
		int right = taskList.stream().mapToInt(Integer::valueOf).sum(); // 最大可能时间（所有任务总和）
		while (left < right) {
			int mid = left + (right - left) / 2;
			if (canAssign(taskList, workers, mid)) {
				right = mid;
			} else {
				left = mid + 1;
			}
		}

		return left;
	}

	/**
	 * 检查是否可以在给定时间限制内分配所有任务
	 *
	 * @param taskList 任务列表（已排序）
	 * @param workers  开发人员数量
	 * @param limit      时间限制
	 * @return 是否可以分配
	 */
	private static boolean canAssign(List<Integer> taskList, int workers, int limit) {
		int[] workLoads = new int[workers]; // 记录每个开发人员的当前工作量

		for (int task : taskList) {
			// 找到当前工作量最小的开发人员
			int minIndex = 0; // 默认先选第0个开发人员
			for (int i = 1; i < workers; i++) { //循环要从第一个开发人员开始找
				if (workLoads[i] < workLoads[minIndex]) {
					minIndex = i;
				}
			}
			// 尝试分配任务
			if (workLoads[minIndex] + task <= limit) {
				workLoads[minIndex] += task;
			} else {
				return false;
			}
		}
		return true;
	}
}
