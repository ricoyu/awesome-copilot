package com.awesomecopilot.search;

import com.awesomecopilot.search.enums.FieldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2021-06-02 9:28
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SettingsTest {
	
	@Test
	public void testMakeSettings() {
		ElasticUtils.Admin.deleteIndex("test666");
		ElasticUtils.Admin.createIndex("test666")
				.settings()
				.numberOfShards(1)
				.numberOfReplicas(1)
				.and()
				.mapping()
				.field("name", FieldType.KEYWORD)
				.thenCreate();
	}
	
	@Test
	public void testPutSettings() {
		ElasticUtils.Admin.deleteIndex("my_product");
		boolean created = ElasticUtils.Admin.createIndex("my_product")
				.settings()
				.numberOfShards(1)
				.numberOfReplicas(0)
				.thenCreate();
		boolean acknowlodged = ElasticUtils.Settings.putSettings("my_product", """
				{
				  "number_of_replicas": 1,
				  "refresh_interval": "30s",
				  "index.max_result_window": 20000,
				}""");
		
		assertTrue(acknowlodged);
	}
}