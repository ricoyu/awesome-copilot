package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.stream.JsonParser;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;


/**
 * <p>
 * Copyright: (C), 2021-03-26 16:37
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticPutMappingBuilder extends AbstractMappingBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticPutMappingBuilder.class);
	
	private String index;
	
	public ElasticPutMappingBuilder(String index, Dynamic dynamic) {
		super(index, dynamic);
		this.index = index;
		notNull(index, "index cannot be null!");
	}
	
	/**
	 * 执行 Mapping 更新操作
	 * <p>
	 * 将之前通过 field() 方法定义的字段 Mapping 应用到指定的索引<br/>
	 * 注意：只能添加新字段或更新已有字段的某些属性，不能修改已有字段的类型
	 * <p>
	 * 使用场景：为已存在的索引添加新字段或更新 Mapping 配置
	 *
	 * @return true 表示操作成功，false 表示操作失败
	 */
	public boolean thenCreate() {
		Map<String, Object> source = build();
		if (log.isDebugEnabled()) {
			log.debug("Mapping:\n{}", JacksonUtils.toPrettyJson(source));
		}
		
		try {
			String json = JacksonUtils.toJson(source);
			JacksonJsonpMapper mapper = new JacksonJsonpMapper();
			JsonParser parser = mapper.jsonProvider().createParser(
					new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
			
			PutMappingRequest.Builder reqBuilder = new PutMappingRequest.Builder();
			reqBuilder.withJson(parser, mapper);
			reqBuilder.index(index);
			
			return ElasticUtils.QUERY_CLIENT.indices().putMapping(reqBuilder.build()).acknowledged();
		} catch (IOException e) {
			throw new RuntimeException("Failed to put mapping for index: " + index, e);
		}
	}
}
