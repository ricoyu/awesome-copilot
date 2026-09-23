package com.awesomecopilot.common.lang.dto;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.Page;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-20 回归测试（CODE_REVIEW_REPORT 2026-09-22）：PageDTO.getPage 只填 orders 不填 order
 * 且不 trim，JPA 分页排序整条失效且不报错。
 * <p>
 * 修复前实测：order="id:asc, create_time:desc" 时 page.getOrder() 恒 null
 * （copilot-orm JPACriteriaQuery 只读 getOrder() 判空加排序 → 前端传什么排序都被忽略），
 * 且第二个 OrderBean 的 orderBy 是 " create_time"（带前导空格）。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class PageDtoOrderTest {

	@Test
	public void testGetPageFillsSingleOrderForJpaConsumer() {
		PageDTO dto = new PageDTO();
		dto.setOrder("id:asc, create_time:desc");

		Page page = dto.getPage();

		//修复前实测: getOrder() 恒 null, JPACriteriaQuery 判空后整条排序被无声丢弃
		assertThat(page.getOrder()).as("第一个排序项必须同时填进 order 字段").isNotNull();
		assertThat(page.getOrder().getOrderBy()).isEqualTo("id");
		assertThat(page.getOrder().getDirection()).isEqualTo(OrderBean.DIRECTION.ASC);
	}

	@Test
	public void testOrderFieldNamesAreTrimmed() {
		PageDTO dto = new PageDTO();
		dto.setOrder("id:asc, create_time:desc");

		Page page = dto.getPage();

		//Page 注释语义: order 是第一排序项, orders 只放第二第三...级
		assertThat(page.getOrders()).hasSize(1);
		//修复前实测: 第二项 orderBy 带前导空格 " create_time"
		assertThat(page.getOrders().get(0).getOrderBy()).isEqualTo("create_time");
		assertThat(page.getOrders().get(0).getDirection()).isEqualTo(OrderBean.DIRECTION.DESC);
	}

	@Test
	public void testDefaultDirectionAndNoWhitespaceRemain() {
		PageDTO dto = new PageDTO();
		dto.setOrder(" name ");
		Page page = dto.getPage();
		assertThat(page.getOrder().getOrderBy()).isEqualTo("name");
		assertThat(page.getOrder().getDirection()).isEqualTo(OrderBean.DIRECTION.ASC);
	}

	@Test
	public void testEmptyOrderLeavesEverythingNull() {
		PageDTO dto = new PageDTO();
		Page page = dto.getPage();
		assertThat(page.getOrder()).isNull();
		assertThat(page.getOrders()).isEmpty();
	}
}
