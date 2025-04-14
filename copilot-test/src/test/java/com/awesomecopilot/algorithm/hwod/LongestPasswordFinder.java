package com.awesomecopilot.algorithm.hwod;

import java.util.Arrays;
import java.util.Scanner;
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
 * <p>
 * 请用Java实现并给出核心的解题思路, 分析过程尽量简洁不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-04-14 8:56
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongestPasswordFinder {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		for (int i = 0; i < 3; i++) {
			System.out.print("请输入密码本:");
			String[] passwords = scanner.nextLine().split(" ");
			System.out.println(findLongestPassword(passwords));
		}
		scanner.close();
	}

	public static String findLongestPassword(String[] passwords) {
		Arrays.sort(passwords, (pass1, pass2) -> {
			if (pass1.length() != pass2.length()) {
				return Integer.compare(pass2.length(), pass1.length());
			} else {
				return pass1.compareTo(pass2);
			}
		});
		Set<String> passwordSet = Set.of(passwords);

		for (String password : passwords) {
			boolean valid = true;
			for (int i = 1; i < password.length() - 1; i++) {
				String subPassword = password.substring(0, password.length() - i);
				if (!passwordSet.contains(subPassword)) {
					valid = false;
					break;
				}
			}
			if (valid) {
				return password;
			}
		}
		return "";
	}
}
