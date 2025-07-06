package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 超级玛丽通过吊桥的走法
 * <p>
 * 一、题目描述
 * <p>
 * 超级玛丽好不容易来到崭新的一关，有一个长长的吊桥，吊桥的尽头是下水管道，其中随机的木板存在缺失，且踩到就会死亡，死亡后如果还有剩余的生命将仍然复活且不受木板缺失影响，但会消耗一次生命，如果跨过了管道，将踏入悬崖，通关失败。
 * <p>
 * 超级玛丽从起点 S 处出发，可以走到下一个木板(计1)，也可以跳跃跨到下两个木板(计3)，最终必须刚好走到终点 D。现在给定超级玛丽当前的生命数 M，吊桥长度的木板数 N，缺失的木板数 K 以及随机缺失的木板编号数组 L, 请帮忙计算一下，超级玛丽有多少种方法可以通过此关。
 * <p>
 * 二、输入描述
 * <p>
 * 第一行三个整数，超级玛丽当前生命数 M(1<=M<=5), 吊桥的长度 N(1<=N<=32, 整数)，缺失木板数 K(K<=K<=32, 整数)
 * <p>
 * 第二行为缺失木板编号数组 L:(长度为 K 的数组, 内容不大于 N 的编号数组), 1<=Li<=N， 由空格分隔的整数数组。
 * <p>
 * 三、输出描述
 * <p>
 * 输出通过此关的吊桥走法个数，如果不能通过此关，请输出0。
 * <p>
 * 提示：
 * <p>
 * 输入总是合法，忽略参数较验 <br/>
 * 必须从起点开始 <br/>
 * 必须离开吊桥到到终点。 <br/>
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 2 2 1
 * 2
 *
 * 2、输出
 * 4
 * </pre>
 *
 * <pre>
 * 3、说明
 * 2个生命，2个木板，缺失1个木板，缺失1个木板，第2个木板有缺失，一共有4种走法
 *
 * 3
 * 1 2
 * 2 1
 * 1（复活）1
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-04 9:29
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MarioBridgeSolver {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("超级玛丽当前生命数, 吊桥的长度，缺失木板数: ");
		int[] arr = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();
		int M = arr[0]; //超级玛丽当前生命数
		int N = arr[1]; //吊桥的长度
		int K = arr[2]; //缺失木板数

		System.out.print("缺失的木板编号数组: ");
		int[] L = Arrays.stream(scanner.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray();

		// dp[m][pos] 表示剩余 m 条生命，当前位置是 pos 的走法数
	}
}
