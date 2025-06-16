package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsGeoDistanceQueryTest {

	@Test
	public void testGeoDistance() {
		List<Object> ecommerces = ElasticUtils.Query.geoDistance("ecommerce")
				.distance("1000km")
				.location(39.9042, 116.4074)
				.queryForList();
		for (Object ecommerce : ecommerces) {
			System.out.println(ecommerce);
		}

		assertThat(ecommerces.size()).isEqualTo(2);
	}
}
