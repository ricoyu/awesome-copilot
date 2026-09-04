package com.awesomecopilot.json;

import com.awesomecopilot.json.jackson.serializer.HtmlEscapeStringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSS 输出序列化测试
 * <p>
 * Copyright: Copyright (c) 2025
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ObjectMapperDecoratorXssTest {

	@Data
	public static class User {
		private String name;
		private String intro;
	}

	@Test
	public void testXssDisabledByDefault() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		new ObjectMapperDecorator().decorate(objectMapper);

		User user = new User();
		user.setName("<script>alert('xss')</script>");
		user.setIntro("普通文本 & 说明");

		String json = objectMapper.writeValueAsString(user);
		assertThat(json).contains("<script>alert('xss')</script>");
		assertThat(json).doesNotContain("&lt;script&gt;");
	}

	@Test
	public void testHtmlEscapeStringSerializer() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(String.class, new HtmlEscapeStringSerializer());
		objectMapper.registerModule(module);

		User user = new User();
		user.setName("<script>alert('xss')</script>");
		user.setIntro("普通文本 & 说明");

		String json = objectMapper.writeValueAsString(user);
		assertThat(json).contains("&lt;script&gt;");
		assertThat(json).doesNotContain("<script>alert('xss')</script>");
	}
}
