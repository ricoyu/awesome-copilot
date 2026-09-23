package com.awesomecopilot.common.lang.dto;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION;
import com.awesomecopilot.common.lang.vo.Page;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION.ASC;

/**
 * 接收分页查询时的DTO可以继承这个 PageDTO 以承载分页参数
 * <p/>
 * Copyright: Copyright (c) 2025-04-03 15:26
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
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
	 * 排序（在字段名后加“:asc或:desc”指定升序 (降序), 多个字段使用逗号分隔, 省略排序默认使用升序)", example = "“字段1,字段2” 或者 “字段1:asc,字段2:desc”
	 */
	private String order;

	/**
	 * 根据PageDTO中的pageNum, pageSize, order初始化Page对象
	 * <p/>
	 * P1-20: 修复前只填 orders 不填 order(copilot-orm 的 JPACriteriaQuery 只读 getOrder(),
	 * 判 null 后整条排序被无声丢弃), 且不 trim 字段名("id:asc, create_time:desc" 的第二项
	 * 变成带前导空格的 " create_time")。现在第一个排序项同时填进 order, 其余按 Page 的
	 * 注释语义("orders 是第二第三...级排序")进 orders, 两个查询入口看到同一份排序。
	 *
	 * @return Page
	 */
	public Page getPage() {
		Page page = new Page();
		page.setPageNum(pageNum);
		page.setPageSize(pageSize);
		if (StringUtils.isNotEmpty(order)) {
			String[] orderList = order.split(",");
			List<OrderBean> beans = new ArrayList<>();
			for (String o : orderList) {
				String[] arr = o.split(":");
				DIRECTION direction = arr.length == 1 ? ASC : DIRECTION.of(arr[1]);
				OrderBean orderBean = new OrderBean();
				orderBean.setOrderBy(arr[0].trim()); //修复前: 不 trim, 第二个及以后的字段名带前导空格
				orderBean.setDirection(direction);
				if (StringUtils.isEmpty(orderBean.getOrderBy())) {
					continue; //评审修复(2026-09-23): 空段(" "或"id:asc,,b")不产出 orderBy="" 的排序项,
				}             //否则被放进 page.order 后, JPA 入口 root.get("") 抛查询期异常(修复前是整条忽略)
				beans.add(orderBean);
			}
			if (!beans.isEmpty()) {
				page.setOrder(beans.get(0)); //第一排序项
				//Page 注释语义: orders 是第二第三...级排序
				page.getOrders().addAll(beans.subList(1, beans.size()));
			}
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
