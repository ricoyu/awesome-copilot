package com.awesomecopilot.common.lang.utils;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static com.awesomecopilot.common.lang.utils.IOUtils.DIR_SEPARATOR;
import static com.awesomecopilot.common.lang.utils.IOUtils.merge;
import static com.awesomecopilot.common.lang.utils.IOUtils.readClassPathFileAsString;
import static com.awesomecopilot.common.lang.utils.IOUtils.readFileAsBytes;
import static com.awesomecopilot.common.lang.utils.IOUtils.readFileAsString;
import static com.awesomecopilot.common.lang.utils.IOUtils.write;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * Copyright: (C), 2020-08-21 14:10
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class IOUtilsTest {
	
	@Test
	public void testLineSeparator() {
		String lineSeparator = IOUtils.LINE_SEPARATOR;
		System.out.println(lineSeparator);
	}
	
	@Test
	public void testReadClasspathFile() {
		String content = readClassPathFileAsString("application.yml");
		String content2 = readClassPathFileAsString("classpath:application.yml");
		assertEquals(content, content2);
	}
	
	@Test
	public void testReadFromWorkDir() {
		String workDir = System.getProperty("user.dir");
		String content = readFileAsString(workDir + "/application.yml");
		System.out.println(content);
	}
	
	@Test
	public void testReadFromFileSystem() {
		String content = readFileAsString("D:\\Learning\\awesome-copilot\\commons-lang\\application.yml");
		System.out.println(content);
	}
	
	@Test
	public void testFileSeparator() {
		System.out.println(DIR_SEPARATOR);
		System.out.println(readClassPathFileAsString("application.yml"));;
		System.out.println(readFileAsString("D:\\Dropbox\\doc\\bw.txt"));;
	}
	
	@Test
	public void testReadParts(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
		//2026-09-21 改造: 原来读某台机器手工准备的绝对路径 data.txt, 换机器必失败;
		//现在用 JUnit 临时目录自建数据, 并补真断言(原方法只打印/写文件, 没有任何校验)
		java.nio.file.Path src = tempDir.resolve("data.txt");
		byte[] payload = new byte[4096];
		for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i % 127);
		java.nio.file.Files.write(src, payload);

		byte[] head = readFileAsBytes(src.toString(), 0, 1024);
		assertEquals(1024, head.length, "按 offset/length 读取应恰好返回 1024 字节");
		assertEquals(payload[0], head[0]);
		assertEquals(payload[1023], head[1023]);
	}

	@Test
	public void testMerge(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
		//同上: 原读 /home/ricoyu/... 在 Windows 上不存在; 改为自建两个分片合并后校验内容
		java.nio.file.Path part0 = tempDir.resolve("data.txt.part0");
		java.nio.file.Path part1 = tempDir.resolve("data.txt.part1");
		java.nio.file.Files.write(part0, "AAA".getBytes("UTF-8"));
		java.nio.file.Files.write(part1, "BBB".getBytes("UTF-8"));
		java.nio.file.Path dest = tempDir.resolve("data-back.txt");

		merge(dest.toString(), part0.toString(), part1.toString());
		assertEquals("AAABBB", new String(java.nio.file.Files.readAllBytes(dest), "UTF-8"));
	}

	@Test
	public void testReadClasspathFileAsStr() {
		String str = readClassPathFileAsString("invalidJson.txt");
		System.out.println(str);
	}
}
