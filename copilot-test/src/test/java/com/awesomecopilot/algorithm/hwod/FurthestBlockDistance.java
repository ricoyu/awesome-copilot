package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 积木最远距离
 * <p>
 * 小华和小薇一起通过玩积木游戏学习数学。
 * <p>
 * 他们有很多积木，每个积木块上都有一个数字，积木块上的数字可能相同。
 * <p>
 * 小华随机拿一些积木块并排成一排，请你帮她找到这排积木中相同数字且所处位置最远的2块积木，计算它们的距离，小薇请你帮忙着地解决这个问题
 * <p>
 * 、输入描述 <p>
 * 第一行输入为N，表示小华排成一排的积木总数。
 * <p>
 * 接下来N行每行一个数字，表示小华排成一排的积木上数字。
 * <p>
 * 三、输出描述 <p>
 * 相同数字的积木块位置最远距离；如果所有积木块的数字都不相同，请返回-1。
 * <p>
 * 备注
 * <p>
 * 0 <= 积木上的数字 < 109
 * <p>
 * 1 <= 积木长度 <= 105
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * 5
 * 1
 * 2
 * 3
 * 1
 * 4
 *
 * 2、输出
 * 3
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 2
 * 1
 * 2
 *
 * 2、输出
 * -1
 * </pre>
 *
 * <pre>
 * 测试用例3：
 * 1、输入
 * 6
 * 5
 * 1
 * 5
 * 2
 * 5
 * 3
 *
 * 2、输出
 * 4
 * </pre>
 * <p>
 * 请用Java实现并给出核心的解题思路, 分析过程尽量简洁不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-04-17 9:27
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FurthestBlockDistance {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入积木总数:");
		int n = scanner.nextInt();
		int[] blocks = new int[n];
		for (int i = 0; i < n; i++) {
			System.out.print("请输入第" + (i + 1) + "个积木上的数字:");
			blocks[i] = scanner.nextInt();
		}
		int furthestDistance = findFurthestDistance(blocks);
		System.out.println("积木最远距离: " + furthestDistance);
	}

	public static int findFurthestDistance(int[] blocks) {
		Map<Integer, Integer> firstOccurMap = new HashMap<>(blocks.length);
		Map<Integer, Integer> secondOccurMap = new HashMap<>(blocks.length);

		for (int i = 0; i < blocks.length; i++) {
			if (!firstOccurMap.containsKey(blocks[i])) {
				firstOccurMap.put(blocks[i], i);
			} else {
				secondOccurMap.put(blocks[i], i);
			}
		}

		int maxDistance = 0;
		for (Integer i : blocks) {
			if (firstOccurMap.containsKey(i) && secondOccurMap.containsKey(i)) {
				maxDistance = Math.max(maxDistance, Math.abs(secondOccurMap.get(i) - firstOccurMap.get(i)));
			}
		}
		return maxDistance;
	}


}
