package com.awesomecopilot.search8x.support;

import com.awesomecopilot.search8x.annotation.Index;
import com.awesomecopilot.search8x.exception.InvalidSettingsException;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 从Entity上的@Index注解抽取Settings相关信息
 * <p>
 * Copyright: (C), 2021-05-06 14:09
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class SettingsSupport {
	
	/**
	 * 从@Index 注解中抽取Settings相关信息
	 * @param entity 必须是标注了@Index注解的类
	 * @return Map<String, Object> settings键值对
	 */
	public static Map<String, Object> extractIndexSettings(Class entity) {
		if (entity == null) {
			return null;
		}
		
		Index index = (Index) entity.getAnnotation(Index.class);
		if (index == null) {
			return null;
		}
		
		//主分片数
		int numberOfShards = index.numberOfShards();
		//副本分片数
		int numberOfReplicas = index.numberOfReplicas();
		String defaultPipeline = index.defaultPipeline();
		//拿到的是key=value形式
		String indexRouting = index.indexRouting();
		boolean blocksWrite = index.blocksWrite();
		
		Map<String, Object> settings = new HashMap<>();
		settings.put("number_of_shards", numberOfShards);
		settings.put("number_of_replicas", numberOfReplicas);
		
		if (isNotBlank(defaultPipeline)) {
			settings.put("index.default_pipeline", defaultPipeline);
		}
		
		if (blocksWrite) {
			settings.put("index.blocks.write", true);
		}
		
		if (isNotBlank(indexRouting)) {
			String[] routing = indexRouting.split("=");
			if (routing.length != 2) {
				throw new InvalidSettingsException("indexRouting can only be name=value pair!");
			}
			
			String routingKey = routing[0];
			String routingValue = routing[1];
			if (isBlank(routingKey) || isBlank(routingValue)) {
				throw new InvalidSettingsException("indexRouting name=value pair cannot be empty!");
			}
			settings.put("index.routing.allocation.require." + routingKey, routingValue);
		}
		
		return settings;
	}
}
