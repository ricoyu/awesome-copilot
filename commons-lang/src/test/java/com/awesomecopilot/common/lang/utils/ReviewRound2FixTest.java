package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.ApplicationException;
import com.awesomecopilot.common.lang.exception.UnsupportedLocalDateFormatException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 评审发现处置测试（2026-09-23，三个独立评审子代理的属本轮范围发现）。
 * 覆盖：
 * <ul>
 * <li>评审1-P1a：显式 format 的 parse 入口同样存在 S 读整段数字的问题（SSS 挡不住 .456789），
 * 5 个入口接入 normalizeFractionalSeconds；</li>
 * <li>评审1-P1b：dash 家族修活后越界输入（13-45-2020）的异常类型——toLocalDate 保持与修复前
 * 一致的 UnsupportedLocalDateFormatException，不再是原生 DateTimeParseException；</li>
 * <li>评审1-P2：DateFormatterHolder 默认入口显式 CHINA（javadoc 承诺），与 SimpleDateFormatHolder 一致；</li>
 * <li>评审2-P2：toByteArray(ByteChannel) 对恒返回 0 的通道抛 IOException，不再永久自旋；</li>
 * <li>评审2-P2：createParentDir 对不带目录的相对文件名返回 true（实际父目录=cwd 存在），copy 不再无声失败；</li>
 * <li>评审3-P2：toBigDecimal(Float) 不被 Double 二进制噪声污染（1.1f→1.1 而非 1.100000023841858）；</li>
 * <li>评审3-P2：ApplicationException(无消息cause) 的 getMessage() 不再为 null。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ReviewRound2FixTest {

	// ---------- 评审1-P1a: 显式 format 入口 ----------

	@Test
	public void testExplicitFormatEntryNormalizesFractionalSeconds() {
		//修复前实测(评审探针): parse("...30.456789", "yyyy-MM-dd HH:mm:ss.SSS") -> 10:28:06.789(错 456.789-0.789=456s)
		java.util.Date d = DateUtils.parse("2026-09-22 10:20:30.456789", "yyyy-MM-dd HH:mm:ss.SSS");
		assertThat(d.getTime()).isEqualTo(
				DateUtils.parse("2026-09-22 10:20:30.456", "yyyy-MM-dd HH:mm:ss.SSS").getTime());
	}

	@Test
	public void testExplicitFormatWithLocaleAndZoneEntriesNormalized() {
		java.util.Date a = DateUtils.parse("2026-09-22 10:20:30.45", "yyyy-MM-dd HH:mm:ss.SSS",
				TimeZone.getTimeZone("Asia/Shanghai"));
		java.util.Date b = DateUtils.parse("2026-09-22 10:20:30.450", "yyyy-MM-dd HH:mm:ss.SSS",
				TimeZone.getTimeZone("Asia/Shanghai"));
		assertThat(a).isEqualTo(b);

		java.util.Date c = DateUtils.parse("2026-09-22 10:20:30.4567", "yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
		java.util.Date d = DateUtils.parse("2026-09-22 10:20:30.456", "yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
		assertThat(c).isEqualTo(d);
	}

	// ---------- 评审1-P1b: dash 家族越界异常类型 ----------

	@Test
	public void testEnDashOutOfRangeThrowsDomainException() {
		//修复前(HEAD)正则不匹配, 走到方法末尾抛 UnsupportedLocalDateFormatException;
		//P1-16 修活后一度变成直接调 LocalDate.parse, 由它抛原生 DateTimeParseException(copilot-json 反序列化链会转成
		//无信息量的 UnsupportedOperationException), 本用例锁回域异常
		assertThatThrownBy(() -> DateUtils.toLocalDate("13-45-2020"))
				.isInstanceOf(UnsupportedLocalDateFormatException.class);
		//合法值正常解析
		assertThat(DateUtils.toLocalDate("12-25-2020").toString()).isEqualTo("2020-12-25");
	}

	// ---------- 评审1-P2: DTF 默认入口显式 CHINA ----------

	@Test
	public void testDtfDefaultEntryIsChinaRegardlessOfJvmLocale() {
		Locale saved = Locale.getDefault();
		try {
			Locale.setDefault(Locale.FRANCE);
			//修复前实测: 默认入口用 JVM 默认 locale, FRANCE 下输出 "mer."(法语), 与 javadoc(承诺CHINA)
			//和 SimpleDateFormatHolder 同名入口(真用 CHINA)相反
			java.time.format.DateTimeFormatter f = DateFormatterHolder.formatFor("yyyy-MM-dd EEE");
			assertThat(f.format(java.time.LocalDate.of(2026, 9, 22))).isEqualTo("2026-09-22 周二");
		} finally {
			Locale.setDefault(saved);
			DateFormatterHolder.clearThreadLocal(); //换 locale 后旧缓存实例作废
		}
	}

	// ---------- 评审2-P2: ByteChannel 恒 0 读不自旋 ----------

	@Test
	public void testToByteArrayFailsFastOnEndlessZeroReads() {
		ByteChannel zeroChannel = new ByteChannel() {
			@Override
			public int read(ByteBuffer dst) {
				return 0; //合法的非阻塞"未就绪"
			}

			@Override
			public int write(ByteBuffer src) {
				return 0;
			}

			@Override
			public boolean isOpen() {
				return true;
			}

			@Override
			public void close() {
			}
		};
		//修复前实测(P1-11 修复引入的新风险): 循环只认 -1, 恒 0 读永久自旋占核
		assertThatThrownBy(() -> IOUtils.toByteArray(zeroChannel))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("连续返回 0");
	}

	@Test
	public void testToByteArrayRecoversAfterTransientZeroReads() throws IOException {
		//偶发 0 读(未就绪后又有数据)不能误杀: 前两次 0, 之后正常给数据
		final byte[] payload = "hello-zero".getBytes("UTF-8");
		ByteChannel channel = new ByteChannel() {
			int calls = 0;
			int pos = 0;

			@Override
			public int read(ByteBuffer dst) {
				if (calls++ < 2) {
					return 0;
				}
				if (pos >= payload.length) {
					return -1;
				}
				int n = Math.min(3, payload.length - pos); //分小段短读
				dst.put(payload, pos, n);
				pos += n;
				return n;
			}

			@Override
			public int write(ByteBuffer src) {
				return 0;
			}

			@Override
			public boolean isOpen() {
				return true;
			}

			@Override
			public void close() {
			}
		};
		assertThat(new String(IOUtils.toByteArray(channel), "UTF-8")).isEqualTo("hello-zero");
	}

	// ---------- 评审2-P2: createParentDir/copy 不带目录的相对文件名 ----------

	@Test
	public void testCopyToBareRelativePathWorks() throws IOException {
		//修复后语义: toAbsolutePath().getParent() => 不带目录的文件名的父目录=cwd(存在) => copy 正常执行
		java.nio.file.Path cwd = java.nio.file.Path.of("").toAbsolutePath();
		java.nio.file.Path src = cwd.resolve("review-round2-src.txt");
		java.nio.file.Path bare = java.nio.file.Path.of("review-round2-dst.txt"); //只有文件名一段
		try (ByteArrayInputStream in = new ByteArrayInputStream("X".getBytes("UTF-8"))) {
			IOUtils.write(src, "X".getBytes("UTF-8"));
			//修复前(P1-12 第一版): createParentDir=false, copy 返回 false 什么都不写且无任何提示
			boolean copied = IOUtils.copy(src, bare);
			assertThat(copied).isTrue();
			assertThat(java.nio.file.Files.exists(bare)).isTrue();
		} finally {
			java.nio.file.Files.deleteIfExists(src);
			java.nio.file.Files.deleteIfExists(cwd.resolve("review-round2-dst.txt"));
		}
	}

	// ---------- 评审3-P2: toBigDecimal(Float) 无二进制噪声 ----------

	@Test
	public void testToBigDecimalFloatKeepsShortDecimal() {
		//修复前(本批 P1-15 初版): Float 走 doubleValue+BigDecimal.valueOf -> 1.1f 变 1.100000023841858
		assertThat(PrimitiveUtils.toBigDecimal(1.1f)).isEqualByComparingTo(new BigDecimal("1.1"));
		assertThat(PrimitiveUtils.toBigDecimal(0.1f)).isEqualByComparingTo(new BigDecimal("0.1"));
		assertThat(PrimitiveUtils.toBigDecimal(-0.1f)).isEqualByComparingTo(new BigDecimal("-0.1"));
		//Double 行为不回退
		assertThat(PrimitiveUtils.toBigDecimal(3.9)).isEqualByComparingTo(new BigDecimal("3.9"));
	}

	// ---------- 评审3-P2: ApplicationException 无消息 cause ----------

	@Test
	public void testApplicationExceptionNeverReturnsNullMessage() {
		//修复前(本批 P1-21 初版): cause.getMessage()==null(如不带消息的 NullPointerException)时 message 被赋成 null,
		//正是 P1-21 要消除的"上层 getMessage().trim() NPE"形态
		ApplicationException e = new ApplicationException(new NullPointerException());
		assertThat(e.getMessage()).isNotNull();
		assertThat(e.getMessage()).contains("NullPointerException");
		assertThat(e.getMessage().trim()).isNotEmpty(); //全局处理器风格调用不再 NPE
	}
}
