package com.awesomecopilot.workbook.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 
 * <p>
 * Copyright: Copyright (c) 2019-10-15 10:42
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MathUtils {
	
	private static final Logger log = LoggerFactory.getLogger(MathUtils.class);

	/**
	 * 为Double类型四舍五入保留小数点后precision位
	 * 
	 * @param v
	 * @param precision
	 * @return Double
	 */
	public static Double formatDouble(Double v, int precision) {
		if (v == null) {
			return null;
		}
		if (precision < 0) {
			throw new IllegalArgumentException("precision不能为负数");
		}
		BigDecimal b = new BigDecimal(v);
		BigDecimal one = new BigDecimal("1");
		double value = b.divide(one, precision, BigDecimal.ROUND_HALF_UP).doubleValue();
		StringBuilder format = new StringBuilder("0");
		if (precision > 0) {
			format.append(".");
			for (int i = 0; i < precision; i++) {
				format.append("0");
			}
		}
		DecimalFormat df = new DecimalFormat(format.toString());
		return Double.parseDouble(df.format(value));
	}
}