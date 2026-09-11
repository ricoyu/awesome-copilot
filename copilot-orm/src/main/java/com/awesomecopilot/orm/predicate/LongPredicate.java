package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Long 属性的比较条件。null 语义与 {@link IntegerPredicate}/{@link StringPredicate} 一致:
 * EQ+null → IS NULL, NOTEQ+null → IS NOT NULL, 其余比较模式+null → 快速失败抛异常。
 * <p>
 * 历史 bug: EQ+null 分支写成了 isNotNull(与 String/Boolean 版相反)——已修正为 isNull。
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LongPredicate extends AbstractPredicate {

	private Long propertyValue;
	private CompareMode compareMode = CompareMode.EQ;

	public LongPredicate(String propertyName, Long propertyValue) {
		setPropertyName(propertyName);
		this.propertyValue = propertyValue;
	}

	public LongPredicate(String propertyName, Long propertyValue, CompareMode compareMode) {
		this(propertyName, propertyValue);
		this.setCompareMode(compareMode);
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
			// 修正: null → IS NULL(与整包 null 语义统一), 旧代码是 isNotNull, 写反了
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
			throw new IllegalArgumentException(
					"LongPredicate 不支持比较模式 " + compareMode + ", 属性: " + getPropertyName());
		}
	}

	private void requireNonNullValueFor(String mode) {
		if (propertyValue == null) {
			throw new IllegalArgumentException(
					"属性[" + getPropertyName() + "]的 " + mode + " 条件值为null, 无法构造有意义的比较");
		}
	}
}
