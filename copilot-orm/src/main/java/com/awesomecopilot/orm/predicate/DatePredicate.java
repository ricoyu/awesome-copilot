package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Date;

/**
 * java.util.Date 属性比较。begin,end 都有值 → BETWEEN; 只给 begin → >= ; 只给 end → <=
 * (与 {@link LocalDateTimePredicate} 语义对齐)。
 * <p>
 * 历史 bug: 旧 toPredicate() 无视 matchMode 永远返回 between(begin,end), 且自带
 * private matchMode 遮蔽父类字段; begin/end 单边为 null 时生成非法 between。
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DatePredicate extends AbstractDatePredicate {
	
	private Date begin;
	private Date end;

	public DatePredicate(String propertyName, Date begin, Date end) {
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
		// 只写父类字段, 不再遮蔽
		setMatchMode(matchMode);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		// 模式经父类 getter 读取(唯一真相源), 按推导出的三种模式分派
		switch (getMatchMode()) {
		case BETWEEN:
			return criteriaBuilder.between(root.get(getPropertyName()), begin, end);
		case LATER_THAN_OR_SAME:
			return criteriaBuilder.greaterThanOrEqualTo(root.get(getPropertyName()), begin);
		case EARLIER_THAN_OR_SAME:
			return criteriaBuilder.lessThanOrEqualTo(root.get(getPropertyName()), end);
		default:
			throw new IllegalStateException("DatePredicate 意外模式: " + getMatchMode());
		}
	}

}
