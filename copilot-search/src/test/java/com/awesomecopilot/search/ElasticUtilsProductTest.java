package com.awesomecopilot.search;

import com.awesomecopilot.search.builder.agg.sub.SubAggregations;
import com.awesomecopilot.search.builder.query.ElasticTermQueryBuilder;
import com.awesomecopilot.search.enums.CalendarInterval;
import com.awesomecopilot.search.vo.ElasticPage;
import com.awesomecopilot.search.vo.ElasticScroll;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;
import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsProductTest {

	@Test
	public void testMatchQuery() {
		List<String> products = ElasticUtils.Query.matchQuery("products")
				.query("description", "蓝牙 降噪")
				.queryForList();

		assertThat(products.size()).isEqualTo(2);
		for (String product : products) {
			System.out.println(product);
		}
	}

	@Test
	public void testMUltiMatch() {
		List<String> products = ElasticUtils.Query.multiMatch("products")
				.query("智能", "name", "description")
				.queryForList();
		assertThat(products.size()).isEqualTo(2);
		for (String product : products) {
			System.out.println(product);
		}
	}

	@Test
	public void testTermQuery() {
		List<Product> products = ElasticUtils.Query.termQuery("products")
				.query("categories", "audio")
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(2);
		for (Product product : products) {
			System.out.println(toPrettyJson(product));
		}
	}

	@Test
	public void testRangeQuery() {
		List<Product> products = ElasticUtils.Query
				.range("products")
				.field("price")
				.gte("100")
				.lte("300")
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(2);
		for (Product product : products) {
			System.out.println(toPrettyJson(product));
		}
	}

	@Test
	public void testBoolQuery() {
		List<Object> products = ElasticUtils.Query.bool("products")
				.match("description", "智能").must()
				.range("price").gte(200).filter()
				.term("is_active", true).filter()
				.term("tags", "new").should()
				.term("tags", "popular").should()
				.minimumShouldMatch(1)
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(2);
		for (Object product : products) {
			System.out.println(toPrettyJson(product));
		}
	}

	@Test
	public void testNested() {
		List<Object> products = ElasticUtils.Query.bool("products")
				.nestedPath("variants")
				.term("variants.color", "黑色").must()
				.range("variants.price").lte(300).must()
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(3);
		for (Object product : products) {
			System.out.println(toPrettyJson(product));
		}
	}

	@Test
	public void testMatchPhrasePrefix() {
		List<Product> products = ElasticUtils.Query.matchPhrasePrefixQuery("products")
				.maxExpansions(10)
				.query("description", "高性能智")
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(1);
		for (Product product : products) {
			System.out.println(toPrettyJson(product));
		}
	}
	
	@Test
	public void testGeoDistance() {
		List<Product> products = ElasticUtils.Query.geoDistance("products")
				.location(40.7128, -74.0060)
				.distance("1000m")
				.resultType(Product.class)
				.queryForList();

		assertThat(products.size()).isEqualTo(1);
		for (Product product : products) {
			System.out.println(toPrettyJson(product));
		}
	}
	
	@Test
	public void testDateHistogram() {
		Map<String, Object> resultMap = ElasticUtils.Aggs.dateHistogram("products")
				.of("date_histogram", "created_at")
				.calendarInterval(CalendarInterval.MONTH)
				.format("yyyy-MM")
				.get();
		System.out.println(toPrettyJson(resultMap));
	}

	@Test
	public void testQueryWithAgg() {
		ElasticTermQueryBuilder queryBuilder = ElasticUtils.Query.termQuery("products").query("categories", "computers");
		List<Map<String, Object>> resultMap = ElasticUtils.Aggs
				.terms("products")
				.of("categories", "categories")
				.subAggregation(SubAggregations.avg("avg_price", "price"))
				.subAggregation(SubAggregations.topHits("top_products").size(1).sort("rating:desc"))
				.setQuery(queryBuilder)
				.get();

		String prettyJson = toPrettyJson(resultMap);
		System.out.println(prettyJson);
	}

	@Test
	public void testNestedAgg() {
		List<Map<String, Object>> resultMap = ElasticUtils.Aggs.terms("products")
				.nestedPath("variants", "variants")
				.of("colors", "variants.color")
				.get();

		System.out.println(toPrettyJson(resultMap));
	}

	@Test
	public void testSimplePaging() {
		List<Object> products = ElasticUtils.Query.matchAllQuery("products")
				.from(1)
				.size(5)
				.sort("price:asc,_id:asc")
				.queryForList();

		assertThat(products.size()).isEqualTo(5);
		for (Object product : products) {
			System.out.println(toPrettyJson(product));
		}
	}

	@Test
	public void testSearchAfter() {
		ElasticPage<Object> elasticPage = ElasticUtils.Query.range("products")
				.field("price")
				.gte(100)
				.size(3)
				.sort("price:asc,_id:asc")
				.queryForPage();

		elasticPage.getResults().forEach(System.out::println);

		elasticPage = ElasticUtils.Query.range("products")
				.field("price")
				.gte(100)
				.size(3)
				.sort("price:asc,_id:asc")
				.searchAfter(elasticPage.getSort())
				.queryForPage();
		elasticPage.getResults().forEach(System.out::println);
	}

	@Test
	public void test4Scroll() {
		ElasticScroll<Object> elasticScroll = ElasticUtils.Query.range("products")
				.field("price")
				.size(2)
				.scroll(1, TimeUnit.MINUTES)
				.queryForScroll();

		elasticScroll.getResults().forEach(System.out::println);

		elasticScroll = ElasticUtils.Query.scrollQuery("products")
				.scrollId(elasticScroll.getScrollId())
				.queryForScroll();
		elasticScroll.getResults().forEach(System.out::println);
	}
}
