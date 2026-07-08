package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.agg.sub.ElasticBucketSortSubAggregation;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.enums.SortOrderEnum;
import com.awesomecopilot.search8x.support.AggResultSupport;
import com.awesomecopilot.search8x.support.SortSupport;
import com.awesomecopilot.search8x.vo.ElasticPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Multi Terms 聚合, 这是 Bucket Aggregation <br/>
 * 基于多个字段的组合来计算分桶
 * <p>
 * Copyright: (C), 2021-05-10 11:34
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticMultiTermsAggregationBuilder extends AbstractAggregationBuilder implements TermAggregationBuilder, SubAggregatable, Compositable {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticMultiTermsAggregationBuilder.class);
	
	/**
	 * 这个是限制返回桶的数量, 如果总共有10个桶, 但是size设为5, 那么聚合结果中只会返回前5个桶
	 */
	private Integer size;
	
	/**
	 * 帮助解决Terms不准的问题<br/>
	 * Terms 聚合分析不准的原因, 数据分散在多个不同的分片上, Coordinating Node 无法获取数据全貌<br/>
	 * 解决方案 1: 当数据量不大时, 设置Primary Shard为1; 实现准确性<br/>
	 * 方案 2: 在分布式数据上, 设置shard_size参数, 提高精确度<br/>
	 * 原理: 每次从Shard上额外多获取数据, 提升准确率
	 */
	private Integer shardSize;
	
	/**
	 * 要对哪些字段聚合
	 */
	private String[] fields;
	
	/**
	 * 聚合分页的时候计算分页信息
	 */
	private Page page;
	
	/**
	 * 要对聚合的KEY或者COUNT排序
	 */
	protected List<SortOrderEnum> sortOrderEnums = new ArrayList<>();
	
	private ElasticMultiTermsAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticMultiTermsAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticMultiTermsAggregationBuilder(indices);
	}
	
	@Override
	public ElasticMultiTermsAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	public ElasticMultiTermsAggregationBuilder of(String name, String... fields) {
		this.name = name;
		this.fields = fields;
		return this;
	}
	
	public ElasticMultiTermsAggregationBuilder of(String name, String field) {
		this.name = name;
		this.fields = new String[]{field};
		return this;
	}
	
	public ElasticMultiTermsAggregationBuilder of(String name, List<String> fields) {
		Objects.requireNonNull(fields, "fields cannot be null!");
		this.name = name;
		this.fields = fields.stream().toArray(String[]::new);
		return this;
	}
	
	/**
	 * 这个是限制返回桶的数量, 如果总共有10个桶, 但是size设为5, 那么聚合结果中只会返回前5个桶
	 *
	 * @param size
	 * @return ElasticMultiTermsAggregationBuilder
	 */
	public ElasticMultiTermsAggregationBuilder size(Integer size) {
		this.size = size;
		return this;
	}
	
	/**
	 * 帮助解决Terms不准的问题<br/>
	 * Terms 聚合分析不准的原因, 数据分散在多个不同的分片上, Coordinating Node 无法获取数据全貌<br/>
	 * 解决方案 1: 当数据量不大时, 设置Primary Shard为1; 实现准确性<br/>
	 * 方案 2: 在分布式数据上, 设置shard_size参数, 提高精确度<br/>
	 * 原理: 每次从Shard上额外多获取数据, 提升准确率
	 *
	 * @param shardSize
	 * @return ElasticTermsAggregationBuilder
	 */
	public ElasticMultiTermsAggregationBuilder shardSize(Integer shardSize) {
		this.shardSize = shardSize;
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数 
	 * @param fetchTotalHits
	 * @return ElasticMultiTermsAggregationBuilder
	 */
	public ElasticMultiTermsAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	public Aggregation buildWithoutSubAggregations() {
		return Aggregation.of(a -> a.terms(t -> {
			if (size != null) {
				t.size(size);
			}
			if (shardSize != null) {
				t.shardSize(shardSize);
			}
			Map<String, JsonData> params = new HashMap<>();
			params.put("fields", JsonData.of(fields));
			//ElasticUtils.Cluster.createMultiFieldAgg 创建的stored script名字是这个: multi_fields
			Script painless = Script.of(s -> s.id("multi_fields").params(params));
			t.script(painless);
			if (!sortOrderEnums.isEmpty()) {
				List<NamedValue<co.elastic.clients.elasticsearch._types.SortOrder>> order = 
						sortOrderEnums.stream().map(SortOrderEnum::toBucketOrder).collect(Collectors.toList());
				t.order(order);
			}
			return t;
		}));
	}
	
	@Override
	public Aggregation build() {
		Map<String, Aggregation> subAggsMap = buildSubAggregationsMap(subAggregations);
		return Aggregation.of(a -> {
			var cb = a.terms(t -> {
				if (size != null) {
					t.size(size);
				}
				if (shardSize != null) {
					t.shardSize(shardSize);
				}
				Map<String, JsonData> params = new HashMap<>();
				params.put("fields", JsonData.of(fields));
				//ElasticUtils.Cluster.createMultiFieldAgg 创建的stored script名字是这个: multi_fields
				Script painless = Script.of(s -> s.id("multi_fields").params(params));
				t.script(painless);
				if (!sortOrderEnums.isEmpty()) {
					List<NamedValue<co.elastic.clients.elasticsearch._types.SortOrder>> order = 
							sortOrderEnums.stream().map(SortOrderEnum::toBucketOrder).collect(Collectors.toList());
					t.order(order);
				}
				return t;
			});
			if (subAggsMap != null) {
				cb.aggregations(subAggsMap);
			}
			return a;
		});
	}
	
	@Override
	public ElasticCompositeAggregationBuilder and() {
		// 使用 this.name 作为聚合名称
		compositeAggregationBuilder.add(this.name, build());
		return compositeAggregationBuilder;
	}
	
	public <T> List<Map<String, T>> get() {
		Aggregation aggregation = build();
		
		SearchRequest.Builder builder = searchRequestBuilder();
		builder.aggregations(name, aggregation).size(0);
		SearchRequest request = builder.build();
		logDsl(request);
		
		SearchResponse<Map> searchResponse = null;
		try {
			searchResponse = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
		} catch (Exception e) {
			String errorDetail = extractEsErrorDetails(e);
			log.error("MultiTerms aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		
		return AggResultSupport.termsResult(aggregations);
	}
	
	public ElasticPage getPage() {
		Aggregation aggregation = build();
		
		SearchRequest.Builder builder = searchRequestBuilder();
		builder.aggregations(name, aggregation).size(0);
		SearchRequest request = builder.build();
		logDsl(request);
		
		SearchResponse<Map> searchResponse = null;
		try {
			searchResponse = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
		} catch (Exception e) {
			String errorDetail = extractEsErrorDetails(e);
			log.error("MultiTerms aggregation getPage failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		
		List<Map<String, Object>> results = AggResultSupport.termsResult(aggregations);
		
		ElasticPage elasticPage = ElasticPage.<Map<String, Object>>builder()
				.results(results)
				.build();
		
		elasticPage.setPageSize(page.getPageSize());
		elasticPage.setPageNum(page.getPageNum());
		return elasticPage;
	}
	
	@Override
	public ElasticMultiTermsAggregationBuilder subAggregation(SubAggregation subAggregation) {
		if (subAggregation instanceof ElasticBucketSortSubAggregation) {
			this.page = ((ElasticBucketSortSubAggregation)subAggregation).toPage();
		}
		subAggregations.add(subAggregation);
		return this;
	}
	
	public ElasticMultiTermsAggregationBuilder sort(String sort) {
		List<SortOrderEnum> sortOrderEnums = SortSupport.sort(sort);
		this.sortOrderEnums.addAll(sortOrderEnums);
		return this;
	}
}
