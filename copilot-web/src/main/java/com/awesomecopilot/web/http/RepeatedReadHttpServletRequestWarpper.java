package com.awesomecopilot.web.http;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 类名拼写错误(Warpper 应为 Wrapper), 已被
 * {@link RepeatedReadHttpServletRequestWrapper} 取代(评审报告 P2-10)。
 * 改名对外部使用者是破坏性变更, 旧名保留一个大版本做转发, 下个大版本删除。
 *
 * <p>
 * Copyright: (C), 2020-09-08 14:31
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 * @deprecated 请使用 {@link RepeatedReadHttpServletRequestWrapper}
 */
@Deprecated
public class RepeatedReadHttpServletRequestWarpper extends RepeatedReadHttpServletRequestWrapper {
	
	/**
	 * @param request the {@link HttpServletRequest} to be wrapped.
	 * @throws IllegalArgumentException if the request is null
	 */
	public RepeatedReadHttpServletRequestWarpper(HttpServletRequest request) {
		super(request);
	}
}
