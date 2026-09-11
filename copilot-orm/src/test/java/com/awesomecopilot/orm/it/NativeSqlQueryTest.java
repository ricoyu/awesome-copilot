package com.awesomecopilot.orm.it;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 原生 SQL 查询场景: query(String) / findOne / findList / findPage / 命名查询 / 结果映射 / 排序
 *
 * @author Rico Yu
 */
class NativeSqlQueryTest extends AbstractOrmIntegrationTest {

	@Test
	@DisplayName("query(sql) 传完整 SQL + addParam 命名参数绑定")
	void rawSqlWithParam() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book where author = :author")
				.addParam("author", "周志明")
				.resultClass(Book.class)
				.findList();
		assertEquals(1, result.size());
		assertEquals("深入理解JVM", result.get(0).getName());
	}

	@Test
	@DisplayName("addParams(Map) 批量参数")
	void rawSqlWithParamsMap() {
		seedStandardBooks();
		Map<String, Object> params = new HashMap<>();
		params.put("author", "Bruce");
		params.put("maxPrice", 100);
		List<Book> result = jpaDao.query("select * from book where author = :author and price < :maxPrice")
				.addParams(params)
				.resultClass(Book.class)
				.findList();
		assertEquals(1, result.size());
	}

	@Test
	@DisplayName("findOne(queryName, paramName, value, clazz) 返回单条")
	void findOneByName() {
		seedStandardBooks();
		Book book = jpaDao.findOne("select * from book where name = :name", "name", "Clean Code", Book.class);
		assertNotNull(book);
		assertEquals("Robert", book.getAuthor());
	}

	@Test
	@DisplayName("findOne 查不到返回 null")
	void findOneNotFoundReturnsNull() {
		seedStandardBooks();
		Book book = jpaDao.findOne("select * from book where name = :name", "name", "不存在", Book.class);
		assertNull(book);
	}

	@Test
	@DisplayName("findList(queryName, Map, clazz) 多条件查询")
	void findListWithMap() {
		seedStandardBooks();
		Map<String, Object> params = new HashMap<>();
		params.put("maxPrice", 80);
		List<Book> result = jpaDao.findList(
				"select * from book where price < :maxPrice order by price asc", params, Book.class);
		assertEquals(2, result.size());
		assertEquals("Clean Code", result.get(0).getName());
	}

	@Test
	@DisplayName("命名查询 @NamedNativeQuery 解析 + 参数绑定")
	void namedNativeQuery() {
		seedStandardBooks();
		List<Book> result = jpaDao.findList("Book.findByExactName",
				"name", "深入理解JVM", Book.class);
		assertEquals(1, result.size());

		// 第二个命名查询: 两个条件 OR
		Map<String, Object> params = new HashMap<>();
		params.put("maxPrice", 70);
		params.put("minStock", 100);
		List<Book> cheapOrPopular = jpaDao.findList("Book.findCheapOrPopular", params, Book.class);
		// price<70 的只有 Clean Code(66); Java编程思想 stock=100 不满足 >100
		assertEquals(1, cheapOrPopular.size());
		assertEquals("Clean Code", cheapOrPopular.get(0).getName());
	}

	@Test
	@DisplayName("addlikeParam 自动加 % 模糊匹配")
	void likeParam() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book where name like :name")
				.addlikeParam("name", "Java")
				.resultClass(Book.class)
				.findList();
		// 含 "Java" 字样的: Java编程思想 / Java并发实战 / Effective Java, 共 3 本
		assertEquals(3, result.size());
	}

	@Test
	@DisplayName("addRlikeParam 右模糊(前缀匹配)")
	void rlikeParam() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book where name like :name")
				.addRlikeParam("name", "Java")
				.resultClass(Book.class)
				.findList();
		assertEquals(2, result.size());
	}

	@Test
	@DisplayName("resultClass 映射: snake_case→驼峰 + BigDecimal→int + 字符串→枚举")
	void resultClassMapping() {
		seedStandardBooks();
		List<BookSummary> result = jpaDao.query(
				"select name as book_name, author, price, status from book where name = :name")
				.addParam("name", "深入理解JVM")
				.resultClass(BookSummary.class)
				.findList();
		assertEquals(1, result.size());
		BookSummary summary = result.get(0);
		assertEquals("深入理解JVM", summary.getBookName());
		assertEquals("周志明", summary.getAuthor());
		assertEquals(88, summary.getPrice(), "BigDecimal 88.00 应转成 int 88");
		assertEquals(BookStatus.PUBLISHED, summary.getStatus());
	}

	@Test
	@DisplayName("findPage 原生 SQL 分页 + autoCount 总记录数")
	void nativeFindPage() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(2);
		page.setPageSize(2);
		List<Book> result = jpaDao.query("select * from book")
				.resultClass(Book.class)
				.page(page)
				.findPage();
		assertEquals(2, result.size());
		assertEquals(5, page.getTotalCount(), "autoCount 应回填总数");
	}

	@Test
	@DisplayName("findPage 配合 order 排序后分页")
	void nativeFindPageWithOrder() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(3);
		List<Book> result = jpaDao.query("select * from book order by price asc")
				.resultClass(Book.class)
				.page(page)
				.findPage();
		assertEquals(3, result.size());
		assertEquals("Clean Code", result.get(0).getName(), "最便宜排第一页首位");
	}

	@Test
	@DisplayName("order(String) 字符串排序, 支持 '字段:方向' 格式")
	void orderByString() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book")
				.order("price:desc")
				.resultClass(Book.class)
				.findList();
		assertEquals("108.00", result.get(0).getPrice().toString());
	}

	@Test
	@DisplayName("order(OrderBean) 显式排序对象")
	void orderByOrderBean() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book")
				.order(new OrderBean("price", OrderBean.DIRECTION.ASC))
				.resultClass(Book.class)
				.findList();
		assertEquals("66.00", result.get(0).getPrice().toString());
	}

	@Test
	@DisplayName("空 IN 集合: 返回 0 行而不是报类型转换错误")
	void emptyInCollectionReturnsEmpty() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book where id in (:ids)")
				.addParam("ids", List.of())
				.resultClass(Book.class)
				.findList();
		assertEquals(0, result.size(), "空 IN 应恒假返回空, 而不是绑成字符串 '' 报错");
	}

	@Test
	@DisplayName("IN 查询: 数组参数自动转 List")
	void inQueryWithArray() {
		seedStandardBooks();
		List<Book> result = jpaDao.query("select * from book where author in (:authors)")
				.addParam("authors", new String[]{"Bruce", "Robert"})
				.resultClass(Book.class)
				.findList();
		assertEquals(2, result.size());
	}

	@Test
	@DisplayName("SQL 自带 limit 走 findPage: limit 被剥离, 分页接管")
	void findPageStripsNativeLimit() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(2);
		// 旧实现会拼出 "order by ... limit 99" 导致语法错误
		List<Book> result = jpaDao.query("select * from book order by price limit 99")
				.resultClass(Book.class)
				.page(page)
				.findPage();
		assertEquals(2, result.size(), "limit 99 应被剥离, 分页返回前 2 条");
	}

	@Test
	@DisplayName("null 参数被剔除, 不污染调用方传入的 Map")
	void nullParamStrippedAndCallerMapIntact() {
		seedStandardBooks();
		Map<String, Object> params = new HashMap<>();
		params.put("author", "周志明");
		params.put("unused", null); // 这个键值为 null, 应被内部剔除但不动原 map

		List<Book> result = jpaDao.findList("select * from book where author = :author", params, Book.class);
		assertEquals(1, result.size());
		// 关键: 调用方的 map 不能被改坏(旧实现会删掉 null 键)
		assertTrue(params.containsKey("unused"), "调用方的 map 不应被修改");
	}

	@Test
	@DisplayName("findRawList 查询单列原始值")
	void findRawList() {
		seedStandardBooks();
		List<String> names = jpaDao.findRawList(
				"select name from book where author = :author", "author", "周志明");
		assertEquals(1, names.size());
		assertEquals("深入理解JVM", names.get(0));
	}

	@Test
	@DisplayName("execute 执行 UPDATE/DELETE 并返回影响行数")
	void executeUpdate() {
		seedStandardBooks();
		jpaDao.begin();
		int updated = jpaDao.execute(
				"update book set stock = stock + 10 where author = :author",
				"author", "周志明");
		jpaDao.commit();
		assertEquals(1, updated);
	}

	@Test
	@DisplayName("findOne 多列聚合函数返回标量")
	void findOneScalar() {
		seedStandardBooks();
		Object count = jpaDao.findOne("select count(*) from book");
		assertNotNull(count);
	}

	@Test
	@DisplayName("列名含关键字子串(order_no)在 select 字段/where/order by 三处都不被改坏")
	void keywordSubstringColumnName() {
		seedStandardBooks();
		// 给每条书补一个 order_no, 让 order_no 列有真实值可查
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery("update book set order_no = 'ORD-' || id").executeUpdate();
		seedEm.getTransaction().commit();

		// 1. select 字段含 order_no: 结果映射后能取到 orderNo(不被 SQL 自动修复改坏)
		List<Book> bySelect = jpaDao.query("select id, name, order_no from book where name = :name")
				.addParam("name", "深入理解JVM")
				.resultClass(Book.class)
				.findList();
		assertEquals(1, bySelect.size());
		assertNotNull(bySelect.get(0).getOrderNo(), "select order_no 应正常映射, 不被关键字替换改坏");
		assertTrue(bySelect.get(0).getOrderNo().startsWith("ORD-"));

		// 2. where 条件用 order_no 过滤
		List<Book> byWhere = jpaDao.query("select * from book where order_no like :no")
				.addlikeParam("no", "ORD-")
				.resultClass(Book.class)
				.findList();
		assertEquals(5, byWhere.size(), "where order_no like 应查出全部 5 条");

		// 3. order by order_no 排序
		List<Book> byOrder = jpaDao.query("select id, order_no from book order by order_no desc")
				.resultClass(Book.class)
				.findList();
		assertEquals(5, byOrder.size(), "order by order_no 应正常排序, 不被当关键字吃掉");
	}

	@Test
	@DisplayName("列名以 or 开头(orderno 无下划线)不被 cleanEmptyWhere 剥前缀")
	void ordernoWithoutUnderscore() {
		// 直接用原生 SQL 建一张带 orderno 列的表来测最贴近生产的情况
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery("create table if not exists t_orderno(id bigint primary key, orderno varchar(50), " +
				"created_at timestamp)").executeUpdate();
		seedEm.createNativeQuery("insert into t_orderno(id, orderno) values (1, 'A001'), (2, 'A002')").executeUpdate();
		seedEm.getTransaction().commit();

		List<Object[]> rows = jpaDao.query("select id, orderno from t_orderno where orderno = :no")
				.addParam("no", "A001")
				.findList();
		assertEquals(1, rows.size(), "orderno 列名(以 or 开头)不应被当成 WHERE 空余连接词剥掉");
	}
}
