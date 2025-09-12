package com.awesomecopilot.algorithm.leetcode;

import com.awesomecopilot.common.lang.utils.StringUtils;

import java.util.Arrays;
import java.util.Scanner;

/**
 * 最长公共前缀
 *
 * 编写一个函数来查找字符串数组中的最长公共前缀。
 * 如果不存在公共前缀，返回空字符串 ""。
 *
 * 示例 1：
 *
 * 输入：strs = ["flower","flow","flight"]
 * 输出："fl"
 *
 * 示例 2：
 *
 * 输入：strs = ["dog","racecar","car"]
 * 输出：""
 * 解释：输入不存在公共前缀。
 *
 * <ul>解题思路
 *     <li/>若字符串数组为空，直接返回空字符串
 *     <li/>以数组第一个字符串作为基准，依次与其他字符串比较
 *     <li/>找到每个字符串与基准字符串的公共前缀，不断缩短基准前缀
 *     <li/>若中途前缀长度变为 0，可提前结束比较
 * </ul>
 * <ul>以示例 1 ["flower","flow","flight"] 为例：
 *     <li/>初始前缀为第一个字符串 "flower"
 *     <li/>与第二个字符串 "flow" 比较：
 *          <ul>
 *              <li/>"flow".indexOf ("flower") ≠ 0，所以前缀缩短为 "flowe"
 *              <li/>仍然不满足，继续缩短为 "flow"
 *              <li/>此时 "flow".indexOf ("flow") = 0，找到与第二个字符串的公共前缀
 *          </ul>
 *     <li/>
 *     <li/>与第三个字符串 "flight" 比较：
 *          <ul>
 *              <li/>"flight".indexOf ("flow") ≠ 0，前缀缩短为 "flo"
 *              <li/>仍然不满足，继续缩短为 "fl"
 *              <li/>此时 "flight".indexOf ("fl") = 0，找到所有字符串的公共前缀
 *          </ul>
 *     <li/>
 *     返回结果 "fl"
 *     <li/>
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-09-10 8:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestCommonPrefix {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串数组strs, 以逗号分隔: ");
		String[] strs =scanner.nextLine().trim().split(",");
		for (int i = 0; i < strs.length; i++) {
		    strs[i] = StringUtils.cleanQuotationMark(strs[i]);
		}
		System.out.println(longestCommonPrefix(strs));
	}

	private static String longestCommonPrefix(String[] strs) {
		if (strs == null || strs.length == 0) {
			return "";
		}

		String prefix = strs[0];
		for (int i = 1; i < strs.length; i++) {
			while (strs[i].indexOf(prefix) != 0) {
				prefix = prefix.substring(0, prefix.length() - 1);
				if (prefix.isEmpty()) {
					return "";
				}
			}
		}
		return prefix;
	}
}
