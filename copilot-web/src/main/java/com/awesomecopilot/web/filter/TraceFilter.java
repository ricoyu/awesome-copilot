package com.awesomecopilot.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * Copyright: (C), 2020/1/2 19:59
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Component
@WebFilter("/*")
public class TraceFilter implements Filter {
	
	private static final int MAX_TRACE_ID = 1000000;
	/**
	 * traceId 前缀, 用 100 * MAX_TRACE_ID 预留"最多 100 个微服务"的编号空间。
	 * 注意: 这是写死的常量 100, 并没有随实例编号变化, 因此不同进程可能生成相同 traceId
	 * (跨服务链路的唯一性靠上游服务透传 TRACE_ID 头保证)。字段从 volatile 改 final(评审报告 P2-5):
	 * 它没有任何写入方, 却声明成可变。
	 */
	private static final int APP_ID = 100 * MAX_TRACE_ID;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
						 FilterChain chain) throws IOException, ServletException {
		// 从请求头中获取traceId
		String traceId = ((HttpServletRequest) request).getHeader("TRACE_ID");
		// 不存在(或全空白)就生成一个
		if (null == traceId || traceId.trim().isEmpty()) {
			traceId = String.valueOf(APP_ID + nextTraceId());
		}
		MDC.put("traceId", traceId);
		/*
		 * 然后在logback-spring.xml里面配置一下输出traceId， 这样log.info("xxx")的输出中就会带traceId了
		 * <property name="CONSOLE_LOG_PATTERN" value="${CONSOLE_LOG_PATTERN:-%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }) --- [%t] [%X{traceId}] {magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} L%-4L %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"/>
		 * <property name="FILE_LOG_PATTERN" value="${FILE_LOG_PATTERN:-%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] [%X{traceId}] %-40.40logger{39} L%-4L : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"/>
		 */
		// finally 里清理 MDC(评审报告 P2-5): Tomcat 线程会被下一个请求复用,
		// 不清理的话, 不走本 Filter 的路径(如异步收尾、内部转发)打出的日志会带上上一个请求的 traceId
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove("traceId");
		}
	}
	
	/**
	 * 取 traceId 的随机段。旧实现每个请求 new Random(): 无谓的对象分配, 且同毫秒创建的
	 * 实例在早期 JDK 里种子相同会产生相同序列。改为复用线程本地的 ThreadLocalRandom。
	 * protected 是为了测试可注入固定序列验证拼接逻辑。
	 */
	protected int nextTraceId() {
		return ThreadLocalRandom.current().nextInt(MAX_TRACE_ID);
	}
}
