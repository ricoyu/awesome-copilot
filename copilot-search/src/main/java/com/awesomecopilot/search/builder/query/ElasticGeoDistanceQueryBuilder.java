package com.awesomecopilot.search.builder.query;

import com.awesomecopilot.search.enums.Direction;
import com.awesomecopilot.search.enums.SortOrder;
import com.awesomecopilot.search.support.SortSupport;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Terms Query: <p>
 * ES 不会对你输入的条件做任何的分词处理<p>
 * 跟term query是一样的, 只是可以搜索字段匹配多个值
 * <p>
 * Copyright: (C), 2021-04-30 11:14
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public final class ElasticGeoDistanceQueryBuilder extends BaseQueryBuilder {

	/**
	 * 距离阈值
	 */
	private String distance;

	/**
	 * 距离单位
	 */
	private DistanceUnit distanceUnit = DistanceUnit.KILOMETERS;

	/**
	 * 经度
	 */
	private double longitude;

	/**
	 * 纬度
	 */
	private double latitude;

	public ElasticGeoDistanceQueryBuilder(String... indices) {
		super(indices);
	}

	/**
	 * 设置距离目标经纬度距离阈值
	 *
	 * @param distance
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder distance(String distance) {
		if (isNotBlank(distance)) {
			StringBuilder unit = new StringBuilder();
			int index = distance.length() - 1;
			char lastChar = distance.charAt(index);
			while (!Character.isDigit(lastChar) && index >= 0) {
				unit.insert(0, lastChar);
				index--;
				lastChar = distance.charAt(index);
			}
			if (unit.length() == 0) {
				unit.append("km");
			}
			distance = distance.substring(0, index + 1);

			DistanceUnit distanceUnit = DistanceUnit.fromString(unit.toString());
			this.distance = distance;
			this.distanceUnit = distanceUnit;
		}
		return this;
	}

	/**
	 * 目标经纬度
	 *
	 * @param latitude  纬度
	 * @param longitude 经度
	 * @return ElasticGeoDistanceQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder location(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		return this;
	}

	/**
	 * 设置分页属性, 深度分页建议用Search After
	 *
	 * @param from
	 * @param size
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder paging(Integer from, Integer size) {
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
	public ElasticGeoDistanceQueryBuilder from(int from) {
		this.from = from;
		return this;
	}

	/**
	 * ES默认只返回10条数据, 这里可以指定返回多少条数据<p>
	 * 通过Search After分页时第一次需要设置size<p>
	 * 深度分页时推荐用Search After
	 *
	 * @param size
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder size(int size) {
		this.size = size;
		return this;
	}

	/**
	 * 添加基于字段的排序, 默认升序
	 *
	 * @param direction
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder scoreSort(Direction direction) {
		super.scoreSort(direction);
		return this;
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
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder searchAfter(Object[] searchAfter) {
		this.searchAfter = searchAfter;
		return this;
	}

	/**
	 * 是否要获取_source
	 *
	 * @param fetchSource
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder fetchSource(boolean fetchSource) {
		this.fetchSource = fetchSource;
		return this;
	}

	/**
	 * 提升或者降低查询的权重
	 *
	 * @param boost
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder boost(float boost) {
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
	public ElasticGeoDistanceQueryBuilder boostMode(CombineFunction boostMode) {
		notNull(boostMode, "boostMode cannot be null!");
		this.boostMode = boostMode;
		return this;
	}

	public ElasticGeoDistanceQueryBuilder resultType(Class resultType) {
		this.resultType = resultType;
		return this;
	}

	/**
	 * 是否要将Query转为constant_scre query, 以避免算分, 提高查询性能
	 *
	 * @param constantScore
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder constantScore(boolean constantScore) {
		this.constantScore = constantScore;
		return this;
	}

	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder includeSources(String... fields) {
		this.includeSource = fields;
		return this;
	}

	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder excludeSources(String... fields) {
		this.excludeSource = fields;
		return this;
	}

	/**
	 * 控制返回自己想要的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder includeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.includeSource = sources;
		return this;
	}

	/**
	 * 控制要排除哪些返回的字段, 而不是整个_source
	 *
	 * @param fields
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder excludeSources(List<String> fields) {
		String[] sources = fields.stream().toArray(String[]::new);
		this.excludeSource = sources;
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
	 * @return ElasticTermsQueryBuilder
	 */
	public ElasticGeoDistanceQueryBuilder sort(String sort) {
		List<SortOrder> sortOrders = SortSupport.sort(sort);
		this.sortOrders.addAll(sortOrders);
		return this;
	}

	public ElasticGeoDistanceQueryBuilder refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	@Override
	protected QueryBuilder builder() {
		GeoDistanceQueryBuilder geoDistanceQueryBuilder = QueryBuilders.geoDistanceQuery("location")
				.point(latitude, longitude)
				.distance(this.distance, this.distanceUnit);
		if (constantScore) {
			return QueryBuilders.constantScoreQuery(geoDistanceQueryBuilder);
		}

		return geoDistanceQueryBuilder;
	}
}

