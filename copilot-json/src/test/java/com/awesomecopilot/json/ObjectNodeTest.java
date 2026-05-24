package com.awesomecopilot.json;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class ObjectNodeTest {
	
	@Test
	public void testCreate() {
		ObjectNode json = JacksonUtils.createJsonObject();
		json.put("name", "三少爷");
		json.put("age", 18);
		System.out.println(JacksonUtils.toJson(json));
	}
}
