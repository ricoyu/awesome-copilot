package com.awesomecopilot.common.lang.dto;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION;
import com.awesomecopilot.common.lang.vo.Page;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

import static com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION.ASC;

public class PageDTO {

	/**
	 * 当前第几页, 从1开始
	 */
	private int pageNum = 1;

	/**
	 * 每页多少条记录
	 */
	private int pageSize = 10;

	/**
	 * 排序（在字段名后加“:asc或:desc”指定升序（降序），多个字段使用逗号分隔，省略排序默认使用升序）", example = "“字段1,字段2” 或者 “字段1:asc,字段2:desc”
	 */
	private String order;

	public Page toPage() {
		Page page = new Page();
		page.setPageNum(pageNum);
		page.setPageSize(pageSize);
		if (StringUtils.isNotEmpty(order)) {
			String[] orderList = order.split(",");
			Stream.of(orderList).forEach(o -> {
				String[] arr = o.split(":");
				DIRECTION direction = arr.length == 1 ? ASC : DIRECTION.of(arr[1]);
				OrderBean order = new OrderBean();
				order.setOrderBy(arr[0]);
				order.setDirection(direction);
				page.getOrders().add(order);
			});
		}
		return page;
	}


	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}
}
