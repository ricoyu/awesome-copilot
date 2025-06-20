package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 绘图机器 - 双指针
 * <p>
 * 一、题目描述
 * <p>
 * <ul>绘图机器的绘图笔初始位置在原点(0,0) 机器启动后按照以下规则来进行绘制直线。
 *     <li/>尝试沿着横线坐标正向绘制直线 直到给定的终点E；
 *     <li/>期间可以通过指令在纵坐标轴方向进行偏移 offsetY为正数表示正向偏移,为负数表示负向偏移。
 *     <li/>给定的横坐标终点值E 以及若干条绘制指令 请计算绘制的直线和横坐标轴以及x=E的直线组成的图形面积。
 * </ul>
 *
 *  二、输入描述 <p>
 * 首行为两个整数N 和 E 表示有N条指令, 机器运行的横坐标终点值E，接下来N行 每行两个整数表示一条绘制指令x offsetY。
 * <p>
 * 用例保证横坐标x以递增排序的方式出现 且不会出现相同横坐标x。
 <p>
 * 三、输出描述 <p>
 * 一个整数表示计算得到的面积 用例保证结果范围在0到4294967295之内。
 * <p>
 *
 * <pre>
 * 1、输入
 * 4 10
 * 1 1
 * 2 1
 * 3 1
 * 4 -2
 *
 * 2、输出
 * 12
 * </pre>
 *
 * <pre>
 * 执行过程如下（记住 offsetY 是相对值）：
 *
 * 步骤	x点	偏移量	操作	                                            y高度
 * 0    0	-       起点	                                            0
 * 1    1	1       从 (0,0) 画水平线到 (1,0)，然后垂直偏移 +1，当前 y=1	1
 * 2    2	1       从 (1,1) 画水平线到 (2,1)，然后垂直偏移 +1，当前 y=2	2
 * 3    3	1       从 (2,2) 画水平线到 (3,2)，然后垂直偏移 +1，当前 y=3	3
 * 4    4	-2      从 (3,3) 画水平线到 (4,3)，然后垂直偏移 -2，当前 y=1	1
 * 5    10	-       从 (4,1) 画水平线到 (10,1)	1
 * </pre>
 *
 * <pre>
 * 每段面积计算
 * (0,0) → (1,0)：宽=1，高=0 → 面积 = 0
 *
 * (1,0) → (2,1)：宽=1，高=1 → 面积 = 1
 *
 * (2,1) → (3,2)：宽=1，高=2 → 面积 = 2
 *
 * (3,2) → (4,3)：宽=1，高=3 → 面积 = 3
 *
 * (4,3) → (10,1)：宽=6，高=1 → 面积 = 6
 * 总面积：0+1+2+3+6 = 12
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-19 8:39
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DrawingMachineAreaCalculator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入指令数量: ");
		int n = scanner.nextInt();
		System.out.print("请输入横坐标终点: ");
		int e = scanner.nextInt();

		int prevX = 0; //起始横坐标
		int currentY = 0; //当前y的高度
		long area = 0;
		scanner.nextLine();
		for (int i = 0; i < n; i++) {
		    System.out.print("请输入当前绘制指令的x坐标和yOffset: ");
			String input = scanner.nextLine().trim();
			String[] parts = input.split(" ");

			int x = Integer.parseInt(parts[0]);
			int yOffset = Integer.parseInt(parts[1]);

			//从 prevX 到 x 画一段水平线，高度是 currentY
			area += (long)(x-prevX) * currentY;
			// 在 x 处垂直偏移
			currentY += yOffset;
			// 更新 prevX
			prevX = x;
		}

		// 最后一段从 prevX 到 E，维持 currentY 高度
		area += (long)(e-prevX) * currentY;

		System.out.println(area);
	}
}
