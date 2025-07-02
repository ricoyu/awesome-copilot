package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 信道分配
 * <p>
 * 一、题目描述
 * <p>
 * 算法工程师Q小明面对着这样一个问题，需要将通信用的信道分配给尽量多的用户：
 * <p>
 * 信道的条件及分配规则如下：
 * <p>
 * 所有信道都有属性"阶"。阶为r的信道的容量为 2^r 比特；<br/>
 * 所有用户需要传输的数据量都一样：D比特；<br/>
 * 一个用户可以分配多个信道，但每个信道只能分配给一个用户；<br/>
 * 当且仅当分配给一个用户的所有信道的容量和 >= D，用户才能传输数据；<br/>
 * 给出一组信道资源，最多可以为多少用户传输数据？<br/>
 * <p>
 * 二、输入描述 <p>
 * 第一行，一个数字 R。R为最大阶数。
 * <br/>
 * 0 <= R < 20
 * <p>
 * 第二行，R+1个数字，用空格隔开。代表每种信道的数量 Ni。按照阶的值从小到大排列。
 * <p>
 * 0 <= i <= R，0 <= Ni < 1000
 * <p>
 * 第三行，一个数字 D。D为单个用户需要传输的数据量。
 * <p>
 * 0 < D < 1000000
 * <p>
 * 三、输出描述 <p>
 * 一个数字（代表最多可以供多少用户传输数据）
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 5
 * 10 5 0 1 3 2
 * 30
 *
 * 2、输出
 * 4
 * </pre>
 * <p>
 * 3、说明 <p>
 * 这个用例中，最大阶数为5，信道的数量分别为10, 5, 0, 1, 3, 2，每个用户需要传输30比特的数据。以下是信道的容量及分布：
 * <p>
 * 2^0: 10个
 * 2^1: 5个
 * 2^2: 0个
 * 2^3: 1个
 * 2^4: 3个
 * 2^5: 2个
 * <p>
 * 30的二进制表示为11110，所需的最小信道组合是：
 * <p>
 * 2^4: 1个 (16)
 * 2^3: 1个 (8)
 * 2^2: 0个 (4)
 * 2^1: 1个 (2)
 * 2^0: 0个 (0)
 * <p>
 * 这样可以满足5个用户的传输需求。
 * <p>
 * 测试用例2：
 * 1、输入
 * 3
 * 2 2 1 0
 * 7
 * <p>
 * 2、输出
 * 1
 * <p/>
 * Copyright: Copyright (c) 2025-07-01 9:00
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ChannelAllocator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("输入最大阶数: ");
		int R = scanner.nextInt();
		System.out.print("输入每种信道的数量: ");
		scanner.nextLine();
		int[] channels = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
		System.out.print("输入单个用户需要传输的数据量: ");
		int D = scanner.nextInt();
		System.out.println(maxUsers(R, channels, D));
	}

	/**
	 * 二分查找最大可服务的用户数量
	 *
	 * @param R        最大阶数
	 * @param channels 每种信道的数量 Ni。按照阶的值从小到大排列
	 * @param D        单个用户需要传输的数据量
	 * @return
	 */
	public static int maxUsers(int R, int[] channels, int D) {
		int left = 0, right = 1000000;// 上限足够大即可
		int result = 0;
		while (left <= right) {
			int mid = (left + right) / 2;
			if (canServeUsers(R, channels, D, mid)) {
				result = mid;
				left = mid + 1;
			} else {
				right = mid - 1;
			}
		}
		return result;
	}

	/**
	 * 判断是否能为k个用户分配满足D的信道组合
	 *
	 * @param R        输入最大阶数
	 * @param channels 每种信道的数量 Ni。按照阶的值从小到大排列
	 * @param D        单个用户需要传输的数据量
	 * @param k        用户数
	 * @return
	 */
	public static boolean canServeUsers(int R, int[] channels, int D, int k) {
		// 拷贝信道数组，避免原始数据被修改
		int[] available = Arrays.copyOf(channels, channels.length);

		for (int user = 0; user < k; user++) {
			int need = D;
			// 从大到小选择信道，贪心满足本用户
			for (int r = R; r >= 0; r--) {
				int capacity = 1 << r;
				int maxUse = Math.min(available[r], need / capacity);
				need -= maxUse * capacity;
				available[r] -= maxUse;
			}
			// 当前用户无法满足D容量
			if (need > 0) {
				return false;
			}
		}
		return true;
	}


}
