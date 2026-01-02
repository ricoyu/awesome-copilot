package com.awesomecopilot.web.http;

import com.awesomecopilot.web.utils.XssCleanUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
	
	private HttpServletRequest request;
	
	public XssHttpServletRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
	}
	
	@Override
	public String getParameter(String name) {
		String value = super.getParameter(name);
		if (isNotEmpty(name)) {
			return XssCleanUtils.clean(value);
		}
		
		return value;
	}
	
	@Override
	public String[] getParameterValues(String name) {
		String[] values = super.getParameterValues(name);
		if (values == null || values.length == 0) {
			return values;
		}
		
		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			if (isNotEmpty(value)) {
				values[i] = XssCleanUtils.clean(value);
			}
		}
		return values;
	}
	
	@Override
	public String getHeader(String name) {
		String value = super.getHeader(name);
		if (isNotEmpty(value)) {
			return XssCleanUtils.clean(value);
		}
		
		return value;
	}
	
	@Override
	public Enumeration<String> getHeaders(String name) {
		Enumeration<String> values = super.getHeaders(name);
		List<String> headerValues = new ArrayList<>();
		while (values.hasMoreElements()) {
			String value = values.nextElement();
			if (isNotEmpty(value)) {
				headerValues.add(XssCleanUtils.clean(value));
			} else {
				headerValues.add(value);
			}
		}
		return Collections.enumeration(headerValues);
	}
}
