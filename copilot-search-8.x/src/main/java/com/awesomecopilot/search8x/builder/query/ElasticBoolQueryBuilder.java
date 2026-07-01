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
        this.nestedPath = path;
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

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
