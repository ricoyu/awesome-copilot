package com.awesomecopilot.nacos;

import com.awesomecopilot.networking.utils.HttpUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;

public class NacosOpenAPITest {

	@Test
	public void testRegister() {
		Map<String, Object> body = new HashMap<>();
		body.put("serviceName", "mockService");
		body.put("port", "8080");
		body.put("ip", "127.0.0.1");
		//Object result = HttpUtils.post("http://localhost:8080/api/poetry/body")
		//		.body(body)
		//		.request();
		Object result = HttpUtils.form("http://localhost:8848/nacos/v2/ns/instance")
				.formData("serviceName", "mockService")
				.formData("port", "8080")
				.formData("ip", "127.0.0.1")
				.request();

		System.out.println(result);
		//System.out.println("注册" + (result ? "成功" : "失败"));
	}

	@Test
	public void testGetInstanceDetail() {
		String result = HttpUtils.get("http://localhost:8848/nacos/v2/ns/instance")
				.addParam("serviceName", "product-service")
				.addParam("port", "8086")
				.addParam("ip", "192.168.100.106")
				.request();

		System.out.print(toPrettyJson(result));
	}

	@Test
	public void testGetServiceDetail() {
		String result = HttpUtils.get("http://localhost:8848/nacos/v2/ns/service")
				.addParam("serviceName", "product-service")
				.request();

		System.out.print(result);
	}

}
