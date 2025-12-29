package com.awesomecopilot.json.jackson.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级别转义豁免注解
 * ✅ 标注在VO类上 → 整个VO的所有字符串字段序列化时，全部原样输出（豁免HTML转义）
 * ✅ 未标注 → 全局默认自动转义
 */
@Target({ElementType.TYPE})  // 仅作用于类
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UnescapeHtml {
}