package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.search8x.exception.ElasticQueryException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 搜索请求执行：查询走 elasticsearch-java 8.x，聚合仍走 RestHighLevelClient。
 */
public final class SearchRequestSupport {

	private SearchRequestSupport() {
	}

	// ---------- 查询模块 (elasticsearch-java 8.x) ----------

	public static SearchResponse search(ElasticsearchClient client, String index,
			java.util.function.Consumer<SearchSourceBuilder> configurer) {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		configurer.accept(sourceBuilder);
		return search(client, new String[] {index}, sourceBuilder);
	}

	public static SearchResponse search(ElasticsearchClient client, String[] indices, SearchSourceBuilder sourceBuilder) {
		return SearchResponseBridge.search(client, indices, sourceBuilder);
	}

	public static SearchResponse search(ElasticsearchClient client, String[] indices, SearchOptions options) {
		if (options.getScrollId() != null) {
			TimeValue keepAlive = options.getScrollKeepAlive() != null ? parseTimeValue(options.getScrollKeepAlive()) : null;
			return SearchResponseBridge.scroll(client, options.getScrollId(), keepAlive);
		}
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		options.toConfigurer().configure(sourceBuilder);
		TimeValue scroll = options.getScrollKeepAlive() != null ? parseTimeValue(options.getScrollKeepAlive()) : null;
		return SearchResponseBridge.search(client, indices, sourceBuilder, scroll);
	}

	public static SearchResponse search(ElasticsearchClient client, String[] indices, SearchSourceConfigurer configurer) {
		SearchOptions options = new SearchOptions();
		options.configurer = configurer;
		return search(client, indices, options);
	}

	// ---------- 聚合模块 (RestHighLevelClient，暂保留) ----------

	public static SearchResponse search(RestHighLevelClient client, String index,
			java.util.function.Consumer<SearchSourceBuilder> configurer) {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		configurer.accept(sourceBuilder);
		return search(client, new String[] {index}, sourceBuilder);
	}

	public static SearchResponse search(RestHighLevelClient client, String[] indices, SearchSourceBuilder sourceBuilder) {
		try {
			SearchRequest searchRequest = new SearchRequest(indices);
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			searchRequest.source(sourceBuilder);
			return client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	public static SearchResponse search(RestHighLevelClient client, String[] indices, SearchOptions options) {
		try {
			if (options.getScrollId() != null) {
				SearchScrollRequest scrollRequest = new SearchScrollRequest(options.getScrollId());
				if (options.getScrollKeepAlive() != null) {
					scrollRequest.scroll(parseTimeValue(options.getScrollKeepAlive()));
				}
				return client.scroll(scrollRequest, RequestOptions.DEFAULT);
			}

			SearchRequest searchRequest = new SearchRequest(indices);
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
			options.toConfigurer().configure(sourceBuilder);
			searchRequest.source(sourceBuilder);
			if (options.getScrollKeepAlive() != null) {
				searchRequest.scroll(parseTimeValue(options.getScrollKeepAlive()));
			}
			return client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	@FunctionalInterface
	public interface SearchSourceConfigurer {
		void configure(SearchSourceBuilder sourceBuilder);
	}

	/**
	 * 将时间字符串转换为 TimeValue
	 * 支持格式: "30s", "1m", "500ms", "2h" 等
	 */
	private static TimeValue parseTimeValue(String timeString) {
		return TimeValue.parseTimeValue(timeString, "scroll");
	}

	public static final class SearchOptions {
		private SearchSourceConfigurer configurer;
		private QueryBuilder query;
		private boolean trackTotalHits = true;
		private Integer from;
		private Integer size;
		private boolean fetchSource = true;
		private String[] includeSource;
		private String[] excludeSource;
		private Object[] searchAfter;
		private String scrollKeepAlive; // 使用时间字符串格式,如 "30s", "1m"
		private String scrollId;
		private HighlightBuilder highlightBuilder;
		private String[] storedFields;
		private List<SortBuilder<?>> sorts;
		private java.util.Map<String, Script> scriptFields = new java.util.HashMap<>();

		public String getScrollId() {
			return scrollId;
		}

		public String getScrollKeepAlive() {
			return scrollKeepAlive;
		}

		public SearchOptions query(QueryBuilder query) {
			this.query = query;
			return this;
		}

		public SearchOptions trackTotalHits(boolean trackTotalHits) {
			this.trackTotalHits = trackTotalHits;
			return this;
		}

		public SearchOptions from(Integer from) {
			this.from = from;
			return this;
		}

		public SearchOptions size(Integer size) {
			this.size = size;
			return this;
		}

		public SearchOptions fetchSource(boolean fetchSource) {
			this.fetchSource = fetchSource;
			return this;
		}

		public SearchOptions includeSource(String[] includeSource) {
			this.includeSource = includeSource;
			return this;
		}

		public SearchOptions excludeSource(String[] excludeSource) {
			this.excludeSource = excludeSource;
			return this;
		}

		public SearchOptions searchAfter(Object[] searchAfter) {
			this.searchAfter = searchAfter;
			return this;
		}

		public SearchOptions scroll(String scrollKeepAlive) {
			this.scrollKeepAlive = scrollKeepAlive;
			return this;
		}

		public SearchOptions scrollId(String scrollId) {
			this.scrollId = scrollId;
			return this;
		}

		public SearchOptions highlight(HighlightBuilder highlightBuilder) {
			this.highlightBuilder = highlightBuilder;
			return this;
		}

		public SearchOptions storedFields(String[] storedFields) {
			this.storedFields = storedFields;
			return this;
		}

		public SearchOptions sorts(List<SortBuilder<?>> sorts) {
			this.sorts = sorts;
			return this;
		}

		public SearchOptions scriptField(String name, Script script) {
			this.scriptFields.put(name, script);
			return this;
		}

		public SearchSourceConfigurer toConfigurer() {
			return sourceBuilder -> {
				if (configurer != null) {
					configurer.configure(sourceBuilder);
					return;
				}
				if (query != null) {
					sourceBuilder.query(query);
				}
				sourceBuilder.trackTotalHits(trackTotalHits);
				if (from != null) {
					sourceBuilder.from(from);
				}
				if (size != null) {
					sourceBuilder.size(size);
				}
				if (searchAfter != null && searchAfter.length > 0) {
					sourceBuilder.searchAfter(searchAfter);
				}
				if (sorts != null) {
					sorts.forEach(sourceBuilder::sort);
				}
				if (highlightBuilder != null) {
					sourceBuilder.highlighter(highlightBuilder);
				}
				if (storedFields != null) {
					sourceBuilder.storedFields(Arrays.asList(storedFields));
				}
				if (!scriptFields.isEmpty()) {
					scriptFields.forEach(sourceBuilder::scriptField);
					sourceBuilder.fetchSource(false);
				} else if (includeSource != null && includeSource.length > 0
						|| excludeSource != null && excludeSource.length > 0) {
					sourceBuilder.fetchSource(includeSource, excludeSource);
				} else {
					sourceBuilder.fetchSource(fetchSource);
				}
			};
		}
	}
}
