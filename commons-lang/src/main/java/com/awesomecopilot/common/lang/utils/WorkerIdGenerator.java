package com.awesomecopilot.common.lang.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于本机IP+进程号自动推测 workerId, 仅供本地开发/单机单实例场景使用。
 * <p/>
 * 为什么不能指望它保证分布式唯一: workerId 只有5位32个槽, IP推导值再均匀也躲不开
 * 跨机撞号(同一网段两台机器IP尾段相同就是同一个值), 同机多进程旧实现(IP推导)更是必撞。
 * 本实现把"同机多JVM进程必撞"降为"约1/32概率撞"(混入进程号), 但生产环境仍然必须通过
 * 系统属性 copilot.snowflake.worker-id / 环境变量 COPILOT_SNOWFLAKE_WORKER_ID /
 * application.properties(yml) 显式配置, 详见 {@link SnowflakeId} 无参构造器的说明。
 * <p/>
 * NetworkInterface.getNetworkInterfaces() 这个 JDK API 非常慢, 所以结果缓存, 只算一次。
 * 计算是幂等的(同进程输入固定), 多线程同时进入慢路径也只是多算一遍、结果相同,
 * 用 CAS 取胜者的结果写入缓存即可。
 * <p/>
 * Copyright: Copyright (c) 2026-01-06 11:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WorkerIdGenerator {

	// 使用 AtomicInteger 缓存推导结果(-1表示还没算过), 保证多线程下只保留一份
	private static final AtomicInteger CACHED_WORKER_ID = new AtomicInteger(-1);

	public static int generateWorkerIdFromIp() {
		// 快速路径：已经计算过，直接返回
		int cached = CACHED_WORKER_ID.get();
		if (cached != -1) {
			return cached;
		}
		
		// 慢路径：取本机第一个非回环地址的尾段做种子, 混入进程号后 % 32。
		// 混入进程号是为了解决旧实现"同一台机器起多个JVM, IP相同推导值必相同"的必撞问题;
		// 0 是完全合法的 workerId, 不做任何"为0改成1"的修正(旧实现那一下反而制造了与真1号机器的撞车)。
		long seed = 0L;
		long derived;
		try {
			derived = -1;
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			outer:
			while (interfaces.hasMoreElements()) {
				NetworkInterface nif = interfaces.nextElement();
				if (nif.isLoopback() || nif.isVirtual() || !nif.isUp()) {
					continue;
				}
				
				Enumeration<InetAddress> addresses = nif.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
						continue;
					}
					
					byte[] ipBytes = addr.getAddress();
					for (int i = Math.max(0, ipBytes.length - 2); i < ipBytes.length; i++) {
						seed = 31 * seed + (ipBytes[i] & 0xFF);
					}
					// 找到第一个可用的非回环地址就够了, 不再枚举
					// 先取正(去掉符号位), 再对32取模, 括号不能省: % 的优先级高于 &
					derived = ((seed * 31 + pid()) & 0x7FFFFFFFL) % 32;
					break outer;
				}
			}
			if (derived == -1) {
				// 没有任何可用网卡时的 fallback
				derived = pid() % 32;
			}
		} catch (Exception e) {
			// 网卡枚举失败(受限环境/容器裁剪), 落到纯pid推导
			derived = pid() % 32;
		}
		
		// 计算可能重复执行但结果一致(幂等), CAS 写入第一个完成者的结果
		int workerId = (int) derived;
		if (!CACHED_WORKER_ID.compareAndSet(-1, workerId)) {
			return CACHED_WORKER_ID.get();
		}
		return workerId;
	}
	
	/**
	 * 当前进程号, ManagementFactory 的运行时名称形如 "12345@hostname"
	 */
	private static long pid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		int at = name.indexOf('@');
		try {
			return Long.parseLong(at > 0 ? name.substring(0, at) : name);
		} catch (NumberFormatException e) {
			return 1L;
		}
	}
}
