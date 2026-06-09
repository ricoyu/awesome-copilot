package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ES REST 客户端可正常初始化 (替代原 transport-client 测试)。
 */
@Slf4j
public class TransportClientTest {

	private static RestHighLevelClient client;

	@BeforeAll
	static void init() {
		client = ElasticsearchClientFactory.createHighLevelClient();
	}

	@Test
	void clientShouldNotBeNull() {
		assertThat(client).isNotNull();
	}
}
