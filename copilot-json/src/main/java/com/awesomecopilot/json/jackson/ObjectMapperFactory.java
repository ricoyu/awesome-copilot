package com.awesomecopilot.json.jackson;

import com.awesomecopilot.common.lang.concurrent.Concurrent;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ObjectMapper 工厂类
 * <p>
 * Copyright: Copyright (c) 2019-10-15 10:01
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ObjectMapperFactory {

	private static final Logger log = LoggerFactory.getLogger(Concurrent.class);

	private static volatile ObjectMapper objectMapper;
	
	/**
	 * 如果是Spring环境, 优先从Spring容器中取ObjectMapper, 如果没有, 则自己创建一个
	 * @return ObjectMapper
	 */
	public static ObjectMapper createOrFromBeanFactory() {
		if (objectMapper == null) {
			synchronized (ObjectMapperFactory.class) {
				if (objectMapper == null) {
					boolean exists = ReflectionUtils.existsClass("org.springframework.context.ApplicationContext");
					if (exists) {
						try {
							Class<?> achClass =
									Class.forName("com.copilot.common.spring.context.ApplicationContextHolder");
							objectMapper = (ObjectMapper) ReflectionUtils.invokeStatic("getBean", achClass, ObjectMapper.class);
						} catch (ClassNotFoundException e) {
							log.warn("非Spring环境, 直接new一个ObjectMapper");
						}
					}
					if (objectMapper == null) {
						objectMapper = new ObjectMapper();
						return objectMapper;
					}
					return objectMapper;
				}
				
				return objectMapper;
			}
		}
		
		return objectMapper;
	}
}