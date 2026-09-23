package com.awesomecopilot.common.lang.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  
 * <p>
 * Copyright: Copyright (c) 2019-10-17 15:16
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class Types {

	private static final Map<String, ArrayTypes> arrayTypeMap = new ConcurrentHashMap<>();
	
	static {
		//P2-39(CODE_REVIEW_REPORT): 修复前这里是逐个手写的 9 行 put——和 ArrayTypes 枚举
		//是两份要同时维护的清单, 枚举补了新类型而这里忘跟就会返回 null。
		//遍历 values() 统一构建, ArrayTypes 成为唯一维护点。
		for (ArrayTypes arrayType : ArrayTypes.values()) {
			arrayTypeMap.put(arrayType.getClassName(), arrayType);
		}
	}
	
	public static ArrayTypes arrayTypes(Object value) {
		if(value == null) {
			return null;
		}
		return arrayTypeMap.get(value.getClass().getName());
	}
}