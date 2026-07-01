package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.support.BulkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;

/**
 * ES 8.x 批量索引文档构建器
 * <p>
 * 从 copilot-search 模块迁移而来, 使用 ES 8.x Java Client API 改造
 * 使用 {@link BulkRequest} + {@link BulkOperation} 实现批量索引
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBulkIndexBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticBulkIndexBuilder.class);

    private String index;

    private List<?> docs;

    /**
     * 是否要立即刷新
     */
    private Boolean refresh;

    public ElasticBulkIndexBuilder(String index) {
        this.index = index;
    }

    /**
     * 要批量插入的文档
     *
     * @param docs JSON字符串数组
     * @return ElasticBulkIndexBuilder
     */
    public ElasticBulkIndexBuilder docs(String... docs) {
        notNull(docs, "docs cannot be null!");
        if (docs.length == 0) {
            throw new IllegalArgumentException("docs cannot be empty!");
        }
        this.docs = Arrays.asList(docs);
        return this;
    }

    /**
     * 要批量插入的文档
     *
     * @param docs 文档列表(支持POJO/Map/String)
     * @return ElasticBulkIndexBuilder
     */
    @SuppressWarnings("rawtypes")
    public ElasticBulkIndexBuilder docs(List docs) {
        notNull(docs, "docs cannot be null!");
        if (docs.isEmpty()) {
            throw new IllegalArgumentException("docs cannot be empty!");
        }
        this.docs = docs;
        return this;
    }

    /**
     * 批量插入后是否强制刷新, 这样可以立即查询到新插入的文档
     *
     * @param refresh 是否立即刷新
     * @return ElasticBulkIndexBuilder
     */
    public ElasticBulkIndexBuilder refresh(Boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    /**
     * 执行批量索引
     *
     * @return BulkResult 批量操作结果
     */
    public BulkResult execute() {
        List<BulkOperation> operations = new ArrayList<>();

        docs.stream()
                .filter(Objects::nonNull)
                .forEach(doc -> {
                    String id = ElasticUtils.extractIdValue(doc);
                    Map<String, Object> document = toMap(doc);

                    operations.add(BulkOperation.of(op -> {
                        op.index(idx -> {
                            idx.index(index).document(document);
                            if (id != null && !id.isEmpty()) {
                                idx.id(id);
                            }
                            return idx;
                        });
                        return op;
                    }));
                });

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder().operations(operations);
        if (Boolean.TRUE.equals(refresh)) {
            bulkBuilder.refresh(Refresh.True);
        }

        try {
            BulkResponse response = ElasticUtils.INDEX_CLIENT.bulk(bulkBuilder.build());
            BulkResult bulkResult = new BulkResult();

            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    bulkResult.fail();
                    bulkResult.addFailMessage(item.error().reason());
                } else {
                    bulkResult.success();
                    bulkResult.addId(item.id());
                }
            }

            return bulkResult;
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute bulk index", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        if (obj instanceof String) {
            return com.awesomecopilot.json.jackson.JacksonUtils.toMap((String) obj);
        }
        String json = toJson(obj);
        return com.awesomecopilot.json.jackson.JacksonUtils.toMap(json);
    }
}
