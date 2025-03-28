package com.awesomecopilot.nio.client;

import com.awesomecopilot.common.lang.utils.IOUtils;

/**
 * <p>
 * Copyright: (C), 2023-10-31 17:56
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class NioClient {
	
	private static NioClientHandler nioClientHandler;
	
	public static void start() {
		nioClientHandler = new NioClientHandler("localhost", 8080);
		new Thread(nioClientHandler, "Client").start();
	}
	
	public static boolean sendMsg(String msg) {
		return nioClientHandler.sendMsg(msg);
	}
	
	public static void main(String[] args) throws InterruptedException {
		start();
		IOUtils.readUserInput((input) -> {
			sendMsg(input);
		});
	}
}
