package com.awesomecopilot.algorithm.hwod;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * 密室逃生
 * <p>
 * 小强在参加《密室逃生》游戏，当前关卡要找到符合给定密-码 K（升序的不重复小写字母组成）的箱子，并给出箱子编号，箱子编号为 1~N。
 * <p>
 * 每个箱子中都有一个字符串，字符串由大写字母、小写字母、数字、标点符号、空格组成，需要在这些字符串中找到所有的字母，忽略大小写后排列出对应的密-码，并返回匹配密-码的箱子序号。
 * <p>
 * 提示：满足条件的箱子不超过 1 个。
 * <p>
 * 输入描述
 * <p>
 * 第一行为 key 的字符串，
 * 第二行为箱子 boxes，为数组样式，以空格分隔
 * <p>
 * 箱子 N 数量满足 1 ≤ N ≤ 10000， <p>
 * ≤ K.length ≤ 50， <p>
 * 密-码仅包含小写字母的升序字符串，且不会有重复字母， <p>
 * 密-码 K 长度 1 ≤ K.length ≤ 26。 <p>
 * <p>
 * 输出描述
 * <p>
 * 返回匹配的箱子编号
 * <p>
 * 如果不存在包含要求的密-码的箱子，则返回 -1。
 * <p>
 * 测试用例1：
 * <p>
 * 1、输入
 * <pre>
 * abc
 * s,sdf134 A2c4b
 * </pre>
 * <p>
 * 2、输出 <br/>
 * 2
 * <p>
 * 3、说明 <p>
 * 第 2 个箱子中的 Abc，符合密-码 abc。
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2025-04-11 8:57
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class BoxPasswordMatcher {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter key: ");
		String key = scanner.nextLine();
		System.out.print("Enter boxes: ");
		String[] boxes = scanner.nextLine().trim().split(" ");
		System.out.println(findMatchingBox(key, boxes));
	}

	public static int findMatchingBox(String key, String[] boxes) {

		for (int i = 0; i < boxes.length; i++) {
			String box = boxes[i];

			char[] charArray = key.toCharArray();
			Set<Character> boxSet = new HashSet<>();
			for (int j = 0; j < charArray.length; j++) {
				boxSet.add(charArray[j]);
			}

			char[] boxChars = box.toLowerCase().toCharArray();
			for (int j = 0; j < boxChars.length; j++) {
				boxSet.remove(boxChars[j]);
			}

			if (boxSet.isEmpty()) {
				return i + 1;
			}
		}

		return -1;
	}
}
