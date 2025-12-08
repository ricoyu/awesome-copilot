package com.awesomecopilot.validation.validation.annotation;

import com.awesomecopilot.validation.validation.UniqueValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @UniqueEntity注解的更新版本, 用于校验某个字段在数据表中值的唯一性
 * <p/>
 * Copyright: Copyright (c) 2025-12-08 20:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = UniqueValueValidator.class)
@Repeatable(value = UniqueValues.class)
@Documented
public @interface UniqueValue {

	/**
	 * 要检查唯一性的数据库表字段名
	 *
	 * @return String
	 */
	String field();


	/**
	 * 要查哪张表
	 * @return String
	 */
	String table();

	/**
	 * tableField对应的Bean属性名
	 * 
	 * @return
	 */
	String property();

	/**
	 * Bean中持有主键的属性名, 默认为id, 表的主键名默认为ID
	 * 在检查唯一性的时候, 如果带主键, 那么要排除自己
	 * 
	 * @return
	 */
	String primaryKey() default "id";
	
	/**
	 * <blockquote><pre>
	 * 表设计是否采用了软删除，默认为true
	 * 即一个已经存在的被软删除的对象不会影响数据校验的结果
	 * </pre></blockquote>
	 */
	boolean isSoftDelete() default true;

	/**
	 * 表的采用软删除的字段名
	 * @return
	 */
	String softDeleteField() default "deleted";

	String message() default "Entity already exists.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String[] value() default "";
}