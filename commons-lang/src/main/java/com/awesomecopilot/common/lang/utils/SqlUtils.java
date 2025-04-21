package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.exception.SqlParseException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.update.Update;
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
	 * 处理所有类型的SQL语句，动态添加deleted=0和tenant_id=xxx条件
	 */
	public static String addDeleteTenantIdCondition(String originalQuerySql) {
		Long tenantId = ThreadContext.get("tenantId");
		String sqlCacheKey = originalQuerySql + (tenantId == null ? "" : tenantId.toString());
		String cachedSql = deleteTenantCache.get(sqlCacheKey);
		if (isNotEmpty(cachedSql)) {
			if (cache.size() > 1000) {
				cache.clear();
			}
			return cachedSql;
		}

		try {
			Statement statement = CCJSqlParserUtil.parse(originalQuerySql);

			if (statement instanceof Select) {
				return handleSelectStatement((Select) statement, sqlCacheKey, originalQuerySql);
			} else if (statement instanceof Update) {
				return handleUpdateStatement((Update) statement, sqlCacheKey, originalQuerySql);
			} else if (statement instanceof Delete) {
				return handleDeleteStatement((Delete) statement, sqlCacheKey, originalQuerySql);
			} else {
				return originalQuerySql;
			}
		} catch (JSQLParserException e) {
			log.error("SQL parse error", e);
			throw new SqlParseException(originalQuerySql, e);
		}
	}

	private static String handleSelectStatement(Select select, String sqlCacheKey, String originalQuerySql) {
		SelectBody selectBody = select.getSelectBody();

		if (selectBody instanceof PlainSelect) {
			PlainSelect plainSelect = (PlainSelect) selectBody;
			String newCondition =
					buildNewCondition(plainSelect.getWhere() != null ? plainSelect.getWhere().toString() : "");

			if (isNotBlank(newCondition)) {
				try {
					plainSelect.setWhere(CCJSqlParserUtil.parseCondExpression(newCondition));
				} catch (JSQLParserException e) {
					throw new SqlParseException(plainSelect.toString());
				}
			}

			String sqlWithDeletedTenantId = plainSelect.toString();
			deleteTenantCache.put(sqlCacheKey, sqlWithDeletedTenantId);
			return sqlWithDeletedTenantId;
		}
		throw new SqlParseException(originalQuerySql);
	}

	private static String handleUpdateStatement(Update update, String sqlCacheKey, String originalQuerySql) {
		String originalCondition = update.getWhere() != null ? update.getWhere().toString() : "";
		String newCondition = buildNewCondition(originalCondition);

		if (isNotBlank(newCondition)) {
			try {
				update.setWhere(CCJSqlParserUtil.parseCondExpression(newCondition));
			} catch (JSQLParserException e) {
				throw new SqlParseException(update.toString());
			}
		}

		String sqlWithDeletedTenantId = update.toString();
		deleteTenantCache.put(sqlCacheKey, sqlWithDeletedTenantId);
		return sqlWithDeletedTenantId;
	}

	private static String handleDeleteStatement(Delete delete, String sqlCacheKey, String originalQuerySql) {
		String originalCondition = delete.getWhere() != null ? delete.getWhere().toString() : "";
		String newCondition = buildNewCondition(originalCondition);

		if (isNotBlank(newCondition)) {
			try {
				delete.setWhere(CCJSqlParserUtil.parseCondExpression(newCondition));
			} catch (JSQLParserException e) {
				throw new SqlParseException(delete.toString());
			}
		}

		String sqlWithDeletedTenantId = delete.toString();
		deleteTenantCache.put(sqlCacheKey, sqlWithDeletedTenantId);
		return sqlWithDeletedTenantId;
	}

	private static String buildNewCondition(String originalCondition) {
		Long tenantId = ThreadContext.get("tenantId");
		String trimedSql = StringUtils.trimAll(originalCondition);
		boolean alreadyContainDeleted = trimedSql.toLowerCase().contains("deleted=");
		boolean alreadyContainTenentId = trimedSql.toLowerCase().contains("tenant_id=");

		String newCondition = originalCondition;
		//表示原来的条件上是否需要加()括起来
		boolean shouldQuoted = false;
		/*
		 * 如果原始SQL有多个条件, 并且包含 or, 就需要在原来的条件上套一个()括起来
		 * 否则, 就不需要在原始条件上套一个()了
		 */
		if (originalCondition.toLowerCase().contains(" or ")) {
			shouldQuoted = true;
		}

		String parsedOriginalcondition = originalCondition;
		if (shouldQuoted) {
			parsedOriginalcondition =  "(" + originalCondition + ")";
		}

		if (!alreadyContainDeleted && logicalDeleteEnabled) {
			if (tenantId == null || alreadyContainTenentId) {
				newCondition = originalCondition.equals("")
						? "deleted = 0"
						: parsedOriginalcondition +" AND " + logicalDeleteField + " = 0";
			} else {
				newCondition = originalCondition.equals("")
						? (logicalDeleteField + " = 0 AND tenant_id = " + tenantId)
						: parsedOriginalcondition + " AND " + logicalDeleteField + " = 0 AND tenant_id = " + tenantId;
			}
		} else {
			if (tenantId == null || alreadyContainTenentId) {
				newCondition = originalCondition;
			} else {
				newCondition = isNotBlank(originalCondition)
						? "(" + originalCondition + ") AND tenant_id = " + tenantId
						: "tenant_id = " + tenantId;
			}
		}

		return newCondition;
	}
}
