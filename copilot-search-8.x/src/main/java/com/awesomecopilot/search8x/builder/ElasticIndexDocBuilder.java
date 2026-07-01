package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;

/**
 * ES 8.x 文档索引构建器
 * <p>
 * 对应原版 ElasticIndexDocBuilder, 使用 ES 8.x Java Client API 改造
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticIndexDocBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticIndexDocBuilder.class);

    private String index;

    private Object doc;

    private String id;

    private String pipeline;

    /**
     * 是否要立即刷新
     */
    private boolean refresh;

    public ElasticIndexDocBuilder(String index) {
        this.index = index;
    }

    /**
     * 要索引的文档
     *
     * @param doc 文档对象(String/POJO)
     * @return ElasticIndexDocBuilder
     */
    public ElasticIndexDocBuilder doc(Object doc) {
        notNull(doc, "docs cannot be null!");
        this.doc = doc;
        return this;
    }

    public ElasticIndexDocBuilder pipeline(String pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public ElasticIndexDocBuilder id(String id) {
        this.id = id;
        return this;
    }

    public ElasticIndexDocBuilder id(int id) {
        this.id = String.valueOf(id);
        return this;
    }

    /**
     * 插入后是否强制刷新, 这样可以立即查询到新插入的文档
     *
     * @param refresh 是否立即刷新
     * @return ElasticIndexDocBuilder
     */
    public ElasticIndexDocBuilder refresh(boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    /**
     * 执行创建
     *
     * @return docId
     */
    public String execute() {
        Objects.requireNonNull(doc, "doc cannot be null! Call doc() before execute()!");

        try {
            Map<String, Object> document;
            if (doc instanceof String) {
                document = com.awesomecopilot.json.jackson.JacksonUtils.toMap((String) doc);
            } else {
                document = toMap(doc);
            }

            final Map<String, Object> docToIndex = document;
            final String pipelineToUse = this.pipeline;
            final boolean doRefresh = this.refresh;

            IndexRequest<Map<String, Object>> request = IndexRequest.of(b -> {
                b.index(index).id(id).document(docToIndex);
                if (pipelineToUse != null && !pipelineToUse.isBlank()) {
                    b.pipeline(pipelineToUse);
                }
                if (doRefresh) {
                    b.refresh(co.elastic.clients.elasticsearch._types.Refresh.True);
                }
                return b;
            });

            IndexResponse response = ElasticUtils.INDEX_CLIENT.index(request);
            return response.id();
        } catch (IOException e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        String json = com.awesomecopilot.json.jackson.JacksonUtils.toJson(obj);
        return com.awesomecopilot.json.jackson.JacksonUtils.toMap(json);
    }
}
