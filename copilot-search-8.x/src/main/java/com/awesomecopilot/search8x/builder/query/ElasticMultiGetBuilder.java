package com.awesomecopilot.search8x.builder.query;

import com.awesomecopilot.json.jackson.JacksonUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest.Item;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.*;

/**
 * <p>
 * Copyright: (C), 2020-12-25 8:58
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticMultiGetBuilder<T> {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticMultiGetBuilder.class);
	
	private RestHighLevelClient client;
	
	private List<Item> items = new ArrayList<>();
	
	private Class<T> clazz;
	
	public ElasticMultiGetBuilder(RestHighLevelClient client) {
		this.client = client;
	}
	
	public ElasticMultiGetBuilder add(String index, String id) {
		items.add(new Item(index, id));
		return this;
	}
	
	public ElasticMultiGetBuilder add(String index, List<String> ids) {
		ids.stream().map(id -> new Item(index, id))
				.forEach(item -> items.add(item));
		return this;
	}
	
	public ElasticMultiGetBuilder resultType(Class<T> clazz) {
		this.clazz = clazz;
		return this;
	}
	
	public List<T> request() {
		MultiGetRequest multiGetRequest = new MultiGetRequest();
		items.forEach(multiGetRequest::add);
		MultiGetResponse multiGetItemResponses;
		try {
			multiGetItemResponses = client.mget(multiGetRequest, RequestOptions.DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		MultiGetItemResponse[] itemResponses = multiGetItemResponses.getResponses();
		List<String> resultJsons = Arrays.asList(itemResponses).stream()
				.map((itemResponse) -> {
					GetResponse response = itemResponse.getResponse();
					if (response.isExists()) {
						return response.getSourceAsString();
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(toList());
		
		if (clazz != null) {
			return resultJsons.stream()
					.map(json -> JacksonUtils.toObject(json, clazz))
					.collect(toList());
		}
		
		return (List<T>) resultJsons;
	}
}