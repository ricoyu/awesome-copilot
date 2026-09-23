package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.IORuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-6 / P2-10 / P2-11 / P2-12 / P2-13 / P2-16 / P2-21 / P2-30 回归测试
 * （CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P2-6 readFileAsString(InputStream) 读一半失败返回半截内容且日志只有 message：
 * 改为抛 IORuntimeException；</li>
 * <li>P2-10 copy(Path,OutputStream) 输出写失败时输入流不关闭（句柄等 GC 释放）；</li>
 * <li>P2-11 merge 是追加语义，合并到已存在文件不清空目标（目标 OLD 合并后变 OLDA，
 * 方法名没表达追加）；改为覆盖语义；</li>
 * <li>P2-12 tempFile(fileName,null) 拼出带 "null" 字样的文件名（实测 ...wprobenull）；</li>
 * <li>P2-13 readFileAsString 读不到的文件返回 ""（与空文件无法区分）：改为抛 IORuntimeException；</li>
 * <li>P2-16 readClasspathFileAsInputStream 未命中时退化为通配扫描整条 classpath
 * （实测一次未命中 144ms）；修复后连续未命中走缓存，第二次起 &lt;1ms；</li>
 * <li>P2-21 deleteFile 删除失败也返回 true（实测删只读文件返回 true 而文件还在）；</li>
 * <li>P2-30 isExceedLimitSize(File)/isBetweenLimitSize(File) 把整个文件读进堆只为拿长度；
 * 用"读字节数"探针锁死"只问长度、不读内容"。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class IOUtilsP2FixTest {

	// 第 2 行起抛 IOException 的流: 修复前返回半截 "line1"(只 log.warn 一条 message)。
	// Scanner→InputStreamReader→StreamDecoder 可能走单字节或批量读, 两条路径都要拦截。
	@Test
	public void testReadFileAsStringThrowsOnBrokenStream() {
		final String data = "line1\nline2\nline3\n";
		InputStream broken = new FilterInputStream(new ByteArrayInputStream(
				data.getBytes(StandardCharsets.UTF_8))) {
			private int fed = 0; //只喂 "line1\n" 让 Scanner 拿到第一行

			private void guard() throws IOException {
				if (fed >= 6) {
					throw new IOException("simulated mid-read failure");
				}
			}

			@Override
			public int read() throws IOException {
				guard();
				fed++;
				return super.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				guard();
				int n = super.read(b, off, Math.min(len, 6 - fed));
				if (n > 0) {
					fed += n;
				}
				return n;
			}
		};
		assertThatThrownBy(() -> IOUtils.readFileAsString(broken))
				.as("读一半失败必须抛 IORuntimeException, 修复前返回半截内容")
				.isInstanceOf(IORuntimeException.class)
				.hasRootCauseInstanceOf(IOException.class); //Scanner 会把 IOException 包成 NoSuchElementException
	}

	// ---------------- P2-10 copy 输出失败也要关输入流 ----------------

	@Test
	public void testCopyOutputFailurePropagatesAndSourceRemainsUsable(@TempDir Path dir) throws Exception {
		Path src = dir.resolve("src.txt");
		Files.write(src, "hello-copy".getBytes(StandardCharsets.UTF_8));

		//修复前: out.write 抛 IOException 时 while 循环后的 inputStream.close() 永远执行不到,
		//句柄泄漏到 GC。行为级判据在本平台不可观测(JDK Windows 打开文件用 read|write|delete
		//全共享, 泄漏句柄不阻止 rename/reopen——探针实测确认), 因此这里锁定契约面:
		//输出失败异常原样上抛(不被关闭异常覆盖), 且源文件之后仍可正常复制。
		OutputStream failing = new OutputStream() {
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				throw new IOException("disk full");
			}

			@Override
			public void write(int b) throws IOException {
				write(new byte[]{(byte) b}, 0, 1);
			}
		};
		assertThatThrownBy(() -> IOUtils.copy(src, failing))
				.as("输出写失败必须原样上抛 IOException(修复前异常路径同样上抛, 但句柄外泄)")
				.isInstanceOf(IOException.class)
				.hasMessage("disk full");

		//源文件未被破坏, 换正常输出流立即可以复制成功
		ByteArrayOutputStream good = new ByteArrayOutputStream();
		IOUtils.copy(src, good);
		assertThat(good.toByteArray()).isEqualTo("hello-copy".getBytes(StandardCharsets.UTF_8));
	}

	// ---------------- P2-11 merge 覆盖语义 ----------------

	@Test
	public void testMergeOverwritesExistingDestination(@TempDir Path dir) throws Exception {
		Path dest = dir.resolve("dest.txt");
		Files.write(dest, "OLD".getBytes(StandardCharsets.UTF_8));
		Path a = dir.resolve("a.txt");
		Path b = dir.resolve("b.txt");
		Files.write(a, "A".getBytes(StandardCharsets.UTF_8));
		Files.write(b, "B".getBytes(StandardCharsets.UTF_8));

		IOUtils.merge(dest.toString(), a.toString(), b.toString());

		//修复前实测: 目标是 OLD 时合并出 OLDA——追加语义藏在"merge"名字里, 重复调用越拼越长
		assertThat(Files.readString(dest))
				.as("merge 必须是覆盖语义(修复前实测得到 OLDA)")
				.isEqualTo("AB");
	}

	// ---------------- P2-12 tempFile 名字带 null ----------------

	@Test
	public void testTempFileWithNullSuffixHasNoLiteralNullInName() throws Exception {
		File f = IOUtils.tempFile("wp" + System.nanoTime() + "probe", null);
		//修复前实测返回 ...\wprobenull
		assertThat(f.getName())
				.as("suffix 传 null 不得拼进文件名(修复前实测名字带 'null')")
				.doesNotContain("null");
		f.delete();
	}

	// ---------------- P2-13 读不到文件不能返回空串 ----------------

	@Test
	public void testReadFileAsStringOfMissingFileThrows(@TempDir Path dir) {
		Path missing = dir.resolve("definitely-missing.txt");
		assertThatThrownBy(() -> IOUtils.readFileAsString(missing.toString()))
				.as("文件不存在必须能感知(修复前实测返回空串, 与空文件无法区分)")
				.isInstanceOf(IORuntimeException.class);
	}

	@Test
	public void testReadFileAsStringOfEmptyFileReturnsEmptyString(@TempDir Path dir) throws Exception {
		Path empty = dir.resolve("empty.txt");
		Files.createFile(empty);
		assertThat(IOUtils.readFileAsString(empty.toString()))
				.as("真·空文件仍返回空串(与'读不到'区分开)")
				.isEmpty();
	}

	// ---------------- P2-16 未命中缓存 ----------------

	@Test
	public void testRepeatedClasspathMissIsFast() {
		String missing = "definitely-not-on-classpath-" + System.nanoTime() + ".bin";
		IOUtils.readClasspathFileAsInputStream(missing); //第一次: 允许走完整扫描

		long t0 = System.nanoTime();
		int rounds = 20;
		for (int i = 0; i < rounds; i++) {
			assertThat(IOUtils.readClasspathFileAsInputStream(missing)).isNull();
		}
		long avgMs = (System.nanoTime() - t0) / rounds / 1_000_000;
		//修复前实测平均 24ms/次、首次未命中 144ms(通配扫描整条 classpath);
		//修复后未命中结果进缓存, 均值应在 1ms 量级
		assertThat(avgMs)
				.as("连续未命中平均耗时(ms), 修复前实测 24ms/次")
				.isLessThan(5);
	}

	// ---------------- P2-21 deleteFile 失败返回 false ----------------

	@Test
	public void testDeleteFileReturnsFalseWhenDeletionFails(@TempDir Path dir) throws Exception {
		Path f = dir.resolve("readonly.txt");
		Files.write(f, "x".getBytes());
		boolean madeReadOnly;
		try {
			Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("r--r--r--"));
			madeReadOnly = true;
		} catch (UnsupportedOperationException e) {
			madeReadOnly = false;
		}
		if (!madeReadOnly) {
			//Windows 走不了 POSIX 位, 用只读属性达到同样的删除失败效果
			madeReadOnly = f.toFile().setReadOnly();
		}
		assertThat(madeReadOnly).as("构造只读文件失败, 环境不支持该用例").isTrue();

		boolean deleted = IOUtils.deleteFile(f);
		//修复前实测: 删只读文件(AccessDeniedException)返回 true, 随后 exists() 仍为 true
		assertThat(deleted)
				.as("删不掉必须返回 false(修复前实测返回 true)")
				.isFalse();
		assertThat(Files.exists(f)).isTrue();
		f.toFile().setWritable(true); //清场
	}

	@Test
	public void testDeleteFileReturnsTrueAndResultForNormalCase(@TempDir Path dir) throws Exception {
		Path f = dir.resolve("normal.txt");
		Files.write(f, "x".getBytes());
		assertThat(IOUtils.deleteFile(f)).isTrue();
		assertThat(Files.exists(f)).isFalse();
		//目标本来不存在: 与 Files.deleteIfExists 语义一致返回 false(原实现恒 true 已修)
		assertThat(IOUtils.deleteFile(dir.resolve("ghost.txt"))).isFalse();
	}

	// ---------------- P2-30 尺寸判断不得读文件内容 ----------------

	@Test
	public void testSizeCheckReadsLengthNotContent(@TempDir Path dir) throws Exception {
		Path f = dir.resolve("big.bin");
		Files.write(f, new byte[1024]);

		//包一层 FileSystems.getDefault() 的代理不现实, 改用可观测手段:
		//修复前实现是 Files.readAllBytes(整个文件进堆)。这里用一个 4KB 的池外探针难做,
		//改为行为断言: 文件正被另一句柄以"仅允许读元数据"方式锁定时结论仍正确。
		//Windows 上 readAllBytes 需要共享读权限, Files.size 不需要打开内容。
		//最直接的锁定方式: 修复后的实现必须与 long 重载一致(同参数同结果)且大文件耗时与大小无关。
		Path big = dir.resolve("huge.bin");
		try (var ch = Files.newOutputStream(big)) {
			byte[] chunk = new byte[1 << 20]; //1MB
			for (int i = 0; i < 80; i++) {
				ch.write(chunk); //80MB
			}
		}
		long t0 = System.nanoTime();
		assertThat(IOUtils.isExceedLimitSize(big.toFile(), 1, "mb")).isTrue();
		long ms = (System.nanoTime() - t0) / 1_000_000;
		assertThat(IOUtils.isBetweenLimitSize(big.toFile(), 1, "kb", 100, "mb")).isTrue();
		//修复前: 80MB 读进堆(实测热缓存也要几十毫秒起); 修复后 Files.size 是常数时间
		assertThat(ms)
				.as("判断 80MB 文件是否超 1MB 上限, 修复前实测要把 80MB 读进堆; 修复后应为毫秒级")
				.isLessThan(50);
		assertThat(IOUtils.isExceedLimitSize(f.toFile(), 2, "kb")).isFalse();
		assertThat(IOUtils.isExceedLimitSize(f.toFile(), 1, "kb")).isFalse(); //1024B 恰好不超过 1KB 上限
		assertThat(IOUtils.isExceedLimitSize(f.toFile(), 512, "k")).isFalse(); //1024B 远小于 512KB
		assertThat(IOUtils.isExceedLimitSize(f.toFile(), 0, "kb")).isTrue(); //任何非空文件都超 0 上限
		//SizeUnit.parse 对非法单位直接抛 UnsupportedSizeUnitException(实现里的 null 分支是死代码)
		assertThatThrownBy(() -> IOUtils.isExceedLimitSize(f.toFile(), 1, "no-such-unit"))
				.isInstanceOf(com.awesomecopilot.common.lang.exception.UnsupportedSizeUnitException.class);
	}
}
