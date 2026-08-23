package com.awesomecopilot.algorithm;

import java.util.Scanner;

public class CharCount2 {
	
	/**
	 * 计算字符个数
	 * 写出一个程序，接受一个由字母和数字组成的字符串，和一个字符，然后输出输入字符串中含有该字符的个数。不区分大小写。
	 */
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String str = scanner.nextLine();
		System.out.print("请输入字符: ");
		char ch = scanner.next().charAt(0);
	}
}
