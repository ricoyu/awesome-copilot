package com.awesomecopilot.validation.validation;

import com.awesomecopilot.validation.validation.annotation.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 强密码验证器（实现主流安全规则）
 * <p/>
 * Copyright: Copyright (c) 2026-03-06 21:16
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {
	
	// 特殊字符正则（覆盖主流允许的特殊字符）
	private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?`~]");
	// 小写字母
	private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
	// 大写字母
	private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
	// 数字
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
	
	// 注解配置参数
	private int minLength;
	private int maxLength;
	private boolean requireSpecialChar;
	private boolean requireCaseDiff;
	private boolean requireNumber;
	
	@Override
	public void initialize(Password annotation) {
		// 初始化配置参数
		this.minLength = annotation.minLength();
		this.maxLength = annotation.maxLength();
		this.requireSpecialChar = annotation.requireSpecialChar();
		this.requireCaseDiff = annotation.requireCaseDiff();
		this.requireNumber = annotation.requireNumber();
	}
	
	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		// 1. 空值验证（null 或 空字符串/纯空格）
		if (password == null) {
			setCustomMessage(context, "密码不能为空");
			return false;
		}
		String trimmedPassword = password.trim();
		if (trimmedPassword.isEmpty()) {
			setCustomMessage(context, "密码不能为空");
			return false;
		}
		
		// 2. 长度验证
		if (trimmedPassword.length() < minLength || trimmedPassword.length() > maxLength) {
			setCustomMessage(context, String.format("密码长度必须在%d-%d位之间", minLength, maxLength));
			return false;
		}
		
		// 3. 数字验证（强制时）
		if (requireNumber && !NUMBER_PATTERN.matcher(trimmedPassword).find()) {
			setCustomMessage(context, "密码必须包含数字");
			return false;
		}
		
		// 4. 大小写验证（强制区分时）
		if (requireCaseDiff) {
			boolean hasLower = LOWER_CASE_PATTERN.matcher(trimmedPassword).find();
			boolean hasUpper = UPPER_CASE_PATTERN.matcher(trimmedPassword).find();
			if (!hasLower || !hasUpper) {
				setCustomMessage(context, "密码必须同时包含大写字母和小写字母");
				return false;
			}
		} else {
			// 不强制区分大小写，但必须包含至少一个字母
			boolean hasLetter = LOWER_CASE_PATTERN.matcher(trimmedPassword).find()
					|| UPPER_CASE_PATTERN.matcher(trimmedPassword).find();
			if (!hasLetter) {
				setCustomMessage(context, "密码必须包含字母");
				return false;
			}
		}
		
		// 5. 特殊字符验证（强制时）
		if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(trimmedPassword).find()) {
			setCustomMessage(context, "密码必须包含特殊字符（!@#$%^&*()_+-=[]{}|;:,.<>?`~）");
			return false;
		}
		
		// 所有规则通过
		return true;
	}
	
	/**
	 * 设置自定义错误提示（替换默认提示）
	 */
	private void setCustomMessage(ConstraintValidatorContext context, String message) {
		// 禁用默认提示
		context.disableDefaultConstraintViolation();
		// 添加自定义提示
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}