package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scroll Query Builder for ES 8.x
 * <p>
 * 用于深度遍历大量数据, 使用scroll上下文保持查询结果一致性
 * <p>
 * <b>上下文释放(评审报告 P0-8)</b>: scroll 上下文是服务端资源, 不主动 clear 只能等
 * scrollTime 到期回收; 全集群打开的上下文数量堆到 search.max_open_scroll_context
 * (默认 500)后, 新的 scroll 请求会全部被拒。本类的释放策略:
 * <ol>
 * <li/>遍历拿到空批次(数据读完)时自动 clear;</li>
 * <li/>queryForList 通信失败(IOException)或 ES 报错(ElasticsearchException)时自动 clear, 不遗留;
 *     结果解析异常不 clear, 上下文保留, 调用方可 catch 后重试继续遍历;</li>
 * <li/>轮转中途不 clear 旧 scrollId(新旧 id 指向同一上下文, 清了会打断遍历, 见 doScroll 注释),
 *     只记录, 在结束/异常/close 时逐条 clear;</li>
 * <li/>提前放弃遍历(如中途 return/break)时, 必须用 try-with-resources 包住本 builder
 *     或手动 close(), 否则上下文仍会保留到 scrollTime 到期; close() 幂等, 与①②叠加调用无害。</li>
 * </ol>
 * close() 之后 scrollId 置空, 同一个 builder 再次 queryForList 会发起新的初始查询。
 * 本类非线程安全, 不要跨线程共享同一 builder 实例。
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticScrollQueryBuilder extends BaseQueryBuilder implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ElasticScrollQueryBuilder.class);

    /**
     * scroll上下文保持时间, 默认5分钟
     */
    private String scrollTime = "5m";

    /**
     * 上一次scroll返回的scrollId, 用于获取下一批数据
     */
    private String scrollId;

    /**
     * 本 builder 生命周期内从服务端拿到过的全部 scrollId(评审报告 P0-8):
     * 轮转产生的旧 id 在 close() 时一并 clear, 保证任何退出路径都不遗留
     */
    private final Set<String> seenScrollIds = new LinkedHashSet<>();

    public ElasticScrollQueryBuilder(String... indices) {
        super(indices);
    }

    /**
     * 设置scrollId, 用于获取下一批scroll结果
     *
     * @param scrollId 上一次scroll返回的scrollId
     * @return ElasticScrollQueryBuilder
     */
    public ElasticScrollQueryBuilder scrollId(String scrollId) {
        this.scrollId = scrollId;
        track(scrollId);
        return this;
    }

    /**
     * 设置scroll上下文保持时间, 默认"5m"
     *
     * @param scrollTime 如 "5m", "1m" 等
     * @return ElasticScrollQueryBuilder
     */
    public ElasticScrollQueryBuilder scrollTime(String scrollTime) {
        this.scrollTime = scrollTime;
        return this;
    }

    public ElasticScrollQueryBuilder resultType(Class resultType) {
        this.resultType = resultType;
        return this;
    }

    public ElasticScrollQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticScrollQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    /**
     * 返回当前scrollId, 用于下一次scroll请求
     *
     * @return scrollId
     */
    public String getScrollId() {
        return this.scrollId;
    }

    @Override
    protected Query buildQuery() {
        return null;
    }

    /**
     * 执行scroll查询, 返回结果列表
     * 如果有scrollId, 则使用scroll API获取下一批数据
     * 如果没有scrollId, 则执行初始查询
     * <p>
     * 异常与上下文的关系(评审报告 P0-8, 独立评审发现 1): 通信层失败(IOException)与
     * ES 返回错误(ElasticsearchException)时先释放上下文再抛出; 结果解析阶段的运行时异常
     * (如 resultType 配置错误导致反序列化失败)不释放——此时响应已取回、上下文已轮转到下一批,
     * 调用方 catch 后重试即可跳过坏批继续遍历(与修复前行为一致)。
     *
     * @param <T> 结果类型
     * @return List<T>
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList() {
        try {
            if (scrollId != null) {
                return doScroll();
            }
            return doInitialSearch();
        } catch (IOException e) {
            // 通信层失败: 服务端上下文不能再续用, 就地释放
            close();
            throw new RuntimeException("Scroll query failed", e);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            // ES 明确报错(如失效 scrollId、too many scroll contexts): 同样释放不遗留
            close();
            throw e;
        }
    }

    private <T> List<T> doInitialSearch() throws IOException {
        // 基类约定: 外部传入的 Query 优先(与其他 builder 一致, 修前漏了这层导致
        // queryBuilder(Query) 设的条件不参与初始查询)
        Query query = this.externalQuery != null ? this.externalQuery : buildQuery();

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        if (indices != null && indices.length > 0) {
            requestBuilder.index(java.util.Arrays.asList(indices));
        }
        if (query != null) {
            requestBuilder.query(query);
        }
        if (size != null) {
            requestBuilder.size(size);
        }

        // source filtering
        if (includeSource != null && includeSource.length > 0) {
            requestBuilder.source(src -> src.filter(f -> f.includes(java.util.Arrays.asList(includeSource))));
        } else if (excludeSource != null && excludeSource.length > 0) {
            requestBuilder.source(src -> src.filter(f -> f.excludes(java.util.Arrays.asList(excludeSource))));
        }

        final String st = this.scrollTime;
        SearchRequest request = requestBuilder.scroll(s -> s.time(st)).build();

        if (log.isDebugEnabled()) {
            log.debug("Scroll initial query DSL:\n{}", request.toString());
        }

        SearchResponse<Map> response = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
        this.scrollId = response.scrollId();
        track(this.scrollId);

        List<Map<String, Object>> sources = extractSources(response.hits().hits());
        if (sources.isEmpty()) {
            // 无命中也会创建上下文(P0-8): 批次空即结束, 就地释放
            close();
        }
        return convert(sources);
    }

    private <T> List<T> doScroll() throws IOException {
        final String sid = this.scrollId;
        final String st = this.scrollTime;

        co.elastic.clients.elasticsearch.core.ScrollRequest scrollRequest =
                co.elastic.clients.elasticsearch.core.ScrollRequest.of(
                        s -> s.scrollId(sid).scroll(t -> t.time(st))
                );

        ScrollResponse<Map> response = ElasticUtils.QUERY_CLIENT.scroll(scrollRequest, Map.class);
        this.scrollId = response.scrollId();
        track(this.scrollId);
        // 注意: 轮转后的新 id 与旧 id 指向同一个服务端上下文, 中途 clear 旧 id 会
        // 把当前上下文一并释放(实测下一轮报 all shards failed), 所以只记录、遍历结束再清

        List<Map<String, Object>> sources = extractSources(response.hits().hits());
        if (sources.isEmpty()) {
            close();
        }
        return convert(sources);
    }

    private List<Map<String, Object>> extractSources(List<Hit<Map>> hits) {
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Hit<Map> hit : hits) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                sources.add(source);
            }
        }
        return sources;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> convert(List<Map<String, Object>> sources) {
        List<T> results = new ArrayList<>();
        for (Map<String, Object> source : sources) {
            if (resultType == null || resultType == Object.class || resultType == String.class) {
                results.add((T) JacksonUtils.toJson(source));
            } else {
                results.add((T) JacksonUtils.toObject(JacksonUtils.toJson(source), resultType));
            }
        }
        return results;
    }

    private void track(String scrollId) {
        if (scrollId != null && !scrollId.isEmpty()) {
            seenScrollIds.add(scrollId);
        }
    }

    /**
     * 释放本 builder 持有的全部 scroll 上下文(评审报告 P0-8)。
     * <p>
     * 幂等: 空批次/异常路径内部已自动调用过, try-with-resources 的 close() 再调一次无害;
     * close() 之后 scrollId 置空, 同一 builder 再次 queryForList 会发起新的初始查询。
     * 释放失败只告警不抛出——遍历结果已到手, 服务端上下文最迟在 scrollTime 到期后回收。
     */
    @Override
    public void close() {
        track(this.scrollId);
        if (seenScrollIds.isEmpty()) {
            return;
        }
        scrollId = null;
        // 逐条 clear: 批量请求里混进一个坏 id(如调用方手动 scrollId() 设过的)会
        // 让整次请求解析失败, 连累正常 id 释放不掉
        List<String> failed = new ArrayList<>();
        for (String id : new ArrayList<>(seenScrollIds)) {
            try {
                ElasticUtils.QUERY_CLIENT.clearScroll(ClearScrollRequest.of(b -> b.scrollId(id)));
                seenScrollIds.remove(id);
            } catch (Exception e) {
                failed.add(id + " -> " + e.getMessage());
            }
        }
        // 失败的 id 留在集合里, 下次 close() 还能重试; 最迟 scrollTime 后由服务端到期回收
        if (!failed.isEmpty()) {
            log.warn("clearScroll 释放 {} 个 scroll 上下文失败(最迟 {} 后由服务端到期回收): {}",
                    failed.size(), scrollTime, failed);
        }
    }
}
