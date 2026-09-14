package com.awesomecopilot.json.jackson;

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

	private static final Logger log = LoggerFactory.getLogger(ObjectMapperFactory.class);

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
									Class.forName("com.awesomecopilot.common.spring.context.ApplicationContextHolder");
							objectMapper = (ObjectMapper) ReflectionUtils.invokeStatic("getBean", achClass, ObjectMapper.class);
						} catch (ClassNotFoundException e) {
							log.warn("非Spring环境, 直接new一个ObjectMapper");
						} catch (RuntimeException e) {
							// 容器里没有ObjectMapper bean时 getBean 抛 NoSuchBeanDefinitionException(经反射包装成
							// RuntimeException往上抛)。不拦的话 JacksonUtils 静态块直接 ExceptionInInitializerError,
							// 整个JSON工具类报废——bean缺失应该降级而非炸类, 这里只认这个用途所以拿不到就自建.
							log.warn("从Spring容器获取ObjectMapper失败({}), 降级为自建实例", e.getMessage());
						}
					}
					if (objectMapper == null) {
						// 走到了这里: 要么非Spring环境, 要么Spring环境但容器还没就绪/没有ObjectMapper bean.
						// 工具类从此用自建实例, 与Web层可能存在的容器bean是两套配置——必须让使用者知道.
						log.warn("未取得Spring容器中的ObjectMapper(非Spring环境或容器未就绪), JSON工具类使用独立实例; "
								+ "若应用同时依赖容器bean, 两边的序列化行为可能出现差异");
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