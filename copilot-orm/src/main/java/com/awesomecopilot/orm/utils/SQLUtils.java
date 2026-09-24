package com.awesomecopilot.orm.utils;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.exception.SqlParseException;
import com.awesomecopilot.common.lang.utils.StringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * ORM 的 SQL 工具集(自 21.0.8 起合并了原 commons-lang 的 SqlUtils)：
 * <ol>
 * <li>{@link #build(String)}——修复 Velocity 渲染后残缺的 WHERE/AND；</li>
 * <li>{@link #generateCountSql(String)}——为分页生成 count SQL；</li>
 * <li>{@link #addDeleteTenantIdCondition(String)}——供 StatementInspector 追加
 * deleted / tenant_id 条件。</li>
 * </ol>
 * 三组转换各自使用有上限的 Caffeine 缓存(线程安全、自动淘汰, 不再需要手工 clear)。
 */
public class SQLUtils {
	
	private static final Logger log = LoggerFactory.getLogger(SQLUtils.class);
	private static final Cache<String, String> SQL_CACHE = Caffeine.newBuilder().maximumSize(2000).build();
	private static final Cache<String, String> COUNT_SQL_CACHE = Caffeine.newBuilder().maximumSize(1000).build();
	private static final Cache<String, String> TENANT_SQL_CACHE = Caffeine.newBuilder().maximumSize(2000).build();
	
	/**
	 * copilot-orm是否启用逻辑删除自动过滤, JpaDao 初始化时调用
	 * {@link #configureLogicalDelete(boolean, String)} 设置
	 */
	private static volatile boolean logicalDeleteEnabled = false;
	/**
	 * 逻辑删除字段名
	 */
	private static volatile String logicalDeleteField = "deleted";
	
	public static void configureLogicalDelete(boolean enabled, String field) {
		logicalDeleteEnabled = enabled;
		logicalDeleteField = isNotBlank(field) ? field : "deleted";
		//已按旧配置改写过的SQL缓存必须作废, 否则配置变更后仍返回旧产物
		TENANT_SQL_CACHE.invalidateAll();
	}
	
	public static boolean isLogicalDeleteEnabled() {
		return logicalDeleteEnabled;
	}
	
	public static String getLogicalDeleteField() {
		return logicalDeleteField;
	}
	
	/**
	 * 字面量占位符哨兵字符(不会出现在正常SQL/空白里), build 内部用
	 */
	private static final char LITERAL_MARK = '\u0001';
	
	public static String build(String rawSql) {
		//Caffeine 的 get(key, loader) 原子加载, 消灭原 containsKey+get+put 的竞态
		return SQL_CACHE.get(rawSql, SQLUtils::doBuild);
	}
	
	private static String doBuild(String rawSql) {
		
		/*
		 * ① 字符串字面量保护: 'xxx' 的内容(含其中的 select/from/and 等单词、多余空格)先换成
		 * 占位符再进修复管线, 结尾还原。否则关键字小写/替换会改写字面量数据本身。
		 */
		List<String> literals = new ArrayList<>();
		String masked = maskStringLiterals(rawSql, literals);
		
		String result = unmask(fixMaskedSql(masked), literals);
		String normalizedOriginal = unmask(normalizeWhitespace(masked), literals);
		
		/*
		 * ② 冒烟校验: 只有当"原始SQL本身合法、修复结果反而解析失败"时才判定是修复把SQL改坏,
		 * 回退原始SQL并告警(原始就不合法正是本工具要修的对象, 修完仍不合法则维持尽力修复)。
		 */
		if (parseable(normalizedOriginal) && !parseable(result)) {
			log.warn("SQLUtils.build 修复后的SQL无法解析, 已回退使用原始SQL. 修复结果: [{}] 原始SQL: [{}]",
					result, normalizedOriginal);
			result = normalizedOriginal;
		}
		
		return result;
	}
	
	/**
	 * 对"字面量已遮蔽"的SQL执行修复管线(不含缓存/遮蔽/还原/校验), 子查询递归也走这里,
	 * 保证占位符只在最外层 build() 被还原一次。
	 */
	private static String fixMaskedSql(String maskedSql) {
		String sql = normalizeWhitespace(maskedSql);
		String result;
		
		boolean hasJoin = sql.toLowerCase().contains(" join ");
		boolean hasAsAfterParen = sql.toLowerCase().replaceAll("\\s+", "").contains(")as");
		
		if (!hasJoin && !hasAsAfterParen) {
			result = processSimpleSql(sql);
		} else if (!hasAsAfterParen) {
			result = processJoinSql(sql);
		} else {
			result = sql;
		}
		
		// 最终统一格式：去重空格 + 关键字小写
		result = lowercaseKeywords(result).replaceAll("\\s+", " ").trim();
		
		result = result.replaceAll("(?i)\\s+and\\s+and\\b\\s*", " and ");
		
		return result;
	}
	
	private static String normalizeWhitespace(String sql) {
		// 预处理：统一换行/制表符为空格，去重空格
		return sql.trim().replaceAll("[\\t\\n\\r]", " ").replaceAll("\\s+", " ");
	}
	
	/**
	 * 把每个 '...' 字符串字面量的内容替换为占位符 \u0001N\u0001(引号保留), 原文按序收集进
	 * literals。支持 '' 转义(Oracle风格双单引号)。未闭合的引号不遮蔽(尽力而为)。
	 */
	private static String maskStringLiterals(String sql, List<String> literals) {
		StringBuilder sb = new StringBuilder(sql.length());
		int i = 0;
		int n = sql.length();
		while (i < n) {
			char c = sql.charAt(i);
			if (c != '\'') {
				sb.append(c);
				i++;
				continue;
			}
			int j = i + 1;
			StringBuilder content = new StringBuilder();
			boolean closed = false;
			while (j < n) {
				char cj = sql.charAt(j);
				if (cj == '\'') {
					if (j + 1 < n && sql.charAt(j + 1) == '\'') {
						content.append("''");
						j += 2;
						continue;
					}
					closed = true;
					break;
				}
				content.append(cj);
				j++;
			}
			if (!closed) {
				// 引号不闭合, 不敢确定字面量边界, 原样保留
				sb.append(c);
				i++;
				continue;
			}
			sb.append('\'').append(LITERAL_MARK).append(literals.size()).append(LITERAL_MARK).append('\'');
			literals.add(content.toString());
			i = j + 1;
		}
		return sb.toString();
	}
	
	private static String unmask(String masked, List<String> literals) {
		if (literals.isEmpty()) {
			return masked;
		}
		StringBuilder sb = new StringBuilder(masked.length());
		int i = 0;
		while (i < masked.length()) {
			char c = masked.charAt(i);
			if (c == LITERAL_MARK) {
				int end = masked.indexOf(LITERAL_MARK, i + 1);
				if (end != -1) {
					try {
						int idx = Integer.parseInt(masked.substring(i + 1, end));
						if (idx >= 0 && idx < literals.size()) {
							sb.append(literals.get(idx));
							i = end + 1;
							continue;
						}
					} catch (NumberFormatException ignore) {
						// 落单/被改写的占位符, 原样输出
					}
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}
	
	private static boolean parseable(String sql) {
		try {
			CCJSqlParserUtil.parse(sql);
			return true;
		} catch (JSQLParserException | StackOverflowError | RuntimeException e) {
			return false;
		}
	}
	
	private static String processJoinSql(String sql) {
		String cleaned = sql.replaceAll("(?i)where\\s+and\\s+", "where ")
				.replaceAll("(?i)where\\s+or\\s+", "where ");
		cleaned = cleaned.replaceAll("(?i)where\\s+(order by|group by|having|limit|union)\\s+", " $1 ");
		cleaned = cleaned.replaceAll("(?i)\\s+where\\s*$", "");
		return cleaned;
	}
	
	private static String processSimpleSql(String sql) {
		// 预清理（保留之前有效的部分）
		String cleaned = sql
				.replaceAll("(?i)\\bwhere\\s+(and|or)\\b\\s*", "where ")
				.replaceAll("(?i)\\bwhere\\s+and\\s+and\\b", "where ")
				.replaceAll("(?i)\\s+(and|or)\\s+(and|or)\\b", " $1 ")
				//第二个 and 必须带 \b: 否则 "and android = 2" 会命中 "and and", 列名被吃掉前缀
				.replaceAll("(?i)\\s+and\\s+and\\b\\s*", " and ");
		
		String sqlWithCorrectTable = fixTableAlias(cleaned);
		
		List<String> subs = new ArrayList<>();
		String mainSql = extractSubqueries(sqlWithCorrectTable, subs);
		String fixedMain = fixWhereAnd(mainSql);
		
		for (int i = 0; i < subs.size(); i++) {
			//子查询里带着外层遮蔽好的字面量占位符, 不能再走 build()(会二次遮蔽/还原/污染缓存),
			//只做修复管线本身
			String processedSub = fixMaskedSql(subs.get(i));
			fixedMain = fixedMain.replace("(@sub" + i + ")", "(" + processedSub + ")");
		}
		
		String lowerFixed = fixedMain.toLowerCase();
		if (lowerFixed.contains("select * (select") && !lowerFixed.contains("select * from (select")) {
			int subPos = lowerFixed.indexOf("select * (select") + 8;
			fixedMain = fixedMain.substring(0, subPos) + "from " + fixedMain.substring(subPos);
		}
		
		String cleanedFinal = cleanEmptyWhere(fixedMain);
		//必须带词边界: 裸 "from" 会把 date_from/time_from 等含关键字子串的列名插入空格改坏
		cleanedFinal = cleanedFinal.replaceAll("\\bfrom\\b", " from").replaceAll("\\s+", " ").trim();
		
		return cleanedFinal;
	}
	
	private static String cleanEmptyWhere(String sql) {
		String lowerSql = sql.toLowerCase();
		int whereIdx = lowerSql.indexOf("where ");
		if (whereIdx == -1) return sql;
		
		int whereContentStart = whereIdx + 6;
		String whereContent = sql.substring(whereContentStart).trim();
		String lowerWhereContent = whereContent.toLowerCase();
		
		if (whereContent.isEmpty()
				|| startsWithKeyword(lowerWhereContent, "and")
				|| startsWithKeyword(lowerWhereContent, "or")
				|| lowerWhereContent.startsWith("order by")
				|| lowerWhereContent.startsWith("group by")
				|| lowerWhereContent.startsWith("having")
				|| lowerWhereContent.startsWith("limit")
				|| lowerWhereContent.startsWith("union")) {
			String suffix = whereContent.replaceAll("^\\s*(and|or)\\b\\s*", "").trim();
			return sql.substring(0, whereIdx).trim() + (suffix.isEmpty() ? "" : " " + suffix);
		}
		return sql;
	}
	
	/**
	 * 判断 s 是否以完整关键词 keyword 开头(后跟空格或到此为止)。
	 * 裸 startsWith("or")/"and" 会把 orderno、android 这类列名前缀误当成连接词剥掉。
	 */
	private static boolean startsWithKeyword(String s, String keyword) {
		if (!s.startsWith(keyword)) {
			return false;
		}
		return s.length() == keyword.length() || Character.isWhitespace(s.charAt(keyword.length()));
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
	
	// ... 其他方法保持不变（processSimpleSql、processJoinSql、fixTableAlias、cleanEmptyWhere、extractSubqueries、fixWhereAnd、skipSpaces、getNextWord、lowercaseKeywords）...
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
			
			String connector = null;
			while (i < conds.length()) {
				if (isKeyword(conds, i, "and")) {
					connector = " and ";
					i += 3;
				} else if (isKeyword(conds, i, "or")) {
					connector = " or ";
					i += 2;
				} else {
					break;
				}
				i = skipSpaces(conds, i);
			}
			
			if (i >= conds.length()) break;
			
			if (!first) {
				if (connector == null) {
					connector = " and ";
				}
				sb.append(connector);
			}
			first = false;
			
			int startCond = i;
			
			// Parse column/identifier
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
			
			// BETWEEN ... AND ...
			String opLc = op.toLowerCase();
			if (opLc.contains("between")) {
				int savedI = i;
				i = skipSpaces(conds, i);
				if (isKeyword(conds, i, "and")) {
					i += 3;
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
	
	/**
	 * 严格判断当前位置是否为完整的关键词（词边界保护）
	 */
	private static boolean isKeyword(String s, int pos, String keyword) {
		String lower = s.toLowerCase();
		int kwLen = keyword.length();
		if (pos + kwLen > s.length()) return false;
		if (!lower.substring(pos, pos + kwLen).equals(keyword.toLowerCase())) return false;
		
		// 前边界
		if (pos > 0) {
			char prev = s.charAt(pos - 1);
			if (Character.isLetterOrDigit(prev) || prev == '_' || prev == '.') {
				return false;
			}
		}
		
		// 后边界
		int after = pos + kwLen;
		if (after < s.length()) {
			char next = s.charAt(after);
			if (Character.isLetterOrDigit(next) || next == '_' || next == '.') {
				return false;
			}
		}
		
		return true;
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
	
	/**
	 * 清空本类全部转换缓存。配置热更新或单元测试隔离时调用。
	 */
	public static void clearCache() {
		SQL_CACHE.invalidateAll();
		COUNT_SQL_CACHE.invalidateAll();
		TENANT_SQL_CACHE.invalidateAll();
	}
	
	/**
	 * 给定任意的select语句, 生成对应的count语句（自 commons-lang SqlUtils 合并而来）
	 */
	public static String generateCountSql(String originalQuerySql) {
		//注意: 不校验 originalQuerySql 是否为 null 与 SqlParseException 语义均保持原实现
		return COUNT_SQL_CACHE.get(originalQuerySql, SQLUtils::doGenerateCountSql);
	}

	private static String doGenerateCountSql(String originalQuerySql) {
		try {
			// 解析原始SQL查询
			Statement statement = CCJSqlParserUtil.parse(originalQuerySql);

			/*
			 * 统一走派生表包裹: select count(*) from ( 原查询 ) copilot_count_t。
			 * 这样对三种旧实现会算错的形态都语义正确:
			 *  - GROUP BY: 旧实现替换 selectItems 生成 "count(*) ... GROUP BY dept" 返回每组一行,
			 *    getSingleResult() 抛 NonUniqueResultException; 包裹后数的是分组数;
			 *  - DISTINCT: 旧实现 count 全部行(总数偏大); 包裹后数去重行;
			 *  - UNION/SetOperationList: 旧实现抛 SqlParseException; 包裹后正确。
			 * 包裹前剥离最外层 ORDER BY / LIMIT / OFFSET(它们对 count 无意义), 子查询里的
			 * ORDER BY/LIMIT 是业务语义, 不动。
			 */
			if (statement instanceof Select select) {
				select.setOrderByElements(null);
				select.setLimit(null);
				select.setOffset(null);
				return "select count(*) from (" + select + ") copilot_count_t";
			}
		} catch (JSQLParserException e) {
			throw new SqlParseException(originalQuerySql, e);
		}
		throw new SqlParseException(originalQuerySql);
	}
	
	/**
	 * 处理所有类型的SQL语句，动态添加deleted=0和tenant_id条件（自 commons-lang SqlUtils 合并而来）。
	 * 供 DeletedTenantIdConditionInterceptor（Hibernate StatementInspector）调用。
	 * <p>
	 * 无法解析、或非 Select/Update/Delete 语句时<b>原样返回</b>并告警, 不再抛异常中断查询
	 * （旧实现在 UNION 等场景直接抛 SqlParseException, 会让任何 UNION 查询失败）。
	 */
	public static String addDeleteTenantIdCondition(String originalQuerySql) {
		Long tenantId = ThreadContext.get("tenantId");
		String sqlCacheKey = tenantId + "\u0000" + originalQuerySql;
		return TENANT_SQL_CACHE.get(sqlCacheKey, k -> doAddDeleteTenantIdCondition(originalQuerySql, tenantId));
	}
	
	private static String doAddDeleteTenantIdCondition(String originalQuerySql, Long tenantId) {
		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(originalQuerySql);
		} catch (JSQLParserException | RuntimeException e) {
			//解析不了就不冒险改写, 交给数据库报错
			log.warn("SQLUtils.addDeleteTenantIdCondition 无法解析SQL, 已原样放行: {}", originalQuerySql, e);
			return originalQuerySql;
		}
		
		try {
			// 评审报告 P0-7: 改为 AST 级递归改写——每张表(含子查询/派生表/UNION 分支内的表)
			// 各自追加 带限定符(deleted = 0 / tenant_id = N) 的条件;
			// "是否已有该条件"按 AST 判断(字符串字面量里的 deleted=1 不再误判)。
			if (statement instanceof Select select) {
				new TableConditionRewriter(tenantId).dispatch(select);
				return select.toString();
			}
			if (statement instanceof Update update) {
				// 评审 F5: 多表 UPDATE(update t1 join t2 ...) 对每张参与表都追加, 不只主表
				List<net.sf.jsqlparser.schema.Table> tables = new ArrayList<>();
				tables.add(update.getTable());
				if (update.getStartJoins() != null) {
					update.getStartJoins().forEach(j -> addIfTable(tables, j.getRightItem()));
				}
				if (update.getJoins() != null) {
					update.getJoins().forEach(j -> addIfTable(tables, j.getRightItem()));
				}
				for (net.sf.jsqlparser.schema.Table t : tables) {
					update.setWhere(combine(update.getWhere(),
							conditionsFor(qualifier(t), tenantId, update.getWhere())));
				}
				return update.toString();
			}
			if (statement instanceof Delete delete) {
				List<net.sf.jsqlparser.schema.Table> tables = new ArrayList<>();
				tables.add(delete.getTable());
				if (delete.getTables() != null) {
					tables.addAll(delete.getTables());
				}
				if (delete.getJoins() != null) {
					delete.getJoins().forEach(j -> addIfTable(tables, j.getRightItem()));
				}
				for (net.sf.jsqlparser.schema.Table t : tables) {
					delete.setWhere(combine(delete.getWhere(),
							conditionsFor(qualifier(t), tenantId, delete.getWhere())));
				}
				return delete.toString();
			}
		} catch (RuntimeException e) {
			log.warn("SQLUtils.addDeleteTenantIdCondition 追加条件失败, 已原样放行: {}", originalQuerySql, e);
			return originalQuerySql;
		}
		return originalQuerySql;
	}
	
	/** join 右项可能是派生表(ParenthesedSelect), 只收集真实表 */
	private static void addIfTable(List<net.sf.jsqlparser.schema.Table> out, FromItem fromItem) {
		if (fromItem instanceof net.sf.jsqlparser.schema.Table table) {
			out.add(table);
		}
	}
	
	/** 表的限定符: 有别名用别名, 否则用表名(与 Column 前缀匹配的一方) */
	private static String qualifier(net.sf.jsqlparser.schema.Table table) {
		if (table == null) {
			return null;
		}
		return table.getAlias() != null && isNotBlank(table.getAlias().getName())
				? table.getAlias().getName() : table.getName();
	}
	
	/**
	 * 为限定符 qual 生成缺失的过滤条件文本; 已有比较(qual.<field> 或无表前缀的 <field>) 则跳过。
	 * qual 为 null(函数/未知来源)时不加条件, 保守放行。
	 */
	private static String conditionsFor(String qual, Long tenantId, Expression where) {
		if (qual == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (logicalDeleteEnabled && !hasComparisonOn(where, logicalDeleteField, qual)) {
			sb.append(qual).append('.').append(logicalDeleteField).append(" = 0");
		}
		if (tenantId != null && !hasComparisonOn(where, "tenant_id", qual)) {
			if (sb.length() > 0) {
				sb.append(" AND ");
			}
			sb.append(qual).append(".tenant_id = ").append(tenantId);
		}
		return sb.toString();
	}
	
	/** 表达式树里是否存在对列 field 的比较(限定符匹配 qual 或无表前缀; qual 为 null 时不比较前缀) */
	private static boolean hasComparisonOn(Expression expr, String field, String qual) {
		if (expr == null) {
			return false;
		}
		ColumnFinder finder = new ColumnFinder(field, qual);
		expr.accept(finder);
		return finder.found;
	}
	
	/** 原 WHERE 与新增条件合并; 原条件含 OR 时先整体括号, 避免 AND 优先级吃进 OR 分支 */
	private static Expression combine(Expression original, String newCondition) {
		if (newCondition.isEmpty()) {
			return original;
		}
		try {
			net.sf.jsqlparser.expression.Expression added = CCJSqlParserUtil.parseCondExpression(newCondition);
			if (original == null) {
				return added;
			}
			String originalText = original.toString();
			String combined = originalText.toLowerCase().matches("(?s).*\\bor\\b.*")
					? "(" + originalText + ") AND " + newCondition
					: originalText + " AND " + newCondition;
			return CCJSqlParserUtil.parseCondExpression(combined);
		} catch (JSQLParserException e) {
			log.warn("combine 新过滤条件失败, 保留原条件: {}", newCondition, e);
			return original;
		}
	}
	
	/**
	 * 递归改写器: 对每一层 PlainSelect 收集本层可见的表(含括号连接组), 追加各自的过滤条件,
	 * 并深入 FROM/WHERE/HAVING/JOIN ON/UNION/CTE 里的子查询。
	 */
	private static class TableConditionRewriter implements SelectVisitor {
		
		private final Long tenantId;
		
		TableConditionRewriter(Long tenantId) {
			this.tenantId = tenantId;
		}

		/** Select 抽象类型分发: 交给 accept 回调到具体 visit 方法 */
		public void dispatch(Select select) {
			if (select != null) {
				select.accept(this);
			}
		}
		
		@Override
		public void visit(PlainSelect plainSelect) {
			applyToPlainSelect(plainSelect);
		}
		
		@Override
		public void visit(ParenthesedSelect parenthesedSelect) {
			dispatch(parenthesedSelect.getSelect());
		}
		
		@Override
		public void visit(SetOperationList setOperationList) {
			for (Select branch : setOperationList.getSelects()) {
					dispatch(branch);
			}
		}
		
		@Override
		public void visit(WithItem withItem) {
			dispatch(withItem.getSelect());
		}
		
		@Override
		public void visit(Values values) {
			// 常量行, 无表
		}
		
		@Override
		public void visit(LateralSubSelect lateralSubSelect) {
			dispatch(lateralSubSelect.getSelect());
		}
		
		@Override
		public void visit(TableStatement tableStatement) {
			// 形如 select * from only t 的表语句, 保守不动
		}
		
		private void applyToPlainSelect(PlainSelect plainSelect) {
			// 1. CTE(WITH)先各自改写
			if (plainSelect.getWithItemsList() != null) {
				for (WithItem withItem : plainSelect.getWithItemsList()) {
						dispatch(withItem.getSelect());
				}
			}
			// 2. 收集本层直接可见的表(括号连接组摊平), 派生表/侧身查询递归
			List<net.sf.jsqlparser.schema.Table> visible = new ArrayList<>();
			collectTables(plainSelect.getFromItem(), visible);
			if (plainSelect.getJoins() != null) {
				for (Join join : plainSelect.getJoins()) {
					collectTables(join.getRightItem(), visible);
				}
			}
			// 评审 F4: WITH 定义的 CTE 名不是物理表, 对它追加 x.deleted 会生成引用不存在列的SQL
			java.util.Set<String> cteNames = new java.util.HashSet<>();
			if (plainSelect.getWithItemsList() != null) {
				for (WithItem wi : plainSelect.getWithItemsList()) {
					if (wi.getAlias() != null && isNotBlank(wi.getAlias().getName())) {
						cteNames.add(wi.getAlias().getName().toLowerCase());
					}
				}
			}
			// 已有条件按合并后的整体 WHERE 判断(逐表追加时用当前累积值)
			for (net.sf.jsqlparser.schema.Table table : visible) {
				if (cteNames.contains(table.getName() == null ? "" : table.getName().toLowerCase())) {
					continue;
				}
				String qual = qualifier(table);
				plainSelect.setWhere(combine(plainSelect.getWhere(),
						conditionsFor(qual, tenantId, plainSelect.getWhere())));
			}
			// 3. 递归 WHERE/HAVING/JOIN ON/SELECT 项里的子查询
			ExpressionSubqueryProbe probe = new ExpressionSubqueryProbe(new TableConditionRewriter(tenantId));
			if (plainSelect.getWhere() != null) {
				plainSelect.getWhere().accept(probe);
			}
			if (plainSelect.getHaving() != null) {
				plainSelect.getHaving().accept(probe);
			}
			if (plainSelect.getJoins() != null) {
				for (Join join : plainSelect.getJoins()) {
					if (join.getOnExpressions() != null) {
						join.getOnExpressions().forEach(on -> on.accept(probe));
					}
				}
			}
			if (plainSelect.getSelectItems() != null) {
				plainSelect.getSelectItems().forEach(item -> {
					if (item.getExpression() != null) {
						item.getExpression().accept(probe);
					}
				});
			}
		}
		
		private void collectTables(FromItem fromItem, List<net.sf.jsqlparser.schema.Table> out) {
			if (fromItem == null) {
				return;
			}
			if (fromItem instanceof net.sf.jsqlparser.schema.Table table) {
				out.add(table);
				return;
			}
			if (fromItem instanceof ParenthesedFromItem pfi) {
				// (a JOIN b ...) 括号组: 内部表仍在本层作用域, 摊平收集
				collectTables(pfi.getFromItem(), out);
				if (pfi.getJoins() != null) {
					for (Join join : pfi.getJoins()) {
						collectTables(join.getRightItem(), out);
					}
				}
				return;
			}
			if (fromItem instanceof ParenthesedSelect ps) {
				// 派生表: 作为独立查询层递归改写
					dispatch(ps.getSelect());
			}
		}
	}
	
	/** 只收集"是否存在对 field 的比较"(=, <>, >, <, >=, <=)的比较器, 表限定符需匹配 qual 或未限定 */
	private static class ColumnFinder extends ExpressionVisitorAdapter {
		
		private final String field;
		private final String qual;
		boolean found = false;
		
		ColumnFinder(String field, String qual) {
			this.field = field;
			this.qual = qual;
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.EqualsTo equalsTo) {
			check(equalsTo.getLeftExpression());
			check(equalsTo.getRightExpression());
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.NotEqualsTo notEqualsTo) {
			check(notEqualsTo.getLeftExpression());
			check(notEqualsTo.getRightExpression());
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.GreaterThan gt) {
			check(gt.getLeftExpression());
			check(gt.getRightExpression());
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals gte) {
			check(gte.getLeftExpression());
			check(gte.getRightExpression());
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.MinorThan lt) {
			check(lt.getLeftExpression());
			check(lt.getRightExpression());
		}
		
		@Override
		public void visit(net.sf.jsqlparser.expression.operators.relational.MinorThanEquals lte) {
			check(lte.getLeftExpression());
			check(lte.getRightExpression());
		}
		
		private void check(Expression expr) {
			if (found || !(expr instanceof net.sf.jsqlparser.schema.Column column)) {
				return;
			}
			if (!field.equalsIgnoreCase(column.getColumnName())) {
				return;
			}
			String colQual = column.getTable() == null ? null : qualifier(column.getTable());
			// 未限定列视为匹配本表(单表场景旧行为); 有前缀则要求一致
			if (colQual == null || colQual.isEmpty() || colQual.equalsIgnoreCase(qual)) {
				found = true;
			}
		}
	}
	
	/**
	 * 表达式遍历探针: 遇到任何子查询(ParenthesedSelect: IN (...)/EXISTS (...)/标量子查询)
	 * 就把它交给 rewriter 递归改写。不覆盖函数参数里的极端形态, 该类 SQL 交由调用方告警策略处理。
	 */
	private static class ExpressionSubqueryProbe extends ExpressionVisitorAdapter {
		
		private final SelectVisitor rewriter;
		
		ExpressionSubqueryProbe(SelectVisitor rewriter) {
			this.rewriter = rewriter;
			setSelectVisitor(rewriter);
		}
		
		@Override
		public void visit(net.sf.jsqlparser.statement.select.ParenthesedSelect parenthesedSelect) {
			((TableConditionRewriter) rewriter).dispatch(parenthesedSelect.getSelect());
			// 继续深入子查询自身内部由 rewriter 处理
		}
		
		@Override
		public void visit(Select select) {
			select.accept(rewriter);
		}
	}
}
