package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;

/**
 *  日期比较, begin, end都传则是between, 只传begin则是大于等于, 只传end则是小于等于 
 * <p>
 * Copyright: Copyright (c) 2024-01-30 14:41
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocalDateTimePredicate extends AbstractDatePredicate {

	private LocalDateTime begin;
	private LocalDateTime end;

	// 历史 bug: 这里曾声明 private DateMatchMode matchMode 遮蔽父类字段,
	// 导致 setMatchMode()(写父类)与 toPredicate()(读子类)两套状态并存, 已删除,
	// 模式统一由 AbstractDatePredicate 持有
	public LocalDateTimePredicate(String propertyName, LocalDateTime begin, LocalDateTime end) {
		setPropertyName(propertyName);
		this.begin = begin;
		this.end = end; 
		if (begin == null && end == null) {
			throw new IllegalArgumentException("begin and end can't both be null");
		}
		DateMatchMode matchMode = DateMatchMode.BETWEEN;
		if (begin == null) {
			matchMode = DateMatchMode.EARLIER_THAN_OR_SAME;
		}
		if (end == null) {
			matchMode = DateMatchMode.LATER_THAN_OR_SAME;
		}
		addCandidateMatchMode(matchMode);
		setMatchMode(matchMode);   // 写父类唯一字段(旧代码直接赋值子类遮蔽字段)
	}
	
	/**
	 * 上面构造函数决定了matchMode只有下面三种情况
	 * @param criteriaBuilder
	 * @param root
	 * @return Predicate
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		// 读父类 getter, 保证与 setMatchMode 一致
		switch (getMatchMode()) {
		case BETWEEN:
			return criteriaBuilder.between(root.get(getPropertyName()), begin, end);
		case EARLIER_THAN_OR_SAME:
			return criteriaBuilder.lessThanOrEqualTo(root.get(getPropertyName()), end);
		case LATER_THAN_OR_SAME:
			return criteriaBuilder.greaterThanOrEqualTo(root.get(getPropertyName()), begin);
		default:
			// 抛异常优于返回 null: null 谓词会带进 where() 变成难排查的 NPE
			throw new IllegalStateException("LocalDateTimePredicate 意外模式: " + getMatchMode());
		}
	}

}
