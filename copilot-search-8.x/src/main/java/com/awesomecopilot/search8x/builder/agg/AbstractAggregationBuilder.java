package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.constants.ElasticConstants;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.stream.JsonGenerator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-05-10 11:51
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class AbstractAggregationBuilder {
	
	protected static final Logger log = LoggerFactory.getLogger(AbstractAggregationBuilder.class);
	
	protected String[] indices;
	
	/**
	 * 聚合名字
	 */
	protected String name;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 */
	protected boolean fetchTotalHits = false;
	
	protected ElasticCompositeAggregationBuilder compositeAggregationBuilder;
	
	protected BaseQueryBuilder baseQueryBuilder;
	
	/**
	 * 添加的子聚合
	 */
	protected List<SubAggregation> subAggregations = new ArrayList<>();
	
	/**
	 * 从 ES 异常中提取详细错误信息(包括 root cause)
	 * <p>
	 * ES 8.x 的 ElasticsearchException.getMessage() 通常只返回 "all shards failed" 等笼统信息,
	 * 真正的错误原因(如字段类型不匹配、索引不存在等)嵌套在 error().rootCause() 中。
	 *
	 * @param e 异常
	 * @return 详细错误描述, 如果不是 ElasticsearchException 则返回普通异常消息
	 */
	protected static String extractEsErrorDetails(Exception e) {
		if (e instanceof ElasticsearchException) {
			ElasticsearchException esException = (ElasticsearchException) e;
			ErrorCause errorCause = esException.error();
			if (errorCause != null) {
				List<ErrorCause> rootCauses = errorCause.rootCause();
				if (rootCauses != null && !rootCauses.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					sb.append(esException.getMessage());
					sb.append(" | Root causes: ");
					for (int i = 0; i < rootCauses.size(); i++) {
						ErrorCause rc = rootCauses.get(i);
						if (i > 0) {
							sb.append("; ");
						}
						sb.append("[").append(rc.type()).append("] ").append(rc.reason());
					}
					return sb.toString();
				}
				// 没有 rootCause, 尝试 causedBy
				ErrorCause causedBy = errorCause.causedBy();
				if (causedBy != null) {
					return esException.getMessage() + " | Caused by: [" + causedBy.type() + "] " + causedBy.reason();
				}
				// 只有顶层 error
				return esException.getMessage() + " | [" + errorCause.type() + "] " + errorCause.reason();
			}
		}
		return e.getMessage();
	}

	protected void logDsl(SearchRequest request) {
		if (log.isDebugEnabled()) {
			try {
				StringWriter sw = new StringWriter();
				JacksonJsonpMapper mapper = new JacksonJsonpMapper();
				JsonGenerator generator = mapper.jsonProvider().createGenerator(sw);
				request.serialize(generator, mapper);
				generator.flush();
				log.debug("Aggregation DSL:\n{}", new JSONObject(sw.toString()).toString(2));
			} catch (Exception e) {
				log.debug("Aggregation DSL (raw): {}", request.toString());
			}
		}
	}
	
	protected AbstractAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		this.baseQueryBuilder = queryBuilder;
		return this;
	}
	
	protected SearchRequest.Builder searchRequestBuilder() {
		SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
		for (String index : indices) {
			requestBuilder.index(index);
		}
		if (baseQueryBuilder != null) {
			var queryBuilder = (BaseQueryBuilder) baseQueryBuilder;
			var query = ReflectionUtils.invokeMethod("buildQuery", queryBuilder);
			if (query != null) {
				requestBuilder.query((co.elastic.clients.elasticsearch._types.query_dsl.Query) query);
			}
		}
		//ES 8.x 默认track_total_hits为true, 不再需要手动设置
		return requestBuilder;
	}
	
	protected void addTotalHitsToThreadLocal(SearchResponse<Map> searchResponse) {
		if (fetchTotalHits) {
			if (searchResponse.hits() != null && searchResponse.hits().total() != null) {
				TotalHits totalHits = searchResponse.hits().total();
				ThreadContext.put(ElasticConstants.TOTAL_HITS, totalHits.value());
			}
		} else {
			ThreadContext.remove(ElasticConstants.TOTAL_HITS);
		}
	}
	
	/**
	 * 递归构建子聚合Map
	 */
	protected Map<String, Aggregation> buildSubAggregationsMap(List<SubAggregation> subs) {
		if (subs == null || subs.isEmpty()) {
			return null;
		}
		Map<String, Aggregation> subAggsMap = new HashMap<>();
		for (SubAggregation sub : subs) {
			Aggregation subAgg = sub.build();
			if (subAgg != null) {
				subAggsMap.put(sub.getName(), subAgg);
			}
		}
		return subAggsMap.isEmpty() ? null : subAggsMap;
	}
}
