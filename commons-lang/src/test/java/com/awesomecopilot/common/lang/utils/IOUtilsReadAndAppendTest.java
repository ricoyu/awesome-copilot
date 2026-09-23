package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-2 / P0-5 回归测试（CODE_REVIEW_REPORT 2026-09-22）。
 * <p>
 * P0-2：toByteArray(InputStream) 把"一次 read 没读满缓冲区"当成读完，
 * 慢速分段流（每次只给 10 字节的 Socket 类流）只拿到第一截且不报错。
 * <p>
 * P0-5：append(String, byte[]) 委托给 write(Path, byte[])（TRUNCATE_EXISTING），
 * 名义追加、实际覆盖，原文件内容丢失；同族 write(Path, String) 反而用 APPEND，
 * 两组方法语义相反。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class IOUtilsReadAndAppendTest {

	// ---------- P0-2: toByteArray 分段读取 ----------

	/**
	 * 模拟慢速 Socket：每次 read 最多返回 chunkSize 字节，即使流里还有很多数据。
	 * InputStream.read(byte[]) 的契约允许这种"没读满"的返回，调用方必须继续读到 -1。
	 */
	private static InputStream chunkedStream(byte[] data, int chunkSize) {
		final ByteArrayInputStream delegate = new ByteArrayInputStream(data);
		return new InputStream() {
			@Override
			public int read() {
				return delegate.read();
			}

			@Override
			public int read(byte[] b, int off, int len) {
				return delegate.read(b, off, Math.min(len, chunkSize));
			}
		};
	}

	@Test
	public void testToByteArrayReadsAllFromSlowChunkedStream() {
		byte[] payload = new byte[5000];
		for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i % 251);

		//修复前实测: 每次只给10字节的5000字节流, toByteArray 返回长度 10, 4990 字节丢失且不报错
		byte[] result = IOUtils.toByteArray(chunkedStream(payload, 10));

		assertThat(result).hasSize(5000);
		assertThat(result).isEqualTo(payload);
	}

	@Test
	public void testToByteArrayExactMultipleAndShortReads() {
		//数据长度恰好等于内部 1024 缓冲区的 3 倍，且每次只给 100 字节
		byte[] payload = "x".repeat(3072).getBytes(StandardCharsets.US_ASCII);
		byte[] result = IOUtils.toByteArray(chunkedStream(payload, 100));
		assertThat(result).isEqualTo(payload);
	}

	@Test
	public void testSingleReadStreamsStillCorrect() {
		//修复后已没有"读满/没读满"两条路径，这里钉住常见输入的结果正确性：
		//一次能读完的小流、恰好等于缓冲区大小的流、空流、null
		byte[] small = "hello".getBytes(StandardCharsets.UTF_8);
		assertThat(IOUtils.toByteArray(new ByteArrayInputStream(small))).isEqualTo(small);

		byte[] exact = new byte[1024]; //恰好等于 MIN_BUFFER_SIZE, 第一次读满, 第二次才 EOF
		for (int i = 0; i < exact.length; i++) exact[i] = (byte) i;
		assertThat(IOUtils.toByteArray(new ByteArrayInputStream(exact))).isEqualTo(exact);

		assertThat(IOUtils.toByteArray(new ByteArrayInputStream(new byte[0]))).isEmpty();
		//null 同时匹配 InputStream/ByteChannel 两个重载, 必须显式转型
		assertThat(IOUtils.toByteArray((java.io.InputStream) null)).isEmpty();
	}

	// ---------- P0-5: append / write 语义 ----------

	@Test
	public void testAppendStringPathKeepsExistingContent(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("append-bytes.log");
		Files.write(file, "AAA".getBytes(StandardCharsets.UTF_8));

		//修复前实测: 调 append 后文件内容变成 BBB, 原来的 AAA 没了(内部走 TRUNCATE_EXISTING)
		boolean ok = IOUtils.append(file.toString(), "BBB".getBytes(StandardCharsets.UTF_8));

		assertThat(ok).isTrue();
		assertThat(Files.readString(file)).isEqualTo("AAABBB");
	}

	@Test
	public void testAppendCreatesFileWhenMissing(@org.junit.jupiter.api.io.TempDir Path tempDir) {
		Path file = tempDir.resolve("brand-new.log");

		assertThat(IOUtils.append(file, "NEW".getBytes(StandardCharsets.UTF_8))).isTrue();
		assertThat(file).exists();
	}

	@Test
	public void testWriteStringDataOverwritesExistingFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("write-string.txt");
		Files.write(file, "OLD".getBytes(StandardCharsets.UTF_8));

		//修复前实测: write(Path,String) 用 APPEND, 写完变成 "OLDHELLO" —— 名字叫 write 却是追加
		assertThat(IOUtils.write(file, "HELLO")).isTrue();
		assertThat(Files.readString(file)).isEqualTo("HELLO");
	}

	@Test
	public void testWriteCharsetOverwritesExistingFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("write-string-gbk.txt");
		Files.write(file, "OLD".getBytes(StandardCharsets.UTF_8));

		assertThat(IOUtils.write(file, "你好", StandardCharsets.UTF_8)).isTrue();
		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("你好");
	}

	@Test
	public void testWriteStringStillCreatesFile(@org.junit.jupiter.api.io.TempDir Path tempDir) {
		Path file = tempDir.resolve("fresh.txt");
		assertThat(IOUtils.write(file, "FRESH")).isTrue();
		assertThat(file).exists();
	}
}
