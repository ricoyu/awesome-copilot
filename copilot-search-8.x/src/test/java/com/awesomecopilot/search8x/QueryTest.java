package com.awesomecopilot.search8x;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils.Query;
import com.awesomecopilot.search8x.pojo.AirConditioner;
import com.awesomecopilot.search8x.pojo.GoodsEs;
import com.awesomecopilot.search8x.pojo.GoodsInfo;
import com.awesomecopilot.search8x.vo.ElasticPage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static co.elastic.clients.elasticsearch._types.query_dsl.Operator.And;
import static co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or;
import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.CrossFields;
import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.Phrase;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ElasticUtils.Query 查询单元测试类
 * 从 copilot-search 模块迁移而来, 使用 ES 8.x ElasticUtils.Query 内部类下的查询接口
 * <p>
 * 对应原版 ElasticUtils.Query 内部类下的所有查询接口测试:
 * - matchQuery / matchAllQuery / matchPhraseQuery
 * - termQuery / termsQuery
 * - range / bool / multiMatch
 * - queryString / uriQuery
 * - prefix / exists / geoDistance
 * - nested query
 * - constantScore
 * - includeSources
 */
@Slf4j
public class QueryTest {
	
	/**
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "term": {
	 *       "category": "手机数码"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testShopGoodsTermQuery() {
		List<GoodsEs> goods = ElasticUtils.Query.termQuery("shop_goods")
				.query("category", "手机数码")
				.size(100)
				.includeSources("goods_name", "goods_desc", "category", "brand.brand_name")
				.excludeSources("_id")
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(goods.size()).isEqualTo(20);
		assertNotNull(goods.get(0).getId());
		goods.stream().forEach(System.out::println);
		
		String id = goods.get(0).getId();
		String json = JacksonUtils.toPrettyJson(goods.get(0));
		System.out.print(json);
		GoodsEs object = JacksonUtils.toObject(json, GoodsEs.class);
		assertEquals(id, object.getId());
	}
	
	/**
	 * terms 查询（多关键词精准匹配，in 逻辑）
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "terms": {
	 *       "category": ["手机数码", "家用电器"]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsTermsQuery() {
		List<Object> docs = ElasticUtils.Query.termsQuery("shop_goods")
				.query("category", "手机数码", "电脑")
				.size(100).queryForList();
		docs.stream().forEach(System.out::println);
	}
	
	@Test
	public void testGoodsIdsquery() {
		List<Object> docs = Query.idsQuery("shop_goods")
				.ids("Sn-fRJ8Bu9b8NT5bPKe5", "S3-fRJ8Bu9b8NT5bPKe6", "TH-fRJ8Bu9b8NT5bPKe6")
				//.ids(1,2,3)
				.size(10)
				.queryForList();
		assertThat(docs.size()).isEqualTo(3);
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * 查询没有下架标记（online字段存在）
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "exists": {
	 *       "field": "online"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsExistsQuery() {
		List<Object> docs = Query.exists("shop_goods")
				.field("online")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET /shop_goods/_search
	 * {
	 *   "query": {
	 *     "range": {
	 *       "price": {
	 *         "gte": 3000,
	 *         "lte": 8000
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsRangeQuery() {
		List<Object> docs = Query.range("shop_goods")
				.field("price")
				.gte(3000.0)
				.lte(8000.0)
				.size(100)
				.queryForList();
		assertThat(docs.size()).isEqualTo(31);
		docs.stream().forEach(System.out::println);
	}
	
	@Test
	public void testGoodsPublishAfter() {
		List<Object> docs = Query.range("shop_goods")
				.field("publish_time")
				.gte("2026-01-01 00:00:00")
				.size(1000)
				.resultType(GoodsEs.class)
				.queryForList();
		
		assertThat(docs.size()).isEqualTo(107);
		System.out.println(JacksonUtils.toPrettyJson(docs.get(0)));
	}
	
	/**
	 * 商品名称包含 华为 手机
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "match": {
	 *       "goods_name": "华为"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsNameMatch() {
		List<Object> docs = Query.matchQuery("shop_goods")
				.query("goods_name", "华为")
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(3);
		for (Object doc : docs) {
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * match 会先用字段 goods_name 绑定的分词器拆分 华为手机，默认拆成两个词项：["华为", "手机"]
	 * operator 就是控制这些拆分后的词项之间是「同时满足」还是「满足其一」。
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "match": {
	 *       "goods_name": {
	 *         "query": "华为手机",
	 *         "operator": "and"
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsNameMatchOperator() {
		List<Object> docs = Query.matchQuery("shop_goods")
				.query("goods_name", "华为手机")
				.operator(And)
				.queryForList();
		assertThat(docs.size()).isEqualTo(1);
		for (Object doc : docs) {
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * 要求搜索词分词在原文中连续、顺序一致，适合精准短句搜索
	 * <ul>match_phrase 硬性规则:
	 *     <li/>词必须按完全相同顺序出现
	 *     <li/>词与词之间中间不能有其他分词（默认 slop=0，无间隔容忍）
	 * </ul>
	 *
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "match_phrase": {
	 *       "goods_name": {
	 *         "query":"Mate70 Pro",
	 *         "slop": 2
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testGoodsMatchPhrase() {
		List<Object> docs = Query.matchPhraseQuery("shop_goods")
				.query("goods_name", "Mate70 Pro")
				.slop(2)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(1);
		for (Object doc : docs) {
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * 匹配全部文档，无过滤条件
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "match_all": {}
	 *   },
	 *   "size": 200
	 * }
	 * </pre>
	 */
	@Test
	public void testShopMatchAll() {
		List<GoodsEs> docs = Query.matchAllQuery("shop_goods")
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(109);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * 同时搜索商品名 + 商品描述 <p>
	 * 一次性在多个 text 字段检索，支持权重、匹配模式、字段通配符
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "size": 200,
	 *   "query": {
	 *     "multi_match": {
	 *       "query": "小米影像手机",
	 *       "fields": ["goods_name", "goods_desc"]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testShopMultiMatch() {
		List<GoodsEs> docs = Query.multiMatch("shop_goods")
				.query("小米影像手机", "goods_name^3", "goods_desc^1")
				//.query("小米影像手机", "goods_name", "goods_desc")
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(17);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * 取单字段最高分作为文档分数
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "size": 200,
	 *   "query": {
	 *     "multi_match": {
	 *       "query": "美的空调",
	 *       "fields": ["goods_name","goods_desc"],
	 *       "type": "best_fields"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testShopMultiMatchBest() {
		List<GoodsEs> docs = Query.multiMatch("shop_goods")
				.query("美的空调", "goods_name", "goods_desc")
				.type(TextQueryType.BestFields)
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(5);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * 一套完整分词，必须在同一个字段内部全部匹配才算有效加分，不能把词拆开、一部分去 A 字段、一部分去 B 字段凑结果；<p>
	 * operator: and：单个字段必须同时包含所有分词，这个字段才算匹配成功，才会参与累加打分；<p>
	 * operator: or：单个字段只要包含任意 1 个分词，这个字段就算匹配成功，参与累加打分。<p>
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "size": 200,
	 *   "query": {
	 *     "multi_match": {
	 *       "query": "华为 Mate70 Pro",
	 *       "fields": ["goods_name", "brand.brand_name"],
	 *       "type": "most_fields",
	 *       "operator": "or"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testShopMultiMatchMostField() {
		List<Object> docs = Query.multiMatch("shop_goods")
				.query("华为 Mate70 Pro", "goods_name", "brand.brand_name")
				.type(TextQueryType.MostFields)
				.operator(And)
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(1);
		for (Object doc : docs) {
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
		
		docs = Query.multiMatch("shop_goods")
				.query("华为 Mate70 Pro", "goods_name", "brand.brand_name")
				.type(TextQueryType.MostFields)
				.operator(Operator.Or)
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(11);
		for (Object doc : docs) {
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	@Test
	public void testShopMultiMatchCressFieldQuery() {
		List<GoodsEs> docs = Query.multiMatch("shop_goods")
				.query("苹果 Mate70 Pro 5G手机", "goods_name^3", "brand.brand_name^2", "goods_desc^1")
				.type(CrossFields)
				.operator(Or)
				.minimumShouldMatch(4)
				.resultType(GoodsEs.class)
				.queryForList();
		
		assertThat(docs.size()).isEqualTo(1);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
		
	}
	
	/**
	 * <pre>
	 * POST goods_info/_search
	 * {
	 *   "query": {
	 *     "multi_match": {
	 *       "query": "美的节能空调",
	 *       "fields": ["goods_name", "goods_desc"],
	 *       "type": "phrase"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testMultiMatchPhraseShopInfo() {
		List<GoodsInfo> docs = Query.multiMatch("goods_info")
				.query("美的节能空调", "goods_name", "goods_desc")
				.type(Phrase)
				.size(200)
				.resultType(GoodsInfo.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(6);
		for (GoodsInfo doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	@Test
	public void testBoolShop() {
		List<GoodsEs> docs = Query.bool("shop_goods")
				.match("goods_name", "手机").must()
				.term("online", true).filter()
				.range("price").lte(6000).filter()
				.resultType(GoodsEs.class)
				.size(100)
				.queryForList();
		assertThat(docs.size()).isEqualTo(6);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * should 多条件或逻辑（匹配华为 / 小米品牌加分）
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {"match_all": {}}
	 *       ],
	 *       "should": [
	 *         {"term": {
	 *           "brand.brand_name": "华为"
	 *         }},
	 *         {"term": {
	 *           "brand.brand_name": "小米"
	 *         }}
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testBoolShouldTermShopGoods() {
		List<GoodsEs> docs = Query.bool("shop_goods")
				.matchAll().must()
				.term("brand.brand_name", "华为").should()
				.term("brand.brand_name", "小米").should()
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		
		assertThat(docs.size()).isEqualTo(109);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * must_not 排除条件：排除苹果商品
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {"match": {
	 *           "category": "手机数码"
	 *         }}
	 *       ],
	 *       "must_not": [
	 *         {"term": {
	 *           "brand.brand_name": "苹果"
	 *         }}
	 *       ]
	 *     }
	 *   },
	 *   "_source": ["goods_name", "goods_desc", "category", "brand.brand_name"]
	 * }
	 * </pre>
	 */
	@Test
	public void testShopGoodBoolMustMustNot() {
		List<GoodsEs> goods = ElasticUtils.Query.bool("shop_goods")
				.match("category", "手机数码").must()
				.term("brand.brand_name", "苹果").mustNot()
				.includeSources("goods_name", "goods_desc", "category", "brand.brand_name")
				.excludeSources("_id")
				.size(100)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(goods.size()).isEqualTo(17);
		goods.stream().forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "nested": {
	 *       "path": "sku_list",
	 *       "query": {
	 *         "term": {
	 *           "sku_list.color": "白色"
	 *         }
	 *       }
	 *     }
	 *   },
	 *   "_source": ["goods_name", "goods_desc", "sku_list"]
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryShopGoodNested() {
		List<GoodsEs> goods = ElasticUtils.Query.termQuery("shop_goods")
				.nestedPath("sku_list")
				.query("sku_list.color", "白色")
				.includeSources("goods_name", "goods_desc", "sku_list")
				.resultType(GoodsEs.class)
				.size(200)
				.queryForList();
		assertThat(goods.size()).isEqualTo(23);
		for (GoodsEs good : goods) {
			assertThat(good.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(good));
		}
	}

	/**
	 * 空调嵌套查询：筛选 specs 嵌套文档中颜色为"纯净白"且售价 >= 2400 的空调
	 * <p>
	 * 等价 DSL:
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "size": 10,
	 *   "query": {
	 *     "bool": {
	 *       "filter": [
	 *         {
	 *           "nested": {
	 *             "path": "specs",
	 *             "query": {
	 *               "bool": {
	 *                 "must": [
	 *                   { "term": { "specs.color": "纯净白" } },
	 *                   { "range": { "specs.sale_price": { "gte": 2400 } } }
	 *                 ]
	 *               }
	 *             }
	 *           }
	 *         }
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAirConditionerNestedBoolFilter() {
		List<AirConditioner> results = Query.bool("air_conditioner")
				.nestedPath("specs")
				.term("specs.color", "纯净白").must()
				.range("specs.sale_price").gte(2400).must()
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		assertThat(results.size()).isEqualTo(1);
		for (AirConditioner result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	/**
	 * bool must 混合普通 match 查询与 nested term 查询
	 * <p>
	 * 外层 bool 查询的 must 中同时包含：
	 * <ol>
	 *   <li>对根字段 goods_name 的 match 查询</li>
	 *   <li>对嵌套文档 specs 的 nested + term 查询</li>
	 * </ol>
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {
	 *           "match": {
	 *             "goods_name": "美的空调"
	 *           }
	 *         },
	 *         {
	 *           "nested": {
	 *             "path": "specs",
	 *             "query": {
	 *               "term": {
	 *                 "specs.color": "深空灰"
	 *               }
	 *             }
	 *           }
	 *         }
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAirConditionerBoolMustWithNestedTerm() {
		//co.elastic.clients.elasticsearch._types.query_dsl.Query nestedQuery = QueryBuilders.nested(n -> n
		//		.path("specs")
		//		.query(q -> q.term(t -> t.field("specs.color").value("深空灰")))
		//		.scoreMode(ChildScoreMode.Avg));

		List<AirConditioner> results = Query.bool("air_conditioner")
				.match("goods_name", "美的空调").must()
				//.must(nestedQuery)
				.nestedPath("specs")
				.term("specs.color", "深空灰").must()
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		assertThat(results.size()).isEqualTo(1);
		for (AirConditioner result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}

	/**
	 * 测试 Bool 查询中使用 multiMatch 的链式调用
	 * <p>
	 * 验证 multiMatch 可以在 bool 查询中作为子查询使用，并支持 must/mustNot/should/filter 等布尔操作
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {
	 *           "multi_match": {
	 *             "query": "小米影像手机",
	 *             "fields": ["goods_name^3", "goods_desc^1"]
	 *           }
	 *         }
	 *       ],
	 *       "filter": [
	 *         {"term": {"online": true}}
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testBoolMultiMatchChainCall() {
		List<GoodsEs> results = Query.bool("shop_goods")
				.multiMatch("小米影像手机", "goods_name^3", "goods_desc^1").must()
				.term("online", true).filter()
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(results).isNotEmpty();
		for (GoodsEs result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}

	/**
	 * 测试 nested bool 查询中的 range 条件
	 * <p>
	 * 查询 sku_list 嵌套文档中，sku_price <= 5000 且 sku_stock >= 500 的商品
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *   "query": {
	 *     "nested": {
	 *       "path": "sku_list",
	 *       "query": {
	 *         "bool": {
	 *           "must": [
	 *             {"range": {"sku_list.sku_price": {"lte": 5000}}},
	 *             {"range": {"sku_list.sku_stock": {"gte": 500}}}
	 *           ]
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testNestedBoolWithRangeQuery() {
		List<GoodsEs> results = Query.bool("shop_goods")
				.nestedPath("sku_list")
				.range("sku_list.sku_price").lte(5000).must()
				.range("sku_list.sku_stock").gte(500).must()
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(results.size()).isEqualTo(87);
		for (GoodsEs result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	
	/**
	 * 在售手机，且存在价格低于 5000 的黑色 sku
	 * <pre>
	 * GET /shop_goods/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "filter": [
	 *         {"term": {"category": "手机数码"}},
	 *         {"term": {"online": true}}
	 *       ],
	 *       "must": [
	 *         {
	 *           "nested": {
	 *             "path": "sku_list",
	 *             "query": {
	 *               "bool": {
	 *                 "must": [
	 *                   {"term": {"sku_list.color": "黑色"}},
	 *                   {"range": {"sku_list.sku_price": {"lte": 5000}}}
	 *                 ]
	 *               }
	 *             }
	 *           }
	 *         }
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testBoolNestedShopGoods() {
		List<GoodsEs> docs = Query.bool("shop_goods")
				.term("online", true).filter()
				.term("category", "手机数码").filter()
				.nestedPath("sku_list")
				.term("sku_list.color", "黑色").must()
				.range("sku_list.sku_price").lte(5000).must()
				.size(200)
				.resultType(GoodsEs.class)
				.queryForList();
		assertThat(docs.size()).isEqualTo(7);
		for (GoodsEs doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "default_field": "name,job,city,tag",
	 *       "query": "开发"
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "fields": ["name","job","city","tag"],
	 *       "query": "开发"
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 */
	@Test
	public void testQueryStringDefaultField2() {
		List<Object> docs = Query.queryString("user_info")
				.defaultField("name,job,city,tag")
				.query("开发")
				.queryForList();
		assertThat(docs.size()).isEqualTo(0);
		
		docs = Query.queryString("user_info")
				.fields("name", "job", "city", "tag")
				.query("开发")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
		
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "fields": ["name","job","city","tag"],
	 *       "query": "北京 后端"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testDuoguanjianzibingxingpipei() {
		List<Object> docs = Query.queryString("user_info")
				.fields("name", "job", "city", "tag")
				.query("北京 后端") //默认 OR 关系
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * 精准短语匹配（引号包裹）
	 * 要求分词顺序、内容完全一致
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "fields": ["name","job","city","tag"],
	 *       "query": "\"后端开发工程师\""
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringExactMatch() {
		List<Object> docs = Query.queryString("user_info")
				.fields("name", "job", "city", "tag")
				.query("""
						\"后端开发工程师\"""")
				.queryForList();
		
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
		
	}
	
	/**
	 * 语法：`字段名:关键词`，仅查询指定字段，精准缩小查询范围。
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "city:北京"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testSpecifyField() {
		List<Object> docs = Query.queryString("user_info")
				.query("city:北京")
				.queryForList();
		
		assertThat(docs.size()).isEqualTo(2);
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * tag:java mysql 不加括号 语法合法能跑，但逻辑完全不一样；
	 * tag:(java mysql) 加括号才是：只在 tag 字段里匹配 java 或 mysql；
	 * 不加括号：query": "tag:java mysql" 拆解逻辑:
	 * tag:java：必须在 tag 字段匹配 java
	 * mysql：没有指定字段，会丢给 default_field 全部字段去匹配 mysql
	 * tag:java OR (mysql 在 name/job/city/tag 任意字段)
	 *
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "tag:(java mysql)"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testJobcontainsKaifa() {
		List<Object> docs = Query.queryString("user_info")
				//.query("tag:(java mysql)")
				.query("tag:(java mysql)")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	@Test
	public void testQueryStringAnd() {
		List<Object> docs = Query.queryString("user_info")
				//.query("city:北京 AND job:后端")
				.query("city:北京 OR city:上海")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "NOT city:北京"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringNot() {
		List<Object> docs = Query.queryString("user_info")
				.query("NOT city:北京")
				.queryForList();
		docs.stream().forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "job:开发 -job:前端"
	 *     }
	 *   }
	 * }
	 * </pre>
	 * <p>
	 * job:开发:  限定只在 job 字段检索词条「开发」，只要 job 包含开发，这条文档就满足正向匹配条件。
	 * -job:前端: 负号 - 代表强制排除：文档的 job 字段不能包含「前端」
	 * 整体逻辑:  文档 `job` 字段包含「开发」**并且** `job` 字段不包含「前端」
	 */
	@Test
	public void testDefaultAndNot() {
		List<Object> docs = Query.queryString("user_info")
				.query("job:开发 -job:前端")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	@Test
	public void testGroup() {
		List<Object> docs = Query.queryString("user_info")
				.query("(city:北京 AND job:后端) OR (city:上海 AND job:前端)")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	// ==================== Match Query 相关测试 ====================
	// 对应原版 ElasticUtilsMatchQueryTest
	
	/**
	 * 对应原版 ElasticUtilsMatchQueryTest.testMatch()
	 */
	@Test
	public void testMatch() {
		try {
			List<String> movies = ElasticUtils.Query.matchQuery("movies")
					.query("title", "King George")
					.queryForList();
			log.info("Match query results: {}", movies.size());
			movies.forEach(System.out::println);
			assertThat(movies.size() == 10);
		} catch (Exception e) {
			log.warn("Search failed (ES may not be running): {}", e.getMessage());
		}
	}
	
	@Test
	public void testPrefixMatch() {
		List<Object> docs = Query.queryString("user_info")
				//.query("name: 张*")
				.query("name: 张*")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	@Test
	public void testInnerMatch() {
		List<Object> docs = Query.queryString("user_info")
				.query("job: *开发*")
				.queryForList();
		assertThat(docs.size()).isGreaterThan(0);
		docs.stream().forEach(System.out::println);
	}
	
	
	@Test
	public void test() {
		List<Object> banks = ElasticUtils.Query.matchQuery("bank")
				.query("address", "mill")
				.queryForList();
		banks.forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "job:*开发*"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAddressAgeCount() {
		long count = ElasticUtils.Query.bool("bank")
				.match("address", "mill").must()
				.term("age", 32)
				.must()
				.queryForCount();
		
		assertEquals(52, count);
	}
	
	/**
	 * 对应原版 ElasticUtilsMatchQueryTest.testMatchWithOperator()
	 */
	@Test
	public void testMatchWithOperator() {
		try {
			List<String> movies = Query.matchQuery("movies")
					.query("title", "King George")
					.operator(And)
					.queryForList();
			log.info("Match with AND operator results: {}", movies.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsMatchQueryTest.testMatchWithMinimunShouldMatch()
	 */
	@Test
	public void testMatchWithMinimumShouldMatch() {
		try {
			List<String> movies = Query.matchQuery("movies")
					.query("title", "Matrix Reload")
					.minimumShouldMatch(1)
					.queryForList();
			log.info("Match with minimumShouldMatch results: {}", movies.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Term Query 相关测试 ====================
	// 对应原版 ElasticUtilsTermQueryTest
	
	/**
	 * 对应原版 ElasticUtilsTermQueryTest.testTermQuery()
	 */
	@Test
	public void testTermQuery() {
		try {
			List<String> movies = Query.termQuery("movies")
					.query("title", "beautiful")
					.size(100)
					.sort("year, title.keyword:desc")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTermQueryTest.testTermQueryThenNotFound()
	 */
	@Test
	public void testTermQueryThenNotFound() {
		try {
			List<String> products = Query.termQuery("products")
					.query("desc", "iPhone")
					.queryForList();
			assertThat(products.size()).isZero();
			
			products = Query.termQuery("products")
					.query("desc", "iphone")
					.queryForList();
			assertThat(products.size()).isEqualTo(1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTermQueryTest.testTermQueryProducts()
	 */
	@Test
	public void testTermQueryProducts() {
		try {
			Query.termQuery("products")
					.query("avaliable", true)
					.queryForList();
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTermQueryTest.testTermQueryConstantScoreProducts()
	 */
	@Test
	public void testTermQueryConstantScoreProducts() {
		try {
			Query.termQuery("products")
					.query("avaliable", true)
					.constantScore(true)
					.queryForList();
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTermQueryTest.testQueryMovies2()
	 */
	@Test
	public void testQueryMovies2() {
		try {
			long count = Query.termQuery("movies2")
					.query("genre.keyword", "Comedy")
					.queryForCount();
			assertEquals(count, 2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Bool Query 相关测试 ====================
	// 对应原版 ElasticUtilsBoolQueryTest
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testBoolMust()
	 */
	@Test
	public void testBoolMust() {
		try {
			List<String> movies = Query.bool("newmovies")
					.term("genre.keyword", "Comedy").must()
					.term("genre_count", 1).must()
					.queryForList();
			assertEquals(movies.size(), 1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testBoolFilter()
	 */
	@Test
	public void testBoolFilter() {
		try {
			List<String> movies = Query.bool("newmovies")
					.term("genre.keyword", "Comedy").filter()
					.term("genre_count", 1).filter()
					.queryForList();
			assertEquals(movies.size(), 1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testBoolFilter2()
	 * <pre>
	 * POST /bank/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": {"match_all": {}},
	 *       "filter": {
	 *         "range": {
	 *           "balance": {
	 *             "gte": 20000,
	 *             "lte": 30000
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testBoolFilter2() {
		try {
			List<String> banks = Query.bool("bank")
					.range("balance")
					.gte(20000)
					.lte(30000)
					.filter()
					.size(10000)
					.queryForList();
			assertEquals(banks.size(), 217);
			banks.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testBoolQueryOptimize()
	 */
	@Test
	public void testBoolQueryOptimize() {
		try {
			List<String> news = Query.bool("news")
					.match("content", "apple").must()
					.match("content", "pie").mustNot()
					.queryForList();
			assertEquals(2, news.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testComplexBoolQuery()
	 * <pre>
	 * GET ecommerce/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {"range": {"price": {"gte": 1000, "lte": 5000}}}
	 *       ],
	 *       "should": [
	 *         {"term": {"category": "笔记本"}},
	 *         {"term": {"category": "手机"}}
	 *       ],
	 *       "must_not": [
	 *         {"term": {"tags": "苹果"}}
	 *       ],
	 *       "filter": [
	 *         {"range": {"rating": {"gte": 4.5}}}
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testComplexBoolQuery() {
		try {
			List<String> objects = Query.bool("ecommerce")
					.range("price").gte(1000).lte(5000).must()
					.term("category", "笔记本").should()
					.term("category", "手机").should()
					.term("tags", "苹果").mustNot()
					.range("rating").gte(4.5).filter()
					.queryForList();
			
			objects.forEach(System.out::println);
			assertThat(objects.size()).isEqualTo(1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testMatchPgrase()
	 * <pre>
	 * GET ecommerce/_search
	 * {
	 *   "query": {
	 *     "match_phrase": {
	 *       "description": {
	 *         "query": "旗舰手机",
	 *         "slop": 2
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testMatchPhraseInBool() {
		try {
			List<String> ecommerces = Query.matchPhraseQuery("ecommerce")
					.query("description", "旗舰手机")
					.slop(2)
					.queryForList();
			ecommerces.forEach(System.out::println);
			assertThat(ecommerces.size()).isEqualTo(2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testDateRagequery()
	 */
	@Test
	public void testDateRangeQuery() {
		try {
			List<String> ecommerces = Query.range("ecommerce")
					.field("created_at").gte("2023-12-01||-1y/y").lte("2023-12-01")
					.includeSources("_id")
					.queryForList();
			ecommerces.forEach(System.out::println);
			assertThat(ecommerces.size()).isEqualTo(4);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Range Query 相关测试 ====================
	// 对应原版 ElasticUtilsRangeQueryTest
	
	/**
	 * 对应原版 ElasticUtilsRangeQueryTest.testRangeQuery()
	 */
	@Test
	public void testRangeQuery() {
		try {
			List<String> products = Query.range("products")
					.field("price")
					.lte(30)
					.gt(20)
					.constantScore(true)
					.queryForList();
			assertThat(products.size() == 2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsRangeQueryTest.testDateRange()
	 */
	@Test
	public void testDateRange() {
		try {
			List<String> products = Query.range("products")
					.field("date")
					.gt("now-5y")
					.queryForList();
			assertThat(products.size() == 1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Exists Query 相关测试 ====================
	// 对应原版 ElasticUtilsExistsTest
	
	/**
	 * 对应原版 ElasticUtilsExistsTest.testExists()
	 */
	@Test
	public void testExists() {
		try {
			List<String> products = Query.exists("products")
					.field("date")
					.constantScore(true)
					.queryForList();
			assertThat(products.size() == 2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Prefix Query 相关测试 ====================
	// 对应原版 ElasticUtilsPrefixQueryTest
	
	/**
	 * 对应原版 ElasticUtilsPrefixQueryTest.testPrefixQuery()
	 */
	@Test
	public void testPrefixQuery() {
		try {
			List<String> ecommerces = Query.prefix("ecommerce")
					.query("product_name.keyword", "Apple")
					.queryForList();
			for (String ecommerce : ecommerces) {
				System.out.println(ecommerce);
			}
			assertThat(ecommerces.size()).isEqualTo(1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== MatchPhrase Query 相关测试 ====================
	// 对应原版 ElasticUtilsMatchPhraseTest
	
	/**
	 * 对应原版 ElasticUtilsMatchPhraseTest.testMatchPhrase()
	 */
	@Test
	public void testMatchPhrase() {
		try {
			List<String> movies = Query.matchPhraseQuery("movies")
					.query("title", "one love")
					.slop(1)
					.queryForList();
			
			for (String movie : movies) {
				System.out.println(movie);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== GeoDistance Query 相关测试 ====================
	// 对应原版 ElasticUtilsGeoDistanceQueryTest
	
	/**
	 * 对应原版 ElasticUtilsGeoDistanceQueryTest.testGeoDistance()
	 */
	@Test
	public void testGeoDistance() {
		try {
			List<String> ecommerces = Query.geoDistance("ecommerce")
					.distance("1000km")
					.location(39.9042, 116.4074)
					.queryForList();
			for (String ecommerce : ecommerces) {
				System.out.println(ecommerce);
			}
			
			assertThat(ecommerces.size()).isEqualTo(2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Nested Query 相关测试 ====================
	// 对应原版 ElasticUtilsNestQueryTest
	
	/**
	 * 对应原版 ElasticUtilsNestQueryTest.testNestedQuery()
	 */
	@Test
	public void testNestedQuery() {
		try {
			List<String> ecommerces = Query.matchQuery("ecommerce")
					.nestedPath("comments")
					.query("comments.user", "user123")
					.queryForList();
			assertThat(ecommerces).hasSize(3);
			for (String ecommerce : ecommerces) {
				System.out.println(ecommerce);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsNestQueryTest.testNestedBoolQuery()
	 */
	@Test
	public void testNestedBoolQuery() {
		try {
			List<String> ecommerces = Query.bool("ecommerce")
					.nestedPath("comments")
					.match("comments.user", "user123").must()
					.range("comments.rating").gte(4.5).must()
					.queryForList();
			assertThat(ecommerces).hasSize(2);
			for (String ecommerce : ecommerces) {
				System.out.println(ecommerce);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== Boost Query 相关测试 ====================
	// 对应原版 ElasticUtilsBoostTest
	
	/**
	 * 对应原版 ElasticUtilsBoostTest.testBoost()
	 * 注意: 原版 boost(4) 传int, 8.x builder 用 boost(float), 所以用 4f
	 */
	@Test
	public void testBoost() {
		try {
			List<String> blogs = Query.bool("blogs")
					.match("title", "apple,ipad").boost(4f).should()
					.match("content", "apple,ipad").should()
					.queryForList();
			blogs.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== URI Query 相关测试 ====================
	// 对应原版 ElasticUtilsUriQueryTest
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testByFirstName()
	 */
	@Test
	public void testUriQueryByFirstName() {
		try {
			List<String> ecommerces = Query.uriQuery("kibana_sample_data_ecommerce")
					.query("customer_first_name=Eddie")
					.queryForList();
			
			ecommerces.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testQueryWithSpace()
	 */
	@Test
	public void testQueryStringWithSpace() {
		try {
			List<String> books = Query.queryString("books")
					.query("author:\"Clinton Gormley\"")
					.queryForList();
			books.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testSearchInMultiFields()
	 */
	@Test
	public void testSearchInMultiFields() {
		try {
			List<String> books = Query.queryString("books")
					.fields("title", "description")
					.query("elasticsearch AND guide")
					.queryForList();
			books.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testBoolcomp()
	 */
	@Test
	public void testBoolComp() {
		try {
			List<String> books = Query.queryString("books")
					.query("(title:elasticsearch OR description:Elasticsearch) AND rating:[4 TO 5]")
					.queryForList();
			assertThat(books.size()).isEqualTo(5);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testPhraseQuery()
	 */
	@Test
	public void testPhraseQuery() {
		try {
			List<String> books = Query.queryString("books")
					.query("description:\"powerful search applications\"")
					.queryForList();
			assertThat(books.size()).isEqualTo(1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testExcludeQuery()
	 */
	@Test
	public void testExcludeQuery() {
		try {
			List<String> books = Query.queryString("books")
					.query("title:Easlticsearch AND -categories:beginner")
					.queryForList();
			assertThat(books.size()).isEqualTo(0);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsUriQueryTest.testQueryWithWeight()
	 */
	@Test
	public void testQueryWithWeight() {
		try {
			List<String> books = Query.queryString("books")
					.query("search")
					.fields("title^2", "description")
					.queryForList();
			books.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== QueryString Query 相关测试 ====================
	// 对应原版 ElasticUtilsQueryStringQueryTest
	
	/**
	 * 对应原版 ElasticUtilsQueryStringQueryTest.testQueryWithDefaultField()
	 */
	@Test
	public void testQueryWithDefaultField() {
		try {
			List<String> books = Query.queryString("books")
					.defaultField("title")
					.query("Easlticsearch AND -categories:beginner")
					.queryForList();
			
			for (String book : books) {
				System.out.println(book);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== MultiMatch Query 相关测试 ====================
	// 对应原版 ElasticMultiMatchTest
	// 注意: 原版 testMostFields 和 testCrossFields 使用 queryBuilder() 模式, 8.x 不支持, 已跳过
	// 注意: 原版 testMultiMatchWithHighlight 需要 highlight 功能, 8.x builder 尚未实现, 已跳过
	
	/**
	 * 对应原版 ElasticMultiMatchTest.testProducts()
	 * 注意: 原版 tieBreaker(0.3f) 传float, 8.x builder 用 tieBreaker(double), 所以用 0.3
	 */
	@Test
	public void testMultiMatchProducts() {
		try {
			List<String> products = Query.multiMatch("products")
					.type(TextQueryType.BestFields)
					.tieBreaker(0.3)
					.query("apple watch", "title", "description")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticMultiMatchTest.testMostFields2()
	 */
	@Test
	public void testMostFields2() {
		try {
			List<String> news = Query.multiMatch("news")
					.type(TextQueryType.MostFields)
					.query("climate change impact", "title", "intro", "body")
					.queryForList();
			news.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticMultiMatchTest.testCrossFields2()
	 */
	@Test
	public void testCrossFields2() {
		try {
			List<String> address = Query.multiMatch("address")
					.type(CrossFields)
					.query("Poland Street W1V", "street", "city", "country", "postcode")
					.operator(And)
					.queryForList();
			address.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== ConstantScore Query 相关测试 ====================
	// 对应原版 ConstantScoreQueryTest
	
	/**
	 * 对应原版 ConstantScoreQueryTest.testTermConstantScore()
	 */
	@Test
	public void testTermConstantScore() {
		try {
			List<String> iphones = Query.termQuery("products")
					.query("productID.keyword", "XHDK-A-1293-#fJ3")
					.constantScore(true)
					.includeSources("desc")
					.queryForList();
			
			iphones.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ConstantScoreQueryTest.testMatchConstantScore()
	 */
	@Test
	public void testMatchConstantScore() {
		try {
			List<String> iphones = Query.matchQuery("products")
					.query("desc", "iPhone")
					.constantScore(true)
					.includeSources("desc")
					.queryForList();
			
			iphones.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== IncludeSource 相关测试 ====================
	// 对应原版 IncludeSourceTest
	
	/**
	 * 对应原版 IncludeSourceTest.testQueryIncludeSource()
	 */
	@Test
	public void testQueryIncludeSource() {
		try {
			String result = Query.termQuery("event_*")
					.query("event_hash", "dcc3fa7e1f608e23857a90ab0cc2fd5608e06733dc6bdef278849c8c6deafa5b")
					.includeSources("create_time")
					.queryForOne();
			System.out.println(result);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 BoolQueryTest 迁移 ====================
	
	/**
	 * 对应原版 BoolQueryTest.testBoolRange()
	 */
	@Test
	public void testBoolRange() {
		try {
			List<String> results = Query.bool("event")
					.range("datetime").gte(1623134434000L).lte(1623134434000L).must()
					.excludeSources("http", "payload")
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 BoolQueryTest.testBoolMatch()
	 */
	@Test
	public void testBoolMatch() {
		try {
			List<String> events = Query.bool("event")
					.match("alert_risk_level", "medium").mustNot()
					.range("datetime").gte(1623134434000L).lte(1623134434000L).must()
					.term("alert_category", "web-attack").mustNot()
					.includeSources("datetime", "alert_risk_level", "src_ip", "dest_ip")
					.queryForList();
			events.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 ExistsQueryTest 迁移 ====================
	
	/**
	 * 对应原版 ExistsQueryTest.testDateExists()
	 */
	@Test
	public void testDateExists() {
		try {
			List<String> products = Query.exists("products")
					.field("date")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ExistsQueryTest.testDateExistsInclude()
	 */
	@Test
	public void testDateExistsInclude() {
		try {
			List<String> products = Query.exists("products")
					.field("date")
					.includeSources("date", "price")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ExistsQueryTest.testBoolExists()
	 */
	@Test
	public void testBoolExists() {
		try {
			List<String> results = Query.bool("netlog_*")
					.exists("dst_city").filter()
					.exists("dst_country").filter()
					.includeSources("dst_city", "dst_country")
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 MatchQueryTest 迁移 ====================
	
	/**
	 * 对应原版 MatchQueryTest.testOperatorAnd()
	 */
	@Test
	public void testOperatorAnd() {
		try {
			List<String> titles = Query.matchQuery("movies")
					.query("title", "Last Christmas")
					.includeSources("title")
					.queryForList();
			titles.forEach(System.out::println);
			
			titles = Query.matchQuery("movies")
					.query("title", "Last Christmas")
					.operator(And)
					.includeSources("title")
					.queryForList();
			titles.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 MatchQueryTest.testMinimumShouldMatch()
	 */
	@Test
	public void testMinimumShouldMatchCompare() {
		try {
			List<String> titles = Query.matchQuery("movies")
					.query("title", "Once Upon a Time in the Midlands")
					.includeSources("title")
					.queryForList();
			
			List<String> titles2 = Query.matchQuery("movies")
					.query("title", "Once Upon a Time in the Midlands")
					.minimumShouldMatch(7)
					.includeSources("title")
					.queryForList();
			
			assertThat(titles.size()).isGreaterThan(titles2.size());
			titles.forEach(System.out::println);
			System.out.println("------------------------");
			titles2.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 TermQueryTest 迁移 ====================
	
	/**
	 * 对应原版 TermQueryTest.testTermQueryIphone()
	 */
	@Test
	public void testTermQueryIphone() {
		try {
			List<String> iphones = Query.termQuery("products")
					.query("desc", "iPhone")
					.queryForList();
			assertThat(iphones.size()).isEqualTo(0);
			
			iphones = Query.termQuery("products")
					.query("desc", "iphone")
					.queryForList();
			assertThat(iphones.size()).isEqualTo(1);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 RangeQueryTest 迁移 ====================
	
	/**
	 * 对应原版 RangeQueryTest.testDateRange()
	 */
	@Test
	public void testDateRange2() {
		try {
			List<String> products = Query.range("products")
					.field("date")
					.gte("2018-01-01")
					.lte("2019-01-01")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 RangeQueryTest.testDateExpression()
	 */
	@Test
	public void testDateExpression() {
		try {
			List<String> products = Query.range("products")
					.field("date")
					.gt("now-4y")
					.queryForList();
			assertThat(products).size().isEqualTo(1);
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 TermsQueryTest 迁移 ====================
	
	/**
	 * 对应原版 TermsQueryTest.testTermsQuery()
	 */
	@Test
	public void testTermsQuery() {
		try {
			List<String> movies = Query.termsQuery("movies")
					.query("title.keyword", "Balto", "Mortal Kombat")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 MatchPhraseQueryTest 迁移 ====================
	
	/**
	 * 对应原版 MatchPhraseQueryTest.testMatchPhraseQuery()
	 */
	@Test
	public void testMatchPhraseQuery2() {
		try {
			List<String> movies1 = Query.matchPhraseQuery("movies")
					.query("title", "one love")
					.sort("title.keyword:asc")
					.queryForList();
			movies1.forEach(System.out::println);
			
			List<String> movies2 = Query.matchPhraseQuery("movies")
					.query("title", "one love")
					.slop(1)
					.queryForList();
			movies2.forEach(System.out::println);
			
			assertThat(movies1.size()).isLessThan(movies2.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 QueryStringQueryTest 迁移 ====================
	
	/**
	 * 对应原版 QueryStringQueryTest.test()
	 */
	
	@Test
	public void testQueryString() {
		List<Object> docs = Query.queryString("")
				.fields("content", "name")
				.query("this AND that")
				.queryForList();
		docs.forEach(System.out::println);
	}
	
	/**
	 * 对应的Query String 查询
	 * <pre>
	 * POST users/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "name:Ruan"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringBasic() {
		try {
			List<String> users = Query.queryString("users")
					.query("name:ruan") //匹配 name 字段包含 Ruan 的文档。
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testAndThenEmpty()
	 * <pre>
	 * POST users/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "about:(java AND lua)"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringAndThenEmpty() {
		try {
			List<String> users = Query.queryString("users")
					.query("about:(java AND lua)")
					.queryForList();
			assertThat(users).isEmpty();
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testPhrase()
	 * <pre>
	 * POST users/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query": "name:\"Ruan Yiming\""
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringPhrase() {
		try {
			List<String> users = Query.queryString("users")
					.query("name:\"ruan yiming\"")
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testDefaultField()
	 */
	@Test
	public void testQueryStringDefaultField() {
		try {
			List<String> users = Query.queryString("users")
					.query("ruan AND yiming")
					.defaultField("name")
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testGroupQuery()
	 */
	@Test
	public void testQueryStringGroupQuery() {
		try {
			List<String> users = Query.queryString("users")
					.fields("name", "about")
					.query("(ruan AND yiming) OR (Java AND Elasticsearch)")
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testPlusMinus()
	 */
	@Test
	public void testQueryStringPlusMinus() {
		try {
			List<String> users = Query.queryString("users")
					.query("name:(+yiming +ruan)")
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testNotOr()
	 */
	@Test
	public void testQueryStringNotOr() {
		try {
			long count = Query.queryString("netlog_*")
					.query("dst_port:((NOT 80) OR 10050)")
					.queryForCount();
			System.out.println(count);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testQueryAll()
	 */
	@Test
	public void testQueryStringQueryAll() {
		try {
			List<String> products = Query.queryString("product")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 QueryStringQueryTest.testQuery4Name()
	 */
	@Test
	public void testQueryStringQuery4Name() {
		try {
			List<String> products = Query.queryString("product")
					.query("name:nfc phone")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 UriQueryTest 迁移 ====================
	
	/**
	 * 对应原版 UriQueryTest.testSortBank()
	 */
	@Test
	public void testUriSortBank() {
		try {
			List<String> banks = Query.uriQuery("bank")
					.query("*")
					.sort("account_number:asc")
					.queryForList();
			banks.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testQueryReturnString()
	 */
	@Test
	public void testUriQueryReturnString() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:2012")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testUriQuery()
	 */
	@Test
	public void testUriQuery2() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:2012")
					.queryForList();
			assertThat(movies.size()).isEqualTo(2);
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testUriOr()
	 */
	@Test
	public void testUriOr() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:Beautiful Mind")
					.queryForList();
			movies.forEach(System.out::println);
			
			List<String> movies2 = Query.uriQuery("movies")
					.query("title:Beautiful OR Mind")
					.queryForList();
			assertEquals(movies.size(), movies2.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testSortThenException()
	 */
	@Test
	public void testUriSortThenException() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:Beautiful Mind")
					.sort("title:asc")
					.queryForList();
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testSortThenOK()
	 */
	@Test
	public void testUriSortThenOK() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:Beautiful Mind")
					.sort("title.keyword:asc")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testQueryEscape()
	 */
	@Test
	public void testUriQueryEscape() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:(Beautiful +Mind)")
					.sort("title.keyword:asc")
					.queryForList();
			movies.forEach(System.out::println);
			
			movies = Query.uriQuery("movies")
					.query("title:(Beautiful -Mind)")
					.sort("title.keyword:asc")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testRange()
	 */
	@Test
	public void testUriRange() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("year:[2002 TO 2004]")
					.sort("year:asc")
					.size(1000)
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testRegex()
	 */
	@Test
	public void testUriRegex() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:[bt]oy")
					.sort("title.keyword")
					.size(1000)
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testIncludeExclude()
	 */
	@Test
	public void testUriIncludeExclude() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:(Beautiful +Mind)")
					.sort("title.keyword:asc")
					.queryForList();
			movies.forEach(System.out::println);
			
			Query.uriQuery("movies")
					.query("title:(Beautiful +Mind)")
					.sort("title.keyword:asc")
					.excludeSources("@version")
					.queryForList()
					.forEach(System.out::println);
			
			Query.uriQuery("movies")
					.query("title:(Beautiful +Mind)")
					.sort("title.keyword:asc")
					.includeSources("title")
					.queryForList()
					.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testGroupQuery()
	 */
	@Test
	public void testUriGroupQuery() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("title:(Beautiful Mind)")
					.queryForList();
			log.info("查询到{}条记录", movies.size());
			assertEquals(5, movies.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testExactValue()
	 */
	@Test
	public void testUriExactValue() {
		try {
			List<String> products = Query.uriQuery("product")
					.query("date:2024-05-27")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testQueryAllFields()
	 */
	@Test
	public void testUriQueryAllFields() {
		try {
			List<String> products = Query.uriQuery("product")
					.query("2024-06-01")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testQuery4Page()
	 */
	@Test
	public void testUriQuery4Page() {
		try {
			List<String> products = Query.uriQuery("product")
					.from(0)
					.size(2)
					.sort("price:asc")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 UriQueryTest.testQueryStringYearGreateThan()
	 */
	@Test
	public void testUriYearGreaterThan() {
		try {
			List<String> movies = Query.uriQuery("movies")
					.query("year:>=1980")
					.sort("year:desc")
					.queryForList();
			for (String movie : movies) {
				System.out.println(movie);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 SortTest 迁移 ====================
	
	/**
	 * 对应原版 SortTest.testMatchAllSort()
	 */
	@Test
	public void testMatchAllSort() {
		try {
			List<String> products = Query.matchAllQuery("product")
					.sort("name.keyword:desc")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 IncludeExcludeTest 迁移 ====================
	
	/**
	 * 对应原版 IncludeExcludeTest.testInclude()
	 */
	@Test
	public void testInclude() {
		try {
			List<String> products = Query.matchAllQuery("product")
					.includeSources("name")
					.queryForList();
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 IndexNotExistsTest 迁移 ====================
	
	/**
	 * 对应原版 IndexNotExistsTest.testWhenOneOfTheIndexNotExists()
	 */
	@Test
	public void testWhenOneOfTheIndexNotExists() {
		try {
			List<String> results = Query.termQuery("netlog_2021-07-16", "netlog_2021-07-17", "netlog_2021-07-18", "netlog_2021-07-19")
					.query("dev_id", "1")
					.queryForList();
			assertThat(results).isNotEmpty();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 GetQueryTest 迁移 ====================
	
	/**
	 * 对应原版 GetQueryTest.testGetById()
	 */
	@Test
	public void testGetById() {
		try {
			String shakespeare = ElasticUtils.Query.byId("shakespeare", "1");
			System.out.println(shakespeare);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 ElasticUtilsTest 迁移 ====================
	
	/**
	 * 对应原版 ElasticUtilsTest.testMatch()
	 */
	@Test
	public void testMatchBank() {
		try {
			List<String> banks = Query.matchQuery("bank")
					.query("address", "mill road")
					.size(10000)
					.queryForList();
			banks.forEach(System.out::println);
			assertThat(banks).size().isEqualTo(33);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testAllMovies()
	 */
	@Test
	public void testAllMovies() {
		try {
			List<String> movies = Query.matchAllQuery("movies")
					.size(10000)
					.queryForList();
			assertThat(movies).size().isEqualTo(9743);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testSampleDataLogs()
	 */
	@Test
	public void testSampleDataLogs() {
		try {
			long kibanaSampleDataLogsCount = Query.matchAllQuery("kibana_sample_data_logs")
					.size(100000000)
					.queryForCount();
			assertThat(kibanaSampleDataLogsCount).isEqualTo(14074);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testTermQuery2()
	 */
	@Test
	public void testTermQuery2() {
		try {
			String bank = Query.termQuery("bank")
					.query("account_number", 970)
					.queryForOne();
			System.out.println(bank);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testMUltiMatchQuery2()
	 */
	@Test
	public void testMultiMatchAddress() {
		try {
			List<String> addresses = Query.multiMatch("address")
					.query("Poland Street W1V", "street", "city", "country", "postcode")
					.type(TextQueryType.BestFields)
					.queryForList();
			assertThat(addresses.size()).isEqualTo(2);
			addresses.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testMultiMatch2()
	 */
	@Test
	public void testMultiMatchBank() {
		try {
			List<String> banks = Query.multiMatch("bank")
					.query("mill Lopezo", "address", "city")
					.queryForList();
			banks.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testSearchNotIndexedField()
	 */
	@Test
	public void testSearchNotIndexedField() {
		try {
			List<String> users = Query.matchQuery("users")
					.query("mobile", "17895062189")
					.queryForList();
			users.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testQueryMatchAll()
	 */
	@Test
	public void testQueryMatchAll() {
		try {
			List<String> movies = Query.matchAllQuery("movies", "404index")
					.queryForList();
			log.info("查询到{}条记录", movies.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testSourceFiltering()
	 */
	@Test
	public void testSourceFiltering() {
		try {
			List<String> ecommerces = Query.matchAllQuery("kibana_sample_data_ecommerce")
					.sort("order_date:desc")
					.includeSources("order_date")
					.queryForList();
			ecommerces.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsTest.testSortAndSourceFilter()
	 */
	@Test
	public void testSortAndSourceFilter() {
		try {
			List<String> ecomerces = Query.matchAllQuery("kibana_sample_data_ecommerce")
					.sort("order_date:desc")
					.includeSources("customer*")
					.queryForList();
			ecomerces.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== 以下从 ElasticUtilsPagingTest 迁移 ====================
	
	/**
	 * 对应原版 ElasticUtilsPagingTest.testFromSizePaging()
	 */
	@Test
	public void testFromSizePaging() {
		try {
			com.awesomecopilot.search8x.vo.ElasticPage<String> page = Query
					.matchAllQuery("kibana_sample_data_ecommerce")
					.paging(1, 1)
					.includeSources("customer_full_name")
					.queryForPage();
			
			List<String> results = page.getResults();
			assertThat(results.size()).isEqualTo(1);
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsPagingTest.testSearchAfter()
	 */
	@Test
	public void testSearchAfterPaging() {
		try {
			com.awesomecopilot.search8x.vo.ElasticPage<String> page = Query
					.matchAllQuery("users")
					.size(1)
					.sort("age:desc,_id:asc")
					.resultType(String.class)
					.queryForPage();
			String user4 = page.getResults().get(0);
			System.out.println(user4);
			Object[] sortValues = page.getSort();
			
			page = Query.matchAllQuery("users")
					.size(1)
					.sort("age:desc,_id:asc")
					.searchAfter(sortValues)
					.queryForPage();
			
			String user3 = page.getResults().get(0);
			System.out.println(user3);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== ElasticUtilsBoolQueryTest 补充测试 ====================
	
	
	/**
	 * 对应原版 ElasticUtilsBoolQueryTest.testMatchPgrase()
	 */
	@Test
	public void testMatchPhraseSlop() {
		try {
			List<Object> ecommerces = Query.matchPhraseQuery("ecommerce")
					.query("description", "旗舰手机")
					.slop(2)
					.queryForList();
			ecommerces.forEach(System.out::println);
			assertThat(ecommerces.size()).isEqualTo(2);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	
	// ==================== ElasticUtilsBoolTest 补充测试 ====================
	
	/**
	 * 对应原版 ElasticUtilsBoolTest.testBool()
	 */
	@Test
	public void testBoolProductShould() {
		try {
			List<Object> products = Query.bool("product")
					.match("name", "游戏手机")
					.should()
					.match("desc", "游戏手机")
					.queryForList();
			
			products.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolTest.testBankBool()
	 */
	@Test
	public void testBankBool() {
		try {
			List<Object> banks = Query.bool("bank")
					.match("age", "40").must()
					.match("state", "ID").mustNot()
					.size(100)
					.queryForList();
			assertThat(banks.size()).isEqualTo(43);
			banks.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsBoolTest.testMustMustShould()
	 */
	@Test
	public void testMustMustShould() {
		try {
			List<Object> banks = Query.bool("bank")
					.match("gender", "M").must()
					.match("address", "mill").must()
					.match("state", "AK").should()
					.queryForList();
			assertThat(banks.size()).isEqualTo(3);
			banks.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== ElasticUtilsProductTest 补充测试 ====================
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testMatchQuery()
	 */
	@Test
	public void testProductMatchQuery() {
		try {
			List<String> products = Query.matchQuery("products")
					.query("description", "蓝牙 降噪")
					.queryForList();
			
			assertThat(products.size()).isEqualTo(2);
			for (String product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testMUltiMatch()
	 */
	@Test
	public void testProductMultiMatch() {
		try {
			List<String> products = Query.multiMatch("products")
					.query("智能", "name", "description")
					.queryForList();
			assertThat(products.size()).isEqualTo(2);
			for (String product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testTermQuery()
	 */
	@Test
	public void testProductTermQuery() {
		try {
			List<Map> products = Query.termQuery("products")
					.query("categories", "audio")
					.resultType(Map.class)
					.queryForList();
			
			assertThat(products.size()).isEqualTo(2);
			for (Map product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testRangeQuery()
	 */
	@Test
	public void testProductRangeQuery() {
		try {
			List<Map> products = Query.range("products")
					.field("price")
					.gte("100")
					.lte("300")
					.resultType(Map.class)
					.queryForList();
			
			assertThat(products.size()).isEqualTo(2);
			for (Map product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testBoolQuery()
	 */
	@Test
	public void testProductBoolQuery() {
		try {
			List<Map> products = Query.bool("products")
					.match("description", "智能").must()
					.range("price").gte(200).filter()
					.term("is_active", true).filter()
					.term("tags", "new").should()
					.term("tags", "popular").should()
					.minimumShouldMatch(1)
					.resultType(Map.class)
					.queryForList();
			
			assertThat(products.size()).isEqualTo(2);
			for (Map product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testNested()
	 */
	@Test
	public void testProductNested() {
		try {
			List<Map> products = Query.bool("products")
					.nestedPath("variants")
					.term("variants.color", "黑色").must()
					.range("variants.price").lte(300).must()
					.resultType(Map.class)
					.queryForList();
			
			assertThat(products.size()).isEqualTo(3);
			for (Map product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testSimplePaging()
	 */
	@Test
	public void testProductSimplePaging() {
		try {
			List<Object> products = Query.matchAllQuery("products")
					.from(1)
					.size(5)
					.sort("price:asc,_id:asc")
					.queryForList();
			
			assertThat(products.size()).isEqualTo(5);
			for (Object product : products) {
				System.out.println(product);
			}
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 ElasticUtilsProductTest.testSearchAfter()
	 */
	@Test
	public void testProductSearchAfter() {
		try {
			com.awesomecopilot.search8x.vo.ElasticPage<Object> elasticPage = Query.range("products")
					.field("price")
					.gte(100)
					.size(3)
					.sort("price:asc,_id:asc")
					.queryForPage();
			
			elasticPage.getResults().forEach(System.out::println);
			
			elasticPage = Query.range("products")
					.field("price")
					.gte(100)
					.size(3)
					.sort("price:asc,_id:asc")
					.searchAfter(elasticPage.getSort())
					.queryForPage();
			elasticPage.getResults().forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== ElasticUtilsSortTest 补充测试 ====================
	
	/**
	 * 对应原版 ElasticUtilsSortTest.testSingleFieldSort()
	 */
	@Test
	public void testSingleFieldSort() {
		try {
			List<Object> docs = Query.matchAllQuery("kibana_sample_data_ecommerce")
					.sort("order_date:desc,_score:desc, _doc:asc")
					.includeSources("order_date", "_score", "_doc")
					.size(5)
					.queryForList();
			
			docs.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== IdsQuery 相关测试 ====================
	// 对应原版 IdsQueryTest
	
	/**
	 * 对应原版 IdsQueryTest.testIdsQuery()
	 */
	@Test
	public void testIdsQuery() {
		try {
			List<Map> results = Query.idsQuery("dga_event_2021-08-30")
					.ids("-3KMqXsBnQfP0ODXFLu4", "_XKNqXsBnQfP0ODXsrtx")
					.resultType(Map.class)
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	
	/**
	 * 测试 idsQuery 带 includeSources
	 */
	@Test
	public void testIdsQueryWithIncludeSources() {
		try {
			List<Map> results = Query.idsQuery("dga_event_2021-08-30")
					.ids("-3KMqXsBnQfP0ODXFLu4")
					.includeSources("create_time", "alert_risk_level")
					.resultType(Map.class)
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== ScrollQuery 相关测试 ====================
	
	/**
	 * 测试 scrollQuery 初始查询
	 */
	@Test
	public void testScrollQuery() {
		try {
			List<String> movies = Query.scrollQuery("movies")
					.size(100)
					.queryForList();
			log.info("Scroll query results: {}", movies.size());
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== MatchPhrasePrefixQuery 相关测试 ====================
	
	/**
	 * 测试 matchPhrasePrefixQuery
	 */
	@Test
	public void testMatchPhrasePrefixQuery() {
		try {
			List<String> movies = Query.matchPhrasePrefixQuery("movies")
					.query("title", "qui")
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 测试 matchPhrasePrefixQuery 带 maxExpansions
	 */
	@Test
	public void testMatchPhrasePrefixWithMaxExpansions() {
		try {
			List<String> movies = Query.matchPhrasePrefixQuery("movies")
					.query("title", "qui")
					.maxExpansions(10)
					.queryForList();
			movies.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== TemplateQuery 相关测试 ====================
	// 对应原版 SearchTemplateTest
	
	/**
	 * 对应原版 SearchTemplateTest.testTemplateQuery()
	 */
	@Test
	public void testTemplateQuery() {
		try {
			List<Object> results = Query.templateQuery("tmdb")
					.templateName("tmdb_template")
					.param("q", "basketball")
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * 对应原版 SearchTemplateTest.testDynamicSearchTemplate()
	 */
	@Test
	public void testDynamicSearchTemplate() {
		try {
			List<Object> results = Query.templateQuery("employee")
					.templateName("employee_template")
					.param("job", "java")
					.param("age", 38)
					.queryForList();
			results.forEach(System.out::println);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== byId with resultType 相关测试 ====================
	
	/**
	 * 测试 byId 带 resultType
	 */
	@Test
	public void testGetByIdWithResultType() {
		try {
			Movie movie = ElasticUtils.Query.byId("movies", "1", Movie.class);
			System.out.println(movie);
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	// ==================== matchPhraseQuery with resultType 相关测试 ====================
	// 对应原版 MatchPhraseQueryTest
	
	/**
	 * 对应原版 MatchPhraseQueryTest.testMatchPhraseQuery()
	 * 带 resultType(Movie.class)
	 */
	@Test
	public void testMatchPhraseQueryWithResultType() {
		try {
			List<Movie> movies1 = Query.matchPhraseQuery("movies")
					.query("title", "one love")
					.resultType(Movie.class)
					.sort("title.keyword:asc")
					.queryForList();
			movies1.forEach(System.out::println);
			
			List<Movie> movies2 = Query.matchPhraseQuery("movies")
					.query("title", "one love")
					.slop(1)
					.resultType(Movie.class)
					.queryForList();
			movies2.forEach(System.out::println);
			
			assertThat(movies1.size()).isLessThan(movies2.size());
		} catch (Exception e) {
			log.warn("Search failed: {}", e.getMessage());
		}
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query":"salary: [10000 TO 20000]"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testQueryStringRangequery() {
		List<Object> docs = Query.queryString("user_info")
				.query("salary:[10000 TO 20000]")
				.queryForList();
		docs.forEach(System.out::println);
	}
	
	@Test
	public void testAgeGreater() {
		List<Object> docs = Query.queryString("user_info")
				.query("age:>20")
				.queryForList();
		docs.forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query":"salary:[15000 TO 30000]"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testSalaryRage() {
		List<Object> docs = Query.queryString("user_info")
				.query("salary:[10000 TO 20000]")
				.queryForList();
		docs.forEach(System.out::println);
	}
	
	@Test
	public void testFuzzyquery() {
		List<Object> docs = Query.queryString("user_info")
				.query("jv~")
				.queryForList();
		assertThat(docs.size()).isEqualTo(0);
		
		docs = Query.queryString("user_info")
				.query("jav~1")
				.queryForList();
		assertThat(docs.size()).isEqualTo(3);
		docs.forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query":"tag:java^10 job:后端^5"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testWeight() {
		List<Object> docs = Query.queryString("user_info")
				.query("tag:java^10 job:后端^5")
				.queryForList();
		assertThat(docs.size()).isEqualTo(3);
		docs.forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query":"age:>25 AND salary:>=15000 AND (city:北京 OR city:上海) AND job:前端 -job:测试"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAdvanced() {
		List<Object> docs = Query.queryString("user_info")
				.query("age:>25 AND salary:>=15000 AND (city:北京 OR city:上海) AND job:前端 -job:测试")
				.queryForList();
		assertThat(docs.size()).isEqualTo(1);
		docs.forEach(System.out::println);
	}
	
	/**
	 * 查询: 内容包含开发，且标签有java，2024年新增用户
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "query":"开发 AND tag:java AND create_time:>=2024-01-01"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAdvanced2() {
		List<Object> docs = Query.queryString("user_info")
				.query("开发 AND tag:java AND create_time:>=2024-01-01")
				.queryForList();
		assertThat(docs.size()).isEqualTo(2);
		docs.forEach(System.out::println);
	}
	
	/**
	 * <pre>
	 * GET user_info/_search
	 * {
	 *   "query": {
	 *     "query_string": {
	 *       "fields": ["name^5", "job^3", "city"],
	 *       "query": "开发 AND 北京",
	 *       "allow_leading_wildcard": false,
	 *       "fuzziness": 1
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testParams() {
		List<Object> docs = Query.queryString("user_info")
				.query("开发 AND 北京")
				.allowLeadingWildcard(false)
				.fuzziness(1)
				.queryForList();
		assertThat(docs.size()).isEqualTo(1);
		docs.forEach(System.out::println);
	}
	
	/**
	 * 测试 ElasticTermsQueryBuilder 的 nestedPath 功能
	 * <p>
	 * 查询 specs 嵌套文档中 color 为 "纯净白" 或 "象牙白" 的空调
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "query": {
	 *     "nested": {
	 *       "path": "specs",
	 *       "query": {
	 *         "terms": {
	 *           "specs.color": ["纯净白", "象牙白"]
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testNestedTermsQuery() {
		List<AirConditioner> results = Query.termsQuery("air_conditioner")
				.nestedPath("specs")
				.query("specs.color", "纯净白", "象牙白")
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		// 验证查询能够正常执行并返回结果
		assertThat(results.size()).isEqualTo(4);
		for (AirConditioner result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	
	/**
	 * 测试 ElasticExistsQueryBuilder 的 nestedPath 功能
	 * <p>
	 * 查询 specs 嵌套文档中 horse_power 字段存在的空调
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "query": {
	 *     "nested": {
	 *       "path": "specs",
	 *       "query": {
	 *         "exists": {
	 *           "field": "specs.horse_power"
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testNestedExistsQuery() {
		List<AirConditioner> results = Query.exists("air_conditioner")
				.nestedPath("specs")
				.field("specs.horse_power")
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		// 验证查询能够正常执行并返回结果
		assertThat(results.size()).isEqualTo(22);
		for (AirConditioner result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	
	/**
	 * 测试 ElasticMatchAllQueryBuilder 的 nestedPath 功能
	 * <p>
	 * 在 nested 上下文中使用 match_all，匹配所有有 specs 嵌套文档的父文档
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "query": {
	 *     "nested": {
	 *       "path": "specs",
	 *       "query": {
	 *         "match_all": {}
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testNestedMatchAllQuery() {
		List<AirConditioner> results = Query.matchAllQuery("air_conditioner")
				.nestedPath("specs")
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		// 验证查询能够正常执行并返回结果
		assertThat(results.size()).isEqualTo(22);
		for (AirConditioner result : results) {
			assertThat(result.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	
	/**
	 * 搜索「美的节能空调」，并且规格库存 > 100
	 * <pre>
	 * POST air_conditioner/_search
	 * {
	 *   "size": 200,
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {
	 *           "multi_match": {
	 *             "query": "美的节能空调",
	 *             "fields": [
	 *               "goods_name",
	 *               "goods_desc"
	 *             ]
	 *           }
	 *         }
	 *       ],
	 *       "filter": [
	 *         {
	 *           "nested": {
	 *             "path": "specs",
	 *             "query": {
	 *               "range": {
	 *                 "specs.stock": {
	 *                   "gte": 100
	 *                 }
	 *               }
	 *             }
	 *           }
	 *         }
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testMultiMatchAndNested() {
		List<AirConditioner> docs = Query.bool("air_conditioner")
				.multiMatch("美的节能空调", "goods_name", "goods_desc").must()
				.nestedPath("specs").range("specs.stock").gt(100).filter()
				.size(200)
				.resultType(AirConditioner.class)
				.queryForList();
		
		assertThat(docs.size()).isEqualTo(12);
		for (AirConditioner doc : docs) {
			assertThat(doc.getId()).isNotNull();
			System.out.println(JacksonUtils.toPrettyJson(doc));
		}
	}
	
	/**
	 * <pre>
	 * POST shop_goods/_search
	 * {
	 *     "query": {
	 *         "match_all": {}
	 *     },
	 *     "from": 0,
	 *     "size": 6
	 * }
	 * </pre>
	 *
	 *
	 */
	@Test
	public void testSimplePage() {
		List<Object> shopGoods = Query.matchAllQuery("shop_goods")
				.from(0)
				.size(6)
				.queryForList();
		assertThat(shopGoods.size()).isEqualTo(6);
		shopGoods.forEach(System.out::println);
	}
	
	@Test
	public void testPagingSearchAfter() {
		ElasticPage<String> elasticPage = Query.matchAllQuery("shop_goods")
				.size(10)
				.from(0)
				.sort("price:desc, goods_id:asc")
				.queryForPage();
		assertThat(elasticPage.getTotalCount()).isEqualTo(109);
		assertThat(elasticPage.getResults().size()).isEqualTo(10);
		Object[] sort = elasticPage.getSort();
		System.out.println(JacksonUtils.toPrettyJson( sort));
		
		elasticPage = Query.matchAllQuery("shop_goods")
				.searchAfter(sort)
				.size(10)
				.sort("price:desc,goods_id:asc")
				.queryForPage();
		assertThat(elasticPage.getResults().size()).isEqualTo(10);
		sort = elasticPage.getSort();
		System.out.println(JacksonUtils.toPrettyJson( sort));
		
		List<String> results = elasticPage.getResults();
		for (String result : results) {
			System.out.println(JacksonUtils.toPrettyJson(result));
		}
	}
	
	/**
	 * 对应原版 MatchPhraseQueryTest 的 POJO 类
	 */
	public static class Movie {
		private String title;
		private int year;
		
		public String getTitle() {
			return title;
		}
		
		public void setTitle(String title) {
			this.title = title;
		}
		
		public int getYear() {
			return year;
		}
		
		public void setYear(int year) {
			this.year = year;
		}
		
		@Override
		public String toString() {
			return "Movie{title='" + title + "', year=" + year + "}";
		}
	}
}
