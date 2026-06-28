package com.awesomecopilot.search8x;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils.Admin;
import com.awesomecopilot.search8x.ElasticUtils.Mappings;
import com.awesomecopilot.search8x.ElasticUtils.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicMappingTest {
	
	/**
	 * <pre>
	 * DELETE dynamic_mapping_test
	 *
	 * PUT dynamic_mapping_test/_doc/1
	 * {
	 *     "field": "somevalue"
	 * }
	 * PUT dynamic_mapping_test/_doc/2
	 * {
	 *   "newField": "somevalue"
	 * }
	 *
	 * GET dynamic_mapping_test/_search
	 * </pre>
	 */
	@Test
	public void testDynamicMappingAddNewField() {
		boolean exists = Admin.existsIndex("dynamic_mapping_test");
		if (exists) {
			boolean deleted = Admin.deleteIndex("dynamic_mapping_test");
		}
		
		String id = ElasticUtils.create("dynamic_mapping_test", """
				{
				    "field": "somevalue"
				}""", 1);
		String id2 = ElasticUtils.create("dynamic_mapping_test", """
				{
				  "newField": "somevalue"
				}
				""", 2);
		
		List<Object> docs = Query.query("dynamic_mapping_test").queryForList(); //实际执行的就是一个GET dynamic_mapping_test/_search
		for (Object doc : docs) {
			System.out.println(doc);
		}
	}
	
	/**
	 * <pre>
	 * PUT dynamic_mapping_test/_mapping
	 * {
	 *   "dynamic": false
	 * }
	 * </pre>
	 */
	@Test
	public void testModifyDynamicProperty() {
		boolean created = ElasticUtils.Mappings.putMapping("dynamic_mapping_test", false).thenCreate();
		assertTrue(created);
		Map<String, Object> mapping = Mappings.getMapping("dynamic_mapping_test");
		System.out.println(JacksonUtils.toPrettyJson( mapping));
		String id = ElasticUtils.create("dynamic_mapping_test", """
				{
				  "anotherField": "somevalue"
				}""", 3);
		assertEquals("3", id);
		List<Object> objects = Query.matchQuery("dynamic_mapping_test").query("anotherField", "somevalue").queryForList();
		assertEquals(0, objects.size());
	}
}
