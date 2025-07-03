package com.awesomecopilot.json;

import com.awesomecopilot.common.lang.utils.DateUtils;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.common.lang.vo.Result;
import com.awesomecopilot.common.lang.vo.Results;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * <p>
 * Copyright: (C), 2019/12/21 19:15
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JacksonUtilsTest {
	
	@Test
	public void testSerDeserPlainString() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String str = "aa";
		System.out.println(str + " , length=" + str.length());
		
		String json = objectMapper.writeValueAsString(str);
		String toJson = toJson(str);
		System.out.println(toJson + " , length=" + str.length());
		System.out.println(json + " , length=" + str.length());
		//assertEquals(json, toJson);
		
		/*String simpleJson = IOUtils.readClassPathFileAsString("simple-json.txt");
		System.out.println(JacksonUtils.toObject(simpleJson, String.class));*/
	}
	
	@Test
	public void testDeserialLocalDateTime() {
		String s = "2019-12-21 19:15:34";
		String dateStr = toJson(new DateObj(LocalDateTime.of(2019, 12, 21, 19, 15, 34)));
		System.out.println(dateStr);
		DateObj dateObj = JacksonUtils.toObject(dateStr, DateObj.class);
		assertEquals(dateObj.getDatetime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), s);
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class DateObj {
		private LocalDateTime datetime;
	}
	
	@Test
	public void testJson2Map() {
		String jsonString = "{\"name\":\"Mahesh\", \"age\":21}";
		Map<String, Object> params = JacksonUtils.toMap(jsonString);
		Map<Object, Object> genericMap = JacksonUtils.toGenericMap(jsonString);
		assertThat(params.get("name")).isEqualTo("Mahesh");
		params = JacksonUtils.toMap(jsonString);
		assertThat(params.get("name")).isEqualTo("Mahesh");
	}
	
	@Test
	public void testPojoMap() {
		Map<String, Object> params = new HashMap<>();
		params.put("fullname", "三少爷");
		params.put("birthday", "2020-03-20");
		Person person = JacksonUtils.mapToPojo(params, Person.class);
		System.out.println(person);
		//Assert.assertEquals("三少爷", person.getFullName());
		Map<String, Object> resultMap = JacksonUtils.pojoToMap(person);
		System.out.println(resultMap);
		assertEquals("2020-03-20", resultMap.get("birthday"));
	}
	
	@Test
	public void testMongoDocumentJson() {
		String json = IOUtils.readClassPathFileAsString("mongodbDocumentJson.json");
		UserInfo userInfo = JacksonUtils.toObject(json, UserInfo.class);
		System.out.println(userInfo.getId());
	}
	
	@Test
	public void testSerializeSingleValue() {
		System.out.println(toJson(1L));
		System.out.println(toJson("hi"));
		System.out.println(toJson(new Object[]{1L, new Date(), null}));
	}
	
	@Test
	public void testDeserializer2Long() {
		String json = "{  \n" +
				"\"timestamp\":\"2020-07-23 14:12:22\"\n" +
				" }  ";
		SimpleEvent event = JacksonUtils.toObject(json, SimpleEvent.class);
		Long timestamp = DateUtils.parse("2020-07-23 14:12:22").getTime();
		assertEquals(event.getTimestamp(), timestamp);
	}
	
	@Test
	public void test() {
		String json = "{\n" +
				"        \"flow_id\": 1479552517000986,\n" +
				"        \"signature_id\": 832661232805232640\n" +
				"}";
		
		CustomRuleEvent customRuleEvent = JacksonUtils.toObject(json, CustomRuleEvent.class);
	}
	
	@Test
	public void testSerializeRawString() {
		String eventJson = IOUtils.readClassPathFileAsString("ids-event1.json");
		byte[] bytes = JacksonUtils.toBytes(eventJson);
		String deserializeback = new String(bytes, UTF_8);
		System.out.println(deserializeback);
		
		System.out.println(JacksonUtils.toPrettyJson(eventJson));
	}
	
	@Test
	public void testSerializeLitOfEnum() {
		SimpleEvent event = new SimpleEvent();
		event.getTypes().add(SimpleEvent.Type.DGA);
		event.getTypes().add(SimpleEvent.Type.DNS);
		event.getTypes().add(SimpleEvent.Type.HTTP);
		event.getTypes().add(SimpleEvent.Type.SMTP);
		String json = toJson(event);
		System.out.println(json);
	}
	
	@Data
	private static class SimpleEvent {
		
		private Long timestamp;
		
		private List<Type> types = new ArrayList<>();
		
		public static enum Type {
			HTTP, DNS, SMTP, DGA
		}
	}
	
	@Data
	private static class CustomRuleEvent  {
		/**
		 * 自定义规则序列id
		 */
		@JsonProperty("signature_id")
		private Long signatureId;
		
		/**
		 * 整型数字, 流编号
		 */
		@JsonProperty("flow_id")
		private Long flowId;
		
	}
	
	@Data
	public static class UserInfo {
		
		@JsonProperty("_id")
		private String id;
		/**
		 * 用户名
		 */
		private String username;
		/**
		 * 权限，角色
		 */
		private String role;
		/**
		 * 用户密码
		 */
		private String password;
		
		/**
		 * 昵称
		 */
		private String nickname;
		/**
		 * 邮箱
		 */
		private String email;
		
		/**
		 * 锁定状态
		 */
		private boolean lock;
		
		/**
		 * 机构ID
		 */
		@JsonProperty("organ_id")
		private String organId;
		
		/**
		 * 机构ID
		 */
		private Boolean auth;
		/**
		 * 机构
		 */
		@JsonIgnore
		private String organ;
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Person {
		
		//private String fullName;
		
		private LocalDate birthday;
	}
	
	@Test
	public void testDeserializeJsonIntTooLong() {
		String json = IOUtils.readClassPathFileAsString("intTooLong.json");
		Event event = JacksonUtils.toObject(json, Event.class);
		System.out.println(toJson(event));
		assertThat(event.getSrc_ip()).isEqualTo("192.168.43.65");
	}
	
	@Test
	public void testToMapPerf() {
		String json = IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\NTA测试数据\\ids-metadata-http.json");
		
		long begin = System.currentTimeMillis();
		for (int i = 0; i < 3000; i++) {
			JacksonUtils.toMap(json);
		}
		long end = System.currentTimeMillis();
		System.out.println((end- begin));
		
		begin = System.currentTimeMillis();
		for (int i = 0; i < 3000; i++) {
			JacksonUtils.toObject(json, NetLog.class);
		}
		end = System.currentTimeMillis();
		System.out.println((end- begin));
	}
	
	@Test
	public void testToJsonNode() {
		String json = IOUtils.readFileAsString("C:\\Users\\ricoy\\Documents\\ids-event-mqtt.json");
		JsonNode jsonNode = JacksonUtils.readTree(json);
	}

	@Test
	public void testToList() {
		String equipments = IOUtils.readClassPathFileAsString("equipment.json");
		List<Equipment> equipmentList = JacksonUtils.toList(equipments, Equipment.class);
		for (Equipment equipment : equipmentList) {
			System.out.println(equipment.getCreatedStamp());
		}

	}

	@Test
	public void testIsValidJson() {
		String str = IOUtils.readClassPathFileAsString("childTask.json");
		boolean valid = JacksonUtils.isValidJson(str);
		System.out.println(valid);
	}

	@Test
	public void testSerialzieListStrInPojo() throws JsonProcessingException {
		Spu spu = new Spu();
		//spu.setBirthday(LocalDateTime.now());
		spu.setName("test");
		List<String> urls = new ArrayList<>();
		urls.add("http://127.0.0.1:48080/admin-api/infra/file/4/get/d8c88ec4b865073945319d638f7159170b5dff988b9119642edcdb04eba7fe72.jpg");
		spu.setPicUrils(urls);
		Result<Spu> result = Results.<Spu>success().data(spu).build();
		//String json = toJson(result);
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(result);
		System.out.println(json);
	}

	@Data
	class Spu {
		private String name;

		//private LocalDateTime birthday;

		private List<String> picUrils;
	}

	@Test
	public void testIterateJsonNode() {
		String json = "[\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"报销公司\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"TextInput\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-edit\",\n" +
				"\t\t\t\t\t\t\t\"value\": \"\",\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"String\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": true,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field3496769157119\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"收款账户\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"TextInput\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-edit\",\n" +
				"\t\t\t\t\t\t\t\"value\": \"\",\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"String\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": true,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field6116787836602\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"报销金额\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"AmountInput\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"iconfont icon-zhufangbutiezhanghu\",\n" +
				"\t\t\t\t\t\t\t\"value\": \"\",\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"Number\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": true,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true,\n" +
				"\t\t\t\t\t\t\t\t\"showChinese\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field8719569158600\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"报销类别\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"TextInput\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-edit\",\n" +
				"\t\t\t\t\t\t\t\"value\": \"\",\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"String\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": true,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field3186687824078\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"费用明细\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"TextareaInput\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-more-outline\",\n" +
				"\t\t\t\t\t\t\t\"value\": \"\",\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"String\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": false,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field6147987854253\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"上传图片\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"ImageUpload\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-picture-outline\",\n" +
				"\t\t\t\t\t\t\t\"value\": [],\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"Array\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": false,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true,\n" +
				"\t\t\t\t\t\t\t\t\"maxSize\": 5,\n" +
				"\t\t\t\t\t\t\t\t\"maxNumber\": 10,\n" +
				"\t\t\t\t\t\t\t\t\"enableZip\": true\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field7382887862503\"\n" +
				"\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t{\n" +
				"\t\t\t\t\t\t\t\"title\": \"上传附件\",\n" +
				"\t\t\t\t\t\t\t\"name\": \"FileUpload\",\n" +
				"\t\t\t\t\t\t\t\"icon\": \"el-icon-folder-opened\",\n" +
				"\t\t\t\t\t\t\t\"value\": [],\n" +
				"\t\t\t\t\t\t\t\"valueType\": \"Array\",\n" +
				"\t\t\t\t\t\t\t\"props\": {\n" +
				"\t\t\t\t\t\t\t\t\"required\": false,\n" +
				"\t\t\t\t\t\t\t\t\"enablePrint\": true,\n" +
				"\t\t\t\t\t\t\t\t\"onlyRead\": false,\n" +
				"\t\t\t\t\t\t\t\t\"maxSize\": 100,\n" +
				"\t\t\t\t\t\t\t\t\"maxNumber\": 10,\n" +
				"\t\t\t\t\t\t\t\t\"fileTypes\": []\n" +
				"\t\t\t\t\t\t\t},\n" +
				"\t\t\t\t\t\t\t\"id\": \"field9884487863703\"\n" +
				"\t\t\t\t\t\t}\n" +
				"\t\t\t\t\t]";
		JsonNode jsonNode = JacksonUtils.readTree(json);
		jsonNode.forEach(node -> {
			JsonNode title = node.get("title");
			System.out.println(title.textValue());
			JsonNode propsNode = node.get("props");
			JsonNode id = node.get("id");
			JsonNode required = propsNode.get("required");
			System.out.println(id);
			System.out.println(required.booleanValue());
		});
	}
	
	
	@Data
	private static class Event implements Serializable {
		
		private String timestamp;
		private Long flow_id;
		private String in_iface;
		private String event_type;
		private List<Integer> vlan;
		private String src_ip;
		private Long src_port;
		private String dest_ip;
		private Long dest_port;
		private String proto;
		private Metadata metadata;
		private Alert alert;
		private Smtp smtp;
		private String app_proto;
		private String app_proto_ts;
		private Flow flow;
		private String payload;
		private Long stream;
		private String pcap;
		private Http http;
		private Dns dns;
		private Dga dga;
		
		private String id;
		private Long datetime;
		private String src_country;
		private String src_country_code;
		private String src_longitude;
		private String src_latitude;
		private String dest_country;
		private String dest_country_code;
		private String dest_longitude;
		private String dest_latitude;
		private String probe_ip;
		private String probe_name;
		private String engine;
		private String certainty;
		
		
		@Data
		public static class Metadata implements Serializable {
			private List<String> flowbits;
		}
		
		@Data
		public static class Alert implements Serializable {
			private String action;
			private Long gid;
			private Long signature_id;
			private Long rev;
			private String signature;
			@JsonProperty(index = 0)
			private String category;
			private Long severity;
			private String risk_level;
			private String name;
			private String rule_type;
			private String metadata;
		}
		
		@Data
		public static class Smtp implements Serializable {
			
		}
		
		@Data
		public static class Flow implements Serializable {
			private Long pkts_toserver;
			private Long pkts_toclient;
			private Long bytes_toserver;
			private Long bytes_toclient;
			private String start;
		}
		
		@Data
		public static class Http implements Serializable {
			@Data
			public static class Header implements Serializable {
				private String name;
				private String value;
				
				public String toString() {
					return String.format("{\"name\":\"%s\",\"value\":\"%s\"}", name, value);
				}
			}
			
			private String hostname;
			private String url;
			private String http_user_agent;
			private String http_content_type;
			private String http_method;
			private String protocol;
			private Long status;
			private Long length;
			private List<Header> request_headers;
			private List<Header> response_headers;
			private String httpRequestBody;
			private String httpResponseBody;
		}
		
		
		@Data
		public static class Dns implements Serializable {
			@Data
			public static class Answer implements Serializable {
				@Data
				public static class Authority implements Serializable {
					private String rrname;
					private String rrtype;
					private Integer ttl;
				}
				
				private Integer id;
				private Integer version;
				private String type;
				private String flags;
				private Boolean qr;
				private Boolean aa;
				private Boolean ra;
				private String rrname;
				private String rrtype;
				private String rcode;
				private List<Authority> authorities;
				private String A;
			}
			
			private List<Object> query;
			private Answer answer;
			private Boolean answer_result;
		}
		
		@Data
		public static class Dga implements Serializable {
			private String domain;
			
			
		}


	}
}
