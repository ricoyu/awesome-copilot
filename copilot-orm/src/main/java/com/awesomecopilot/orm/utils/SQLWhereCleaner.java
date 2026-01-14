package com.awesomecopilot.orm.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL工具类：移除where后紧跟的and/or（忽略空白字符和大小写）
 */
public class SQLWhereCleaner {

	// 核心正则：匹配 where + 任意空白 + and/or（忽略大小写）
	// 分组1：where（保留），分组2：and/or（移除）
	private static final Pattern WHERE_AND_OR_PATTERN = Pattern.compile("(?i)(where)\\s+(and|or)");

	// 定义where后无有效条件的关键字（忽略大小写）
	private static final Set<String> POST_WHERE_KEYWORDS = new HashSet<>(
			Arrays.asList("order", "group", "having", "limit", "union", "")
	);

	/**
	 * 清理SQL中无效的where/and/or，重点解决where+order by场景
	 *
	 * @param rawSql 原始SQL
	 * @return 绝对正确的清理后SQL
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
	 *
	 * @param rawSql 原始SQL
	 * @return 清理后的SQL
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
		// 测试用例1：where后无内容
		String sql1 = "select * from pms_attr_group where";
		// 测试用例2：where后紧跟order by
		String sql2 = "select * from pms_attr_group where order by `sort` asc, attr_group_name asc limit ?";
		// 测试用例3：where后多个空格+and，再无其他条件
		String sql3 = "select * from user where    and order by create_time";
		// 测试用例4：where后有有效条件（不处理）
		String sql4 = "select * from user where name = 'test' order by age";
		// 测试用例5：where后换行+or，再跟limit
		String sql5 = "select * from order where\n   or limit 10";

		System.out.println("处理前sql1：" + sql1);
		System.out.println("处理后sql1：" + cleanInvalidWhere(sql1));
		System.out.println("------------------------");
		System.out.println("处理前sql2：" + sql2);
		System.out.println("处理后sql2：" + cleanInvalidWhere(sql2));
		System.out.println("------------------------");
		System.out.println("处理前sql3：" + sql3);
		System.out.println("处理后sql3：" + cleanInvalidWhere(sql3));
		System.out.println("------------------------");
		System.out.println("处理前sql4：" + sql4);
		System.out.println("处理后sql4：" + cleanInvalidWhere(sql4));
		System.out.println("------------------------");
		System.out.println("处理前sql5：" + sql5);
		System.out.println("处理后sql5：" + cleanInvalidWhere(sql5));

		String sql6 = """
				select * from pms_attr_group where
				                        order by `sort` asc, attr_group_name asc limit ?""";
		System.out.println("处理前sql6：" + sql6);
		System.out.println("处理后sql6：" + cleanWhereAndOr(sql6));

		String sql7 = """
				select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
				left join pms_category c on a.catelog_id = c.cat_id
				LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
				LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
				where
				                        order by agr.attr_group_id asc, agr.attr_sort""";
		System.out.println("处理前sql6：" + sql7);
		System.out.println("处理后sql6：" + cleanWhereAndOr(sql7));
	}
}