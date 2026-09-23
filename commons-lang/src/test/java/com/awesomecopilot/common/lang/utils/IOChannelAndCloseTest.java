package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * IO 组回归测试（CODE_REVIEW_REPORT P1-11 / P1-12 / P1-13）。
 * <p>
 * P1-11 toByteArray(ByteChannel)：短读时把整个 1024 缓冲区原样返回（尾部 0 填充当数据），
 * 且只 read 一次，大文件必截断。
 * P1-12 write/createParentDir 对不带目录的相对路径抛 NPE（path.getParent() 为 null）。
 * P1-13 readAsString(in, false)：autoClose=false 却仍关闭了调用方的流
 * （包装器 BufferedInputStream 被放进 try-with-resources，关外层连带关内层）。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class IOChannelAndCloseTest {

	// ---------- P1-11 ----------

	/** 每次最多给 chunkSize 字节的通道, 模拟短读(IOUtils.toByteArray 形参是 ByteChannel) */
	private static java.nio.channels.ByteChannel chunkedChannel(byte[] data, int chunkSize) {
		final ByteArrayInputStream delegate = new ByteArrayInputStream(data);
		return new java.nio.channels.ByteChannel() {
			boolean closed = false;

			@Override
			public int read(ByteBuffer dst) {
				int len = Math.min(dst.remaining(), chunkSize);
				if (delegate.available() == 0) {
					return -1;
				}
				byte[] tmp = new byte[len];
				int n = delegate.read(tmp, 0, len);
				if (n <= 0) return -1;
				dst.put(tmp, 0, n);
				return n;
			}

			@Override
			public int write(ByteBuffer src) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isOpen() {
				return !closed;
			}

			@Override
			public void close() {
				closed = true;
			}
		};
	}

	@Test
	public void testToByteArrayFromChannelExactLength() throws IOException {
		byte[] payload = new byte[5];
		for (int i = 0; i < 5; i++) payload[i] = (byte) (i + 1);

		//修复前实测: 读 5 字节返回长度 1024, 尾部 1019 个 0 被当成数据
		byte[] result = IOUtils.toByteArray(chunkedChannel(payload, 5));
		assertThat(result).isEqualTo(payload);
	}

	@Test
	public void testToByteArrayFromChannelLargePayload() throws IOException {
		//修复前实测: 只 read 一次, 超过缓冲区的大文件必截断
		byte[] payload = new byte[5000];
		for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i % 251);

		byte[] result = IOUtils.toByteArray(chunkedChannel(payload, 100));
		assertThat(result).hasSize(5000);
		assertThat(result).isEqualTo(payload);
	}

	@Test
	public void testToByteArrayFromChannelStillWorksForFullBufferReads() throws IOException {
		//合法路径不能被改坏: 单次给满缓冲区(chunkSize>=1024)与跨多次大读取结果都正确
		byte[] payload = "hello channel".getBytes(StandardCharsets.UTF_8);
		assertThat(IOUtils.toByteArray(chunkedChannel(payload, 2048))).isEqualTo(payload);

		byte[] big = new byte[4096];
		for (int i = 0; i < big.length; i++) big[i] = (byte) i;
		//chunkSize=1024: 第一读恰好填满缓冲区, 后续进入多次读取累积分支
		assertThat(IOUtils.toByteArray(chunkedChannel(big, 1024))).isEqualTo(big);
	}

	// ---------- P1-12 ----------

	@Test
	public void testCreateParentDirOnBareFileNameDoesNotThrow() throws IOException {
		//切到临时目录当前工作路径下, 传不带目录的相对文件名: getParent() 为 null
		//修复前实测: NPE "Cannot invoke Path.getFileSystem() because path is null"
		Path bare = Path.of("out.txt");
		assertThat(bare.getParent()).isNull(); //前置条件: 确实是无父路径的相对名
		assertThatCode(() -> IOUtils.createParentDir(bare)).doesNotThrowAnyException();
		//评审修复后语义: 先 toAbsolutePath 再取父, 裸文件名的实际父目录是进程当前目录(存在) => true
		assertThat(IOUtils.createParentDir(bare)).isTrue();
	}

	@Test
	public void testWriteBareRelativePathDoesNotThrow() throws IOException {
		//write(Path,byte[]) 内部先调 createParentDir; 修复前不带目录的相对文件名直接 NPE, 根本走不到写
		java.nio.file.Path previousCwdHint = Path.of("").toAbsolutePath();
		Path relative = previousCwdHint.resolve("written-by-test.txt").getFileName(); //只有文件名一段
		try {
			boolean ok = IOUtils.write(relative, "DATA".getBytes(StandardCharsets.UTF_8));
			assertThat(ok).isTrue();
			//写到进程 cwd 下, 验证后删除
			Path actual = previousCwdHint.resolve("written-by-test.txt");
			if (Files.exists(actual)) {
				assertThat(new String(Files.readAllBytes(actual), StandardCharsets.UTF_8)).isEqualTo("DATA");
				Files.delete(actual);
			}
		} finally {
			Files.deleteIfExists(previousCwdHint.resolve("written-by-test.txt"));
		}
	}

	// ---------- P1-13 ----------

	private static class CloseCountingStream extends FilterInputStream {
		int closeCount = 0;

		CloseCountingStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			closeCount++;
			super.close();
		}
	}

	@Test
	public void testReadAsStringAutoCloseFalseKeepsStreamOpen() {
		byte[] data = "line1\nline2".getBytes(StandardCharsets.UTF_8);
		CloseCountingStream counting = new CloseCountingStream(new ByteArrayInputStream(data));

		String text = IOUtils.readAsString(counting, false);

		assertThat(text).isEqualTo("line1" + System.lineSeparator() + "line2");
		//修复前实测: closeCount==1 —— try-with-resources 关外层 BufferedInputStream 连带关了调用方的流
		assertThat(counting.closeCount)
				.as("autoClose=false 时不应关闭调用方传入的流")
				.isZero();
	}

	@Test
	public void testReadAsStringAutoCloseTrueClosesStream() {
		byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
		CloseCountingStream counting = new CloseCountingStream(new ByteArrayInputStream(data));

		String text = IOUtils.readAsString(counting, true);

		assertThat(text).isEqualTo("xyz");
		assertThat(counting.closeCount).as("autoClose=true 时应关闭调用方的流").isPositive();
	}
}
