package com.awesomecopilot.algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 罗马数字转整数
 * <p>
 * 罗马数字包含以下七种字符: I， V， X， L，C，D 和 M。
 * <pre>
 * 字符          数值
 * I             1
 * V             5
 * X             10
 * L             50
 * C             100
 * D             500
 * M             1000
 * </pre>
 * <p>
 * 例如, 罗马数字 2 写做 II, 即为两个并列的 1。12 写做 XII, 即为 X + II。 27 写做XXVII, 即为 XX + V + II 。
 * <p>
 * 通常情况下, 罗马数字中小的数字在大的数字的右边。但也存在特例, 例如4不写做 IIII, 而是 IV。数字1在数字5的左边, 所表示的数等于大数5减小数1得到的数值 4。同样地, 数字 9 表示为
 * IX。这个特殊的规则只适用于以下六种情况:
 * <p>
 * <pre>
 * I 可以放在 V (5) 和 X (10) 的左边，来表示 4 和 9。
 * X 可以放在 L (50) 和 C (100) 的左边，来表示 40 和 90。
 * C 可以放在 D (500) 和 M (1000) 的左边，来表示 400 和 900。
 * </pre>
 * <p>
 * 给定一个罗马数字，将其转换成整数。
 *
 * <pre>
 * 示例 1:
 *
 * 输入: s = "III"
 * 输出: 3
 * </pre>
 *
 * <pre>
 * 示例 2:
 *
 * 输入: s = "IV"
 * 输出: 4
 * </pre>
 *
 * <pre>
 * 示例 3:
 *
 * 输入: s = "IX"
 * 输出: 9
 * </pre>
 *
 * <pre>
 * 示例 4:
 *
 * 输入: s = "LVIII"
 * 输出: 58
 * 解释: L = 50, V= 5, III = 3.
 * </pre>
 *
 * <pre>
 * 示例 5:
 *
 * 输入: s = "MCMXCIV"
 * 输出: 1994
 * 解释: M = 1000, CM = 900, XC = 90, IV = 4.
 * </pre>
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2025-09-15 8:12
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RomanToIntegerConverter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter roman number: ");
		String s = scanner.nextLine().trim();
		System.out.println(romanToInt(s));
		scanner.close();
	}

	/**
	 * 将罗马数字转换为整数
	 *
	 * @param s 罗马数字字符串
	 * @return 对应的整数
	 */
	private static int romanToInt(String s) {
		// 创建罗马字符到数值的映射表
		Map<Character, Integer> romanMap = new HashMap<>();
		romanMap.put('I', 1);
		romanMap.put('V', 5);
		romanMap.put('X', 10);
		romanMap.put('L', 50);
		romanMap.put('C', 100);
		romanMap.put('D', 500);
		romanMap.put('M', 1000);

		int result = 0;
		int length = s.length();

		// 遍历罗马数字字符串
		for (int i = 0; i < length; i++) {
			char currentChar = s.charAt(i);
			// 获取当前字符对应的数值
			int currentValue = romanMap.get(currentChar);

			// 如果不是最后一个字符，且当前值小于下一个值，使用减法
			// 否则使用加法
			if (i < length - 1 && currentValue < romanMap.get(s.charAt(i + 1))) {
				result -= currentValue;
			} else {
				result += currentValue;
			}
		}
		return result;
	}
}
