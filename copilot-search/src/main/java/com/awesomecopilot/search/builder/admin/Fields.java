package com.awesomecopilot.search.builder.admin;

import com.awesomecopilot.search.enums.FieldType;

/**
 * 构建 FieldDefBuilder的帮助类
 * <p>
 * Copyright: Copyright (c) 2024-05-16 10:39
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class Fields {

    public static FieldDefBuilder field(String fieldName, FieldType fieldType) {
        return FieldDefBuilder.builder(fieldName, fieldType);
    }
}
