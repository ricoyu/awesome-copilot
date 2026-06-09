package com.awesomecopilot.search8x.support;

import org.elasticsearch.index.query.QueryBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Copyright: (C), 2021-04-29 11:47
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class LogSupport {
	
	private static final Logger log = LoggerFactory.getLogger(LogSupport.class);
	
	/**
	 * 打印QueryDSL
	 * @param builder
	 */
	public static void logQueryDsl(QueryBuilder builder) {
		if (log.isDebugEnabled()) {
			log.debug("Query DSL:\n{}", new JSONObject(builder.toString()).toString(2));
		}
	}
}