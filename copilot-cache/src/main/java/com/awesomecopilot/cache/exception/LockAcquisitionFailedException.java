package com.awesomecopilot.cache.exception;

/**
 * 获取锁失败异常
 * <p>
 * 当非阻塞锁在有限自旋后仍无法获取锁时抛出
 * <p/>
 * Copyright: Copyright (c) 2026-04-23 8:58
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LockAcquisitionFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String lockKey;
    private final int spinCount;

    public LockAcquisitionFailedException(String lockKey, int spinCount) {
        super(String.format("获取锁失败: key=%s, 自旋次数=%d", lockKey, spinCount));
        this.lockKey = lockKey;
        this.spinCount = spinCount;
    }

    public LockAcquisitionFailedException(String message) {
        super(message);
        this.lockKey = null;
        this.spinCount = 0;
    }

    public String getLockKey() {
        return lockKey;
    }

    public int getSpinCount() {
        return spinCount;
    }
}