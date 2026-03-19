package com.awesomecopilot.search;

import com.awesomecopilot.search.support.UpdateResult;
import com.awesomecopilot.search.vo.VersionedDoc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2023-08-14 15:32
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticUtilsVersionControllTest {
	
	@Test
	public void testGet() {
		VersionedDoc<String> versionedResult = ElasticUtils.getWithVersion("products", 1);
		System.out.println(versionedResult.getSource());
		
		UpdateResult updateResult = ElasticUtils.update("products")
				.id(versionedResult.getId())
				.doc("{\"title\": \"iphone\", \"count\": 98 }")
				.ifPrimaryTerm(333L)
				.ifSeqNo(versionedResult.getIfSeqNo())
				.update();
		
		assertTrue(updateResult.getResult() == UpdateResult.Result.UPDATED);
		
		updateResult = ElasticUtils.update("products")
				.id(versionedResult.getId())
				.doc("{\"title\": \"iphone\", \"count\": 97 }")
				.ifPrimaryTerm(versionedResult.getIfPrimaryTerm())
				.ifSeqNo(versionedResult.getIfSeqNo())
				.update();
		
		assertTrue(updateResult.getResult() == UpdateResult.Result.UPDATED);
	}
	
	@Test
	public void testUpdateEmployee() {
		VersionedDoc<String> employees = ElasticUtils.getWithVersion("employees", 1);
		System.out.println(employees.getSource());
		
		/*
		 * 等价于
		 * <pre>
		 * POST /employees/_update/1
		 * {
		 *   "doc": {
		 *     "age": 44
		 *   }
		 * }
		 * </pre>
		 * 这里更新的文档不要加"doc"
		 */
		UpdateResult updateResult = ElasticUtils.update("employees")
				.id(employees.getId())
				.doc("""
						{
						    "age": 43
						}""")
				.ifSeqNo(employees.getIfSeqNo())
				.ifPrimaryTerm(employees.getIfPrimaryTerm())
				.update();
		assertTrue(updateResult.getResult() == UpdateResult.Result.UPDATED);
	}
}