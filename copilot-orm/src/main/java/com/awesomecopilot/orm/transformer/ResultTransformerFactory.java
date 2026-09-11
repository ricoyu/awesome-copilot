package com.awesomecopilot.orm.transformer;

import org.springframework.util.Assert;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ResultTransformerFactory {

	//命名sql查询的name+resultClass的名字作为键，相应的ValueHandlerResultTransformer作为value
	private static final ConcurrentHashMap<String, ValueHandlerResultTransformer> cache = new ConcurrentHashMap<>();

	private ResultTransformerFactory() {
	}

	public static ValueHandlerResultTransformer getResultTransformer(String queryName, Class<?> resultClass) {
		return getResultTransformer(queryName, resultClass, null);
	}

	public static ValueHandlerResultTransformer getResultTransformer(String queryName, Class<?> resultClass, String queryMode) {
		return getResultTransformer(queryName, resultClass, queryMode, null);
	}
	
	/**
	 * 工厂缓存 key 必须包含 queryMode 与 enumLookupProperties:
	 * 它们直接影响 transformer 的行为(strict 缺列报错/loose 容忍; enum 按哪个属性查),
	 * 旧 key 只含 SQL+resultClass, 同一个 SQL 先 loose 后 strict 会拿回旧实例, 行为悄悄变。
	 */
	public static ValueHandlerResultTransformer getResultTransformer(String queryName, Class<?> resultClass, String queryMode,
			Set<String> enumLookupProperties) {
		Assert.notNull(resultClass, "resultClass cannot be null");
		Assert.notNull(queryName, "queryName cannot be null");
		// null 与空集合要区分对待吗? 不——enumLookupProperties 为空集合和为 null 行为一致(都不按属性查),
		// 归一成同一个字符串避免同一配置产生两个 key
		String modePart = queryMode == null ? "" : queryMode.toLowerCase();
		String enumPart = (enumLookupProperties == null || enumLookupProperties.isEmpty())
				? ""
				: new TreeSet<>(enumLookupProperties).toString();
		String key = queryName + resultClass.getName() + "#" + modePart + "#" + enumPart;
		return cache.computeIfAbsent(key, (k) -> {
			ValueHandlerResultTransformer transformer = new ValueHandlerResultTransformer(resultClass, queryMode);
			if(enumLookupProperties != null) {
				transformer.setEnumLookupProperties(enumLookupProperties);
			}
			return transformer;
		});
	}
}
