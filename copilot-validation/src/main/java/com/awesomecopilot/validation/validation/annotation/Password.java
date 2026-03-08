package com.awesomecopilot.validation.validation.annotation;

import com.awesomecopilot.validation.validation.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在密码字段上, 验证密码符合密码规范, 比如大小写, 特殊字符, 长度等
 
 * 可配置：最小长度、最大长度、是否强制特殊字符、是否强制大小写分离
 * <p/>
 * Copyright: Copyright (c) 2026-03-06 21:13
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordValidator.class)
public @interface Password {

    /**
     * 默认错误提示（通用）
     */
    String message() default "密码不符合安全规则";

    /**
     * 验证分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载信息
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 最小长度（默认8位，主流网站最低要求）
     */
    int minLength() default 8;

    /**
     * 最大长度（默认20位，避免密码过长）
     */
    int maxLength() default 20;

    /**
     * 是否强制包含特殊字符（默认true）
     * 特殊字符范围：!@#$%^&*()_+-=[]{}|;:,.<>?`~
     */
    boolean requireSpecialChar() default true;

    /**
     * 是否强制区分大小写（默认true，必须同时包含大写+小写字母）
     */
    boolean requireCaseDiff() default true;

    /**
     * 是否强制包含数字（默认true）
     */
    boolean requireNumber() default true;
}