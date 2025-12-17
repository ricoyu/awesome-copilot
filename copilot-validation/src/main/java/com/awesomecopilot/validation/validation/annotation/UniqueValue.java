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
	 * 要查哪张表
	 * @return String
	 */
	String table();

	/**
	 * 要检查唯一性的数据库表字段名, 一般不需要显式提供, 根据property推断即可; 但如果开发人员命名的表字段和bean属性名不一致, 可以显式指定
	 * 如果同时指定, 表字段名以这个为准
	 *
	 * @return String
	 */
	String field() default "";

	/**
	 * 如果是驼峰式的, 表示这个bean的哪个属性是唯一性的, 然后对应的表字段名是转成下划线风格的名字
	 * 如果是下划线分隔的, 默认认为是数据库字段名, 会自动转成驼峰式bean属性名
	 * 
	 * @return
	 */
	String property();

	/**
	 * Bean中持有主键的属性名, 或者数据库字段名
	 * 如果是驼峰式的, 默认认为是bean属性名, 会自动转成_分隔的数据库字段名,
	 * 同理, 如果是下划线分隔的, 默认认为是数据库字段名, 会自动转成驼峰式bean属性名
	 * 
	 * @return
	 */
	String primaryKey();
	
	/**
	 * <blockquote><pre>
	 * 表设计是否采用了软删除，默认为true
	 * 即一个已经存在的被软删除的对象不会影响数据校验的结果
	 * </pre></blockquote>
	 */
	boolean isSoftDelete() default false;

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