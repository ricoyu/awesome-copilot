package com.awesomecopilot.codec;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2021-02-01 17:56
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HexUtilsTest {
	
	
	@Test
	public void test() {
		System.out.println(HexUtils.hex2Binary("0015"));
	}
	
	@Test
	public void testHexToBinary() {
		String ipv6 = "2001:0db8:3c4d:0015";
		asList(ipv6.split(":"))
				.forEach((hex) -> {
					String binaryString = HexUtils.hex2Binary(hex);
					System.out.println(binaryString);
				});
	}
	
	@Test
	public void testBinary2Ten() {
		System.out.println(HexUtils.hexToInteger("11111111"));
		System.out.println(HexUtils.hexToInteger("1111"));
	}
	
	@Test
	public void testHexToIntegerWith0xPrefix() {
		//"0x1A" 正常解析为 26
		assertEquals(Integer.valueOf(26), HexUtils.hexToInteger("0x1A"));
		//"0X1A" 大写前缀同样解析为 26
		assertEquals(Integer.valueOf(26), HexUtils.hexToInteger("0X1A"));
		//无前缀的 "1A" 仍解析为 26
		assertEquals(Integer.valueOf(26), HexUtils.hexToInteger("1A"));
		//"0x" 移除前缀后为空, 返回 null 而非抛 NumberFormatException
		assertNull(HexUtils.hexToInteger("0x"));
		//"120x3" 中间的 "0x" 不应被当作前缀移除; 该串非法, 抛 NumberFormatException 且异常信息保留完整原始输入
		NumberFormatException e = assertThrows(NumberFormatException.class, () -> HexUtils.hexToInteger("120x3"));
		assertTrue(e.getMessage().contains("120x3"), "中间的 0x 不应被移除, 异常信息应包含完整原始输入 120x3");
	}
	
	@Test
	public void testHexToLongWith0xPrefix() {
		//"0x1A" 正常解析为 26
		assertEquals(Long.valueOf(26L), HexUtils.hexToLong("0x1A"));
		//"0X1A" 大写前缀同样解析为 26
		assertEquals(Long.valueOf(26L), HexUtils.hexToLong("0X1A"));
		//无前缀的 "1A" 仍解析为 26
		assertEquals(Long.valueOf(26L), HexUtils.hexToLong("1A"));
		//"0x" 移除前缀后为空, 返回 null 而非抛 NumberFormatException
		assertNull(HexUtils.hexToLong("0x"));
		//"120x3" 中间的 "0x" 不应被当作前缀移除
		NumberFormatException e = assertThrows(NumberFormatException.class, () -> HexUtils.hexToLong("120x3"));
		assertTrue(e.getMessage().contains("120x3"), "中间的 0x 不应被移除, 异常信息应包含完整原始输入 120x3");
	}
	
	@Test
	public void testStringToHexZeroPadding() {
		//小于 0x10 的字符应补零为 2 位, 避免只输出 1 位产生奇数长度/歧义
		assertEquals("0a", HexUtils.stringToHex("\n"));
		assertEquals("09", HexUtils.stringToHex("\t"));
		assertEquals("00", HexUtils.stringToHex("\0"));
		//普通 ASCII 字符仍为 2 位
		assertEquals("41", HexUtils.stringToHex("A"));
		//多字符拼接后长度必为偶数, 且能被 hexToString 无损还原
		String src = "A\nB\tC";
		String hex = HexUtils.stringToHex(src);
		assertEquals(src.length() * 2, hex.length());
		assertEquals(src, HexUtils.hexToString(hex));
	}
}