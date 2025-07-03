package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 转骰子
 * <p>
 * 初始为：左1，右2，前3（观察者方向），后4，上5，下6，用123456表示这个状态，放置在平面上。
 * <p>
 * 可以向左翻转（用L表示向左翻转1次）； <p>
 * 可以向右翻转（用R表示向右翻转1次）； <p>
 * 可以向前翻转（用F表示向前翻转1次）； <p>
 * 可以向后翻转（用B表示向后翻转1次）； <p>
 * 可以以逆时针旋转（用A表示逆时针旋转90度）； <p>
 * 可以以顺时针旋转（用C表示顺时针旋转90度）。 <p>
 * <p>
 * 现在从123456这个初始状态开始，根据输入的动作/指令，计算得出最终的状态。
 * <p>
 * 二、输入描述 <p>
 * 输入一行，为只包含LRFBAC的字母序列，最大长度为50，字母可重复。
 * <p>
 * 三、输出描述 <p>
 * 输出最终状态。
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * LR
 *
 * 2、输出
 * 123456
 * </pre>
 * <p>
 * 3、说明 <p>
 * 立方体先向左翻转，再向右翻转回去，最终还是恢复到初始状态123456。 <p>
 * <pre>
 * 测试用例2：
 * 1、输入
 * FCR
 *
 * 2、输出
 * 342156
 * </pre>
 * <p>
 * 操作的正确定义:
 * <ul>向左翻转 (L)
 *     <li/>上面 → 左面
 *     <li/>左面 → 下面
 *     <li/>下面 → 右面
 *     <li/>右面 → 上面
 *     <li/>前面和后面不变
 * </ul>
 * <ul>向右翻转 (R)
 *     <li/>向左翻转的逆操作
 * </ul>
 * <ul>向前翻转 (F)
 *     <li/>上面 → 前面
 *     <li/>前面 → 下面
 *     <li/>下面 → 后面
 *     <li/>后面 → 上面
 *     <li/>左面和右面不变
 * </ul>
 * <ul>向后翻转 (B)
 *     <li/>向前翻转的逆操作
 * </ul>
 * <ul>逆时针旋转 (A)
 *     <li/>左面 → 前面
 *     <li/>前面 → 右面
 *     <li/>右面 → 后面
 *     <li/>后面 → 左面
 *     <li/>上面和下面不变
 * </ul>
 * <ul>顺时针旋转 (C)
 *     <li/>逆时针旋转的逆操作
 * </ul>
 * <ol>骰子的六个面可以用一个数组 state 表示，索引分别对应：
 *     <li/>state[0]：左面
 *     <li/>state[1]：右面
 *     <li/>state[2]：前面
 *     <li/>state[3]：后面
 *     <li/>state[4]：上面
 *     <li/>state[5]：下面
 * </ol>
 * 初始状态为 [1, 2, 3, 4, 5, 6]。
 * <p/>
 * Copyright: Copyright (c) 2025-04-24 9:28
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DiceRoller {

	// 骰子的六个面：左、右、前、后、上、下
	private int[] state;

	public DiceRoller() {
		// 初始状态：左1，右2，前3，后4，上5，下6
		state = new int[]{1, 2, 3, 4, 5, 6};
	}

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		for (int i = 0; i < 3; i++) {
			DiceRoller diceRoller = new DiceRoller();
			System.out.print("请输入操作序列:");
			String commands = scanner.nextLine().trim().toUpperCase();
			diceRoller.executeCommands(commands);
		}
		scanner.close();
	}

	// 执行操作序列
	public void executeCommands(String commands) {
		for (char op : commands.toCharArray()) {
			switch (op) {
				case 'L':  leftRoll();
				case 'R': rightRoll();
				case 'F': forwardRoll();
				case 'B':  backwardRoll();
				case 'A': anticlockwiseRotate();
				case 'C': clockwiseRotate();
			}
		}
		System.out.println(state[0] + " " + state[1] + " " + state[2] + " " + state[3] + " " + state[4] + " " + state[5]);
	}

	/**
	 * <ul>顺时针旋转 (A)
	 *     <li/>右面 → 前面
	 *     <li/>前面 → 左面
	 *     <li/>左面 → 后面
	 *     <li/>后面 → 右面
	 *     <li/>上面和下面不变
	 * </ul>
	 */
	private void clockwiseRotate() {
		int originalLeft = state[0]; //原来的左面
		int originalRight = state[1]; //原来的右面
		int originalFront = state[2]; //原来的前面
		int originalBack = state[3]; //原来的后面

		state[0] = originalFront;  // 新的左面 = 原来的前面
		state[1] = originalBack; // 新的右面 = 原来的后面
		state[2] = originalRight;  // 新的前面 = 原来的右面
		state[3] = originalLeft; // 新的后面 = 原来的左面
	}

	/**
	 * <ul>逆时针旋转 (A)
	 *     <li/>左面 → 前面
	 *     <li/>前面 → 右面
	 *     <li/>右面 → 后面
	 *     <li/>后面 → 左面
	 *     <li/>上面和下面不变
	 * </ul>
	 */
	private void anticlockwiseRotate() {
		int originalLeft = state[0]; //原来的左面
		int originalRight = state[1]; //原来的右面
		int originalFront = state[2]; //原来的前面
		int originalBack = state[3]; //原来的后面

		state[0] = originalBack;  // 新的左面 = 原来的后面
		state[1] = originalFront; // 新的右面 = 原来的前面
		state[2] = originalLeft;  // 新的前面 = 原来的左面
		state[3] = originalRight; // 新的后面 = 原来的右面
	}

	/**
	 * <ul>向后翻转 (F)
	 *     <li/>上面 → 后面
	 *     <li/>后面 → 下面
	 *     <li/>下面 → 前面
	 *     <li/>前面 → 上面
	 *     <li/>左面和右面不变
	 * </ul>
	 */
	private void backwardRoll() {
		// 初始状态：左1，右2，前3，后4，上5，下6
		//state = new int[]{1, 2, 3, 4, 5, 6};
		int originalUp = state[4]; //原来的上面
		int originalDown = state[5]; //原来的下面
		int originalFront = state[2]; //原来的前面
		int originalBack = state[3]; //原来的后面
		state[3] = originalUp; // 新的后面 = 原来的上面
		state[5] = originalBack; // 新的下面 = 原来的后面
		state[2] = originalDown; // 新的前面 = 原来的下面
		state[4] = originalFront; // 新的上面 = 原来的前面
	}

	/**
	 * 向左翻转
	 * <ul>向左翻转 (L)
	 *     <li/>上面 → 左面
	 *     <li/>左面 → 下面
	 *     <li/>下面 → 右面
	 *     <li/>右面 → 上面
	 *     <li/>前面和后面不变
	 * </ul>
	 */
	public void leftRoll() {
		int originalLeft = state[0];
		int originalRight = state[1];
		int originalUp = state[4];
		int originalDown = state[5];

		state[0] = originalUp; // 新的左面 = 原来的上面
		state[1] = originalDown; // 新的右面 = 原来的下面
		state[4] = originalRight; // 新的上面 = 原来的右面
		state[5] = originalLeft; // 新的下面 = 原来的左面
		// 前面和后面不变
	}

	/**
	 * 向右翻转
	 * <ul>向右翻转 (L)
	 *     <li/>上面 → 右面
	 *     <li/>左面 → 上面
	 *     <li/>下面 → 左面
	 *     <li/>右面 → 下面
	 *     <li/>前面和后面不变
	 * </ul>
	 */
	public void rightRoll() {
		int originalLeft = state[0];
		int originalRight = state[1];
		int originalUp = state[4];
		int originalDown = state[5];

		state[0] = originalDown; // 新的左面 = 原来的下面
		state[1] = originalUp; // 新的右面 = 原来的上面
		state[4] = originalLeft; // 新的上面 = 原来的下面
		state[5] = originalRight; // 新的下面 = 原来的右面
		// 前面和后面不变
	}

	/**
	 * <ul>向前翻转 (F)
	 *     <li/>上面 → 前面
	 *     <li/>前面 → 下面
	 *     <li/>下面 → 后面
	 *     <li/>后面 → 上面
	 *     <li/>左面和右面不变
	 * </ul>
	 */
	public void forwardRoll() {
		int originalFront = state[2]; //原来的前面
		int originalBack = state[3]; //原来的后面
		int originalUp = state[4]; //原来的上面
		int originalDown = state[5]; //原来的下面

		state[2] = originalUp; // 新的前面 = 原来的上面
		state[3] = originalDown; // 新的后面 = 原来的下面
		state[4] = originalBack; // 新的上面 = 原来的后面
		state[5] = originalFront; // 新的下面 = 原来的前面
		// 左面和右面不变
	}

}
