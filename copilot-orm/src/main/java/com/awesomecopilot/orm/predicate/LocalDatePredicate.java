package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;

/**
 * LocalDate 属性的比较条件。
 * <p>
 * 本次修复的三个历史 bug:
 * <ol>
 * <li>(begin,end) 双参构造器不再保持默认的 EXACT 模式——EXACT 会生成 {@code col = begin}
 * 把 end 条件整个丢掉, 结果集静默偏小; 现在按参数推导: 两个都有值 → BETWEEN,
 * 只有 begin → LATER_THAN_OR_SAME, 只有 end → EARLIER_THAN_OR_SAME
 * (与 {@link LocalDateTimePredicate} 的既有做法对齐);</li>
 * <li>{@code case LATER_THAN} 缺 break, 贯穿进 EARLIER_THAN_OR_SAME 把 predicate
 * 覆盖成 {@code <=} —— 请求"晚于"实际执行"早于等于", 方向完全写反; 已补齐 break;</li>
 * <li>不再自己声明 private matchMode 字段。此前子类字段遮蔽了
 * {@link AbstractDatePredicate} 的同名字段: toPredicate() 读子类字段、继承来的
 * setMatchMode()/getMatchMode() 读写父类字段, 调用 public 的 setMatchMode() 静默无效。
 * 现在模式统一存放在父类, 读写只有一个真相源。</li>
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocalDatePredicate extends AbstractDatePredicate {

	private LocalDate begin;
	private LocalDate end;

	/**
	 * 单值 + 默认 EXACT(等值)模式。
	 */
	public LocalDatePredicate(String propertyName, LocalDate localDate) {
		this(propertyName, localDate, DateMatchMode.EXACT);
	}

	public LocalDatePredicate(String propertyName, LocalDate localDate, DateMatchMode matchMode) {
		setPropertyName(propertyName);
		this.begin = localDate;
		addCandidateMatchMode(DateMatchMode.EXACT, DateMatchMode.LATER_THAN, DateMatchMode.LATER_THAN_OR_SAME,
				DateMatchMode.EARLIER_THAN, DateMatchMode.EARLIER_THAN_OR_SAME);
		checkDateMatchMode(matchMode);   // 先校验再赋值, 非法模式不允许进入对象状态
		setMatchMode(matchMode);         // 写父类字段(唯一真相源)
	}

	/**
	 * 双值构造器: 按非空参数推导比较模式(修复 bug#1, 旧实现固定 EXACT 导致 end 丢失)。
	 */
	public LocalDatePredicate(String propertyName, LocalDate begin, LocalDate end) {
		setPropertyName(propertyName);
		this.begin = begin;
		this.end = end;
		DateMatchMode matchMode;
		if (begin != null && end != null) {
			matchMode = DateMatchMode.BETWEEN;
		} else if (begin != null) {
			matchMode = DateMatchMode.LATER_THAN_OR_SAME;  // 只有下界: col >= begin
		} else if (end != null) {
			matchMode = DateMatchMode.EARLIER_THAN_OR_SAME; // 只有上界: col <= end
		} else {
			throw new IllegalArgumentException("begin and end can't both be null");
		}
		addCandidateMatchMode(matchMode);
		setMatchMode(matchMode);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		// 统一经父类 getter 读取模式, 杜绝字段遮蔽
		DateMatchMode matchMode = getMatchMode();
		switch (matchMode) {
		case BETWEEN:
			return criteriaBuilder.between(root.get(getPropertyName()), begin, end);
		case EXACT:
			return begin != null
					? criteriaBuilder.equal(root.get(getPropertyName()), begin)
					: criteriaBuilder.isNull(root.get(getPropertyName()));
		case EARLIER_THAN:
			return criteriaBuilder.lessThan(root.get(getPropertyName()), begin);
		// 修复 bug#2: 以下每个 case 都必须 return/break。旧代码 LATER_THAN 缺 break,
		// 贯穿到 EARLIER_THAN_OR_SAME 把 ">" 覆盖成 "<="
		case LATER_THAN:
			return criteriaBuilder.greaterThan(root.get(getPropertyName()), begin);
		case EARLIER_THAN_OR_SAME:
			return criteriaBuilder.lessThanOrEqualTo(root.get(getPropertyName()), begin);
		case LATER_THAN_OR_SAME:
			return criteriaBuilder.greaterThanOrEqualTo(root.get(getPropertyName()), begin);
		default:
			// 抛异常优于兜底 equal: 未知模式说明调用方与本类约定脱节, 静默查等值更危险
			throw new IllegalArgumentException("LocalDatePredicate 不支持模式 " + matchMode
					+ ", 属性: " + getPropertyName());
		}
	}
}
