package com.awesomecopilot.codec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 hash 工具类
 * <p/>
 * 标准 MD5 哈希输出是 128 位的十六进制字符串（32 个字符），而一致性哈希算法需要的是整型数值(映射到哈希环的0 ~ 2³²-1区间)
 * <p>
 * <p/>
 * Copyright: Copyright (c) 2026-01-05 18:28
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class MD5Utils {

	private final static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
			'b', 'c', 'd', 'e', 'f' };

	/**
	 * 获取字符串的MD5hash值, 一个32位无符号整数
	 * @param origin 字符串
	 * @return 32位无符号整数, MD5 hash值
	 */
	public static int encode2Int(String origin) {
		if (origin == null) {
			return 0 ;
		}
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] md5Bytes = md.digest(origin.getBytes(StandardCharsets.UTF_8));

			// 4. MD5字节数组（16位）→ 32位int（取前4个字节拼接，行业标准）
			int hashInt = (md5Bytes[0] & 0xFF)
					| ((md5Bytes[1] & 0xFF) << 8)
					| ((md5Bytes[2] & 0xFF) << 16)
					| ((md5Bytes[3] & 0xFF) << 24);
			// 5. 转为无符号int（Java无原生unsigned int，位运算模拟），保证结果≥0
			return hashInt & 0xFFFFFFFF;
		} catch (Exception e) {
			// 6. 异常兜底：日志可替换为项目日志框架（logback/log4j2）
			// logger.error("MD5 encode error, origin:{}", origin, e);
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 将字节数组算出16进制MD5串
	 * @param origin 字节数组
	 * @return 16进制MD5字符串
	 */
	public static String encode2Hex(byte[] origin) {
		String resultString = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			resultString = byteArrayToHexString(md.digest(origin));
		} catch (Exception e) {
			//logger.error(e, e);
		}
		return resultString;
	}

	/**
	 * 转换字节数组为16进制字串
	 * @param b 字节数组
	 * @return 16进制字串
	 */
	public static String byteArrayToHexString(byte[] b) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));//若使用本函数转换则可得到加密结果的16进制表示，即数字字母混合的形式
		}
		return resultSb.toString();
	}


	/**
	 * 转换字节为10进制字符串
	 * @param b 字节
	 * @return 10进制字符串
	 */
	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0) {
			n = 256 + n;
		}
		int d1 = n / 16;
		int d2 = n % 16;
		return "" + hexDigits[d1] + hexDigits[d2];
	}

}
