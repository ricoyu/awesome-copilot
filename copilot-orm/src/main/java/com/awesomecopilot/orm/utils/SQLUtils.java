package com.awesomecopilot.orm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 自动修复SQL中得语法错误, 比如少了where关键字会自动添加, where后面条件多了and关键字会自动去掉, 迎合动态SQL需求
 * <p/>
 * Copyright: Copyright (c) 2026-03-02 8:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SQLUtils {
	
	private static final Logger log = LoggerFactory.getLogger(SQLUtils.class);
	private static final HashMap<String, String> SQL_CACHE = new HashMap<>();
	
	public static String build(String rawSql) {
		if (SQL_CACHE.containsKey(rawSql)) {
			return SQL_CACHE.get(rawSql);
		}
		
		// 预处理：统一换行/制表符为空格，去重空格
		String sql = rawSql.trim().replaceAll("[\\t\\n\\r]", " ").replaceAll("\\s+", " ");
		String result;
		
		boolean hasJoin = sql.toLowerCase().contains(" join ");
		boolean hasAsAfterParen = sql.toLowerCase().replaceAll("\\s+", "").contains(")as");
		
		if (!hasJoin && !hasAsAfterParen) {
			result = processSimpleSql(sql);
		} else if (!hasAsAfterParen) {
			result = processJoinSql(sql);
		} else {
			result = rawSql;
		}
		
		// 最终统一格式：去重空格 + 关键字小写
		result = lowercaseKeywords(result).replaceAll("\\s+", " ").trim();
		
		// ====================== 你提出的最终清理 ======================
		// 彻底消灭所有 “and and” 和 “and and (” 场景（放在最后，安全无副作用）
		result = result.replaceAll("(?i)\\s+and\\s+and\\s*", " and ");
		// ============================================================
		
		SQL_CACHE.put(rawSql, result);
		return result;
	}
	
	private static String processSimpleSql(String sql) {
		// 预清理（保留之前有效的部分）
		String cleaned = sql
				.replaceAll("(?i)\\bwhere\\s+(and|or)\\b\\s*", "where ")
				.replaceAll("(?i)\\bwhere\\s+and\\s+and\\b", "where ")
				.replaceAll("(?i)\\s+(and|or)\\s+(and|or)\\b", " $1 ")
				.replaceAll("(?i)\\s+and\\s+and\\s*", " and ");
		
		String sqlWithCorrectTable = fixTableAlias(cleaned);
		
		List<String> subs = new ArrayList<>();
		String mainSql = extractSubqueries(sqlWithCorrectTable, subs);
		String fixedMain = fixWhereAnd(mainSql);
		
		for (int i = 0; i < subs.size(); i++) {
			String processedSub = build(subs.get(i));
			fixedMain = fixedMain.replace("(@sub" + i + ")", "(" + processedSub + ")");
		}
		
		String lowerFixed = fixedMain.toLowerCase();
		if (lowerFixed.contains("select * (select") && !lowerFixed.contains("select * from (select")) {
			int subPos = lowerFixed.indexOf("select * (select") + 8;
			fixedMain = fixedMain.substring(0, subPos) + "from " + fixedMain.substring(subPos);
		}
		
		String cleanedFinal = cleanEmptyWhere(fixedMain);
		cleanedFinal = cleanedFinal.replaceAll("from", " from").replaceAll("\\s+", " ").trim();
		
		return cleanedFinal;
	}
	
	private static String processJoinSql(String sql) {
		String cleaned = sql.replaceAll("(?i)where\\s+and\\s+", "where ")
				.replaceAll("(?i)where\\s+or\\s+", "where ");
		cleaned = cleaned.replaceAll("(?i)where\\s+(order by|group by|having|limit|union)\\s+", " $1 ");
		cleaned = cleaned.replaceAll("(?i)\\s+where\\s*$", "");
		return cleaned;
	}
	
	private static String fixTableAlias(String sql) {
		String lowerSql = sql.toLowerCase();
		int fromIdx = lowerSql.indexOf("from ");
		if (fromIdx == -1) return sql;
		
		int tableStart = fromIdx + 5;
		int tableEnd = tableStart;
		while (tableEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(tableEnd))
				|| sql.charAt(tableEnd) == '_' || sql.charAt(tableEnd) == '`')) {
			tableEnd++;
		}
		
		tableEnd = skipSpaces(sql, tableEnd);
		
		boolean hasAlias = false;
		int aliasStart = tableEnd;
		if (tableEnd < sql.length()) {
			String remaining = sql.substring(tableEnd).toLowerCase();
			if (remaining.startsWith("as ")) {
				hasAlias = true;
				tableEnd += 3;
				tableEnd = skipSpaces(sql, tableEnd);
				while (tableEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(tableEnd))
						|| sql.charAt(tableEnd) == '_' || sql.charAt(tableEnd) == '`')) {
					tableEnd++;
				}
			} else if (Character.isLetter(sql.charAt(tableEnd)) || sql.charAt(tableEnd) == '`') {
				hasAlias = true;
				while (tableEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(tableEnd))
						|| sql.charAt(tableEnd) == '_' || sql.charAt(tableEnd) == '`')) {
					tableEnd++;
				}
			}
		}
		
		tableEnd = skipSpaces(sql, tableEnd);
		if (hasAlias && tableEnd < sql.length() && sql.substring(tableEnd).toLowerCase().startsWith("where")) {
			return sql;
		}
		return sql;
	}
	
	private static String cleanEmptyWhere(String sql) {
		String lowerSql = sql.toLowerCase();
		int whereIdx = lowerSql.indexOf("where ");
		if (whereIdx == -1) return sql;
		
		int whereContentStart = whereIdx + 6;
		String whereContent = sql.substring(whereContentStart).trim();
		String lowerWhereContent = whereContent.toLowerCase();
		
		if (whereContent.isEmpty()
				|| lowerWhereContent.startsWith("and")
				|| lowerWhereContent.startsWith("or")
				|| lowerWhereContent.startsWith("order by")
				|| lowerWhereContent.startsWith("group by")
				|| lowerWhereContent.startsWith("having")
				|| lowerWhereContent.startsWith("limit")
				|| lowerWhereContent.startsWith("union")) {
			String suffix = whereContent.replaceAll("^\\s*(and|or)\\s*", "").trim();
			return sql.substring(0, whereIdx).trim() + (suffix.isEmpty() ? "" : " " + suffix);
		}
		return sql;
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
		
		String lowerSql = sql.toLowerCase();
		int tableEnd = fromIdx + 5;
		while (tableEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(tableEnd))
				|| sql.charAt(tableEnd) == '_' || sql.charAt(tableEnd) == '`')) {
			tableEnd++;
		}
		tableEnd = skipSpaces(sql, tableEnd);
		int aliasEnd = tableEnd;
		boolean hasAlias = false;
		if (aliasEnd < sql.length()) {
			if (lowerSql.substring(aliasEnd).startsWith("as ")) {
				hasAlias = true;
				aliasEnd += 3;
				aliasEnd = skipSpaces(sql, aliasEnd);
				while (aliasEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(aliasEnd))
						|| sql.charAt(aliasEnd) == '_' || sql.charAt(aliasEnd) == '`')) {
					aliasEnd++;
				}
			} else if (Character.isLetter(sql.charAt(aliasEnd)) || sql.charAt(aliasEnd) == '`') {
				hasAlias = true;
				while (aliasEnd < sql.length() && (Character.isLetterOrDigit(sql.charAt(aliasEnd))
						|| sql.charAt(aliasEnd) == '_' || sql.charAt(aliasEnd) == '`')) {
					aliasEnd++;
				}
			}
		}
		aliasEnd = skipSpaces(sql, aliasEnd);
		if (hasAlias && aliasEnd < sql.length() && lowerSql.substring(aliasEnd).startsWith("where")) {
			return sql;
		}
		
		tableEnd = fromIdx + 5;
		while (tableEnd < sql.length() && !Character.isWhitespace(sql.charAt(tableEnd))) {
			tableEnd++;
		}
		while (tableEnd < sql.length() && Character.isWhitespace(sql.charAt(tableEnd))) {
			tableEnd++;
		}
		
		String afterTable = sql.substring(tableEnd);
		String trimmedLower = afterTable.trim().toLowerCase();
		
		if (trimmedLower.isEmpty() ||
				trimmedLower.startsWith("order by") ||
				trimmedLower.startsWith("group by") ||
				trimmedLower.startsWith("having") ||
				trimmedLower.startsWith("limit") ||
				trimmedLower.startsWith("union") ||
				trimmedLower.startsWith("where")) {
			if (trimmedLower.startsWith("where")) {
				String afterWhere = afterTable.trim().substring(5).trim();
				String afterWhereLower = afterWhere.toLowerCase();
				if (afterWhereLower.isEmpty() || afterWhereLower.startsWith("order by") || afterWhereLower.startsWith(
						"group by") ||
						afterWhereLower.startsWith("having") || afterWhereLower.startsWith("limit") || afterWhereLower.startsWith("union")) {
					String prefix = sql.substring(0, tableEnd);
					String suffix = afterTable.replaceAll("(?i)^where\\s*", " ").replaceAll("\\s+", " ").trim();
					return prefix + " " + suffix;
				}
			} else {
				return sql;
			}
		}
		
		String prefix = sql.substring(0, tableEnd);
		
		String lower = sql.toLowerCase();
		int orderIdx = lower.indexOf(" order by ", tableEnd);
		int groupIdx = lower.indexOf(" group by ", tableEnd);
		int havingIdx = lower.indexOf(" having ", tableEnd);
		int limitIdx = lower.indexOf(" limit ", tableEnd);
		int unionIdx = lower.indexOf(" union ", tableEnd);
		
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
		if (conds.trim().isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		int i = 0;
		
		while (i < conds.length()) {
			i = skipSpaces(conds, i);
			if (i >= conds.length()) break;
			
			while (i < conds.length()) {
				String lowerSub = conds.substring(i).toLowerCase();
				if (lowerSub.startsWith("and") || lowerSub.startsWith("or")) {
					i += lowerSub.startsWith("and") ? 3 : 2;
					i = skipSpaces(conds, i);
				} else {
					break;
				}
			}
			
			if (i >= conds.length()) break;
			
			if (!first) {
				sb.append(" and ");
			}
			first = false;
			
			int startCond = i;
			
			while (i < conds.length() && (Character.isLetterOrDigit(conds.charAt(i)) ||
					conds.charAt(i) == '_' || conds.charAt(i) == ':' || conds.charAt(i) == '"' ||
					conds.charAt(i) == '`' || conds.charAt(i) == '.')) {
				i++;
			}
			
			i = skipSpaces(conds, i);
			
			String op = "";
			if (i < conds.length()) {
				char c = conds.charAt(i);
				if ("=><!".indexOf(c) != -1) {
					op = String.valueOf(c);
					i++;
					if (i < conds.length() && conds.charAt(i) == '=') {
						op += "=";
						i++;
					}
				} else {
					String word = getNextWord(conds, i);
					String wordLc = word.toLowerCase();
					if ("in like is between not exists".contains(wordLc)) {
						op = word;
						i += word.length();
						if (wordLc.equals("not")) {
							int tempI = skipSpaces(conds, i);
							String nextWord = getNextWord(conds, tempI);
							String nextLc = nextWord.toLowerCase();
							if ("in like between exists".contains(nextLc)) {
								op += " " + nextWord;
								i = tempI + nextWord.length();
							}
						} else if (wordLc.equals("is")) {
							int tempI = skipSpaces(conds, i);
							String nextWord = getNextWord(conds, tempI);
							if (nextWord.toLowerCase().equals("not")) {
								op += " " + nextWord;
								i = tempI + nextWord.length();
							}
						}
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
			
			// Handle special case for BETWEEN ... AND ...
			String opLc = op.toLowerCase();
			if (opLc.contains("between")) {
				int savedI = i;
				i = skipSpaces(conds, i);
				String nextWord = getNextWord(conds, i);
				if (nextWord.toLowerCase().equals("and")) {
					i += nextWord.length();
					i = skipSpaces(conds, i);
					if (i < conds.length()) {
						char c2 = conds.charAt(i);
						if (c2 == '\'') {
							i++;
							while (i < conds.length() && conds.charAt(i) != '\'') i++;
							if (i < conds.length()) i++;
						} else if (c2 == '(') {
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
				} else {
					i = savedI;
				}
			}
			
			sb.append(conds.substring(startCond, i));
		}
		
		return sb.toString().trim();
	}
	
	private static int skipSpaces(String s, int start) {
		while (start < s.length() && Character.isWhitespace(s.charAt(start))) {
			start++;
		}
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
		String[] kws = {"SELECT", "FROM", "WHERE", "AND", "OR", "IN", "ORDER", "BY",
				"GROUP", "HAVING", "LIMIT", "UNION", "EXISTS", "JOIN", "LEFT", "RIGHT", "INNER", "ON", "NOT"};
		for (String kw : kws) {
			sql = sql.replaceAll("(?i)\\b" + kw + "\\b", kw.toLowerCase());
		}
		return sql;
	}
	
	public static void clearCache() {
		SQL_CACHE.clear();
	}
}