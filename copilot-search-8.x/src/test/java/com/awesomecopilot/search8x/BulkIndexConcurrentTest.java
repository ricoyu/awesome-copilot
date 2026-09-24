package com.awesomecopilot.search8x;

import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import com.awesomecopilot.search8x.support.BulkResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.search8x.ElasticUtils.Admin.deleteIndex;
import static com.awesomecopilot.search8x.ElasticUtils.Admin.existsIndex;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * bulkIndexConcurrent 逐条失败感知的回归测试(评估报告 P0-5)
 * <p>
 * ES bulk 在个别文档映射冲突时仍返回 HTTP 200, 失败信息藏在响应的 items 里(errors=true)。
 * 原实现不接收 bulk 返回值, 这些失败被丢弃, 调用方以为全部写入成功。
 * 本测试用 dynamic=strict 的映射制造"一条好文档 + 一条冲突文档"的真实逐条失败场景。
 * <p>
 * Copyright: (C), 2026/9/24
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class BulkIndexConcurrentTest {

	private static final String INDEX = "audit_bulk_concurrent_test";

	@BeforeEach
	@AfterEach
	public void cleanup() {
		if (existsIndex(INDEX)) {
			deleteIndex(INDEX);
		}
	}

	/**
	 * 用 strict 映射建索引: 只有 price(long) 和 name(keyword) 两个字段,
	 * 文档多带别的字段会被逐条拒绝(HTTP 仍 200, items 里报 "mapping set to strict")
	 */
	private void createStrictIndex() {
		try {
			ElasticUtils.QUERY_CLIENT.indices().create(c -> c
					.index(INDEX)
					.mappings(m -> m
							.dynamic(DynamicMapping.Strict)
							.properties("price", p -> p.long_(l -> l))
							.properties("name", p -> p.keyword(k -> k))));
		} catch (Exception e) {
			throw new RuntimeException("创建测试索引失败", e);
		}
	}

	private Map<String, Object> doc(String name, Object price) {
		Map<String, Object> m = new HashMap<>();
		m.put("name", name);
		m.put("price", price);
		return m;
	}

	@Test
	public void testReturnsFailuresForRejectedDocuments() {
		createStrictIndex();

		List<Map<String, Object>> docs = new ArrayList<>();
		docs.add(doc("good-1", 1));
		// strict 映射下多出未知字段 unknownField, 该条被逐条拒绝
		Map<String, Object> bad = doc("bad", 2);
		bad.put("unknownField", "boom");
		docs.add(bad);
		docs.add(doc("good-2", 3));

		BulkResult result = ElasticUtils.bulkIndexConcurrent(INDEX, docs);

		// 调用方必须能看到 2 成功 / 1 失败, 以及失败原因
		assertThat(result.getSuccessCount()).isEqualTo(2);
		assertThat(result.getFailCount()).isEqualTo(1);
		assertThat(result.getFailMessages()).hasSize(1);
		assertThat(result.getFailMessages().get(0)).contains("unknownField");
	}

	@Test
	public void testCountsAcrossBatches() {
		createStrictIndex();

		// 超过单批 1000 条, 验证多批结果的累计
		List<Map<String, Object>> docs = new ArrayList<>();
		for (int i = 0; i < 1201; i++) {
			docs.add(doc("n" + i, i));
		}
		docs.get(500).put("unknownField", "x");   // 第一批里 1 条坏
		docs.get(1200).put("unknownField", "x");  // 第二批里 1 条坏

		BulkResult result = ElasticUtils.bulkIndexConcurrent(INDEX, docs);

		assertThat(result.getSuccessCount()).isEqualTo(1199);
		assertThat(result.getFailCount()).isEqualTo(2);
		assertThat(result.getIds()).hasSize(1199);
	}

	@Test
	public void testEmptyAndNullInput() {
		createStrictIndex();

		assertThat(ElasticUtils.bulkIndexConcurrent(INDEX, null).getSuccessCount()).isZero();
		assertThat(ElasticUtils.bulkIndexConcurrent(INDEX, new ArrayList<>()).getFailCount()).isZero();
	}
}
