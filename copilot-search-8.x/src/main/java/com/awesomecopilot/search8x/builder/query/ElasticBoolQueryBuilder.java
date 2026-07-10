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

        Node(BoolQueryType type, Query query) {
            this.type = type;
            this.query = query;
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
        this.queries.add(new Node(MUST, query));
        return this;
    }

    /**
     * 添加 must_not 条件
     */
    public ElasticBoolQueryBuilder mustNot(Query query) {
        this.queries.add(new Node(MUST_NOT, query));
        return this;
    }

    /**
     * 添加 should 条件
     */
    public ElasticBoolQueryBuilder should(Query query) {
        this.queries.add(new Node(SHOULD, query));
        return this;
    }

    /**
     * 添加 filter 条件
     */
    public ElasticBoolQueryBuilder filter(Query query) {
        this.queries.add(new Node(FILTER, query));
        return this;
    }

    /**
     * 创建 term 查询并返回 BoolTermQuery 接口
     */
    public BoolTermQuery term(String field, Object value) {
        ElasticTermQueryBuilder termBuilder = new ElasticTermQueryBuilder();
        termBuilder.query(field, value);
        if (pendingNestedPath != null) {
            termBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
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
        if (pendingNestedPath != null) {
            termsBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
        termsBuilder.setBoolQueryBuilder(this);
        return termsBuilder;
    }

    /**
     * 创建 match 查询并返回 BoolMatchQuery 接口
     */
    public BoolMatchQuery match(String field, String value) {
        ElasticMatchQueryBuilder matchBuilder = new ElasticMatchQueryBuilder();
        matchBuilder.query(field, value);
        if (pendingNestedPath != null) {
            matchBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
        matchBuilder.setBoolQueryBuilder(this);
        return matchBuilder;
    }

    /**
     * 创建 range 查询并返回 BoolRangeQuery 接口
     */
    public BoolRangeQuery range(String field) {
        ElasticRangeQueryBuilder rangeBuilder = new ElasticRangeQueryBuilder();
        rangeBuilder.field(field);
        if (pendingNestedPath != null) {
            rangeBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
        rangeBuilder.setBoolQueryBuilder(this);
        return rangeBuilder;
    }

    /**
     * 创建 exists 查询
     */
    public com.awesomecopilot.search8x.builder.query.BoolQuery exists(String field) {
        ElasticExistsQueryBuilder existsBuilder = new ElasticExistsQueryBuilder();
        existsBuilder.field(field);
        if (pendingNestedPath != null) {
            existsBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
        existsBuilder.setBoolQueryBuilder(this);
        return existsBuilder;
    }

    /**
     * 创建 match_all 查询并返回 BoolQuery 接口
     */
    public com.awesomecopilot.search8x.builder.query.BoolQuery matchAll() {
        ElasticMatchAllQueryBuilder matchAllBuilder = new ElasticMatchAllQueryBuilder();
        if (pendingNestedPath != null) {
            matchAllBuilder.nestedPath(pendingNestedPath);
            pendingNestedPath = null;
        }
        matchAllBuilder.setBoolQueryBuilder(this);
        return matchAllBuilder;
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
        Query query = Query.of(q -> q.bool(b -> {
            for (Node node : queries) {
                if (node.type == MUST) {
                    b.must(node.query);
                } else if (node.type == MUST_NOT) {
                    b.mustNot(node.query);
                } else if (node.type == SHOULD) {
                    b.should(node.query);
                } else if (node.type == FILTER) {
                    b.filter(node.query);
                }
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
}
