package com.awesomecopilot.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RedixUtils 单元测试
 * <p>
 * 重点验证 byte2Hex 对高位为 1 (即负 byte) 的处理: 必须始终输出 2 位十六进制,
 * 不能因符号扩展产生 "FFFFFFFF" 之类的越界结果。
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class RedixUtilsTest {
	
	@Test
	public void testByte2Hex() {
		//边界与代表值: 0x00 / 0x0A / 0x7F / 0x80 / 0xFF
		assertEquals("00", RedixUtils.byte2Hex((byte) 0x00));
		assertEquals("0A", RedixUtils.byte2Hex((byte) 0x0A));
		assertEquals("7F", RedixUtils.byte2Hex((byte) 0x7F));
		assertEquals("80", RedixUtils.byte2Hex((byte) 0x80));
		assertEquals("FF", RedixUtils.byte2Hex((byte) 0xFF));
		//-1 与 (byte) 0xFF 是同一个 byte 值, 应输出 "FF" 而非 "FFFFFFFF"
		assertEquals("FF", RedixUtils.byte2Hex((byte) -1));
	}
	
	@Test
	public void testByte2HexAllValuesAlwaysTwoDigits() {
		//遍历全部 256 个 byte 取值, 确保输出恒为 2 位且与无符号掩码结果一致, 并与 bytes2Hex 交叉校验
		for (int v = 0; v < 256; v++) {
			byte b = (byte) v;
			String hex = RedixUtils.byte2Hex(b);
			assertEquals(2, hex.length(), "byte2Hex 应始终输出 2 位十六进制, v=" + v);
			assertEquals(String.format("%02X", v), hex, "无符号值格式化结果应与 byte2Hex 一致, v=" + v);
			assertEquals(hex, RedixUtils.bytes2Hex(new byte[]{b}).toUpperCase(), "byte2Hex 应与 bytes2Hex 一致, v=" + v);
		}
	}
}
