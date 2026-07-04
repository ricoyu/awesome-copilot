package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.CompletionContext;
import co.elastic.clients.elasticsearch.core.search.Context;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.SuggestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 基于上下文的自动完成
 * <p>
 * Copyright: (C), 2021-02-16 9:33
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticContextSuggestBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticContextSuggestBuilder.class);

	/**
	 * 索引名
	 */
	private String[] indices;

	/**
	 * 要在哪个字段上实现自动完成
	 */
	private String field;

	/**
	 * 用户的输入, 前缀匹配
	 */
	private String prefix;

	/**
	 * 要实现基于上下文的自动完成, 用户需要先为Index建立Mapping
	 * 下面这个Mapping设置了字段comment_autocomplete提供上下文的自动完成
	 * 其type是category
	 * <pre>
	 * {
	 *   "properties": {
	 *     "comment_autocomplete": {
	 *       "type": "completion",
	 *       "contexts": [
	 *         {
	 *           "type": "category",
	 *           "name": "comment_category"
	 *         }
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * 下面插入两篇文档, 一篇category设为movies, 一篇coffee
	 * <pre>
	 * {
	 *   "comment": "I love the star war movies",
	 *   "comment_autocomplete": {
	 *     "input": [
	 *       "star wars"
	 *     ],
	 *     "contexts": {
	 *       "comment_category": "movies"
	 *     }
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * {
	 *   "comment": "Where can I find a Starbucks",
	 *   "comment_autocomplete": {
	 *     "input": [
	 *       "starbucks"
	 *     ],
	 *     "contexts": {
	 *       "comment_category": "coffee"
	 *     }
	 *   }
	 * }
	 * <p>
	 * 这里category属性就对应movies和coffee这两个值
	 */
	private String category;

	private String categoryName;

	/**
	 * 这个suggest的名字
	 */
	private String name;

	public ElasticContextSuggestBuilder(String... indices) {
		this.indices = indices;
	}

	/**
	 * 这个suggest的名字, 必须提供
	 *
	 * @param name
	 * @return
	 */
	public ElasticContextSuggestBuilder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * 要实现基于上下文的自动完成, 用户需要先为Index建立Mapping<p/>
	 * 下面这个Mapping设置了字段comment_autocomplete提供上下文的自动完成
	 * 其type是category
	 * <pre>
	 * {
	 *   "properties": {
	 *     "comment_autocomplete": {
	 *       "type": "completion",
	 *       "contexts": [{
	 *         "type": "category",
	 *         "name": "comment_category"
	 *       }]
	 *     }
	 *   }
	 * }
	 * </pre>
	 * 下面插入两篇文档, 一篇category设为movies, 一篇coffee
	 * <pre>
	 * {
	 *   "comment": "I love the star war movies",
	 *   "comment_autocomplete": {
	 *     "input": ["star wars"],
	 *     "contexts": {
	 *       "comment_category": "movies"
	 *     }
	 *   }
	 * }
	 * </pre>
	 * <pre>
	 * {
	 *   "comment": "Where can I find a Starbucks",
	 *   "comment_autocomplete": {
	 *     "input": ["starbucks"],
	 *     "contexts": {
	 *       "comment_category": "coffee"
	 *     }
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * 这里category属性就对应movies和coffee这两个值
	 *
	 * @param category
	 * @return
	 */
	public ElasticContextSuggestBuilder category(String category) {
		this.category = category;
		return this;
	}

	/**
	 * 定义的Mapping中context的名字, 比如comment_category
	 * <pre>
	 * {
	 *   "properties": {
	 *     "comment_autocomplete": {
	 *       "type": "completion",
	 *       "contexts": [{
	 *         "type": "category",
	 *         "name": "comment_category"
	 *       }]
	 *     }
	 *   }
	 * }
	 * </pre>
	 * 因为一个字段可以指定多个context, 所以这里要指定具体哪一个context
	 *
	 * @param categoryName
	 * @return
	 */
	public ElasticContextSuggestBuilder categoryName(String categoryName) {
		this.categoryName = categoryName;
		return this;
	}

	/**
	 * 用户的输入, 前缀匹配
	 *
	 * @param prefix
	 * @return
	 */
	public ElasticContextSuggestBuilder prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * 要实现自动完成的字段, Mapping中type是completion, 比如下面的Mapping的话就是 comment_autocomplete
	 * <pre>
	 * PUT comments/_mapping
	 * {
	 *   "properties": {
	 *     "comment_autocomplete": {
	 *       "type": "completion", 
	 *       "contexts": [{
	 *         "type": "category", 
	 *         "name": "comment_category"
	 *       }]
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param field
	 * @return
	 */
	public ElasticContextSuggestBuilder field(String field) {
		this.field = field;
		return this;
	}

	/**
	 * 执行查询, 返回suggest信息
	 *
	 * @return Set<String>
	 */
	@SuppressWarnings("unchecked")
	public Set<String> suggest() {
		if (isBlank(name)) {
			throw new SuggestException("Every suggestion need a name!");
		}

		if (isBlank(field)) {
			throw new SuggestException("field is required!");
		}

		ElasticsearchClient client = ElasticUtils.QUERY_CLIENT;

		try {
			SearchResponse<Map> response = client.search(SearchRequest.of(s -> {
				s.index(List.of(indices));
				s.suggest(Suggester.of(sg -> {
					sg.suggesters(name, FieldSuggester.of(f -> f
							.prefix(prefix)
							.completion(c -> {
								c.field(field);
								// 设置上下文: categoryName -> category
								CompletionContext context = CompletionContext.of(ctx -> ctx
										.context(ctxBuilder -> ctxBuilder.category(category))
								);
								c.contexts(categoryName, List.of(context));
								return c;
							})
					));
					return sg;
				}));
				return s;
			}), (Class<Map>) (Class<?>) Map.class);

			Set<String> suggesters = new HashSet<>();

			Map<String, List<Suggestion<Map>>> suggestMap = response.suggest();
			if (suggestMap == null || suggestMap.isEmpty()) {
				return suggesters;
			}

			List<Suggestion<Map>> suggestionList = suggestMap.get(name);
			if (suggestionList == null || suggestionList.isEmpty()) {
				return suggesters;
			}

			Suggestion<Map> suggestion = suggestionList.get(0);

			// Completion suggestion results
			if (suggestion.isCompletion()) {
				CompletionSuggest<Map> completionSuggest = suggestion.completion();
				List<CompletionSuggestOption<Map>> completionOptions = completionSuggest.options();
				for (CompletionSuggestOption<Map> option : completionOptions) {
					suggesters.add(option.text());
				}
			}

			return suggesters;
		} catch (IOException e) {
			throw new RuntimeException("Failed to execute context suggest query", e);
		}
	}
}
