package com.awesomecopilot.search8x.enums;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.util.NamedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Copyright: (C), 2021-01-11 20:17
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SortOrderEnum {
	
	private static final Logger log = LoggerFactory.getLogger(SortOrderEnum.class);
	
	private SortType sortType;
	
	private String field;
	
	/**
	 * 默认升序
	 */
	private Direction direction = Direction.ASC;
	
	
	private SortOrderEnum() {
	}
	
	private SortOrderEnum(SortType sortType) {
		this.sortType = sortType;
	}
	
	/**
	 * 构建ES 8.x SortOptions
	 * @return SortOptions
	 */
	public SortOptions toSortOptions() {
		if (sortType == SortType.SCORE) {
			return SortOptions.of(s -> s.score(sc -> sc.order(direction.toSortOrder())));
		} else if (sortType == SortType.FIELD) {
			return SortOptions.of(s -> s.field(f -> f.field(field).order(direction.toSortOrder())));
		}
		throw new IllegalStateException("SortType is not set");
	}
	
	/**
	 * BucketSort 用的排序规则
	 * @return SortOptions
	 */
	public SortOptions toBucketSortSortOptions() {
		return toSortOptions();
	}
	
	/**
	 * Terms 聚合排序用, 返回 NamedValue<SortOrder>
	 * @return NamedValue<SortOrder>
	 */
	public NamedValue<SortOrder> toBucketOrder() {
		if ("key".equalsIgnoreCase(field)) {
			return NamedValue.of("_key", direction.toSortOrder());
		}
		return NamedValue.of("_count", direction.toSortOrder());
	}
	
	/**
	 * 根据权重排序
	 */
	public static SortOrderBuilder scoreSort() {
		return new SortOrderBuilder(SortType.SCORE);
	}
	
	/**
	 * 根据字段值排序
	 */
	public static SortOrderBuilder fieldSort(String field) {
		return new SortOrderBuilder(field);
	}
	
	private enum SortType {
		
		/**
		 * 根据权重排序
		 */
		SCORE,
		
		/**
		 * 根据字段值排序
		 */
		FIELD;
	}
	
	
	public static class SortOrderBuilder {
		
		private SortType sortType;
		
		private String field;
		
		private SortOrderBuilder(SortType sortType) {
			this.sortType = sortType;
		}
		
		private SortOrderBuilder(String field) {
			this.sortType = SortType.FIELD;
			this.field = field;
		}
		
		/**
		 * 升序排列
		 *
		 * @return
		 */
		public SortOrderEnum asc() {
			SortOrderEnum sortOrderEnum = new SortOrderEnum(sortType);
			sortOrderEnum.direction = Direction.ASC;
			sortOrderEnum.field = field;
			return sortOrderEnum;
		}
		
		/**
		 * 降序排列
		 *
		 * @return
		 */
		public SortOrderEnum desc() {
			SortOrderEnum sortOrderEnum = new SortOrderEnum(sortType);
			sortOrderEnum.direction = Direction.DESC;
			sortOrderEnum.field = field;
			return sortOrderEnum;
		}
		
		/**
		 * 指定字符串排序规则
		 * @param direction
		 * @return SortOrder
		 */
		public SortOrderEnum direction(String direction) {
			SortOrderEnum sortOrderEnum = new SortOrderEnum(sortType);
			sortOrderEnum.field = field;
			if (Direction.ASC.name().equalsIgnoreCase(direction)) {
				sortOrderEnum.direction = Direction.ASC;
			} else if (Direction.DESC.name().equalsIgnoreCase(direction)) {
				sortOrderEnum.direction = Direction.DESC;
			} else {
				log.error("Sort direction can only be asc, desc, your's {}", direction);
				throw new IllegalArgumentException("Sort direction can only be asc, desc, your's " + direction);
			}
			
			return sortOrderEnum;
		}
	}
}
