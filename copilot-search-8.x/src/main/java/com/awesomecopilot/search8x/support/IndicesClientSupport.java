package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * 基于 Elasticsearch 8.x 新客户端的索引管理支持类
 */
public final class IndicesClientSupport {

    private static final Logger log = LoggerFactory.getLogger(IndicesClientSupport.class);

    private IndicesClientSupport() {
    }

    /**
     * 检查索引是否存在
     *
     * @param client  Elasticsearch 客户端
     * @param indices 索引名称数组
     * @return 如果所有索引都存在则返回 true
     */
    public static boolean existsIndex(ElasticsearchClient client, String... indices) {
        try {
            ExistsRequest request = ExistsRequest.of(builder -> builder.index(Arrays.asList(indices)));
            return client.indices().exists(request).value();
        } catch (IOException e) {
            throw new RuntimeException("检查索引存在性失败: " + Arrays.toString(indices), e);
        }
    }

    /**
     * 删除索引
     *
     * @param client  Elasticsearch 客户端
     * @param indices 要删除的索引名称数组
     * @return 如果删除成功则返回 true
     */
    public static boolean deleteIndex(ElasticsearchClient client, String... indices) {
        try {
            DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder.index(Arrays.asList(indices)));
            DeleteIndexResponse response = client.indices().delete(request);
            return response.acknowledged();
        } catch (IOException e) {
            throw new RuntimeException("删除索引失败: " + Arrays.toString(indices), e);
        }
    }
}
