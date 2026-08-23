package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * ES 8.x 查询构建器基类
 * <p>
 * 提供公共的查询功能: 分页, 排序, source过滤, 结果转换等
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class BaseQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(BaseQueryBuilder.class);

    /**
     * 要查询的索引
     */
    protected String[] indices;

    /**
     * 要查询的字段
     */
    protected String field;

    /**
     * 要查询的值
     */
    protected Object value;

    /**
     * 是否要获取_source
     */
    protected boolean fetchSource = true;

    /**
     * 设置 _source 属性, 即指定要返回哪些字段
     */
    protected String[] includeSource;

    /**
     * 指定查询要排除哪些字段
     */
    protected String[] excludeSource;

    /**
     * 分页相关, 起始位置
     */
    protected Integer from;

    /**
     * 分页相关, 每页大小
     */
    protected Integer size;

    /**
     * 排序: 每个元素格式为 "field:asc" 或 "field:desc"
     */
    protected List<String> sortClauses = new ArrayList<>();

    /**
     * 提升或者降低查询的权重
     */
    protected float boost = 1f;

    /**
     * 查询返回的结果类型
     */
    protected Class resultType;

    /**
     * 是否要将Query转为constant_score query, 以避免算分, 提高查询性能
     */
    protected boolean constantScore = false;

    /**
     * 避免深度分页, search_after
     */
    protected Object[] searchAfter;

    /**
     * 外部传入的Query对象
     * <p>
     * 允许用户直接传入已构建好的Query对象, 而不是通过子类实现buildQuery()
     */
    protected Query externalQuery;

    /**
     * Function Score Query 的 score functions
     * <p>
     * 用于在查询结束后对每个匹配的文档进行重新算分
     */
    protected List<FunctionScore> scoreFunctions = new ArrayList<>();

    /**
     * Function Score Query 的 boost_mode
     * <p>
     * 默认值: multiply (算分与函数值的乘积)
     */
    protected String boostMode = "multiply";

    /**
     * 高亮字段列表
     */
    protected List<String> highlightFieldNames = new ArrayList<>();

    /**
     * 高亮前缀标签, 如 "<em style='color:red'>"
     */
    protected String highlightPreTag;

    /**
     * 高亮后缀标签, 如 "</em>"
     */
    protected String highlightPostTag;

    /**
     * 子类实现此方法来构建ES 8.x Query对象
     *
     * @return Query
     */
    protected abstract Query buildQuery();

    protected BaseQueryBuilder() {
    }

    protected BaseQueryBuilder(String... indices) {
        if (indices != null) {
            this.indices = indices;
        }
    }

    /**
     * 设置分页属性
     */
    public <T extends BaseQueryBuilder> T paging(Integer from, Integer size) {
        this.from = from;
        this.size = size;
        return (T) this;
    }

    /**
     * 设置每页大小
     */
    public <T extends BaseQueryBuilder> T size(int size) {
        this.size = size;
        return (T) this;
    }

    /**
     * 设置起始位置
     */
    public <T extends BaseQueryBuilder> T from(int from) {
        this.from = from;
        return (T) this;
    }

    /**
     * 添加排序规则
     * sort格式: 字段1:asc,字段2:desc,字段3
     */
    public <T extends BaseQueryBuilder> T sort(String sort) {
        if (isNotBlank(sort)) {
            String[] parts = sort.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (isNotBlank(trimmed)) {
                    sortClauses.add(trimmed);
                }
            }
        }
        return (T) this;
    }

    /**
     * 是否要获取_source
     */
    public <T extends BaseQueryBuilder> T fetchSource(boolean fetchSource) {
        this.fetchSource = fetchSource;
        return (T) this;
    }

    /**
     * 控制返回自己想要的字段
     */
    public <T extends BaseQueryBuilder> T includeSources(String... fields) {
        this.includeSource = fields;
        return (T) this;
    }

    /**
     * 控制要排除哪些返回的字段
     */
    public <T extends BaseQueryBuilder> T excludeSources(String... fields) {
        this.excludeSource = fields;
        return (T) this;
    }

    /**
     * 设置结果类型
     */
    public <T extends BaseQueryBuilder> T resultType(Class resultType) {
        this.resultType = resultType;
        return (T) this;
    }

    /**
     * 避免深度分页, search_after
     */
    public <T extends BaseQueryBuilder> T searchAfter(Object[] searchAfter) {
        this.searchAfter = searchAfter;
        return (T) this;
    }

    /**
     * 是否要将Query转为constant_score query, 以避免算分, 提高查询性能
     * <p>
     * 即便是对Keyword 进行 Term 查询, 同样会被算分
     * 可以将查询转为 Filtering, 取消相关性算分的环节, 以提升性能
     * filter可以有效利用缓存
     *
     * @param constantScore 是否启用constant_score
     * @return T
     */
    public <T extends BaseQueryBuilder> T constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return (T) this;
    }

    /**
     * 设置高亮字段
     * <p>
     * 对应 Query DSL:
     * <pre>
     * "highlight": {
     *     "fields": {
     *         "goods_name": {},
     *         "goods_desc": {}
     *     }
     * }
     * </pre>
     *
     * @param fields 需要高亮的字段名
     * @return T
     */
    public <T extends BaseQueryBuilder> T highlightFields(String... fields) {
        if (fields != null && fields.length > 0) {
            Collections.addAll(this.highlightFieldNames, fields);
        }
        return (T) this;
    }

    /**
     * 设置高亮标签
     * <p>
     * 对应 Query DSL:
     * <pre>
     * "highlight": {
     *     "pre_tags": ["&lt;em style='color:red'&gt;"],
     *     "post_tags": ["&lt;/em&gt;"]
     * }
     * </pre>
     *
     * @param preTag  高亮前缀标签
     * @param postTag 高亮后缀标签
     * @return T
     */
    public <T extends BaseQueryBuilder> T highlightTags(String preTag, String postTag) {
        this.highlightPreTag = preTag;
        this.highlightPostTag = postTag;
        return (T) this;
    }

    /**
     * 设置外部传入的Query对象
     * <p>
     * 允许用户直接传入已构建好的Query对象, 而不是通过子类实现buildQuery()
     * 这种方式可以复用已有的Query构建逻辑
     *
     * @param query ES 8.x Query对象
     * @return T
     */
    public <T extends BaseQueryBuilder> T queryBuilder(Query query) {
        this.externalQuery = query;
        return (T) this;
    }

    /**
     * 设置外部传入的QueryBuilder对象
     * <p>
     * 允许用户传入另一个QueryBuilder实例, 会自动调用其buildQuery()获取底层Query对象
     * 这种方式可以组合多个查询构建器
     *
     * @param builder 另一个QueryBuilder实例
     * @return T
     */
    public <T extends BaseQueryBuilder> T queryBuilder(BaseQueryBuilder builder) {
        if (builder != null) {
            this.externalQuery = builder.buildQuery();
        }
        return (T) this;
    }

    /**
     * 添加 Function Score Query 的 score function
     * <p>
     * 用于在查询结束后对每个匹配的文档进行重新算分
     *
     * @param scoreFunction ES 8.x FunctionScore
     * @return T
     */
    public <T extends BaseQueryBuilder> T addScoreFunction(FunctionScore scoreFunction) {
        if (scoreFunction != null) {
            this.scoreFunctions.add(scoreFunction);
        }
        return (T) this;
    }

    /**
     * 设置 Function Score Query 的 boost_mode
     * <p>
     * 可用的值: multiply(默认), sum, min, max, replace
     *
     * @param boostMode boost模式
     * @return T
     */
    public <T extends BaseQueryBuilder> T boostMode(String boostMode) {
        if (boostMode != null && !boostMode.isEmpty()) {
            this.boostMode = boostMode.toLowerCase();
        }
        return (T) this;
    }

    /**
     * 执行查询, 返回结果列表, 如果设置了高亮字段则将高亮片段合并进 source
     */
    public <T> List<T> queryForList() {
        SearchResponse<Map> response = doSearch();
        List<Hit<Map>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return emptyList();
        }

        List<T> results = new ArrayList<>();
        for (Hit<Map> hit : hits) {
            Map<String, Object> source = hit.source();
            if (source == null) {
                continue;
            } else {
                source.put("_id", hit.id());
            }
            // 将高亮片段合并到 source 中
            mergeHighlight(source, hit);
            if (resultType == null || resultType == Object.class || resultType == String.class) {
                results.add((T) JacksonUtils.toJson(source));
            } else {
                results.add((T) JacksonUtils.mapToPojo(source, resultType));
            }
        }
        return results;
    }

    /**
     * 执行查询, 返回一条记录
     */
    public <T> T queryForOne() {
        SearchResponse<Map> response = doSearch();
        List<Hit<Map>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return null;
        }

        Hit<Map> hit = hits.get(0);
        Map<String, Object> source = hit.source();
        if (source == null) {
            return null;
        }

        if (resultType != null && resultType == Map.class) {
            return (T) source;
        }

        if (resultType == null || resultType == Object.class || resultType == String.class) {
            return (T) JacksonUtils.toJson(source);
        }

        return (T) JacksonUtils.toObject(JacksonUtils.toJson(source), resultType);
    }

    /**
     * 执行查询, 返回一条记录（与queryForOne相同）
     */
    public <T> T queryForObject() {
        return queryForOne();
    }

    /**
     * 返回查询到的记录数
     */
    public long queryForCount() {
        SearchResponse<Map> response = doSearch();
        TotalHits totalHits = response.hits().total();
        if (totalHits == null) {
            return 0L;
        }
        return totalHits.value();
    }

    /**
     * 执行查询, 返回分页结果
     */
    public <T> com.awesomecopilot.search8x.vo.ElasticPage<T> queryForPage() {
        SearchResponse<Map> response = doSearch();
        TotalHits totalHits = response.hits().total();
        List<Hit<Map>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return com.awesomecopilot.search8x.vo.ElasticPage.emptyResult();
        }

        List<T> results = new ArrayList<>();
        for (Hit<Map> hit : hits) {
            Map<String, Object> source = hit.source();
            if (source == null) continue;
            if (resultType == null || resultType == Object.class || resultType == String.class) {
                results.add((T) JacksonUtils.toJson(source));
            } else {
                results.add((T) JacksonUtils.toObject(JacksonUtils.toJson(source), resultType));
            }
        }

        // 取最后一个hit的sort值, 提取FieldValue内部的原始值
        Object[] sortValues = null;
        Hit<Map> lastHit = hits.get(hits.size() - 1);
        if (lastHit.sort() != null && !lastHit.sort().isEmpty()) {
            sortValues = lastHit.sort().stream()
                    .map(fv -> fv == null ? null : fv._get())
                    .toArray();
        }

        com.awesomecopilot.search8x.vo.ElasticPage<T> page = new com.awesomecopilot.search8x.vo.ElasticPage<>();
        page.setResults(results);
        page.setSort(sortValues);
        if (totalHits != null) {
            page.setTotalCount((int) totalHits.value());
        }
        return page;
    }

    /**
     * 执行Search
     */
    protected SearchResponse<Map> doSearch() {
        // 优先使用外部传入的Query, 否则调用子类实现的buildQuery()
        Query query = this.externalQuery != null ? this.externalQuery : buildQuery();

        // constant_score 包装
        if (this.constantScore && query != null) {
            final Query innerQuery = query;
            query = Query.of(q -> q.constantScore(cs -> cs.filter(innerQuery)));
        }

        // function_score 包装
        if (!this.scoreFunctions.isEmpty() && query != null) {
            final Query innerQuery = query;
            final List<FunctionScore> functions = this.scoreFunctions;
            final String mode = this.boostMode;
            
            query = Query.of(q -> q.functionScore(fs -> {
                fs.query(innerQuery);
                // 添加所有的 score functions
                for (FunctionScore func : functions) {
                    fs.functions(func);
                }
                // 设置 boost_mode
                switch (mode) {
                    case "sum":
                        fs.boostMode(Sum);
                        break;
                    case "min":
                        fs.boostMode(Min);
                        break;
                    case "max":
                        fs.boostMode(Max);
                        break;
                    case "replace":
                        fs.boostMode(Replace);
                        break;
                    default: // multiply
                        fs.boostMode(Multiply);
                        break;
                }
                return fs;
            }));
        }

        try {
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder();

            // 设置索引
            if (indices != null && indices.length > 0) {
                List<String> indexList = new ArrayList<>();
                for (String index : indices) {
                    indexList.add(index);
                }
                requestBuilder.index(indexList);
            }

            // 设置查询
            if (query != null) {
                requestBuilder.query(query);
            }

            // 分页
            if (from != null) {
                requestBuilder.from(from);
            }
            if (size != null) {
                requestBuilder.size(size);
            }

            // 排序
            if (!sortClauses.isEmpty()) {
                for (String sortClause : sortClauses) {
                    String[] parts = sortClause.split(":");
                    String fieldName = parts[0].trim();
                    SortOrder order = SortOrder.Asc;
                    if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                        order = SortOrder.Desc;
                    }
                    final SortOrder sortOrder = order;
                    requestBuilder.sort(s -> s.field(f -> f.field(fieldName).order(sortOrder)));
                }
            }

            // search_after
            if (searchAfter != null && searchAfter.length > 0) {
                if (sortClauses.isEmpty()) {
                    throw new IllegalArgumentException("search_after requires at least one sort clause. " +
                            "Please add .sort(\"field:order\") before calling queryForPage().");
                }
                List<FieldValue> sortValues = new ArrayList<>();
                for (Object sv : searchAfter) {
                    if (sv == null) {
                        sortValues.add(FieldValue.NULL);
                    } else if (sv instanceof FieldValue) {
                        sortValues.add((FieldValue) sv);
                    } else if (sv instanceof String) {
                        sortValues.add(FieldValue.of((String) sv));
                    } else if (sv instanceof Number) {
                        sortValues.add(FieldValue.of(((Number) sv).doubleValue()));
                    } else if (sv instanceof Boolean) {
                        sortValues.add(FieldValue.of((Boolean) sv));
                    } else {
                        sortValues.add(FieldValue.of(sv.toString()));
                    }
                }
                requestBuilder.searchAfter(sortValues);
            }

            // ES 中 excludes 优先级高于 includes，同名字段会被排除，如果同时设置最终 _source 为空, 与Query DSL语义保持一致
            if ((includeSource != null && includeSource.length > 0)
                    || (excludeSource != null && excludeSource.length > 0)) {
                requestBuilder.source(src -> src.filter(f -> {
                    if (includeSource != null && includeSource.length > 0) {
                        f.includes(asList(includeSource));
                    }
                    if (excludeSource != null && excludeSource.length > 0) {
                        f.excludes(asList(excludeSource));
                    }
                    return f;
                }));
            } else if (!fetchSource) {
                requestBuilder.source(src -> src.filter(f -> f.includes(emptyList())));
            }

            // highlight
            if (!highlightFieldNames.isEmpty()) {
                final String preTag = highlightPreTag;
                final String postTag = highlightPostTag;
                requestBuilder.highlight(Highlight.of(h -> {
                    for (String fieldName : highlightFieldNames) {
                        h.fields(fieldName, HighlightField.of(hf -> hf));
                    }
                    if (preTag != null && postTag != null) {
                        h.preTags(preTag);
                        h.postTags(postTag);
                    }
                    return h;
                }));
            }

            SearchRequest request = requestBuilder.build();

            if (log.isDebugEnabled()) {
                log.debug("Query DSL:\n{}", request.toString());
            }

            return ElasticUtils.QUERY_CLIENT.search(request, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    /**
     * 将 Hit 中的高亮片段合并到 source Map 中
     * <p>
     * 高亮结果以 "_highlight" 为 key 存入 source, 值为 Map&lt;String, String&gt;,
     * 其中 key 是字段名, value 是拼接后的高亮片段
     */
    private void mergeHighlight(Map<String, Object> source, Hit<Map> hit) {
        Map<String, List<String>> highlight = hit.highlight();
        if (highlight == null || highlight.isEmpty()) {
            return;
        }
        Map<String, String> highlightMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : highlight.entrySet()) {
            highlightMap.put(entry.getKey(), String.join("", entry.getValue()));
        }
        source.put("_highlight", highlightMap);
    }

    protected static void notNull(Object obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }
    }
}
