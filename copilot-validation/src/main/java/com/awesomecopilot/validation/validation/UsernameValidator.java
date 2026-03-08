package com.awesomecopilot.validation.validation;

import com.awesomecopilot.validation.validation.annotation.Username;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 可以包含字母、数字、下划线、@符号，必须以字母或者数字开头
 *
 * https://www.mkyong.com/regular-expressions/how-to-validate-username-with-regular-expression/
 * <p>
 * Copyright: Copyright (c) 2018-03-29 17:13
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class UsernameValidator implements ConstraintValidator<Username, String> {
	
	private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_@]*$");
	
	@Override
	public void initialize(Username constraintAnnotation) {
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (isBlank(value)) {
			return false;
		}
		
		if (value.length() < 3 || value.length() > 15) {
			return false;
		}
		
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
}