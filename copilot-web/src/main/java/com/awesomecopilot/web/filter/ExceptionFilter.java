package com.awesomecopilot.web.filter;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.vo.Results;
import com.awesomecopilot.web.utils.RestUtils;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static com.awesomecopilot.common.lang.errors.ErrorTypes.INTERNAL_SERVER_ERROR;

/**
 * 在过滤器链上发生未处理的异常时, RestExceptionAdvice是处理不到的, 所以通过这个Filter来统一处理
 * <p>
 * Copyright: (C), 2020-08-18 13:57
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class ExceptionFilter implements Filter {
	
	public static final String ROUTE_CAUSE = "routeCause";
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		} catch (Throwable e) {
			log.error("", e);
			ThreadContext.put(ROUTE_CAUSE, e);
			RestUtils.writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR, Results.status(INTERNAL_SERVER_ERROR).build());
		}
	}
}
