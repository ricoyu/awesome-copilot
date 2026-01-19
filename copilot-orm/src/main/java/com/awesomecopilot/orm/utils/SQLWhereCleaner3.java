package com.awesomecopilot.orm.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL工具类：移除where后紧跟的and/or（忽略空白字符和大小写）
 * 新增：复杂SQL（含JOIN/子查询）简化逻辑，避免处理出错
 */
public class SQLWhereCleaner3 {

	// 核心正则：匹配 where + 任意空白 + and/or（忽略大小写）
	// 分组1：where（保留），分组2：and/or（移除）
	private static final Pattern WHERE_AND_OR_PATTERN = Pattern.compile("(?i)(where)\\s+(and|or)");

	// 定义where后无有效条件的关键字（忽略大小写）
	private static final Set<String> POST_WHERE_KEYWORDS = new HashSet<>(
			Arrays.asList("order", "group", "having", "limit", "union", "")
	);

	// 匹配JOIN关键字（忽略大小写）
	private static final Pattern JOIN_PATTERN = Pattern.compile("(?i)\\bjoin\\b");
	// 匹配子查询（括号内包含SELECT...FROM，忽略大小写）
	private static final Pattern SUBQUERY_PATTERN = Pattern.compile("(?i)\\(\\s*select\\s+.+?\\s+from\\s+.+?\\)");
	// 匹配第一个SELECT（忽略大小写）
	private static final Pattern FIRST_SELECT_PATTERN = Pattern.compile("(?i)^\\s*select\\s+");
	// 匹配最后一个WHERE（忽略大小写）
	private static final Pattern LAST_WHERE_PATTERN = Pattern.compile("(?i)\\bwhere\\b(?!.*\\bwhere\\b)");

	/**
	 * 核心入口方法：先简化复杂SQL，再清理无效的where/and/or
	 *
	 * @param rawSql 原始SQL
	 * @return 处理后的SQL
	 */
	public static String cleanSql(String rawSql) {
		if (rawSql == null || rawSql.trim().isEmpty()) {
			return rawSql;
		}
		// 第一步：简化复杂SQL（含JOIN/子查询）
		String simplifiedSql = simplifyComplexSql(rawSql);
		// 第二步：清理无效的where/and/or
		return cleanInvalidWhere(simplifiedSql);
	}

	/**
	 * 判断是否为复杂SQL（包含JOIN或子查询）
	 *
	 * @param sql SQL语句
	 * @return true=复杂SQL，false=简单SQL
	 */
	private static boolean isComplexSql(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return false;
		}
		// 匹配JOIN关键字
		if (JOIN_PATTERN.matcher(sql).find()) {
			return true;
		}
		// 匹配子查询
		if (SUBQUERY_PATTERN.matcher(sql).find()) {
			return true;
		}
		return false;
	}

	/**
	 * 简化复杂SQL：将第一个SELECT和最后一个WHERE之间的内容替换为*
	 *
	 * @param rawSql 原始SQL
	 * @return 简化后的SQL
	 */
	private static String simplifyComplexSql(String rawSql) {
		String sql = rawSql.trim();
		// 非复杂SQL直接返回
		if (!isComplexSql(sql)) {
			return sql;
		}

		Matcher firstSelectMatcher = FIRST_SELECT_PATTERN.matcher(sql);
		Matcher lastWhereMatcher = LAST_WHERE_PATTERN.matcher(sql);

		// 找不到SELECT或WHERE，直接返回原SQL
		if (!firstSelectMatcher.find() || !lastWhereMatcher.find()) {
			return sql;
		}

		// 第一个SELECT的结束位置（"select "后的位置）
		int selectEndIdx = firstSelectMatcher.end();
		// 最后一个WHERE的起始位置
		int whereStartIdx = lastWhereMatcher.start();

		// 如果SELECT在WHERE之后（异常SQL），直接返回原SQL
		if (selectEndIdx >= whereStartIdx) {
			return sql;
		}

		// 拼接简化后的SQL：SELECT * WHERE...
		StringBuilder sb = new StringBuilder();
		sb.append(sql.substring(0, selectEndIdx)) // 保留第一个SELECT
				.append("*") // 替换中间内容为*
				.append(sql.substring(whereStartIdx)); // 保留最后一个WHERE及之后的内容

		return sb.toString().replaceAll("\\s+", " ").trim();
	}

	/**
	 * 清理SQL中无效的where/and/or，重点解决where+order by场景
	 * （原有逻辑，保持不变）
	 */
	public static String cleanWhereAndOr(String rawSql) {
		if (rawSql == null || rawSql.trim().isEmpty()) {
			return rawSql;
		}

		String normalizedSql = rawSql.replaceAll("[\\s\\t\\n\\r]+", " ").trim();

		// 精准替换所有无效组合（带单词边界，零拼接错误）
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+order by", "order by");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+and\\s+order by", "order by");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+or\\s+order by", "order by");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+or\\s+limit", "limit");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+and\\s+limit", "limit");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+limit", "limit");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+group by", "group by");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+having", "having");
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b\\s+union", "union");
		normalizedSql = normalizedSql.replaceAll("(?i)\\s+\\bwhere\\b$", "");

		// 兜底修复：确保where后有空格（防止拼接）
		normalizedSql = normalizedSql.replaceAll("(?i)\\bwhere\\b([^\\s])", "where $1");

		return normalizedSql.replaceAll("\\s+", " ").trim();
	}

	// 辅助方法：匹配关键字（忽略大小写），返回匹配的长度，无匹配返回0
	private static int matchKeyword(char[] chars, int idx, String keyword) {
		if (idx + keyword.length() > chars.length) {
			return 0;
		}
		// 检查是否是独立关键字（后接空白/结束/标点）
		boolean isFollowedByValidChar = (idx + keyword.length() == chars.length)
				|| Character.isWhitespace(chars[idx + keyword.length()])
				|| chars[idx + keyword.length()] == '=' || chars[idx + keyword.length()] == '>' || chars[idx + keyword.length()] == '<';
		if (!isFollowedByValidChar) {
			return 0;
		}
		// 逐字符匹配（忽略大小写）
		for (int k = 0; k < keyword.length(); k++) {
			char c1 = Character.toLowerCase(chars[idx + k]);
			char c2 = keyword.charAt(k);
			if (c1 != c2) {
				return 0;
			}
		}
		return keyword.length();
	}

	/**
	 * 清理SQL中where后面没有跟查询条件时, 多余的where关键字
	 * （原有逻辑，保持不变）
	 */
	public static String cleanInvalidWhere(String rawSql) {
		if (rawSql == null || rawSql.trim().isEmpty()) {
			return rawSql;
		}

		// 步骤1：统一空白字符为单个空格（仅这一步用正则，其余纯字符操作）
		char[] sqlChars = rawSql.replaceAll("[\\s\\t\\n\\r]+", " ").trim().toCharArray();
		StringBuilder sb = new StringBuilder();

		int i = 0;
		while (i < sqlChars.length) {
			// 匹配独立的where关键字（忽略大小写）
			int whereLen = matchKeyword(sqlChars, i, "where");
			if (whereLen > 0) {
				// 记录where起始位置和前导空格
				int whereStart = i;
				// 保存where关键字（含前导空格，避免拼接）
				String whereStr = new String(sqlChars, whereStart, whereLen);
				i += whereLen;

				// 跳过where后的所有空白（记录跳过的空格数）
				int spaceCount = 0;
				while (i < sqlChars.length && Character.isWhitespace(sqlChars[i])) {
					spaceCount++;
					i++;
				}

				// 检查是否是and/or，若是则跳过
				int andOrLen = matchKeyword(sqlChars, i, "and");
				if (andOrLen == 0) {
					andOrLen = matchKeyword(sqlChars, i, "or");
				}
				if (andOrLen > 0) {
					i += andOrLen;
					// 跳过and/or后的空白
					while (i < sqlChars.length && Character.isWhitespace(sqlChars[i])) {
						i++;
					}
				}

				// 检查where后是否是order by/limit/group by/having/union（无有效条件）
				boolean isEmptyWhere = false;
				if (i >= sqlChars.length) {
					isEmptyWhere = true; // where后无内容
				} else if (matchKeyword(sqlChars, i, "order by") > 0) {
					isEmptyWhere = true;
				} else if (matchKeyword(sqlChars, i, "limit") > 0) {
					isEmptyWhere = true;
				} else if (matchKeyword(sqlChars, i, "group by") > 0) {
					isEmptyWhere = true;
				} else if (matchKeyword(sqlChars, i, "having") > 0) {
					isEmptyWhere = true;
				} else if (matchKeyword(sqlChars, i, "union") > 0) {
					isEmptyWhere = true;
				}

				if (isEmptyWhere) {
					// 无有效条件：完全不写入where
					continue;
				} else {
					// 有有效条件：写入where + 原本的空格（修复wherename问题）
					sb.append(whereStr);
					// 补回where后的空格，避免拼接
					if (spaceCount > 0) {
						sb.append(" ");
					}
				}
			} else {
				// 非where关键字，正常写入
				sb.append(sqlChars[i]);
				i++;
			}
		}

		// 最终清理多余空格
		String result = sb.toString().replaceAll("\\s+", " ").trim();
		// 兜底修复：确保where后有空格（防止极端场景拼接）
		result = result.replaceAll("(?i)\\bwhere\\b([^\\s])", "where $1");
		return result;
	}

	// 测试示例
	public static void main(String[] args) {
		// 测试复杂SQL（含JOIN）
		String complexSql = """
                select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
                left join pms_category c on a.catelog_id = c.cat_id
                LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
                LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
                where
                                        order by agr.attr_group_id asc, agr.attr_sort""";

		System.out.println("原始复杂SQL：");
		System.out.println(complexSql);
		System.out.println("------------------------");
		// 调用新的核心方法
		String cleanedSql = cleanSql(complexSql);
		System.out.println("处理后SQL：");
		System.out.println(cleanedSql);
		System.out.println("------------------------");

		// 测试子查询SQL
		String subQuerySql = "select id from user where age > (select avg(age) from user where gender = 'male') and order by id";
		System.out.println("原始子查询SQL：");
		System.out.println(subQuerySql);
		System.out.println("------------------------");
		System.out.println("处理后SQL：");
		System.out.println(cleanSql(subQuerySql));
		System.out.println("------------------------");

		// 测试简单SQL（不简化）
		String simpleSql = "select * from user where and order by id";
		System.out.println("原始简单SQL：");
		System.out.println(simpleSql);
		System.out.println("------------------------");
		System.out.println("处理后SQL：");
		System.out.println(cleanSql(simpleSql));
	}
}