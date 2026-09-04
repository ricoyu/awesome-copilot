package com.awesomecopilot.json.jackson.serializer;

import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.json.jackson.annotation.UnescapeHtml;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * 全局字符串 HTML 转义序列化器（唯一实现）。
 * <p>
 * 如果开启了 copilot.filter.xss-enabled, 后端在输出JSON结果给前端时, 会对输出的字符串进行HTML转义。
 * 转义统一走 {@link StringUtils#escapeHtml4(String)}, 与 commons-lang3 保持一致并额外保留人民币符号。
 * <p>
 * 豁免判定通过 {@link ContextualSerializer} 在 {@link #createContextual} 阶段拿到字符串字段的声明类,
 * 若该类标注了 {@link UnescapeHtml}, 则该字段原样输出, 不再依赖 {@code generator.currentValue()} 的运行时取值。
 */
public class HtmlEscapeStringSerializer extends StdScalarSerializer<String> implements ContextualSerializer {

	/**
	 * 默认转义实例
	 */
	public static final HtmlEscapeStringSerializer ESCAPE_INSTANCE = new HtmlEscapeStringSerializer(true);

	/**
	 * 豁免转义实例（VO 类标注了 @UnescapeHtml）
	 */
	public static final HtmlEscapeStringSerializer NO_ESCAPE_INSTANCE = new HtmlEscapeStringSerializer(false);

	private final boolean escape;

	public HtmlEscapeStringSerializer() {
		this(true);
	}

	private HtmlEscapeStringSerializer(boolean escape) {
		super(String.class);
		this.escape = escape;
	}

	@Override
	public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}
		if (escape) {
			gen.writeString(StringUtils.escapeHtml4(value));
		} else {
			gen.writeString(value);
		}
	}

	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
		if (property != null && property.getMember() != null
				&& property.getMember().getDeclaringClass().isAnnotationPresent(UnescapeHtml.class)) {
			return NO_ESCAPE_INSTANCE;
		}
		return ESCAPE_INSTANCE;
	}
}
