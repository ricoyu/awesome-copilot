package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

/**
 * 日志首次上报最多积分
 * <p>
 * 一、题目描述
 * <p>
 * 日志采集是运维系统的核心组件。日志是按行生成，每行记录做一条，由采集系统分批上报。
 * <p>
 * 如果上报太频繁，会对服务端造成压力； <p>
 * 如果上报太晚，会降低用户的体验； <p>
 * 如果一次上报的条数太多，会导致接口失败。 <p>
 * <p>
 * 为此，项目组设计了如下的上报策略：
 * <p>
 * 每成功上报一条日志，奖励1分； <p>
 * 每条日志延迟上报1秒，扣1分； <p>
 * 积累日志达到100条，必须立刻上报。 <p>
 * 给出日志序列，根据该规则，计算首次上报能获得的最多积分数。 <p>
 * <p>
 * 二、输入描述 <p>
 * 按时序产生的日志条数 T1,T2,…Tn，其中 1 <= n <= 1000，0 <= Ti <= 100 <p>
 * <p>
 * 三、输出描述 <p>
 * 首次上报最多能获得的积分分数 <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * 1 98 1
 *
 * 2、输出
 * 98
 * </pre>
 *
 * <pre>
 * 测试用例2：
 * 1、输入
 * 50 60 1
 *
 * 2、输出
 * 50
 * </pre>
 *
 * <ul>测试用例1：输入 1 98 1
 *     <li/>时刻1：累计日志=1，上报1条，扣分=0（因为是在生成时刻立即上报），积分=1-0=1。
 *     <li/>时刻2：累计日志=1+98=99，上报99条，扣分=1 * 1（第1时刻的1条延迟1秒）=1，积分=99-1=98。
 *     <li/>时刻3：累计日志=1+98+1=100，必须上报100条。扣分=1 * 2（第1时刻的1条延迟2秒）+98 * 1（第2时刻的98条延迟1秒）=2+98=100，积分=100-100=0。
 * </ul>
 * <ul>测试用例2：输入 50 60 1
 *     <li/>时刻1：累计日志=50，上报50条，扣分=0，积分=50-0=50。
 *     <li/>时刻2：累计日志=50+60=110，上报100条（前50+后50，但后60只能取50）。扣分=50 * 1（第1时刻的50条延迟1秒）=50，积分=100-50=50。
 *     <li/>时刻3：累计日志=50+60+1=111，必须上报100条。扣分=50 * 2+60 * 1=100+60=160，积分=100-160=-60。
 *     <li/>最大积分是50（时刻1或时刻2上报）。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2025-05-19 8:22
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LogReportMaxScore {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入待挖掘的文本内容: ");
		String[] parts = scanner.nextLine().trim().split(" ");
		int[] logs = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			logs[i] = Integer.parseInt(parts[i]);
		}
		System.out.println(getMaxFirstUploadScore(logs));
	}

	public static int getMaxFirstUploadScore(int[] logs) {
		int maxScore = Integer.MIN_VALUE; // 初始化最大得分
		int totalLogs = 0;  // 当前累计日志数量

		// 枚举上报时刻：从第0秒到第n-1秒
		for (int i = 0; i < logs.length; i++) {
			totalLogs+=logs[i]; // 累加当前时刻日志

			// 每次最多只能上传100条日志
			int uploadCount = Math.min(100, totalLogs);

			// 每条日志延迟上报的时间是：当前时刻减去它生成的时间
			// 所以我们需要统计这些延迟带来的扣分
			int penalty = 0;
			int remaining = uploadCount;

			// 从当前时刻往前遍历日志生成数（优先上传最近的日志，延迟最短）
			for (int j = i; j >= 0 && remaining>0; j--) {
				int toUpload =  Math.min(remaining, logs[j]);
				penalty += toUpload * (i-j);// 延迟时间 * 上传条数
				remaining -= toUpload;
			}

			int score = uploadCount - penalty; // 总得分 = 成功上传数 - 延迟扣分
			maxScore = Math.max(maxScore, score);

			// 如果累计日志数已经 >= 100，就不能再等待了
			if (totalLogs >= 100) {
				break;
			}
		}
		return maxScore;
	}
	private static int calculateMaxScore(int[] logs) {
		int maxScore = 0;
		int totalLogs = 0; // 累计日志条数

		for (int i = 0; i < logs.length; i++) {
			totalLogs += logs[i];
			int reportLogs = Math.min(100, totalLogs);// 实际上报的日志条数
			int delayPenalty = 0; // 延迟扣分
			int temptotal = 0;

			// 计算扣分：遍历之前的所有时刻
			for (int j = 0; j < i; j++) {
				delayPenalty += logs[j];
				// 如果当前时刻i上报，那么j时刻的日志延迟(i-j)秒
				delayPenalty += logs[j] * (i - j);
			}

			// 如果累计日志超过100，前面的部分会被丢弃，只保留最后100条
			// 因此扣分需要重新计算（只计算最后100条对应的延迟）
			if (totalLogs > 100) {
				int overflows = totalLogs - 100;// 需要丢弃的日志条数
				delayPenalty = 0;
				temptotal = 0;

				// 从前往后丢弃overflow条日志
				for (int j = 0; j < i; j++) {
					if (overflows <= 0) {
						// 剩余的日志都保留，计算延迟
						delayPenalty += logs[j] * (i - j);
					} else {
						if (logs[j] <= overflows) {
							overflows -= logs[j];
						} else {
							delayPenalty += (logs[j] - overflows) * (i - j);
							overflows = 0;
						}
					}
				}

				// 当前时刻的日志也可能被部分丢弃
				if (overflows > 0) {
					reportLogs += logs[i] - overflows;
				}
			}

			int currentScore = reportLogs - delayPenalty;
			if (currentScore > maxScore) {
				maxScore = currentScore;
			}
			// 如果累计日志达到或超过100，必须立即上报，后续不再考虑
			if (totalLogs >= 100) {
				break;
			}
		}
		return maxScore;
	}
}
