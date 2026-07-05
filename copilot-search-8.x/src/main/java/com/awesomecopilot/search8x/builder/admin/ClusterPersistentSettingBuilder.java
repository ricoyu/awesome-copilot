package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsRequest;
import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsResponse;
import co.elastic.clients.json.JsonData;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.cluster.AllocationEnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 集群相关Settings
 * <p>
 * Copyright: (C), 2021-04-18 8:19
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ClusterPersistentSettingBuilder {
	
	private ClusterSettingBuilder clusterSettingBuilder;
	
	/**
	 * cluster.routing.allocation.enable
	 */
	private AllocationEnable allocationEnable;
	
	private Integer searchMaxBuckets = 10000;
	
	public ClusterPersistentSettingBuilder(ClusterSettingBuilder clusterSettingBuilder) {
		this.clusterSettingBuilder = clusterSettingBuilder;
		clusterSettingBuilder.setPersistentSettingBuilder(this);
	}
	
	public ClusterPersistentSettingBuilder routingAllocationEnable(AllocationEnable allocationEnable) {
		this.allocationEnable = allocationEnable;
		return this;
	}
	
	public ClusterPersistentSettingBuilder searchMaxBuckets(Integer searchMaxBuckets) {
		this.searchMaxBuckets = searchMaxBuckets;
		return this;
	}
	
	Map<String, JsonData> build() {
		Map<String, JsonData> persistent = new HashMap<>();
		if (allocationEnable != null) {
			persistent.put("cluster.routing.allocation.enable", JsonData.of(allocationEnable.toString()));
		}
		if (searchMaxBuckets != null) {
			persistent.put("search.max_buckets", JsonData.of(searchMaxBuckets));
		}
		return persistent;
	}
	
	public ClusterSettingBuilder and() {
		return clusterSettingBuilder;
	}
	
	public boolean thenUpdate() {
		return clusterSettingBuilder.update();
	}
}
