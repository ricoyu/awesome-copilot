package com.awesomecopilot.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Web 相关工具类 
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:30
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WebUtils {
	private static final Logger log = LoggerFactory.getLogger(WebUtils.class);

	/**
	 * 读取HttpServletRequest Body(逐行读取后直接拼接, 不保留换行符)。
	 * <p>
	 * 注意: 旧实现在返回前还会再执行一次 replaceAll 把回车/换行/制表符删除, 那正是
	 * 评审报告 P1-1 里"缓存的 body 与客户端原始字节不一致"的元凶(readLine 本来就不会
	 * 返回回车和换行, 这个 replaceAll 唯一的实际作用就是破坏制表符), 已删除。
	 * 需要保留换行的场景(缓存/转发/签名)请直接用 request.getInputStream() 读字节,
	 * 例如 RepeatedReadHttpServletRequestWrapper 的做法。
	 */
	public static String bodyString(HttpServletRequest request) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		
		try {
			br = request.getReader();
			String str = null;
			while ((str = br.readLine()) != null) {
				sb.append(str);
			}
			br.close();
		} catch (IOException e) {
			log.error("获取body参数失败", e);
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("获取body参数失败", e);
				}
			}
		}
		
		return sb.toString();
	}
}
