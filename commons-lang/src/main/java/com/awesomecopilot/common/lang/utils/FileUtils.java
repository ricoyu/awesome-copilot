package com.awesomecopilot.common.lang.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class FileUtils {

	// 替换所有不安全或不推荐用于URL/文件名的字符
	private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile(
			"[\\\\/:*?\"<>|\\x00-\\x1F#()@!&%^$+={}\\[\\],; '~`]");

	// 连续的下划线或中划线（2个及以上）规整为单个下划线
	private static final Pattern CONTINUOUS_UNDERSCORE_PATTERN = Pattern.compile("[_\\-]{2,}");

	// 检测是否已经是 clean 过的文件名：以 12位小写十六进制 + 下划线 开头
	private static final Pattern CLEANED_FILENAME_PATTERN = Pattern.compile("^[a-f0-9]{12}_.*");

	public static String cleanFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "unknown_file";
		}

		String trimmed = originalFilename.trim();

		// 【关键修复1】：特殊值 "unknown_file" 直接返回，不处理
		if ("unknown_file".equals(trimmed)) {
			return "unknown_file";
		}

		if (trimmed.isEmpty()) {
			return "unknown_file";
		}

		// 【关键新增逻辑】：如果已经是 clean 过的文件名，直接返回原样
		if (CLEANED_FILENAME_PATTERN.matcher(trimmed).matches()) {
			return trimmed;  // 幂等：不再处理，直接返回
		}

		// 下面是原有清洗逻辑（只对未 clean 过的文件名执行）

		String lower = trimmed.toLowerCase();

		String namePart;
		String suffixPart = "";
		int lastDotIndex = lower.lastIndexOf(".");
		if (lastDotIndex > 0 && lastDotIndex < lower.length() - 1) {
			namePart = lower.substring(0, lastDotIndex);
			suffixPart = lower.substring(lastDotIndex + 1);
		} else {
			namePart = lower;
		}

		String spaced = namePart.replaceAll("\\s+", "_");

		String cleanedName = INVALID_CHAR_PATTERN.matcher(spaced).replaceAll("_");

		cleanedName = CONTINUOUS_UNDERSCORE_PATTERN.matcher(cleanedName).replaceAll("_");

		cleanedName = cleanedName.replaceAll("^_+", "").replaceAll("_+$", "");

		cleanedName = cleanedName.isBlank() ? "unknown" : cleanedName;

		String hashSource = cleanedName + "_" + suffixPart;
		String hash = DigestUtils.md5Hex(hashSource.getBytes(StandardCharsets.UTF_8)).substring(0, 12);

		String shortName = cleanedName.length() > 40 ? cleanedName.substring(0, 40) : cleanedName;

		return suffixPart.isBlank()
				? String.format("%s_%s", hash, shortName)
				: String.format("%s_%s.%s", hash, shortName, suffixPart);
	}
}