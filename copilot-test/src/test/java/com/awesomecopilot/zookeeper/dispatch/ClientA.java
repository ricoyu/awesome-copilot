package com.awesomecopilot.zookeeper.dispatch;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import lombok.extern.slf4j.Slf4j;

/**
 * 先分别启动ClientA和ClientB, 然后通过zkCli.sh分别执行一下命令并观察结果
 * 
 * create /sishuok/dispatch ""
 * set /sishuok/dispatch ClientA
 * set /sishuok/dispatch ClientB
 * <p>
 * Copyright: Copyright (c) 2019-04-04 14:43
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
@Slf4j
public class ClientA {
	private ZooKeeper zk = null;
	private final static String DISPATCH_NODE = "/sishuok/dispatch";

	private ZooKeeper startZK(String hostPort, int sessionTime) {
		try {
			CountDownLatch countDownLatch = new CountDownLatch(1);
			zk = new ZooKeeper(hostPort, sessionTime, (event) -> {
				if (event.getState() == KeeperState.SyncConnected) {
					countDownLatch.countDown();
				}
			});
			countDownLatch.await();
		} catch (Exception e) {
			log.error("", e);
			throw new RuntimeException(e);
		}

		getNode();
		return zk;
	}

	private void stopZK(ZooKeeper zk) {
		try {
			zk.close();
		} catch (Exception e) {
			log.error("", e);
			throw new RuntimeException(e);
		}
	}

	private void getNode() {
		byte[] rets = null;
		try {
			rets = zk.getData(DISPATCH_NODE, event -> getNode(), new Stat());
		} catch (Exception e) {
//			log.error("", e);
			return;
//			throw new RuntimeException(e);
		}
		String retStr = new String(rets);
		System.out.println("AAnow state=" + retStr);
		if ("ClientA".equals(retStr)) {
			System.out.println("now is ClientA execute...");
		}
	}

	public static void main(String[] args) throws InterruptedException {
		ClientA hello = new ClientA();
		ZooKeeper zk = hello.startZK("192.168.102.104:2181,192.168.102.106:2181,192.168.102.107:2181", 200000);

		// 功能处理
		Thread.currentThread().join();
//		hello.stopZK(zk);

	}
}
