package com.awesomecopilot.search.builder;

import com.awesomecopilot.search.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search.builder.query.BoolRangeQuery;
import com.awesomecopilot.search.enums.Direction;
import com.awesomecopilot.search.enums.SortOrder;
import com.awesomecopilot.search.support.SortSupport;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * <p>
 * Copyright: (C), 2021-06-06 14:50
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticRangeQueryBuilder extends BaseQueryBuilder implements BoolRangeQuery {

	/**
	 * 嵌套查询的字段
	 */
	private String nestedPath;

	private Object gte;
	
	private Object lte;
	
	private Object gt;
	
	private Object lt;
	
	private boolean constantScore = false;
	
	public ElasticRangeQueryBuilder() {
	}
	
	public ElasticRangeQueryBuilder(String... indices) {
		this.indices = indices;
	}

	/**
	 * 设置嵌套查询字段
	 *
	 * @param path
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticRangeQueryBuilder nestedPath(String path) {
		this.nestedPath = path;
		return this;
	}

	/**
	 * 返回存在该字段的文档
	 *
	 * @param field
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder field(String field) {
		this.field = field;
		return this;
	}
	
	public ElasticRangeQueryBuilder gte(Object gte) {
		this.gte = gte;
		return this;
	}
	
	public ElasticRangeQueryBuilder lte(Object lte) {
		this.lte = lte;
		return this;
	}
	
	public ElasticRangeQueryBuilder gt(Object gt) {
		this.gt = gt;
		return this;
	}
	
	public ElasticRangeQueryBuilder lt(Object lt) {
		this.lt = lt;
		return this;
	}

	/**
	 * 设置分页属性, 深度分页建议用Search After
	 *
	 * @param from
	 * @param size
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder paging(Integer from, Integer size) {
		this.from = from;
		this.size = size;
		return this;
	}

	/**
	 * 从第几条记录开始, 第一条记录是1
	 *
	 * @param from 从第几页开始
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder from(int from) {
		this.from = from;
		return this;
	}

	/**
	 * ES默认只返回10条数据, 这里可以指定返回多少条数据<p>
	 * 通过Search After分页时第一次需要设置size<p>
	 * 深度分页时推荐用Search After
	 *
	 * @param size
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder size(int size) {
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
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder sort(String sort) {
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
	 * @return ElasticMatchQueryBuilder
	 */
	public ElasticRangeQueryBuilder sort(String field, Direction direction) {
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
	 * @return ElasticRangeQueryBuilder
	 */
	public ElasticRangeQueryBuilder searchAfter(Object[] searchAfter) {
		this.searchAfter = searchAfter;
		return this;
	}

	/**
	 * Scroll API
	 * @param scrollWindow Scroll查询的时间窗口
	 * @param timeUnit scrollWindow的单位, 最终scrollWindow会转成毫秒数s
	 * @return
	 */
	@Override
	public ElasticRangeQueryBuilder scroll(long scrollWindow, TimeUnit timeUnit) {
		super.scroll(scrollWindow, timeUnit);
		return this;
	}
	
	public ElasticRangeQueryBuilder constantScore(boolean constantScore) {
		this.constantScore = constantScore;
		return this;
	}
	
	public ElasticRangeQueryBuilder refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	public ElasticRangeQueryBuilder resultType(Class resultType) {
		this.resultType = resultType;
		return this;
	}

	@Override
	protected QueryBuilder builder() {
		RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(field);
		if (gte != null) {
			rangeQueryBuilder.gte(gte);
		} else if (gt != null) {
			rangeQueryBuilder.gt(gt);
		}
		
		if (lte != null) {
			rangeQueryBuilder.lte(lte);
		} else if (lt != null) {
			rangeQueryBuilder.lt(lt);
		}
		
		if (constantScore) {
			ConstantScoreQueryBuilder constantScoreQueryBuilder = QueryBuilders.constantScoreQuery(rangeQueryBuilder);
			if (isNotBlank(nestedPath)) {
				return QueryBuilders.nestedQuery(nestedPath, constantScoreQueryBuilder, ScoreMode.Avg);
			}
			return constantScoreQueryBuilder;
		} else {
			if (isNotBlank(nestedPath)) {
				return QueryBuilders.nestedQuery(nestedPath, rangeQueryBuilder, ScoreMode.Avg);
			}
			return rangeQueryBuilder;
		}
	}
	
}
