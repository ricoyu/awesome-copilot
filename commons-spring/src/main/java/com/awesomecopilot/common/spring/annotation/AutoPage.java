package com.awesomecopilot.common.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 原来分页的PageResultAspect会拦截@PostMapping注解的方法
 * 但是有些公司使用微服务的时候直接放到feign接口上, Controller实现feign接口并且不加@PostMapping注解
 * 这种情况就拦截不到了, 所以自定义一个AutoPage注解用于辅助拦截
 * <p/>
 * Copyright: Copyright (c) 2026-06-03 16:08
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoPage {}