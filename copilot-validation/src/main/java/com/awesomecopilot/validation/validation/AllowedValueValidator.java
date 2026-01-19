package com.awesomecopilot.validation.validation;

import com.awesomecopilot.validation.validation.annotation.AllowedValues;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 验证字段值是否在允许范围内（支持普通类型+枚举类型）
 * <p>
 * Copyright: Copyright (c) 2020-08-18 10:21
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class AllowedValueValidator implements ConstraintValidator<AllowedValues, Object> {
	private String[] candidateValues = null;
	private String[] exceptValues = null;
	private boolean caseSensitive = false;
	private boolean mandatory = true;

	@Override
	public void initialize(AllowedValues constraintAnnotation) {
		candidateValues = constraintAnnotation.value();
		exceptValues = constraintAnnotation.except();
		caseSensitive = constraintAnnotation.caseSensitive();
		mandatory = constraintAnnotation.mandatory();

		checkIfValueConflict();
	}

	/**
	 * Allowed values cannot conflict with except values
	 */
	private void checkIfValueConflict() {
		if (candidateValues.length > 0 && exceptValues.length > 0) {
			throw new ConstraintDeclarationException("value and except cannot exist in both. ");
		}
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		// 1. 处理null值的必填校验
		if (null == value) {
			if (mandatory) {
				context.disableDefaultConstraintViolation();
				String mandatoryMsg = (String) ((ConstraintValidatorContextImpl) context)
						.getConstraintDescriptor().getAttributes().get("mandatoryMsg");
				context.buildConstraintViolationWithTemplate(mandatoryMsg)
						.addConstraintViolation();
				return false;
			}
			return true;
		}

		// 2. 提取待校验值的字符串形式（适配枚举+普通类型）
		String ordinalStr = getOrdinalAsString(value);
		String nameStr = getNameAsString(value);

		// 3. 处理排除值逻辑
		for (String except : exceptValues) {
			String exceptStr = caseSensitive ? except : except.toLowerCase();
			String targetValueStr = caseSensitive ? ordinalStr : ordinalStr.toLowerCase();

			if (targetValueStr.equals(exceptStr)) {
				return false;
			}
		}
		// 如果配置了排除值且未命中，直接返回true
		if (exceptValues.length > 0) {
			return true;
		}

		// 4. 处理允许值逻辑
		for (String candidate : candidateValues) {
			String candidateStr = caseSensitive ? candidate : candidate.toLowerCase();
			String targetValueStr = caseSensitive ? ordinalStr : ordinalStr.toLowerCase();

			if (targetValueStr.equals(candidateStr)) {
				return true;
			}
		}

		for (String candidate : candidateValues) {
			String candidateStr = caseSensitive ? candidate : candidate.toLowerCase();
			String targetValueStr = caseSensitive ? nameStr : nameStr.toLowerCase();

			if (targetValueStr.equals(candidateStr)) {
				return true;
			}
		}

		// 未命中任何允许值，校验失败
		return false;
	}

	/**
	 * 提取值的字符串形式（核心适配枚举类型）
	 * @param value 待校验值
	 * @return 字符串形式的校验值
	 */
	private String getOrdinalAsString(Object value) {
		// 如果是枚举类型，提取枚举的name()（推荐）或ordinal()
		if (value instanceof Enum<?>) {
			// 方案1：如果需要用枚举的序号（如0、1、2），取消下面注释并注释上面一行
			 return String.valueOf(((Enum<?>) value).ordinal());
		}

		// 普通类型直接转字符串
		return value.toString();
	}

	/**
	 * 提取值的字符串形式（核心适配枚举类型）
	 * @param value 待校验值
	 * @return 字符串形式的校验值
	 */
	private String getNameAsString(Object value) {
		// 如果是枚举类型，提取枚举的name()（推荐）或ordinal()
		if (value instanceof Enum<?>) {
			// 方案2：使用枚举的名称（如枚举值为USER，取"USER"）【推荐】
			return ((Enum<?>) value).name();
		}

		// 普通类型直接转字符串
		return value.toString();
	}
}