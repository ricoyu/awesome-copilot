package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * 字符串属性的比较条件。
 * <p>
 * null 语义约定（本包所有 Predicate 统一）：
 * <ul>
 * <li>EQ + null → {@code col is null}（"等于没值"就是 IS NULL）</li>
 * <li>NOTEQ + null → {@code col is not null}</li>
 * <li>GT/GE/LT/LE/ANYWHERE + null → 没有意义, 直接抛 IllegalArgumentException
 * （历史 bug：静默返回 null 谓词, 到 where() 才 NPE, 报错点远离病因）</li>
 * </ul>
 * <p>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringPredicate extends AbstractPredicate {

	private String propertyValue;
	private CompareMode compareMode = CompareMode.EQ;

	public StringPredicate(String propertyName, String propertyValue) {
		setPropertyName(propertyName);
		this.propertyValue = propertyValue;
	}

	public StringPredicate(String propertyName, String propertyValue, CompareMode compareMode) {
		this(propertyName, propertyValue);
		this.setCompareMode(compareMode);
	}

	public CompareMode getCompareMode() {
		return compareMode;
	}

	public void setCompareMode(CompareMode compareMode) {
		this.compareMode = compareMode;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		Path<String> path = root.get(getPropertyName());
		switch (compareMode) {
		case EQ:
			// null 值按 IS NULL 处理, 与整包 null 语义一致
			return propertyValue != null
					? criteriaBuilder.equal(path, propertyValue)
					: criteriaBuilder.isNull(path);
		case NOTEQ:
			return propertyValue != null
					? criteriaBuilder.notEqual(path, propertyValue)
					: criteriaBuilder.isNotNull(path);
		case GT:
			requireNonNullValueFor("GT");
			return criteriaBuilder.greaterThan(path, propertyValue);
		case GE:
			requireNonNullValueFor("GE");
			return criteriaBuilder.greaterThanOrEqualTo(path, propertyValue);
		case LT:
			requireNonNullValueFor("LT");
			return criteriaBuilder.lessThan(path, propertyValue);
		case LE:
			requireNonNullValueFor("LE");
			return criteriaBuilder.lessThanOrEqualTo(path, propertyValue);
		case ANYWHERE:
			// 包含匹配: 两端各补一个 %。
			// 历史 bug: 写成 String.join("%", value, "%") —— join 的第一个参数是【分隔符】,
			// 产出 "value%%"(等价前缀匹配)而非 "%value%", 查询语义被悄悄改变; 且 value 为 null
			// 时 join 会拼出字面量 "null%"。这里显式拼接并做 null 校验。
			requireNonNullValueFor("ANYWHERE");
			return criteriaBuilder.like(path, "%" + propertyValue + "%");
		default:
			// IN/NOTIN 请改用 InPredicate, 本类不支持; 抛异常优于返回 null 谓词
			throw new IllegalArgumentException(
					"StringPredicate 不支持比较模式 " + compareMode + ", 属性: " + getPropertyName());
		}
	}

	private void requireNonNullValueFor(String mode) {
		if (propertyValue == null) {
			throw new IllegalArgumentException(
					"属性[" + getPropertyName() + "]的 " + mode + " 条件值为null, 无法构造有意义的比较");
		}
	}
}
