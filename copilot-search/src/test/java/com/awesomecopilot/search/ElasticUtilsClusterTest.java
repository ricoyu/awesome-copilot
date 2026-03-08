package com.awesomecopilot.search;

import org.junit.jupiter.api.Test;

public class ElasticUtilsClusterTest {

	@Test
	public void testClusterHealth() {
		String health = ElasticUtils.Cluster.health();
		System.out.println(health);
		String health1 = ElasticUtils.Cluster.health();
		System.out.println(health1);
	}
}