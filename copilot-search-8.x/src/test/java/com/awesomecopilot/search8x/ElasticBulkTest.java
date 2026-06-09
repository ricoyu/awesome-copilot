package com.awesomecopilot.search8x;

import com.awesomecopilot.common.lang.enums.Gender;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.enums.FieldType;
import com.awesomecopilot.search8x.support.BulkResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2021-08-31 14:01
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class ElasticBulkTest {
	
	@Test
	public void testBulkInsertWithEmptyData() {
		//BulkResult bulkResult = ElasticUtils.bulkIndex("test_index-001", asList());
		//assertThat(bulkResult.getSuccessCount()).isEqualTo(0);
	}
	
	/**
	 * <pre>
	 * POST /products/_bulk
	 * { "index": { "_id": "1" } }
	 * { "title": "Apple Watch Series 7", "description": "The future of health is on your wrist." }
	 * { "index": { "_id": "2" } }
	 * { "title": "Samsung Galaxy Watch", "description": "Meet the new face of Android smartwatches." }
	 * { "index": { "_id": "3" } }
	 * { "title": "Fitbit Versa", "description": "Your all-day health and fitness companion." }
	 * </pre>
	 */
	@Test
	public void testBulkProducts() {
		boolean deleted = ElasticUtils.Admin.deleteIndex("products");
		boolean productsExisted = ElasticUtils.Admin.existsIndex("products");
		assertFalse(productsExisted);
		boolean created = ElasticUtils.Admin.createIndex("products").settings().numberOfShards(1).numberOfReplicas(0).thenCreate();
		productsExisted = ElasticUtils.Admin.existsIndex("products");
		assertTrue(productsExisted);
		ElasticUtils.Mappings.putMapping("products", Dynamic.FALSE)
				.field("title", FieldType.TEXT)
				.field("description", FieldType.TEXT)
				.thenCreate();
		
		List<Product> products = new ArrayList<>();
		products.add(new Product("Apple Watch Series 7", "The future of health is on your wrist."));
		products.add(new Product("Samsung Galaxy Watch", "Meet the new face of Android smartwatches."));
		products.add(new Product("Fitbit Versa", "Your all-day health and fitness companion."));
		
		BulkResult bulkResult = ElasticUtils.bulkIndex("products", products);
		assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Product {
		private String title;
		private String description;
	}
	
	@Test
	public void testBulkIndex() {
		try {
			ElasticUtils.Admin.deleteIndex("employees1");
		} catch (Exception e) {
			log.error("", e);
		}
		BulkResult bulkResult = ElasticUtils.bulkIndex("employees1")
				.docs("{ \"name\" : \"Emma\",\"age\":32,\"job\":\"Product Manager\",\"gender\":\"female\",\"salary\":35000 }",
						"{ \"name\" : \"Underwood\",\"age\":41,\"job\":\"Dev Manager\",\"gender\":\"male\",\"salary\": 50000}",
						"{ \"name\" : \"Tran\",\"age\":25,\"job\":\"Web Designer\",\"gender\":\"male\",\"salary\":18000 }")
				.refresh(true)
				.execute();
		assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
		
		List<Object> employees1 = ElasticUtils.Query.matchAllQuery("employees1").queryForList();
		assertThat(employees1.size()).isEqualTo(3);
	}
	
	@Test
	public void testBulkIndex2() {
		try {
			ElasticUtils.Admin.deleteIndex("employees2");
		} catch (Exception e) {
			log.error("", e);
		}
		List<Employee> employees =
				asList("{ \"name\" : \"Emma\",\"age\":32,\"job\":\"Product Manager\",\"gender\":\"female\",\"salary\":35000 }",
						"{ \"name\" : \"Underwood\",\"age\":41,\"job\":\"Dev Manager\",\"gender\":\"male\",\"salary\": 50000}",
						"{ \"name\" : \"Tran\",\"age\":25,\"job\":\"Web Designer\",\"gender\":\"male\",\"salary\":18000 }")
						.stream()
						.map((json) -> JacksonUtils.toObject(json, Employee.class))
						.collect(toList());
		BulkResult bulkResult = ElasticUtils.bulkIndex("employees2")
				.docs(employees)
				.refresh(true)
				.execute();
		assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
		
		List<Object> employees1 = ElasticUtils.Query.matchAllQuery("employees2").queryForList();
		assertThat(employees1.size()).isEqualTo(3);
	}
	
	@Data
	private static class Employee {
		private String name;
		
		private Integer age;
		
		private String job;
		
		private Gender gender;
		
		private BigDecimal salary;
	}
	
	@Test
	public void testBUlkIndex() {
		BulkResult bulkResult = ElasticUtils.bulkIndex("products")
				.docs("{\"productID\": \"XHDK-A-1293-#fJ3\", \"desc\": \"iPhone\"}",
						"{\"productID\": \"KDKE-B-9947-#kL5\", \"desc\": \"iPad\"}",
						"{\"productID\": \"JODL-X-1937-#pV7\", \"desc\": \"MBP\"}")
				.execute();
		System.out.println(toJson(bulkResult));
	}
}