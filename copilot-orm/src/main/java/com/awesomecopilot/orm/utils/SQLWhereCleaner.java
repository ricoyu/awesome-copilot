package com.awesomecopilot.orm.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL工具类：统一清理WHERE子句中无效的AND/OR、无意义的WHERE关键字
 * 最终修复：复杂SQL仅替换SELECT查询字段为*，保留FROM/JOIN结构，还原后完全匹配预期
 */
public class SQLWhereCleaner {

	// 定义where后无有效条件的关键字（忽略大小写）
	private static final Set<String> POST_WHERE_KEYWORDS = new HashSet<>(
			Arrays.asList("order", "group", "having", "limit", "union", "")
	);

	// 匹配JOIN关键字（忽略大小写）
	private static final Pattern JOIN_PATTERN = Pattern.compile("(?i)\\bjoin\\b");
	// 匹配子查询（括号内包含SELECT...FROM，忽略大小写）
	private static final Pattern SUBQUERY_PATTERN = Pattern.compile("(?i)\\(\\s*select\\s+.+?\\s+from\\s+.+?\\)");

	// 匹配第一个SELECT子句（捕获：select关键字 + 查询字段 + from及之后到where前的内容）
	// 分组1: select  分组2: 查询字段  分组3: from+表+JOIN  分组4: where及之后
	private static final Pattern SQL_STRUCTURE_PATTERN = Pattern.compile("(?i)^\\s*(select)\\s+(.+?)\\s+(from\\s+.+?)\\s*(where\\s+.+)?$");

	// 匹配最后一个WHERE（忽略大小写）
	private static final Pattern LAST_WHERE_PATTERN = Pattern.compile("(?i)\\bwhere\\b(?!.*\\bwhere\\b)");

	/**
	 * 核心入口方法：复杂SQL仅替换查询字段为*处理WHERE，再还原原始字段，保留所有结构
	 *
	 * @param rawSql 原始SQL
	 * @return 处理后的SQL（完全匹配测试预期）
	 */
	public static String cleanSql(String rawSql) {
		if (rawSql == null || rawSql.trim().isEmpty()) {
			return rawSql;
		}

		String originalSql = rawSql.replaceAll("[\\s\\t\\n\\r]+", " ").trim();
		// 步骤1：判断是否为复杂SQL
		if (isComplexSql(originalSql)) {
			return processComplexSql(originalSql);
		} else {
			// 简单SQL直接清理
			return cleanInvalidWhere(originalSql);
		}
	}

	/**
	 * 处理复杂SQL：仅替换查询字段为*，保留FROM/JOIN，清理后还原
	 */
	private static String processComplexSql(String originalSql) {
		// 步骤1：解析SQL结构，提取各部分内容
		Matcher structureMatcher = SQL_STRUCTURE_PATTERN.matcher(originalSql);
		String selectKeyword = "select";
		String originalSelectFields = "*";
		String fromAndJoinPart = "";
		String whereAndAfterPart = "";

		if (structureMatcher.matches()) {
			selectKeyword = structureMatcher.group(1).trim();
			originalSelectFields = structureMatcher.group(2).trim(); // 原始查询字段
			fromAndJoinPart = structureMatcher.group(3).trim();     // from+表+JOIN
			whereAndAfterPart = structureMatcher.group(4) != null ? structureMatcher.group(4).trim() : ""; // where及之后
		} else {
			// 正则匹配失败时，手动提取关键部分（兜底）
			Matcher lastWhereMatcher = LAST_WHERE_PATTERN.matcher(originalSql);
			int whereIdx = -1;
			if (lastWhereMatcher.find()) {
				whereIdx = lastWhereMatcher.start();
				// 提取from及之前部分
				String beforeWhere = originalSql.substring(0, whereIdx).trim();
				Matcher selectFromMatcher = Pattern.compile("(?i)(select)\\s+(.+?)\\s+(from\\s+.+)").matcher(beforeWhere);
				if (selectFromMatcher.find()) {
					selectKeyword = selectFromMatcher.group(1).trim();
					originalSelectFields = selectFromMatcher.group(2).trim();
					fromAndJoinPart = selectFromMatcher.group(3).trim();
				}
				whereAndAfterPart = originalSql.substring(whereIdx).trim();
			} else {
				// 没有WHERE的情况，直接清理返回
				return cleanInvalidWhere(originalSql);
			}
		}

		// 步骤2：构建简化SQL（仅替换查询字段为*，保留FROM/JOIN）
		String simplifiedSql = String.format("%s * %s %s",
				selectKeyword,
				fromAndJoinPart,
				whereAndAfterPart
		).trim();

		// 步骤3：清理简化SQL中的无效WHERE/AND/OR
		String cleanedSimplifiedSql = cleanInvalidWhere(simplifiedSql);

		// 步骤4：还原原始查询字段（把*替换回原始字段）
		String restoredSql = cleanedSimplifiedSql.replaceFirst(
				"(?i)^\\s*" + selectKeyword + "\\s+\\*\\s+",
				selectKeyword + " " + originalSelectFields + " "
		).trim();

		// 最终清理多余空格，确保格式和预期一致
		return restoredSql.replaceAll("\\s+", " ").trim();
	}

	/**
	 * 判断是否为复杂SQL（包含JOIN或子查询）
	 */
	private static boolean isComplexSql(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return false;
		}
		return JOIN_PATTERN.matcher(sql).find() || SUBQUERY_PATTERN.matcher(sql).find();
	}

	/**
	 * 统一清理方法：仅清理无效WHERE/AND/OR，不修改其他结构
	 */
	public static String cleanInvalidWhere(String rawSql) {
		if (rawSql == null || rawSql.trim().isEmpty()) {
			return rawSql;
		}

		String normalizedSql = rawSql.replaceAll("[\\s\\t\\n\\r]+", " ").trim();
		char[] sqlChars = normalizedSql.toCharArray();
		StringBuilder sb = new StringBuilder();

		int i = 0;
		while (i < sqlChars.length) {
			// 匹配独立的WHERE关键字（忽略大小写）
			int whereLen = matchKeyword(sqlChars, i, "where");
			if (whereLen > 0) {
				String whereStr = new String(sqlChars, i, whereLen);
				i += whereLen;

				// 跳过WHERE后的所有空白
				int spaceCount = 0;
				while (i < sqlChars.length && Character.isWhitespace(sqlChars[i])) {
					spaceCount++;
					i++;
				}

				// 跳过WHERE后紧跟的AND/OR
				int andOrLen = matchKeyword(sqlChars, i, "and");
				if (andOrLen == 0) {
					andOrLen = matchKeyword(sqlChars, i, "or");
				}
				if (andOrLen > 0) {
					i += andOrLen;
					// 跳过AND/OR后的空白
					while (i < sqlChars.length && Character.isWhitespace(sqlChars[i])) {
						i++;
					}
				}

				// 检查WHERE后是否是无有效条件的关键字
				boolean isEmptyWhere = false;
				if (i >= sqlChars.length) {
					isEmptyWhere = true;
				} else if (matchKeyword(sqlChars, i, "order by") > 0
						|| matchKeyword(sqlChars, i, "limit") > 0
						|| matchKeyword(sqlChars, i, "group by") > 0
						|| matchKeyword(sqlChars, i, "having") > 0
						|| matchKeyword(sqlChars, i, "union") > 0) {
					isEmptyWhere = true;
				}

				if (isEmptyWhere) {
					// 无有效条件：不写入WHERE（核心）
					continue;
				} else {
					// 有有效条件：写入WHERE + 补回空格
					sb.append(whereStr);
					if (spaceCount > 0) {
						sb.append(" ");
					}
				}
			} else {
				// 非WHERE关键字，正常写入（保留所有结构）
				sb.append(sqlChars[i]);
				i++;
			}
		}

		// 正则兜底，确保覆盖所有边界场景
		String result = sb.toString().replaceAll("\\s+", " ").trim();
		result = result.replaceAll("(?i)\\bwhere\\b\\s+(order by|limit|group by|having|union)", "$1");
		result = result.replaceAll("(?i)\\bwhere\\b\\s+and\\s+(order by|limit|group by|having|union)", "$1");
		result = result.replaceAll("(?i)\\bwhere\\b\\s+or\\s+(order by|limit|group by|having|union)", "$1");
		result = result.replaceAll("(?i)\\s+\\bwhere\\b$", "");
		result = result.replaceAll("(?i)\\bwhere\\b([^\\s])", "where $1");

		return result.replaceAll("\\s+", " ").trim();
	}

	/**
	 * 兼容旧方法调用
	 */
	@Deprecated
	public static String cleanWhereAndOr(String rawSql) {
		return cleanInvalidWhere(rawSql);
	}

	// 辅助方法：匹配关键字（忽略大小写）
	private static int matchKeyword(char[] chars, int idx, String keyword) {
		if (idx + keyword.length() > chars.length) {
			return 0;
		}
		boolean isFollowedByValidChar = (idx + keyword.length() == chars.length)
				|| Character.isWhitespace(chars[idx + keyword.length()])
				|| chars[idx + keyword.length()] == '='
				|| chars[idx + keyword.length()] == '>'
				|| chars[idx + keyword.length()] == '<';
		if (!isFollowedByValidChar) {
			return 0;
		}
		for (int k = 0; k < keyword.length(); k++) {
			char c1 = Character.toLowerCase(chars[idx + k]);
			char c2 = keyword.charAt(k);
			if (c1 != c2) {
				return 0;
			}
		}
		return keyword.length();
	}

	// 测试方法
	public static void main(String[] args) {
		String sql7 = """
                select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
                left join pms_category c on a.catelog_id = c.cat_id
                LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
                LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
                where
                                        order by agr.attr_group_id asc, agr.attr_sort""";
		String result = cleanSql(sql7);
		System.out.println("=== 处理结果 ===");
		System.out.println(result);

		// 预期结果对比（你的测试用例预期）
		String expected = "select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id order by agr.attr_group_id asc, agr.attr_sort";
		System.out.println("\n=== 匹配预期：" + result.equals(expected) + " ===");
	}
}