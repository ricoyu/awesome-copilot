package com.awesomecopilot.algorithm.hwod;

import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * IPv4地址转换成整数
 * <p>
 * 一、题目描述
 * <p>
 * 存在一种虚拟 IPv4 地址，由4小节组成，每节的范围为0~255，以#号间隔，虚拟 IPv4 地址可以转换为一个32位的整数，例如： 128#0#255#255，转换为32位整数的结果为2147549183（0x8000FFFF） 1#0#0#0，转换为 32 位整数的结果为16777216（0x01000000） 现以字符串形式给出一个虚拟 IPv4 地址，限制第1小节的范围为1~128， 即每一节范围分别为(1~128)#(0~255)#(0~255)#(0~255)， 要求每个 IPv4 地址只能对应到唯一的整数上。 如果是非法 IPv4，返回invalid IP
 * <p>
 * 备注： 输入不能确保是合法的 IPv4 地址， 需要对非法 IPv4（空串，含有 IP 地址中不存在的字符， 非合法的#分十进制，十进制整数不在合法区间内）进行识别，返回特定错误
 * <p>
 * 二、输入描述 <p>
 * 输入一行，虚拟 IPv4 地址格式字符串。
 * <p>
 * 三、输出描述
 * 输出以上，按照要求输出整型或者特定字符。
 * <pre>
 * 测试用例1
 * 1、输入
 * 128#0#255#255
 *
 * 2、输出
 * 2147549183
 * </pre>
 *<pre>
 * 测试用例2
 * 1、输入
 * 1#0#0#0
 *
 * 2、输出
 * 16777216
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-06-16 8:38
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class VirtualIPv4Converter {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入虚拟 IPv4 地址, 输入exit退出: ");
		String input = scanner.nextLine().trim();
		while (isNotBlank(input)) {
			if ("exit".equalsIgnoreCase(input)) {
				break;
			}
			System.out.println(convertToInteger(input));
			System.out.print("请输入虚拟 IPv4 地址, 输入exit退出: ");
			input = scanner.nextLine().trim();
		}
	}

	public static String convertToInteger(String ip) {
		// 判空
		if (isBlank(ip)) {
			return "invalid IP";
		}

		// 用 # 分割地址段
		String[] parts = ip.split("#");
		if (parts.length != 4) {
			return "invalid IP";
		}

		long[] nums = new long[4];
		for (int i = 0; i < parts.length; i++) {
			try {
				nums[i] = Long.parseLong(parts[i].trim());
			}catch (NumberFormatException e) {
				return "invalid IP";
			}
		}

		// 检查每段是否合法
		if (nums[0] <1 || nums[0] > 128) {
			return "invalid IP";
		}
		if (nums[1] <0 || nums[1] > 255) {
			return "invalid IP";
		}
		if (nums[2] <0 || nums[2] > 255) {
			return "invalid IP";
		}
		if (nums[3] <0 || nums[3] > 255) {
			return "invalid IP";
		}

		long result = nums[0] << 24 | nums[1] << 16 | nums[2] << 8 | nums[3];
		return Long.toString(result);
	}
}
