package com.awesomecopilot.json.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;

/**
 * 如果开启了copilot.filter.xss-enabled, 后端在输出JSON结果给前端时, 会对输出的字符串进行HTML转义
 */
public class HtmlEscapeStringSerializer extends StdScalarSerializer<String> {

	public HtmlEscapeStringSerializer() {
		super(String.class);
	}

	@Override
	public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}
		// 修正：Jackson 2.15+ getAttribute 仅接收1个参数，手动处理默认值
		Object skipEscapeAttr = provider.getAttribute("SKIP_HTML_ESCAPE");
		boolean skipEscape = skipEscapeAttr != null && (Boolean) skipEscapeAttr;
		boolean escape = !skipEscape; // 非豁免则转义

		if (escape) {
			// 转义 HTML 特殊字符
			gen.writeString(StringEscapeUtils.escapeHtml4(value));
		} else {
			// 豁免转义，直接输出原始值
			gen.writeString(value);
		}
	}
}