package com.awesomecopilot.common.spring.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public final class SpElUtils {

	private static final Logger log = LoggerFactory.getLogger(SpElUtils.class);

	// 1. EL解析器（全局单例）
	private static final ExpressionParser spelParser = new SpelExpressionParser();
	// 2. 模板解析上下文（关键：识别#{}/}作为EL定界符）
	private static final ParserContext templateParserContext = new TemplateParserContext();

	/**
	 * 解析SpEL表达式, 如果rootObject有一个属性brandId, 那么el可以写成类似这样
	 * <pre> {@code
	 * category_brands_#{brandId}
	 * }</pre>
	 * 如果rootObject中有一个属性也是对象, 比如pmsCategoryBrandRelationDTO, 这个DTO里面也有一个brandId, 那么el可以这样写
	 * <pre> {@code
	 * category_brands_#{pmsCategoryBrandRelationDTO.brandId}
	 * }</pre>
	 *
	 * @param el
	 * @param rootObject
	 * @return
	 */
	public static String parse(String el, Object rootObject) {
		// 创建上下文，以 DTO 作为 root
		StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

		ParserContext templateContext = new TemplateParserContext();

		try {
			Expression exp = spelParser.parseExpression(el, templateContext);
			Object value = exp.getValue(context);
			log.debug("SpEL 解析：{} → {}", el, value);
			return value == null ? el : value.toString();
		} catch (Exception e) {
			log.error("SpEL 解析失败：{}，原因：{}", el, e);
			throw new RuntimeException("SpEL 解析失败", e);
		}

	}
}
