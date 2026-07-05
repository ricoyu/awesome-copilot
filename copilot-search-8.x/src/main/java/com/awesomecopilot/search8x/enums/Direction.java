package com.awesomecopilot.search8x.enums;

import co.elastic.clients.elasticsearch._types.SortOrder;

/**
 * 排序
 */
public enum Direction {
	
	ASC,
	
	DESC;
	
	/**
	 * Direction 转Elasticsearch API SortOrder
	 * @return
	 */
	public SortOrder toSortOrder() {
		if (this == ASC) {
			return SortOrder.Asc;
		}
		
		return SortOrder.Desc;
	}
	
	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
