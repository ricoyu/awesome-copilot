package com.awesomecopilot.algorithm.hwod.round2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 最长的密码
 * <p>
 * 小王正在进行游戏大闯关，有一个关卡需要输入一个密码才能通过，密码获得的条件如下：
 * <p>
 * 在一个密码本中，每一页都有一个由26个小写字母组成的密码，每一页的密码不同，需要从这个密码本中寻找这样一个最长的密码， 从它的末尾开始依次去掉一位得到的新密码也在密码本中存在。
 * <p>
 * 请输出符合要求的密码，如果有多个符合要求的密码，返回长度最大的密码。
 * <p>
 * 若没有符合要求的密码，则返回空字符串。
 * <p>
 * 二、输入描述 <p>
 * 密码本由一个字符串数组组成，不同元素之间使用空格隔开，每一个元素代表密码本每一页的密码。
 * <p>
 * 三、输出描述 <p>
 * 一个字符串
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * h he hel hell hello
 *
 * 2、输出
 * hello
 * </pre>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * b ereddred bw bww bwwl bwwlm bwwln
 *
 * 2、输出
 * bwwlm
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-22 9:23
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestPasswordFinder {

	public static void main(String[] args) {
		String[] passwords = new String[]{"h", "he", "hel", "hell", "hello"};
		System.out.println(findLongestValidPassword(passwords)); // 输出: hello

		String[] passwords2 = {"b", "ereddred", "bw", "bww", "bwwl", "bwwlm", "bwwln"};
		System.out.println(findLongestValidPassword(passwords2)); // 输出: bwwlm
	}

	public static String findLongestValidPassword(String[] passwords) {
		Set<String> passwordSet = new HashSet<>(Arrays.asList(passwords));
		Arrays.sort(passwords, (a, b) -> {
			return b.length() - a.length();
		});

		// 遍历排序后的密码
		for (String password : passwords) {
			boolean isValid = true;
			// 从密码的末尾开始依次去掉一位，检查所有子密码是否存在
			for (int i = password.length() - 1; i >= 1; i--) {
				String pass = password.substring(0, i);
				if (!passwordSet.contains(pass)) {
					isValid = false;
					break;
				}
			}
			// 如果所有子密码都存在，返回当前密码
			if (isValid) {
				return password;
			}
		}
		// 没有满足条件的密码，返回空字符串
		return "";
	}
}
