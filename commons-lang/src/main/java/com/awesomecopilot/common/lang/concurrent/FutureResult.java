package com.awesomecopilot.common.lang.concurrent;

import com.awesomecopilot.common.lang.exception.AsyncExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 封装Concurrent报下的Future接口, 出错则抛AsyncExecutionException
 * <p>
 * Copyright: Copyright (c) Nov 9, 2017
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @param <T>
 */
public class FutureResult<T> implements Serializable {

	private static final long serialVersionUID = 3535392824373357338L;

	private static final Logger logger = LoggerFactory.getLogger(FutureResult.class);

	private CompletableFuture<T> future;

	public FutureResult(CompletableFuture<T> future) {
		this.future = future;
	}

	/**
	 * 返回结果
	 * 
	 * @return
	 */
	public T get() {
		try {
			Object t = future.get();
			if (t instanceof CompletableFuture) {
				return ((CompletableFuture<T>) t).get();
			}
			//P2-37: 修复前 else 分支对同一个 future 再调一次 get() 重新取值——值已在手, 直接转型返回
			return (T) t;
		} catch (InterruptedException e) {
			//P2-37: 修复前把中断与业务异常混在一起 catch, 且不恢复中断标记——
			//线程池的取消信号在这里断链, 上层分不清"任务失败"与"被中断"。
			//先恢复标记再包装抛出, 调用方既能从异常链看到 Interrupted 原因, 线程也保留中断状态。
			Thread.currentThread().interrupt();
			logger.error(">>>>>> Error on FutureResult.get: ", e);
			throw new AsyncExecutionException("FutureResult.get() interrupted", e);
		} catch (Exception e) {
			//注意: 这里必须保持 catch(Exception) 的宽 catch——future 被 cancel() 后 get()
			//抛的 CancellationException 也要按老契约包装成 AsyncExecutionException,
			//调用方 catch AsyncExecutionException 兜住一切失败场景的语义不能变
			//(评审纠正 2026-09-23: 中途曾收窄成只 catch ExecutionException, 会让取消场景漏出)。
			logger.error(">>>>>> Error on FutureResult.get: ", e);
			throw new AsyncExecutionException("", e);
		}
	}
	
	/**
	 * 如果从future对象获取时抛出异常，则返回supplier的返回值
	 * 
	 * @return
	 */
	public T orElseGet(Supplier<T> supplier) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			//P2-37: 走回落分支也要恢复中断标记, 让调用方有机会感知线程被中断过
			Thread.currentThread().interrupt();
			logger.error(">>>>>> Error on FutureResult.get: ", e);
		} catch (ExecutionException e) {
			logger.error(">>>>>> Error on FutureResult.get: ", e);
		}
		return supplier.get();
	}
	
	/**
	 * 如果从future对象获取时抛出异常，则返回result
	 * 
	 * @param result
	 * @return T
	 */
	public T orElseGet(T result) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			//P2-37: 同上, 中断标记不能丢弃
			Thread.currentThread().interrupt();
			logger.error(">>>>>> Error on FutureResult.get: ", e);
		} catch (ExecutionException e) {
			logger.error(">>>>>> Error on FutureResult.get: ", e);
		}
		return result;
	}

	/**
	 * 如果返回结果不为null，那么消费此结果。
	 * 不管消费与否，最终都返回结果T
	 * @param consumer
	 * @return T
	 */
	public T ifPresent(Consumer<T> consumer) {
		T result = get();
		if (result != null) {
			consumer.accept(result);
			logger.info("FutureResult Consumed! {}", result);
		}
		return result;
	}
}
