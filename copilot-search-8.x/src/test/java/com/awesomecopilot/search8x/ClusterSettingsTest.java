package com.awesomecopilot.search8x;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2021-10-08 11:40
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ClusterSettingsTest {
	
	@Test
	public void testSetSearchMaxBuckets() {
		boolean updated = ElasticUtils.Cluster.settings()
				.persistent()
				.searchMaxBuckets(100000)
				.thenUpdate();
		
		assertTrue(updated);
	}
}