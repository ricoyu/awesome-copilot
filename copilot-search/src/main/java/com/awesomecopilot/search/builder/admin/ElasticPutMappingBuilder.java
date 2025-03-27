package com.awesomecopilot.search.builder.admin;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search.ElasticUtils;
import com.awesomecopilot.search.enums.Dynamic;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

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
@Slf4j
public class ElasticPutMappingBuilder extends AbstractMappingBuilder {
	
	private String index;
	
	public ElasticPutMappingBuilder(String index, Dynamic dynamic) {
		super(index, dynamic);
		this.index = index;
		notNull(index, "index cannot be null!");
	}
	
	public boolean thenCreate() {
		Map<String, Object> source = build();
		if (log.isDebugEnabled()) {
			log.debug("Mapping:\n{}", JacksonUtils.toPrettyJson(source));
		}
		PutMappingRequestBuilder putMappingRequestBuilder = ElasticUtils.CLIENT.admin().indices().preparePutMapping(index);
		AcknowledgedResponse acknowledgedResponse = putMappingRequestBuilder.setType(ElasticUtils.ONLY_TYPE)
				.setSource(source)
				.get();
		return acknowledgedResponse.isAcknowledged();
	}
}
