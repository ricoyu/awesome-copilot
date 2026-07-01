package com.awesomecopilot.search8x.builder;

import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.*;

/**
 * ES 8.x 多文档获取构建器
 * <p>
 * 从 copilot-search 模块迁移而来, 使用 ES 8.x Java Client API 改造
 * 使用 {@link MgetRequest} + {@link MultiGetOperation} 实现多文档获取
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticMultiGetBuilder<T> {

    private static final Logger log = LoggerFactory.getLogger(ElasticMultiGetBuilder.class);

    private final List<MultiGetOperation> operations = new ArrayList<>();

    private Class<T> clazz;

    public ElasticMultiGetBuilder() {
    }

    /**
     * 添加一个要获取的文档
     *
     * @param index 索引名
     * @param id    文档ID
     * @return ElasticMultiGetBuilder
     */
    public ElasticMultiGetBuilder<T> add(String index, String id) {
        operations.add(MultiGetOperation.of(op -> op
                .index(index)
                .id(id)
        ));
        return this;
    }

    /**
     * 批量添加同一索引下的多个文档ID
     *
     * @param index 索引名
     * @param ids   文档ID列表
     * @return ElasticMultiGetBuilder
     */
    public ElasticMultiGetBuilder<T> add(String index, List<String> ids) {
        ids.stream()
                .filter(Objects::nonNull)
                .forEach(id -> operations.add(MultiGetOperation.of(op -> op
                        .index(index)
                        .id(id)
                )));
        return this;
    }

    /**
     * 设置返回结果类型
     *
     * @param clazz 目标类型
     * @param <T1>  类型参数
     * @return ElasticMultiGetBuilder
     */
    @SuppressWarnings("unchecked")
    public <T1> ElasticMultiGetBuilder<T1> resultType(Class<T1> clazz) {
        ElasticMultiGetBuilder<T1> builder = (ElasticMultiGetBuilder<T1>) this;
        builder.clazz = clazz;
        return builder;
    }

    /**
     * 执行多文档获取请求
     *
     * @return 文档列表(如果设置了resultType则返回对应类型对象列表, 否则返回JSON字符串列表)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<T> request() {
        MgetRequest request = MgetRequest.of(b -> b.docs(operations));

        try {
            MgetResponse response = ElasticUtils.QUERY_CLIENT.mget(request, Map.class);
            List<MultiGetResponseItem<Map>> items = (List<MultiGetResponseItem<Map>>) (List<?>) response.docs();

            List<String> resultJsons = items.stream()
                    .filter(item -> item.isResult() && item.result().found())
                    .map(item -> {
                        Map source = item.result().source();
                        return JacksonUtils.toJson(source);
                    })
                    .collect(toList());

            if (clazz != null) {
                return resultJsons.stream()
                        .map(json -> JacksonUtils.toObject(json, clazz))
                        .collect(toList());
            }

            return (List<T>) resultJsons;
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute multi-get request", e);
        }
    }
}
