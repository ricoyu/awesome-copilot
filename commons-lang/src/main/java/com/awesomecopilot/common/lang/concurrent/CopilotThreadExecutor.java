package com.awesomecopilot.common.lang.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自定义线程池<p>
 * 1.监控线程池状态及异常关闭等情况<p>
 * 2.监控线程池运行时的各项指标, 比如:任务等待数、已完成任务数、任务异常信息、核心线程数、最大线程数等<p>
 * http://ifeve.com/java%E8%B8%A9%E5%9D%91%E8%AE%B0%E7%B3%BB%E5%88%97%E4%B9%8B%E7%BA%BF%E7%A8%8B%E6%B1%A0/
 *
 * <p>
 * Copyright: Copyright (c) 2020-12-07 13:47
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CopilotThreadExecutor extends ThreadPoolExecutor {
	
	private static final Logger log = LoggerFactory.getLogger(CopilotThreadExecutor.class);
	
	private TimeUnit timeUnit;
	
	/**
	 * @param corePoolSize    核心线程数
	 * @param maximumPoolSize 最大线程数
	 * @param keepAliveTime
	 * @param unit
	 */
	public CopilotThreadExecutor(int corePoolSize,
	                             int maximumPoolSize,
	                             long keepAliveTime,
	                             TimeUnit unit) {
		super(corePoolSize,
				maximumPoolSize,
				keepAliveTime,
				unit,
				new SynchronousQueue<>(),
				new CopilotThreadFactory(),
				new AbortWithReportPolicy());
		this.timeUnit = unit;
	}
	
	/**
	 * @param corePoolSize    核心线程数
	 * @param maximumPoolSize 最大线程数
	 * @param keepAliveTime
	 * @param unit
	 */
	public CopilotThreadExecutor(int corePoolSize,
	                             int maximumPoolSize,
	                             long keepAliveTime,
	                             TimeUnit unit,
	                             String poolNamePrefix) {
		super(corePoolSize,
				maximumPoolSize,
				keepAliveTime,
				unit,
				new SynchronousQueue<>(),
                new CopilotThreadFactory(poolNamePrefix),
				new AbortWithReportPolicy());
		this.timeUnit = unit;
	}
	
	/**
	 * @param corePoolSize    核心线程数
	 * @param maximumPoolSize 最大线程数
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue       等待队列
	 * @param threadFactory
	 * @param handler         决绝策略
	 */
	public CopilotThreadExecutor(int corePoolSize,
	                             int maximumPoolSize,
	                             long keepAliveTime,
	                             TimeUnit unit,
	                             BlockingQueue<Runnable> workQueue,
	                             ThreadFactory threadFactory,
	                             RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
		this.timeUnit = unit;
	}
	
	@Override
	public void shutdown() {
		/*
		 * 线程池将要关闭事件
		 * 此方法会等待线程池中的任务(包括正在执行的任务和队列中等待的任务)执行完毕再关闭
		 */
		monitor("ThreadPool will be shutdown:", true);
		super.shutdown();
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		/*
		 * 此方法会立即关闭线程池, 同时会返回队列中等待的任务
		 */
		monitor("ThreadPool going to immediately be shutdown:", true);
		/*
		 * 这是在等待队列中还没有执行, 被丢弃的任务
		 */
		List<Runnable> dropTasks;
		try {
			dropTasks = super.shutdownNow();
			log.error("ThreadPool discard task count:{}", dropTasks.size());
		} catch (Exception e) {
			//P2-34: ThreadPoolExecutor.shutdownNow() 契约返回非 null; 修复前这里返回 null,
			//调用方对返回值 .size() 直接 NPE。降级为空列表(异常已带堆栈记录)。
			log.error("ThreadPool shutdownNow error", e);
			dropTasks = Collections.emptyList();
		}
		return dropTasks;
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		/*
		 * 监控线程池运行时的各项指标
		 */
		//P2-34: 修复前每执行一个任务就以 INFO 级别打印 13 个字段的监控行, 高 QPS 下日志量
		//急剧增长、反过来拖慢被观测的池。降到 DEBUG: 排障时打开 DEBUG 即有全量数据, 平时不产出。
		monitor("ThreadPool monitor data:", false);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable e) {
		if (e != null) {
			log.error("Unknown exception caught in ThreadPool afterExecute:", e);
		}
	}
	
	/**
	 * 监控线程池运行时的各项指标
	 * 比如:任务等待数、任务异常信息、已完成任务数、核心线程数、最大线程数等
	 *
	 * @param title         事件标题
	 * @param atErrorLevel  true 以 ERROR 输出(关停等低频关键事件); false 以 DEBUG 输出
	 *                      (每任务监控行, 修复前是 INFO, 高 QPS 下刷屏, 见 P2-34;
	 *                      级别不开时直接跳过 MessageFormat 拼接, 不产生格式化开销)
	 */
	private void monitor(String title, boolean atErrorLevel) {
		if (!atErrorLevel && !log.isDebugEnabled()) {
			return;
		}
		try {
			// 线程池监控信息记录
			String threadPoolMonitor = MessageFormat.format(
					"{0}{1} " +
							"Core pool size:{2}, " +
							"Current pool size:{3}, " +
							"Queue wait size:{4}, " +
							"Active count:{5}, " +
							"Completed task count:{6}, " +
							"Task count:{7}, " +
							"Largest pool size:{8}, " +
							"Max pool size:{9}, " +
							"Keep alive time:{10}, " +
							"Is shutdown:{11}, " +
							"Is terminated:{12}, " +
							"Thread name:{13}{14}",
					System.lineSeparator(), title,
					this.getCorePoolSize(),
					this.getPoolSize(),
					this.getQueue().size(), //当前排队的线程数
					this.getActiveCount(),  //当前活动线程数
					this.getCompletedTaskCount(), //执行完成线程数
					this.getTaskCount(),    //总线程数 = 排队线程数 + 活动线程数 +  执行完成线程数
					this.getLargestPoolSize(),
					this.getMaximumPoolSize(),
					this.getKeepAliveTime(timeUnit != null ? timeUnit : TimeUnit.SECONDS),
					this.isShutdown(),
					this.isTerminated(),
					Thread.currentThread().getName(), System.lineSeparator());
			if (atErrorLevel) {
				log.error(threadPoolMonitor);
			} else {
				log.debug(threadPoolMonitor);
			}
		} catch (Exception e) {
			log.error("ThreadPool monitor error", e);
		}
	}
	
}
