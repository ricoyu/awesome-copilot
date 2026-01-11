package com.awesomecopilot.common.lang.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于服务器IP地址生成workerId, 使用了缓存机制提升性能, 因为NetworkInterface.getNetworkInterfaces() 这个 JDK API 非常慢, 每次都调用的话会产生性能瓶颈
 * <p/>
 * Copyright: Copyright (c) 2026-01-06 11:43
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WorkerIdGenerator {

    // 使用 AtomicInteger 保证线程安全的懒加载
    private static final AtomicInteger CACHED_WORKER_ID = new AtomicInteger(-1);

    public static int generateWorkerIdFromIp() {
        // 快速路径：已经计算过，直接返回
        int cached = CACHED_WORKER_ID.get();
        if (cached != -1) {
            return cached;
        }

        // 慢路径：使用 CAS 确保只有一个线程执行计算
        if (CACHED_WORKER_ID.compareAndSet(-1, calculateWorkerId())) {
            // 计算成功
        }
        return CACHED_WORKER_ID.get();
    }

    private static int calculateWorkerId() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                // 过滤条件保持不变
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
                    int hash = 0;
                    for (int i = Math.max(0, ipBytes.length - 2); i < ipBytes.length; i++) {
                        hash = 31 * hash + (ipBytes[i] & 0xFF);
                    }
                    int workerId = Math.abs(hash) % 32;
                    return workerId > 0 ? workerId : 1; // 避免返回0（可选）
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 1; // fallback
    }
}