package com.awesomecopilot.algorithm.leetcode;

import java.util.HashMap;
import java.util.Map;

/**
 * 字符串中的第一个唯一字符
 * <p>
 * 给定一个字符串 s ，找到它的第一个不重复的字符，并返回它的索引。如果不存在，则返回 -1 。
 * <p>
 * 示例 1：
 * <p>
 * 输入: s = "leetcode"
 * 输出: 0
 * <p>
 * 示例 2:
 * <p>
 * 输入: s = "loveleetcode"
 * 输出: 2
 * <p>
 * 示例 3:
 * <p>
 * 输入: s = "aabb"
 * 输出: -1
 * <p/>
 * Copyright: Copyright (c) 2025-03-30 8:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class FirstUniqueChar {

	public int firstUniqChar(String s) {
		Map<Character, Integer> count = new HashMap<>();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			count.put(c, count.getOrDefault(c, 0) + 1);
		}

		for (int i = 0; i < s.length(); i++) {
			if (count.get(s.charAt(i)) == 1) {
				return i;
			}
		}

		return -1;
	}
}
