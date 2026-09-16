package com.awesomecopilot.networking.httpclient;

import org.apache.http.conn.HttpClientConnectionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 清除连接池中过期的连接
 * <p>
 * Copyright: Copyright (c) 2019-12-25 11:59
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IdleConnectionEvictor extends Thread {

	private final List<? extends HttpClientConnectionManager> connectionManagers;
	private final long idleTime;
	private final TimeUnit idleTimeUnit;
	private volatile boolean shutdown;

	/**
	 * 创建回收线程（评审报告 P2-2 修复：此类此前定义了但全仓库没有任何地方启动它）。
	 * 构造器不再自动 start()——线程生命周期交给创建方管理（AbstractRequestBuilder 静态块
	 * 创建后立即 start），测试里也可以只构造不启动。
	 *
	 * @param connectionManagers 要清理的一个或多个连接池
	 * @param idleTime           空闲超过该时长的连接即使未过期也主动关闭
	 * @param idleTimeUnit       idleTime 的单位
	 */
	public IdleConnectionEvictor(Collection<? extends HttpClientConnectionManager> connectionManagers,
	                             long idleTime, TimeUnit idleTimeUnit) {
		this.connectionManagers = new ArrayList<>(connectionManagers);
		this.idleTime = idleTime;
		this.idleTimeUnit = idleTimeUnit;
		setName("copilot-http-idle-connection-evictor");
		setDaemon(true);
	}

	@Override
	public void run() {
		try {
			while (!shutdown) {
				synchronized (this) {
					wait(5000);
					for (HttpClientConnectionManager cm : connectionManagers) {
						// 关闭已过期的连接
						cm.closeExpiredConnections();
						/*
						 * 再关闭空闲超过阈值的连接。只做 closeExpiredConnections 不够:
						 * keep-alive 连接被服务端或负载均衡单方面掐断后, 在客户端这边它
						 * "没有过期", 会一直留在池里; 下次借出它发请求才会失败
						 * (NoHttpResponseException)。主动关掉闲置过久的连接, 这个时间窗就基本消除。
						 */
						cm.closeIdleConnections(idleTime, idleTimeUnit);
					}
				}
			}
		} catch (InterruptedException ex) {
			/*
			 * 被中断就退出线程, 但必须把中断标记恢复回去——吞掉中断标记会让
			 * 上层(JVM 关闭流程、线程池管理器)收不到信号。原实现是空的 catch 块。
			 */
			Thread.currentThread().interrupt();
		}
	}

	public void shutdown() {
		shutdown = true;
		synchronized (this) {
			notifyAll();
		}
	}
}
