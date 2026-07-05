package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.Slices;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;

/**
 * <p>
 * Copyright: (C), 2021-03-11 13:53
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticReindexBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticReindexBuilder.class);

	private Query filter;

	private String srcIndex;

	private String destIndex;

	/**
	 * 可以把Reindex拆分成几个子任务并发执行
	 * Reindex supports Sliced scroll to parallelize the reindexing process. 
	 * This parallelization can improve efficiency and provide a convenient way to break the request down into smaller parts.
	 */
	private int slices;

	/**
	 * 一次操作多少文档
	 */
	private int size;

	public ElasticReindexBuilder(String srcIndex, String destIndex) {
		this.srcIndex = srcIndex;
		this.destIndex = destIndex;
	}

	/**
	 * 过滤出一部分文档进行Reindex
	 * @param filter
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder filter(Query filter) {
		this.filter = filter;
		return this;
	}

	/**
	 * Reindex的目标索引
	 * @param destIndex
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder dest(String destIndex) {
		this.destIndex = destIndex;
		return this;
	}

	/**
	 * 可以把Reindex拆分成几个子任务并发执行
	 * @param slices
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder slices(int slices) {
		this.slices = slices;
		return this;
	}

	/**
	 * 一次batch Reindex多少文档
	 * @param size
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder size(int size) {
		this.size = size;
		return this;
	}

	/**
	 * 直接执行Reindex操作, 返回Reindex结果
	 * @return ReindexResponse
	 */
	public ReindexResponse get() {
		notNull(srcIndex, "srcIndex 不能为null");
		notNull(destIndex, "destIndex 不能为null");

		try {
			ReindexRequest request = ReindexRequest.of(b -> {
				b.source(s -> {
					s.index(srcIndex);
					if (filter != null) {
						s.query(filter);
					}
					return s;
				});
				b.dest(d -> d.index(destIndex));
				if (size != 0) {
					b.maxDocs((long) size);
				}
				if (slices != 0) {
					b.slices(Slices.of(sl -> sl.value(slices)));
				}
				b.conflicts(Conflicts.Proceed);
				return b;
			});

			ReindexResponse response = ElasticUtils.QUERY_CLIENT.reindex(request);
			log.info("Reindex response: total={}, created={}, updated={}, deleted={}",
					response.total(), response.created(), response.updated(), response.deleted());
			return response;
		} catch (IOException e) {
			throw new RuntimeException("Failed to reindex from " + srcIndex + " to " + destIndex, e);
		}
	}
}
