package com.awesomecopilot.orm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SQLUtils4 {

	private static final Logger log = LoggerFactory.getLogger(SQLUtils4.class);

	private static final HashMap<String, String> SQL_CACHE = new HashMap<>();

	public static String build(String rawSql) {
		if (SQL_CACHE.containsKey(rawSql)) {
			String finalSql = SQL_CACHE.get(rawSql);
			//log.debug("从缓存中取出SQL: {} 处理后的版本: {}", rawSql, finalSql);
			return finalSql;
		}
		String sql = rawSql.trim().replaceAll("\\s+", " ");
		List<String> subs = new ArrayList<>();
		String mainSql = extractSubqueries(sql, subs);
		String fixedMain = fixWhereAnd(mainSql);

		for (int i = 0; i < subs.size(); i++) {
			String processedSub = build(subs.get(i));
			fixedMain = fixedMain.replace("(@sub" + i + ")", "(" + processedSub + ")");
		}

		// 修复缺少 FROM 的子查询情况：如果主查询是 "select * (select ...)"，插入 " from "
		String lowerFixed = fixedMain.toLowerCase();
		if (lowerFixed.contains("select * (select") && !lowerFixed.contains("select * from (select")) {
			int subPos = lowerFixed.indexOf("select * (select") + 8;  // "select * " 的长度是9，但调整为插入点
			fixedMain = fixedMain.substring(0, subPos) + "from " + fixedMain.substring(subPos);
		}

		String result = lowercaseKeywords(fixedMain);
		result = result.replaceAll("from", " from");
		result = result.replaceAll("\\s+", " ").trim();

		SQL_CACHE.put(rawSql, result);
		return result;
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

		// 准确找到表名结束位置
		int tableEnd = fromIdx + 5;
		while (tableEnd < sql.length() && !Character.isWhitespace(sql.charAt(tableEnd))) tableEnd++;
		while (tableEnd < sql.length() && Character.isWhitespace(sql.charAt(tableEnd))) tableEnd++;

		String afterTable = sql.substring(tableEnd);
		String trimmedLower = afterTable.trim().toLowerCase();

		// 快速路径：表名后直接是后续子句或 "where" + 子句或单纯 "where"
		if (trimmedLower.isEmpty() ||
				trimmedLower.startsWith("order by") ||
				trimmedLower.startsWith("group by") ||
				trimmedLower.startsWith("having") ||
				trimmedLower.startsWith("limit") ||
				trimmedLower.startsWith("union") ||
				trimmedLower.startsWith("where order by") ||
				trimmedLower.startsWith("where group by") ||
				trimmedLower.startsWith("where having") ||
				trimmedLower.startsWith("where limit") ||
				trimmedLower.startsWith("where union") ||
				trimmedLower.startsWith("where")) {
			// 特殊处理：如果以 "where " 开头，且后面没有真实条件，则去除 "where "
			if (trimmedLower.startsWith("where")) {
				String afterWhere = afterTable.trim().substring(5).trim();
				String afterWhereLower = afterWhere.toLowerCase();
				if (afterWhereLower.isEmpty() || afterWhereLower.startsWith("order by") || afterWhereLower.startsWith("group by") ||
						afterWhereLower.startsWith("having") || afterWhereLower.startsWith("limit") || afterWhereLower.startsWith("union")) {
					String prefix = sql.substring(0, tableEnd);
					String suffix = afterTable.replaceAll("(?i)^where\\s*", " ").replaceAll("\\s+", " ").trim();
					return lowercaseKeywords(prefix + " " + suffix);
				}
			} else {
				return lowercaseKeywords(sql);
			}
		}

		// 原有正常逻辑
		String prefix = sql.substring(0, tableEnd);

		String lower = sql.toLowerCase();
		int orderIdx  = lower.indexOf(" order by ", tableEnd);
		int groupIdx  = lower.indexOf(" group by ", tableEnd);
		int havingIdx = lower.indexOf(" having ", tableEnd);
		int limitIdx  = lower.indexOf(" limit ", tableEnd);
		int unionIdx  = lower.indexOf(" union ", tableEnd);

		int conditionEnd = sql.length();
		for (int idx : new int[]{orderIdx, groupIdx, havingIdx, limitIdx, unionIdx}) {
			if (idx != -1 && idx < conditionEnd) {
				conditionEnd = idx;
			}
		}

		String conds = sql.substring(tableEnd, conditionEnd);
		String suffix = sql.substring(conditionEnd);

		if (conds.toLowerCase().startsWith("where ")) {
			conds = conds.substring(6).trim();
		} else if (conds.toLowerCase().startsWith("where")) {
			conds = conds.substring(5).trim();
		}

		String fixedConds = fixConditions(conds);

		StringBuilder result = new StringBuilder(prefix);
		if (!fixedConds.isEmpty()) {
			result.append(" where ").append(fixedConds);
		}
		result.append(suffix);
		return result.toString();
	}

	private static String fixConditions(String conds) {
		if (conds.trim().isEmpty()) return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		int i = 0;

		while (i < conds.length()) {
			i = skipSpaces(conds, i);
			if (i >= conds.length()) break;

			// 循环跳过连续 and
			while (i < conds.length() && conds.substring(i).toLowerCase().startsWith("and")) {
				i += 3;
				i = skipSpaces(conds, i);
			}

			if (i >= conds.length()) break;

			if (!first) {
				sb.append(" and ");
			}
			first = false;

			int startCond = i;

			// 字段名
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
		return sql;
	}
}