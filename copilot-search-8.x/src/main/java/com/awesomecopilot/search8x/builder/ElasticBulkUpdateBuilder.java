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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 8.x 批量更新文档构建器
 * <p>
 * 从 copilot-search 模块迁移而来, 使用 ES 8.x Java Client API 改造
 * 使用 {@link BulkRequest} + {@link BulkOperation} 实现批量更新
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBulkUpdateBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticBulkUpdateBuilder.class);

    /**
     * 是否要立即刷新
     */
    private Boolean refresh;

    private final List<BulkOperation> operations = new ArrayList<>();

    public ElasticBulkUpdateBuilder() {
    }

    /**
     * 要批量更新的文档
     *
     * @param index 索引名
     * @param id    文档的_id
     * @param doc   要更新的文档的一部分, 如果doc里面还包含嵌套文档, 也要用Map来表示, 不能直接用一个对象
     * @return ElasticBulkUpdateBuilder
     */
    public ElasticBulkUpdateBuilder doc(String index, String id, Map<String, Object> doc) {
        operations.add(BulkOperation.of(op -> op.update(upd -> upd
                .index(index)
                .id(id)
                .action(a -> a.doc(doc))
        )));
        return this;
    }

    /**
     * 要批量更新的文档
     *
     * @param index 索引名
     * @param id    文档的_id
     * @param doc   要更新的文档的一部分, 这里跟直接传Map类型不同, 这边可变参数是: 字段名, 字段值, 字段名, 字段值 这样成对出现
     * @return ElasticBulkUpdateBuilder
     */
    public ElasticBulkUpdateBuilder doc(String index, String id, Object... doc) {
        Map<String, Object> docMap = new HashMap<>();
        if (doc != null) {
            for (int i = 0; i < doc.length - 1; i += 2) {
                docMap.put(String.valueOf(doc[i]), doc[i + 1]);
            }
        }
        operations.add(BulkOperation.of(op -> op.update(upd -> upd
                .index(index)
                .id(id)
                .action(a -> a.doc(docMap))
        )));
        return this;
    }

    /**
     * 批量更新后是否强制刷新
     *
     * @param refresh 是否立即刷新
     * @return ElasticBulkUpdateBuilder
     */
    public ElasticBulkUpdateBuilder refresh(Boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    /**
     * 执行批量更新
     *
     * @return BulkResult 批量操作结果
     */
    public BulkResult execute() {
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
            throw new RuntimeException("Failed to execute bulk update", e);
        }
    }
}
