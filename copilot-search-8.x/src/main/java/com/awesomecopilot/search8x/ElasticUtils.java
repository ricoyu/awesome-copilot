package com.awesomecopilot.search8x;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.common.lang.transformer.Transformers;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.networking.enums.HttpMethod;
import com.awesomecopilot.networking.utils.HttpUtils;
import com.awesomecopilot.search8x.builder.ElasticIndexDocBuilder;
import com.awesomecopilot.search8x.builder.ElasticRangeQueryBuilder;
import com.awesomecopilot.search8x.builder.admin.ClusterSettingBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticIndexBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticIndexTemplateBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticPutMappingBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticReindexBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticSettingsBuilder;
import com.awesomecopilot.search8x.builder.admin.ElasticUpdateSettingBuilder;
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
import com.awesomecopilot.search8x.builder.agg.v8.V8TermsAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8RangeAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8HistogramAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8DateHistogramAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8MinAggregationBuilder;
import com.awesomecopilot.search8x.builder.bulk.ESBulkProcessor;
import com.awesomecopilot.search8x.builder.bulk.ElasticBulkIndexBuilder;
import com.awesomecopilot.search8x.builder.bulk.ElasticBulkUpdateBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticBoolQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticContextSuggestBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticExistsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticGeoDistanceQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticIdsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchAllQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchPhrasePrefixQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchPhraseQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMatchQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMultiGetBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticMultiMatchQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticPipelineBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticPrefixQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticQueryStringBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticScrollQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticSuggestBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTemplateQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTermQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticTermsQueryBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticUpdateBuilder;
import com.awesomecopilot.search8x.builder.query.ElasticUriQueryBuilder;
import com.awesomecopilot.search8x.cache.ElasticCacheUtils;
import com.awesomecopilot.search8x.constants.ElasticConstants;
import com.awesomecopilot.search8x.enums.Analyzer;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.exception.DocumentSaveException;
import com.awesomecopilot.search8x.exception.IndexTemplateException;
import com.awesomecopilot.search8x.exception.PutMappingException;
import com.awesomecopilot.search8x.exception.PutSettingsException;
import com.awesomecopilot.search8x.factory.ElasticsearchClientFactory;
import com.awesomecopilot.search8x.support.BulkResult;
import com.awesomecopilot.search8x.support.DocumentOperationResult;
import com.awesomecopilot.search8x.support.DocumentRestSupport;
import com.awesomecopilot.search8x.support.IndexSupport;
import com.awesomecopilot.search8x.support.IndicesClientSupport;
import com.awesomecopilot.search8x.support.IndicesRestSupport;
import com.awesomecopilot.search8x.support.MappingSupport;
import com.awesomecopilot.search8x.support.RestSupport;
import com.awesomecopilot.search8x.support.SearchRequestSupport;
import com.awesomecopilot.search8x.support.SettingsSupport;
import com.awesomecopilot.search8x.support.UpdateResult;
import com.awesomecopilot.search8x.vo.Index;
import com.awesomecopilot.search8x.vo.VersionedDoc;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.IndexTemplateMetadata;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.indices.IndexTemplateMissingException;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.json.jackson.JacksonUtils.toObject;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Elasticsearch 的工具类, 开箱即用的ES操作
 * <p>
 * Copyright: (C), 2021-01-01 8:37
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ElasticUtils.class);
    /**
     * 唯一的一个type是_doc
     */
    public static final String ONLY_TYPE = "_doc";

    /**
     * 查询模块客户端 (elasticsearch-java 8.x)。
     */
    public static final ElasticsearchClient QUERY_CLIENT = ElasticsearchClientFactory.createQueryClient();

    /**
     * 聚合、索引管理、文档 CRUD 等暂用 HLRC（7.x REST 适配）。
     */
    public static final RestHighLevelClient CLIENT = ElasticsearchClientFactory.createHighLevelClient();

    private static final ESBulkProcessor BULK_PROCESSOR = new ESBulkProcessor();
    
    private static final String USERNAME = "elastic.username";
    private static final String PASSWORD = "elastic.password";
    
    /**
     * 默认读取classpath下elastic.properties文件
     */
    private static PropertyReader propertyReader = new PropertyReader("elastic");
    
    private static String username = propertyReader.getString(USERNAME);
    private static String password = propertyReader.getString(PASSWORD);
    
    /**
     * 只是初始化一下ES客户端连接
     */
    public static void ping() {
        Admin.existsIndex("ricoyu");
    }

    /**
     * Fluent风格创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index
     * @return String 文档ID
     */
    public static ElasticIndexDocBuilder index(String index) {
        return new ElasticIndexDocBuilder(index);
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index
     * @param doc
     * @return String 文档ID
     */
    public static String index(String index, String doc) {
        return index(index, doc, null);
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档, 会自动通过Jackson序列化成JSON串
     * @return String 文档ID
     */
    public static String index(String index, Object doc) {
        if (doc == null) {
            return null;
        }

        String id = ElasticCacheUtils.getIdValue(doc);
        return index(index, doc, id);
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index
     * @param doc
     * @return String 文档ID
     */
    public static String index(String index, String doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.indexWithResult(QUERY_CLIENT, index, id, doc, false);
        if (!result.isSuccess()) {
            throw new DocumentSaveException("Failed to index document: " + result.getErrorMessage());
        }
        return result.getId();
    }

    /**
     * 返回索引的文档数量
     * @param index
     * @return 索引的文档数量
     */
    public static long docCount(String index) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(0);
        SearchResponse response = SearchRequestSupport.search(QUERY_CLIENT, new String[] {index}, sourceBuilder);
        long totalHits = response.getHits().getTotalHits().value;
        log.debug("索引 {} 中的文档总数: {}", index, totalHits);
        return totalHits;
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 使用/_create`端点是用来明确创建一个新的文档, 而不是更新它。如果指定的文档ID已经存在, 则会返回一个错误。
     * 对应REST API POST 方式
     *
     * @param index
     * @param doc
     * @return String 文档ID
     */
    public static String create(String index, String doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.indexWithResult(QUERY_CLIENT, index, id, doc, true);
        if (!result.isSuccess()) {
            throw new DocumentSaveException("Failed to create document: " + result.getErrorMessage());
        }
        return result.getId();
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID, 相同ID的文档如果已存在, Elasticsearch底层会先删掉该文档, 然后重新创建一个文档, 版本号+1
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String index(String index, String doc, int id) {
        return index(index, doc, String.valueOf(id));
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String index(String index, Object doc, int id) {
        return index(index, doc, String.valueOf(id));
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String index(String index, Object doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.indexWithResult(QUERY_CLIENT, index, id, toJson(doc), false);
        if (!result.isSuccess()) {
            throw new DocumentSaveException("Failed to index document: " + result.getErrorMessage());
        }
        return result.getId();
    }

    /**
     * 创建一个新的文档, 返回新创建文档的ID, 使用/_create`端点是用来明确创建一个新的文档, 而不是更新它。如果指定的文档ID已经存在, 则会返回一个错误。
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String create(String index, String doc, int id) {
        return create(index, doc, String.valueOf(id));
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
     * 创建一个新的文档, 返回新创建文档的ID
     * 使用/_create`端点是用来明确创建一个新的文档, 而不是更新它。如果指定的文档ID已经存在, 则会返回一个错误。
     * 对应REST API POST 方式
     *
     * @param index 索引名
     * @param doc   要保存的文档
     * @return String 文档ID
     */
    public static String create(String index, Object doc, String id) {
        Objects.requireNonNull(index, "index cannot be null!");
        if (doc == null) {
            return null;
        }
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.indexWithResult(QUERY_CLIENT, index, id, toJson(doc), true);
        if (!result.isSuccess()) {
            throw new DocumentSaveException("Failed to create document: " + result.getErrorMessage());
        }
        return result.getId();
    }

    /**
     * 批量创建文档<p>
     * 返回创建结果, 包括成功数量, 失败数量, 失败消息, 成功创建的文档id列表<p>
     * 单个bulk请求体的数据量不要太大, 官方建议大于5~15mb
     *
     * @param index
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
     * @param index
     * @param docs
     * @return BulkResult
     */
    public static BulkResult bulkIndex(String index, String... docs) {
        BulkRequest bulkRequest = new BulkRequest();
        asList(docs).forEach((doc) -> bulkRequest.add(new IndexRequest(index).source(doc, XContentType.JSON)));
        BulkResponse responses = DocumentRestSupport.bulk(QUERY_CLIENT, bulkRequest);

        BulkResult bulkResult = new BulkResult();
        for (BulkItemResponse response : responses) {
            if (response.isFailed()) {
                //记录失败数
                bulkResult.fail();
                //记录失败message
                bulkResult.addFailMessage(response.getFailureMessage());
            } else {
                //记录成功数
                bulkResult.success();
                //记录生成的id
                bulkResult.addId(response.getId());
            }
        }

        return bulkResult;
    }

    /**
     * 基于BulkProcessor批量创建文档, 该方式本身已经集成了多线程<p>
     *
     * @param index
     * @param docs
     * @return void
     */
    public static void bulkIndexConcurrent(String index, List<?> docs) {
        BulkProcessor bulkProcessor = BULK_PROCESSOR.bulkProcessor();
        for (Object doc : docs) {
            IndexRequest indexRequest = new IndexRequest(index).source(doc, XContentType.JSON);
            bulkProcessor.add(indexRequest);
        }
        bulkProcessor.flush();
        try {
            bulkProcessor.awaitClose(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    /**
     * 批量创建文档<p>
     * 返回创建结果, 包括成功数量, 失败数量, 失败消息, 成功创建的文档id列表<p>
     *
     * @param index
     * @param docs
     * @return BulkResult
     */
    public static BulkResult bulkIndex(String index, List<?> docs) {
        BulkRequest bulkRequest = new BulkRequest();
        docs.stream()
                .filter(Objects::nonNull)
                .forEach((doc) -> {
                    String id = ElasticCacheUtils.getIdValue(doc);
                    IndexRequest indexRequest = new IndexRequest(index).source(toJson(doc), XContentType.JSON);
                    if (id != null) {
                        indexRequest.id(id);
                    }
                    bulkRequest.add(indexRequest);
                });
        BulkResponse itemResponses = DocumentRestSupport.bulk(QUERY_CLIENT, bulkRequest);
        BulkResult bulkResult = new BulkResult();

        for (Iterator<BulkItemResponse> iterator = itemResponses.iterator(); iterator.hasNext(); ) {
            BulkItemResponse itemResponse = iterator.next();
            if (itemResponse.isFailed()) {
                //记录失败数
                bulkResult.fail();
                //记录失败message
                bulkResult.addFailMessage(itemResponse.getFailureMessage());
            } else {
                bulkResult.success();
                bulkResult.addId(itemResponse.getId());
            }
        }

        return bulkResult;
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
     * 根据ID获取文档
     *
     * @param index 索引名
     * @param id    文档id
     * @return T
     */
    public static String get(String index, String id) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");

        GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id, true);
        return response.getSourceAsString();
    }


    /**
     * 根据ID获取文档
     *
     * @param index 索引名
     * @param id    文档id
     * @return T
     */
    public static VersionedDoc<String> getWithVersion(String index, String id) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");

        GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id, true);
        long ifSeqNo = response.getSeqNo();
        long ifPrimaryTerm = response.getPrimaryTerm();

        String source = response.getSourceAsString();
        return VersionedDoc.<String>builder()
                .source(source)
                .id(id)
                .version(response.getVersion())
                .ifSeqNo(ifSeqNo)
                .ifPrimaryTerm(ifPrimaryTerm)
                .build();
    }

    /**
     * 根据ID获取并转成指定类型对象
     *
     * @param index 索引名
     * @param id    文档id
     * @param clazz
     * @param <T>
     * @return T
     */
    public static <T> T get(String index, String id, Class<T> clazz) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");
        Objects.requireNonNull(clazz, "clazz不能为null");

        GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id, true);
        String source = response.getSourceAsString();
        return toObject(source, clazz);
    }

    /**
     * 根据ID获取并转成指定类型对象
     *
     * @param index 索引名
     * @param id    文档id
     * @param clazz
     * @param <T>
     * @return T
     */
    public static <T> VersionedDoc<T> getWithVersion(String index, String id, Class<T> clazz) {
        Objects.requireNonNull(index, "索引名不能为null");
        Objects.requireNonNull(id, "id 不能为null");
        Objects.requireNonNull(clazz, "clazz不能为null");

        GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id, true);

        long seqNo = response.getSeqNo();
        long primaryTerm = response.getPrimaryTerm();
        String source = response.getSourceAsString();
        T result = toObject(source, clazz);

        return VersionedDoc.<T>builder()
                .ifSeqNo(seqNo)
                .ifPrimaryTerm(primaryTerm)
                .source(result)
                .build();
    }

    /**
     * 从一个或者多个索引中根据id获取多文档
     *
     * @return
     */
    public static ElasticMultiGetBuilder mget() {
        return new ElasticMultiGetBuilder(QUERY_CLIENT);
    }

    /**
     * 删除一篇文档
     *
     * @param index 索引名
     * @param id 文档ID
     * @return Result
     */
    public static boolean delete(String index, Integer id) {
        Objects.requireNonNull(id, "id cannot be null!");
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.deleteWithResult(QUERY_CLIENT, index, id.toString());
        return result.isSuccess() && "deleted".equals(result.getResult());
    }

    /**
     * 删除一篇文档
     *
     * @param index
     * @param id
     * @return Result
     */
    public static boolean delete(String index, String id) {
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.deleteWithResult(QUERY_CLIENT, index, id);
        return result.isSuccess() && "deleted".equals(result.getResult());
    }

    /**
     * 根据单个条件删除, 返回删除的记录数
     * 条件匹配是按照精确匹配来处理的, 为了防止误删
     *
     * @param index
     * @param field
     * @param value
     * @return long
     */
    public static long deleteBy(String index, String field, String value) {
        // 使用底层 RestClient 执行 delete-by-query 请求
        try {
            RestClientTransport transport =
                    (RestClientTransport) QUERY_CLIENT._transport();
            RestClient restClient = transport.restClient();
            
            // 构建查询条件
            JSONObject queryObj = new JSONObject();
            JSONObject termObj = new JSONObject();
            termObj.put(field, value);
            JSONObject queryTermObj = new JSONObject();
            queryTermObj.put("term", termObj);
            queryObj.put("query", queryTermObj);
            
            // 执行 HTTP 请求
            Request request = new Request("POST", "/" + index + "/_delete_by_query");
            request.setJsonEntity(queryObj.toString());
            
            Response response = restClient.performRequest(request);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            
            // 解析响应获取删除数量
            JSONObject root = new JSONObject(jsonResponse);
            return root.optLong("deleted", 0);
        } catch (Exception e) {
            throw new com.awesomecopilot.search8x.exception.DocumentDeleteException(e);
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
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.6/java-rest-high-document-update.html
     *
     * @param index String 索引名
     * @param id String  文档的id
     * @param doc   整篇文档或者文档的一部分
     * @return Result 更新结果(更新了? 没更新?)
     */
    public static UpdateResult update(String index, String id, String doc) {
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult result = DocumentRestSupport.updateWithResult(QUERY_CLIENT, index, id, doc, false);
        return UpdateResult.from(result);
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
    public static UpdateResult update(String index, Integer id, String doc) {
        if (id == null) {
            return null;
        }
        return update(index, id.toString(), doc);
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
        return update(index, id, toJson(doc));
    }

    /**
     * 更新文档的一部分, 或者ID对应的文档不存在时创建文档
     * <ol>
     * <li/>如果ID对应的文档在ES中还不存在, 那么创建文档
     * <li/>如果docPiece对应的字段在文档中还不存在, 那么在原文档中插入这个字段
     * <li/>如果docPiece对应的字段在文档中存在, 并且值不一样, 那么执行更新
     * <li/>docPiece对应的字段在文档中存在, 但是值是一样的, 那么不执行更新
     * </ol>
     * https://www.elastic.co/guide/en/elasticsearch/client/java-api/7.x/java-docs-update.html
     *
     * @param index
     * @param id
     * @param doc   整篇文档或者文档的一部分
     * @return Result 创建? 更新? 没更新?
     */
    public static UpdateResult upsert(String index, String id, String doc) {
        // 使用新的方法，避免版本兼容性问题
        DocumentOperationResult updateResponse = DocumentRestSupport.updateWithResult(QUERY_CLIENT, index, id, doc, true);
        return UpdateResult.from(updateResponse);
    }

    /**
     * 检查指定索引中是否存在指定id的文档
     *
     * @param index
     * @param id
     * @return boolean
     */
    public static boolean exists(String index, String id) {
        GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id, false);
        return response.isExists();
    }

    /**
     * https://www.elastic.co/guide/cn/elasticsearch/guide/current/ignoring-tfidf.html
     * <p>
     * 即便是对Keyword 进行 Term 查询, 同样会被算分
     * 可以将查询转为 Filtering, 取消相关性算分的环节, 以提升性能
     * filter可以有效利用缓存
     *
     * @param indices
     * @return
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
     * ScoreFunctionBuilder可以通过ScoreFunctionBuilders构造出来<p/>
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
     * @param scoreFunctionBuilder
     * @param indices
     * @return ElasticQueryBuilder
     */
    public static ElasticQueryBuilder functionScoreQuery(ScoreFunctionBuilder scoreFunctionBuilder, String... indices) {
        if (scoreFunctionBuilder == null) {
            throw new IllegalArgumentException("scoreFunctionBuilder can not be null!");
        }
        if (indices == null || indices.length == 0) {
            throw new IllegalArgumentException("indices can not be empty");
        }
        ElasticQueryBuilder builder = ElasticQueryBuilder.instance(indices);
        ReflectionUtils.setField("scoreFunctionBuilder", builder, scoreFunctionBuilder);
        return builder;
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
        ElasticSuggestBuilder elasticSuggestBuilder = new ElasticSuggestBuilder(indices);

        TermSuggestionBuilder suggestionBuilder = SuggestBuilders.termSuggestion(field)
                .suggestMode(TermSuggestionBuilder.SuggestMode.POPULAR)
                .text(text);

        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .suggestionBuilder(suggestionBuilder)
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
        ElasticSuggestBuilder elasticSuggestBuilder = new ElasticSuggestBuilder(indices);

        PhraseSuggestionBuilder suggestionBuilder = SuggestBuilders.phraseSuggestion(field)
                .text(text)
                .maxErrors(2f)
                .confidence(0)
                .highlight("<em>", "</em>");

        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .suggestionBuilder(suggestionBuilder)
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
        ElasticSuggestBuilder elasticSuggestBuilder = new ElasticSuggestBuilder(indices);

        CompletionSuggestionBuilder suggestionBuilder = SuggestBuilders.completionSuggestion(field)
                .prefix(prefix);

        String suggestName = UUID.randomUUID().toString();
        return ElasticUtils.suggest(indices)
                .name(suggestName)
                .suggestionBuilder(suggestionBuilder)
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
            // 使用 Elasticsearch 8.x Java Client 的 analyze API
            AnalyzeResponse response = QUERY_CLIENT.indices().analyze(a -> a
                    .analyzer(analyzer.toString())
                    .text(java.util.Arrays.asList(texts))
            );
            
            return response.tokens()
                    .stream()
                    .map(token -> token.token())
                    .distinct()
                    .collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在更新索引的mapping后, 在原索引上重建索引<p>
     * https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/java-docs-update-by-query.html
     *
     * @param indices
     * @return BulkByScrollResponse
     */
    public static BulkByScrollResponse updateByQuery(String... indices) {
        // 使用底层 RestClient 执行 update-by-query 请求
        try {
            RestClientTransport transport =
                    (RestClientTransport) QUERY_CLIENT._transport();
            RestClient restClient = transport.restClient();
            
            // 构建请求体 - 匹配所有文档
            JSONObject queryObj = new JSONObject();
            JSONObject matchAllObj = new JSONObject();
            matchAllObj.put("match_all", new JSONObject());
            queryObj.put("query", matchAllObj);
            
            // 执行 HTTP 请求
            Request request = new Request("POST", "/" + String.join(",", indices) + "/_update_by_query");
            request.addParameter("conflicts", "proceed"); // 相当于 abortOnVersionConflict(false)
            request.setJsonEntity(queryObj.toString());
            
            Response response = restClient.performRequest(request);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            
            log.info("Update by query response: {}", jsonResponse);
            
            // 解析响应为 BulkByScrollResponse
            return parseBulkByScrollResponse(jsonResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 从 JSON 响应解析为 BulkByScrollResponse
     */
    private static BulkByScrollResponse parseBulkByScrollResponse(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            
            // 使用反射创建 BulkByScrollResponse
            java.lang.reflect.Constructor<BulkByScrollResponse> constructor = 
                    BulkByScrollResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            BulkByScrollResponse response = constructor.newInstance();
            
            // 设置 updated 字段
            long updated = root.optLong("updated", 0);
            java.lang.reflect.Field updatedField = BulkByScrollResponse.class.getDeclaredField("updated");
            updatedField.setAccessible(true);
            updatedField.set(response, updated);
            
            // 设置 noops 字段
            long noops = root.optLong("noops", 0);
            java.lang.reflect.Field noopsField = BulkByScrollResponse.class.getDeclaredField("noops");
            noopsField.setAccessible(true);
            noopsField.set(response, noops);
            
            // 设置 versionConflicts 字段
            long versionConflicts = root.optLong("version_conflicts", 0);
            java.lang.reflect.Field versionConflictsField = BulkByScrollResponse.class.getDeclaredField("versionConflicts");
            versionConflictsField.setAccessible(true);
            versionConflictsField.set(response, versionConflicts);
            
            // 设置 bulkFailures 字段
            java.lang.reflect.Field bulkFailuresField = BulkByScrollResponse.class.getDeclaredField("bulkFailures");
            bulkFailuresField.setAccessible(true);
            bulkFailuresField.set(response, java.util.Collections.emptyList());
            
            // 设置 searchFailures 字段
            java.lang.reflect.Field searchFailuresField = BulkByScrollResponse.class.getDeclaredField("searchFailures");
            searchFailuresField.setAccessible(true);
            searchFailuresField.set(response, java.util.Collections.emptyList());
            
            // 设置 timedOut 字段
            boolean timedOut = root.optBoolean("timed_out", false);
            java.lang.reflect.Field timedOutField = BulkByScrollResponse.class.getDeclaredField("timedOut");
            timedOutField.setAccessible(true);
            timedOutField.set(response, timedOut);
            
            return response;
        } catch (Exception e) {
            log.error("Failed to parse update-by-query response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse update-by-query response", e);
        }
    }

    /**
     * Elasticsearch 索引管理相关API
     */
    public static class Admin {

        /**
         * 默认读取classpath下elastic.properties文件
         */
        //private static PropertyReader propertyReader = new PropertyReader("elastic");

        //private static final String USERNAME = "elastic.username";
        //private static final String PASSWORD = "elastic.password";
        //
        //
        //private static String username = propertyReader.getString(USERNAME);
        //private static String password = propertyReader.getString(PASSWORD);

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
            ElasticPutMappingBuilder putMappingBuilder = MappingSupport.extractIndexMapping(entityClass);
            //抽取索引的Setting信息
            ElasticSettingsBuilder settingsBuilder = SettingsSupport.extractIndexSettings(entityClass);
            //抽取索引名
            if (isBlank(index)) {
                index = IndexSupport.indexName(entityClass);
            }

            // 使用 ES 8.x Java Client
            ElasticIndexBuilder indexBuilder = new ElasticIndexBuilder(QUERY_CLIENT, index);
            boolean created = indexBuilder.settings(settingsBuilder)
                    .mapping(putMappingBuilder)
                    .create();
            log.info("Index {} {}", index, (created ? "created" : "not created"));
            return created;
        }

        /**
         * 创建索引, 默认1个分片, 0个副本
         *
         * @param index 索引名
         * @return boolean 创建成功失败标识
         */
        public static ElasticIndexBuilder createIndex(String index) {
            // 使用 ES 8.x Java Client
            return new ElasticIndexBuilder(QUERY_CLIENT, index);
        }

        /**
         * 判断索引存在与否
         *
         * @param indices
         * @return boolean
         */
        public static boolean existsIndex(String... indices) {
            return IndicesClientSupport.existsIndex(QUERY_CLIENT, indices);
        }

        /**
         * 删除索引
         *
         * @param indices
         * @return boolean 删除成功与否
         */
        public static boolean deleteIndex(String... indices) {
            if (!existsIndex(indices)) {
                log.info("索引{}不存在", (Object) indices);
                return false;
            }
            return IndicesClientSupport.deleteIndex(QUERY_CLIENT, indices);
        }

        /**
         * 列出所有索引
         *
         * @return List<String>
         */
        public static List<String> listIndexNames() {
            return IndicesRestSupport.listIndexNames(QUERY_CLIENT);
        }

        /**
         * 列出所有索引, 包含索引名, 主分片数, 副本数, uuid
         * @return List<Index>
         */
        public static List<Index> listIndices() {
            return IndicesRestSupport.listIndices(QUERY_CLIENT);
        }

        /**
         * 为Index创建别名
         *
         * @param index
         * @param alias
         * @return 创建成功与否
         */
        public static boolean createIndexAlias(String index, String alias) {
            return IndicesRestSupport.addAlias(QUERY_CLIENT, index, alias);
        }

        /**
         * 为Index创建别名
         *
         * @param indices
         * @param alias
         * @param queryBuilder
         * @return
         */
        public static boolean createIndexAlias(String[] indices, String alias, QueryBuilder queryBuilder) {
            return IndicesRestSupport.addAlias(QUERY_CLIENT, indices, alias, queryBuilder);
        }

        /**
         * 删除Index的别名
         *
         * @param index
         * @param alias
         * @return 删除成功与否
         */
        public static boolean deleteIndexAlias(String index, String alias) {
            return IndicesRestSupport.removeAlias(QUERY_CLIENT, index, alias);
        }


        /**
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/indices-templates.html
         *
         * @param templateName
         */
        public static ElasticIndexTemplateBuilder putIndexTemplateByFile(String templateName) {
            return ElasticIndexTemplateBuilder.newInstance(QUERY_CLIENT, templateName);
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
        public static boolean putIndexTemplate(String templateName, String templateContent) {
            int tryCount = 0;
            for (String host : RestSupport.HOSTS) {
                String result = "";
                try {
                    if (isBlank(username)) {
                        result = HttpUtils.post(host + "/_template/" + templateName)
                                .body(templateContent)
                                .method(HttpMethod.PUT)
                                .request();
                    } else {
                        result = HttpUtils.post(host + "/_template/" + templateName)
                                .body(templateContent)
                                .method(HttpMethod.PUT)
                                .basicAuth(username, password)
                                .request();

                    }
                } catch (Exception e) {
                    log.error("", e);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new IndexTemplateException(e.getMessage());
                    }
                    continue;
                }
                boolean hasError = JsonPathUtils.ifExists(result, "$.error");
                if (hasError) {
                    String errors = JsonPathUtils.readNode(result, "$.error.caused_by.reason");
                    log.error("PUT index template failed, host {}, [{}]", host, errors);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new IndexTemplateException(errors);
                    }
                }

                Boolean acknowledged = JsonPathUtils.readNode(result, "$.acknowledged");
                log.info("acknowledged: {}", acknowledged);
                return acknowledged;
            }

            return false;
        }

        /**
         * 获取指定的Index Template
         *
         * @param templateName
         * @return IndexTemplateMetaData
         */
        public static Map<String, IndexTemplateMetadata> getIndexTemplate(String templateName) {
            GetIndexTemplatesResponse response = IndicesRestSupport.getIndexTemplates(CLIENT, templateName);
            List<IndexTemplateMetadata> indexTemplates = response.getIndexTemplates();
            return indexTemplates.stream()
                    .collect(toMap(IndexTemplateMetadata::getName, identity()));
        }

        /**
         * 删除Index Template
         *
         * @param templateName
         * @return boolean
         */
        public static boolean deleteIndexTemplate(String templateName) {
            try {
                return IndicesRestSupport.deleteIndexTemplate(CLIENT, templateName);
            } catch (IndexTemplateMissingException e) {
                log.info("Index Template [{}] 不存在", templateName);
                return false;
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

        /**
         * 创建 Search Template
         *
         * @param templateName
         * @return boolean
         */
        public static boolean createSearchTemplate(String templateName, String templateFileName) {
            String templateContent = IOUtils.readClassPathFileAsString(templateFileName);

            Map<String, Object> rootNode = new HashMap<>();
            Map<String, Object> scriptNode = new HashMap<>();
            rootNode.put("script", scriptNode);
            scriptNode.put("lang", "mustache");
            scriptNode.put("source", templateContent);

            try {
                PutStoredScriptRequest request = new PutStoredScriptRequest();
                request.id(templateName);
                request.content(new BytesArray(toJson(rootNode)), XContentType.JSON);
                AcknowledgedResponse response = CLIENT.putScript(request, RequestOptions.DEFAULT);
                return response.isAcknowledged();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
                DeleteStoredScriptRequest request = new DeleteStoredScriptRequest(templateName);
                AcknowledgedResponse response = CLIENT.deleteScript(request, RequestOptions.DEFAULT);
                return response.isAcknowledged();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 将索引设为只读, 不再写入的索引设为只读后, 可以提升索引的读性能
         *
         * @param indices
         * @return boolean
         */
        public boolean setReadOnly(String... indices) {
            // 使用 ES 8.x ElasticsearchClient
            Map<String, Object> settings = new HashMap<>();
            settings.put("blocks.read_only", true);
            return IndicesRestSupport.updateIndexSettings(QUERY_CLIENT, indices, settings);
        }

        /**
         * 执行段合并, 可以先设为只读, 然后进行段合并
         *
         * @param indices
         * @return ForceMergeResponse
         */
        public ForceMergeResponse forceMerge(String indices) {
            return IndicesRestSupport.forceMerge(CLIENT, indices);
        }
    }

    /**
     * Elasticsearch Mapping 相关API
     */
    public static class Mappings {
        
        //private static String username = propertyReader.getString(USERNAME);
        //private static String password = propertyReader.getString(PASSWORD);
        
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
         * @param index
         * @return
         */
        public static Map<String, Object> getMapping(String index) {
            return IndicesRestSupport.getMapping(QUERY_CLIENT, index);
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
         * @param index
         * @param fields
         * @return Map<String, Object>
         */
        public static Map<String, Map<String, Object>> getMapping(String index, String... fields) {
            return IndicesRestSupport.getFieldMapping(CLIENT, index, fields);
        }

        /**
         * 设置索引的Mapping, index必须先创建, 可以为index增加字段定义, 但是不能删除已有的字段定义<p/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/mapping.html<br/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/dynamic-mapping.html<br/>
         * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/mapping-params.html
         *
         * @param index
         * @param dynamic
         * @return boolean Mapping创建成功失败标识
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
         * @param index  索引名
         * @param mapping mapping JSON串
         * @return
         */
        public static boolean putMapping(String index, String mapping) {
            int tryCount = 0;
            for (String host : RestSupport.HOSTS) {
                if (host.endsWith("/")) {
                    host = host.substring(0, host.length() - 1);
                }
                String result = "";
                try {
                    if (isBlank(username)) {
                        result = HttpUtils.put(host + "/" + index + "/_mapping")
                                .body(mapping)
                                .method(HttpMethod.PUT)
                                .request();
                    } else {
                        result = HttpUtils.put(host + "/" + index + "/_mapping")
                                .body(mapping)
                                .method(HttpMethod.PUT)
                                .basicAuth(username, password)
                                .request();
                        
                    }
                } catch (Exception e) {
                    log.error("", e);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new PutMappingException(e);
                    }
                    continue;
                }
                boolean hasError = JsonPathUtils.ifExists(result, "$.error");
                if (hasError) {
                    String errors = JsonPathUtils.readNode(result, "$.error.root_cause[0].reason");
                    log.error("PUT Mapping failed, host {}, [{}]", host, errors);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new PutMappingException(errors);
                    }
                }
                
                Boolean acknowledged = JsonPathUtils.readNode(result, "$.acknowledged");
                log.info("acknowledged: {}", acknowledged);
                return acknowledged;
            }
            return false;
        }
    }

    /**
     * Elasticsearch Settings 相关 API
     */
    public static class Settings {

        /**
         * 更新索引的Settings
         *
         * @param indices
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
         * @param index 索引名
         * @param settings settings JSON串
         * @return 是否设置成功
         */
        public static boolean putSettings(String index, String settings) {
            int tryCount = 0;
            for (String host : RestSupport.HOSTS) {
                if (host.endsWith("/")) {
                    host = host.substring(0, host.length() - 1);
                }
                String result = "";
                try {
                    if (isBlank(username)) {
                        result = HttpUtils.put(host + "/" + index + "/_settings")
                                .body(settings)
                                .method(HttpMethod.PUT)
                                .request();
                    } else {
                        result = HttpUtils.put(host + "/" + index + "/_settings")
                                .body(settings)
                                .method(HttpMethod.PUT)
                                .basicAuth(username, password)
                                .request();
                        
                    }
                } catch (Exception e) {
                    log.error("", e);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new PutSettingsException(e);
                    }
                    continue;
                }
                boolean hasError = JsonPathUtils.ifExists(result, "$.error");
                if (hasError) {
                    String errors = JsonPathUtils.readNode(result, "$.error.root_cause[0].reason");
                    log.error("PUT Settings failed, host {}, [{}]", host, errors);
                    if (++tryCount == RestSupport.HOSTS.size()) {
                        throw new PutSettingsException(errors);
                    }
                }
                
                Boolean acknowledged = JsonPathUtils.readNode(result, "$.acknowledged");
                log.info("acknowledged: {}", acknowledged);
                return acknowledged;
            }
            return false;
        }
    }

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
            ClusterHealthResponse response = IndicesRestSupport.clusterHealth(CLIENT);
            ClusterHealthStatus status = response.getStatus();
            return status.toString();
        }

        public static ClusterSettingBuilder settings() {
            return new ClusterSettingBuilder();
        }

        /**
         * 返回所有cluster setting, 包括persistent, transient
         * @return
         */
        public static Map<String, Object> allSettings() {
            String url = RestSupport.HOSTS.get(0) +"/_cluster/settings?include_defaults=true&flat_settings=true";
            String settings = HttpUtils.get(url).basicAuth(username, password)
                    .request();
            Map<String, Object> settingsMap = JacksonUtils.toMap(settings);
            return settingsMap;
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
         * @return
         */
        public static boolean createMultiFieldAgg() {
            String script = "{  \"script\": { " +
                    "\"lang\": \"painless\", " +
                    "\"source\": " +
                    "\"String fieldName = ''; " +
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
                    "return fieldName;\" }}";

            try {
                PutStoredScriptRequest request = new PutStoredScriptRequest();
                request.id("multi_fields");
                request.content(new BytesArray(script), XContentType.JSON);
                AcknowledgedResponse response = CLIENT.putScript(request, RequestOptions.DEFAULT);
                return response.isAcknowledged();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * <p>
     * Copyright: Copyright (c) 2021-04-28 18:21
     * <p>
     * Company: Sexy Uncle Inc.
     * <p>
     *
     * @author Rico Yu  ricoyu520@gmail.com
     * @version 1.0
     */
    public static final class Query {

        /**
         * 根据ID获取文档
         *
         * @param index
         * @param id
         * @return String
         */
        public static String byId(String index, Object id) {
            Objects.requireNonNull(index, "index cannot be null!");
            Objects.requireNonNull(id, "id cannot be null!");
            GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id.toString(), true);
            return response.getSourceAsString();
        }

        /**
         * 根据ID获取文档
         *
         * @param index
         * @param id
         * @return String
         */
        public static <T> T byId(String index, Object id, Class<T> resultType) {
            Objects.requireNonNull(index, "index cannot be null!");
            Objects.requireNonNull(id, "id cannot be null!");
            Objects.requireNonNull(resultType, "clazz cannot be null!");
            GetResponse response = DocumentRestSupport.get(QUERY_CLIENT, index, id.toString(), true);
            String source = response.getSourceAsString();
            T obj = toObject(source, resultType);
            Field idField = ElasticCacheUtils.idField(resultType);
            if (idField != null) {
                ReflectionUtils.setField(idField, obj, Transformers.convert(id, idField.getType()));
            }

            return obj;
        }

        /**
         * 基于ID列表获取
         *
         * @param indices
         * @return ElasticIdsQueryBuilder
         */
        public static ElasticIdsQueryBuilder idsQuery(String... indices) {
            return new ElasticIdsQueryBuilder(indices);
        }

        /**
         * 基于ID列表获取
         *
         * @param indices
         * @return ElasticIdsQueryBuilder
         */
        public static ElasticIdsQueryBuilder idsQuery(Collection<String> indices) {
            return new ElasticIdsQueryBuilder(indices.stream().toArray(String[]::new));
        }

        /**
         * 指定查询语句, 使用Query String Syntax<p>
         * 你可以就写查询条件: 2012<p>
         * 也可以写完成的查询: q=2012 或者 q=2012&df=title 等<p>
         * 有多种查询语法
         * <ul>
         * <li/>df查询                            GET movies/_search?q=2012&df=title
         * <li/>指定字段查询                       GET movies/_search?q=title:2012
         * <li/>泛查询                            GET movies/_search?q=2012              会对文档中所有字段进行查询
         * <li/>Term Query                       GET movies/_search?q=title:Beautiful Mind     与下面的等价
         * <li/>                                 GET movies/_search?q=title:Beautiful OR Mind
         * <li/>Pahrase Query(引号引起来的)        GET movies/_search?q=title:"Beautiful Mind"  表示Beautiful Mind要同时出现并且按照规定的顺序, 与下面的等价
         * <li/>                                  GET movies/_search?q=title:Beautiful AND Mind
         * <li/>分组                              GET movies/_search?q=title:(Beautiful Mind)
         * <li/>范围查询                          GET movies/_search?q=year:>1980
         * <li/>包含Beautiful 不包含 Mind          GET movies/_search?q=title:(Beautiful NOT Mind)
         * <li/>必须包含Mind, %2B是 + 号的转义字符  GET movies/_search?q=title:(Beautiful %2BMind)
         * </ul>
         *
         * @param index
         * @return QueryStringQueryBuilder
         */
        public static ElasticUriQueryBuilder uriQuery(String index) {
            return new ElasticUriQueryBuilder(index);
        }

        /**
         * 是Match Query的一种
         *
         * @param index
         * @return QueryStringBuilder
         */
        public static ElasticQueryStringBuilder queryString(String index) {
            return new ElasticQueryStringBuilder(index);
        }

        /**
         * Match Query是会对搜索的内容做分词后再去ES中查询的
         *
         * @param indices
         * @return ElasticMatchQueryBuilder
         */
        public static ElasticMatchQueryBuilder matchQuery(String... indices) {
            return new ElasticMatchQueryBuilder(indices);
        }

        /**
         * Match All Query
         *
         * @param indices
         * @return ElasticMatchAllQueryBuilder
         */
        public static ElasticMatchAllQueryBuilder matchAllQuery(String... indices) {
            return new ElasticMatchAllQueryBuilder(indices);
        }

        public static ElasticScrollQueryBuilder scrollQuery(String... indices) {
            return new ElasticScrollQueryBuilder(indices);
        }

        /**
         * 在ES中, Term查询, 对输入不做分词. 会将输入作为一个整体, 在倒排索引中查找准确的词项, 并且使用相关度计算公式为每个包含该此项的文档进行相关度算分<p>
         * <ol>
         * <li/> Term Query 不会对查询条件做分词
         * <li/> 如果被查询字段在文档里面是被分词的, 但是又想用Term Query对其做精确匹配, 那么可以使用Elasticsearch提供的多字段特性, 查询其keyword字段, 如productID.keyword
         * <li/> Term Query 会算分
         * <li/> 可以通过 Constant Score 将查询转换成一个 Filtering, 避免算分, 并利用缓存, 提高性能
         * <li/> Avoid using the term query for text fields.
         * </ol>
         *
         * @param indices
         * @return ElasticTermQueryBuilder
         */
        public static ElasticTermQueryBuilder termQuery(String... indices) {
            return new ElasticTermQueryBuilder(indices);
        }

        /**
         * 在ES中, Term查询, 对输入不做分词. 会将输入作为一个整体, 在倒排索引中查找准确的词项, 并且使用相关度计算公式为每个包含该此项的文档进行相关度算分<p>
         * <ol>
         * <li/> Term Query 不会对查询条件做分词
         * <li/> 如果被查询字段在文档里面是被分词的, 但是又想用Term Query对其做精确匹配, 那么可以使用Elasticsearch提供的多字段特性, 查询其keyword字段, 如productID.keyword
         * <li/> Term Query 会算分
         * <li/> 可以通过 Constant Score 将查询转换成一个 Filtering, 避免算分, 并利用缓存, 提高性能
         * <li/> Avoid using the term query for text fields.
         * </ol>
         *
         * @param indices
         * @return ElasticTermsQueryBuilder
         */
        public static ElasticTermsQueryBuilder termsQuery(String... indices) {
            return new ElasticTermsQueryBuilder(indices);
        }

        /**
         * Match Phrase Query查的是一个短语, 比如查title="one love", 那么title是"the one love"可以搜到, "one I love"搜不到
         * <p>
         * 在query里面的查询词必须是按照顺序出现的, slop 1表示King George之间可以插入一个其他的单词
         *
         * @param indices
         * @return
         */
        public static ElasticMatchPhraseQueryBuilder matchPhraseQuery(String... indices) {
            return new ElasticMatchPhraseQueryBuilder(indices);
        }

        /**
         * Match Phrase Query查的是一个短语, 比如查title="one love", 那么title是"the one love"可以搜到, "one I love"搜不到
         * <p>
         * 在query里面的查询词必须是按照顺序出现的, slop 1表示King George之间可以插入一个其他的单词
         *
         * @param indices
         * @return
         */
        public static ElasticMatchPhrasePrefixQueryBuilder matchPhrasePrefixQuery(String... indices) {
            return new ElasticMatchPhrasePrefixQueryBuilder(indices);
        }

        /**
         * geo_distance查询
         *
         * @param indices
         * @return ElasticExistsQueryBuilder
         */
        public static ElasticGeoDistanceQueryBuilder geoDistance(String... indices) {
            return new ElasticGeoDistanceQueryBuilder(indices);
        }

        /**
         * exists Query
         *
         * @param indices
         * @return ElasticExistsQueryBuilder
         */
        public static ElasticExistsQueryBuilder exists(String... indices) {
            return new ElasticExistsQueryBuilder(indices);
        }

        /**
         * prefix Query
         *
         * @param indices
         * @return ElasticPrefixQueryBuilder
         */
        public static ElasticPrefixQueryBuilder prefix(String... indices) {
            return new ElasticPrefixQueryBuilder(indices);
        }

        /**
         * Range Query, 支持日期, 数字类型
         *
         * @param indices
         * @return ElasticRangeQueryBuilder
         */
        public static ElasticRangeQueryBuilder range(String... indices) {
            return new ElasticRangeQueryBuilder(indices);
        }

        /**
         * 通用查询接口, 既可以基于值查询, 如: Term Query, Match Query, Query string, Simple query string
         * 可以基于字段存在性查询, 比如 Exists Query
         * 给ElasticQueryBuilder传入不同的 QueryBuilder即可
         *
         * @param indices
         * @return ElasticQueryBuilder
         */
        public static ElasticQueryBuilder query(String... indices) {
            return ElasticQueryBuilder.instance(indices);
        }

        /**
         * Multi Match Query 跨字段搜索
         *
         * <pre> {@code
         * POST blogs/_search
         * {
         *   "query": {
         *     "multi_match": {
         *       "query": "Quick pets",
         *       "type": "best_fields",
         *       "fields": ["title", "body"],
         *       "tie_breaker": 0.2,
         *       "minimum_should_match": "20%"
         *     }
         *   }
         * }
         * }</pre>
         *
         * <ul>
         *     <li/>multi_match 声明这是一个Multi Match Query
         *     <li/>query       提供一个查询的语句
         *     <li/>fields      查询语句要匹配到哪些字段上
         *     <li/>type        best_fields 默认类型, 可以不指定. 表示会在fields指定的字段中取一个评分最高的作为一个返回结果
         * </ul>
         *
         * @param indices 要查询的索引
         */
        public static ElasticMultiMatchQueryBuilder multiMatch(String... indices) {
            return new ElasticMultiMatchQueryBuilder(indices);
        }

        /**
         * 布尔/组合 查询
         *
         * @param indices
         * @return ElasticBoolQueryBuilder
         */
        public static ElasticBoolQueryBuilder bool(String... indices) {
            return new ElasticBoolQueryBuilder(indices);
        }

        /**
         * Search Template Query
         *
         * @param indices
         * @return ElasticTemplateQueryBuilder
         */
        public static ElasticTemplateQueryBuilder templateQuery(String... indices) {
            return new ElasticTemplateQueryBuilder(indices);
        }
    }

    /**
     * 聚合查询相关API<p/>
     * 聚合分两大类
     * <ol>
     *     <li/>Bucket 聚合<br/>
     *          按照一定的规则, 将文档分配到不同的桶中, 从而达到分类的目的. ES提供了一些常见的Bucket Aggregation
     *          <ul>
     *              <li/>Terms
     *              <li/>Range / Date Range
     *              <li/>Histogram / Date Histogram
     *              <li/>支持嵌套, 也就是在桶里再做分桶
     *          </ul>
     *     <li/>Metric 聚合<br/>
     *          主要是对数据做一些统计分析, 分为两大类
     *          <ul>
     *              <li/>单值分析: 只输出一个统计结果
     *              <ul>
     *                  <li/>min max avg sum
     *                  <li/>Cardinality(类似distinct count)
     *              </ul>
     *              <li/>多值分析:输出多个分析结果
     *              <ul>
     *                  <li/>stats, extended stats
     *                  <li/>percentile, percentile rank
     *                  <li/>top_hits(排在前面的示例)
     *              </ul>
     *          </ul>
     * </ol>
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
         * @return ElasticTermsAggregationBuilder
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
         * @return ElasticMaxAggregationBuilder
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
         * @return ElasticSumAggregationBuilder
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

    /**
     * ES 8.x 原生 API 聚合入口 (使用 co.elastic.clients)
     */
    public static class AggsV8 {

        /**
         * Terms 聚合 (ES 8.x 原生 API)
         *
         * @param indices 索引名称
         * @return V8TermsAggregationBuilder
         */
        public static V8TermsAggregationBuilder terms(String... indices) {
            return V8TermsAggregationBuilder.instance(indices);
        }

        /**
         * Multi Terms 聚合 (ES 8.x 原生 API)
         * 基于多个字段的组合来计算分桶
         *
         * @param indices 索引名称
         * @return V8MultiTermsAggregationBuilder
         */
        public static com.awesomecopilot.search8x.builder.agg.v8.V8MultiTermsAggregationBuilder multiTerms(String... indices) {
            return com.awesomecopilot.search8x.builder.agg.v8.V8MultiTermsAggregationBuilder.instance(indices);
        }

        /**
         * Range 聚合 (ES 8.x 原生 API)
         * 基于数值范围进行分桶，支持自定义范围和边界
         *
         * @param indices 索引名称
         * @return V8RangeAggregationBuilder
         */
        public static V8RangeAggregationBuilder range(String... indices) {
            return V8RangeAggregationBuilder.instance(indices);
        }

        /**
         * Histogram 聚合 (ES 8.x 原生 API)
         * 基于数值字段按固定间隔创建分桶，适用于统计分布分析
         *
         * @param indices 索引名称
         * @return V8HistogramAggregationBuilder
         */
        public static V8HistogramAggregationBuilder histogram(String... indices) {
            return V8HistogramAggregationBuilder.instance(indices);
        }

        /**
         * Date Histogram 聚合 (ES 8.x 原生 API)
         * 基于日期字段按时间间隔创建分桶，支持日历间隔和固定间隔，适用于时间序列分析
         *
         * @param indices 索引名称
         * @return V8DateHistogramAggregationBuilder
         */
        public static V8DateHistogramAggregationBuilder dateHistogram(String... indices) {
            return V8DateHistogramAggregationBuilder.instance(indices);
        }

        /**
         * Min 聚合 (ES 8.x 原生 API)
         * 计算数值字段的最小值，适用于价格、年龄等数值字段的统计分析
         *
         * @param indices 索引名称
         * @return V8MinAggregationBuilder
         */
        public static V8MinAggregationBuilder min(String... indices) {
            return V8MinAggregationBuilder.instance(indices);
        }
    }


    protected static void logDsl(SearchSourceBuilder builder) {
        if (log.isDebugEnabled()) {
            log.debug("Query DSL:\n{}", new JSONObject(builder.toString()).toString(2));
        }
    }

    protected static void logDsl(UpdateRequest builder) {
        if (log.isDebugEnabled()) {
            log.debug("Update DSL:\n{}", new JSONObject(builder.toString()).toString(2));
        }
    }
}