package com.awesomecopilot.workbook.convertor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * 将Cell中读到的数据转换成POJO中对应字段的数据类型, 提供一些公共属性, 方法给子类使用
 * 
 * <p>
 * Copyright: Copyright (c) 2019-05-09 21:22
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 * @on
 * @param <S>
 * @param <T>
 */
public abstract class AbstractConvertor<S, T> implements Convertor<S, T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractConvertor.class);
	private Field field;

	/**
	 * 保存了Cell所在列名, 没有的话则为null
	 */
	private String columnName;

	/**
	 * Cell的索引, 从0开始
	 */
	private int cellIndex;
	
	public Field getField() {
		return field;
	}
	
	public void setField(Field field) {
		this.field = field;
	}
	
	public String getColumnName() {
		return columnName;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public int getCellIndex() {
		return cellIndex;
	}
	
	public void setCellIndex(int cellIndex) {
		this.cellIndex = cellIndex;
	}
}