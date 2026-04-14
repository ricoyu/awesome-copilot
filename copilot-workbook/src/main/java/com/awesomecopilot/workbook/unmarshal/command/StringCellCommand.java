package com.awesomecopilot.workbook.unmarshal.command;

import com.awesomecopilot.workbook.utils.ReflectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * 负责从Cell中读取值并设置到String类型的字段上
 * <p>
 * Copyright: Copyright (c) 2019-05-23 14:41
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class StringCellCommand extends BaseCellCommand {
	private static final Logger log = LoggerFactory.getLogger(StringCellCommand.class);

	private DataFormatter df = new DataFormatter();
	
	public StringCellCommand(Field field) {
		super(field);
	}

	@Override
	public void invoke(Cell cell, Object pojo) {
		if (cell.getCellTypeEnum() == CellType.STRING) {
			String value = str(cell);
			if (value != null) {
				ReflectionUtils.setField(field, pojo, value);
			}
			return;
		}

		if (cell.getCellTypeEnum() == CellType.NUMERIC || cell.getCellTypeEnum() == CellType.FORMULA) {
			String value = df.formatCellValue(cell);
			if (value != null) {
				ReflectionUtils.setField(field, pojo, value);
			}
		}
	}

}