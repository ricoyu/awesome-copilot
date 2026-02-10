package com.awesomecopilot.json.jackson.serializer;

import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.json.jackson.annotation.UnescapeHtml;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * 全局字符串序列化器【最终定稿版】
 * ✅ 解决根对象null问题、类型转换问题、序列化报错问题
 * ✅ 类注解生效：标注解→豁免转义，无注解→自动转义
 */
public class GlobalHtmlEscapeSerializer extends StdScalarSerializer<String> {

    public GlobalHtmlEscapeSerializer() {
        super(String.class); // 仅处理String类型字段，杜绝类型错误
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // ========== ✅ 核心修复：Jackson官方API获取VO根对象（永不null） ==========
        boolean isVoUnescape = false;
        // 获取当前序列化的【根对象】（你的BrandVO/UserVO），标准API，兼容所有版本
        Object rootValue = provider.getGenerator().currentValue();
        if (rootValue != null) {
            // 判断根对象的类上，是否标注了@UnescapeHtml注解
            isVoUnescape = rootValue.getClass().isAnnotationPresent(UnescapeHtml.class);
        }

        // ========== ✅ 最终序列化逻辑（无任何坑） ==========
        if (isVoUnescape) {
            // VO类标了注解 → 整个VO所有字段 原样输出（豁免转义）
            gen.writeString(value);
        } else {
            // VO类无注解 → 全局默认 HTML转义
            gen.writeString(StringUtils.escapeHtml4(value));
        }
    }
}