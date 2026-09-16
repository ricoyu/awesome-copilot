package com.awesomecopilot.search;

import com.awesomecopilot.networking.utils.HttpUtils;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * Copyright: (C), 2020-11-30 8:57
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ESIndexTest {
	
	/**
	 * 查看indices
	 */
	@Test
	public void testCat() {
		// 2026-09-16 评审报告 P2-4: HttpClientUtils 整类删除, 迁移到门面 HttpUtils
		String response = HttpUtils.get("http://192.168.100.104:9200/_cat/indices/kibana*?v&s=index").request();
		System.out.println(response);
	}
}