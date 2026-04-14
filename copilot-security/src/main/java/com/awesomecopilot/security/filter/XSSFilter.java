package com.awesomecopilot.security.filter;

import com.awesomecopilot.security.http.XSSRequestWrapper;
import com.awesomecopilot.security.utils.XSSUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 注册到该bean到Spring容器中即可
 * <p>
 * Copyright: (C), 2020-7-22 0022 10:06
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class XSSFilter implements Filter {
	
	private static final Logger log = LoggerFactory.getLogger(XSSFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("XSSFilter inited");
	}
	
	@Override
	public void destroy() {
		log.info("XSSFilter destory");
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		XSSRequestWrapper wrappedRequest = new XSSRequestWrapper((HttpServletRequest) request);
		
		String body = IOUtils.toString(wrappedRequest.getReader());
		if (isNotBlank(body)) {
			body = XSSUtils.stripXSS(body);
			wrappedRequest.resetInputStream(body.getBytes());
		}
		
		chain.doFilter(wrappedRequest, response);
	}
}