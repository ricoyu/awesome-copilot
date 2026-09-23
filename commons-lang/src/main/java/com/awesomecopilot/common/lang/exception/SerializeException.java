package com.awesomecopilot.common.lang.exception;

/**
 * 序列化/反序列化失败异常
 * <p>
 * P2-9(CODE_REVIEW_REPORT): 修复前 SerializeUtils/KryoUtils/ProtostuffUtils 把失败
 * 降级成 null(或 byte[0]、字段全默认值的对象), 调用方分不清"值本来就是空""字节损坏"
 * "对象不可序列化"三种情况。统一契约:
 * <ul>
 * <li>入参 null → 返回 null;</li>
 * <li>序列化/反序列化真正失败 → 抛本异常(携带原始 cause), 不再返回 null 伪装成'值本来就是空'。</li>
 * </ul>
 * <p>
 * Copyright: (C), 2026-09-23
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SerializeException extends RuntimeException {
	private static final long serialVersionUID = 1L;


	public SerializeException(String message) {
		super(message);
	}

	public SerializeException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerializeException(Throwable cause) {
		super(cause);
	}
}
