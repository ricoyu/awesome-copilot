package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;

/**
 * IN / NOT IN 条件。
 * <p>
 * 历史 bug（静默生成"零值的 IN"）: 以下三种输入旧实现都会产出一个一个 value 都没加的
 * In 谓词——渲染成 SQL 要么语法错误 {@code in ()}，要么恒假，NOT IN 时更是灾难：
 * <ol>
 * <li>传标量(如 inPredicate("name","bob"))：既非 Collection 也不在 ArrayTypes 表内, 直接落到方法尾;</li>
 * <li>空 Collection：循环零次;</li>
 * <li>不支持的数组类型(Boolean[]/LocalDate[] 等): switch 走 default 静默 break。</li>
 * </ol>
 * 现在三种情况全部快速失败, 异常信息点名属性与实际类型; propertyValues 为 null 同样抛异常
 * （旧实现返回 null 谓词, 调用方 where() 处才 NPE, 定位成本极高）。
 * <p/>
 * Copyright: Copyright (c) 2025-03-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class InPredicate extends AbstractPredicate {

	private Object propertyValues;
	private CompareMode compareMode = CompareMode.IN;

	/**
	 * @param propertyName
	 * @param propertyValues Object类型即可, toPredicate 里动态判断是 Collection 还是各种数组
	 */
	public InPredicate(String propertyName, Object propertyValues) {
		setPropertyName(propertyName);
		this.propertyValues = propertyValues;
	}

	public InPredicate(String propertyName, Object propertyValues, CompareMode compareMode) {
		setPropertyName(propertyName);
		this.propertyValues = propertyValues;
		this.compareMode = compareMode;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		if (propertyValues == null) {
			// 旧代码: return null → 调用方 conditions.add(null) → where() 处 NPE
			throw new IllegalArgumentException(
					"属性[" + getPropertyName() + "]的 IN 条件值为null; 若想跳过该条件请在调用方判空, 别构造 InPredicate");
		}

		Path path = root.get(getPropertyName());
		In inPredicate;
		if (compareMode == CompareMode.IN) {
			inPredicate = criteriaBuilder.in(path);
		} else if (compareMode == CompareMode.NOTIN) {
			// Hibernate 6.5 下 in().not() 仍返回 In 实例(SqmInListPredicate), 强转安全(已对照官方 sources)
			inPredicate = (In) criteriaBuilder.in(path).not();
		} else {
			throw new IllegalArgumentException(
					"InPredicate 只支持 IN/NOTIN, 传入 " + compareMode + ", 属性: " + getPropertyName());
		}

		int added = 0;
		if (Collection.class.isAssignableFrom(propertyValues.getClass())) {
			Collection values = (Collection) propertyValues;
			for (Object value : values) {
				inPredicate.value(value);
				added++;
			}
		} else if (propertyValues.getClass().isArray()) {
			// 基本类型数组与包装数组统一按元素遍历取值, 不再按 9 种 ArrayTypes 写 9 段重复 switch;
			// 之前 switch 未覆盖的类型(Boolean[]/Short[]/LocalDate[]/char[]...)会静默产出空 IN
			Object[] elements;
			Class<?> componentType = propertyValues.getClass().getComponentType();
			if (componentType.isPrimitive()) {
				// 基本类型数组无法直接当 Object[] 用, 走 java.util.Arrays 的装箱反射路径
				int len = java.lang.reflect.Array.getLength(propertyValues);
				elements = new Object[len];
				for (int i = 0; i < len; i++) {
					elements[i] = java.lang.reflect.Array.get(propertyValues, i);
				}
			} else {
				elements = (Object[]) propertyValues;
			}
			for (Object value : elements) {
				inPredicate.value(value);
				added++;
			}
		} else {
			// 标量: 单值场景请写 List.of(value) 或直接用 eq
			throw new IllegalArgumentException("属性[" + getPropertyName() + "]的 IN 值类型不支持: "
					+ propertyValues.getClass().getName() + " 是标量而非 Collection/数组");
		}

		if (added == 0) {
			// 空 Collection / 长度为 0 的数组: 宁可抛异常, 也不产出恒假的空 IN
			throw new IllegalArgumentException("属性[" + getPropertyName() + "]的 IN 集合为空(" +
					compareMode + "); 空集合应跳过该条件或直接返回空结果");
		}
		return inPredicate;
	}

}
