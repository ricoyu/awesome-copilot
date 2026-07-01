package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.PhraseSuggest;
import co.elastic.clients.elasticsearch.core.search.PhraseSuggestOption;
import co.elastic.clients.elasticsearch.core.search.StringDistance;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TermSuggest;
import co.elastic.clients.elasticsearch.core.search.TermSuggestOption;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.SuggestMode;
import com.awesomecopilot.search8x.enums.SuggestSort;
import com.awesomecopilot.search8x.exception.SuggestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.awesomecopilot.search8x.enums.SuggestMode.MISSING;
import static com.awesomecopilot.search8x.enums.SuggestSort.FREQUENCY;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * <p>
 * Copyright: (C), 2021-02-13 20:55
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticSuggestBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticSuggestBuilder.class);

	private String[] indices;

	/**
	 * 给这个suggestion起一个名字
	 */
	private String name;

	/**
	 * The suggest text is a required option that needs to be set globally or per suggestion.
	 */
	private String text;

	/**
	 * The field to fetch the candidate suggestions from.
	 */
	private String field;

	/**
	 * The number of minimal prefix characters that must match in order be a candidate for suggestions. Defaults to 1.
	 */
	private int prefixLength = 1;

	/**
	 * 按照score还是文档的frequency来排序
	 */
	private SuggestSort sort = FREQUENCY;

	private SuggestMode suggestMode = MISSING;

	private String stringDistance;

	/**
	 * Phrase suggestion: max number of errors
	 */
	private float maxErrors = 2f;

	/**
	 * Phrase suggestion: confidence threshold
	 */
	private float confidence = 0f;

	/**
	 * Phrase suggestion: highlight tags
	 */
	private String highlightPreTag;
	private String highlightPostTag;

	/**
	 * Completion suggestion: prefix
	 */
	private String prefix;

	public ElasticSuggestBuilder(String... indices) {
		this.indices = indices;
	}

	/**
	 * 设置建议查询的名称
	 * <p>
	 * 每个 suggestion 都需要一个唯一的名称来标识，在返回结果时通过这个名称来获取对应的建议结果
	 *
	 * @param name 建议查询的名称，不能为空
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * 设置要查询建议的字段
	 * <p>
	 * 指定从哪个字段获取候选建议，这是一个必填参数
	 *
	 * @param field 字段名称，不能为空
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder field(String field) {
		this.field = field;
		return this;
	}

	/**
	 * 设置要进行拼写纠错或建议的文本
	 * <p>
	 * 用于 Term Suggestion 和 Phrase Suggestion，指定需要纠错的原始文本<br/>
	 * 例如：text("lucen hocks") 会建议 "lucene" 和 "rocks"
	 * <p>
	 * 注意：Completion Suggestion 不使用此参数，而是使用 prefix() 方法
	 *
	 * @param text 要纠错的文本
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder text(String text) {
		this.text = text;
		return this;
	}

	/**
	 * 设置最小前缀匹配长度
	 * <p>
	 * 指定必须匹配的最小前缀字符数才能成为候选建议，默认为 1<br/>
	 * 例如：prefixLength(2) 表示至少前 2 个字符必须匹配
	 * <p>
	 * 较大的值可以提高性能但可能减少建议数量
	 *
	 * @param prefixLength 最小前缀长度，必须大于 0
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder prefixLength(int prefixLength) {
		this.prefixLength = prefixLength;
		return this;
	}

	/**
	 * 设置建议结果的排序方式
	 * <p>
	 * 可选值：
	 * <ul>
	 * <li/>SCORE - 按照相关性分数排序
	 * <li/>FREQUENCY - 按照词项在文档中出现的频率排序（默认）
	 * </ul>
	 * 通常使用 FREQUENCY 可以获得更常见、更可能正确的建议
	 *
	 * @param sort 排序方式
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder sort(SuggestSort sort) {
		this.sort = sort;
		return this;
	}

	/**
	 * 设置建议模式
	 * <p>
	 * 控制何时返回建议结果，可选值：
	 * <ul>
	 * <li/>MISSING - 仅当索引中不存在该词项时才返回建议（默认）
	 * <li/>POPULAR - 仅返回比原词项出现频率更高的建议
	 * <li/>ALWAYS - 总是返回建议，无论原词项是否存在
	 * </ul>
	 * 推荐使用 POPULAR 模式，可以避免将正确的词纠正为错误的词
	 *
	 * @param suggestMode 建议模式
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder suggestMode(SuggestMode suggestMode) {
		this.suggestMode = suggestMode;
		return this;
	}

	/**
	 * 设置字符串距离算法
	 * <p>
	 * 用于计算原词项与候选建议之间的相似度，可选值：
	 * <ul>
	 * <li/>internal - 基于 Damerau-Levenshtein 算法，但针对性能进行了优化（默认）
	 * <li/>damerau_levenshtein - 标准的 Damerau-Levenshtein 距离算法
	 * <li/>levenshtein - 标准的 Levenshtein 编辑距离算法
	 * <li/>jaro_winkler - Jaro-Winkler 相似度算法
	 * <li/>ngram - 基于 N-gram 的相似度算法
	 * </ul>
	 * 不同算法适用于不同场景，internal 通常是性能和准确性的最佳平衡
	 *
	 * @param stringDistance 字符串距离算法
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder stringDistance(String stringDistance) {
		this.stringDistance = stringDistance;
		return this;
	}

	/**
	 * Phrase suggestion 专用：最大错误数
	 */
	public ElasticSuggestBuilder maxErrors(float maxErrors) {
		this.maxErrors = maxErrors;
		return this;
	}

	/**
	 * Phrase suggestion 专用：置信度
	 */
	public ElasticSuggestBuilder confidence(float confidence) {
		this.confidence = confidence;
		return this;
	}

	/**
	 * Phrase suggestion 专用：高亮标签
	 */
	public ElasticSuggestBuilder highlight(String preTag, String postTag) {
		this.highlightPreTag = preTag;
		this.highlightPostTag = postTag;
		return this;
	}

	/**
	 * Completion suggestion 专用：前缀匹配
	 */
	public ElasticSuggestBuilder prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * 执行建议查询并返回建议结果
	 * <p>
	 * 根据之前设置的参数自动判断并执行对应类型的 Suggestion：
	 * <ul>
	 * <li/>Term Suggestion - 基于编辑距离的拼写纠错，适用于单个词项的纠错
	 * <li/>Phrase Suggestion - 基于上下文的短语纠错，考虑词组之间的关系
	 * <li/>Completion Suggestion - 基于前缀的自动补全，适用于搜索框的实时建议
	 * </ul>
	 * <p>
	 * 使用示例：
	 * <pre>
	 * // Term Suggestion 示例
	 * Set&lt;String&gt; suggesters = ElasticUtils.suggest("articles")
	 *     .field("body")
	 *     .text("lucen hocks")
	 *     .name("term-suggestion")
	 *     .suggestMode(POPULAR)
	 *     .stringDistance("internal")
	 *     .suggest();
	 *
	 * // Phrase Suggestion 示例
	 * Set&lt;String&gt; suggesters = ElasticUtils.suggest("articles")
	 *     .field("body")
	 *     .text("lucen rock")
	 *     .name("phrase-suggestion")
	 *     .highlight("&lt;em&gt;", "&lt;/em&gt;")
	 *     .suggest();
	 *
	 * // Completion Suggestion 示例
	 * Set&lt;String&gt; suggesters = ElasticUtils.suggest("articles")
	 *     .field("title.completion")
	 *     .prefix("lucen")
	 *     .name("completion-suggestion")
	 *     .suggest();
	 * </pre>
	 *
	 * @return 建议结果集合，如果没有建议则返回空集合
	 * @throws SuggestException 如果 name 或 field 未设置
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
					sg.text(text);

					// Completion suggestion
					if (!isBlank(prefix)) {
						sg.suggesters(name, FieldSuggester.of(f -> f
								.prefix(prefix)
								.completion(c -> c.field(field))
						));
					}
					// Phrase suggestion
					else if (highlightPreTag != null || highlightPostTag != null) {
						sg.suggesters(name, FieldSuggester.of(f -> f
								.phrase(p -> {
									p.field(field)
											.maxErrors((double) maxErrors)
											.confidence((double) confidence);
									if (highlightPreTag != null && highlightPostTag != null) {
										p.highlight(h -> h
												.preTag(highlightPreTag)
												.postTag(highlightPostTag));
									}
									return p;
								})
						));
					}
					// Term suggestion（默认）
					else {
						sg.suggesters(name, FieldSuggester.of(f -> f
								.term(t -> {
									t.field(field)
											.prefixLength(prefixLength)
											.sort(toEsSortBy(sort))
											.suggestMode(toEsSuggestMode(suggestMode));
									if (stringDistance != null) {
										t.stringDistance(toEsStringDistance(stringDistance));
									}
									return t;
								})
						));
					}

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

			// Term suggestion results
			if (suggestion.isTerm()) {
				TermSuggest termSuggest = suggestion.term();
				List<TermSuggestOption> termOptions = termSuggest.options();
				for (TermSuggestOption option : termOptions) {
					suggesters.add(option.text());
				}
				return suggesters;
			}

			// Phrase suggestion results
			if (suggestion.isPhrase()) {
				PhraseSuggest phraseSuggest = suggestion.phrase();
				List<PhraseSuggestOption> phraseOptions = phraseSuggest.options();
				for (PhraseSuggestOption option : phraseOptions) {
					String highlighted = option.highlighted();
					suggesters.add(highlighted != null ? highlighted : option.text());
				}
				return suggesters;
			}

			// Completion suggestion results
			if (suggestion.isCompletion()) {
				CompletionSuggest<Map> completionSuggest = suggestion.completion();
				List<CompletionSuggestOption<Map>> completionOptions = completionSuggest.options();
				for (CompletionSuggestOption<Map> option : completionOptions) {
					suggesters.add(option.text());
				}
				return suggesters;
			}

			return suggesters;
		} catch (IOException e) {
			throw new RuntimeException("Failed to execute suggest query", e);
		}
	}

	/**
	 * 将自定义的排序枚举转换为 Elasticsearch 原生的 SuggestSort 类型
	 *
	 * @param sort 自定义排序枚举
	 * @return Elasticsearch 的 SuggestSort 枚举
	 */
	private co.elastic.clients.elasticsearch.core.search.SuggestSort toEsSortBy(SuggestSort sort) {
		if (sort == SuggestSort.SCORE) {
			return co.elastic.clients.elasticsearch.core.search.SuggestSort.Score;
		}
		return co.elastic.clients.elasticsearch.core.search.SuggestSort.Frequency;
	}

	/**
	 * 将自定义的建议模式枚举转换为 Elasticsearch 原生的 SuggestMode 类型
	 *
	 * @param mode 自定义建议模式枚举
	 * @return Elasticsearch 的 SuggestMode 枚举
	 */
	private co.elastic.clients.elasticsearch._types.SuggestMode toEsSuggestMode(SuggestMode mode) {
		switch (mode) {
			case ALWAYS:  return co.elastic.clients.elasticsearch._types.SuggestMode.Always;
			case POPULAR: return co.elastic.clients.elasticsearch._types.SuggestMode.Popular;
			default:      return co.elastic.clients.elasticsearch._types.SuggestMode.Missing;
		}
	}

	/**
	 * 将字符串距离算法名称转换为 Elasticsearch 原生的 StringDistance 枚举
	 *
	 * @param distance 字符串距离算法名称
	 * @return Elasticsearch 的 StringDistance 枚举
	 */
	private StringDistance toEsStringDistance(String distance) {
		switch (distance) {
			case "damerau_levenshtein": return StringDistance.DamerauLevenshtein;
			case "levenshtein":         return StringDistance.Levenshtein;
			case "jaro_winkler":        return StringDistance.JaroWinkler;
			case "ngram":               return StringDistance.Ngram;
			default:                    return StringDistance.Internal;
		}
	}
}
