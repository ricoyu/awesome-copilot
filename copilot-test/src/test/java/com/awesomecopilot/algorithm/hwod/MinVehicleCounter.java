package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 停车场车辆统计
 * <p>
 * 一、题目描述
 * <p>
 * 特定大小的停车场，数组cars[]表示，其中1表示有车，0表示没车。车辆大小不一，小车占一个车位（长度1），货车占两个车位（长度2），卡车占三个车位（长度3），统计停车场最少可以停多少辆车，返回具体的数目。
 * <p>
 * 二、输入描述
 * <p>
 * 整型字符串数组cars[]，其中1表示有车，0表示没车，数组长度小于1000。
 * <p>
 * 三、输出描述
 * <p>
 * 整型数字字符串，表示最少停车数目。
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 1,0,1
 *
 * 2、输出
 * 2
 * </pre>
 * <p>
 * 3、说明 <p>
 * 1个小车占第1个车位
 * <p>
 * 第二个车位空
 * <p>
 * 1个小车占第3个车位
 * <p>
 * 最少有两辆车
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 1,1,0,0,1,1,1,0,1
 *
 * 2、输出
 * 3
 * </pre>
 * <p>
 * 3、说明 <p>
 * 1个货车占第1、2个车位
 * <p>
 * 第3、4个车位空
 * <p>
 * 1个卡车占第5、6、7个车位
 * <p>
 * 第8个车位空
 * <p>
 * 1个小车占第9个车位
 * <p>
 * 最少3辆车
 * <p>
 * <ul>我们要统计最少车辆数量，因此需要“尽可能合并连续的 1”：
 *     <li/>连续 3 个 1 → 卡车（长度3）
 *     <li/>连续 2 个 1 → 货车（长度2）
 *     <li/>连续 1 个 1 → 小车（长度1）
 * </ul>
 * <ul>遇到 0 或数组结束时，就将当前累计的连续 1 长度除以 3，再分别处理余下的部分：
 *     <li/>先停卡车（每3个1）
 *     <li/>再停货车（每2个1）
 *     <li/>最后剩下的小车（每1个1）
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-07-06 12:30
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MinVehicleCounter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入停车场车辆数组：");
		int[] cars = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(countVehicles(cars));
	}

	public static String countVehicles(int[] cars) {
		int count = 0; // 最少车辆数
		int i = 0;
		while (i < cars.length) {
			// 只处理连续的1段
			if (1 == cars[i]) {
				int len = 0; // 当前连续1的长度
				while (i < cars.length && 1 == cars[i]) {
					len++;
					i++;
				}
				// 处理该段连续1
				count += len / 3; // 每3个1组成1辆卡车
				len %= 3;
				count += len / 2; // 每2个1组成1辆货车
				len %= 2;
				count += len; // 每1个1组成1辆小车
			}else {
				i++; // 当前是0，跳过
			}
		}

		return String.valueOf(count);
	}
}
