package com.awesomecopilot.cache.exception;

/**
 * 加锁线程已经被中断异常
 * <p/>
 * Copyright: Copyright (c) 2025-12-26 16:44
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LockThreadInterruptedException extends RuntimeException{

	public LockThreadInterruptedException() {
		super();
	}

	public LockThreadInterruptedException(String message) {
		super(message);
	}

	public LockThreadInterruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockThreadInterruptedException(Throwable cause) {
		super(cause);
	}
}
