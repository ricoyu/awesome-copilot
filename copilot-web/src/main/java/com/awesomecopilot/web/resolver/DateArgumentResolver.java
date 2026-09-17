package com.awesomecopilot.web.resolver;

import com.awesomecopilot.common.lang.exception.DateParseException;
import com.awesomecopilot.common.lang.utils.DateUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 支持Controller方法参数各种日期格式绑定, 需要调用WebMvcConfigurer#addArgumentResolvers来添加
 * <p>
 * 解析优先级(评审报告 P1-4): 参数上的 @DateTimeFormat(pattern=...) 最高, 其次按 iso 指示的
 * ISO格式解析, 都没有才走 DateUtils.parse 的自动格式匹配。旧实现直接自动匹配, 开发者写在
 * 参数上的 @DateTimeFormat 被无声忽略。
 *
 * @author Rico Yu
 * @since May 22, 2016
 * @version
 *
 */
public class DateArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Date.class.isAssignableFrom(parameter.getParameterType());
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
				return parseStrict(value, dateTimeFormat.pattern());
			}
			DateTimeFormat.ISO iso = dateTimeFormat.iso();
			if (iso != null && iso != DateTimeFormat.ISO.NONE) {
				return fromIso(value, iso);
			}
		}

		return DateUtils.parse(value);
	}

	/**
	 * 严格模式按 pattern 解析: DateUtils.parse(source, format) 底层 SimpleDateFormat 是
	 * lenient 模式(评审独立复核发现), "2026-09-17" 按 "dd-MM-yyyy" 会不报错地解析出公元23年的
	 * 错值而不报错, 这里自建 SimpleDateFormat 关掉宽松模式, 形态不符直接抛 ParseException。
	 * 时区固定 Asia/Shanghai、Locale 用 CHINA, 与 DateUtils/SimpleDateFormatHolder 的时区、Locale 设置保持一致。
	 */
	private Date parseStrict(String value, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINA);
		sdf.setLenient(false);
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		try {
			return sdf.parse(value);
		} catch (ParseException e) {
			throw new DateParseException("Parse date string:[" + value + "] with pattern:[" + pattern + "]", e);
		}
	}

	/**
	 * 按 @DateTimeFormat(iso=...) 解析。用 ISO_DATE/ISO_TIME/ISO_DATE_TIME(而非 LOCAL 版),
	 * 与 Spring 官方 iso 语义一致——LOCAL 版会拒绝 2026-09-17T08:30:00.003Z 这类带时区偏移的
	 * 值, 而旧代码自动匹配路径本来能解析它, 独立评审确认那构成修复引入的倒退。
	 * 值带偏移时按偏移换算真实时点; 不带偏移按 Asia/Shanghai 解释, 与 pattern 分支使用相同的时区设置。
	 */
	private Date fromIso(String value, DateTimeFormat.ISO iso) {
		ZoneId china = ZoneId.of("Asia/Shanghai");
		switch (iso) {
			case DATE: {
				TemporalAccessor parsed = DateTimeFormatter.ISO_DATE.parse(value);
				return Date.from(LocalDate.from(parsed).atStartOfDay(china).toInstant());
			}
			case TIME: {
				TemporalAccessor parsed = DateTimeFormatter.ISO_TIME.parse(value);
				return Date.from(LocalTime.from(parsed).atDate(LocalDate.now(china)).atZone(china).toInstant());
			}
			case DATE_TIME: {
				TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME.parse(value);
				if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
					// 带 Z 或 +08:00 之类偏移: 偏移定义了绝对时点, 直接取 Instant
					return Date.from(Instant.from(parsed));
				}
				return Date.from(LocalDateTime.from(parsed).atZone(china).toInstant());
			}
			default: {
				// DateTimeFormat.ISO 只有以上取值+NONE, 此分支为防御性回退
				Date parsed = DateUtils.parse(value);
				if (parsed == null) {
					throw new IllegalArgumentException("无法按ISO格式解析日期参数: " + value);
				}
				return parsed;
			}
		}
	}

}
