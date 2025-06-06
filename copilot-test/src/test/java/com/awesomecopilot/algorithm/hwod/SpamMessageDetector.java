package com.awesomecopilot.algorithm.hwod;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 垃圾短信识别
 * <p>
 * 大众对垃圾短信深恶痛绝，希望能对垃圾短信发送者进行识别，为此，很多软件增加了垃圾短信的识别机制。经分析，发现正常用户的短信通常具备交与性，而垃圾短信往往都是大量单向的短信，按照如下规则进行垃圾短信识别:
 * <p>
 * 本题中，发送者A符合以下条件之一的，则认为A是垃圾短信发送者:
 * <p>
 * A发送短信的接收者中，没有发过短信给A的人数L>5; <br/>
 * A发送的短信数-A接收的短信数M>10; <br/>
 * 如果存在X，A发送给X的短信数-A接收到X的短信数N>5; <br/>
 * <p>
 * <p>
 * 输入描述
 * <p>
 * 第一行是条目数，接下来几行是具体的条目，每个条目，是一对ID，第一个数字是发送者ID，后面的数字是接收者ID，中间空格隔开，所有的ID都为无符号整型，ID最大值为100；同一个条目中，两个ID不会相同(即不会自己给自己发消息)
 * <p>
 * 最后一行为指定的ID
 * <p>
 * 输出描述
 * <p>
 * 输出该ID是否为垃圾短信发送者，并且按序列输出LM的值(由于值不唯一，不需要输出);输出均为字符串。
 * <pre>
 * 1、输入
 * 15
 * 1 2
 * 1 3
 * 1 4
 * 1 5
 * 1 6
 * 1 7
 * 1 8
 * 1 9
 * 1 10
 * 1 11
 * 1 12
 * 1 13
 * 1 14
 * 14 1
 * 1 15
 * 1
 *
 * 2、输出
 * true 13 13
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-20 9:11
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SpamMessageDetector {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入条目数: ");
		int entryCount = scanner.nextInt();

		// 记录每个发送者发送给每个接收者的短信数量 发送者 -> 接收者 -> 发送次数
		Map<Integer, Map<Integer, Integer>> sendCount = new HashMap<>();

		// 记录每个接收者接收到每个发送者的短信数量 接收者 -> 发送者 -> 接收次数
		Map<Integer, Map<Integer, Integer>> receiveCount = new HashMap<>();
		scanner.nextLine();

		for (int i = 0; i < entryCount; i++) {
			System.out.print("请输入第 "+(i+1)+" 个条目: ");
			String[] parts = scanner.nextLine().trim().split(" ");
			int sender = Integer.parseInt(parts[0].trim());
			int receiver = Integer.parseInt(parts[1].trim());

			// 更新发送者的发送记录
			Map<Integer, Integer> senderValueMap = sendCount.computeIfAbsent(sender, k -> new HashMap<>());
			senderValueMap.put(receiver, senderValueMap.getOrDefault(receiver, 0) + 1);

			Map<Integer, Integer> receiverValueMap = receiveCount.computeIfAbsent(receiver, k -> new HashMap<>());
			receiverValueMap.put(sender, receiverValueMap.getOrDefault(sender, 0) + 1);
		}

		int targetId = scanner.nextInt();

		// 计算L：A发送的接收者中没有给A发过短信的人数
		int L = 0;
		if (sendCount.containsKey(targetId)) {
			for (Integer receiver : sendCount.get(targetId).keySet()) {
				// 检查receiver是否给targetId发过短信
				if (!receiveCount.containsKey(targetId) || !receiveCount.get(targetId).containsKey(receiver)) {
					L++;
				}
			}
		}

		// 计算M：A发送的短信总数 - A接收的短信总数
		int sendTotal =
				sendCount.getOrDefault(targetId, new HashMap<>()).values().stream().mapToInt(Integer::intValue).sum();
		int receiveTotal =
				receiveCount.getOrDefault(targetId, new HashMap<>()).values().stream().mapToInt(Integer::intValue).sum();
		int M = sendTotal - receiveTotal;

		// 计算N：是否存在X，使得A发送给X的短信数 - A接收到X的短信数 > 5
		boolean hasN = false;
		if (sendCount.containsKey(targetId)) {
			for (Map.Entry<Integer, Integer> entry : sendCount.get(targetId).entrySet()) {
				int x = entry.getKey();
				int sendToX = entry.getValue();
				int receiveFromX = receiveCount.getOrDefault(targetId, new HashMap<>()).getOrDefault(x, 0);
				if (sendToX - receiveFromX > 5) {
					hasN = true;
					break;
				}
			}
		}

		// 判断是否为垃圾短信发送者
		boolean isSpam = (L > 5) || (M > 10) || hasN;
		System.out.println(isSpam + " " + L + " " + M);
	}
}
