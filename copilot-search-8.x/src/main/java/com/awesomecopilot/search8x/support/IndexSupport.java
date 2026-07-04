package com.awesomecopilot.search8x.support;

import com.awesomecopilot.search8x.annotation.Index;

/**
 * 从Entity上的@Index注解抽取索引名
 * <p>
 * Copyright: (C), 2021-05-06 14:09
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class IndexSupport {
	
	/**
	 * 从@Index注解上拿索引名
	 * @param entityClass 
	 * @return String 索引名
	 */
	public static String indexName(Class entityClass) {
		if (entityClass == null) {
			return null;
		}
		
		Index index = (Index) entityClass.getAnnotation(Index.class);
		if (index == null) {
			return null;
		}
		
		return index.value().trim();
	}
}
