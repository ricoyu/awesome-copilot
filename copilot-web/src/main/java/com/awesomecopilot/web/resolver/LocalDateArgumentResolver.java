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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 支持Controller方法参数各种日期格式绑定, 需要调用WebMvcConfigurer#addArgumentResolvers来添加
 * <p>
 * 解析优先级(评审报告 P1-4): 参数上的 @DateTimeFormat 最高。pattern 属性优先,
 * 其次按 iso 指示的 ISO 日期格式解析, 都没有才走 DateUtils 自动匹配。
 *
 * @author Rico Yu
 * @since May 22, 2016
 * @version
 *
 */
public class LocalDateArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return LocalDate.class.isAssignableFrom(parameter.getParameterType());
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
				return LocalDate.parse(value, DateTimeFormatter.ofPattern(dateTimeFormat.pattern()));
			}
			DateTimeFormat.ISO iso = dateTimeFormat.iso();
			if (iso != null && iso != DateTimeFormat.ISO.NONE) {
				// ISO_DATE(而非 ISO_LOCAL_DATE)与 Spring 官方 iso 语义一致, 允许可选的时区后缀
				return LocalDate.from(DateTimeFormatter.ISO_DATE.parse(value));
			}
		}

		return DateUtils.toLocalDate(value);
	}
}
