package com.awesomecopilot.search8x.builder.query;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.builder.ElasticRangeQueryBuilder;
import com.awesomecopilot.search8x.enums.BoolQueryType;
import com.awesomecopilot.search8x.enums.Direction;
import com.awesomecopilot.search8x.enums.SortOrder;
import com.awesomecopilot.search8x.support.SortSupport;
import lombok.Data;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.awesomecopilot.search8x.enums.BoolQueryType.FILTER;
import static com.awesomecopilot.search8x.enums.BoolQueryType.MUST;
import static com.awesomecopilot.search8x.enums.BoolQueryType.MUST_NOT;
import static com.awesomecopilot.search8x.enums.BoolQueryType.SHOULD;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * <p>
 * Copyright: (C), 2021-06-06 21:24
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBoolQueryBuilder extends BaseQueryBuilder {

	/**
	 * 嵌套查询的字段
	 */
	private String nestedPath;

	/**
	 * 如果用户给定 5 个查询词项, 想查找只包含其中 4 个的文档, 该如何处理？<p>
	 * match 查询支持 minimum_should_match 最小匹配参数, 这让我们可以指定必须匹配的词项数用来表示一个文档是否相关。
	 * 我们可以将其设置为某个具体数字, 更常用的做法是将其设置为一个百分数, 因为我们无法控制用户搜索时输入的单词数量:
	 * <p>
	 * 比如我们搜索 Once Upon a Time in the Midlands<p>
	 * 如果是standard分词器, 分词后得到的词项为: "once", "upon", "a", "time", "in", "the", "midlands"<p>
	 * 所以 minimum_should_match: 7 或者 minimum_should_match: "100%" 表示这些词都要包含<p>
	 * minimum_should_match: 6 就表示文本中这个字段少一个词项的也可以搜索到
	 */
	private Object minimumShouldMatch;
	
	private List<Node> queryBuilders = new ArrayList<>();
	
	@Data
	private static class Node{
		
		private BoolQueryType type;
		
		private QueryBuilder builder;
		
		public Node(BoolQueryType type, QueryBuilder builder) {
			this.type = type;
			this.builder = builder;
		}
	}
	
	public ElasticBoolQueryBuilder(String... indices) {
		this.indices = indices;	
	}

	/**
	 * 设置嵌套查询字段
	 *
	 * @param path
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticBoolQueryBuilder nestedPath(String path) {
		this.nestedPath = path;
		return this;
	}

	/**
	 * 添加 must 条件到布尔查询
	 * <p>
	 * must 条件表示文档必须匹配该查询，相当于逻辑 AND 操作<br/>
	 * must 条件会参与相关性算分，影响文档的 _score
	 * <p>
	 * 使用场景：需要强制匹配且关心相关性排序的查询条件
	 *
	 * @param queryBuilder 查询构建器
	 * @return ElasticBoolQueryBuilder
	 */
	public ElasticBoolQueryBuilder must(QueryBuilder queryBuilder) {
		this.queryBuilders.add(new Node(MUST, queryBuilder));
		return this;
	}
	
	/**
	 * 添加 must_not 条件到布尔查询
	 * <p>
	 * must_not 条件表示文档必须不匹配该查询，相当于逻辑 NOT 操作<br/>
	 * must_not 条件不参与相关性算分，在 Filter Context 中执行
	 * <p>
	 * 使用场景：需要排除某些文档的查询条件，例如排除已删除的文档
	 *
	 * @param queryBuilder 查询构建器
	 * @return ElasticBoolQueryBuilder
	 */
	public ElasticBoolQueryBuilder mustNot(QueryBuilder queryBuilder) {
		this.queryBuilders.add(new Node(MUST_NOT, queryBuilder));
		return this;
	}
	
	/**
	 * 添加 should 条件到布尔查询
	 * <p>
	 * should 条件表示文档应该匹配该查询，相当于逻辑 OR 操作<br/>
	 * should 条件会参与相关性算分，匹配的 should 条件越多，_score 越高
	 * <p>
	 * 注意：
	 * <ul>
	 * <li/>如果布尔查询中没有 must 或 filter 条件，则至少需要匹配一个 should 条件
	 * <li/>如果布尔查询中有 must 或 filter 条件，则 should 条件变为可选，仅影响算分
	 * <li/>可以通过 minimumShouldMatch() 方法控制最少需要匹配的 should 条件数量
	 * </ul>
	 * 使用场景：多个可选的查询条件，匹配越多相关性越高
	 *
	 * @param queryBuilder 查询构建器
	 * @return ElasticBoolQueryBuilder
	 */
	public ElasticBoolQueryBuilder should(QueryBuilder queryBuilder) {
		this.queryBuilders.add(new Node(SHOULD, queryBuilder));
		return this;
	}
	
	/**
	 * 添加 filter 条件到布尔查询
	 * <p>
	 * filter 条件表示文档必须匹配该查询，相当于逻辑 AND 操作<br/>
	 * filter 条件不参与相关性算分，在 Filter Context 中执行，性能更好且结果可缓存
	 * <p>
	 * filter 与 must 的区别：
	 * <ul>
	 * <li/>filter 不计算 _score，性能更好，适合精确匹配和范围查询
	 * <li/>must 会计算 _score，适合需要相关性排序的全文检索
	 * </ul>
	 * 使用场景：不需要算分的过滤条件，如状态、时间范围、分类等精确匹配
	 *
	 * @param queryBuilder 查询构建器
	 * @return ElasticBoolQueryBuilder
	 */
	public ElasticBoolQueryBuilder filter(QueryBuilder queryBuilder) {
		this.queryBuilders.add(new Node(FILTER, queryBuilder));
		return this;
	}
	
	/**
	 * 创建 term 精确查询并返回 BoolTermQuery 接口
	 * <p>
	 * term 查询用于精确匹配，不会对查询词进行分词<br/>
	 * 适用于 keyword、数字、日期、布尔值等不分词字段
	 * <p>
	 * 返回 BoolTermQuery 接口，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param field 要查询的字段名
	 * @param value 要匹配的精确值
	 * @return BoolTermQuery 接口，支持继续添加布尔查询条件
	 */
	public BoolTermQuery term(String field, Object value) {
		ElasticTermQueryBuilder termQueryBuilder = new ElasticTermQueryBuilder();
		termQueryBuilder.query(field, value);
		ReflectionUtils.setField("boolQueryBuilder", termQueryBuilder, this);
		return termQueryBuilder;
	}
	
	/**
	 * 创建 terms 多值查询并返回 BoolQuery 接口
	 * <p>
	 * terms 查询用于匹配多个精确值中的任意一个，相当于 SQL 中的 IN 操作<br/>
	 * 不会对查询词进行分词，适用于 keyword、数字等不分词字段
	 * <p>
	 * 返回 BoolQuery 接口，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param field  要查询的字段名
	 * @param values 要匹配的值列表，可以是数组形式 "val1", "val2"，也可以是一个 List 类型
	 * @return BoolQuery 接口，支持继续添加布尔查询条件
	 */
	public BoolQuery terms(String field, Object... values) {
		if (values != null && values.length == 1) {
			if (values[0] instanceof Collection) {
				values = ((Collection)values[0]).stream().toArray(Object[]::new);
			}
		}
		ElasticTermsQueryBuilder termsQueryBuilder = new ElasticTermsQueryBuilder();
		termsQueryBuilder.query(field, values);
		ReflectionUtils.setField("boolQueryBuilder", termsQueryBuilder, this);
		return termsQueryBuilder;
	}
	
	/**
	 * 创建 match 全文查询并返回 BoolMatchQuery 接口
	 * <p>
	 * match 查询用于全文检索，会对查询词进行分词，然后匹配分词后的词项<br/>
	 * 适用于 text 类型的分词字段，支持相关性算分
	 * <p>
	 * 返回 BoolMatchQuery 接口，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param field 要查询的字段名
	 * @param value 要匹配的文本，会被分词
	 * @return BoolMatchQuery 接口，支持继续添加布尔查询条件
	 */
	public BoolMatchQuery match(String field, String value) {
		ElasticMatchQueryBuilder matchQueryBuilder = new ElasticMatchQueryBuilder();
		matchQueryBuilder.query(field, value);
		ReflectionUtils.setField("boolQueryBuilder", matchQueryBuilder, this);
		return matchQueryBuilder;
	}
	
	/**
	 * 创建 range 范围查询并返回 BoolRangeQuery 接口
	 * <p>
	 * range 查询用于范围匹配，支持数字、日期、字符串等类型的范围查询<br/>
	 * 可以使用 gte(大于等于)、gt(大于)、lte(小于等于)、lt(小于) 等方法设置范围
	 * <p>
	 * 返回 BoolRangeQuery 接口，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param field 要查询的字段名
	 * @return BoolRangeQuery 接口，支持继续添加布尔查询条件
	 */
	public BoolRangeQuery range(String field) {
		ElasticRangeQueryBuilder rangeQueryBuilder = new ElasticRangeQueryBuilder();
		rangeQueryBuilder.field(field);
		ReflectionUtils.setField("boolQueryBuilder", rangeQueryBuilder, this);
		return rangeQueryBuilder;
	}
	
	/**
	 * 创建 exists 存在性查询并返回 BoolQuery 接口
	 * <p>
	 * exists 查询用于查找指定字段存在且有值的文档<br/>
	 * 字段值为 null 或不存在的文档不会被匹配
	 * <p>
	 * 返回 BoolQuery 接口，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param field 要检查的字段名
	 * @return BoolQuery 接口，支持继续添加布尔查询条件
	 */
	public BoolQuery exists(String field) {
		ElasticExistsQueryBuilder existsQueryBuilder = new ElasticExistsQueryBuilder();
		existsQueryBuilder.field(field);
		ReflectionUtils.setField("boolQueryBuilder", existsQueryBuilder, this);
		return existsQueryBuilder;
	}
	
	/**
	 * 创建 query_string 查询并返回 ElasticQueryStringBuilder
	 * <p>
	 * query_string 查询支持 Lucene 查询语法，可以使用 AND、OR、NOT 等操作符<br/>
	 * 支持通配符、模糊查询、范围查询等高级语法
	 * <p>
	 * 返回 ElasticQueryStringBuilder，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param queryString Lucene 查询语法字符串，例如 "title:elasticsearch AND status:published"
	 * @return ElasticQueryStringBuilder，支持继续添加布尔查询条件
	 */
	public ElasticQueryStringBuilder queryString(String queryString) {
		ElasticQueryStringBuilder queryStringBuilder = new ElasticQueryStringBuilder();
		queryStringBuilder.query(queryString);
		ReflectionUtils.setField("boolQueryBuilder", queryStringBuilder, this);
		return queryStringBuilder;
	}
	
	/**
	 * 创建 ids 查询并返回 ElasticIdsQueryBuilder（数组形式）
	 * <p>
	 * ids 查询用于根据文档 ID 列表查询文档，支持一次查询多个 ID
	 * <p>
	 * 返回 ElasticIdsQueryBuilder，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param ids 文档 ID 数组
	 * @return ElasticIdsQueryBuilder，支持继续添加布尔查询条件
	 */
	public ElasticIdsQueryBuilder ids(String... ids) {
		ElasticIdsQueryBuilder elasticIdsQueryBuilder = new ElasticIdsQueryBuilder();
		elasticIdsQueryBuilder.ids(ids);
		ReflectionUtils.setField("boolQueryBuilder", elasticIdsQueryBuilder, this);
		return elasticIdsQueryBuilder;
	}
	
	/**
	 * 创建 ids 查询并返回 ElasticIdsQueryBuilder（List 形式）
	 * <p>
	 * ids 查询用于根据文档 ID 列表查询文档，支持一次查询多个 ID
	 * <p>
	 * 返回 ElasticIdsQueryBuilder，支持链式调用继续添加 must/mustNot/should/filter 等布尔条件
	 *
	 * @param ids 文档 ID 列表
	 * @return ElasticIdsQueryBuilder，支持继续添加布尔查询条件
	 */
	public ElasticIdsQueryBuilder ids(List<String> ids) {
		ElasticIdsQueryBuilder elasticIdsQueryBuilder = new ElasticIdsQueryBuilder();
		elasticIdsQueryBuilder.ids(ids.stream().toArray(String[]::new));
		ReflectionUtils.setField("boolQueryBuilder", elasticIdsQueryBuilder, this);
		return elasticIdsQueryBuilder;
	}
	
	/**
	 * 数字形式指定 minimum_should_match: 6 <p>
	 * <p>
	 * 如果用户给定 5 个查询词项, 想查找只包含其中 4 个的文档, 该如何处理？<p>
	 * match 查询支持 minimum_should_match 最小匹配参数, 这让我们可以指定必须匹配的词项数用来表示一个文档是否相关。
	 * 我们可以将其设置为某个具体数字, 更常用的做法是将其设置为一个百分数, 因为我们无法控制用户搜索时输入的单词数量:
	 * <p>
	 * 比如我们搜索 Once Upon a Time in the Midlands<p>
	 * 如果是standard分词器, 分词后得到的词项为: "once", "upon", "a", "time", "in", "the", "midlands"<p>
	 * 所以 minimum_should_match: 7 或者 minimum_should_match: "100%" 表示这些词都要包含<p>
	 * minimum_should_match: 6 就表示文本中这个字段少一个词项的也可以搜索到
	 *
	 * @param minimumShouldMatch
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticBoolQueryBuilder minimumShouldMatch(int minimumShouldMatch) {
		this.minimumShouldMatch = minimumShouldMatch;
		return this;
	}
	
	/**
	 * 字符串百分比形式指定 minimum_should_match: 50% <p>
	 * <p>
	 * 如果用户给定 5 个查询词项, 想查找只包含其中 4 个的文档, 该如何处理？<p>
	 * match 查询支持 minimum_should_match 最小匹配参数, 这让我们可以指定必须匹配的词项数用来表示一个文档是否相关。
	 * 我们可以将其设置为某个具体数字, 更常用的做法是将其设置为一个百分数, 因为我们无法控制用户搜索时输入的单词数量:
	 * <p>
	 * 比如我们搜索 Once Upon a Time in the Midlands<p>
	 * 如果是standard分词器, 分词后得到的词项为: "once", "upon", "a", "time", "in", "the", "midlands"<p>
	 * 所以 minimum_should_match: 7 或者 minimum_should_match: "100%" 表示这些词都要包含<p>
	 * minimum_should_match: 6 就表示文本中这个字段少一个词项的也可以搜索到
	 *
	 * @param minimumShouldMatch
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticBoolQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
		this.minimumShouldMatch = minimumShouldMatch;
		return this;
	}
	
	/**
	 * 设置分页属性, 深度分页建议用Search After
	 *
	 * @param from
	 * @param size
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticBoolQueryBuilder paging(Integer from, Integer size) {
		this.from = from;
		this.size = size;
		return this;
	}
	
	/**
	 * ES默认只返回10条数据, 这里可以指定返回多少条数据<p>
	 * 通过Search After分页时第一次需要设置size<p>
	 * 深度分页时推荐用Search After
	 *
	 * @param size
	 * @return ElasticMatchQueryBuilder
	 */
		public ElasticBoolQueryBuilder size(int size) {
		this.size = size;
		return this;
	}
	
	/**
	 * 添加排序规则<p>
	 * sort格式: 字段1:asc,字段2:desc,字段3<p>
	 * 其中字段3按升序排(ASC)<p>
	 * <p>
	 * 注意: text类型字段不能排序, 要用field
	 *
	 * @param sort
	 * @return QueryBuilder
	 */
	public ElasticBoolQueryBuilder sort(String sort) {
		List<SortOrder> sortOrders = SortSupport.sort(sort);
		this.sortOrders.addAll(sortOrders);
		return this;
	}
	
	/**
	 * GET movies/_search?q=2012&df=title&sort=year:desc&from=0&size=10&timeout=1s<p>
	 * <p>
	 * 参考上面的查询, sort语法是 字段名:asc|desc
	 *
	 * @param direction
	 * @return UriQueryBuilder
	 */
	public ElasticBoolQueryBuilder sort(String field, Direction direction) {
		notNull(field, "field cannot be null!");
		notNull(direction, "direction cannot be null!");
		return sort(field + ":" + direction);
	}
	
	/**
	 * 避免深度分页的性能问题, 可以实时获取下一页文档信息<p>
	 * 第一步搜索需要指定sort, 并且保证值是唯一的(可以通过加入_id保证唯一性)<p>
	 * 然后使用上一次, 最后一个文档的sort值进行查询<p>
	 * 这个就是查询得到的最后一个文档的sort值
	 * <p>
	 * 注意设置了searchAfter就不要设置from了, 只要指定size以及排序就可以了
	 *
	 * @param searchAfter
	 * @return ElasticQueryBuilder
	 */
	public ElasticBoolQueryBuilder searchAfter(Object[] searchAfter) {
		this.searchAfter = searchAfter;
		return this;
	}
	
	/**
	 * 是否要获取_source
	 *
	 * @param fetchSource
	 * @return QueryBuilder
	 */
	public ElasticBoolQueryBuilder fetchSource(boolean fetchSource) {
		this.fetchSource = fetchSource;
		return this;
	}
	
	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticBoolQueryBuilder includeSources(String... fields) {
		this.includeSource = fields;
		return this;
	}
	
	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticBoolQueryBuilder includeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.includeSource = sources;
		return this;
	}
	
	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticBoolQueryBuilder excludeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.excludeSource = sources;
		return this;
	}
	
	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticBoolQueryBuilder excludeSources(String... fields) {
		this.excludeSource = fields;
		return this;
	}
	
	/**
	 * 提升或者降低查询的权重
	 *
	 * @param boost
	 * @return QueryBuilder
	 */
	public ElasticBoolQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}
	
	public ElasticBoolQueryBuilder resultType(Class resultType) {
		this.resultType = resultType;
		return this;
	}
	
	@Override
	protected QueryBuilder builder() {
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		queryBuilders.sort((prev, next) -> prev.type.compareTo(next.type));
		for (Node node : queryBuilders) {
			if (node.type == MUST) {
				boolQueryBuilder.must(node.builder);
				continue;
			}
			if (node.type == MUST_NOT) {
				boolQueryBuilder.mustNot(node.builder);
				continue;
			}
			if (node.type == SHOULD) {
				boolQueryBuilder.should(node.builder);
				continue;
			}
			if (node.type == FILTER) {
				boolQueryBuilder.filter(node.builder);
				continue;
			}
		}
		if (isNotBlank(nestedPath)) {
			return QueryBuilders.nestedQuery(nestedPath, boolQueryBuilder, ScoreMode.Avg);
		}
		return boolQueryBuilder;
	}
}
