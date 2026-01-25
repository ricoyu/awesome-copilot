package com.awesomecopilot.common.lang.utils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 动态创建对象并用指定的值初始化属性, 基于byte-buddy实现, 适配Java17
 * <p/>
 * Copyright: Copyright (c) 2026-01-20 18:15
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DynamicUtils {

	/**
	 * 基于某个类创建一个动态对象，并设置属性值
	 * 同时为这些属性生成getter/setter
	 *
	 * @param targetClass The base class to extend (e.g., Object.class or a custom class).
	 * @param properties  A map where keys are property names and values are initial values (types inferred from
	 *                    values).
	 * @param <T>         The type of the target class.
	 * @return An instance of the dynamically created class, cast to T.
	 * @throws Exception If there's an issue with class generation, instantiation, or reflection.
	 */
	public static <T> T createObject(Class<T> targetClass, Map<String, Object> properties) {
		if (properties == null || properties.isEmpty()) {
			// If no properties, just instantiate the target class if possible
			try {
				return targetClass.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 输入属性值是null, 那么在生成对象的时候无法进行类型推断导致报错
		 */
		for (Map.Entry entry : properties.entrySet()) {
			if (entry.getValue() == null) {
				properties.remove(entry.getKey());
			}
		}

		ByteBuddy byteBuddy = new ByteBuddy();
		DynamicType.Builder<T> builder = byteBuddy.subclass(targetClass);

		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				throw new IllegalArgumentException("Property values cannot be null; type cannot be inferred.");
			}
			Class<?> type = value.getClass();

			// Add private field
			builder = builder.defineField(name, type, Visibility.PRIVATE);

			// Add getter
			String getterName = "get" + capitalize(name);
			builder = builder.defineMethod(getterName, type, Visibility.PUBLIC)
					.intercept(FieldAccessor.ofField(name));

			// Add setter
			String setterName = "set" + capitalize(name);
			builder = builder.defineMethod(setterName, void.class, Visibility.PUBLIC)
					.withParameter(type)
					.intercept(FieldAccessor.ofField(name));
		}

		// Build the dynamic type
		DynamicType.Unloaded<T> dynamicType = builder.make();

		// 关键修改：使用当前线程的上下文 ClassLoader 作为 parent
		// 这样生成的类就能看到项目中的所有类（包括 PmsCategoryBrandRelationDTO）
		ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
		if (parentLoader == null) {
			parentLoader = DynamicUtils.class.getClassLoader();
		}

		Class<? extends T> dynamicClass = dynamicType
				.load(parentLoader, ClassLoadingStrategy.Default.WRAPPER)
				.getLoaded();

		try {
			// Instantiate
			Constructor<? extends T> constructor = dynamicClass.getDeclaredConstructor();
			T instance = constructor.newInstance();

			// Set initial values using setters
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				String name = entry.getKey();
				Object value = entry.getValue();
				String setterName = "set" + capitalize(name);
				Method setter = dynamicClass.getMethod(setterName, value.getClass());
				setter.invoke(instance, value);
			}

			return instance;
		} catch (Exception e) {
			throw new RuntimeException("动态对象创建失败", e);
		}
	}

	private static String capitalize(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}