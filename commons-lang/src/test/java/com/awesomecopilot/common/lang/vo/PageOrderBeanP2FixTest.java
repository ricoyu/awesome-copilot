package com.awesomecopilot.common.lang.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-31 分页/排序 VO 簇回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>Page：修复前只有 setTotalCount 触发 totalPages 重算——实测 setTotalCount(95)
 * 后 totalPages=10，再 setPageSize(20) 仍 10（期望 5）。Jackson 反序列化按字段顺序
 * 先 total 后 pageSize 时必踩。修复后 setPageSize 同样重算；</li>
 * <li>Page：修复前 hashCode 基于全部可变字段但没重写 equals——Page 进 HashSet 后
 * 调一次 setter 就再也 contains 不到自己。纯 DTO 用默认标识语义，删 hashCode；</li>
 * <li>OrderBean.DIRECTION.of：修复前 of("xyz") 返回 ASC 不抛异常（只有一条 ERROR
 * 日志），而 of(null) 抛 NullPointerException——两种非法输入两种相反策略。
 * 统一为抛 IllegalArgumentException（PageDTO 的转换失败路径已在用它）。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class PageOrderBeanP2FixTest {

	// ---------------- Page: totalPages 重算 ----------------

	@Test
	public void testSetPageSizeRecalculatesTotalPages() {
		Page page = new Page();
		page.setTotalCount(95); //pageSize 默认 10 → 10 页
		assertThat(page.getTotalPages()).isEqualTo(10);

		page.setPageSize(20); //95 条 / 每页 20 = 5 页
		assertThat(page.getTotalPages())
				.as("改每页条数后总页数必须跟着重算(修复前实测仍停留在 10)")
				.isEqualTo(5);
	}

	@Test
	public void testTotalCountStillRecalculatesAfterChange() {
		//原有语义保持: setTotalCount 重算
		Page page = new Page();
		page.setPageSize(20);
		page.setTotalCount(95);
		assertThat(page.getTotalPages()).isEqualTo(5);
	}

	@Test
	public void testEqualsIsIdentitySemanticsForDto() {
		//修复前 hashCode 重写了但 equals 没有(半套): 两个内容相同的 Page hashCode 可能相等
		//而 equals 永真不等, 且字段一改 hashCode 就变、HashSet 里找不回自己。
		//删除自定义 hashCode 后回到 Object 默认标识语义, equals 与 hashCode 成对一致。
		Page a = new Page();
		a.setTotalCount(95);
		Page b = new Page();
		b.setTotalCount(95);

		assertThat(a).isEqualTo(a); //同一实例
		assertThat(a).isNotEqualTo(b); //DTO 默认标识语义: 内容相同也不相等
	}

	// ---------------- OrderBean.DIRECTION.of ----------------

	@Test
	public void testDirectionOfInvalidThrows() {
		//修复前实测: of("xyz") 返回 ASC 不抛(只打日志)——前端参数拼错时查询顺序会反
		assertThatThrownBy(() -> OrderBean.DIRECTION.of("xyz"))
				.as("非法方向值必须报错而不是降级成 ASC")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testDirectionOfNullThrowsSameType() {
		//修复前实测: of(null) 抛 NullPointerException——与非法字符串的策略相反, 现统一
		assertThatThrownBy(() -> OrderBean.DIRECTION.of(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testDirectionOfValidValuesUnchanged() {
		assertThat(OrderBean.DIRECTION.of("asc")).isEqualTo(OrderBean.DIRECTION.ASC);
		assertThat(OrderBean.DIRECTION.of("DESC")).isEqualTo(OrderBean.DIRECTION.DESC);
	}
}
