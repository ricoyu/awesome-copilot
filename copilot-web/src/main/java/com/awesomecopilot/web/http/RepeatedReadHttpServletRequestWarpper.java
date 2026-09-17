package com.awesomecopilot.web.http;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.awesomecopilot.networking.constants.ContentTypes.MULTIPART_FORM_DATA;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * <p>
 * Copyright: (C), 2020-09-08 14:31
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class RepeatedReadHttpServletRequestWarpper extends HttpServletRequestWrapper {
	
	private final byte[] body;
	
	/**
	 * Constructs a request object wrapping the given request.
	 *
	 * @param request the {@link HttpServletRequest} to be wrapped.
	 * @throws IllegalArgumentException if the request is null
	 */
	public RepeatedReadHttpServletRequestWarpper(HttpServletRequest request) {
		super(request);
		byte[] data = null;
		/*
		 * 表单上传完整的Content-Type类似这样
		 * multipart/form-data; boundary=Z5Y7E2JUVdczoE_2jdS2xlSxPQcWP3
		 */
		String contentType = request.getHeader("Content-Type");
		//contentType是multipart/form-data时, 不允许重复读request body, 这个是文件上传, 重复读不了
		if (isNotBlank(contentType) && contentType.indexOf(MULTIPART_FORM_DATA) != 0) {
			/*
			 * 按原始字节缓存请求体(评审报告 P1-1): 旧实现走 WebUtils.bodyString,
			 * 那里按行读取后删除 \r\n\t, 字段值含真实换行的 JSON 缓存后就和客户端
			 * 原始字节不一致, 下游签名校验/审计比对全部失真, 所以这里直接读 InputStream。
			 */
			try {
				data = IOUtils.toByteArray(request.getInputStream());
			} catch (IOException e) {
				throw new IllegalStateException("缓存请求体失败: " + e.getMessage(), e);
			}
		}
		
		body = data;
	}
	
	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(), UTF_8));
	}
	
	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (body == null) {
			return super.getInputStream();
		}
		
		final ByteArrayInputStream bias = new ByteArrayInputStream(body);
		
		return new ServletInputStream() {
			@Override
			public boolean isFinished() {
				return bias.available() == 0;
			}
			
			@Override
			public boolean isReady() {
				return true;
			}
			
			@Override
			public void setReadListener(ReadListener readListener) {
				
			}
			
			@Override
			public int read() throws IOException {
				return bias.read();
			}
		};
	}
}
