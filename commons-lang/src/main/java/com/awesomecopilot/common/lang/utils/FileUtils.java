package com.awesomecopilot.common.lang.utils;

import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileUtils {

	// 替换所有不安全或不推荐用于URL/文件名的字符
	private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile(
			"[\\\\/:*?\"<>|\\x00-\\x1F#()@!&%^$+={}\\[\\],; '~`]");

	// 连续的下划线或中划线（2个及以上）规整为单个下划线
	private static final Pattern CONTINUOUS_UNDERSCORE_PATTERN = Pattern.compile("[_\\-]{2,}");

	// 检测是否已经是 clean 过的文件名：以 12位小写十六进制 + 下划线 开头
	private static final Pattern CLEANED_FILENAME_PATTERN = Pattern.compile("^[a-f0-9]{12}_.*");

	// Java ImageIO 标准支持的图片格式（读者名称）
	private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
			"jpg", "jpeg", "png", "gif", "bmp", "wbmp", "tif", "tiff"
			// 如果后续需要支持 webp、svg 等，需要额外库，这里暂不包含
	);

	/**
	 * 这个方法会去掉文件名中的一些特殊字符，并生成一个更适合的文件名
	 * <p>
	 * 同一个文件名 cleanFilename后得到的总是相同的最终文件名
	 * <ul>比如:
	 *     <li/>"这是一个测试文件.docx" -> "d9b1c8a6ec35_这是一个测试文件.docx"
	 *     <li/>" 报告 #2025 (最终版).xlsx " -> "37545fd3a5f0_报告_2025_最终版.xlsx"
	 *     <li/>"DSC00064.JPG" -> "b99e560c5746_dsc00064.jpg"
	 *     <li/>"!@#$%^&*().zip" -> "837ddf55c8af_unknown.zip"
	 * </ul>
	 * @param originalFilename
	 * @return
	 */
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
		// P2-29(CODE_REVIEW_REPORT): 修复前这里无条件短路——实测 "abcdef123456_../../evil.txt"
		// 清洗后 "../" 原样保留, 返回值若参与拼接存储路径即为路径穿越。
		// 现短路前先校验不含 ".." "/" "\" , 含则按普通名字走完整清洗逻辑。
		if (CLEANED_FILENAME_PATTERN.matcher(trimmed).matches()
				&& !trimmed.contains("..") && !trimmed.contains("/") && !trimmed.contains("\\")) {
			return trimmed;  // 幂等：不再处理，直接返回
		}

		// 下面是原有清洗逻辑（只对未 clean 过的文件名执行）

		// P2-29: 修复前 toLowerCase() 无 Locale——土耳其语默认 Locale 下 'I'→'ı'(无点小写i),
		// 同一文件名在不同部署机器上哈希前缀不同, "同一个文件名总是得到相同结果"契约被破坏
		String lower = trimmed.toLowerCase(Locale.ROOT);

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

		// P2-29: 连续点折叠为单个下划线——".." 作为文件名成分在 File(dir,"..") 这类
		// 拼接下会解析到父目录, 清洗函数不应输出含 ".." 的名字
		cleanedName = cleanedName.replaceAll("\\.{2,}", "_");

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

	/**
	 * 验证上传的文件是否为真实图片
	 * P2-14(CODE_REVIEW_REPORT): 修复前 ImageIO.read(inputStream) 消耗调用方的流且不重置
	 * (探针实测 68 字节 PNG 调用后只剩 16 字节可读)——"先判断是不是图片、再保存同一个流"
	 * 的上传流程会保存出残缺文件。现: 入参流支持 mark/reset 时(ByteArrayInputStream、
	 * BufferedInputStream 等)判断结束后位置复原, 调用方可继续使用;
	 * 不支持 mark 的流(部分 ServletInputStream)无法回退(数据已被读进 JVM, 物理上收不回来),
	 * 此时本方法会消费流。
	 *
	 * @param size    文件大小
	 * @param inputStream 输入流
	 * @return boolean
	 * @throws IOException
	 */
	public static boolean isImage(long size, String filename, InputStream inputStream) {
		if (size == 0 || inputStream == null) {
			return false;
		}

		// 1. 检查文件名后缀
		if (filename == null || filename.trim().isEmpty()) {
			return false;
		}
		String extension = getFileExtension(filename);
		if (extension.isEmpty() || !SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ENGLISH))) {
			return false;
		}

		// 2. 可回退则 mark, 读完 reset, 把流位置原样交还调用方(P2-14)
		boolean markable = inputStream.markSupported();
		try {
			if (markable) {
				inputStream.mark(Math.max((int) Math.min(size, Integer.MAX_VALUE), 1));
			}
			BufferedImage image = ImageIO.read(inputStream);
			return image != null;
		} catch (IOException e) {
			// 如果读取失败或IO异常，则不是有效图片
			return false;
		} finally {
			if (markable) {
				try {
					inputStream.reset();
				} catch (IOException ignored) { //mark 预算内必然可 reset, 到这里说明容器异常, 不再扩大失败面
				}
			}
		}
	}

	/**
	 * 字节数组转十六进制字符串（工具方法）
	 */
	private static String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xFF & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	/**
	 * 从文件名中提取后缀（不包含点）
	 * 示例： "photo.JPG" -> "jpg"
	 *       "image.png"   -> "png"
	 *       "noext"       -> ""
	 *       ".hidden"     -> ""
	 */
	private static String getFileExtension(String filename) {
		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
			return "";
		}
		return filename.substring(lastDotIndex + 1);
	}

}