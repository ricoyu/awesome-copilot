package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 代码编辑器
 * <p>
 * 一、题目描述 <p>
 * 某公司为了更高效的编写代码，邀请你开发一款代码编辑器程序。
 * <p>
 * 程序的输入为已有的代码文本和指令序列，指针初始位置位于文本的开头。程序需要输出编辑后的最终文本。支持的指令（X 均为大于等于 1 的整数，word 为无空格的字符串<>)：
 * <ul>
 *     <li/>FORWARD  X 指针向前（右）移动 X 格，如果指针移动位置超过了文本末尾，则将指针移动到文本末尾
 *     <li/>BACKWARD X 指针向后（左）移动 X 格，如果指针移动位置超过了文本开头，则将指针移动到文本开头
 *     <li/>SEARCH-FORWARD word 从指针当前位置向前查找 word 的起始位置，如果未找到则保持不变
 *     <li/>SEARCH-BACKWARD word 在文本中向后查找 word 并将指针移动到 word 的起始位置，如果未找到则保持不变
 *     <li/>INSERT word 在指针当前位置前插入 word，并将指针移动到 word 的结尾
 *     <li/>REPLACE word 在指针当前位置替换并插入字符串（删除原有字符），并增加新的字符
 *     <li/>DELETE X 在指针位置删除 X 个字符
 * </ul>
 * <p>
 * 二、输入描述 <p>
 * 输入的第一行为命令列表的长度 K
 * <p>
 * 输入的第二行为文件中的原始文本
 * <p>
 * 接下来的 K 行，每行为一个指令
 * <p>
 * 文本最大长度不超过 256K
 * <p>
 * 三、输出描述 <p>
 * 编辑后的最终结果
 *
 * <pre>
 * 测试用例1：
 * 1、输入
 * 1
 * ello
 * INSERT h
 *
 * 2、输出
 * hello
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 2
 * hllo
 * FORWARD 1
 * INSERT e
 *
 * 2、输出
 * hello
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-05 8:38
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CodeEditor {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入指令列表的长度:");
		int n = scanner.nextInt();
		scanner.nextLine();
		System.out.print("请输入原始文本:");
		String originalText = scanner.nextLine().trim();
		StringBuilder text = new StringBuilder(originalText);
		int cursor = 0;

		for (int i = 0; i < n; i++) {
			System.out.print("请输入第" + (i + 1) + "个指令:");
			String input = scanner.nextLine().trim();
			String[] parts = input.split(" ");
			String command = parts[0];
			switch (command) {
				case "FORWARD":
					int fwd = Integer.parseInt(parts[1]);
					cursor = Math.min(cursor + fwd, originalText.length());// 指针不能超过文本长度
					break;
				case "BACKWARD":
					int back = Integer.parseInt(parts[1]);
					cursor = Math.max(cursor - back, 0);// 指针不能小于0
				case "SEARCH-BACKWARD":
					String word = parts[1];
					// 限制搜索范围为cursor之前的部分（含）
					int limit = Math.max(0, cursor);
					int idx = originalText.lastIndexOf(word, limit);
					if (idx != -1) {
						cursor = idx;
					}
					break;
				case "SEARCH-FORWARD":
					word = parts[1];
					limit = Math.min(cursor, originalText.length());
					idx = originalText.indexOf(word, limit);
					if (idx != -1) {
						cursor = idx;
					}
					break;
				case "INSERT":
					String insertWord = parts[1];
					text.insert(cursor, insertWord); // 在当前光标前插入
					cursor += insertWord.length(); // 插入后，指针移动到插入单词末尾
					break;

				case "REPLACE":
					String replaceWord = parts[1];
					// 删除当前位置的原有字符，个数为replaceWord长度（防止越界）
					int deleteLen = Math.min(replaceWord.length(), originalText.length() - cursor);
					text.delete(cursor, cursor + deleteLen);
					text.insert(cursor, replaceWord);
					cursor += replaceWord.length();
					break;

				case "DELETE":
					int delLen = Integer.parseInt(parts[1]);
					// 删除范围不能越界
					int end = Math.min(cursor + delLen, text.length());
					text.delete(cursor, end);
					break;

				default:
					// 不支持的指令忽略
					break;
			}
		}
	}
}
