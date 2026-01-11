package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

/**
 * 验证回文串
 * <p>
 * 如果在将所有大写字符转换为小写字符、并移除所有非字母数字字符之后，短语正着读和反着读都一样。则可以认为该短语是一个 回文串 。
 * <p>
 * 字母和数字都属于字母数字字符。
 * <p>
 * 给你一个字符串 s，如果它是 回文串 ，返回 true ；否则，返回 false 。
 *
 * <pre>
 * 示例 1：
 *
 * 输入: s = "A man, a plan, a canal: Panama"
 * 输出：true
 * 解释："amanaplanacanalpanama" 是回文串。
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：s = "race a car"
 * 输出：false
 * 解释："raceacar" 不是回文串。
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：s = " "
 * 输出：true
 * 解释：在移除非字母数字字符之后，s 是一个空字符串 "" 。
 * 由于空字符串正着反着读都一样，所以是回文串。
 * </pre>
 * 我会采用双指针法来解决这个问题, 这是处理回文串问题最高效的方法之一, 时间复杂度为 O (n), 空间复杂度为 O (1):
 * <ul>解题思路:
 *     <li/>预处理与指针初始化：定义左指针从字符串头部（0）开始，右指针从字符串尾部（length-1）开始。
 *     <li/>左指针向右移动，跳过所有非字母数字的字符。
 *     <li/>右指针向左移动，跳过所有非字母数字的字符。
 *     <ul>将左右指针指向的字符都转为小写后进行比较：
 *       <li/>如果不相等，直接返回 false。
 *       <li/>如果相等，左指针右移、右指针左移，继续循环。
 *     </ul>
 *     <li/>循环终止条件：当左指针 >= 右指针时，说明所有有效字符都已匹配完成，返回 true。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-10 8:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PalindromeValidator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入字符串: ");
		String s = scanner.nextLine().trim();
	}

	public boolean isPalindrome(String s) {
		int left = 0;
		int right = s.length() - 1;

		// 循环条件：左指针未超过右指针
		while (left < right) {
			// 移动左指针：跳过非字母数字的字符，直到找到有效字符或指针相遇
			while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
				left++;
			}
			// 移动右指针：跳过非字母数字的字符，直到找到有效字符或指针相遇
			while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
				right--;
			}

			// 此时左右指针都指向有效字符（字母/数字），转为小写后比较
			if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
				return false;
			}
			// 字符相等，继续向中间移动指针
			left++;
			right--;
		}
		return true;
	}
}
