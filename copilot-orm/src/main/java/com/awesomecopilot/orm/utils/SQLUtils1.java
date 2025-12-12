package com.awesomecopilot.orm.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 接近完美的版本, 只有select * from pms_category order by cat_id asc还没通过
 * <p/>
 * Copyright: Copyright (c) 2025-12-11 20:48
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SQLUtils1 {

	public static String build(String rawSql) {
		String sql = rawSql.trim().replaceAll("\\s+", " ");
		List<String> subs = new ArrayList<>();
		String mainSql = extractSubqueries(sql, subs);
		String fixedMain = fixWhereAnd(mainSql);

		// 正确写法：递归处理所有子查询
		for (int i = 0; i < subs.size(); i++) {
			String processedSub = build(subs.get(i));  // 递归调用
			fixedMain = fixedMain.replace("(@sub" + i + ")", "(" + processedSub + ")");
		}

		return lowercaseKeywords(fixedMain);
	}

	private static String extractSubqueries(String sql, List<String> subs) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < sql.length()) {
			char c = sql.charAt(i);
			if (c == '(') {
				int j = skipSpaces(sql, i + 1);
				if (j + 6 <= sql.length() && sql.substring(j, j + 6).toLowerCase().equals("select")) {
					int count = 1;
					int start = i;
					i = j + 6;
					while (i < sql.length() && count > 0) {
						char ch = sql.charAt(i);
						if (ch == '(') count++;
						else if (ch == ')') count--;
						i++;
					}
					String subSql = sql.substring(start + 1, i - 1).trim();
					subs.add(subSql);
					sb.append("(@sub").append(subs.size() - 1).append(")");
					continue;
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}

	private static String fixWhereAnd(String sql) {
		int fromIdx = sql.toLowerCase().indexOf("from ");
		if (fromIdx == -1) return sql;

		int tableStart = fromIdx + 5;
		int condStart = sql.indexOf(' ', tableStart);
		if (condStart == -1) condStart = sql.length();
		else condStart++;

		String prefix = sql.substring(0, condStart);

		// 支持多种结束关键字
		String lower = sql.toLowerCase();
		int orderIdx  = lower.indexOf(" order by ", condStart);
		int groupIdx  = lower.indexOf(" group by ", condStart);
		int havingIdx = lower.indexOf(" having ", condStart);
		int limitIdx    = lower.indexOf(" limit ", condStart);
		int unionIdx  = lower.indexOf(" union ", condStart);

		int conditionEnd = sql.length();
		for (int idx : new int[]{orderIdx, groupIdx, havingIdx, limitIdx, unionIdx}) {
			if (idx != -1 && idx < conditionEnd) {
				conditionEnd = idx;
			}
		}

		String conds = sql.substring(condStart, conditionEnd);
		String suffix = sql.substring(conditionEnd);

		if (conds.toLowerCase().startsWith("where ")) {
			conds = conds.substring(6).trim();
		}

		String fixedConds = fixConditions(conds);

		StringBuilder result = new StringBuilder(prefix);
		if (!fixedConds.isEmpty()) {
			result.append("where ").append(fixedConds);
		}
		result.append(suffix);
		return result.toString();
	}

	private static String fixConditions(String conds) {
		if (conds.isEmpty()) return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		int i = 0;

		while (i < conds.length()) {
			i = skipSpaces(conds, i);
			if (i >= conds.length()) break;

			if (conds.substring(i).toLowerCase().startsWith("and")) {
				i += 3;
				i = skipSpaces(conds, i);
				continue;
			}

			if (!first) {
				sb.append(" and ");
			}
			first = false;

			int startCond = i;

			while (i < conds.length() && (Character.isLetterOrDigit(conds.charAt(i)) ||
					conds.charAt(i) == '_' || conds.charAt(i) == ':' || conds.charAt(i) == '"' || conds.charAt(i) == '`')) {
				i++;
			}

			i = skipSpaces(conds, i);

			if (i < conds.length()) {
				char c = conds.charAt(i);
				if ("=><!".indexOf(c) != -1) {
					i++;
					if (i < conds.length() && conds.charAt(i) == '=') i++;
				} else {
					String word = getNextWord(conds, i);
					if ("in like is between not exists".contains(word.toLowerCase())) {
						i += word.length();
					}
				}
			}

			i = skipSpaces(conds, i);

			if (i < conds.length()) {
				char c = conds.charAt(i);
				if (c == '\'') {
					i++;
					while (i < conds.length() && conds.charAt(i) != '\'') i++;
					if (i < conds.length()) i++;
				} else if (c == '(') {
					int count = 1;
					i++;
					while (i < conds.length() && count > 0) {
						if (conds.charAt(i) == '(') count++;
						else if (conds.charAt(i) == ')') count--;
						i++;
					}
				} else {
					while (i < conds.length() && !Character.isWhitespace(conds.charAt(i))) i++;
				}
			}

			sb.append(conds.substring(startCond, i));
		}
		return sb.toString().trim();
	}

	private static int skipSpaces(String s, int start) {
		while (start < s.length() && Character.isWhitespace(s.charAt(start))) start++;
		return start;
	}

	private static String getNextWord(String s, int start) {
		StringBuilder sb = new StringBuilder();
		int i = start;
		while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
			sb.append(s.charAt(i++));
		}
		return sb.toString();
	}

	private static String lowercaseKeywords(String sql) {
		String[] kws = {"SELECT","FROM","WHERE","AND","OR","IN","ORDER","BY","GROUP","HAVING","LIMIT","UNION","EXISTS"};
		for (String kw : kws) {
			sql = sql.replaceAll("(?i)\\b" + kw + "\\b", kw.toLowerCase());
		}
		return sql.trim();
	}
}