package com.awesomecopilot.orm.utils;

import com.awesomecopilot.common.lang.vo.OrderBean;

import java.util.regex.Pattern;

/**
 * 原生 SQL 动态 ORDER BY 字段校验，防止 ORDER BY 注入。
 *
 * @author Rico Yu
 */
public final class OrderByValidator {

	private static final Pattern ORDER_BY_FIELD = Pattern.compile("^[a-zA-Z_][\\w.]*$");

	private OrderByValidator() {
	}

	public static String validateField(String orderBy) {
		if (orderBy == null) {
			throw new IllegalArgumentException("orderBy must not be null");
		}
		String trimmed = orderBy.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("orderBy must not be blank");
		}
		if (!ORDER_BY_FIELD.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("Invalid ORDER BY field: " + orderBy);
		}
		return trimmed;
	}

	public static String validateDirection(OrderBean.DIRECTION direction) {
		if (direction == null) {
			throw new IllegalArgumentException("order direction must not be null");
		}
		return direction.name();
	}

	public static void appendOrderClause(StringBuilder sql, OrderBean orderBean) {
		sql.append(validateField(orderBean.getOrderBy()))
				.append(' ')
				.append(validateDirection(orderBean.getDirection()));
	}
}
