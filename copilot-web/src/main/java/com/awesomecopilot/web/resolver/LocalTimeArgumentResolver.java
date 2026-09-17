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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 支持Spring Controller LocalTime类型参数绑定 
 * 需要调用WebMvcConfigurer#addArgumentResolvers来添加
 * <p>
 * 解析优先级(评审报告 P1-4): 参数上的 @DateTimeFormat 最高。pattern 属性优先,
 * 其次按 iso 指示的 ISO 时间格式解析, 都没有才走 DateUtils 自动匹配。
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocalTimeArgumentResolver implements HandlerMethodArgumentResolver {
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return LocalTime.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		String value = request.getParameter(parameter.getParameterName());

		if (StringUtils.isBlank(value)) {
			return null;
		}

		DateTimeFormat dateTimeFormat = parameter.getParameterAnnotation(DateTimeFormat.class);
		if (dateTimeFormat != null) {
			if (StringUtils.isNotBlank(dateTimeFormat.pattern())) {
				return LocalTime.parse(value, DateTimeFormatter.ofPattern(dateTimeFormat.pattern()));
			}
			DateTimeFormat.ISO iso = dateTimeFormat.iso();
			if (iso != null && iso != DateTimeFormat.ISO.NONE) {
				// ISO_TIME(而非 ISO_LOCAL_TIME)与 Spring 官方 iso 语义一致, 允许带时区后缀
				return LocalTime.from(DateTimeFormatter.ISO_TIME.parse(value));
			}
		}

		return DateUtils.toLocalTime(value);
	}
}
