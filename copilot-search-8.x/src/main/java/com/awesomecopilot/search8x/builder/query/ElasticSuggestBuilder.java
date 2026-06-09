package com.awesomecopilot.search8x.builder.query;

import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.SuggestMode;
import com.awesomecopilot.search8x.enums.SuggestSort;
import com.awesomecopilot.search8x.exception.SuggestException;
import com.awesomecopilot.search8x.support.SearchRequestSupport;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.Suggest.Suggestion;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.SortBy;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
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
	 * 直接传入已构建好的 SuggestionBuilder（兼容旧用法）
	 */
	private SuggestionBuilder suggestionBuilder;

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

	private TermSuggestionBuilder.StringDistanceImpl stringDistance;

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
	 * 兼容旧用法：直接传入已构建好的 SuggestionBuilder
	 */
	public ElasticSuggestBuilder suggestionBuilder(SuggestionBuilder suggestionBuilder) {
		this.suggestionBuilder = suggestionBuilder;
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
	 * <li/>INTERNAL - 基于 Damerau-Levenshtein 算法，但针对性能进行了优化（默认）
	 * <li/>DAMERAU_LEVENSHTEIN - 标准的 Damerau-Levenshtein 距离算法
	 * <li/>LEVENSHTEIN - 标准的 Levenshtein 编辑距离算法
	 * <li/>JARO_WINKLER - Jaro-Winkler 相似度算法
	 * <li/>NGRAM - 基于 N-gram 的相似度算法
	 * </ul>
	 * 不同算法适用于不同场景，INTERNAL 通常是性能和准确性的最佳平衡
	 *
	 * @param stringDistance 字符串距离算法
	 * @return ElasticSuggestBuilder
	 */
	public ElasticSuggestBuilder stringDistance(TermSuggestionBuilder.StringDistanceImpl stringDistance) {
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
	 *     .stringDistance(INTERNAL)
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
	public Set<String> suggest() {
		if (isBlank(name)) {
			throw new SuggestException("Every suggestion need a name!");
		}

		// 如果没有直接传入 suggestionBuilder，则根据参数自动构建
		if (suggestionBuilder == null) {
			suggestionBuilder = buildSuggestionBuilder();
		}

		SuggestBuilder suggestBuilder = new SuggestBuilder();
		suggestBuilder.addSuggestion(name, suggestionBuilder);
		if (log.isDebugEnabled()) {
			log.debug("Suggest DSL:\n {}", suggestBuilder.toString());
		}
		SearchResponse searchResponse = SearchRequestSupport.search(ElasticUtils.QUERY_CLIENT, indices,
				sourceBuilder -> sourceBuilder.suggest(suggestBuilder));

		Suggest suggest = searchResponse.getSuggest();
		Set<String> suggesters = new HashSet<>();

		if (suggest == null) {
			return suggesters;
		}

		Suggestion suggestion = suggest.getSuggestion(name);

		if (suggestion instanceof TermSuggestion) {
			List<TermSuggestion.Entry> entries = ((TermSuggestion) suggestion).getEntries();
			for (TermSuggestion.Entry entry : entries) {
				entry.getOptions().forEach(option -> suggesters.add(option.getText().string()));
			}
			return suggesters;
		}

		if (suggestion instanceof PhraseSuggestion) {
			List<PhraseSuggestion.Entry> entries = ((PhraseSuggestion) suggestion).getEntries();
			for (PhraseSuggestion.Entry entry : entries) {
				entry.getOptions().forEach(option -> suggesters.add(option.getHighlighted().string()));
			}
			return suggesters;
		}

		if (suggestion instanceof CompletionSuggestion) {
			List<CompletionSuggestion.Entry> entries = ((CompletionSuggestion) suggestion).getEntries();
			for (CompletionSuggestion.Entry entry : entries) {
				entry.getOptions().forEach(option -> suggesters.add(option.getText().string()));
			}
			return suggesters;
		}

		return suggesters;
	}

	/**
	 * 根据设置的参数自动判断并构建对应类型的 SuggestionBuilder
	 * <p>
	 * 判断逻辑：
	 * <ul>
	 * <li/>如果设置了 prefix 参数 → 构建 CompletionSuggestionBuilder（自动补全）
	 * <li/>如果设置了 highlightPreTag 或 highlightPostTag → 构建 PhraseSuggestionBuilder（短语建议）
	 * <li/>其他情况 → 构建 TermSuggestionBuilder（词项建议，默认）
	 * </ul>
	 *
	 * @return 对应类型的 SuggestionBuilder
	 * @throws SuggestException 如果 field 未设置
	 */
	private SuggestionBuilder buildSuggestionBuilder() {
		if (isBlank(field)) {
			throw new SuggestException("field is required!");
		}

		// Completion suggestion
		if (!isBlank(prefix)) {
			return SuggestBuilders.completionSuggestion(field).prefix(prefix);
		}

		// Phrase suggestion
		if (highlightPreTag != null || highlightPostTag != null) {
			PhraseSuggestionBuilder builder = SuggestBuilders.phraseSuggestion(field)
					.text(text)
					.maxErrors(maxErrors)
					.confidence(confidence);
			if (highlightPreTag != null && highlightPostTag != null) {
				builder.highlight(highlightPreTag, highlightPostTag);
			}
			return builder;
		}

		// Term suggestion（默认）
		TermSuggestionBuilder builder = SuggestBuilders.termSuggestion(field)
				.text(text)
				.prefixLength(prefixLength)
				.sort(toEsSortBy(sort))
				.suggestMode(toEsSuggestMode(suggestMode));
		if (stringDistance != null) {
			builder.stringDistance(stringDistance);
		}
		return builder;
	}

	/**
	 * 将自定义的排序枚举转换为 Elasticsearch 原生的 SortBy 类型
	 *
	 * @param sort 自定义排序枚举
	 * @return Elasticsearch 的 SortBy 枚举
	 */
	private SortBy toEsSortBy(SuggestSort sort) {
		if (sort == SuggestSort.SCORE) {
			return SortBy.SCORE;
		}
		return SortBy.FREQUENCY;
	}

	/**
	 * 将自定义的建议模式枚举转换为 Elasticsearch 原生的 SuggestMode 类型
	 *
	 * @param mode 自定义建议模式枚举
	 * @return Elasticsearch 的 TermSuggestionBuilder.SuggestMode 枚举
	 */
	private TermSuggestionBuilder.SuggestMode toEsSuggestMode(SuggestMode mode) {
		switch (mode) {
			case ALWAYS:  return TermSuggestionBuilder.SuggestMode.ALWAYS;
			case POPULAR: return TermSuggestionBuilder.SuggestMode.POPULAR;
			default:      return TermSuggestionBuilder.SuggestMode.MISSING;
		}
	}
}