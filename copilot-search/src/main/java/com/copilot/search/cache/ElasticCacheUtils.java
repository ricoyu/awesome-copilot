package com.copilot.search.cache;

import com.copilot.common.lang.transformer.Transformers;
import com.copilot.common.lang.utils.ReflectionUtils;
import com.copilot.search.annotation.DocId;
import com.copilot.search.cache.FieldCache.CacheItem;

import java.lang.reflect.Field;

/**
 * <p>
 * Copyright: (C), 2021-01-16 9:15
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticCacheUtils {
	
	private static FieldCache fieldCache = new FieldCache();
	
	/**
	 * 找给定的Class里面标注了@DocId注解的字段
	 *
	 * @param clazz
	 * @return Field
	 */
	public static Field idField(Class clazz) {
		if (clazz == null) {
			return null;
		}
		if (!ReflectionUtils.isPojo(clazz)) {
			return null;
		}
		
		//从缓存中根据class获取@DocId字段
		CacheItem cacheItem = fieldCache.get(clazz);
		
		//已经缓存过这个Class
		if (cacheItem != null) {
			//这个Class不存在标注了@DocId的字段
			if (cacheItem.getField() == null) {
				return null;
			}
			
			//这个Class存在标注了@DocId的字段
			return cacheItem.getField();
		}
		
		/*
		 * 缓存中不存在以这个Class对象为key的item,
		 * 表示还没有分析过这个Class对象, 通过反射获取它所有的字段
		 * 拿到这个class的所有字段, 包括自己定义的和父类中定义的字段, 但是不包括Object对象中的字段
		 */
		Field[] fields = ReflectionUtils.getFields(clazz);
		
		for (Field field : fields) {
			//如果找到标记了@DocId注解的字段, 把这个字段的值作为文档的_id
			DocId elasticId = field.getAnnotation(DocId.class);
			if (elasticId != null) {
				//先把这个field对象加入缓存, 这样下次就不用通过反射再找一遍了
				fieldCache.put(clazz, field);
				return field;
			}
		}
		
		/*
		 * 走到这里表示这个Class对象中不存在@DocId字段
		 * 但是也要把 "这个Class对象中不存在@DocId字段" 这个结果加入缓存, 这样下次再
		 * 找这个clazz对象的时候可以直接从缓存中取
		 */
		fieldCache.put(clazz, null);
		
		return null;
	}
	
	/**
	 * 找给定的POJO里面标注了@DocId注解的字段
	 *
	 * @param pojo
	 * @return Field
	 */
	public static Field idField(Object pojo) {
		if (pojo == null) {
			return null;
		}
		return idField(pojo.getClass());
	}
	
	/**
	 * 如果对象是一个pojo(不是String, 不是List, 不是Map, 不是简单类型)<br/>
	 * 那么看一下这个POJO中有没有加了@DocId注解的字段, 有的话把这个字段的值当作整片文档的id
	 *
	 * @param pojo
	 * @return String
	 */
	public static String getIdValue(Object pojo) {
		Field id = idField(pojo);
		if (id == null) {
			return null;
		}
		Object value = ReflectionUtils.getFieldValue(id, pojo);
		//字段值转成字符串类型
		return Transformers.toString(value);
	}
	
	/**
	 * 给标注了@DocId的字段赋值
	 *
	 * @param pojo
	 * @param value
	 */
	public static void setIdValue(Object pojo, String value) {
		Field id = idField(pojo);
		if (id == null) {
			return;
		}
		
		ReflectionUtils.setField(id, pojo, value);
	}
}
