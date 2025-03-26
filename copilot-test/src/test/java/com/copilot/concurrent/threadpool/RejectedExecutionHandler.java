package com.copilot.concurrent.threadpool;

public interface RejectedExecutionHandler {
	
	void rejectedExecution(Runnable r, CopilotThreadPoolExecutor executor);
}
