package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static com.awesomecopilot.search.ElasticUtils.Query;
import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsPrefixQueryTest {

	@Test
	public void testPrefixQuery() {
		List<Object> ecommerces = Query.prefix("ecommerce")
				.query("product_name.keyword", "Apple")
				.queryForList();
		for (Object ecommerce : ecommerces) {
			System.out.println(ecommerce);
		}
		assertThat(ecommerces.size()).isEqualTo(1);
	}
}
