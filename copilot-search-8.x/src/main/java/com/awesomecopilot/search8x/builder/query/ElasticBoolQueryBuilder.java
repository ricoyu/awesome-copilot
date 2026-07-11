package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.awesomecopilot.search8x.enums.BoolQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.awesomecopilot.search8x.enums.BoolQueryType.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Bool Query Builder for ES 8.x
 * <p>
 * 布尔/组合 查询, 支持链式调用:
 * <pre>
 * ElasticUtils.Query.bool("index")
 *     .term("field1", "value1").must()
 *     .match("field2", "value2").should()
 *     .range("field3").gte(10).lte(20).filter()
 *     .queryForList();
 * </pre>
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticBoolQueryBuilder extends BaseQueryBuilder {

    private String nestedPath;
    private Object minimumShouldMatch;
    private List<Node> queries = new ArrayList<>();
    
    /**
     * 临时的 nestedPath，用于传递给下一个创建的子查询
     */
    private String pendingNestedPath;

    private static class Node {
        BoolQueryType type;
        Query query;
        /** 标记此节点是否属于嵌套组（在 pendingNestedPath 设置后添加） */
        boolean nestedGroup;

        Node(BoolQueryType type, Query query) {
            this(type, query, false);
        }

        Node(BoolQueryType type, Query query, boolean nestedGroup) {
            this.type = type;
            this.query = query;
            this.nestedGroup = nestedGroup;
        }
    }

    public ElasticBoolQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticBoolQueryBuilder nestedPath(String path) {
        // 如果还没有任何子查询，则设置整体的 nestedPath
        // 如果已经有子查询，则设置 pendingNestedPath 给下一个子查询
        if (queries.isEmpty()) {
            this.nestedPath = path;
        } else {
            this.pendingNestedPath = path;
        }
        return this;
    }

    /**
     * 添加 must 条件
     */
    public ElasticBoolQueryBuilder must(Query query) {
        this.queries.add(new Node(MUST, query, isNotBlank(pendingNestedPath)));
        return this;
    }

    /**
     * 添加 must_not 条件
     */
    public ElasticBoolQueryBuilder mustNot(Query query) {
        this.queries.add(new Node(MUST_NOT, query, isNotBlank(pendingNestedPath)));
        return this;
    }

    /**
     * 添加 should 条件
     */
    public ElasticBoolQueryBuilder should(Query query) {
        this.queries.add(new Node(SHOULD, query, isNotBlank(pendingNestedPath)));
        return this;
    }

    /**
     * 添加 filter 条件
     */
    public ElasticBoolQueryBuilder filter(Query query) {
        this.queries.add(new Node(FILTER, query, isNotBlank(pendingNestedPath)));
        return this;
    }

    /**
     * 创建 term 查询并返回 BoolTermQuery 接口
     */
    public BoolTermQuery term(String field, Object value) {
        ElasticTermQueryBuilder termBuilder = new ElasticTermQueryBuilder();
        termBuilder.query(field, value);
        termBuilder.setBoolQueryBuilder(this);
        return termBuilder;
    }

    /**
     * 创建 terms 查询
     */
    public com.awesomecopilot.search8x.builder.query.BoolQuery terms(String field, Object... values) {
        if (values != null && values.length == 1 && values[0] instanceof Collection) {
            values = ((Collection) values[0]).stream().toArray(Object[]::new);
        }
        ElasticTermsQueryBuilder termsBuilder = new ElasticTermsQueryBuilder();
        termsBuilder.query(field, values);
        termsBuilder.setBoolQueryBuilder(this);
        return termsBuilder;
    }

    /**
     * 创建 match 查询并返回 BoolMatchQuery 接口
     */
    public BoolMatchQuery match(String field, String value) {
        ElasticMatchQueryBuilder matchBuilder = new ElasticMatchQueryBuilder();
        matchBuilder.query(field, value);
        matchBuilder.setBoolQueryBuilder(this);
        return matchBuilder;
    }

    /**
     * 创建 range 查询并返回 BoolRangeQuery 接口
     */
    public BoolRangeQuery range(String field) {
        ElasticRangeQueryBuilder rangeBuilder = new ElasticRangeQueryBuilder();
        rangeBuilder.field(field);
        rangeBuilder.setBoolQueryBuilder(this);
        return rangeBuilder;
    }

    /**
     * 创建 exists 查询
     */
    public com.awesomecopilot.search8x.builder.query.BoolQuery exists(String field) {
        ElasticExistsQueryBuilder existsBuilder = new ElasticExistsQueryBuilder();
        existsBuilder.field(field);
        existsBuilder.setBoolQueryBuilder(this);
        return existsBuilder;
    }

    /**
     * 创建 match_all 查询并返回 BoolQuery 接口
     */
    public com.awesomecopilot.search8x.builder.query.BoolQuery matchAll() {
        ElasticMatchAllQueryBuilder matchAllBuilder = new ElasticMatchAllQueryBuilder();
        matchAllBuilder.setBoolQueryBuilder(this);
        return matchAllBuilder;
    }

    /**
     * 创建 multi_match 查询并返回 BoolMultiMatchQuery 接口
     * <p>
     * multi_match 查询用于在多个字段中搜索相同的文本，支持多种匹配类型（best_fields, most_fields, cross_fields, phrase 等）
     *
     * @param queryText 要搜索的文本
     * @param fields 要搜索的字段列表，可以带权重（如 "goods_name^3", "goods_desc^1"）
     * @return BoolMultiMatchQuery 接口，支持继续添加布尔查询条件
     */
    public BoolMultiMatchQuery multiMatch(String queryText, String... fields) {
        ElasticMultiMatchQueryBuilder multiMatchBuilder = new ElasticMultiMatchQueryBuilder();
        multiMatchBuilder.query(queryText, fields);
        multiMatchBuilder.setBoolQueryBuilder(this);
        return multiMatchBuilder;
    }

    public ElasticBoolQueryBuilder minimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    public ElasticBoolQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    public ElasticBoolQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    public ElasticBoolQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticBoolQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        // 如果有 pendingNestedPath，需要将子查询分为两组：
        // 1. 非嵌套组：pendingNestedPath 设置之前添加的子查询
        // 2. 嵌套组：pendingNestedPath 设置之后添加的子查询，整体包装在一个 nested bool 查询中
        if (isNotBlank(pendingNestedPath) && !queries.isEmpty()) {
            int splitIndex = queries.size();
            for (int i = 0; i < queries.size(); i++) {
                if (queries.get(i).nestedGroup) {
                    splitIndex = i;
                    break;
                }
            }
            List<Node> nonNestedQueries = queries.subList(0, splitIndex);
            List<Node> nestedQueries = queries.subList(splitIndex, queries.size());

            return Query.of(q -> q.bool(outerBool -> {
                // 添加非嵌套条件
                for (Node node : nonNestedQueries) {
                    addClause(outerBool, node);
                }
                // 将嵌套条件包装在一个 nested bool 查询中
                if (!nestedQueries.isEmpty()) {
                    Query nestedBoolQuery = Query.of(q2 -> q2.bool(innerBool -> {
                        for (Node node : nestedQueries) {
                            addClause(innerBool, node);
                        }
                        if (minimumShouldMatch != null) {
                            innerBool.minimumShouldMatch(minimumShouldMatch.toString());
                        }
                        return innerBool;
                    }));
                    Query wrappedNested = Query.of(q2 -> q2.nested(n -> n
                            .path(pendingNestedPath).query(nestedBoolQuery).scoreMode(ChildScoreMode.Avg)));
                    outerBool.must(wrappedNested);
                }
                return outerBool;
            }));
        }

        Query query = Query.of(q -> q.bool(b -> {
            for (Node node : queries) {
                addClause(b, node);
            }
            if (minimumShouldMatch != null) {
                b.minimumShouldMatch(minimumShouldMatch.toString());
            }
            return b;
        }));

        // 只有当显式设置了 nestedPath 且没有子查询使用它时，才将整个 bool 查询包裹在 nested 中
        // 这种情况适用于所有子查询都应该在同一个 nested 上下文中的场景
        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }

    /**
     * 将查询节点添加到 bool 查询的对应子句中
     */
    private void addClause(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder, Node node) {
        if (node.type == MUST) {
            boolBuilder.must(node.query);
        } else if (node.type == MUST_NOT) {
            boolBuilder.mustNot(node.query);
        } else if (node.type == SHOULD) {
            boolBuilder.should(node.query);
        } else if (node.type == FILTER) {
            boolBuilder.filter(node.query);
        }
    }
}
