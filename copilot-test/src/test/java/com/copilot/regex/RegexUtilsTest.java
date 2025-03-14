package com.copilot.regex;

import com.copilot.common.lang.utils.RegexUtils;

import java.util.Scanner;

public class RegexUtilsTest {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String s = scanner.nextLine();
		System.out.println(RegexUtils.trimQuotes(s));
	}
}
