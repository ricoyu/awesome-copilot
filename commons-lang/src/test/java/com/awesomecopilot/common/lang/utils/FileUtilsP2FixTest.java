package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-14 / P2-29 FileUtils 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>P2-14: isImage 内部 ImageIO.read 消耗调用方的输入流且不重置——探针实测 68 字节 PNG
 * 调用后只剩 16 字节可读，"先判断是不是图片、再保存同一个流"会保存出残缺文件。
 * 修复契约: 入参流支持 mark/reset 时(ByteArrayInputStream、BufferedInputStream 等),
 * 判断结束后流位置复原; 不支持 mark 的流无法回退(数据已被读进 JVM 缓冲区,
 * 物理上收不回来), javadoc 写明会消费。</li>
 * <li>P2-29: cleanFilename 的幂等短路放行路径穿越（实测 "abcdef123456_../../evil.txt"
 * 清洗后 "../" 原样保留），且 toLowerCase() 不带 Locale——土耳其语环境同一输入
 * 得到不同结果（'I'→'ı'），"同一文件名总是得到同一结果"契约被破坏。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class FileUtilsP2FixTest {

	private static byte[] tinyPng() throws Exception {
		BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", bos);
		return bos.toByteArray();
	}

	// ---------------- P2-14 isImage 不消耗可回退的输入流 ----------------

	@Test
	public void testIsImageRestoresMarkableStreamPosition() throws Exception {
		byte[] png = tinyPng();
		ByteArrayInputStream in = new ByteArrayInputStream(png); //支持 mark/reset 的典型流

		assertThat(FileUtils.isImage(png.length, "photo.png", in))
				.as("合法 PNG 应判定为图片").isTrue();

		//修复前实测: 调用后只剩 16/68 字节可读——继续保存同一文件得到残缺内容
		ByteArrayOutputStream rest = new ByteArrayOutputStream();
		byte[] buf = new byte[128];
		int len;
		while ((len = in.read(buf)) != -1) {
			rest.write(buf, 0, len);
		}
		assertThat(rest.toByteArray())
				.as("isImage 判断完后, 可回退流必须仍能读出完整内容(修复前只剩 16 字节)")
				.hasSize(png.length);
	}

	@Test
	public void testIsImageRestoresPositionEvenForFakeImage() throws Exception {
		byte[] fake = "this is not an image at all!!!".getBytes("UTF-8");
		ByteArrayInputStream in = new ByteArrayInputStream(fake);
		assertThat(FileUtils.isImage(fake.length, "evil.png", in)).isFalse();
		//失败路径同样复原位置
		assertThat(in.read())
				.as("非图片判定后流位置仍应可读回第一个字节")
				.isEqualTo(fake[0] & 0xFF);
	}

	@Test
	public void testIsImageStillRejectsBadExtension() throws Exception {
		byte[] png = tinyPng();
		assertThat(FileUtils.isImage(png.length, "script.exe", new ByteArrayInputStream(png)))
				.as("后缀不在白名单直接 false, 不碰流").isFalse();
	}

	@Test
	public void testIsImageNonMarkableStreamStillDetectsImage() throws Exception {
		byte[] png = tinyPng();
		//不支持 mark 的流(如某些 ServletInputStream): 不报错, 判定结果正确即可,
		//流被消费是 javadoc 写明的行为(已读进 JVM 缓冲区的数据物理上收不回来)
		InputStream noMark = new InputStream() {
			private final ByteArrayInputStream delegate = new ByteArrayInputStream(png);

			@Override
			public int read() throws IOException {
				return delegate.read();
			}

			@Override
			public boolean markSupported() {
				return false;
			}
		};
		assertThat(FileUtils.isImage(png.length, "photo.png", noMark)).isTrue();
	}

	// ---------------- P2-29 幂等短路放行路径穿越 ----------------

	@Test
	public void testCleanFilenameBlocksTraversalInIdempotentShortcut() {
		//修复前实测: 前 12 位形如 [a-f0-9]{12}_ 就原样返回, "../" 保留——
		//返回值参与拼接存储路径即为路径穿越
		String cleaned = FileUtils.cleanFilename("abcdef123456_../../evil.txt");
		assertThat(cleaned)
				.as("幂等短路不得放行 ../ 或反斜杠(修复前原样返回)")
				.doesNotContain("..")
				.doesNotContain("/")
				.doesNotContain("\\");
	}

	@Test
	public void testCleanFilenameIdempotencyStillHoldsForNormalNames() {
		String once = FileUtils.cleanFilename("DSC00064.JPG");
		String twice = FileUtils.cleanFilename(once);
		assertThat(twice).as("正常清洗结果二次调用应保持不变(幂等契约不能因修复而失效)")
				.isEqualTo(once);
	}

	@Test
	public void testCleanFilenameStableAcrossDefaultLocale() {
		Locale saved = Locale.getDefault();
		try {
			//名字里必须含 'I': 土耳其语下 'I'→'ı'(无点小写i), 修复前才暴露差异
			String name = "IMG_20260923_最终版.JPG";
			Locale.setDefault(Locale.ENGLISH);
			String inEnglish = FileUtils.cleanFilename(name);
			Locale.setDefault(new Locale("tr", "TR"));
			String inTurkish = FileUtils.cleanFilename(name);
			assertThat(inTurkish)
					.as("toLowerCase 必须固定 Locale.ROOT(修复前随 JVM 默认 Locale, 土耳其语下 'I'→'ı' 结果不同)")
					.isEqualTo(inEnglish);
		} finally {
			Locale.setDefault(saved);
		}
	}
}
