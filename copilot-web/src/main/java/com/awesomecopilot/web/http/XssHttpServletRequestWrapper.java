package com.awesomecopilot.web.http;

import com.awesomecopilot.web.utils.XssCleanUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 获取到的请求参数值, 进行XSS过滤, 把一些危险的标签比如<script></script>都给替换掉, 但是这里还不会对HTML标签做转义
 *
 * <p>
 * Copyright: (C), 2020-7-22 0022 9:57
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

	private final HttpServletRequest request;
	// 缓存处理后的请求体, 避免重复读取
	private byte[] body;

	public XssHttpServletRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
	}

	/**
	 * 处理单个参数
	 */
	@Override
	public String getParameter(String name) {
		String value = super.getParameter(name);
		if (StringUtils.isNotEmpty(value)) {
			return XssCleanUtils.clean(value);
		}
		return value;
	}

	/**
	 * 处理数组参数
	 */
	@Override
	public String[] getParameterValues(String name) {
		String[] values = super.getParameterValues(name);
		if (values == null || values.length == 0) {
			return values;
		}

		String[] cleanValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			if (StringUtils.isNotEmpty(value)) {
				cleanValues[i] = XssCleanUtils.clean(value);
			} else {
				cleanValues[i] = value;
			}
		}
		return cleanValues;
	}

	/**
	 * 处理请求头
	 */
	@Override
	public String getHeader(String name) {
		String value = super.getHeader(name);
		if (StringUtils.isNotEmpty(value)) {
			return XssCleanUtils.clean(value);
		}
		return value;
	}

	/**
	 * 处理请求头枚举
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		Enumeration<String> values = super.getHeaders(name);
		List<String> headerValues = new ArrayList<>();
		while (values.hasMoreElements()) {
			String value = values.nextElement();
			if (StringUtils.isNotEmpty(value)) {
				headerValues.add(XssCleanUtils.clean(value));
			} else {
				headerValues.add(value);
			}
		}
		return Collections.enumeration(headerValues);
	}

	/**
	 * 核心：处理 Request Body
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		// 如果不是 JSON/表单类型，直接返回原始流
		String contentType = request.getContentType();
		if (contentType == null ||
				(!contentType.contains(MediaType.APPLICATION_JSON_VALUE) &&
						!contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE))) {
			return super.getInputStream();
		}

		// 缓存并处理请求体
		if (body == null) {
			// 读取原始请求体
			byte[] originalBytes = IOUtils.toByteArray(request.getInputStream());
			if (originalBytes.length == 0) {
				body = originalBytes;
			} else {
				// 转换为字符串并清理 XSS
				String originalStr = new String(originalBytes, StandardCharsets.UTF_8);
				String cleanStr = XssCleanUtils.clean(originalStr);
				body = cleanStr.getBytes(StandardCharsets.UTF_8);
			}
		}

		// 返回处理后的字节流
		return new ServletInputStream() {
			private final ByteArrayInputStream bis = new ByteArrayInputStream(body);

			@Override
			public boolean isFinished() {
				return bis.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener listener) {
				// 无需实现
			}

			@Override
			public int read() throws IOException {
				return bis.read();
			}
		};
	}

	/**
	 * 重写 getReader，确保读取 Body 的方式都被处理（适配 Jakarta EE）
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(
				new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)
		);
	}
}