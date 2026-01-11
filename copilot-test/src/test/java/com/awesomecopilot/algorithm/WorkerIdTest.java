package com.awesomecopilot.algorithm;

import com.awesomecopilot.common.lang.utils.WorkerIdGenerator;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkerIdTest {

	@Test
	public void testgenerateWorkerIdById() {
		int workerId = WorkerIdGenerator.generateWorkerIdFromIp();
		for (int i = 0; i < 10000; i++) {
		    int newWorkerId = WorkerIdGenerator.generateWorkerIdFromIp();
			assertEquals(workerId, newWorkerId);
			workerId = newWorkerId;
			System.out.println(workerId);
		}
	}

	private static int generateWorkerIdFromIp() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface nif = interfaces.nextElement();
				if (nif.isLoopback() || nif.isVirtual() || !nif.isUp()) continue;

				Enumeration<InetAddress> addresses = nif.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;

					byte[] ipBytes = addr.getAddress();
					// 取IP地址的最后两个字节（IPv4最后两段，IPv6也适用）
					int hash = 0;
					for (int i = Math.max(0, ipBytes.length - 2); i < ipBytes.length; i++) {
						hash = 31 * hash + (ipBytes[i] & 0xFF);
					}
					return Math.abs(hash) % 32;  // 32 = 1<<5，确保0~31
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return 1; // fallback
	}
}
