package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Integer 属性的比较条件。null 语义与 {@link StringPredicate} 保持一致:
 * EQ+null → IS NULL, NOTEQ+null → IS NOT NULL, 其余比较模式+null → 快速失败抛异常。
 * <p>
 * 历史 bug: EQ+null 分支写成了 isNotNull(与 String/Boolean 版的 isNull 恰好相反),
 * "等于null"的查询被悄悄变成"非空"过滤, 结果集完全错误且无任何报错——已修正为 isNull。
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IntegerPredicate extends AbstractPredicate {

	private Integer propertyValue;
	private CompareMode compareMode = CompareMode.EQ;

	public IntegerPredicate(String propertyName, Integer propertyValue) {
		setPropertyName(propertyName);
		this.propertyValue = propertyValue;
	}

	public IntegerPredicate(String propertyName, Integer propertyValue, CompareMode compareMode) {
		this(propertyName, propertyValue);
		this.compareMode = compareMode;
	}

	public CompareMode getCompareMode() {
		return compareMode;
	}

	public void setCompareMode(CompareMode compareMode) {
		this.compareMode = compareMode;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		Path path = root.get(getPropertyName());
		switch (compareMode) {
		case GT:
			requireNonNullValueFor("GT");
			return criteriaBuilder.gt(path, propertyValue);
		case GE:
			requireNonNullValueFor("GE");
			return criteriaBuilder.ge(path, propertyValue);
		case EQ:
			// 修正: null 表示"查该列为 null 的记录"(IS NULL), 与 String/Boolean 版统一。
			// 旧代码这里是 isNotNull, 语义写反
			return propertyValue != null
					? criteriaBuilder.equal(path, propertyValue)
					: criteriaBuilder.isNull(path);
		case LT:
			requireNonNullValueFor("LT");
			return criteriaBuilder.lessThan(path, propertyValue);
		case LE:
			requireNonNullValueFor("LE");
			return criteriaBuilder.lessThanOrEqualTo(path, propertyValue);
		case NOTEQ:
			return propertyValue != null
					? criteriaBuilder.notEqual(path, propertyValue)
					: criteriaBuilder.isNotNull(path);
		default:
			// 抛异常优于返回 null 谓词: null 会一路带到 where() 才以 NPE 爆出, 定位成本极高
			throw new IllegalArgumentException(
					"IntegerPredicate 不支持比较模式 " + compareMode + ", 属性: " + getPropertyName());
		}
	}

	private void requireNonNullValueFor(String mode) {
		if (propertyValue == null) {
			throw new IllegalArgumentException(
					"属性[" + getPropertyName() + "]的 " + mode + " 条件值为null, 无法构造有意义的比较");
		}
	}
}
