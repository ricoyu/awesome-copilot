package com.awesomecopilot.search.builder.query;

import com.awesomecopilot.search.enums.Direction;
import com.awesomecopilot.search.enums.SortOrder;
import com.awesomecopilot.search.support.SortSupport;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;

/**
 * 匹配一个短语前缀, 比如查description包含"高性能智"这个短语前缀, 那么description是"高性能智能手机"可以查到, "高性能智慧设备"也可以查到
 * <p/>
 * Copyright: Copyright (c) 2025-06-09 18:02
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticMatchPhrasePrefixQueryBuilder extends BaseQueryBuilder {

	/**
	 * 限制 最后一个词的前缀可扩展成多少个实际词项(token)
	 * 因为最后一个词是“前缀”, 它可能匹配很多不同的词 (比如 "智" 可以扩展为 "智能", "智慧", "智商" 等), 这些都叫做 “expansions”。
	 * <ul>max_expansions 决定了:
	 *     <li/>Elasticsearch 会最多尝试多少个匹配项来扩展这个前缀
	 *     <li/>防止性能问题, 默认值是 50, 你可以设置为更小的值 (如 10) 来控制开销
	 * </ul>
	 */
	private Integer maxExpansions;

	public ElasticMatchPhrasePrefixQueryBuilder(String... indices) {
		this.indices = indices;
	}
	
	/**
	 * 设置查询字段, 值
	 *
	 * @param field
	 * @param value
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder query(String field, Object value) {
		this.field = field;
		this.value = value;
		return this;
	}
	
	/**
	 * 限制 最后一个词的前缀可扩展成多少个实际词项(token)
	 * 因为最后一个词是“前缀”, 它可能匹配很多不同的词 (比如 "智" 可以扩展为 "智能", "智慧", "智商" 等), 这些都叫做 “expansions”。
	 * <ul>max_expansions 决定了:
	 *     <li/>Elasticsearch 会最多尝试多少个匹配项来扩展这个前缀
	 *     <li/>防止性能问题, 默认值是 50, 你可以设置为更小的值 (如 10) 来控制开销
	 * </ul>
	 * <p>
	 * @param maxExpansions
	 * @return ElasticMatchPhraseQuery
	 */
	public ElasticMatchPhrasePrefixQueryBuilder maxExpansions(Integer maxExpansions) {
		this.maxExpansions = maxExpansions;
		return this;
	}
	
	/**
	 * 设置分页属性, 深度分页建议用Search After
	 *
	 * @param from
	 * @param size
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder paging(Integer from, Integer size) {
		this.from = from;
		this.size = size;
		return this;
	}
	
	
	/**
	 * 从第几条记录开始, 第一条记录是1
	 *
	 * @param from 从第几页开始
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder from(int from) {
		this.from = from;
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
	public ElasticMatchPhrasePrefixQueryBuilder size(int size) {
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
	public ElasticMatchPhrasePrefixQueryBuilder sort(String sort) {
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
	public ElasticMatchPhrasePrefixQueryBuilder sort(String field, Direction direction) {
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
	public ElasticMatchPhrasePrefixQueryBuilder searchAfter(Object[] searchAfter) {
		this.searchAfter = searchAfter;
		return this;
	}
	
	/**
	 * 是否要获取_source
	 *
	 * @param fetchSource
	 * @return QueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder fetchSource(boolean fetchSource) {
		this.fetchSource = fetchSource;
		return this;
	}
	
	/**
	 * 提升或者降低查询的权重
	 *
	 * @param boost
	 * @return QueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}
	
	/**
	 * Function Score Query中用到
	 * <ul>
	 * <li/>Multiply(默认值)  算分与函数值的乘积
	 * <li/>Sum  算分与函数的和
	 * <li/>Min / Max   算分与函数取 最小/最大值
	 * <li/>Replace   使用函数值取代算分
	 * </ul>
	 *
	 * @param boostMode
	 * @return
	 */
	public ElasticMatchPhrasePrefixQueryBuilder boostMode(CombineFunction boostMode) {
		notNull(boostMode, "boostMode cannot be null!");
		this.boostMode = boostMode;
		return this;
	}
	
	public ElasticMatchPhrasePrefixQueryBuilder resultType(Class resultType) {
		this.resultType = resultType;
		return this;
	}
	
	/**
	 * 是否要将Query转为constant_scre query, 以避免算分, 提高查询性能
	 *
	 * @param constantScore
	 * @return ElasticQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder constantScore(boolean constantScore) {
		this.constantScore = constantScore;
		return this;
	}
	
	
	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder includeSources(String... fields) {
		this.includeSource = fields;
		return this;
	}
	
	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return QueryStringBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder excludeSources(String... fields) {
		this.excludeSource = fields;
		return this;
	}
	
	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticMatchPhraseQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder includeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.includeSource = sources;
		return this;
	}
	
	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticMatchPhraseQueryBuilder
	 */
	public ElasticMatchPhrasePrefixQueryBuilder excludeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.excludeSource = sources;
		return this;
	}
	
	public ElasticMatchPhrasePrefixQueryBuilder refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}
	
	@Override
	protected QueryBuilder builder() {
		MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder = new MatchPhrasePrefixQueryBuilder(field, value);
		if (maxExpansions != null) {
			matchPhrasePrefixQueryBuilder.maxExpansions(maxExpansions);
		}
		return matchPhrasePrefixQueryBuilder;
	}
}
