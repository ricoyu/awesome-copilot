package com.awesomecopilot.common.lang.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * <p>
 * Copyright: (C), 2024-01-12 15:21
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class TraceId {
	
	private static final AtomicLong SEQUENCE = new AtomicLong(0);
	
	private static final String PREFIX = "TID-";
	
	/**
	 * P1-24: 修复前串里只有"服务名-毫秒-进程内序列"——SEQUENCE 每个 JVM 各自从 0 起步,
	 * 同服务多实例在同一毫秒生成的 traceId 完全相同, 全链路日志按 traceId 聚合会把
	 * 不同实例的不同请求串成一条(必然重号, 不是概率碰撞)。
	 * 掺入每 JVM 随机生成的 8 位实例段, 让不同进程即使同毫秒、同序列值也不同。
	 */
	private static final String INSTANCE_ID =
			String.format("%08x", UUID.randomUUID().getMostSignificantBits() >>> 32); //定长8位hex(完整32bit熵, 评审修复: hashCode&0x7fffffff 只剩31bit)
	
	/**
	 * 生成traceId, 格式为TID-时间戳-实例段-序列号
	 *
	 * @return String
	 */
	public static String traceId() {
		return traceId(null);
	}
	
	/**
	 * 生成traceId, 格式为TID-服务名-时间戳-实例段-序列号
	 *
	 * @param serviceId
	 * @return String
	 */
	public static String traceId(String serviceId) {
		long currentTimeMillis = System.currentTimeMillis();
		if (isBlank(serviceId)) {
			return PREFIX + currentTimeMillis + "-" + INSTANCE_ID + "-" + SEQUENCE.incrementAndGet();
		}
		return PREFIX + serviceId + "-" + currentTimeMillis + "-" + INSTANCE_ID + "-" + SEQUENCE.incrementAndGet();
	}
	
}
