package com.awesomecopilot.common.spring.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SpringBeanUtil
 * <p/>
 * Copyright: Copyright (c) 2026-07-15 10:31
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Component
public class SpringBeanUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringBeanUtil.applicationContext == null) {
            SpringBeanUtil.applicationContext = applicationContext;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Map<String, Object> getBeans(Class clazz) {
        Map<String, Object> beansOfType = applicationContext.getBeansOfType(clazz);
        return beansOfType;
    }
    
    /**
     * 找到打了annotationClass注解的bean
     * @param annotationClass
     * @return
     */
    public static Map<String, Object> getAnnotationBeans(Class annotationClass) {
        Map<String, Object> beansOfType = applicationContext.getBeansWithAnnotation(annotationClass);
        return beansOfType;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    public static <T> T getBean(String name, Class<T> beanClass) {
        return applicationContext.getBean(name, beanClass);
    }
}