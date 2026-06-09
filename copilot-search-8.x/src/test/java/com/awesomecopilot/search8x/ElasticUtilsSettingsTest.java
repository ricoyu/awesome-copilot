package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElasticUtilsSettingsTest {
	
	@Test
	public void testUpdateReplicas() {
		boolean updated = ElasticUtils.Settings.update("product")
				.numberOfReplicas(0)
				.thenUpdate();
		
		assertTrue(updated) ;
	}
}