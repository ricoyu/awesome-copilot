package com.copilot.search.builder.agg;

import com.copilot.common.lang.vo.Page;
import com.copilot.search.builder.agg.sub.ElasticBucketSortSubAggregation;
import com.copilot.search.builder.agg.sub.SubAggregation;
import com.copilot.search.builder.agg.sub.SubAggregationSupport;
import com.copilot.search.builder.query.BaseQueryBuilder;
import com.copilot.search.enums.SortOrder;
import com.copilot.search.support.AggResultSupport;
import com.copilot.search.support.SortSupport;
import com.copilot.search.vo.ElasticPage;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Terms 聚合, 这是 Bucket Aggregation
 * <p>
 * Copyright: (C), 2021-05-10 11:34
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class ElasticTermsAggregationBuilder extends AbstractAggregationBuilder implements TermAggregationBuilder, SubAggregatable, Compositable {
	
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
	 * 聚合分页的时候计算分页信息
	 */
	private Page page;
	
	/**
	 * 要对聚合的KEY或者COUNT排序
	 */
	protected List<SortOrder> sortOrders = new ArrayList<>();
	
	private ElasticTermsAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticTermsAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticTermsAggregationBuilder(indices);
	}
	
	@Override
	public ElasticTermsAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	@Override
	public ElasticTermsAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	/**
	 * 这个是限制返回桶的数量, 如果总共有10个桶, 但是size设为5, 那么聚合结果中只会返回前5个桶
	 *
	 * @param size
	 * @return ElasticTermsAggregationBuilder
	 */
	public ElasticTermsAggregationBuilder size(Integer size) {
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
	public ElasticTermsAggregationBuilder shardSize(Integer shardSize) {
		this.shardSize = shardSize;
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits
	 * @return ElasticTermsAggregationBuilder
	 */
	public ElasticTermsAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	@Override
	public AggregationBuilder build() {
		TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(name).field(field);
		if (size != null) {
			aggregationBuilder.size(size);
		}
		if (shardSize != null) {
			aggregationBuilder.shardSize(shardSize);
		}
		SubAggregationSupport.addSubAggregations(aggregationBuilder, subAggregations);
		if (!sortOrders.isEmpty()) {
			if (sortOrders.size() == 1) {
				aggregationBuilder.order(sortOrders.get(0).toBucketOrder());
			} else {
				aggregationBuilder.order(sortOrders.stream().map(SortOrder::toBucketOrder).collect(Collectors.toList()));
			}
		}
		return aggregationBuilder;
	}
	
	@Override
	public ElasticCompositeAggregationBuilder and() {
		compositeAggregationBuilder.add(build());
		return compositeAggregationBuilder;
	}
	
	public <T> List<Map<String, T>> get() {
		TermsAggregationBuilder arrregationBuilder = (TermsAggregationBuilder) build();
		
		SearchRequestBuilder searchRequestBuilder = searchRequestBuilder();
		
		SearchRequestBuilder builder = searchRequestBuilder
				.addAggregation(arrregationBuilder)
				.setSize(0);
		logDsl(builder);
		SearchResponse searchResponse = builder.get();
		addTotalHitsToThreadLocal(searchResponse);
		Aggregations aggregations = searchResponse.getAggregations();
		
		return AggResultSupport.termsResult(aggregations);
	}
	
	
	public ElasticPage getPage() {
		TermsAggregationBuilder arrregationBuilder = (TermsAggregationBuilder) build();
		
		SearchRequestBuilder searchRequestBuilder = searchRequestBuilder();
		
		SearchRequestBuilder builder = searchRequestBuilder
				.addAggregation(arrregationBuilder)
				.setSize(0);
		logDsl(builder);
		SearchResponse searchResponse = builder.get();
		addTotalHitsToThreadLocal(searchResponse);
		Aggregations aggregations = searchResponse.getAggregations();
		
		List<Map<String, Object>> results = AggResultSupport.termsResult(aggregations);
		
		ElasticPage elasticPage = ElasticPage.<Map<String, Object>>builder()
				.results(results)
				.build();
		elasticPage.setPageSize(page.getPageSize());
		elasticPage.setPageNum(page.getPageNum());
		return elasticPage;
	}
	
	@Override
	public ElasticTermsAggregationBuilder subAggregation(SubAggregation subAggregation) {
		if (subAggregation instanceof ElasticBucketSortSubAggregation) {
			this.page = ((ElasticBucketSortSubAggregation)subAggregation).toPage();
		}
		subAggregations.add(subAggregation);
		return this;
	}
	
	public ElasticTermsAggregationBuilder sort(String sort) {
		List<SortOrder> sortOrders = SortSupport.sort(sort);
		this.sortOrders.addAll(sortOrders);
		return this;
	}
	
}
