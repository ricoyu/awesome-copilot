package com.awesomecopilot.common.lang.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * 泛型相关的反射操作
 * <p>
 * Copyright: (C), 2020/4/20 10:36
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class GenericUtils {
	
	/**
	 * 获取cls的第一个泛型参数
	 * <p>
	 * P2-18(CODE_REVIEW_REPORT): 修复前只看第一个 ParameterizedType 接口——实参是类型
	 * 变量时内外层一起中断(实测 class X implements Supplier&lt;T&gt;,
	 * Comparable&lt;String&gt; 返回 null); 嵌套泛型 Supplier&lt;Map&lt;K,V&gt;&gt;
	 * 把 ParameterizedType 强转 Class 抛 ClassCastException。
	 * 现遍历所有接口, 找到第一个"非类型变量/通配符"的实参; 实参本身是嵌套泛型时
	 * 返回其 rawType; 全部找不到返回 null。
	 *
	 * @param cls
	 * @return Class&lt;?&gt; 或 null
	 */
	public static Class<?> getTypeArgument(Class<?> cls) {
		Type[] types = cls.getGenericInterfaces();
		for (Type type : types) {
			if (!(type instanceof ParameterizedType)) {
				continue;
			}
			Type[] args = ((ParameterizedType) type).getActualTypeArguments();
			if (ArrayUtils.isEmpty(args)) {
				continue;
			}
			for (Type arg : args) {
				if (arg instanceof TypeVariable || arg instanceof WildcardType) {
					continue; //这个实参是 T 或 ? , 看下一个
				}
				if (arg instanceof Class) {
					return (Class<?>) arg;
				}
				if (arg instanceof ParameterizedType) {
					//嵌套泛型(如 Supplier<Map<String,Long>>): 返回原始类型, 修复前此处强转直接抛 ClassCastException
					return (Class<?>) ((ParameterizedType) arg).getRawType();
				}
				//GenericArrayType 等其余种类无法安全收窄为 Class, 继续找下一个实参
			}
		}
		return null;
	}
}
