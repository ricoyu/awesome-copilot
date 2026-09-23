package com.awesomecopilot.common.lang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class MathUtils {
	private static final Logger logger = LoggerFactory.getLogger(MathUtils.class);
	
	/**
	 * 提供精确的加法运算。
	 *
	 * @param v1 被加数
	 * @param v2 加数
	 * @return 两个参数的和
	 */
	public static double add(double v1, double v2, int precision) {
		BigDecimal b1 = BigDecimal.valueOf(v1); //P1-18: 原 new BigDecimal(double) 取二进制近似值, 金额恒少一分
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.add(b2).setScale(precision, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 提供精确的加法运算。
	 *
	 * @param v1 被加数
	 * @param v2 加数
	 * @return 两个参数的和
	 */
	public static BigDecimal add(BigDecimal v1, BigDecimal v2, int precision) {
		if (v1 == null) {
			v1 = BigDecimal.ZERO;
		}
		if (v2 == null) {
			v2 = BigDecimal.ZERO;
		}
		return v1.add(v2).setScale(precision, BigDecimal.ROUND_HALF_UP);
	}
	
	public static BigDecimal add(BigDecimal v1, BigDecimal v2) {
		if (v1 == null) {
			v1 = BigDecimal.ZERO;
		}
		if (v2 == null) {
			v2 = BigDecimal.ZERO;
		}
		return v1.add(v2);
	}
	
	/**
	 * 对items中每个元素使用mapper进行转换并累加其值, 如果items == null 或者 items.size() == 0, 返回0
	 *
	 * @param items
	 * @param mapper
	 * @return BigDecimal
	 */
	public static <T> BigDecimal add(List<T> items, Function<T, BigDecimal> mapper) {
		if (items == null || items.size() == 0) {
			return BigDecimal.ZERO;
		}
		return items.stream()
				.map(mapper)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	/**
	 * 如果v为null，返回0，否则返回v的绝对值
	 *
	 * @param v
	 * @return
	 */
	public static BigDecimal abs(BigDecimal v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}
		return v.abs();
	}
	
	/**
	 * 提供精确的减法运算。
	 *
	 * @param v1 被减数
	 * @param v2 减数
	 * @return 两个参数的差
	 */
	public static double sub(double v1, double v2, int precision) {
		BigDecimal b1 = BigDecimal.valueOf(v1); //P1-18: 原 new BigDecimal(double) 取二进制近似值, 金额恒少一分
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.subtract(b2).setScale(precision, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 提供精确的减法运算 如果v1、v2为null，认为其值为0
	 *
	 * @param v1
	 * @param v2
	 * @param precision
	 * @return
	 */
	public static BigDecimal sub(BigDecimal v1, BigDecimal v2, int precision) {
		if (v1 == null) {
			v1 = BigDecimal.ZERO;
		}
		if (v2 == null) {
			v2 = BigDecimal.ZERO;
		}
		return v1.subtract(v2).setScale(precision, BigDecimal.ROUND_HALF_UP);
	}
	
	public static BigDecimal sub(BigDecimal v1, BigDecimal v2) {
		if (v1 == null) {
			v1 = BigDecimal.ZERO;
		}
		if (v2 == null) {
			v2 = BigDecimal.ZERO;
		}
		return v1.subtract(v2);
	}
	
	/**
	 * 提供精确的乘法运算。
	 *
	 * @param v1 被乘数
	 * @param v2 乘数
	 * @return 两个参数的积
	 */
	public static double mul(double v1, double v2, int precision) {
		BigDecimal b1 = BigDecimal.valueOf(v1); //P1-18: 原 new BigDecimal(double) 取二进制近似值, 金额恒少一分
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.multiply(b2).setScale(precision, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 两数相乘，按四舍五入保留指定位数的小数
	 *
	 * @param v1
	 * @param v2
	 * @param precision
	 * @return
	 */
	public static BigDecimal mul(BigDecimal v1, BigDecimal v2, int precision) {
		if (v1 == null || v2 == null) {
			return BigDecimal.ZERO;
		}
		return v1.multiply(v2).setScale(precision, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * 两数相乘，按四舍五入保留指定位数的小数
	 *
	 * @param v1
	 * @param v2
	 * @param precision
	 * @return
	 */
	public static BigDecimal mul(BigDecimal v1, int v2, int precision) {
		if (v1 == null || v2 == 0) {
			return BigDecimal.ZERO;
		}
		return v1.multiply(BigDecimal.valueOf(v2)).setScale(precision, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * 测试v1是否小于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lessThan(BigDecimal v1, int v2) {
		return lessThan(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否小于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lt(BigDecimal v1, int v2) {
		return lessThan(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否小于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lessThanOrEqual(BigDecimal v1, BigDecimal v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) <= 0;
	}
	
	/**
	 * 测试v1是否小于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lte(BigDecimal v1, BigDecimal v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) <= 0;
	}
	
	/**
	 * 测试v1是否小于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lessThan(BigDecimal v1, BigDecimal v2) {
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) < 0;
	}
	
	/**
	 * 测试v1是否小于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean lt(BigDecimal v1, BigDecimal v2) {
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) < 0;
	}
	
	/**
	 * 测试v1是否等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	/*	public static boolean equals(BigDecimal v1, int v2) {
			return equals(v1, BigDecimal.valueOf(v2));
		}*/
	
	/**
	 * 测试v1是否等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	/*	public static boolean equals(BigDecimal v1, BigDecimal v2) {
			if (v1 == null && v2 == null) {
				return true;
			}
	
			if (v1 == null || v2 == null) {
				return false;
			}
			return v1.compareTo(v2) == 0;
		}*/
	
	/**
	 * 测试v1是否大于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean greatThan(BigDecimal v1, int v2) {
		return greatThan(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否大于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean gt(BigDecimal v1, int v2) {
		return greatThan(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否大于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean greatThan(BigDecimal v1, BigDecimal v2) {
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) > 0;
	}
	
	/**
	 * 测试v1是否大于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean gt(BigDecimal v1, BigDecimal v2) {
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) > 0;
	}
	
	/**
	 * 测试v1是否大于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean greatThanOrEqual(BigDecimal v1, int v2) {
		return greatThanOrEqual(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否大于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean gte(BigDecimal v1, int v2) {
		return greatThanOrEqual(v1, BigDecimal.valueOf(v2));
	}
	
	/**
	 * 测试v1是否大于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean greatThanOrEqual(BigDecimal v1, BigDecimal v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) >= 0;
	}
	
	/**
	 * 测试v1是否大于等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean gte(BigDecimal v1, BigDecimal v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) >= 0;
	}
	
	/**
	 * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指 定精度，以后的数字四舍五入。
	 * <p>
	 * P2-23(CODE_REVIEW_REPORT): 补契约说明——v2 为 0 时抛 ArithmeticException
	 * (BigDecimal.divide 的行为), 与 add/sub/mul 的"null 归零"不同, 除零是调用错误,
	 * 不返回 null——返回 null 会让调用方把脏数据继续传下去)。
	 *
	 * @param v1        被除数
	 * @param v2        除数, 不可为 0
	 * @param precision 表示表示需要精确到小数点以后几位。
	 * @return 两个参数的商
	 */
	public static double div(double v1, double v2, int precision) {
		BigDecimal b1 = BigDecimal.valueOf(v1); //P1-18: 原 new BigDecimal(double) 取二进制近似值, 金额恒少一分
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.divide(b2, precision, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 对v进行四舍五入保留两位小数
	 *
	 * @param v
	 * @return BigDecimal
	 */
	public static BigDecimal roundUpTwo(BigDecimal v) {
		if (v == null) {
			v = BigDecimal.ZERO;
		}
		return v.setScale(2, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * 提供精确的小数位四舍五入处理。
	 *
	 * @param v         需要四舍五入的数字
	 * @param precision 小数点后保留几位
	 * @return 四舍五入后的结果
	 */
	public static double round(double v, int precision) {
		// P2-23(CODE_REVIEW_REPORT): 修复前缺 precision<0 校验——round(1.55,-1) 按
		//"十位取整"返回 0.0 且不报错(同文件 formatDouble 却有校验)。现补齐。
		if (precision < 0) {
			throw new IllegalArgumentException("precision不能为负数");
		}
		BigDecimal b = BigDecimal.valueOf(v); //P1-18
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
		BigDecimal b = BigDecimal.valueOf(v); //P1-18
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
	
	public static String format(Double v, int precision) {
		if (v == null) {
			return null;
		}
		if (precision < 0) {
			throw new IllegalArgumentException("precision不能为负数");
		}
		BigDecimal b = BigDecimal.valueOf(v); //P1-18
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
		return df.format(value);
	}
	
	/**
	 * 四舍五入保留小数点后precision位
	 *
	 * @param v
	 * @param precision
	 * @return
	 */
	public static String format(BigDecimal v, int precision) {
		if (v == null) {
			return null;
		}
		if (precision < 0) {
			throw new IllegalArgumentException("precision不能为负数");
		}
		StringBuilder format = new StringBuilder("0");
		if (precision > 0) {
			format.append(".");
			for (int i = 0; i < precision; i++) {
				format.append("0");
			}
		}
		DecimalFormat df = new DecimalFormat(format.toString());
		return df.format(v);
	}
	
	/**
	 * 四舍五入保留小数点后precision位
	 *
	 * @param v
	 * @param precision
	 * @return
	 */
	public static String format2Currency(BigDecimal v, int precision) {
		if (v == null) {
			v = BigDecimal.ZERO;
		}
		if (precision < 0) {
			throw new IllegalArgumentException("precision不能为负数");
		}
		// P2-23(CODE_REVIEW_REPORT): 修复前整数部分模板是 ",000"——DecimalFormat 会把整数
		// 部分补足 3 位, 实测 format2Currency(5,2)="005.00"、(99,2)="099.00"。现 ",##0":
		// 千分位分组保留(12345.5→12,345.50), 不再补前导零(5→5.00)。
		StringBuilder format = new StringBuilder(v.compareTo(BigDecimal.ZERO) == 0 ? "0" : ",##0");
		if (precision > 0) {
			format.append(".");
			for (int i = 0; i < precision; i++) {
				format.append("0");
			}
		}
		DecimalFormat df = new DecimalFormat(format.toString());
		return df.format(v);
	}
	
	/**
	 * <blockquote><pre>
	 * value1为null, value2不为null	false
	 * value1不为null, value2为null	false
	 * value1, value2都为null		true
	 * 否则比较其long值
	 * </pre></blockquote>
	 *
	 * @param value1
	 * @param value2
	 * @return boolean
	 * @on
	 */
	public static boolean longEqual(Long value1, Long value2) {
		if (value1 == null && value2 == null) {
			return true;
		}
		if (value1 == null) {
			return false;
		}
		
		if (value2 == null) {
			return false;
		}
		
		return value1.longValue() == value2.longValue();
	}
	
	/**
	 * <blockquote><pre>
	 * value1为null, value2不为null	false
	 * value1不为null, value2为null	false
	 * value1, value2都为null		true
	 * 否则比较其long值
	 * </pre></blockquote>
	 * P2-23(CODE_REVIEW_REPORT): 修复前这张表写"都为 null 返回 false", 实现返回 true——
	 * 注释抄错了(longEqual 的注释与实现一致, 可对照)。现改注释, 行为保持实现。
	 *
	 * @param v1
	 * @param v2
	 * @return boolean
	 * @on
	 */
	public static boolean equals(Long v1, Long v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		
		if (v1 == null || v2 == null) {
			return false;
		}
		
		return v1.longValue() == v2.longValue();
	}
	
	/**
	 * 测试v1是否等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean equals(BigDecimal v1, BigDecimal v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.compareTo(v2) == 0;
	}
	
	/**
	 * 测试在保留指定小数位后, v1是否等于v2，小数点末尾的0不计算在内
	 *
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean equals(BigDecimal v1, BigDecimal v2, int precision) {
		if (v1 == null && v2 == null) {
			return true;
		}
		
		if (v1 == null || v2 == null) {
			return false;
		}
		BigDecimal scaled1 = v1.setScale(precision, BigDecimal.ROUND_HALF_UP);
		BigDecimal svaled2 = v2.setScale(precision, BigDecimal.ROUND_HALF_UP);
		return scaled1.compareTo(svaled2) == 0;
	}
	
	/**
	 * <blockquote><pre>
	 * value1为null, value2不为null	false
	 * value1不为null, value2为null	false
	 * value1, value2都为null		true
	 * 否则比较其int值
	 * </pre></blockquote>
	 *
	 * @param v1
	 * @param v2
	 * @return boolean
	 * @on
	 */
	public static boolean equals(Integer v1, Integer v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		
		return v1.intValue() == v2.intValue();
	}
	
	/**
	 * <blockquote><pre>
	 * value1为null, value2不为null	false
	 * value1不为null, value2为null	false
	 * value1, value2都为null		true
	 * 否则比较其int值
	 * </pre></blockquote>
	 *
	 * @param v1
	 * @param v2
	 * @return boolean
	 * @on
	 */
	public static boolean integerEqual(Integer v1, Integer v2) {
		if (v1 == null && v2 == null) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		
		return v1.intValue() == v2.intValue();
	}
	
	public static Long toLong(String value, Long... defaults) {
		if (isBlank(value)) {
			if (defaults == null || defaults.length == 0) {
				return null;
			} else {
				return defaults[0];
			}
		} else {
			try {
				return Long.parseLong(value);
			} catch (Exception e) {
				logger.error("Convert from {0} to Long failed", value, e);
			}
			return null;
		}
	}
	
	public static Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		
		if (value instanceof Integer) {
			return (Integer) value;
		}
		
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				logger.error("Convert String to Integer failed", e);
			}
		}
		
		if (value instanceof Long) {
			// P2-23(CODE_REVIEW_REPORT): 修复前 ((Long) value).intValue() 直接截断——
			// 实测 toInteger(3000000000L) 返回 -1294967296 不报错。现溢出抛 ArithmeticException。
			return Math.toIntExact((Long) value);
		}
		
		if (value instanceof Double) {
			return ((Double) value).intValue(); //保留原行为: 截断(报告仅要求修 Long 溢出不报错的问题)
		}
		
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).intValue();
		}
		
		return null;
	}
	
	public static Double toDouble(Object value) {
		if (value == null) {
			return null;
		}
		
		if (value instanceof Double) {
			return (Double) value;
		}
		
		if (value instanceof Long) {
			return ((Long) value).doubleValue();
		}
		
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).doubleValue();
		}
		
		if (value instanceof Integer) {
			return ((Integer) value).doubleValue();
		}
		
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (NumberFormatException e) {
				logger.error("msg", e);
			}
		}
		
		return null;
	}
	
	public static Double toDouble(Object value, boolean toNegative) {
		Double v = toDouble(value);
		// P2-23(CODE_REVIEW_REPORT): 修复前 toDouble("abc", true) 实测抛 NullPointerException
		// ——内层 toDouble 对不可转换值返回 null, 下面 0 - v 拆箱直接 NPE。现先判 null。
		if (toNegative && v != null) {
			return 0 - v;
		}
		return v;
	}
	
	public static String toString(Long value) {
		if (value == null) {
			return null;
		}
		
		return value.toString();
	}
}