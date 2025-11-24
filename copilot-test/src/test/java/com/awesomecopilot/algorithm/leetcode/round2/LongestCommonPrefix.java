package com.awesomecopilot.algorithm.leetcode.round2;

import com.awesomecopilot.common.lang.utils.StringUtils;

import java.util.Scanner;

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
					return  "";
				}
			}
		}
		return prefix;
	}
}
