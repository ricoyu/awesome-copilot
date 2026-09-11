package com.awesomecopilot.orm.it.entity;

/**
 * 测试用的状态枚举, 用于验证原生SQL结果集到枚举属性的映射(按 name 或 ordinal 查)
 *
 * @author Rico Yu
 */
public enum BookStatus {

	DRAFT(0),
	PUBLISHED(1),
	ARCHIVED(2);

	private final int code;

	BookStatus(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
