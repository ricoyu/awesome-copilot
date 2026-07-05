package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.search8x.enums.SortOrderEnum;
import com.awesomecopilot.search8x.support.SortSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 对Bucket Aggregation进行分页操作
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-aggregations-pipeline-bucket-sort-aggregation.html#CO106-1
 * <p>
 * Copyright: (C), 2021-08-25 14:08
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBucketSortSubAggregation extends SubAggregation {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticBucketSortSubAggregation.class);
	
	
	private SubAggregation parentAggregation;
	
	/**
	 * 分页相关, 起始位置
	 */
	protected Integer from;
	
	/**
	 * 分页相关, 每页大小
	 */
	protected Integer size;
	
	/**
	 * 排序 ASC DESC
	 */
	protected List<SortOrderEnum> sortOrderEnums = new ArrayList<>();
	
	public ElasticBucketSortSubAggregation(String name) {
		Objects.requireNonNull(name, "name cannot be null!");
		this.name = name;
	}
	
	
	/**
	 * 设置分页属性, 第一条数据从0开始
	 *
	 * @param from
	 * @param size
	 * @return ElasticBucketSortSubAggregation
	 */
	public ElasticBucketSortSubAggregation paging(Integer from, Integer size) {
		this.from = from;
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
	public ElasticBucketSortSubAggregation sort(String sort) {
		List<SortOrderEnum> sortOrderEnums = SortSupport.sort(sort);
		this.sortOrderEnums.addAll(sortOrderEnums);
		return this;
	}
	
	@Override
	public Aggregation build() {
		List<SortOptions> sorts = sortOrderEnums.stream()
				.map(SortOrderEnum::toBucketSortSortOptions)
				.collect(Collectors.toList());
		
		return Aggregation.of(a -> a.bucketSort(bs -> {
			bs.sort(sorts);
			if (from != null) {
				bs.from(from);
			}
			if (size != null) {
				bs.size(size);
			}
			return bs;
		}));
	}
	
	public Page toPage() {
		Page page = new Page();
		int size = this.size == null ? 10 : this.size;
		page.setPageSize(size);
		if (from != null) {
			int currentPage = (int) Math.floor(from / (double) size) + 1;
			page.setPageNum(currentPage);
		}
		return page;
	}
	
	@Override
	public SubAggregation and() {
		return parentAggregation;
	}
}
