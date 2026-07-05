package com.awesomecopilot.search8x;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch.cluster.GetClusterSettingsResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.DeleteScriptRequest;
import co.elastic.clients.elasticsearch.core.DeleteScriptResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.PutScriptRequest;
import co.elastic.clients.elasticsearch.core.PutScriptResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexTemplate;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplateItem;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.awesomecopilot.common.lang.utils.EnumUtils;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.search8x.constants.ElasticConstants;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.builder.ElasticBulkIndexBuilder;
import com.awesomecopilot.search8x.builder.ElasticBulkUpdateBuilder;
import com.awesomecopilot.search8x.builder.ElasticContextSuggestBuilder;
import com.awesomecopilot.search8x.builder.ElasticIndexDocBuilder;
import com.awesomecopilot.search8x.builder.ElasticMultiGetBuilder;
import com.awesomecopilot.search8x.builder.ElasticSuggestBuilder;
import com.awesomecopilot.search8x.builder.ElasticUpdateBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticAvgAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticCardinalityAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticCompositeAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticDateHistogramAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticHistogramAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticMaxAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticMinAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticMultiTermsAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticRangeAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticStatsAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticSumAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.ElasticTermsAggregationBuilder;
import com.awesomecopilot.search8x.builder.admin.ClusterSettingBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticIndexBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticIndexTemplateBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticPipelineBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticPutMappingBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticReindexBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticUpdateSettingBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticBoolQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticExistsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticGeoDistanceQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticIdsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchAllQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchPhrasePrefixQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchPhraseQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMultiMatchQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticPrefixQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticQueryStringBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticRangeQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticScrollQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTemplateQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTermQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTermsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticUriQueryBuilder;
import com.awesomecopilot.search8x.enums.Analyzer;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.enums.IndexState;
import com.awesomecopilot.search8x.enums.SuggestMode;
import com.awesomecopilot.search8x.exception.IndexTemplateException;
import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import com.awesomecopilot.search8x.support.BulkResult;
import com.awesomecopilot.search8x.support.IndexSupport;
import com.awesomecopilot.search8x.support.MappingSupport;
import com.awesomecopilot.search8x.support.SettingsSupport;
import com.awesomecopilot.search8x.support.UpdateResult;
import com.awesomecopilot.search8x.vo.Index;
import com.awesomecopilot.search8x.vo.VersionedDoc;
import jakarta.json.stream.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.json.jackson.JacksonUtils.toObject;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Elasticsearch 8.x 工具类, 使用 ES 8.x Java Client API
 * <p>
 * Copyright: (C), 2021-01-01 8:37
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticUtils.class);
    
    /**
     * 唯一的一个type是_doc (ES 7.x+ 已废弃 type 概念)
     */
    public static final String ONLY_TYPE = "_doc";

    /**
     * ES 8.x Java Client - 用于查询操作
     */
    public static final ElasticsearchClient QUERY_CLIENT = ElasticsearchClientFactory.createQueryClient();

    /**
     * ES 8.x Java Client - 用于写入操作  
     */
    public static final ElasticsearchClient INDEX_CLIENT = QUERY_CLIENT;
    
    /**
     * 初始化客户端连接
     */
    public static void ping() {
        try {
            Admin.existsIndex("ricoyu");
        } catch (Exception e) {
            log.debug("Ping ES cluster", e);
        }
    }
    
    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式, 使用Builder模式
     *
     * @param index 索引名
     * @return ElasticIndexDocBuilder
     */
    public static ElasticIndexDocBuilder index(String index) {
        return new ElasticIndexDocBuilder(index);
    }
    
    // ==================== 文档 CRUD 操作 ====================
    
    /**
     * 创建一个新的文档, 返回新创建文档的ID
     *
     * @param index 索引名
     * @param doc   要保存的文档, 会自动通过Jackson序列化成JSON串
     * @return String 文档ID
     */
    public static String index(String index, Object doc) {
        if (doc == null) {
            return null;
        }
        
        String id = extractId(doc);
        return index(index, doc, id);
    }



    /**
     * 创建一个新的文档, 返回新创建文档的ID
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @param id    文档ID
     * @return String 文档ID
     */
    public static String index(String index, Object doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(builder -> builder
                    .index(index)
                    .id(id)
                    .document(toMap(doc))
            );
            
            IndexResponse response = INDEX_CLIENT.index(request);
            return response.id();
        } catch (IOException e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }
    
    /**
     * 创建一个新的文档, 返回新创建文档的ID, 相同ID的文档如果已存在, Elasticsearch底层会先删掉该文档, 然后重新创建一个文档, 版本号+1
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String index(String index, Object doc, int id) {
        return index(index, doc, String.valueOf(id));
    }
    
    // ==================== 文档计数 ====================
    
    /**
     * 返回索引的文档数量
     * <p>
     * 使用ES 8.x Count API, 比 SearchRequest + size(0) 更高效
     *
     * @param index 索引名
     * @return 索引的文档数量
     */
    public static long docCount(String index) {
        Objects.requireNonNull(index, "index cannot be null!");
        
        try {
            CountRequest request = CountRequest.of(b -> b
                    .index(index)
                    .query(q -> q.matchAll(m -> m))
            );
            
            CountResponse response = QUERY_CLIENT.count(request);
            long count = response.count();
            log.info("索引 {} 中的文档总数: {}", index, count);
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count documents", e);
        }
    }
    
    /**
     * 创建一个新的文档, 返回新创建文档的ID, 使用/_create`端点是用来明确创建一个新的文档, 而不是更新它。如果指定的文档ID已经存在, 则会返回一个错误。
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String create(String index, Object doc, int id) {
        return create(index, doc, String.valueOf(id));
    }
    
    /**
     * 创建一个新的文档, 如果ID已存在则报错
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @param id    文档ID
     * @return String 文档ID
     */
    public static String create(String index, Object doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(builder -> builder
                    .index(index)
                    .id(id)
                    .document(toMap(doc))
                    .opType(co.elastic.clients.elasticsearch._types.OpType.Create)
            );
            
            IndexResponse response = INDEX_CLIENT.index(request);
            return response.id();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create document", e);
        }
    }
    
    /**
     * 批量创建文档<p>
     * 返回创建结果, 包括成功数量, 失败数量, 失败消息, 成功创建的文档id列表<p>
     * 单个bulk请求体的数据量不要太大, 官方建议大于5~15mb
     *
     * @param index 索引名
     * @return ElasticBulkIndexBuilder
     */
    public static ElasticBulkIndexBuilder bulkIndex(String index) {
        return new ElasticBulkIndexBuilder(index);
    }

    /**
     * 批量创建文档<p>
     * 返回创建结果, 包括成功数量, 失败数量, 失败消息, 成功创建的文档id列表<p>
     * 单个bulk请求体的数据量不要太大, 官方建议大于5~15mb
     *
     * @param index 索引名
     * @param docs  JSON字符串数组
     * @return BulkResult
     */
    public static BulkResult bulkIndex(String index, String... docs) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (docs == null || docs.length == 0) {
            return new BulkResult();
        }

        List<BulkOperation> operations = asList(docs).stream()
                .filter(Objects::nonNull)
                .map(doc -> BulkOperation.of(op -> op.index(idx -> idx
                        .index(index)
                        .document(JacksonUtils.toMap(doc))
                )))
                .collect(Collectors.toList());

        try {
            BulkResponse response = INDEX_CLIENT.bulk(BulkRequest.of(b -> b.operations(operations)));
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

    /**
     * 批量创建文档<p>
     * 返回创建结果, 包括成功数量, 失败数量, 失败消息, 成功创建的文档id列表<p>
     *
     * @param index 索引名
     * @param docs  文档列表(支持POJO/Map/JSON字符串)
     * @return BulkResult
     */
    public static BulkResult bulkIndex(String index, List<?> docs) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (docs == null || docs.isEmpty()) {
            return new BulkResult();
        }

        List<BulkOperation> operations = docs.stream()
                .filter(Objects::nonNull)
                .map(doc -> {
                    String id = extractIdValue(doc);
                    Map<String, Object> document = toMap(doc);
                    return BulkOperation.of(op -> op.index(idx -> {
                        idx.index(index).document(document);
                        if (id != null && !id.isEmpty()) {
                            idx.id(id);
                        }
                        return idx;
                    }));
                })
                .collect(Collectors.toList());

        try {
            BulkResponse response = INDEX_CLIENT.bulk(BulkRequest.of(b -> b.operations(operations)));
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

    /**
     * 基于分批BulkRequest批量创建文档, 模拟原ES 7.x BulkProcessor的多线程批量行为<p>
     * 将文档列表按每批1000条拆分为多个BulkRequest依次发送
     *
     * @param index 索引名
     * @param docs  文档列表(支持POJO/Map/String)
     */
    public static void bulkIndexConcurrent(String index, List<?> docs) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (docs == null || docs.isEmpty()) {
            return;
        }

        int batchSize = 1000;
        List<BulkOperation> batch = new ArrayList<>(batchSize);

        for (Object doc : docs) {
            if (doc == null) {
                continue;
            }
            Map<String, Object> document = toMap(doc);
            batch.add(BulkOperation.of(op -> op.index(idx -> idx
                    .index(index)
                    .document(document)
            )));

            if (batch.size() >= batchSize) {
                try {
                    INDEX_CLIENT.bulk(BulkRequest.of(b -> b.operations(batch)));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to execute bulk index", e);
                }
                batch.clear();
            }
        }

        // 发送剩余文档
        if (!batch.isEmpty()) {
            try {
                INDEX_CLIENT.bulk(BulkRequest.of(b -> b.operations(batch)));
            } catch (IOException e) {
                throw new RuntimeException("Failed to execute bulk index", e);
            }
        }
    }

    /**
     * 批量更新
     *
     * @return ElasticBulkUpdateBuilder
     */
    public static ElasticBulkUpdateBuilder bulkUpdate() {
        return new ElasticBulkUpdateBuilder();
    }
    
    /**
     * 根据ID获取文档
     *
     * @param index 索引名
     * @param id    文档id
     * @return T
     */
    public static String get(String index, int id) {
        return get(index, String.valueOf(id));
    }
    
    /**
     * 根据ID获取文档
     *
     * @param index 索引名
     * @param id    文档id
     * @return JSON字符串
     */
    public static String get(String index, String id) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");

        try {
            GetRequest request = GetRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );
            
            GetResponse<Map<String, Object>> response = QUERY_CLIENT.get(request, (Class<Map<String, Object>>)(Class<?>)Map.class);
            
            if (response.found()) {
                return toJson(response.source());
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get document", e);
        }
    }

    /**
     * 根据ID获取并转成指定类型对象
     *
     * @param index 索引名
     * @param id    文档id
     * @param clazz 目标类型
     * @return T
     */
    public static <T> T get(String index, String id, Class<T> clazz) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");
        Objects.requireNonNull(clazz, "clazz不能为null");

        try {
            GetRequest request = GetRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );
            
            GetResponse<Map<String, Object>> response = QUERY_CLIENT.get(request, (Class<Map<String, Object>>)(Class<?>)Map.class);
            
            if (response.found()) {
                return toObject(toJson(response.source()), clazz);
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get document", e);
        }
    }
    
    /**
     * 根据ID获取文档, 等价于getWithVersion
     *
     * @param index 索引名
     * @param id    文档id
     * @return T
     */
    public static VersionedDoc<String> getWithVersion(String index, int id) {
        return getWithVersion(index, String.valueOf(id));
    }
    
    /**
     * 根据ID获取文档, 包含版本信息
     *
     * @param index 索引名
     * @param id    文档id
     * @return VersionedDoc
     */
    public static VersionedDoc<String> getWithVersion(String index, String id) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");

        try {
            GetRequest request = GetRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );
            
            GetResponse<Map<String, Object>> response = QUERY_CLIENT.get(request, (Class<Map<String, Object>>)(Class<?>)Map.class);
            
            if (response.found()) {
                String source = toJson(response.source());
                return VersionedDoc.<String>builder()
                        .source(source)
                        .id(id)
                        .version(response.version())
                        .ifSeqNo(response.seqNo())
                        .ifPrimaryTerm(response.primaryTerm())
                        .build();
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get document with version", e);
        }
    }

    /**
     * 根据ID获取并转成指定类型对象, 包含版本信息
     *
     * @param index 索引名
     * @param id    文档id
     * @param clazz 目标类型
     * @param <T>
     * @return VersionedDoc<T>
     */
    public static <T> VersionedDoc<T> getWithVersion(String index, String id, Class<T> clazz) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");
        Objects.requireNonNull(clazz, "clazz不能为null");

        try {
            GetRequest request = GetRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );

            GetResponse<Map<String, Object>> response = QUERY_CLIENT.get(request, (Class<Map<String, Object>>)(Class<?>)Map.class);

            if (response.found()) {
                String source = toJson(response.source());
                T result = toObject(source, clazz);
                return VersionedDoc.<T>builder()
                        .ifSeqNo(response.seqNo())
                        .ifPrimaryTerm(response.primaryTerm())
                        .source(result)
                        .build();
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get document with version", e);
        }
    }

    /**
     * 从一个或者多个索引中根据id获取多文档
     *
     * @return ElasticMultiGetBuilder
     */
    public static ElasticMultiGetBuilder<Object> mget() {
        return new ElasticMultiGetBuilder<>();
    }
    
    /**
     * 删除一篇文档
     *
     * @param index 索引名
     * @param id 文档ID
     * @return Result
     */
    public static boolean delete(String index, Integer id) {
        if (id == null) {
            log.warn("id不能为null");
            return false;
        }
        return delete(index, String.valueOf(id));
    }
    
    /**
     * 删除一篇文档
     *
     * @param index 索引名
     * @param id    文档ID
     * @return 是否删除成功
     */
    public static boolean delete(String index, String id) {
        try {
            DeleteRequest request = DeleteRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );
            
            DeleteResponse response = INDEX_CLIENT.delete(request);
            return response.result() == co.elastic.clients.elasticsearch._types.Result.Deleted;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete document", e);
        }
    }

    /**
     * 根据单个条件删除, 返回删除的记录数
     * 条件匹配是按照精确匹配来处理的, 为了防止误删
     *
     * @param index 索引名
     * @param field 字段名
     * @param value 字段值
     * @return 删除的记录数
     */
    public static long deleteBy(String index, String field, String value) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(builder -> builder
                    .index(index)
                    .query(q -> q.term(t -> t.field(field).value(value)))
            );
            
            DeleteByQueryResponse response = INDEX_CLIENT.deleteByQuery(request);
            return response.deleted() != null ? response.deleted() : 0L;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete by query", e);
        }
    }

    /**
     * 检查指定索引中是否存在指定id的文档
     *
     * @param index 索引名
     * @param id    文档ID
     * @return boolean
     */
    public static boolean exists(String index, String id) {
        try {
            ExistsRequest request = ExistsRequest.of(builder -> builder
                    .index(index)
                    .id(id)
            );
            
            return INDEX_CLIENT.exists(request).value();
        } catch (IOException e) {
            throw new RuntimeException("Failed to check document existence", e);
        }
    }

    /**
     * 更新文档的一部分
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么报错
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     *
     * @param index 索引名
     * @return ElasticUpdateBuilder
     */
    public static ElasticUpdateBuilder update(String index) {
        return new ElasticUpdateBuilder(index);
    }

    /**
     * 更新文档的一部分
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么报错
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     *
     * @param index 索引名
     * @param id    文档ID
     * @param doc   整篇文档或者文档的一部分(支持JSON字符串或Map)
     * @return UpdateResult 更新结果(更新了? 没更新?)
     */
    public static UpdateResult update(String index, String id, Object doc) {
        Objects.requireNonNull(index, "index cannot be null!");
        Objects.requireNonNull(id, "id cannot be null!");
        if (doc == null) {
            return null;
        }
        
        try {
            UpdateRequest<Map<String, Object>, Map<String, Object>> request = UpdateRequest.of(builder -> builder
                    .index(index)
                    .id(id)
                    .doc(toMap(doc))
            );
            
            var response = INDEX_CLIENT.update(request, Map.class);
            return UpdateResult.from(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update document", e);
        }
    }

    /**
     * 更新文档的一部分
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么报错
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.6/java-rest-high-document-update.html
     *
     * @param index
     * @param id
     * @param doc   整篇文档或者文档的一部分
     * @return Result 更新结果(更新了? 没更新?)
     */
    public static UpdateResult update(String index, String id, Map<String, Object> doc) {
        Objects.requireNonNull(index, "index cannot be null!");
        Objects.requireNonNull(id, "id cannot be null!");
        if (doc == null) {
            return null;
        }

        try {
            UpdateRequest<Map<String, Object>, Map<String, Object>> request = UpdateRequest.of(builder -> builder
                    .index(index)
                    .id(id)
                    .doc(doc)
                    .refresh(Refresh.True)
            );

            var response = INDEX_CLIENT.update(request, Map.class);
            return UpdateResult.from(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update document", e);
        }
    }

    /**
     * 更新文档的一部分
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么报错
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.6/java-rest-high-document-update.html
     *
     * @param index 索引名
     * @param id Integer 文档的id
     * @param doc   整篇文档或者文档的一部分
     * @return Result 更新结果(更新了? 没更新?)
     */
    public static UpdateResult update(String index, Integer id, Map<String, Object> doc) {
        if (id == null) {
            return null;
        }
        return update(index, id.toString(), doc);
    }

    /**
     * 更新文档, 如果不存在则创建(Upsert)
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么创建文档
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     *
     * @param index 索引名
     * @param id    文档ID
     * @param doc   整篇文档或者文档的一部分
     * @return UpdateResult 创建? 更新? 没更新?
     */
    public static UpdateResult upsert(String index, String id, Object doc) {
        Objects.requireNonNull(index, "index cannot be null!");
        Objects.requireNonNull(id, "id cannot be null!");
        if (doc == null) {
            return null;
        }
        
        try {
            UpdateRequest<Map<String, Object>, Map<String, Object>> request = UpdateRequest.of(builder -> builder
                    .index(index)
                    .id(id)
                    .doc(toMap(doc))
                    .docAsUpsert(true)
            );
            
            var response = INDEX_CLIENT.update(request, Map.class);
            return UpdateResult.from(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upsert document", e);
        }
    }
    
    /**
     * https://www.programcreek.com/java-api-examples/?api=org.elasticsearch.search.suggest.term.TermSuggestionBuilder
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-search.html#_requesting_suggestions
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html#context-suggester
     *
     * @param indices
     */
    public static ElasticSuggestBuilder suggest(String... indices) {
        return new ElasticSuggestBuilder(indices);
    }

    /**
     * Term Suggest, suggest mode 是POPULAR
     *
     * @param text
     * @param field
     * @param indices
     * @return Set<String>
     */
    public static Set<String> termSuggest(String text, String field, String... indices) {
        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .field(field)
                .text(text)
                .suggestMode(SuggestMode.POPULAR)
                .suggest();
    }

    /**
     * Phrase Suggestion
     *
     * @param text
     * @param field
     * @param indices
     * @return Set<String>
     */
    public static Set<String> phraseSuggest(String text, String field, String... indices) {
        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .field(field)
                .text(text)
                .maxErrors(2f)
                .confidence(0)
                .highlight("<em>", "</em>")
                .suggest();
    }

    /**
     * Completion Suggestion 只支持前缀匹配<p/>
     * 对索引的Mapping有要求, 自动完成的字段, 其类型必须是completion
     *
     * @param prefix
     * @param field
     * @param indices
     * @return Set<String>
     */
    public static Set<String> completionSuggest(String prefix, String field, String... indices) {
        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .field(field)
                .prefix(prefix)
                .suggest();
    }

    /**
     * 基于上下文的自动完成
     *
     * @param indices
     */
    public static ElasticContextSuggestBuilder contextSuggest(String... indices) {
        return new ElasticContextSuggestBuilder(indices);
    }
    
    /**
     * 用指定分词器分析文本
     *
     * @param analyzer
     * @param texts
     * @return List<String> 分析后的文本
     */
    public static List<String> analyze(Analyzer analyzer, String... texts) {
        if (texts == null || texts.length == 0) {
            return Collections.emptyList();
        }
        
        if (analyzer == null) {
            throw new IllegalArgumentException("analyzer cannot be null");
        }
        
        try {
            AnalyzeResponse response = QUERY_CLIENT.indices().analyze(r -> r
                    .analyzer(analyzer.toString())
                    .text(Arrays.asList(texts))
            );
            
            return response.tokens()
                    .stream()
                    .map(AnalyzeToken::token)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze text with analyzer: " + analyzer, e);
        }
    }
    
    /**
     * 在更新索引的mapping后, 在原索引上重建索引<p>
     * https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/java-docs-update-by-query.html
     *
     * @param indices
     * @return UpdateByQueryResponse
     */
    public static UpdateByQueryResponse updateByQuery(String... indices) {
        try {
            UpdateByQueryRequest request = UpdateByQueryRequest.of(builder -> builder
                    .index(asList(indices))
                    .conflicts(Conflicts.Proceed)
            );
            UpdateByQueryResponse response = QUERY_CLIENT.updateByQuery(request);
            log.info("UpdateByQuery took: {}ms, total: {}, updated: {}, versionConflicts: {}",
                    response.took(), response.total(), response.updated(), response.versionConflicts());
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute updateByQuery", e);
        }
    }
    
    // ==================== 内部工具方法 ====================
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        String json = toJson(obj);
        return JacksonUtils.toMap(json);
    }

    /**
     * 从文档对象中提取ID值
     * <p>
     * 当前实现返回随机UUID, 后续需要从注解提取ID
     *
     * @param doc 文档对象
     * @return 文档ID
     */
    public static String extractIdValue(Object doc) {
        // TODO: 需要从注解提取ID
        return UUID.randomUUID().toString();
    }
    
    private static String extractId(Object doc) {
        return extractIdValue(doc);
    }

    // ==================== Admin 内部类 ====================
    
    /**
     * Elasticsearch 索引管理相关API
     */
    public static class Admin {

        /**
         * 判断索引存在与否
         *
         * @param indices 索引名数组
         * @return boolean
         */
        public static boolean existsIndex(String... indices) {
            try {
                co.elastic.clients.elasticsearch.indices.ExistsRequest request = co.elastic.clients.elasticsearch.indices.ExistsRequest.of(builder -> builder
                        .index(asList(indices))
                );
                return QUERY_CLIENT.indices().exists(request).value();
            } catch (IOException e) {
                throw new RuntimeException("Failed to check index existence", e);
            }
        }

        /**
         * 删除索引
         *
         * @param indices 索引名数组
         * @return boolean 删除成功与否
         */
        public static boolean deleteIndex(String... indices) {
            if (!existsIndex(indices)) {
                log.info("索引{}不存在", Arrays.toString(indices));
                return false;
            }
            try {
                DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder
                        .index(asList(indices))
                );
                return QUERY_CLIENT.indices().delete(request).acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete index", e);
            }
        }

        /**
         * 列出所有索引名称
         *
         * @return List<String>
         */
        public static List<String> listIndexNames() {
            try {
                var response = QUERY_CLIENT.cat().indices(builder -> builder);
                return response.valueBody().stream()
                        .map(record -> record.index())
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Failed to list indices", e);
            }
        }

        /**
         * 列出所有索引, 包含索引名, 主分片数, 副本数, uuid
         * @return List<Index>
         */
        public static List<Index> listIndices() {
            try {
                // 通过get API获取索引元数据(名称, uuid, 主分片数, 副本数)
                var indexResponse = QUERY_CLIENT.indices().get(b -> b.index("*"));
                // 通过cat API获取索引状态(open/close)
                var catResponse = QUERY_CLIENT.cat().indices(c -> c);
                Map<String, String> indexStatusMap = new HashMap<>();
                for (var record : catResponse.valueBody()) {
                    indexStatusMap.put(record.index(), record.status());
                }

                List<Index> indexList = new ArrayList<>();
                indexResponse.result().forEach((indexName, indexState) -> {
                    Index index = new Index();
                    index.setName(indexName);
                    var settings = indexState.settings();
                    if (settings != null) {
                        index.setUuid(settings.uuid());
                        if (settings.numberOfShards() != null) {
                            index.setNumberOfShards(Integer.parseInt(settings.numberOfShards()));
                        }
                        if (settings.numberOfReplicas() != null) {
                            index.setNumberOfReplicas(Integer.parseInt(settings.numberOfReplicas()));
                        }
                    }
                    String status = indexStatusMap.get(indexName);
                    if (status != null) {
                        index.setState(EnumUtils.lookupEnum(IndexState.class, status));
                    }
                    indexList.add(index);
                });
                return indexList;
            } catch (IOException e) {
                throw new RuntimeException("Failed to list indices", e);
            }
        }

        /**
         * 基于Entity上的注解信息创建索引
         *
         * @param entityClass 标注了@Index注解的POJO
         * @return boolean 索引是否创建成功
         */
        public static boolean createIndex(Class entityClass) {
            return createIndex(entityClass, null);
        }

        /**
         * 基于Entity上的注解信息创建索引
         *
         * @param entityClass 标注了@Index注解的POJO
         * @param index       显式指定的索引名
         * @return boolean 索引是否创建成功
         */
        public static boolean createIndex(Class entityClass, String index) {
            notNull(entityClass, "entityClass cannot be null!");
            //抽取索引的Mapping信息
            Map<String, Object> mappingMap = MappingSupport.extractIndexMapping(entityClass);
            //抽取索引的Setting信息
            Map<String, Object> settingsMap = SettingsSupport.extractIndexSettings(entityClass);
            //抽取索引名
            if (isBlank(index)) {
                index = IndexSupport.indexName(entityClass);
            }
            final String indexName = index;

            try {
                TypeMapping typeMapping = buildTypeMapping(mappingMap);
                IndexSettings indexSettings = buildIndexSettings(settingsMap);

                CreateIndexRequest request = CreateIndexRequest.of(b -> b
                        .index(indexName)
                        .mappings(typeMapping)
                        .settings(indexSettings)
                );
                CreateIndexResponse response = QUERY_CLIENT.indices().create(request);
                boolean created = response.acknowledged();
                log.info("Index {} {}", indexName, (created ? "created" : "not created"));
                return created;
            } catch (IOException e) {
                throw new RuntimeException("Failed to create index: " + indexName, e);
            }
        }

        /**
         * 创建索引, 默认1个分片, 0个副本
         *
         * @param index 索引名
         * @return boolean 创建成功失败标识
         */
        public static ElasticIndexBuilder createIndex(String index) {
            return new ElasticIndexBuilder(index);
        }

        /**
         * 为Index创建别名
         *
         * @param index
         * @param alias
         * @return 创建成功与否
         */
        public static boolean createIndexAlias(String index, String alias) {
            try {
                var response = QUERY_CLIENT.indices().updateAliases(b -> b
                        .actions(a -> a
                                .add(add -> add
                                        .index(index)
                                        .alias(alias)
                                )
                        )
                );
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create index alias", e);
            }
        }

        /**
         * 为Index创建别名
         *
         * @param indices
         * @param alias
         * @param queryBuilder
         * @return
         */
        public static boolean createIndexAlias(String[] indices, String alias, co.elastic.clients.elasticsearch._types.query_dsl.Query queryBuilder) {
            try {
                var response = QUERY_CLIENT.indices().updateAliases(b -> {
                    for (String idx : indices) {
                        b.actions(a -> a
                                .add(add -> add
                                        .index(idx)
                                        .alias(alias)
                                        .filter(queryBuilder)
                                )
                        );
                    }
                    return b;
                });
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create index alias with filter", e);
            }
        }

        /**
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/indices-templates.html
         *
         * @param templateName
         */
        public static ElasticIndexTemplateBuilder putIndexTemplateByFile(String templateName) {
            return ElasticIndexTemplateBuilder.newInstance(templateName);
        }

        /**
         * 从classpath读取指定的Index Template文件, 然后创建Index Template
         *
         * @param templateName
         * @param templateFileName
         * @return boolean
         */
        public static boolean putIndexTemplateByFile(String templateName, String templateFileName) {
            return putIndexTemplate(templateName, IOUtils.readClassPathFileAsString(templateFileName));
        }

        /**
         * 从classpath读取指定的Index Template文件, 然后创建Index Template
         *
         * @param templateName
         * @param templateContent
         * @return boolean
         */
        @SuppressWarnings("unchecked")
        public static boolean putIndexTemplate(String templateName, String templateContent) {
            try {
                Map<String, Object> template = toObject(templateContent, Map.class);

                PutIndexTemplateRequest.Builder reqBuilder = new PutIndexTemplateRequest.Builder();
                reqBuilder.name(templateName);

                // index_patterns
                List<String> indexPatterns = (List<String>) template.get("index_patterns");
                if (indexPatterns != null) {
                    reqBuilder.indexPatterns(indexPatterns);
                }

                // priority (composable template) or order (legacy)
                Object priority = template.get("priority");
                if (priority == null) {
                    priority = template.get("order");
                }
                if (priority instanceof Number) {
                    reqBuilder.priority(((Number) priority).longValue());
                }

                // version
                Object version = template.get("version");
                if (version instanceof Number) {
                    reqBuilder.version(((Number) version).longValue());
                }

                // template (composable format) or top-level settings/mappings (legacy format)
                Map<String, Object> templateBody = (Map<String, Object>) template.get("template");
                if (templateBody == null) {
                    templateBody = template;
                }

                IndexTemplateMapping.Builder templateMappingBuilder = new IndexTemplateMapping.Builder();
                boolean hasTemplateContent = false;

                Map<String, Object> settingsMap = (Map<String, Object>) templateBody.get("settings");
                if (settingsMap != null) {
                    templateMappingBuilder.settings(buildIndexSettings(settingsMap));
                    hasTemplateContent = true;
                }

                Map<String, Object> mappingsMap = (Map<String, Object>) templateBody.get("mappings");
                if (mappingsMap != null) {
                    templateMappingBuilder.mappings(buildTypeMapping(mappingsMap));
                    hasTemplateContent = true;
                }

                if (hasTemplateContent) {
                    reqBuilder.template(templateMappingBuilder.build());
                }

                PutIndexTemplateResponse response = QUERY_CLIENT.indices().putIndexTemplate(reqBuilder.build());
                boolean acknowledged = response.acknowledged();
                log.info("acknowledged: {}", acknowledged);
                return acknowledged;
            } catch (Exception e) {
                log.error("PUT index template failed, templateName: [{}]", templateName, e);
                throw new IndexTemplateException(e.getMessage());
            }
        }

        /**
         * 获取指定的Index Template
         *
         * @param templateName
         * @return IndexTemplateMetaData
         */
        public static Map<String, IndexTemplate> getIndexTemplate(String templateName) {
            try {
                GetIndexTemplateRequest request = GetIndexTemplateRequest.of(b -> b.name(templateName));
                GetIndexTemplateResponse response = QUERY_CLIENT.indices().getIndexTemplate(request);
                return response.indexTemplates().stream()
                        .collect(Collectors.toMap(IndexTemplateItem::name, IndexTemplateItem::indexTemplate));
            } catch (IOException e) {
                throw new RuntimeException("Failed to get index template: " + templateName, e);
            }
        }

        /**
         * 删除Index Template
         *
         * @param templateName
         * @return boolean
         */
        public static boolean deleteIndexTemplate(String templateName) {
            try {
                DeleteIndexTemplateRequest request = DeleteIndexTemplateRequest.of(b -> b.name(templateName));
                DeleteIndexTemplateResponse response = QUERY_CLIENT.indices().deleteIndexTemplate(request);
                return response.acknowledged();
            } catch (Exception e) {
                log.info("Index Template [{}] 不存在", templateName);
                return false;
            }
        }

        /**
         * 创建 Search Template
         *
         * @param templateName
         * @return boolean
         */
        public static boolean createSearchTemplate(String templateName, String templateFileName) {
            try {
                String templateContent = IOUtils.readClassPathFileAsString(templateFileName);
                PutScriptRequest request = PutScriptRequest.of(b -> b
                        .id(templateName)
                        .script(s -> s.lang("mustache").source(templateContent))
                );
                PutScriptResponse response = QUERY_CLIENT.putScript(request);
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create search template: " + templateName, e);
            }
        }

        /**
         * 删除 Search Template
         *
         * @param templateName
         * @return boolean
         */
        public static boolean deleteSearchTemplate(String templateName) {
            try {
                DeleteScriptRequest request = DeleteScriptRequest.of(b -> b.id(templateName));
                DeleteScriptResponse response = QUERY_CLIENT.deleteScript(request);
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete search template: " + templateName, e);
            }
        }

        /**
         * 将索引设为只读, 不再写入的索引设为只读后, 可以提升索引的读性能
         *
         * @param indices
         * @return boolean
         */
        public static boolean setReadOnly(String... indices) {
            try {
                PutIndicesSettingsRequest request = PutIndicesSettingsRequest.of(b -> b
                        .index(Arrays.asList(indices))
                        .settings(s -> s.blocks(bl -> bl.readOnly(true)))
                );
                PutIndicesSettingsResponse response = QUERY_CLIENT.indices().putSettings(request);
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to set read only on indices: " + Arrays.toString(indices), e);
            }
        }

        /**
         * 执行段合并, 可以先设为只读, 然后进行段合并
         *
         * @param indices
         * @return ForcemergeResponse
         */
        public static ForcemergeResponse forceMerge(String indices) {
            try {
                ForcemergeRequest request = ForcemergeRequest.of(b -> b
                        .index(indices)
                        .maxNumSegments(1L)
                );
                return QUERY_CLIENT.indices().forcemerge(request);
            } catch (IOException e) {
                throw new RuntimeException("Failed to force merge index: " + indices, e);
            }
        }

        /**
         * 创建pipeline
         *
         * @param pipelineName
         * @return ElasticPipelineBuilder
         */
        public static ElasticPipelineBuilder pipeline(String pipelineName) {
            ElasticPipelineBuilder elasticPipelineBuilder = new ElasticPipelineBuilder(pipelineName);
            return elasticPipelineBuilder;
        }

        /**
         * 重建索引<p>
         * https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/java-docs-reindex.html
         * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html
         *
         * @param srcIndex
         * @param destIndex
         * @return ElasticReindexBuilder
         */
        public static ElasticReindexBuilder reindex(String srcIndex, String destIndex) {
            return new ElasticReindexBuilder(srcIndex, destIndex);
        }

        private static IndexSettings buildIndexSettings(Map<String, Object> settingsMap) {
            return IndexSettings.of(b -> {
                if (settingsMap != null) {
                    if (settingsMap.containsKey("number_of_shards")) {
                        b.numberOfShards(String.valueOf(settingsMap.get("number_of_shards")));
                    }
                    if (settingsMap.containsKey("number_of_replicas")) {
                        b.numberOfReplicas(String.valueOf(settingsMap.get("number_of_replicas")));
                    }
                    if (settingsMap.containsKey("index.default_pipeline")) {
                        b.defaultPipeline(settingsMap.get("index.default_pipeline").toString());
                    }
                    if (Boolean.TRUE.equals(settingsMap.get("index.blocks.write"))) {
                        b.blocks(bl -> bl.write(true));
                    }
                    // routing allocation require settings
                    Map<String, JsonData> otherSettings = new HashMap<>();
                    for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
                        if (entry.getKey().startsWith("index.routing.allocation.require.")) {
                            otherSettings.put(entry.getKey(), JsonData.of(entry.getValue().toString()));
                        }
                    }
                    if (!otherSettings.isEmpty()) {
                        b.otherSettings(otherSettings);
                    }
                }
                return b;
            });
        }

        @SuppressWarnings("unchecked")
        private static TypeMapping buildTypeMapping(Map<String, Object> mappingMap) {
            return TypeMapping.of(b -> {
                if (mappingMap == null) {
                    return b;
                }
                String dynamic = (String) mappingMap.get("dynamic");
                if ("true".equals(dynamic)) {
                    b.dynamic(DynamicMapping.True);
                } else if ("strict".equals(dynamic)) {
                    b.dynamic(DynamicMapping.Strict);
                } else {
                    b.dynamic(DynamicMapping.False);
                }

                Map<String, Object> source = (Map<String, Object>) mappingMap.get("_source");
                if (source != null && Boolean.FALSE.equals(source.get("enabled"))) {
                    b.source(s -> s.enabled(false));
                }

                Map<String, Object> propsMap = (Map<String, Object>) mappingMap.get("properties");
                if (propsMap != null) {
                    for (Map.Entry<String, Object> entry : propsMap.entrySet()) {
                        Map<String, Object> fieldMapping = (Map<String, Object>) entry.getValue();
                        b.properties(entry.getKey(), buildProperty(fieldMapping));
                    }
                }
                return b;
            });
        }

        @SuppressWarnings("unchecked")
        private static Property buildProperty(Map<String, Object> fieldMapping) {
            String type = (String) fieldMapping.get("type");
            if (type == null) {
                type = "keyword";
            }
            switch (type) {
                case "text":
                    return Property.of(p -> p.text(t -> {
                        if (fieldMapping.containsKey("analyzer")) {
                            t.analyzer(fieldMapping.get("analyzer").toString());
                        }
                        if (fieldMapping.containsKey("search_analyzer")) {
                            t.searchAnalyzer(fieldMapping.get("search_analyzer").toString());
                        }
                        if (fieldMapping.containsKey("copy_to")) {
                            t.copyTo(Collections.singletonList(fieldMapping.get("copy_to").toString()));
                        }
                        if (Boolean.TRUE.equals(fieldMapping.get("eager_global_ordinals"))) {
                            t.eagerGlobalOrdinals(true);
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) {
                            t.index(false);
                        }
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) {
                            t.store(true);
                        }
                        return t;
                    }));
                case "keyword":
                    return Property.of(p -> p.keyword(k -> {
                        if (fieldMapping.containsKey("null_value")) {
                            k.nullValue(fieldMapping.get("null_value").toString());
                        }
                        if (Boolean.TRUE.equals(fieldMapping.get("eager_global_ordinals"))) {
                            k.eagerGlobalOrdinals(true);
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) {
                            k.index(false);
                        }
                        if (fieldMapping.containsKey("copy_to")) {
                            k.copyTo(Collections.singletonList(fieldMapping.get("copy_to").toString()));
                        }
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) {
                            k.store(true);
                        }
                        return k;
                    }));
                case "long":
                    return Property.of(p -> p.long_(l -> {
                        if (fieldMapping.containsKey("null_value")) {
                            l.nullValue(Long.parseLong(fieldMapping.get("null_value").toString()));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) l.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) l.store(true);
                        return l;
                    }));
                case "integer":
                    return Property.of(p -> p.integer(i -> {
                        if (fieldMapping.containsKey("null_value")) {
                            i.nullValue(Integer.parseInt(fieldMapping.get("null_value").toString()));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) i.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) i.store(true);
                        return i;
                    }));
                case "double":
                    return Property.of(p -> p.double_(d -> {
                        if (fieldMapping.containsKey("null_value")) {
                            d.nullValue(Double.parseDouble(fieldMapping.get("null_value").toString()));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) d.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) d.store(true);
                        return d;
                    }));
                case "float":
                    return Property.of(p -> p.float_(f -> {
                        if (fieldMapping.containsKey("null_value")) {
                            f.nullValue(Float.parseFloat(fieldMapping.get("null_value").toString()));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) f.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) f.store(true);
                        return f;
                    }));
                case "date":
                    return Property.of(p -> p.date(d -> {
                        if (fieldMapping.containsKey("format")) {
                            d.format(fieldMapping.get("format").toString());
                        }
                        if (fieldMapping.containsKey("null_value")) {
                            d.nullValue(co.elastic.clients.util.DateTime.ofEpochMilli(
                                    Long.parseLong(fieldMapping.get("null_value").toString())));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) d.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) d.store(true);
                        return d;
                    }));
                case "boolean":
                    return Property.of(p -> p.boolean_(bl -> {
                        if (fieldMapping.containsKey("null_value")) {
                            bl.nullValue(Boolean.parseBoolean(fieldMapping.get("null_value").toString()));
                        }
                        if (Boolean.FALSE.equals(fieldMapping.get("index"))) bl.index(false);
                        if (Boolean.TRUE.equals(fieldMapping.get("store"))) bl.store(true);
                        return bl;
                    }));
                case "object":
                    return Property.of(p -> p.object(o -> o));
                case "nested":
                    return Property.of(p -> p.nested(n -> n));
                default:
                    return Property.of(p -> p.keyword(k -> k));
            }
        }
    }
    // ==================== Mappings 内部类 ====================
    
    /**
     * Elasticsearch Mapping 相关API
     */
    public static class Mappings {
        
        /**
         * 获取所有的Mapping信息
         * 返回的Map是Map套Map, 输出成JSON大概是这样子
         * <pre> {@code
         * {
         *   "properties": {
         *     "title": {
         *       "type": "text",
         *       "fields": {
         *         "keyword": {
         *           "type": "keyword",
         *           "ignore_above": 256
         *         }
         *       }
         *     },
         *     "year": {
         *       "type": "long"
         *     }
         *   }
         * }
         * }</pre>
         *
         * @param index 索引名
         * @return Map<String, Object> mapping信息
         */
        public static Map<String, Object> getMapping(String index) {
            try {
                GetMappingRequest request = GetMappingRequest.of(builder -> builder
                        .index(index)
                );
                
                var response = QUERY_CLIENT.indices().getMapping(request);
                // TypeMapping 没有 toMap() 方法,需要通过 Jackson 转换
                return JacksonUtils.toMap(JacksonUtils.toJson(response.result().get(index).mappings()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to get mapping", e);
            }
        }

        /**
         * 获取索引中某个字段的mapping定义, 返回的Map格式类似这样:
         * <pre>
         * {
         *   "income": {
         *     "type": "long",
         *     "index": false
         *   },
         *   "carrer": {
         *     "type": "text",
         *     "analyzer": "ik_max_word",
         *     "search_analyzer": "ik_smart"
         *   },
         *   "fans": {
         *     "type": "text"
         *   }
         * }
         * </pre>
         *
         * @param index  索引名
         * @param fields 字段名
         * @return Map<String, Map<String, Object>>
         */
        public static Map<String, Map<String, Object>> getMapping(String index, String... fields) {
            try {
                var request = co.elastic.clients.elasticsearch.indices.GetFieldMappingRequest.of(b -> b
                        .index(index)
                        .fields(Arrays.asList(fields))
                );
                var response = QUERY_CLIENT.indices().getFieldMapping(request);
                
                Map<String, Map<String, Object>> result = new HashMap<>();
                var typeFieldMappings = response.result().get(index);
                if (typeFieldMappings != null) {
                    for (var entry : typeFieldMappings.mappings().entrySet()) {
                        String fieldName = entry.getKey();
                        var fieldMapping = entry.getValue();
                        Map<String, Object> mappingMap = JacksonUtils.toMap(JacksonUtils.toJson(fieldMapping.mapping()));
                        result.put(fieldName, mappingMap);
                    }
                }
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Failed to get field mapping for index: " + index, e);
            }
        }

        /**
         * 设置索引的Mapping, index必须先创建, 可以为index增加字段定义, 但是不能删除已有的字段定义<p/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/mapping.html<br/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/dynamic-mapping.html<br/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/mapping-params.html
         *
         * @param index   索引名
         * @param dynamic Dynamic枚举值
         * @return ElasticPutMappingBuilder Mapping创建成功失败标识
         */
        public static ElasticPutMappingBuilder putMapping(String index, Dynamic dynamic) {
            return new ElasticPutMappingBuilder(index, dynamic);
        }
        
        /**
         * 为已有的Index设置Mapping
         * mapping 格式类似这样:
         * <pre>
         * {
         *   "properties": {
         *     "title": {
         *       "type": "text",
         *       "boost": 2.0
         *     },
         *     "content": {
         *       "type": "text"
         *     }
         *   }
         * }
         * </pre>
         * @param index   索引名
         * @param mapping mapping JSON串
         * @return boolean
         */
        public static boolean putMapping(String index, String mapping) {
            try {
                JacksonJsonpMapper mapper = new JacksonJsonpMapper();
                JsonParser parser = mapper.jsonProvider().createParser(
                        new java.io.ByteArrayInputStream(mapping.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                
                PutMappingRequest.Builder reqBuilder = new PutMappingRequest.Builder();
                reqBuilder.withJson(parser, mapper);
                reqBuilder.index(index);
                
                return QUERY_CLIENT.indices().putMapping(reqBuilder.build()).acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to put mapping for index: " + index, e);
            }
        }
    }

    // ==================== Settings 内部类 ====================
    
    /**
     * Elasticsearch Settings 相关 API
     */
    public static class Settings {

        /**
         * 更新索引的Settings
         *
         * @param indices 索引名
         * @return ElasticUpdateSettingBuilder
         */
        public static ElasticUpdateSettingBuilder update(String... indices) {
            return new ElasticUpdateSettingBuilder(indices);
        }
        
        /**
         * 设置索引的Settings, 比如
         * <pre>
         * {
         *   "number_of_replicas": 2,          // 修改副本数（最常用）
         *   "refresh_interval": "30s",        // 修改数据刷新间隔
         *   "index.max_result_window": 20000, // 修改分页查询最大条数
         *   "index.unassigned.node_left.delayed_timeout": "5m" // 分片延迟分配
         * }
         * </pre>
         * @param index    索引名
         * @param settings settings JSON串
         * @return 是否设置成功
         */
        public static boolean putSettings(String index, String settings) {
            try {
                JacksonJsonpMapper mapper = new JacksonJsonpMapper();
                JsonParser parser = mapper.jsonProvider().createParser(
                        new java.io.ByteArrayInputStream(settings.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                
                PutIndicesSettingsRequest.Builder reqBuilder = new PutIndicesSettingsRequest.Builder();
                reqBuilder.withJson(parser, mapper);
                reqBuilder.index(index);
                
                PutIndicesSettingsResponse response = QUERY_CLIENT.indices().putSettings(reqBuilder.build());
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to put settings for index: " + index, e);
            }
        }
    }

    // ==================== Cluster 内部类 ====================
    
    /**
     * Elasticsearch 集群相关 API
     */
    public static class Cluster {

        /**
         * 获取集群的健康状态, Green Yellow Red
         *
         * @return String
         */
        public static String health() {
            try {
                var response = QUERY_CLIENT.cluster().health(builder -> builder);
                return response.status().name().toLowerCase();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get cluster health", e);
            }
        }

        /**
         * 获取集群设置构建器
         *
         * @return ClusterSettingBuilder
         */
        public static ClusterSettingBuilder settings() {
            return new ClusterSettingBuilder();
        }

        /**
         * 返回所有cluster setting, 包括persistent, transient
         *
         * @return Map<String, Object> 集群设置
         */
        public static Map<String, Object> allSettings() {
            try {
                GetClusterSettingsResponse response = QUERY_CLIENT.cluster().getSettings(b -> b);
                
                Map<String, Object> result = new HashMap<>();
                
                Map<String, Object> persistent = new HashMap<>();
                for (var entry : response.persistent().entrySet()) {
                    persistent.put(entry.getKey(), entry.getValue().to(String.class));
                }
                result.put("persistent", persistent);
                
                Map<String, Object> transientSettings = new HashMap<>();
                for (var entry : response.transient_().entrySet()) {
                    transientSettings.put(entry.getKey(), entry.getValue().to(String.class));
                }
                result.put("transient", transientSettings);
                
                Map<String, Object> defaults = new HashMap<>();
                for (var entry : response.defaults().entrySet()) {
                    defaults.put(entry.getKey(), entry.getValue().to(String.class));
                }
                result.put("defaults", defaults);
                
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Failed to get cluster settings", e);
            }
        }

        /**
         * 创建多字段聚合, 用法:
         * <pre> {@code
         * Map<String, Object> params = new HashMap<>();
         * params.put("fields", new String[]{"src_ip", "src_port"});
         *
         * Script painless = new Script(ScriptType.STORED, null, "multi_field_agg", params);
         * SearchResponse response = ElasticUtils.client.prepareSearch("events")
         * 		.addAggregation(AggregationBuilders.terms("script_agg").script(painless))
         * 		.get();
         * Aggregations aggregations = response.getAggregations();
         * Aggregation scriptAgg = aggregations.get("script_agg");
         * System.out.println(JacksonUtils.toPrettyJson(scriptAgg.toString()));
         * }</pre>
         *
         * @return boolean
         */
        public static boolean createMultiFieldAgg() {
            String script = "String fieldName = ''; " +
                    "for(int i=0; i<params.fields.length; i++) {" +
                    "String field = params.fields[i];" +
                    "if(!''.equals(fieldName) && (doc.containsKey(field+'.keyword') || doc.containsKey(field))) {" +
                    "fieldName +='|';" +
                    "}" +
                    "if(doc.containsKey(field+'.keyword')) {" +
                    "if(doc[field+'.keyword'].size() != 0) {" +
                    "fieldName += doc[field+'.keyword'].value;" +
                    "} else {" +
                    "fieldName += 'null';" +
                    "}" +
                    "} else if(doc.containsKey(field)){" +
                    "if(doc[field].size() != 0) {" +
                    "fieldName += doc[field].value;" +
                    "} else {" +
                    "fieldName += 'null';" +
                    "}" +
                    "}" +
                    "}" +
                    "return fieldName;";

            try {
                PutScriptRequest request = PutScriptRequest.of(b -> b
                        .id("multi_fields")
                        .script(s -> s
                                .lang("painless")
                                .source(script)
                        )
                );
                PutScriptResponse response = QUERY_CLIENT.putScript(request);
                return response.acknowledged();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create multi field agg script", e);
            }
        }
    }

    // ==================== Query 内部类 ====================

    /**
     * Elasticsearch 查询相关API
     * <p>
     * 提供丰富的查询方式: match, term, bool, range, matchPhrase, exists, prefix, geoDistance, multiMatch, queryString, uriQuery 等
     */
    public static final class Query {

        /**
         * 根据ID获取文档
         *
         * @param index 索引名
         * @param id    文档ID
         * @return String JSON字符串
         */
        public static String byId(String index, Object id) {
            return get(index, id.toString());
        }

        /**
         * 根据ID获取文档
         *
         * @param index     索引名
         * @param id        文档ID
         * @param resultType 返回类型
         * @return T
         */
        public static <T> T byId(String index, Object id, Class<T> resultType) {
            return get(index, id.toString(), resultType);
        }

        /**
         * 基于ID列表获取
         *
         * @param indices 索引名
         * @return ElasticIdsQueryBuilder
         */
        public static ElasticIdsQueryBuilder idsQuery(String... indices) {
            return new ElasticIdsQueryBuilder(indices);
        }

        /**
         * 基于ID列表获取
         *
         * @param indices ID集合
         * @return ElasticIdsQueryBuilder
         */
        public static ElasticIdsQueryBuilder idsQuery(Collection<String> indices) {
            return new ElasticIdsQueryBuilder(indices.stream().toArray(String[]::new));
        }

        /**
         * 指定查询语句, 使用Query String Syntax
         *
         * @param index 索引名
         * @return ElasticUriQueryBuilder
         */
        public static ElasticUriQueryBuilder uriQuery(String index) {
            return new ElasticUriQueryBuilder(index);
        }

        /**
         * Query String查询
         *
         * @param index 索引名
         * @return ElasticQueryStringBuilder
         */
        public static ElasticQueryStringBuilder queryString(String index) {
            return new ElasticQueryStringBuilder(index);
        }

        /**
         * Match Query是会对搜索的内容做分词后再去ES中查询的
         *
         * @param indices 索引名
         * @return ElasticMatchQueryBuilder
         */
        public static ElasticMatchQueryBuilder matchQuery(String... indices) {
            return new ElasticMatchQueryBuilder(indices);
        }

        /**
         * Match All Query
         *
         * @param indices 索引名
         * @return ElasticMatchAllQueryBuilder
         */
        public static ElasticMatchAllQueryBuilder matchAllQuery(String... indices) {
            return new ElasticMatchAllQueryBuilder(indices);
        }

        /**
         * Scroll查询, 用于深度遍历大量数据
         *
         * @param indices 索引名
         * @return ElasticScrollQueryBuilder
         */
        public static ElasticScrollQueryBuilder scrollQuery(String... indices) {
            return new ElasticScrollQueryBuilder(indices);
        }

        /**
         * Term查询, 对输入不做分词
         *
         * @param indices 索引名
         * @return ElasticTermQueryBuilder
         */
        public static ElasticTermQueryBuilder termQuery(String... indices) {
            return new ElasticTermQueryBuilder(indices);
        }

        /**
         * Term查询, 对输入不做分词(便捷方法)
         * <p>
         * 直接指定field和value, 无需先创建builder再设置field/value
         *
         * @param field 字段名
         * @param value 值
         * @return ElasticTermQueryBuilder
         */
        public static ElasticTermQueryBuilder termQuery(String field, Object value) {
            return new ElasticTermQueryBuilder().query(field, value);
        }

        /**
         * Terms查询, 匹配多个精确值中的任意一个
         *
         * @param indices 索引名
         * @return ElasticTermsQueryBuilder
         */
        public static ElasticTermsQueryBuilder termsQuery(String... indices) {
            return new ElasticTermsQueryBuilder(indices);
        }

        /**
         * Match Phrase Query查的是一个短语
         *
         * @param indices 索引名
         * @return ElasticMatchPhraseQueryBuilder
         */
        public static ElasticMatchPhraseQueryBuilder matchPhraseQuery(String... indices) {
            return new ElasticMatchPhraseQueryBuilder(indices);
        }

        /**
         * Match Phrase Prefix Query查的是一个短语前缀
         *
         * @param indices 索引名
         * @return ElasticMatchPhrasePrefixQueryBuilder
         */
        public static ElasticMatchPhrasePrefixQueryBuilder matchPhrasePrefixQuery(String... indices) {
            return new ElasticMatchPhrasePrefixQueryBuilder(indices);
        }

        /**
         * geo_distance查询
         *
         * @param indices 索引名
         * @return ElasticGeoDistanceQueryBuilder
         */
        public static ElasticGeoDistanceQueryBuilder geoDistance(String... indices) {
            return new ElasticGeoDistanceQueryBuilder(indices);
        }

        /**
         * exists Query
         *
         * @param indices 索引名
         * @return ElasticExistsQueryBuilder
         */
        public static ElasticExistsQueryBuilder exists(String... indices) {
            return new ElasticExistsQueryBuilder(indices);
        }

        /**
         * prefix Query
         *
         * @param indices 索引名
         * @return ElasticPrefixQueryBuilder
         */
        public static ElasticPrefixQueryBuilder prefix(String... indices) {
            return new ElasticPrefixQueryBuilder(indices);
        }

        /**
         * Range Query, 支持日期, 数字类型
         *
         * @param indices 索引名
         * @return ElasticRangeQueryBuilder
         */
        public static ElasticRangeQueryBuilder range(String... indices) {
            return new ElasticRangeQueryBuilder(indices);
        }

        /**
         * 通用查询接口
         *
         * @param indices 索引名
         * @return ElasticQueryBuilder
         */
        public static ElasticQueryBuilder query(String... indices) {
            return ElasticQueryBuilder.instance(indices);
        }

        /**
         * Multi Match Query 跨字段搜索
         *
         * @param indices 索引名
         * @return ElasticMultiMatchQueryBuilder
         */
        public static ElasticMultiMatchQueryBuilder multiMatch(String... indices) {
            return new ElasticMultiMatchQueryBuilder(indices);
        }

        /**
         * 布尔/组合 查询
         *
         * @param indices 索引名
         * @return ElasticBoolQueryBuilder
         */
        public static ElasticBoolQueryBuilder bool(String... indices) {
            return new ElasticBoolQueryBuilder(indices);
        }

        /**
         * Constant Score查询, 将查询转为Filtering以取消相关性算分, 提升查询性能
         * <p>
         * 即便是对Keyword 进行 Term 查询, 同样会被算分
         * 可以将查询转为 Filtering, 取消相关性算分的环节, 以提升性能
         * filter可以有效利用缓存
         * <p>
         * https://www.elastic.co/guide/cn/elasticsearch/guide/current/ignoring-tfidf.html
         *
         * @param indices 索引名
         * @return ElasticQueryBuilder
         */
        public static ElasticQueryBuilder constantScoreQuery(String... indices) {
            ElasticQueryBuilder builder = ElasticQueryBuilder.instance(indices);
            builder.constantScore(true);
            return builder;
        }

        /**
         * Elasticsearch 默认会以文档的相关度算分进行排序<br/>
         * 可以通过指定一个或多个字段进行排序<br/>
         * 使用相关度算分(Score)排序, 不能满足某些特定的条件: 无法针对相关度, 对排序实现更多的控制<br/>
         * <p/>
         * 可以在查询结束后, 对每一个匹配的文档进行一系列的重新算分, 根据新生成的分数进行排序<br/>
         * 提供了几种默认的计算分值的函数:
         * <ul>
         * <li/>Weight             为每一个文档设置一个简单而不被规范化的权重
         * <li/>Field Value Factor 使用该数值来修改_score, 例如将"热度"和"点赞数"作为算分的参考因素
         * <li/>Random Score       为每一个用户使用一个不同的, 随机算分结果
         * <li/>衰减函数            以某个字段的值为标准, 距离某个值越近, 得分越高
         * <li/>Script Score       自定义脚本完全控制所需逻辑
         * </ul>
         * <p/>
         * FunctionScore可以通过FunctionScoreBuilders构造出来<p/>
         * <p>
         * random_score 一致性随机函数
         *
         * <ul>
         * <li/>使用场景  网站的广告需要提高展现率
         * <li/>具体需求  让每一个用户能看到不同的随机排名, 但是也希望同一个用户访问时, 结果的相对顺序保持一致(Constantly Random)
         * </ul>
         * <br/>
         * 实际使用random_score时, 只要同一个人使用同一个seed就可以保证这个人的多次查询顺序是一致的
         * <p>
         * https://www.elastic.co/guide/cn/elasticsearch/guide/current/function-score-query.html
         * https://www.elastic.co/guide/cn/elasticsearch/guide/current/random-scoring.html
         *
         * @param scoreFunction ES 8.x FunctionScore
         * @param indices       索引名
         * @return ElasticQueryBuilderzo
         */
        public static ElasticQueryBuilder functionScoreQuery(FunctionScore scoreFunction, String... indices) {
            if (scoreFunction == null) {
                throw new IllegalArgumentException("scoreFunction can not be null!");
            }
            if (indices == null || indices.length == 0) {
                throw new IllegalArgumentException("indices can not be empty");
            }
            ElasticQueryBuilder builder = ElasticQueryBuilder.instance(indices);
            builder.addScoreFunction(scoreFunction);
            return builder;
        }

        /**
         * Search Template Query, 基于预定义的Mustache模板查询
         *
         * @param indices 索引名
         * @return ElasticTemplateQueryBuilder
         */
        public static ElasticTemplateQueryBuilder templateQuery(String... indices) {
            return new ElasticTemplateQueryBuilder(indices);
        }
    }

    // ==================== Aggs 内部类 ====================

    /**
     * 聚合分析相关API
     */
    public static class Aggs {

        /**
         * 返回总命中数
         *
         * @return Long
         */
        public static Long totalHits() {
            return ThreadContext.get(ElasticConstants.TOTAL_HITS);
        }

        // ---------------------- Bucket 聚合 ----------------------

        /**
         * terms聚合, Bucket聚合的一种
         * https://www.elastic.co/guide/en/elasticsearch/client/java-api/7.x/java-aggs.html
         *
         * @param indices
         * @return ElasticTermsAggregationBuilder
         */
        public static ElasticTermsAggregationBuilder terms(String... indices) {
            return ElasticTermsAggregationBuilder.instance(indices);
        }

        /**
         * multi terms聚合, Bucket聚合的一种, 使用painless script实现
         * https://www.elastic.co/guide/en/elasticsearch/client/java-api/7.x/java-aggs.html
         *
         * @param indices
         * @return ElasticMultiTermsAggregationBuilder
         */
        public static ElasticMultiTermsAggregationBuilder multiTerms(String... indices) {
            return ElasticMultiTermsAggregationBuilder.instance(indices);
        }

        /**
         * Range Aggregation
         * <p>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-aggregations-bucket-histogram-aggregation.html
         *
         * @param indices
         * @return ElasticRangeAggregationBuilder
         */
        public static ElasticRangeAggregationBuilder range(String... indices) {
            return ElasticRangeAggregationBuilder.instance(indices);
        }

        /**
         * Histogram Aggregation
         * <p>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-aggregations-bucket-histogram-aggregation.html
         *
         * @param indices
         * @return ElasticHistogramAggregationBuilder
         */
        public static ElasticHistogramAggregationBuilder histogram(String... indices) {
            return ElasticHistogramAggregationBuilder.instance(indices);
        }

        /**
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-aggregations-bucket-datehistogram-aggregation.html
         *
         * @param indices
         * @return ElasticDateHistogramAggregationBuilder
         */
        public static ElasticDateHistogramAggregationBuilder dateHistogram(String... indices) {
            return ElasticDateHistogramAggregationBuilder.instance(indices);
        }

        // ---------------------- Metric 聚合 ----------------------

        /**
         * min聚合, Metric聚合的一种
         *
         * @param indices
         * @return ElasticMinAggregationBuilder
         */
        public static ElasticMinAggregationBuilder min(String... indices) {
            return ElasticMinAggregationBuilder.instance(indices);
        }

        /**
         * max聚合, Bucket聚合的一种
         *
         * @param indices
         * @return ElasticMaxAggregationBuilder
         */
        public static ElasticMaxAggregationBuilder max(String... indices) {
            return ElasticMaxAggregationBuilder.instance(indices);
        }

        /**
         * avg聚合
         *
         * @param indices
         * @return ElasticAvgAggregationBuilder
         */
        public static ElasticAvgAggregationBuilder avg(String... indices) {
            return ElasticAvgAggregationBuilder.instance(indices);
        }

        /**
         * sum聚合
         *
         * @param indices
         * @return ElasticSumAggregationBuilder
         */
        public static ElasticSumAggregationBuilder sum(String... indices) {
            return ElasticSumAggregationBuilder.instance(indices);
        }

        /**
         * stats聚合
         *
         * @param indices
         * @return ElasticStatsAggregationBuilder
         */
        public static ElasticStatsAggregationBuilder stats(String... indices) {
            return ElasticStatsAggregationBuilder.instance(indices);
        }

        /**
         * Cardinality聚合, 对字段去重后统计数量 <br/>
         * 比如你想通过聚合分析知道, 每天网站中的访客来自多少个不同的IP
         * <p>
         *
         * @param indices
         * @return ElasticCardinalityAggregationBuilder
         */
        public static ElasticCardinalityAggregationBuilder cardinality(String... indices) {
            return ElasticCardinalityAggregationBuilder.instance(indices);
        }

        /**
         * 组合多个聚合, 就像这个, 一个查询中包含两个聚合
         * <pre>
         * POST bank/_search
         * {
         *   "query": {
         *     "match": {
         *       "address": "mill"
         *     }
         *   },
         *   "size": 0,
         *   "aggs": {
         *     "age_agg": {
         *       "terms": {
         *         "field": "age"
         *       }
         *     },
         *     "age_avg":{
         *       "avg": {
         *         "field": "age"
         *       }
         *     }
         *   }
         * }
         * </pre>
         *
         * @param indices
         * @return ElasticCompositeAggregationBuilder
         */
        public static ElasticCompositeAggregationBuilder composite(String... indices) {
            return ElasticCompositeAggregationBuilder.instance(indices);
        }
    }

}
