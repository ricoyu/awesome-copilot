package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.search8x.builder.agg.sub.ElasticBucketSortSubAggregation;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregationSupport;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.enums.SortOrder;
import com.awesomecopilot.search8x.support.SortSupport;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import com.awesomecopilot.search8x.vo.ElasticPage;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
public class ElasticTermsAggregationBuilder extends AbstractAggregationBuilder implements TermAggregationBuilder, SubAggregatable, Compositable {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticTermsAggregationBuilder.class);

	/**
	 * 嵌套聚合名称
	 */
	private String nestedAggName;
	/**
	 * 嵌套查询的字段
	 */
	private String nestedPath;

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
	
	/**
	 * 设置聚合名称和聚合字段
	 * <p>
	 * 聚合名称用于在结果中标识该聚合，聚合字段指定要对哪个字段进行分组统计
	 *
	 * @param name  聚合名称
	 * @param field 要聚合的字段名
	 * @return ElasticTermsAggregationBuilder
	 */
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
	 * 设置嵌套聚合字段
	 * @param nestedPath
	 * @return
	 */
	public ElasticTermsAggregationBuilder nestedPath(String nestedAggName, String nestedPath) {
		this.nestedAggName = nestedAggName;
		this.nestedPath = nestedPath;
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
		NestedAggregationBuilder nestedAggregationBuilder = null;
		if (isNotBlank(nestedPath)) {
			nestedAggregationBuilder = AggregationBuilders.nested(nestedAggName, nestedPath)
					.subAggregation(aggregationBuilder);
		}
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

		if (nestedAggregationBuilder != null) {
			return nestedAggregationBuilder;
		}
		return aggregationBuilder;
	}
	
	/**
	 * 构建 ES 8.x 原生 Terms Aggregation
	 *
	 * @return ES 8.x Aggregation 对象
	 */
	private Aggregation buildV8Aggregation() {
		co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation.Builder builder = 
				new co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation.Builder();
		builder.field(field);
		
		if (size != null) {
			builder.size(size);
		}
		if (shardSize != null) {
			builder.shardSize(shardSize);
		}

		// TODO: 支持排序和子聚合（后续实现）
		
		return new Aggregation.Builder()
				.terms(builder.build())
				.build();
	}
	
	@Override
	public ElasticCompositeAggregationBuilder and() {
		compositeAggregationBuilder.add(build());
		return compositeAggregationBuilder;
	}
	
	/**
	 * 执行聚合查询并返回结果
	 * <p>
	 * 执行 Terms 聚合查询，返回每个桶的统计结果<br/>
	 * 结果格式为 List&lt;Map&lt;String, T&gt;&gt;，每个 Map 代表一个桶，包含 key 和 doc_count 等信息
	 *
	 * @param <T> 结果值的类型
	 * @return 聚合结果列表，每个元素是一个桶的统计信息
	 */
	public <T> List<Map<String, T>> get() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());

		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生结果解析器
		if (searchResponse.aggregations() != null) {
			return V8AggResultSupport.termsResult(searchResponse.aggregations());
		}
		return new ArrayList<>();
	}
	
	
	/**
	 * 执行聚合查询并返回分页结果
	 * <p>
	 * 执行 Terms 聚合查询，返回分页格式的结果<br/>
	 * 需要先通过 subAggregation() 方法添加 bucket_sort 子聚合来实现分页
	 * <p>
	 * 使用示例：
	 * <pre>
	 * ElasticPage page = ElasticUtils.Aggs.terms("index")
	 *     .of("agg_name", "field")
	 *     .subAggregation(bucketSort(0, 10))
	 *     .getPage();
	 * </pre>
	 *
	 * @return ElasticPage 分页结果对象，包含当前页数据和分页信息
	 */
	public ElasticPage getPage() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());

		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生结果解析器
		List<Map<String, Object>> results = new ArrayList<>();
		if (searchResponse.aggregations() != null) {
			results = V8AggResultSupport.termsResult(searchResponse.aggregations());
		}
		
		// 计算总数
		long total = termsTotalBucketsFromResponse(searchResponse);
		
		ElasticPage elasticPage = ElasticPage.<Map<String, Object>>builder()
				.results(results)
				.build();
		elasticPage.setTotalCount((int) total);
		elasticPage.setPageSize(page.getPageSize());
		elasticPage.setPageNum(page.getPageNum());
		return elasticPage;
	}
	
	/**
	 * 添加子聚合
	 * <p>
	 * 在当前 Terms 聚合的基础上添加子聚合，可以实现多层嵌套聚合<br/>
	 * 例如：在按类别聚合的基础上，再按日期进行子聚合
	 * <p>
	 * 支持的子聚合类型包括：
	 * <ul>
	 * <li/>Metric 聚合 - sum, avg, min, max, stats 等
	 * <li/>Bucket 聚合 - terms, date_histogram, histogram 等
	 * <li/>Pipeline 聚合 - bucket_sort, bucket_selector 等
	 * </ul>
	 *
	 * @param subAggregation 子聚合对象
	 * @return ElasticTermsAggregationBuilder
	 */
	@Override
	public ElasticTermsAggregationBuilder subAggregation(SubAggregation subAggregation) {
		if (subAggregation instanceof ElasticBucketSortSubAggregation) {
			this.page = ((ElasticBucketSortSubAggregation)subAggregation).toPage();
		}
		subAggregations.add(subAggregation);
		return this;
	}
	
	/**
	 * 添加排序规则
	 * <p>
	 * 对聚合结果进行排序，可以按照 key（桶的键值）或 count（文档数量）排序<br/>
	 * 排序格式：字段1:asc,字段2:desc
	 * <p>
	 * 常用排序：
	 * <ul>
	 * <li/>_key:asc - 按桶的键值升序排序
	 * <li/>_count:desc - 按文档数量降序排序（默认）
	 * <li/>子聚合名:asc - 按子聚合的结果排序
	 * </ul>
	 *
	 * @param sort 排序规则字符串
	 * @return ElasticTermsAggregationBuilder
	 */
	public ElasticTermsAggregationBuilder sort(String sort) {
		List<SortOrder> sortOrders = SortSupport.sort(sort);
		this.sortOrders.addAll(sortOrders);
		return this;
	}
	
	/**
	 * 从 ES 8.x SearchResponse 中获取 Terms 聚合的总桶数
	 *
	 * @param searchResponse ES 8.x SearchResponse
	 * @return 总桶数
	 */
	private long termsTotalBucketsFromResponse(SearchResponse searchResponse) {
		if (searchResponse.aggregations() == null) {
			return 0;
		}
		
		Aggregate agg =
			(Aggregate) searchResponse.aggregations().get(name);
		if (agg == null) {
			return 0;
		}
		
		// 处理 StringTerms 聚合
		if (agg.isSterms()) {
			return agg.sterms().buckets().array().size();
		}
		// 处理 LongTerms 聚合
		else if (agg.isLterms()) {
			return agg.lterms().buckets().array().size();
		}
		
		return 0;
	}
	
}
