package com.awesomecopilot.search8x.builder.admin;

import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.enums.FieldType;
import com.awesomecopilot.search8x.support.FieldDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * TODO 为字段设置子字段?
 * <p>
 * Copyright: (C), 2020-12-28 8:54
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class AbstractMappingBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(AbstractMappingBuilder.class);
	
	private String index;
	
	/**
	 * 指定从这个索引复制Mapping设置
	 */
	private String copyIndex;
	
	/**
	 * 设置索引的dynamic属性: true false strict
	 */
	private Dynamic dynamic = null;
	
	/**
	 * 是否要存储_source
	 */
	private Boolean sourceEnabled = null;
	
	/**
	 * 自动将日期字符串识别为date类型
	 */
	private Boolean dateDetection = null;
	
	private Boolean numericDetection = null;
	
	/**
	 * 字段定义, 字段名-字段类型
	 */
	private Set<FieldDef> fields = new HashSet<>();
	
	/**
	 * 某些字段定义不需要的话恶意删除
	 */
	private Set<String> deleteFields = new HashSet<>();
	
	public AbstractMappingBuilder() {
	}
	
	public AbstractMappingBuilder(Dynamic dynamic) {
		this.dynamic = dynamic;
	}
	
	public AbstractMappingBuilder(String index, Dynamic dynamic) {
		this.index = index;
		this.dynamic = dynamic;
	}
	
	public AbstractMappingBuilder index(String index) {
		this.index = index;
		return this;
	}
	
	/**
	 * 从一个索引中拷贝其mapping设置, 然后只需要显式设置某些字段的mapping, 减少coding量
	 *
	 * @param copyIndex
	 * @return PutMappingBuilder
	 */
	public AbstractMappingBuilder copy(String copyIndex) {
		this.copyIndex = copyIndex;
		return this;
	}
	
	/**
	 * 是否要存储_source
	 *
	 * @param sourceEnabled
	 * @return ElasticMappingBuilder
	 */
	public AbstractMappingBuilder sourceEnabled(Boolean sourceEnabled) {
		this.sourceEnabled = sourceEnabled;
		return this;
	}
	
	/**
	 * 自动将日期字符串识别为date类型
	 * @param dateDetection
	 * @return AbstractMappingBuilder
	 */
	public AbstractMappingBuilder dateDetection(Boolean dateDetection) {
		this.dateDetection = dateDetection;
		return this;
	}
	
	public AbstractMappingBuilder numericDetection(Boolean numericDetection) {
		this.numericDetection = numericDetection;
		return this;
	}
	
	/**
	 * FieldDefBuilder设置字段
	 *
	 * @param fieldName
	 * @param fieldType
	 * @return
	 */
	public FieldDefBuilder field(String fieldName, FieldType fieldType) {
		return new FieldDefBuilder(this, fieldName, fieldType);
	}
	
	public AbstractMappingBuilder field(FieldDef fieldDef) {
		fields.add(fieldDef);
		return this;
	}
	
	public AbstractMappingBuilder field(FieldDefBuilder fieldDefBuilder) {
		fields.add(fieldDefBuilder.build());
		return this;
	}
	
	public AbstractMappingBuilder delete(String... fields) {
		for (int i = 0; i < fields.length; i++) {
			deleteFields.add(fields[i]);
		}
		return this;
	}
	
	Map<String, Object> build() {
		Map<String, Object> source = new HashMap<>();
		
		/*
		 * 从已有索引中拷贝mapping信息
		 */
		if (isNotBlank(copyIndex)) {
			source = ElasticUtils.Mappings.getMapping(copyIndex);
		}
		
		/*
		 * 设置dynamic
		 */
		if (dynamic != null) {
			source.put("dynamic", dynamic.toString());
		}
		
		/**
		 * 设置 "_source": {"enabled": true}
		 */
		if (sourceEnabled != null) {
			source.put("_source", new HashMap(1) {{
				put("enabled", sourceEnabled);
			}});
		}
		
		if (dateDetection != null) {
			source.put("date_detection", dateDetection);
		}
		
		if (numericDetection != null) {
			source.put("numeric_detection", numericDetection);
		}
		
		//先取出properties, 如果没有, 那么创建一个
		Map<String, Object> properties = (Map<String, Object>) source.get("properties");
		if (properties == null) {
			properties = new HashMap<>();
			source.put("properties", properties);
		}
		
		//去掉要删除的字段定义
		if (!deleteFields.isEmpty() && !properties.isEmpty()) {
			for (String fieldName : deleteFields) {
				properties.remove(fieldName);
			}
		}
		
		/*
		 * 挨个设置字段定义
		 */
		if (!fields.isEmpty()) {
			for (FieldDef fieldDef : fields) {
				properties.put(fieldDef.getFieldName(), fieldDef.toDefMap());
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Mappings:\n{}", toPrettyJson(source));
		}
		return source;
	}
	
	/**
	 * 执行实际操作
	 *
	 * @return
	 */
	public abstract boolean thenCreate();
	
}
