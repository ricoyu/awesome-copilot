package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.builder.query.ElasticScrollQueryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.search8x.ElasticUtils.Admin.deleteIndex;
import static com.awesomecopilot.search8x.ElasticUtils.Admin.existsIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * scroll 上下文清理的回归测试（评估报告 P0-8）。
 * <p>
 * 原缺陷：ElasticScrollQueryBuilder 遍历结束（空批次）、初始查询无命中、
 * 中途抛异常这三条路径都不释放 scroll 上下文，全集群的上下文堆到
 * search.max_open_scroll_context（默认 500）后所有新 scroll 请求被拒。
 * <p>
 * 断言方式：直查真实 ES 的 _nodes/stats/indices/search 数 open_contexts，
 * 验证每条路径跑完后回到测试前基线（clear 生效可能有短暂延迟，用轮询等待）。
 * 低层 RestClient 凭据与 src/test/resources/elastic.properties 一致。
 *
 * @author Rico Yu
 */
@Slf4j
class ScrollCleanupTest {
	
	private static final String INDEX = "audit_scroll_cleanup_test";
	private static final int DOC_COUNT = 120;
	
	private static volatile RestClient restClient;
	
	@AfterAll
	static void closeRestClient() throws Exception {
		if (restClient != null) {
			restClient.close();
			restClient = null;
		}
	}
	
	@BeforeEach
	@AfterEach
	void prepareIndex() {
		if (existsIndex(INDEX)) {
			deleteIndex(INDEX);
		}
	}
	
	private void indexDocs() throws Exception {
		List<Map<String, Object>> docs = new ArrayList<>();
		for (int i = 0; i < DOC_COUNT; i++) {
			Map<String, Object> doc = new HashMap<>();
			doc.put("seq", i);
			doc.put("title", "doc-" + i);
			docs.add(doc);
		}
		ElasticUtils.bulkIndex(INDEX, docs);
		ElasticUtils.QUERY_CLIENT.indices().refresh(r -> r.index(INDEX));
	}
	
	private synchronized RestClient restClient() {
		if (restClient == null) {
			BasicCredentialsProvider creds = new BasicCredentialsProvider();
			creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "123456"));
			restClient = RestClient.builder(new HttpHost("172.30.31.182", 9200, "http"))
					.setHttpClientConfigCallback(cb -> cb.setDefaultCredentialsProvider(creds))
					.build();
		}
		return restClient;
	}
	
	/** 集群当前打开的 scroll 上下文总数(所有节点相加) */
	private long openScrollContexts() throws Exception {
		Response response = restClient().performRequest(new Request("GET", "/_nodes/stats/indices/search"));
		String json = new String(response.getEntity().getContent().readAllBytes());
		// 不引额外 JSON 库, 直接提取 "open_contexts":N 求和
		long total = 0;
		int idx = 0;
		String marker = "\"open_contexts\":";
		while ((idx = json.indexOf(marker, idx)) >= 0) {
			int start = idx + marker.length();
			int end = start;
			while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
				end++;
			}
			total += Long.parseLong(json.substring(start, end));
			idx = end;
		}
		return total;
	}
	
	/** 轮询等待上下文数回落到基线(最多 5 秒); 断言用 <= 判定, 避免别处遗留的上下文恰在
	 * 轮询窗口内到期把计数压到基线以下造成误判 */
	private long awaitContexts(long baseline) throws Exception {
		long current = openScrollContexts();
		for (int i = 0; i < 25 && current > baseline; i++) {
			Thread.sleep(200);
			current = openScrollContexts();
		}
		return current;
	}
	
	@Test
	@DisplayName("遍历到空批次后, scroll 上下文必须被释放(open_contexts 回到基线)")
	void exhaustiveTraversalReleasesContext() throws Exception {
		indexDocs();
		long baseline = openScrollContexts();
		
		ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery(INDEX).size(50);
		int rounds = 0;
		while (true) {
			List<String> batch = builder.queryForList();
			if (batch.isEmpty()) {
				break;
			}
			rounds += batch.size();
			if (rounds > 10_000) {
				throw new IllegalStateException("traversal runaway");
			}
		}
		// 120 条文档, 每批 50: 应有 50+50+20+空 共 4 轮
		assertEquals(DOC_COUNT, rounds);
		assertTrue(awaitContexts(baseline) <= baseline,
				"遍历耗尽后 open_contexts 未回落——上下文没被 clear, 只等到期回收");
	}
	
	@Test
	@DisplayName("初始查询就无命中时, 已创建的上下文同样要释放")
	void initialSearchWithNoHitsReleasesContext() throws Exception {
		indexDocs();
		long baseline = openScrollContexts();
		
		// match_none 的查询: 初始 scroll 查询返回空批次但服务端已创建上下文
		ElasticUtils.Query.scrollQuery(INDEX)
				.size(10)
				.queryBuilder(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(
						q -> q.matchNone(m -> m)))
				.queryForList();
		
		assertTrue(awaitContexts(baseline) <= baseline,
				"空结果集的初始 scroll 也创建了上下文, 返回前必须 clear");
	}
	
	@Test
	@DisplayName("scroll 调用抛异常时, 本 builder 已创建的上下文不得遗留")
	void exceptionDuringScrollReleasesContext() throws Exception {
		indexDocs();
		long baseline = openScrollContexts();
		
		ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery(INDEX).size(50);
		builder.queryForList();                            // 初始查询: 服务端已创建上下文
		builder.scrollId("ZmFrZS1zY3JvbGwtaWQtYnVnZ2Vk");  // 换成失效 id, 下一轮必抛
		assertThrows(RuntimeException.class, builder::queryForList);
		
		assertTrue(awaitContexts(baseline) <= baseline,
				"异常路径遗留了第一轮创建的 scroll 上下文");
	}
	
	@Test
	@DisplayName("结果解析异常不释放上下文: catch 后修正 resultType 重试, 从下一批继续")
	void parseErrorKeepsContextForRetry() throws Exception {
		indexDocs();
		ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery(INDEX).size(50);
		List<String> first = builder.resultType(String.class).queryForList();
		assertEquals(50, first.size());
		
		// 制造一次"本批必然解析失败"的运行时异常: 文档是对象, 强转 Integer 必抛
		assertThrows(Exception.class, () -> builder.resultType(Integer.class).queryForList());
		
		// 解析异常路径不 close: scrollId 仍在, 重试跳过坏批拿下一批(120-50-50=最后 20 条)
		List<String> retry = builder.resultType(String.class).queryForList();
		assertEquals(20, retry.size(), "解析异常后重试应拿到下一批(末尾 20 条)");
		assertTrue(retry.get(0).contains("\"seq\":100"),
				"重试结果应从 seq=100 开始(说明上下文被保留、坏批被跳过), 实际: " + retry.get(0));
		builder.close();
	}
	
	@Test
	@DisplayName("中途放弃遍历时, try-with-resources 的 close() 释放上下文且幂等")
	void earlyAbandonClosesViaTryWithResources() throws Exception {
		indexDocs();
		long baseline = openScrollContexts();
		
		int collected = 0;
		try (ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery(INDEX).size(50)) {
			List<String> first = builder.queryForList();
			collected = first.size();
			// 只取第一批就走人(导出中断/异常return的典型形态)
		}
		assertEquals(50, collected);
		assertTrue(awaitContexts(baseline) <= baseline,
				"提前放弃遍历后上下文未释放——close() 没起作用");
		
		closeThenReuseStartsFreshScan();
	}
	
	/** close() 之后 scrollId 已置空, 同一 builder 再次 queryForList 应发起新的初始查询而不是报错 */
	private void closeThenReuseStartsFreshScan() throws Exception {
		ElasticScrollQueryBuilder builder = ElasticUtils.Query.scrollQuery(INDEX).size(10);
		builder.queryForList();          // 初始
		builder.close();                 // 释放
		List<String> again = builder.queryForList();  // 再查: 新初始查询
		assertEquals(10, again.size(), "close 后复用应能重新发起初始查询");
		builder.close();
	}
}
