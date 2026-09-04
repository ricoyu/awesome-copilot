package com.awesomecopilot.json;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jackson.JsonNodeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * Copyright: (C), 2021-09-02 10:25
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JsonNodeUtilsTest {
	
	private static final Logger log = LoggerFactory.getLogger(JsonNodeUtilsTest.class);
	
	@Test
	@SneakyThrows
	public void test() {
		JsonNode node = JacksonUtils.objectMapper().readTree(IOUtils.readClassPathFileAsBytes("array-field.json"));
		JsonNode jsonNode = node.get("dns_grouped_A");
		if (jsonNode == null) {
			log.info("node not exists");
		}
		
		if (jsonNode.isNull()) {
			log.info("is null");
		}
		
		if (jsonNode.isArray()) {
			List<String> values = new ArrayList<>();
			Iterator<JsonNode> elements = jsonNode.elements();
			log.info("node is array");
			while (elements.hasNext()) {
				JsonNode childNode =  elements.next();
				String nodeValue = childNode.textValue();
				values.add(nodeValue);
			}
			values.forEach(System.out::println);
		}
		
		jsonNode = node.get("dns_answers");
		if (jsonNode.isArray()) {
			Iterator<JsonNode> elements = jsonNode.elements();
			List<DnsAnswer> dnsAnswers = new ArrayList<>();
			while (elements.hasNext()) {
				JsonNode next = elements.next();
				DnsAnswer dnsAnswer = JacksonUtils.toObject(next.toString(), DnsAnswer.class);
				dnsAnswers.forEach(System.out::println);
			}
		}
	}
	
	@Test
	@SneakyThrows
	public void testReadListStringWithoutQuotes() {
		JsonNode node = JacksonUtils.objectMapper().readTree(IOUtils.readClassPathFileAsBytes("array-field.json"));
		List<String> values = JsonNodeUtils.readList(node, "dns_grouped_A");
		assertThat(values).containsExactly("180.101.49.11", "180.101.49.12");
	}

	@Test
	@SneakyThrows
	public void testReadListObjects() {
		JsonNode node = JacksonUtils.objectMapper().readTree(IOUtils.readClassPathFileAsBytes("array-field.json"));
		List<DnsAnswer> answers = JsonNodeUtils.readList(node, "dns_answers", DnsAnswer.class);
		assertThat(answers).hasSize(3);
		assertThat(answers.get(0).getRrname()).isEqualTo("www.baidu.com");
		assertThat(answers.get(0).getRrtype()).isEqualTo("CNAME");
		assertThat(answers.get(0).getTtl()).isEqualTo(600L);
		assertThat(answers.get(0).getRdata()).isEqualTo("www.a.shifen.com");
	}

	@Test
	@SneakyThrows
	public void testReadArrObjects() {
		JsonNode node = JacksonUtils.objectMapper().readTree(IOUtils.readClassPathFileAsBytes("array-field.json"));
		DnsAnswer[] answers = JsonNodeUtils.readArr(node, "dns_answers", DnsAnswer.class);
		assertThat(answers).hasSize(3);
		assertThat(answers[1].getRrname()).isEqualTo("www.a.shifen.com");
		assertThat(answers[1].getRrtype()).isEqualTo("A");
		assertThat(answers[1].getTtl()).isEqualTo(600L);
		assertThat(answers[1].getRdata()).isEqualTo("180.101.49.11");
	}

	@Data
	public static class DnsAnswer {
		
		private String rrname;
		
		/**
		 * 解析记录(左边a cname)
		 */
		private String rrtype;
		
		private Long ttl;
		
		/**
		 * 解析记录(右边 10.21.86.106 a.1.com)
		 */
		private String rdata;
	}
}