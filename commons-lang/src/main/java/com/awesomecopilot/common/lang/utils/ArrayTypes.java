package com.awesomecopilot.common.lang.utils;

/**
 * 定义常见的数组类型class名字
 * <p>
 * Copyright: Copyright (c) 2018-06-13 18:14
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public enum ArrayTypes {

	LONG_WRAPPER("[Ljava.lang.Long;"),
	LONG("[J"),
	INTEGER_WRAPPER("[Ljava.lang.Integer;"),
	INTEGER("[I"),
	DOUBLE_WRAPPER("[Ljava.lang.Double;"),
	DOUBLE("[D"),
	FLOAT_WRAPPER("[Ljava.lang.Float;"),
	FLOAT("[F"),
	STRING("[Ljava.lang.String;"),
	//P2-39(CODE_REVIEW_REPORT): 修复前注册表缺 boolean/short/byte/char 四类基本类型数组
	//及其包装类数组——Types.arrayTypes(new byte[0]) 返回 null 且没有任何提示
	BOOLEAN_WRAPPER("[Ljava.lang.Boolean;"),
	BOOLEAN("[Z"),
	SHORT_WRAPPER("[Ljava.lang.Short;"),
	SHORT("[S"),
	BYTE_WRAPPER("[Ljava.lang.Byte;"),
	BYTE("[B"),
	CHAR_WRAPPER("[Ljava.lang.Character;"),
	CHAR("[C");

	private String className;

	private ArrayTypes(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

}