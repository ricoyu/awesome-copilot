package com.awesomecopilot.web.utils;

import com.awesomecopilot.common.spring.context.ApplicationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 获取MessageSource帮助类
 * <p>
 * Copyright: Copyright (c) 2019-10-14 15:26
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MessageHelper {

	private static final Logger log = LoggerFactory.getLogger(MessageHelper.class);

	private static volatile MessageSource messageSource;

	/**
	 * 标记"查过容器但确实没有 MessageSource Bean"。没有这个标记时, 每次调用都会
	 * 重新走一遍容器查找; 有了它, 缺失状态只WARN一次, 后续直接走降级分支。
	 */
	private static volatile boolean messageSourceMissing;

	/**
	 * 懒加载MessageSource。应用没配MessageSource(或根本不在Spring环境)时不抛异常,
	 * 返回null让调用方走defaultMessage/code降级; 但会WARN一次, 让"消息一直取不到"
	 * 这种状态在日志里可查(评审报告 P1-2: 调用方拿到null不能没有提示)。
	 */
	private static MessageSource messageSource() {
		MessageSource ms = messageSource;
		if (ms != null) {
			return ms;
		}
		if (messageSourceMissing) {
			return null;
		}
		synchronized (MessageHelper.class) {
			// 双检锁: 进锁后先看一眼别的线程是否已经取到, 再查容器
			if (messageSource != null) {
				return messageSource;
			}
			if (messageSourceMissing) {
				return null;
			}
			try {
				ms = ApplicationContextHolder.getBean(MessageSource.class);
			} catch (BeansException e) {
				// 容器里没有MessageSource Bean时Spring抛NoSuchBeanDefinitionException, 视为缺失
				ms = null;
			}
			if (ms == null) {
				messageSourceMissing = true;
				log.warn("Spring容器中找不到MessageSource Bean(Bean名称须为messageSource), 国际化消息将降级为默认消息; 若应用确实不需要i18n可忽略本警告");
				return null;
			}
			messageSource = ms;
			return ms;
		}
	}

	public static final String getMessage(String code) {
		MessageSource ms = messageSource();
		if (ms == null) {
			return null;
		}

		try {
			return ms.getMessage(code, null, LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException e) {
			log.debug("找不到国际化消息, code={}", code);
			return null;
		}
	}

	public static final String getMessage(Supplier<String> templateSupplier) {
		return getMessage(templateSupplier.get());
	}

	public static final String getMessage(String code, String defaultMessage) {
		MessageSource ms = messageSource();
		if (ms == null) {
			return defaultMessage;
		}

		return ms.getMessage(code, null, defaultMessage, LocaleContextHolder.getLocale());
	}

	public static final String getMessage(String code, List<Object> args) {
		MessageSource ms = messageSource();
		if (ms == null) {
			return null;
		}

		try {
			return ms.getMessage(code, args.toArray(new Object[0]), LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException e) {
			log.debug("找不到国际化消息, code={}", code);
			return null;
		}
	}

	public static final String getMessage(String code, String defaultMessage, Object... args) {
		MessageSource ms = messageSource();
		if (ms == null) {
			return defaultMessage;
		}

		try {
			return ms.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException e) {
			log.debug("找不到国际化消息, code={}", code);
			return null;
		}
	}

	/**
	 * 获取指定语言的信息
	 *
	 * @param code
	 * @param locale
	 * @param args
	 * @return
	 */
	public static String getMessage(String code, Locale locale, Object... args) {
		MessageSource ms = messageSource();
		if (ms == null) {
			// 该重载的语义是"取不到返回code本身", 与下面try块里defaultMessage传code一致
			return code;
		}

		try {
			return ms.getMessage(code, args, code, locale);
		} catch (NoSuchMessageException e) {
			log.debug("找不到国际化消息, code={}", code);
			return null;
		}
	}

	public static String getParameteredMessage(String code, Object... args) {
		MessageSource ms = messageSource();
		if (ms == null) {
			return null;
		}

		try {
			return ms.getMessage(code, args, LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException e) {
			log.debug("找不到国际化消息, code={}", code);
			return null;
		}
	}

}
