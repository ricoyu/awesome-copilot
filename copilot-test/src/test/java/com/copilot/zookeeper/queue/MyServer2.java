package com.copilot.zookeeper.queue;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import com.copilot.zookeeper.ZookeeperClient;

public class MyServer2 {
	private ZookeeperClient zookeeperClient;
	private ZooKeeper zk = null;
	private final static String PARENT_NODE = "/sishuok/myqueue1";

	private ZooKeeper startZK(String hostPort, int sessionTime) {
		zookeeperClient = ZookeeperClient.initialize(hostPort, sessionTime);
		zk = zookeeperClient.zk();
		return zk;
	}

	private void stopZK(ZooKeeper zk) {
		try {
			zk.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createNode(String nodePath, String nodeData, CreateMode cm) {
		zookeeperClient.create(nodePath, nodeData, cm);
	}

	public static void main(String[] args) {
		MyServer2 hello = new MyServer2();
		hello.startZK("192.168.102.104:2181,192.168.102.106:2181,192.168.102.107:2181/queue-test", 200000);

		// 功能处理
		hello.createNode(PARENT_NODE + "/queueMember", "server2", CreateMode.EPHEMERAL_SEQUENTIAL);

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		hello.stopZK(zk);

	}
}
