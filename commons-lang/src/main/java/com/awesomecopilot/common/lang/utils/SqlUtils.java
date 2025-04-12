package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.exception.SqlParseException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * <p>
 * Copyright: (C), 2023-10-02 19:17
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SqlUtils {

	private static final Logger log = LoggerFactory.getLogger(SqlUtils.class);

	private static ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

	private static ConcurrentMap<String, String> deleteTenantCache = new ConcurrentHashMap<>();

	/**
	 * copilot-orm是否启用自动支持逻辑删除, JpaDao在初始化完成后会设置本属性
	 */
	public static boolean logicalDeleteEnabled = false;

	/**
	 * 逻辑删除字段名, JpaDao在初始化完成后会设置本属性
	 */
	public static String logicalDeleteField = "deleted";

	/**
	 * 给定任意的select语句, 生成对应的count语句
	 *
	 * @param originalQuerySql
	 * @return
	 */
	public static String generateCountSql(String originalQuerySql) {
		String countSql = cache.get(originalQuerySql);
		if (isNotEmpty(countSql)) {
			if (cache.size() > 1000) {
				cache.clear();
			}
			return countSql;
		}
		try {
			// 解析原始SQL查询
			Statement statement = CCJSqlParserUtil.parse(originalQuerySql);

			if (statement instanceof Select) {
				Select selectStatement = (Select) statement;
				SelectBody selectBody = selectStatement.getSelectBody();

				if (selectBody instanceof PlainSelect) {
					PlainSelect plainSelect = (PlainSelect) selectBody;

					// 构建新的COUNT(*)查询
					SelectExpressionItem countItem = new SelectExpressionItem(new Column("COUNT(*)"));
					plainSelect.setSelectItems(Arrays.asList(countItem));

					// 转换为字符串形式的COUNT(*)查询
					countSql = selectStatement.toString();
					cache.put(originalQuerySql, countSql);
					return countSql;
				}
			}
		} catch (JSQLParserException e) {
			throw new SqlParseException(originalQuerySql, e);
			// 处理解析错误，根据需求返回空字符串或抛出异常
		}

		// 如果无法正确处理原始查询，返回空字符串或抛出异常
		throw new SqlParseException(originalQuerySql);
	}

	/**
	 * 给定任意的select语句, 动态按需添加deleted=0 and tenant_id=xxx的条件
	 *
	 * @param originalQuerySql
	 * @return
	 */
	public static String addDeleteTenantIdCondition(String originalQuerySql) {
		Long tenantId = ThreadContext.get("tenantId");
		// 不同请求携带的租户ID可能不一样, 所以缓存key必须加上租户ID
		String sqlCacheKey = originalQuerySql + (tenantId == null ? "" : tenantId.toString());
		String deleteSql = deleteTenantCache.get(sqlCacheKey);
		if (isNotEmpty(deleteSql)) {
			if (cache.size() > 1000) {
				cache.clear();
			}
			return deleteSql;
		}
		try {
			// 解析原始SQL查询
			Statement statement = CCJSqlParserUtil.parse(originalQuerySql);

			if (statement instanceof Select) {
				Select selectStatement = (Select) statement;
				SelectBody selectBody = selectStatement.getSelectBody();

				if (selectBody instanceof PlainSelect) {
					PlainSelect plainSelect = (PlainSelect) selectBody;

					// 添加 deleted=0 条件
					// 获取原始 WHERE 条件
					String originalCondition = plainSelect.getWhere() != null
							? plainSelect.getWhere().toString()
							: ""; // 无 WHERE 时默认条件

					String trimedSql = StringUtils.trimAll(originalCondition);
					boolean alreadyContainDeleted = false; //原始SQL中是否已经包含deleted条件
					boolean alreadyContainTenentId = false; //原始SQL中是否已经包含tenant_id条件
					if (trimedSql.toLowerCase().contains("deleted=")) {
						alreadyContainDeleted = true;
					}
					if (trimedSql.toLowerCase().contains("tenant_id=")) {
						alreadyContainTenentId = true;
					}

					String newCondition = originalCondition;
					//原始SQL不包含deleted条件, 则需要自动添加deleted=0条件
					if (!alreadyContainDeleted && logicalDeleteEnabled) {
						if (tenantId == null || alreadyContainTenentId) {
							// 构建新条件：(originalCondition) AND deleted = 0
							if (originalCondition.equals("")) {
								newCondition += "deleted = 0";
							} else {
								newCondition = "(" + originalCondition + ") AND " + logicalDeleteField + " = 0";
							}
						} else {
							if (originalCondition.equals("")) {
								newCondition += (logicalDeleteField + " = 0 AND tenant_id = " + tenantId);
							} else {
								newCondition =
										"(" + originalCondition + ") AND " + logicalDeleteField + " = 0 AND tenant_id" +
												" " +
												"= " + tenantId;
							}
						}
					} else {
						if (tenantId == null || alreadyContainTenentId) {
							newCondition = originalCondition;
						} else {
							if (isNotBlank(originalCondition)) {
								newCondition = "(" + originalCondition + ") AND tenant_id = " + tenantId;
							} else {
								newCondition = "tenant_id = " + tenantId;
							}
						}
					}

					// 替换 WHERE 子句
					try {
						if (isNotBlank(newCondition)) {
							plainSelect.setWhere(CCJSqlParserUtil.parseCondExpression(newCondition));
						}
					} catch (JSQLParserException e) {
						// 如果无法正确处理原始查询，返回空字符串或抛出异常
						throw new SqlParseException(plainSelect.toString());
					}

					String sqlWithDeletedTenantId = plainSelect.toString();
					cache.put(sqlCacheKey, sqlWithDeletedTenantId);
					return sqlWithDeletedTenantId;
				}
			}
		} catch (JSQLParserException e) {
			log.error("", e);
			throw new SqlParseException(originalQuerySql, e);
			// 处理解析错误，根据需求返回空字符串或抛出异常
		}
		// 如果无法正确处理原始查询，返回空字符串或抛出异常
		throw new SqlParseException(originalQuerySql);
	}

}
