package com.copilot.concurrent.threadpool;

public interface RejectedExecutionHandler {
	
	void rejectedExecution(Runnable r, LoserThreadPoolExecutor executor);
}
