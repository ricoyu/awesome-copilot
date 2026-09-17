package com.awesomecopilot.web.resolver;

import com.awesomecopilot.common.lang.utils.DateUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller LocalDateTime 参数类型绑定
 * 需要调用WebMvcConfigurer#addArgumentResolvers来添加
 * <p>
 * 解析优先级(评审报告 P1-4): 参数上的 @DateTimeFormat 最高。pattern 属性优先,
 * 其次按 iso 指示的 ISO 日期时间格式解析, 都没有才走 DateUtils 自动匹配。
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:08
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocalDateTimeArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return LocalDateTime.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		String value = request.getParameter(parameter.getParameterName());

		if (StringUtils.isBlank(value)) {
			return null;
		}

		DateTimeFormat dateTimeFormat = parameter.getParameterAnnotation(DateTimeFormat.class);
		if (dateTimeFormat != null) {
			if (StringUtils.isNotBlank(dateTimeFormat.pattern())) {
				return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(dateTimeFormat.pattern()));
			}
			DateTimeFormat.ISO iso = dateTimeFormat.iso();
			if (iso != null && iso != DateTimeFormat.ISO.NONE) {
				// ISO_DATE_TIME(而非 ISO_LOCAL_DATE_TIME)与 Spring 官方 iso 语义一致,
				// 可解析带 Z/+08:00 偏移的值(取本地时间部分, 偏移被丢弃——LocalDateTime 本就不含时区)
				return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value));
			}
		}

		return DateUtils.toLocalDateTime(value);
	}
}
