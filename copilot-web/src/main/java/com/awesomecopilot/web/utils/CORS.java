package com.awesomecopilot.web.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.awesomecopilot.common.lang.utils.StringUtils.joinWith;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * 跨域访问支持
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:22
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class CORS {
	
	public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	
	private static final Logger logger = LoggerFactory.getLogger(CORS.class);
	
	public static CorsBuilder builder() {
		return new CorsBuilder();
	}
	
	public static class CorsBuilder {
		
		private CorsBuilder() {
		}
		
		private List<String> allowedOrigins = new ArrayList<>();
		private List<String> allowedMethods = new ArrayList<>();
		private List<String> allowedHeaders = new ArrayList<>();
		private boolean allowCredentials;
		
		public CorsBuilder allowedOrigins(String... origins) {
			for (int i = 0; i < origins.length; i++) {
				String origin = origins[i];
				if (isNotBlank(origin)) {
					this.allowedOrigins.add(trim(origin));
				} else {
					logger.info("给定的Origin为空，忽略之");
				}
			}
			return this;
		}
		
		public CorsBuilder allowedMethods(String... methods) {
			for (int i = 0; i < methods.length; i++) {
				String method = methods[i];
				if (isNotBlank(method)) {
					this.allowedMethods.add(trim(method));
				} else {
					logger.info("给定的Mehtod为空，忽略之");
				}
			}
			return this;
		}
		
		public CorsBuilder allowedHeaders(String... headers) {
			for (int i = 0; i < headers.length; i++) {
				String header = headers[i];
				if (isNotBlank(header)) {
					this.allowedHeaders.add(trim(header));
				} else {
					logger.info("给定的Header为空，忽略之");
				}
			}
			return this;
		}
		
		public CorsBuilder allowAll() {
			this.allowedHeaders.clear();
			this.allowedHeaders.add("*");
			
			this.allowedMethods.clear();
			this.allowedMethods.add("*");
			
			this.allowedOrigins.clear();
			this.allowedOrigins.add("*");
			return this;
		}
		
		/**
		 * 是否允许携带凭证(Cookie)。浏览器规则: Access-Control-Allow-Origin 为 * 时
		 * Allow-Credentials 必须不为 true, 因此 allowAll() 场景下本设置会被 build() 忽略
		 * (评审报告 P2-7: 带 Cookie 的跨域要回显具体 Origin, 并配 Vary: Origin 防缓存串用)。
		 */
		public CorsBuilder allowCredentials(boolean allowCredentials) {
			this.allowCredentials = allowCredentials;
			return this;
		}
		
		public void build(HttpServletResponse response) {
			response.setCharacterEncoding("UTF-8");
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, joinWith(", ", allowedHeaders));
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, joinWith(", ", allowedOrigins));
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_METHODS, joinWith(", ", allowedMethods));
			// 凭证只允许配合"恰好一个具体Origin"使用(独立评审 S-3):
			// 通配符 * 与 credentials 是浏览器规定互斥的组合; 多 Origin 时
			// Access-Control-Allow-Origin 输出的是逗号列表, 浏览器同样必然拒绝,
			// 再写 Credentials:true 等于对注定失败的响应宣称凭证可用
			boolean singleConcreteOrigin = allowedOrigins.size() == 1 && !"*".equals(allowedOrigins.get(0));
			if (allowCredentials && singleConcreteOrigin) {
				response.setHeader("Access-Control-Allow-Credentials", "true");
				// 回显具体Origin的响应按请求Origin变化, 必须声明 Vary: Origin, 否则共享缓存会把
				// A 站的允许响应发给 B 站的预检
				response.setHeader("Vary", "Origin");
			}
			// allowAll() 的多 Origin 列表是 HEAD 旧有行为(输出逗号列表), 本次仅收紧 credentials 判定
			if (allowCredentials && !singleConcreteOrigin) {
				logger.warn("allowCredentials(true) 只在恰好配置 1 个具体 Origin 时生效; 当前 Origin 配置为 {}"
						+ "(通配符与凭证互斥是浏览器规定; 多 Origin 需要按请求头回显单个 Origin, 本静态构建器做不到)",
						allowedOrigins);
			}
		}
	}
}
