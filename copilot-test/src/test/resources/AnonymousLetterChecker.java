package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 匿名信
 * <p>
 * 电视剧《分界线Q》里面有一个片段，男主为了向警察透露案件细节，且不暴露自己，于是将报刊上的字剪下来，剪拼成匿名信。 <p>
 * 现在一名举报人，希望借鉴这部片段，使用英文报刊为范本来剪拼举报信。 <p>
 * 但为了增加文章的混淆程度，尽量避免每个单词中字母数量一致即可，不关注每个字母的顺序。 <p>
 * 解释：单词n允许抄袭过n的字母组合。 <p>
 * <p>
 * 报纸代表newspaper，匿名信代表anonymousLetter，求报纸内容是否可以拼成匿名信。 <p>
 * <p>
 * 二、输入描述 <p>
 * 第一行newspaper内容，包括1~N个字符，并且空格分开 <p>
 * <p>
 * 第二行anonymousLetter内容，包括1~N个字符，并且空格分开。 <p>
 * <p>
 * newspaper和anonymousLetter的字符串中均为英文字母组成，且每个字母只能使用一次； <p>
 * <p>
 * newspaper内容中的每个字符中字母顺序可以任意调整，但必须保证字符串的完整性（每个字符串不能有多余字母） <p>
 * <p>
 * 1 < N < 100，
 * <p>
 * 1 <= newspaper.length，anonymousLetter.length <= 104
 * <p>
 * 三、输出描述 <p>
 * 如果报纸可以拼成匿名信返回true，否则返回false <p>
 * <p>
 * 测试用例1： <p>
 * 1、输入 <br/>
 * ab cd <br/>
 * ab <br/>
 * <p>
 * 2、输出 <p>
 * true
 * <p>
 * 测试用例2：
 * 1、输入 <br/>
 * ab ef <br/>
 * aef <br/>
 * <p>
 * 2、输出 <br/>
 * false
 * <p>
 * 本题类似于经典的“勒索信”问题，要求判断匿名信中的单词是否可以由报纸中的单词通过重新排列字母组成。具体要求如下：
 * <p>
 * 单词完整性：报纸中的每个单词可以重新排列其字母，但不能拆分或合并多个单词来构成匿名信中的单词。 <p>
 * 字母使用限制：每个字母在报纸中只能使用一次。
 * <p/>
 * Copyright: Copyright (c) 2025-04-12 9:47
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class AnonymousLetterChecker {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		for (int i = 0; i < 3; i++) {
			System.out.print("Enter newspaper: ");
			String newspaper = scanner.nextLine().trim();
			System.out.print("Enter anonymous letter: ");
			String anonmousLetter = scanner.nextLine().trim();

			AnonymousLetterChecker checker = new AnonymousLetterChecker();
			System.out.println(checker.canConstruct(newspaper, anonmousLetter));
		}
	}

	public boolean canConstruct(String newspaper, String anonmousLetter) {
		// 分割报纸和匿名信的单词
		String[] newspaperWords = newspaper.split(" ");
		String[] anonmousWords = anonmousLetter.split(" ");

		// 预处理报纸单词的字母频率 List<Map<Character, Integer>> newspaperLetterCounts = new ArrayList<>();
		List<Map<Character, Integer>> newspaperLetterCounts = new ArrayList<>();

		for (String newpaperWord : newspaperWords) {
			Map<Character, Integer> countMap = new HashMap<>();
			for (char c : newpaperWord.toCharArray()) {
				countMap.put(c, countMap.getOrDefault(c, 0) + 1);
			}
			newspaperLetterCounts.add(countMap);
		}

		boolean[] used = new boolean[newspaperWords.length];
		// 检查匿名信中的每个单词
		for (String anonmousWord : anonmousWords) {
			Map<Character, Integer> targetCount = new HashMap<>();
			for (char c : anonmousWord.toCharArray()) {
				targetCount.put(c, targetCount.getOrDefault(c, 0) + 1);
			}

			boolean found = false;

			// 遍历报纸单词，寻找匹配
			for (int i = 0; i < newspaperLetterCounts.size(); i++) {
				if (!used[i] && isMatch(newspaperLetterCounts.get(i), targetCount)) {
					used[i] = true;
					found = true;
					break;
				}
			}

			return found;
		}

		return false;
	}

	private boolean isMatch(Map<Character, Integer> source, Map<Character, Integer> target) {
		if (source.size() != target.size()) {
			return false;
		}

		for (Map.Entry<Character, Integer> entry : source.entrySet()) {
			if (!target.containsKey(entry.getKey()) || target.get(entry.getKey()) != entry.getValue()) {
				return false;
			}
		}
		return true;
	}
}
