package com.awesomecopilot.common.lang.resource;

import com.awesomecopilot.common.lang.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * Copyright: (C), 2021-01-21 11:28
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class YamlReaderTest {
	
	@Test
	public void test() {
		YamlReader yamlReader = new YamlReader("application");
		String activeProfile = yamlReader.getString("spring.profiles.active");
		System.out.println(activeProfile);
		assertEquals(activeProfile, "prod");
		System.out.println(System.getProperty("user.dir"));
	}
	
	@Test
	public void testK8sYaml() throws IOException {
		InputStream inputStream = IOUtils.readFileAsStream("D:\\Dropbox\\Docker & Kubernetes\\tulingmall-gateway-ingress.yaml");
		Map<String, Object> map = new Yaml().load(inputStream);
		for (String s : map.keySet()) {
			System.out.println(s+": " + map.get(s));
		}
	}
	
	@Test
	public void testbootstrapYaml() throws IOException {
		//2026-09-21 改造: 原来读另一个仓库(D:\Learning\awesome-plus)的绝对路径, 该仓库不存在/未克隆时必失败;
		//改为读本模块 test/resources 下的示例文件, 并补真断言(原方法只打印, 没有校验)
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("bootstrap-sample.yaml");
		Map<String, Object> map = new Yaml().load(inputStream);
		Map<?, ?> spring = (Map<?, ?>) map.get("spring");
		assertEquals("dev", ((Map<?, ?>) spring.get("profiles")).get("active"));
		assertEquals("awesome-order", ((Map<?, ?>) spring.get("application")).get("name"));
	}
}
