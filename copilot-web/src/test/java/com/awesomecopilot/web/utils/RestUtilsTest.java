package com.awesomecopilot.web.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RestUtils.download 与跨域开关字段测试。
 * <p>
 * 评审报告 P1-5：① enableCors 是 static 可变字段, 运行期可被任意代码改写, 属全局可变状态;
 * ② download 用 IOUtils.readFileAsBytes 把整个文件读进内存再一次性写出, 大文件并发下载
 * 会推高峰值内存。修复后 enableCors 必须为 final(启动读一次配置后不可变), download 改为
 * 流式拷贝并带上 Content-Length。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RestUtilsTest {

	@Test
	public void testEnableCorsIsImmutableAfterClassLoad() throws Exception {
		Field f = RestUtils.class.getDeclaredField("enableCors");
		assertTrue(Modifier.isFinal(f.getModifiers()),
				"enableCors 必须是 final: 只在类加载时读一次配置, 运行期不允许再改");
	}

	@Test
	public void testDownloadStreamsFileWithContentLength() throws Exception {
		File tmp = File.createTempFile("restutils", ".bin");
		tmp.deleteOnExit();
		byte[] payload = new byte[256 * 1024];
		for (int i = 0; i < payload.length; i++) {
			payload[i] = (byte) (i % 127);
		}
		Files.write(tmp.toPath(), payload);

		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
		try {
			RestUtils.download(tmp, "data.bin");

			assertArrayEquals(payload, response.getContentAsByteArray(), "下载内容必须与文件字节一致");
			assertEquals(String.valueOf(payload.length),
					response.getHeader("Content-Length"), "必须设置Content-Length");
			assertTrue(response.getHeader("Content-Disposition").contains("data.bin"));
		} finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}
}
