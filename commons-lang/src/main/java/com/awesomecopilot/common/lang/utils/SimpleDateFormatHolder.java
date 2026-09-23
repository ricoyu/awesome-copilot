package com.awesomecopilot.common.lang.utils;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.awesomecopilot.common.lang.constants.DateConstants.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * A factory for {@link SimpleDateFormat}s. The instances are stored in a threadlocal way
 * because SimpleDateFormat is not threadsafe as noted in {@link SimpleDateFormat its javadoc}.
 */
final class SimpleDateFormatHolder {
	
	private static final ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>> THREADLOCAL_FORMATS =
			new ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>();
	
	/**
	 * 获取SimpleDateFormat对象，timezone默认为Asia/Shanghai，locale为SIMPLIFIED_CHINESE
	 *
	 * @param pattern
	 * @return
	 */
	public static SimpleDateFormat formatFor(final String pattern) {
		Objects.requireNonNull(pattern);
		final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
		Map<String, SimpleDateFormat> formats = ref == null ? null : ref.get();
		if (formats == null) {
			formats = new HashMap<String, SimpleDateFormat>();
			THREADLOCAL_FORMATS.set(new SoftReference<Map<String, SimpleDateFormat>>(formats));
		}
		
		//P1-2: 键带完整维度并用分隔符连接, 与 (pattern,locale) 入口区分
		String key = pattern + "|#|zh-CN-default";
		SimpleDateFormat format = formats.get(key);
		if (format == null) {
			format = new SimpleDateFormat(pattern, Locale.CHINA);
			format.setTimeZone(CHINA);
			formats.put(key, format);
		}
		
		return format;
	}
	
	/**
	 * 根据format和timezone获取SimpleDateFormat对象，根据时区决定locale是什么
	 *
	 * @param pattern
	 * @param timezone
	 * @return
	 */
	public static SimpleDateFormat formatFor(final String pattern, TimeZone timezone) {
		Objects.requireNonNull(pattern);
		final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
		Map<String, SimpleDateFormat> formats = ref == null ? null : ref.get();
		if (formats == null) {
			formats = new HashMap<String, SimpleDateFormat>();
			THREADLOCAL_FORMATS.set(new SoftReference<Map<String, SimpleDateFormat>>(formats));
		}
		
		String key = pattern + "|#|!derived!|#|" + timezone.getID();
		SimpleDateFormat format = formats.get(key);
		if (format == null) {
			Locale locale = localeOf(timezone.getID()); //P2-36: 表降为 private, 经取值方法访问
			if (locale == null) {
				format = new SimpleDateFormat(pattern);
			} else {
				format = new SimpleDateFormat(pattern, locale);
			}
			format.setTimeZone(timezone);
			formats.put(key, format);
		}
		
		return format;
	}
	
	/**
	 * 根据format,locale获取SimpleDateFormat对象，显示指定locale
	 *
	 * @param pattern
	 * @param locale
	 * @return
	 */
	public static SimpleDateFormat formatFor(final String pattern, Locale locale) {
		Objects.requireNonNull(pattern);
		final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
		Map<String, SimpleDateFormat> formats = ref == null ? null : ref.get();
		if (formats == null) {
			formats = new HashMap<String, SimpleDateFormat>();
			THREADLOCAL_FORMATS.set(new SoftReference<Map<String, SimpleDateFormat>>(formats));
		}
		
		//P1-2: getCountry() 对 ENGLISH/FRENCH 都是空串, 键会碰撞; 改用 toLanguageTag + 分隔符
		String key = pattern + "|#|" + locale.toLanguageTag() + "|#|" + TimeZone.getDefault().getID(); //评审修复: 键带默认时区, 运行期改默认时区后不会拿过期实例
		SimpleDateFormat format = formats.get(key);
		if (format == null) {
			format = new SimpleDateFormat(pattern, locale);
			formats.put(key, format);
		}
		
		return format;
	}
	
	/**
	 * 根据format,timezone和locale获取SimpleDateFormat对象，显示指定timezone和locale
	 *
	 * @param pattern
	 * @param timezone
	 * @param locale
	 * @return
	 */
	public static SimpleDateFormat formatFor(final String pattern, TimeZone timezone, Locale locale) {
		Objects.requireNonNull(pattern);
		final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
		Map<String, SimpleDateFormat> formats = ref == null ? null : ref.get();
		if (formats == null) {
			formats = new HashMap<String, SimpleDateFormat>();
			THREADLOCAL_FORMATS.set(new SoftReference<Map<String, SimpleDateFormat>>(formats));
		}
		
		String key = pattern + "|#|" + locale.toLanguageTag() + "|#|" + timezone.getID();
		SimpleDateFormat format = formats.get(key);
		if (format == null) {
			format = new SimpleDateFormat(pattern, locale);
			format.setTimeZone(timezone);
			formats.put(key, format);
		}
		
		return format;
	}
	
	public static SimpleDateFormat getSimpleDateFormat(String source) {
		if (matches(PT_ISO_DATETIME_MILLIS, source)) { // 带毫秒, 放在无毫秒模式前面(全串matches互斥, 顺序只为省扫描)
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_MILLIS);
		}
		if (matches(PT_ISO_DATETIME, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME);
		}
		if (matches(PT_ISO_DATE, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE);
		}
		
		if (matches(PT_ISO_DATETIME_1, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_1);
		}
		
		if (matches(PT_ISO_DATETIME_2, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_2);
		}
		
		if (matches(PT_ISO_DATETIME_3, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_3);
		}
		
		if (matches(PT_ISO_DATETIME_4, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_4);
		}
		
		if (matches(PT_ISO_DATETIME_5, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_5);
		}
		
		if (matches(PT_ISO_DATETIME_SHORT, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT);
		}
		
		if (matches(PT_ISO_DATETIME_SHORT_1, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_1);
		}
		
		if (matches(PT_ISO_DATETIME_SHORT_2, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_2);
		}
		
		if (matches(PT_ISO_DATETIME_SHORT_3, source)) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_3);
		}
		
		if (PT_ISO_DATETIME_SHORT_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_4);
		}
		
		if (PT_ISO_DATETIME_SHORT_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_5);
		}
		
		if (PT_ISO_DATETIME_SHORT_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_6);
		}
		
		if (PT_ISO_DATETIME_SHORT_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_7);
		}
		
		if (PT_DATETIME_FORMAT_EN.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN);
		}
		
		if (PT_DATETIME_FORMAT_EN_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_1);
		}
		
		if (PT_DATETIME_FORMAT_EN_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_2);
		}
		
		if (PT_DATETIME_FORMAT_EN_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_3);
		}
		
		if (PT_DATETIME_FORMAT_EN_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_4);
		}
		
		if (PT_DATETIME_FORMAT_EN_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_5);
		}
		
		if (PT_DATETIME_FORMAT_EN_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_6);
		}
		
		if (PT_DATETIME_FORMAT_EN_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_7);
		}
		
		if (PT_DATETIME_FORMAT_EN_8.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_8);
		}
		
		if (PT_DATETIME_FORMAT_EN_9.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_9);
		}
		
		if (PT_ISO_DATE_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_1);
		}
		
		if (PT_ISO_DATE_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_2);
		}
		
		if (PT_ISO_DATE_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_3);
		}
		
		if (PT_DATE_EN.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN);
		}
		
		if (PT_DATE_EN_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_1);
		}
		
		if (PT_DATE_EN_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_2);
		}
		
		if (PT_DATE_EN_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_3);
		}
		
		if (PT_DATE_EN_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_4);
		}
		
		if (PT_DATE_EN_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_5);
		}
		
		if (PT_DATE_EN_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_6);
		}
		
		if (PT_DATE_EN_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_7);
		}
		
		return finalShot(source);
	}
	
	public static SimpleDateFormat getSimpleDateFormat(String source, TimeZone timeZone) {
		
		if (PT_ISO_DATETIME_MILLIS.matcher(source).matches()) { // yyyy-MM-dd HH:mm:ss.SSS
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_MILLIS, timeZone);
		}
		
		if (PT_ISO_DATETIME.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_1, timeZone);
		}
		
		if (PT_ISO_DATE.matcher(source).matches()) { // yyyy-MM-dd
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE, timeZone);
		}
		
		if (PT_ISO_DATETIME_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_1, timeZone);
		}
		
		if (PT_ISO_DATETIME_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_2, timeZone);
		}
		
		if (PT_ISO_DATETIME_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_3, timeZone);
		}
		
		if (PT_ISO_DATETIME_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_4, timeZone);
		}
		
		if (PT_ISO_DATETIME_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_5, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_1, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_2, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_3, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_4, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_5, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_6, timeZone);
		}
		
		if (PT_ISO_DATETIME_SHORT_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATETIME_SHORT_7, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_1, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_2, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_3, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_4, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_5, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_6, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_7, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_8.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_8, timeZone);
		}
		
		if (PT_DATETIME_FORMAT_EN_9.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATETIME_FORMAT_EN_9, timeZone);
		}
		
		if (PT_ISO_DATE_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_1, timeZone);
		}
		
		if (PT_ISO_DATE_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_2, timeZone);
		}
		
		if (PT_ISO_DATE_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_ISO_DATE_3, timeZone);
		}
		
		if (PT_DATE_EN.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN, timeZone);
		}
		
		if (PT_DATE_EN_1.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_1, timeZone);
		}
		
		if (PT_DATE_EN_2.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_2, timeZone);
		}
		
		if (PT_DATE_EN_3.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_3, timeZone);
		}
		
		if (PT_DATE_EN_4.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_4, timeZone);
		}
		
		if (PT_DATE_EN_5.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_5, timeZone);
		}
		
		if (PT_DATE_EN_6.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_6, timeZone);
		}
		
		if (PT_DATE_EN_7.matcher(source).matches()) {
			return SimpleDateFormatHolder.formatFor(FMT_DATE_FORMAT_EN_7, timeZone);
		}
		
		return finalShot(source, timeZone);
	}
	
	public static void clearThreadLocal() {
		THREADLOCAL_FORMATS.remove();
	}
	
	private static boolean matches(Pattern pattern, String source) {
		if (isBlank(source)) {
			return false;
		}
		Matcher matcher = pattern.matcher(source);
		return matcher.matches();
	}
	
	private static boolean isBlank(String s) {
		return s == null || "".equals(s.trim());
	}
	
	private static SimpleDateFormat finalShot(String source) {
		return finalShot(source, null);
	}
	
	/**
	 * 把时间串里的秒小数部分规整为 3 位（P0-4 的输入侧修复）：
	 * 多于 3 位取前 3 位（Date 只有毫秒精度，截断规则与 java.time 转毫秒值的结果一致），少于 3 位右补 0
	 * （0.45 秒 = 450 毫秒）。最终拼接的解析模式固定 3 个 S，输入必须先把小数位补齐/截断到 3 位，
	 * 否则 SimpleDateFormat 会把连续数字整个当毫秒整数读（"4567"→4567 毫秒并进位）。
	 * 不含小数秒、或本来就 3 位、或不匹配 PT_ALL 的串原样返回。
	 */
	static String normalizeFractionalSeconds(String source) {
		if (source == null) {
			return null;
		}
		Matcher matcher = PT_ALL.matcher(source);
		if (!matcher.matches()) {
			return source;
		}
		String milli = matcher.group(8);
		if (milli == null || milli.length() == 0 || milli.length() == 3) {
			return source;
		}
		String normalized = milli.length() > 3
				? milli.substring(0, 3)
				: milli + "0".repeat(3 - milli.length());
		return source.substring(0, matcher.start(8)) + normalized + source.substring(matcher.end(8));
	}
	
	private static SimpleDateFormat finalShot(String source, TimeZone timeZone) {
		Matcher matcher = PT_ALL.matcher(source);
		if (!matcher.matches()) {
			return null;
		}
		
		String year = matcher.group(1);
		String month = matcher.group(2);
		String day = matcher.group(3);
		String t = matcher.group(4);
		String hour = matcher.group(5);
		String minute = matcher.group(6);
		String second = matcher.group(7);
		String milli = matcher.group(8);
		String zone = matcher.group(9);
		
		StringBuilder format = new StringBuilder();
		//yyyy
		if (isNotBlank(year)) {
			for (int i = 0; i < year.length(); i++) {
				format.append("y");
			}
		}
		format.append("-");
		
		//MM
		if (isNotBlank(month)) {
			for (int i = 0; i < month.length(); i++) {
				format.append("M");
			}
		}
		format.append("-");
		
		//dd
		if (isNotBlank(day)) {
			for (int i = 0; i < day.length(); i++) {
				format.append("d");
			}
		}
		
		//"yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
		if (isNotBlank(t)) {
			format.append("'T'");
		} else {
			format.append(" ");
		}
		
		//HH
		if (isNotBlank(hour)) {
			for (int i = 0; i < hour.length(); i++) {
				format.append("H");
			}
		}
		
		//:mm
		if (isNotBlank(minute)) {
			format.append(":");
			for (int i = 0; i < minute.length(); i++) {
				format.append("m");
			}
		}
		
		//:ss
		if (isNotBlank(second)) {
			format.append(":");
			for (int i = 0; i < second.length(); i++) {
				format.append("s");
			}
		}
		//.SSS
		if (isNotBlank(milli)) {
			//P0-4: SimpleDateFormat 的 S 是"毫秒数"不是"小数位", 按输入位数动态拼 S 必错:
			//"45"被当 45 毫秒(应为 450)、位数>3 时 lenient 进位改秒/分。
			//模式固定 3 位 S, 输入由 normalizeFractionalSeconds 统一补齐/截断到 3 位。
			format.append(".");
			format.append("SSS");
		}
		//Z
		if (isNotBlank(zone)) {
			format.append("Z");
		}
		
		if (timeZone != null) {
			return SimpleDateFormatHolder.formatFor(format.toString(), timeZone);
		}
		
		return SimpleDateFormatHolder.formatFor(format.toString());
	}
	
}
