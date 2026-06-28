package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Aggsv8;
import com.awesomecopilot.search8x.builder.ElasticRangeQueryBuilder;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregations;
import com.awesomecopilot.search8x.builder.agg.v8.V8MultiTermsAggregationBuilder;
import com.awesomecopilot.search8x.vo.ElasticPage;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * Copyright: (C), 2021-09-24 11:19
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class MultiTermsTest {
	
	@Test
	public void testMultiTermsPaging() {
		ElasticRangeQueryBuilder queryBuilder = ElasticUtils.Query.range("netlog_2021-09*")
				.field("create_time")
				.gte(1632418220000L)
				.lte(1632454220000L);
		V8MultiTermsAggregationBuilder aggregationBuilder = Aggsv8
				.multiTerms("netlog_2021-09*")
				.setQuery(queryBuilder)
				.of("multi-field", new String[]{"src_ip", "dst_ip"})
				.fetchTotalHits(true);
		
		aggregationBuilder.subAggregation(
				SubAggregations.bucketSort("multi_terms_sort")
						.paging(0, 20))
				.getPage();
		
		ElasticPage page = aggregationBuilder.getPage();
		System.out.println(page.getTotalCount());
		System.out.println(page.getTotalPages());
		System.out.println(page.getPageSize());
		System.out.println(page.getPageNum());
	}
}