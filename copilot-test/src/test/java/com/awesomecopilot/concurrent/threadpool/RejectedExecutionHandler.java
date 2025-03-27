package com.awesomecopilot.concurrent.threadpool;

public interface RejectedExecutionHandler {
	
	void rejectedExecution(Runnable r, CopilotThreadPoolExecutor executor);
}
