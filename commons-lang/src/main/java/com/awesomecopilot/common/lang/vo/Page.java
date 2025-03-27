package com.awesomecopilot.common.lang.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * 分页支持, pageNum, pageSize这两个参数是必传的, 否则读取RequestBody报错
 * 
 * @author xuehyu
 * @since 2014-10-13
 * @version 1.0
 * 
 */
public class Page implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 当前第几页, 从1开始
	 */
	private int pageNum = 1;

	/**
	 * 每页多少条记录
	 */
	private int pageSize = 10;

	/**
	 * 第一级排序, 如果只按一个字段排序，那么就用这个
	 */
	private OrderBean order;

	/**
	 * 如果提供了order，那么orders是第二第三...级排序
	 * <p>
	 * 如果需要先按A排序，相同的再按B排序，就可以用这个orders指定多个排序规则
	 */
	private List<OrderBean> orders = new ArrayList<>();

	/**
	 * 总共有多少页，通过查询结果返回给你显示用
	 */
	private int totalPages = 0;

	/**
	 * 总共有多少条记录，通过查询结果返回给你显示用"
	 */
	@JsonProperty("total")
	private int totalCount = 0;
	
	private boolean autoCount = true;

	/**
	 * 对于某些接口，有些调用需要分页，有些不需要分页，如果不需要分页，传true就可以了
	 */
	private boolean pagingIgnore = false;


	/**
	 * firstRowIndex begins from 0
	 * 
	 * @return
	 */
	@JsonIgnore
	public int getFirstResult() {
		return (pageNum - 1) * pageSize;
	}

	/**
	 * Record limits to maxResults per time
	 * 
	 * @return
	 */
	@JsonIgnore
	public int getMaxResults() {
		return this.pageSize;
	}

	public int getPageNum() {
		return pageNum;
	}
	
	/**
	 * 页码从1开始
	 * @param pageNum
	 */
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
		updatePagingStatus();
	}
	
	@JsonIgnore
	public boolean isAutoCount() {
		return autoCount;
	}

	public void setAutoCount(boolean autoCount) {
		this.autoCount = autoCount;
	}
	
	public void addOrder(OrderBean... orderBeans) {
		requireNonNull(orders, "order不能为null");
		for (int i = 0; i < orderBeans.length; i++) {
			OrderBean orderBean = orderBeans[i];
			requireNonNull(orderBean);
			orders.add(orderBean);
		}
	}
	
	public Page addOrder(String orderBy, OrderBean.DIRECTION direction) {
		orders.add(new OrderBean(orderBy, direction));
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pageNum, pageSize, totalPages, totalCount, order, orders);
	}

	/**
	 * Based on record return from sql query, determine if there is another page
	 * 
	 */
	private void updatePagingStatus() {
		this.totalPages = (int) Math.ceil(this.totalCount / (double) this.pageSize);
	}
	
	@JsonIgnore
	public boolean isPagingIgnore() {
		return pagingIgnore;
	}

	public void setPagingIgnore(boolean pagingIgnore) {
		this.pagingIgnore = pagingIgnore;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@JsonIgnore
	public OrderBean getOrder() {
		return order;
	}
	
	public void setOrder(OrderBean order) {
		this.order = order;
	}
	
	@JsonIgnore
	public List<OrderBean> getOrders() {
		return orders;
	}
}
