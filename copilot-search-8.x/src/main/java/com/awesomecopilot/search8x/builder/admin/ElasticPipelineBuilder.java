package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.ingest.DeletePipelineRequest;
import co.elastic.clients.elasticsearch.ingest.GetPipelineRequest;
import co.elastic.clients.elasticsearch.ingest.GetPipelineResponse;
import co.elastic.clients.elasticsearch.ingest.PutPipelineRequest;
import co.elastic.clients.elasticsearch.ingest.PutPipelineResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.CreatePipelineException;
import jakarta.json.stream.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * URI Query 接收的查询参数
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-search.html#search-search-api-query-params
 * <p>
 * Copyright: (C), 2021-04-28 17:30
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticPipelineBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticPipelineBuilder.class);

	/**
	 * pipeline的名字
	 */
	private String pipelineName;

	/**
	 * pipeline内容所在的文件名
	 */
	private String pipelineFileName;

	/**
	 * 直接给出pipeline的内容
	 */
	private String pipelineContent;

	public ElasticPipelineBuilder(String pipelineName) {
		this.pipelineName = pipelineName;
	}

	public ElasticPipelineBuilder pipeFilename(String pipelineFileName) {
		this.pipelineFileName = pipelineFileName;
		return this;
	}

	public ElasticPipelineBuilder pipelineContent(String pipelineContent) {
		this.pipelineContent = pipelineContent;
		return this;
	}

	/**
	 * 创建pipeline, 如果创建失败则抛异常
	 *
	 */
	public void create() {
		log.info("Create Pipeline: {}", pipelineName);

		String content = pipelineContent;
		if (isNotBlank(pipelineFileName)) {
			content = IOUtils.readClassPathFileAsString(pipelineFileName);
		}

		try {
			JacksonJsonpMapper mapper = new JacksonJsonpMapper();
			JsonParser parser = mapper.jsonProvider().createParser(
					new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

			PutPipelineRequest.Builder reqBuilder = new PutPipelineRequest.Builder();
			reqBuilder.withJson(parser, mapper);
			reqBuilder.id(pipelineName);

			PutPipelineResponse response = ElasticUtils.QUERY_CLIENT.ingest().putPipeline(reqBuilder.build());
			if (!response.acknowledged()) {
				throw new CreatePipelineException("Pipeline [" + pipelineName + "] create not acknowledged");
			}
		} catch (CreatePipelineException e) {
			throw e;
		} catch (IOException e) {
			throw new CreatePipelineException("Failed to create pipeline: " + pipelineName, e);
		}
	}

	/**
	 * 查看pipeline
	 */
	public String get() {
		log.info("Get Pipeline: {}", pipelineName);

		try {
			GetPipelineRequest request = GetPipelineRequest.of(b -> b.id(pipelineName));
			GetPipelineResponse response = ElasticUtils.QUERY_CLIENT.ingest().getPipeline(request);
			return response.toString();
		} catch (IOException e) {
			throw new CreatePipelineException("Failed to get pipeline: " + pipelineName, e);
		}
	}

	/**
	 * 删除pipeline
	 */
	public void delete() {
		log.info("Delete Pipeline: {}", pipelineName);

		try {
			DeletePipelineRequest request = DeletePipelineRequest.of(b -> b.id(pipelineName));
			ElasticUtils.QUERY_CLIENT.ingest().deletePipeline(request);
		} catch (IOException e) {
			throw new CreatePipelineException("Failed to delete pipeline: " + pipelineName, e);
		}
	}
}
