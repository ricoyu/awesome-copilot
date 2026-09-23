package com.awesomecopilot.common.lang.transformer;

import com.awesomecopilot.common.lang.reflection.GenericTypeInspector;
import com.awesomecopilot.common.lang.utils.DateUtils;
import com.awesomecopilot.common.lang.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * @author Rico
 * @since May 29, 2016
 */
public class ValueHandlerFactory {
	private static Logger log = LoggerFactory.getLogger(ValueHandlerFactory.class);

	private ValueHandlerFactory() {
	}

	public static interface ValueHandler<T> {
		public T convert(Object value);

		public String render(T value);
	}

	public static abstract class BaseValueHandler<T> implements ValueHandler<T>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public String render(T value) {
			return value.toString();
		}
	}

	public static class NoOpValueHandler<T> extends BaseValueHandler<T> {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({"unchecked"})
		public T convert(Object value) {
			return (T) value;
		}
	}

	public static class IntegerValueHandler extends BaseValueHandler<Integer> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final IntegerValueHandler INSTANCE = new IntegerValueHandler();

		@Override
		public Integer convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof Integer) {
				return (Integer) value;
			}

			if (BigDecimal.class.isInstance(value)) {
				return ((BigDecimal) value).intValue();
			}

			if (Long.class.isInstance(value)) {
				return ((Long) value).intValue();
			}

			if (BigInteger.class.isInstance(value)) {
				return ((BigInteger) value).intValue();
			}

			if (String.class.isInstance(value)) {
				try {
					return Integer.parseInt((String) value);
				} catch (NumberFormatException e) {
					log.warn("{} 不是一个数字", value);
				}
			}
			if (Byte.class.isInstance(value)) {
				return ((Byte) value).intValue();
			}
			throw unknownConversion(value, Integer.class);
		}
	}

	public static class LongValueHandler extends BaseValueHandler<Long> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final LongValueHandler INSTANCE = new LongValueHandler();

		@Override
		public Long convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof Long) {
				return (Long) value;
			}

			if (BigDecimal.class.isInstance(value)) {
				return Long.valueOf(((BigDecimal) value).longValue());
			}

			if (BigInteger.class.isInstance(value)) {
				return ((BigInteger) value).longValue();
			}

			if (Integer.class.isInstance(value)) {
				return ((Integer) value).longValue();
			}

			if (String.class.isInstance(value)) {
				try {
					return Long.parseLong((String) value);
				} catch (NumberFormatException e) {
					log.warn("{} 不是一个数字", value);
				}
			}

			if (Short.class.isInstance(value)) {
				return ((Short) value).longValue();
			}
			throw unknownConversion(value, Long.class);
		}

		@Override
		public String render(Long value) {
			return value.toString() + 'L';
		}
	}

	public static class FloatValueHandler extends BaseValueHandler<Float> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final FloatValueHandler INSTANCE = new FloatValueHandler();

		@Override
		public Float convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof Float) {
				return (Float) value;
			}

			if (BigDecimal.class.isInstance(value)) {
				return ((BigDecimal) value).floatValue();
			}
			throw unknownConversion(value, Float.class);
		}

		@Override
		public String render(Float value) {
			return value.toString() + 'F';
		}
	}


	public static class DoubleValueHandler extends BaseValueHandler<Double> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final DoubleValueHandler INSTANCE = new DoubleValueHandler();

		@Override
		public Double convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof Float) {
				//P1-4: 原来直接 (Double) 强转 Float, 必抛 ClassCastException
				return ((Float) value).doubleValue();
			}

			if (BigDecimal.class.isInstance(value)) {
				return ((BigDecimal) value).doubleValue();
			}
			throw unknownConversion(value, Double.class);
		}

		@Override
		public String render(Double value) {
			//P2-26(CODE_REVIEW_REPORT): 修复前后缀是 'F'——那是 Float 的字面量后缀(见
			//FloatValueHandler), java 里 Double 字面量后缀是 D(1.5d)。
			return value.toString() + 'D';
		}
	}

	public static class BigDecimalValueHandler extends BaseValueHandler<BigDecimal> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final BigDecimalValueHandler INSTANCE = new BigDecimalValueHandler();

		@Override
		public BigDecimal convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof BigDecimal) {
				return (BigDecimal) value;
			}

			if (Float.class.isInstance(value)) {
				return new BigDecimal(((Float) value).toString());
			}

			if (Double.class.isInstance(value)) {
				return new BigDecimal(((Double) value).toString());
			}

			if (Integer.class.isInstance(value)) {
				return BigDecimal.valueOf(((Integer) value).doubleValue());
			}

			//P2-26(CODE_REVIEW_REPORT): Long/Short/String 本可无损转 BigDecimal, 修复前
			//实测 convert(100L) 直接抛异常, 且报错消息写的是 "requested type [java.lang.Float]"
			//(从 Float handler 复制粘贴的痕迹)。补分支并修正文案。
			if (Long.class.isInstance(value)) {
				return BigDecimal.valueOf((Long) value);
			}
			if (Short.class.isInstance(value)) {
				return BigDecimal.valueOf((Short) value);
			}
			if (String.class.isInstance(value)) {
				return new BigDecimal((String) value);
			}

			throw unknownConversion(value, BigDecimal.class);
		}

		@Override
		public String render(BigDecimal value) {
			return value.toString();
		}
	}

	public static class DateValueHandler extends BaseValueHandler<Date> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final DateValueHandler INSTANCE = new DateValueHandler();

		@Override
		public Date convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof Date) {
				return (Date) value;
			}

			if (Timestamp.class.isInstance(value)) {
				return new Date(((Timestamp) value).getTime());
			}

			if (java.sql.Date.class.isInstance(value)) {
				return new Date(((java.sql.Date) value).getTime());
			}

			if (value instanceof LocalDateTime) {
				return DateUtils.toDate((LocalDateTime) value);
			}
			throw unknownConversion(value, Date.class);
		}

		@Override
		public String render(Date value) {
			//P2-26(CODE_REVIEW_REPORT): 修复前实测返回 "Thu Jan 01 ... 1970F"——
			//日期没有浮点字面量后缀一说, 'F' 是从数值 handler 复制过来的。
			return value.toString();
		}
	}

	public static class StringValueHandler extends BaseValueHandler<String> implements Serializable {
		private static final long serialVersionUID = -3809324482933886451L;
		public static final StringValueHandler INSTANCE = new StringValueHandler();

		@Override
		public String convert(Object value) {
			if (value == null) {
				return null;
			}

			//本身是字符串类型的, 直接强转
			if (value instanceof String) {
				return (String) value;
			}

			//基本类型的包装器类型, 直接调用其toString方法转成字符串
			if (Character.class.isInstance(value) || Integer.class.isInstance(value)
					|| Long.class.isInstance(value) || Double.class.isInstance(value)
					|| BigDecimal.class.isInstance(value) || BigInteger.class.isInstance(value)
					|| Character.TYPE.isInstance(value) || Integer.TYPE.isInstance(value)
					|| Long.TYPE.isInstance(value) || Double.TYPE.isInstance(value)) {
				return value.toString();
			}

			//Date类型默认按照 yyyy-MM-dd HH:mm:ss 转成字符串
			if (value instanceof Date) {
				return DateUtils.format((Date) value);
			}

			//LocalDateTime类型默认按照 yyyy-MM-dd HH:mm:ss 转成字符串
			if (value instanceof LocalDateTime) {
				return DateUtils.format((LocalDateTime) value);
			}

			//LocalDate默认按照 yyyy-MM-dd 格式转成字符串
			if (value instanceof LocalDate) {
				return DateUtils.format((LocalDate) value);
			}

			//LocalTime默认按照 HH:mm:ss 格式转成字符串
			if (value instanceof LocalTime) {
				return DateUtils.format((LocalTime) value);
			}

			//其他未检测到的类型默认调用其toString方法完成转换
			return value.toString();
		}

		@Override
		public String render(String value) {
			//P2-26(CODE_REVIEW_REPORT): 修复前实测 render("abc") 返回 "abcF"。
			//字符串渲染成 java 字面量应带双引号。
			return "\"" + value + "\"";
		}
	}

	public static class LocalDateTimeValueHandler extends BaseValueHandler<LocalDateTime> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final LocalDateTimeValueHandler INSTANCE = new LocalDateTimeValueHandler();

		@Override
		public LocalDateTime convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof LocalDateTime) {
				return (LocalDateTime) value;
			}

			if (Timestamp.class.isInstance(value)) {
				return ((Timestamp) value).toLocalDateTime();
			}

			if (value instanceof Date) {
				return DateUtils.toLocalDateTime((Date) value);
			}
			return null;
		}

	}

	public static class LocalTimeValueHandler extends BaseValueHandler<LocalTime> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final LocalTimeValueHandler INSTANCE = new LocalTimeValueHandler();

		@Override
		public LocalTime convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof LocalTime) {
				return (LocalTime) value;
			}

			if (Time.class.isInstance(value)) {
				return ((Time) value).toLocalTime();
			}

			if (value instanceof String) {
				String time = (String) value;
				return DateUtils.toLocalTime(time);
			}
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	public static class LocalDateValueHandler extends BaseValueHandler<LocalDate> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final LocalDateValueHandler INSTANCE = new LocalDateValueHandler();
		private static final Class SQL_DATE_TYPE = java.sql.Date.class;
		private static final Class DATE_TYPE = Date.class;
		private static final Class TIMESTAMP_TYPE = Timestamp.class;

		@Override
		public LocalDate convert(Object value) {
			if (value == null) {
				return null;
			}

			if (value instanceof LocalDate) {
				return (LocalDate) value;
			}

			if (SQL_DATE_TYPE.isInstance(value)) {
				return DateUtils.toLocalDate((java.sql.Date) value);
			}

			if (DATE_TYPE.isInstance(value)) {
				return DateUtils.toLocalDate((Date) value);
			}

			if (TIMESTAMP_TYPE.isInstance(value)) {
				return ((Timestamp) value).toLocalDateTime().toLocalDate();
			}

			if (value instanceof String) {
				return DateUtils.toLocalDate((String) value);
			}
			return null;
		}

	}

	public static class BooleanValueHandler extends BaseValueHandler<Boolean> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final BooleanValueHandler INSTANCE = new BooleanValueHandler();

		@Override
		public Boolean convert(Object value) {
			if (value == null) {
				return false;
			}
			if (Boolean.class.isInstance(value)) {
				return (Boolean) value;
			}
			if (String.class.isInstance(value)) {
				//P1-17: 原来用 Boolean.getBoolean(字符串) —— 那是"读名叫这个字符串的系统属性",
				//不是解析布尔文本, 结果恒 false 且不报错。改用 parseBoolean。
				return Boolean.parseBoolean(((String) value).trim());
			}
			if (Integer.class.isInstance(value)) {
				Integer integerValue = (Integer) value;
				if (integerValue.intValue() == 0) {
					return false;
				}
				return true;
			}
			if (Long.class.isInstance(value)) {
				Long longValue = (Long) value;
				if (longValue.intValue() == 0) {
					return false;
				}
				return true;
			}
			if (BigInteger.class.isInstance(value)) {
				BigInteger bigInteger = (BigInteger) value;
				if (bigInteger.intValue() == 0) {
					return false;
				}
				return true;
			}
			if (Byte.class.isInstance(value)) {
				Byte byteValue = (Byte) value;
				if (byteValue.byteValue() == (byte) 1) {
					return true;
				} else {
					return false;
				}
			}
			return (Boolean) value;
		}

	}

	public static class ShortValueHandler extends BaseValueHandler<Short> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final ShortValueHandler INSTANCE = new ShortValueHandler();

		@Override
		public Short convert(Object value) {
			//P2-26(CODE_REVIEW_REPORT): 修复前实测 convert(null) 返回 0——同文件其余
			//handler 对 null 一律返回 null, DB 的 NULL smallint 列经这里会变成 0。统一返回 null。
			if (value == null) {
				return null;
			}
			if (Short.class.isInstance(value)) {
				return (Short) value;
			}
			if (String.class.isInstance(value)) {
				return Short.valueOf((String) value);
			}
			return (Short) value;
		}

	}

	public static class StringListValueHandler extends BaseValueHandler<List<String>> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final StringListValueHandler INSTANCE = new StringListValueHandler();

		@Override
		public List<String> convert(Object value) {
			if (value == null) {
				return null;
			}
			if (String.class.isInstance(value)) {
				String result = (String) value;
				result = result.trim();
				if (result.startsWith("[") && result.endsWith("]")) {
					result = result.substring(1, result.length() - 1);
				}
				String[] split = StringUtils.split(result);
				return Arrays.asList(split);

			}

			return null;
		}
	}

	public static class IntegerListValueHandler extends BaseValueHandler<List<Integer>> implements Serializable {
		private static final long serialVersionUID = 1L;
		public static final IntegerListValueHandler INSTANCE = new IntegerListValueHandler();

		@Override
		public List<Integer> convert(Object value) {
			if (value == null) {
				return null;
			}
			if (String.class.isInstance(value)) {
				String result = (String) value;
				result = result.trim();
				if (result.startsWith("[") && result.endsWith("]")) {
					result = result.substring(1, result.length() - 1);
				}
				String[] split = StringUtils.split(result);
				return Arrays.asList(split).stream().map((val) -> {
					return Integer.parseInt(val);
				}).collect(toList());
			}

			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private static IllegalArgumentException unknownConversion(Object value, Class type) {
		return new IllegalArgumentException("Unaware how to convert value [" + value + "] : of type [" + typeName(value)
				+ "] to requested type [" + type.getName() + "]");
	}

	private static String typeName(Object value) {
		return value == null ? "???" : value.getClass().getName();
	}

	/**
	 * Convert the given value into the specified target type.
	 *
	 * @param value      The value to convert
	 * @param targetType The type to which it should be converted
	 * @return The converted value.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> T convert(Object value, Class<T> targetType) {
		if (value == null) {
			return null;
		}
		//		if (targetType.equals(value.getClass())) {
		//			return (T) value;
		//		}
		if (targetType.isAssignableFrom(value.getClass())) {
			return (T) value;
		}

		ValueHandler<T> valueHandler = determineAppropriateHandler(targetType);
		if (valueHandler == null) {
			throw unknownConversion(value, targetType);
		}
		return valueHandler.convert(value);
	}

	/**
	 * 这个不支持集合类型
	 * Determine the appropriate {@link ValueHandler} strategy for
	 * converting a value to the given target type
	 *
	 * @param targetType The target type (to which we want to convert values).
	 * @param <T>        parameterized type for the target type.
	 * @return The conversion
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> ValueHandler<T> determineAppropriateHandler(Class<T> targetType) {
		if (Integer.class.equals(targetType) || Integer.TYPE.equals(targetType)) {
			return (ValueHandler<T>) IntegerValueHandler.INSTANCE;
		}
		if (Long.class.equals(targetType) || Long.TYPE.equals(targetType)) {
			return (ValueHandler<T>) LongValueHandler.INSTANCE;
		}
		if (Float.class.equals(targetType) || Float.TYPE.equals(targetType)) {
			return (ValueHandler<T>) FloatValueHandler.INSTANCE;
		}
		if (Double.class.equals(targetType) || Double.TYPE.equals(targetType)) {
			return (ValueHandler<T>) DoubleValueHandler.INSTANCE;
		}
		if (BigDecimal.class.equals(targetType)) {
			return (ValueHandler<T>) BigDecimalValueHandler.INSTANCE;
		}
		if (Date.class.equals(targetType)) {
			return (ValueHandler<T>) DateValueHandler.INSTANCE;
		}
		if (String.class.equals(targetType)) {
			return (ValueHandler<T>) StringValueHandler.INSTANCE;
		}
		if (LocalDateTime.class.equals(targetType)) {
			return (ValueHandler<T>) LocalDateTimeValueHandler.INSTANCE;
		}
		if (LocalDate.class.equals(targetType)) {
			return (ValueHandler<T>) LocalDateValueHandler.INSTANCE;
		}
		if (Boolean.class.equals(targetType) || Boolean.TYPE.equals(targetType)) {
			return (ValueHandler<T>) BooleanValueHandler.INSTANCE;
		}
		if (Short.class.equals(targetType) || Short.TYPE.equals(targetType)) {
			return (ValueHandler<T>) ShortValueHandler.INSTANCE;
		}
		if (LocalTime.class.equals(targetType)) {
			return (ValueHandler<T>) LocalTimeValueHandler.INSTANCE;
		}
		//P2-27(CODE_REVIEW_REPORT): 修复前实测 Transformers.convert("[a, b, c]", List.class)
		//永远抛 NoSuitableValueHandlerException——单参入口完全不认 List, 集合转换能力
		//(StringListValueHandler)只有带 Field 的两参入口能走到。无 field 可查元素类型,
		//给默认的 String 列表 handler(元素本来就是任意值的字符串形式)。
		if (List.class.equals(targetType)) {
			return (ValueHandler<T>) StringListValueHandler.INSTANCE;
		}

		return null;
	}

	/**
	 * Determine the appropriate {@link ValueHandler} strategy for
	 * converting a value to the given target type
	 *
	 * @param targetType The target type (to which we want to convert values).
	 * @param <T>        parameterized type for the target type.
	 * @return The conversion
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> ValueHandler<T> determineAppropriateHandler(Class<T> targetType, Field field) {
		if (Integer.class.equals(targetType) || Integer.TYPE.equals(targetType)) {
			return (ValueHandler<T>) IntegerValueHandler.INSTANCE;
		}
		if (Long.class.equals(targetType) || Long.TYPE.equals(targetType)) {
			return (ValueHandler<T>) LongValueHandler.INSTANCE;
		}
		if (Float.class.equals(targetType) || Float.TYPE.equals(targetType)) {
			return (ValueHandler<T>) FloatValueHandler.INSTANCE;
		}
		if (Double.class.equals(targetType) || Double.TYPE.equals(targetType)) {
			return (ValueHandler<T>) DoubleValueHandler.INSTANCE;
		}
		if (BigDecimal.class.equals(targetType)) {
			return (ValueHandler<T>) BigDecimalValueHandler.INSTANCE;
		}
		if (Date.class.equals(targetType)) {
			return (ValueHandler<T>) DateValueHandler.INSTANCE;
		}
		if (String.class.equals(targetType)) {
			return (ValueHandler<T>) StringValueHandler.INSTANCE;
		}
		if (LocalDateTime.class.equals(targetType)) {
			return (ValueHandler<T>) LocalDateTimeValueHandler.INSTANCE;
		}
		if (LocalDate.class.equals(targetType)) {
			return (ValueHandler<T>) LocalDateValueHandler.INSTANCE;
		}
		if (Boolean.class.equals(targetType) || Boolean.TYPE.equals(targetType)) {
			return (ValueHandler<T>) BooleanValueHandler.INSTANCE;
		}
		if (Short.class.equals(targetType) || Short.TYPE.equals(targetType)) {
			return (ValueHandler<T>) ShortValueHandler.INSTANCE;
		}
		if (LocalTime.class.equals(targetType)) {
			return (ValueHandler<T>) LocalTimeValueHandler.INSTANCE;
		}
		if (List.class.equals(targetType)) {
			//P2-27(CODE_REVIEW_REPORT): 修复前 field==null 时实测 NPE——
			//GenericTypeInspector.inspectGenericTypes 直接调 field.getType()。无 field 可查
			//元素类型时退化为默认的 String 列表 handler(列表元素转 String 是最通用需求)。
			if (field == null) {
				return (ValueHandler<T>) StringListValueHandler.INSTANCE;
			}
			String type = GenericTypeInspector.inspectGenericTypes(field);
			if ("java.lang.String".equalsIgnoreCase(type)) {
				return (ValueHandler<T>) StringListValueHandler.INSTANCE;
			}
			if ("java.lang.Integer".equalsIgnoreCase(type)) {
				return (ValueHandler<T>) IntegerListValueHandler.INSTANCE;
			}
			//元素类型不受支持: 与上面两支一样显式返回 null, 语义不变但意图写明
			return null;
		}

		return null;
	}

}
