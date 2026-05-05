package com.awesomecopilot.json;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * Copyright: (C), 2021-04-29 16:47
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class JsonPathUtilsTest2 {
	
	@Test
	public void test() {
		String json = IOUtils.readClassPathFileAsString("es-data.json");
		List<Movie> movies = JsonPathUtils.readListNode(json, "$.hits.hits[*]._source", Movie.class);
		movies.forEach(System.out::println);
		JsonPathUtils.readNode(json, "$.hits");
	}

	@Test
	public void testParseChildTask() {
		String jsonTask = IOUtils.readClassPathFileAsString("childTask.json");
		String equipmentId = JsonPathUtils.readNode(jsonTask, "$.EquipmentId");
		assertEquals(equipmentId, "TAMR006");
		String loadingPlatform = JsonPathUtils.readNode(jsonTask, "$.LoadingPlatform");
		assertEquals(loadingPlatform, "0");
		List<String> PathArr = JsonPathUtils.readListNode(jsonTask, "$.PathArr");
		assertThat(PathArr.size()).isEqualTo(1);
		String speed = JsonPathUtils.readNode(jsonTask, "$.PathArr[0].Speed");
		assertEquals(speed, "2");
		String startPosition = JsonPathUtils.readNode(jsonTask, "$.PathArr[0].StartPosition");
		String srcColumn = startPosition.substring(0, 2);
		assertEquals(srcColumn, "0D");
		String srcRow = startPosition.substring(2);
		assertEquals(srcRow, "05");
		String endPosition = JsonPathUtils.readNode(jsonTask, "$.PathArr[0].EndPosition");
		String dstColumn = endPosition.substring(0, 2);
		assertEquals(dstColumn, "0C");
		String dstRow = endPosition.substring(2);
		assertEquals(dstRow, "05");

	}
	
}