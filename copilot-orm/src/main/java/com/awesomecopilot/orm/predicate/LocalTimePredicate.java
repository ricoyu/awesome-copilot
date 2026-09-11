package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalTime;

/**
 * LocalTime 属性区间比较。
 * <p>
 * 历史 bug: 自带 private matchMode 遮蔽父类字段(已删, 状态收敛到
 * {@link AbstractDatePredicate}); 且 begin/end 任一为 null 时仍生成
 * {@code between(col, null, x)} 这种非法 SQL。现在单边 null 降级为 >=/<=,
 * 双边 null 直接拒绝构造。
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocalTimePredicate extends AbstractDatePredicate {

	private LocalTime begin;
	private LocalTime end;

	public LocalTimePredicate(String propertyName, LocalTime begin, LocalTime end) {
		setPropertyName(propertyName);
		this.begin = begin;
		this.end = end;
		DateMatchMode matchMode;
		if (begin != null && end != null) {
			matchMode = DateMatchMode.BETWEEN;
		} else if (begin != null) {
			matchMode = DateMatchMode.LATER_THAN_OR_SAME;   // 只有下界: col >= begin
		} else if (end != null) {
			matchMode = DateMatchMode.EARLIER_THAN_OR_SAME; // 只有上界: col <= end
		} else {
			throw new IllegalArgumentException("begin and end can't both be null");
		}
		addCandidateMatchMode(matchMode);
		setMatchMode(matchMode);   // 写父类唯一字段
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		switch (getMatchMode()) {
		case BETWEEN:
			return criteriaBuilder.between(root.get(getPropertyName()), begin, end);
		case LATER_THAN_OR_SAME:
			return criteriaBuilder.greaterThanOrEqualTo(root.get(getPropertyName()), begin);
		case EARLIER_THAN_OR_SAME:
			return criteriaBuilder.lessThanOrEqualTo(root.get(getPropertyName()), end);
		default:
			throw new IllegalStateException("LocalTimePredicate 意外模式: " + getMatchMode());
		}
	}

}
