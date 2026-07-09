package com.awesomecopilot.search8x.introspector;

import com.awesomecopilot.search8x.annotation.DocId;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

import java.util.Arrays;
import java.util.List;

public class DocIdAnnotationIntrospector extends NopAnnotationIntrospector {
    
    private static final PropertyName ID_NAME = PropertyName.construct("_id");
    
    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        // 先检查是否有 @DocId
        if (a.hasAnnotation(DocId.class)) {
            // 主名称使用字段本身的名称（通常是 "id"），或者固定为 "id"
            return PropertyName.USE_DEFAULT;
        }
        // 继续使用默认行为（支持 @JsonProperty 等）
        return null;
    }
    
    // 可选：如果你也希望序列化时把字段输出为 _id
    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        if (a.hasAnnotation(DocId.class)) {
            return PropertyName.USE_DEFAULT;
        }
        return null;
    }
    
    @Override
    public List<PropertyName> findPropertyAliases(Annotated a) {
        if (a.hasAnnotation(DocId.class)) {
            return Arrays.asList(PropertyName.construct("_id"));
        }
        return null;
    }
}