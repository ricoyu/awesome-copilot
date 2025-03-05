package com.copilot.common.spring.annotation.aspect;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Copyright: (C), 2020/4/11 17:43
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Aspect
@Slf4j
@Component
public class SmartLoggerAspect {

	private static Logger logger = LoggerFactory.getLogger(SmartLoggerAspect.class);

	private ObjectMapper objectMapper = new ObjectMapper();

	{
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		//对POJO字段排序
		objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
	}
	
	@Pointcut("@annotation(com.copilot.common.spring.annotation.SmartLogger)")
	public void pointcut() {
	}
	
	@Before("pointcut()")
	public void before(JoinPoint joinPoint) {
		log.info("方法名: {}", joinPoint.getSignature().getName());
		try {
			log.info("参数: {}", objectMapper.writeValueAsString(joinPoint.getArgs()));
		} catch (JsonProcessingException e) {
			logger.info("打印参数报错", e);
		}
	}
	
}
