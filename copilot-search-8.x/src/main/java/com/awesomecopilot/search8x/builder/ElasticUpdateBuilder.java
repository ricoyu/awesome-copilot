package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.support.UpdateResult;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.support.UpdateResult.Result.VERSION_CONFLICT;

/**
 * ES 8.x 局部更新文档构建器
 * <p>
 * 从 copilot-search 的 ElasticUpdateBuilder 迁移改造而来
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticUpdateBuilder {

    private final String index;

    private String id;

    /**
     * 更新文档的一部分, 文档不存在时创建文档
     */
    private Boolean upsert;

    /**
     * 文档
     */
    private Object doc;

    /**
     * 更新后是否立即刷新? 立即刷新可以马上搜索到, 不立即刷新可能要1s过后
     */
    private Boolean refresh;

    /**
     * 这个文档每次修改, ifSeqNo都会增加1
     */
    private Long ifSeqNo;

    /**
     * 每个主分片的当前期号。如果主分片因故障而重新分配, 期号会增加。
     * 这可以用来识别文档所在的主分片是否在你上次读取后发生了变化。
     */
    private Long ifPrimaryTerm;

    public ElasticUpdateBuilder(String index) {
        Objects.requireNonNull(index, "index cannot be null!");
        this.index = index;
    }

    /**
     * 要更新的文档的ID
     *
     * @param id 文档ID
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder id(Integer id) {
        Objects.requireNonNull(id, "id cannot be null!");
        this.id = id.toString();
        return this;
    }

    /**
     * 要更新的文档的ID
     *
     * @param id 文档ID
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder id(String id) {
        Objects.requireNonNull(id, "id cannot be null!");
        this.id = id;
        return this;
    }

    /**
     * 更新文档的一部分, 文档不存在时创建文档
     *
     * @param upsert 是否在文档不存在时创建
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder upsert(Boolean upsert) {
        this.upsert = upsert;
        return this;
    }

    /**
     * 要更新的内容, 可以是一串JSON字符串, 也可以是对象类型(会自动序列化成JSON)
     *
     * @param doc 更新内容
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder doc(Object doc) {
        Objects.requireNonNull(doc, "doc cannot be null!");
        this.doc = doc;
        return this;
    }

    /**
     * 更新后是否立即刷新? 立即刷新可以马上搜索到, 不立即刷新可能要1s过后
     *
     * @param refresh 是否立即刷新
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder refresh(Boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    /**
     * Elasticsearch 为索引中的每次变更 (包括添加、更新、删除操作) 维护一个全局序列号。
     * 这个序列号是在索引级别上的, 不是针对单个文档的。每当索引中发生更改时, 序列号递增。
     *
     * @param ifSeqNo 乐观并发控制序列号
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder ifSeqNo(Long ifSeqNo) {
        this.ifSeqNo = ifSeqNo;
        return this;
    }

    /**
     * 每个主分片的当前期号。如果主分片因故障而重新分配, 期号会增加。
     * 这可以用来识别文档所在的主分片是否在你上次读取后发生了变化。
     *
     * @param ifPrimaryTerm 乐观并发控制期号
     * @return ElasticUpdateBuilder
     */
    public ElasticUpdateBuilder ifPrimaryTerm(Long ifPrimaryTerm) {
        this.ifPrimaryTerm = ifPrimaryTerm;
        return this;
    }

    /**
     * 执行更新操作
     *
     * @return UpdateResult 更新结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public UpdateResult update() {
        Map<String, Object> docMap;
        if (doc instanceof String) {
            docMap = com.awesomecopilot.json.jackson.JacksonUtils.toMap((String) doc);
        } else if (doc instanceof Map) {
            docMap = (Map<String, Object>) doc;
        } else {
            docMap = com.awesomecopilot.json.jackson.JacksonUtils.toMap(toJson(doc));
        }

        try {
            var request = UpdateRequest.<Map<String, Object>, Map<String, Object>>of(b -> {
                b.index(index).id(id).doc(docMap);
                if (Boolean.TRUE.equals(upsert)) {
                    b.docAsUpsert(true);
                }
                if (ifSeqNo != null) {
                    b.ifSeqNo(ifSeqNo);
                }
                if (ifPrimaryTerm != null) {
                    b.ifPrimaryTerm(ifPrimaryTerm);
                }
                if (Boolean.TRUE.equals(refresh)) {
                    b.refresh(Refresh.True);
                }
                return b;
            });

            UpdateResponse response = ElasticUtils.INDEX_CLIENT.update(request, Map.class);
            return UpdateResult.from(response);
        } catch (ElasticsearchException e) {
            if (e.status() == 409) {
                UpdateResult updateResult = new UpdateResult();
                updateResult.setResult(VERSION_CONFLICT);
                return updateResult;
            }
            throw new RuntimeException("Failed to execute update", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute update", e);
        }
    }
}
