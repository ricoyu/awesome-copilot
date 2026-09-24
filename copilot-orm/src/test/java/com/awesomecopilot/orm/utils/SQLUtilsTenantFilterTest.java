package com.awesomecopilot.orm.utils;

import com.awesomecopilot.common.lang.context.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQLUtils.addDeleteTenantIdCondition 的 AST 级过滤回归（评估报告 P0-7 次因）。
 * <p>
 * 原缺陷：
 * ① 追加的 deleted / tenant_id 条件不带表别名——join 两表都有 deleted 列时
 * SQL 报列名歧义；
 * ② "已包含条件"的判断用整段 SQL 文本 contains("deleted=")——字符串字面量里
 * 出现 deleted=1 就误判为已过滤而漏加；
 * ③ 子查询（IN/EXISTS 内的 select）不递归追加——跨租户判断拿到错的结果。
 * <p>
 * 修复后要求：按 FROM/JOIN 的每张表（有别名用别名）逐个追加限定条件，
 * 判断是否已过滤改为 AST 级（该表确实已有 <别名.>deleted 比较），并递归所有子查询。
 *
 * @author Rico Yu
 */
class SQLUtilsTenantFilterTest {
	
	@BeforeEach
	void configure() {
		SQLUtils.configureLogicalDelete(true, "deleted");
		ThreadContext.put("tenantId", 42L);
		SQLUtils.clearCache();
	}
	
	@AfterEach
	void cleanup() {
		ThreadContext.put("tenantId", null);
		SQLUtils.configureLogicalDelete(false, "deleted");
		SQLUtils.clearCache();
	}
	
	private String rewrite(String sql) {
		return SQLUtils.addDeleteTenantIdCondition(sql).toLowerCase();
	}
	
	@Test
	@DisplayName("join 两表: 条件按别名限定, 每张表都有各自的 deleted 与 tenant_id")
	void joinTablesGetQualifiedConditions() {
		String out = rewrite("select * from orders o join users u on o.user_id = u.id where o.status = 1");
		assertTrue(out.contains("o.deleted"), "orders 的条件应用别名限定: " + out);
		assertTrue(out.contains("u.deleted"), "users 的条件应用别名限定: " + out);
		assertTrue(out.contains("o.tenant_id = 42"), "orders 缺 tenant_id: " + out);
		assertTrue(out.contains("u.tenant_id = 42"), "users 缺 tenant_id: " + out);
	}
	
	@Test
	@DisplayName("单表无别名: 用表名限定")
	void singleTableQualifiedByTableName() {
		String out = rewrite("select * from orders where status = 1");
		assertTrue(out.contains("orders.deleted"), "应按表名限定: " + out);
		assertTrue(out.contains("orders.tenant_id = 42"), "应按表名限定: " + out);
	}
	
	@Test
	@DisplayName("IN 子查询内的表也要过滤(递归改写)")
	void inSubqueryIsFiltered() {
		String out = rewrite("select * from orders o where o.id in "
				+ "(select oi.order_id from order_items oi where oi.qty > 2)");
		assertTrue(out.contains("oi.deleted"), "子查询表 order_items 未被过滤: " + out);
		assertTrue(out.contains("oi.tenant_id = 42"), "子查询表缺 tenant_id: " + out);
		assertTrue(out.contains("o.deleted"), "外层表未被过滤: " + out);
	}
	
	@Test
	@DisplayName("EXISTS 子查询同样递归过滤")
	void existsSubqueryIsFiltered() {
		String out = rewrite("select * from orders o where exists "
				+ "(select 1 from order_items oi where oi.order_id = o.id)");
		assertTrue(out.contains("oi.deleted"), "EXISTS 子查询未被过滤: " + out);
	}
	
	@Test
	@DisplayName("字符串字面量含 deleted=1 不算已有过滤条件(文本 contains 误判)")
	void stringLiteralDoesNotCountAsExistingFilter() {
		String out = rewrite("select * from orders o where o.remark = 'deleted=1'");
		assertTrue(out.contains("o.deleted"), "字面量里的 deleted= 被误判为已过滤: " + out);
	}
	
	@Test
	@DisplayName("该表已有 <别名.deleted 比较时不重复追加")
	void existingQualifiedConditionNotDuplicated() {
		String out = rewrite("select * from orders o where o.deleted = 0");
		assertTrue(out.contains("o.tenant_id = 42"), "tenant_id 仍应追加: " + out);
		int first = out.indexOf("deleted = 0");
		int last = out.lastIndexOf("deleted = 0");
		assertTrue(first == last, "deleted 条件不应出现两次: " + out);
	}
	
	@Test
	@DisplayName("update 语句按表名限定追加")
	void updateGetsQualifiedConditions() {
		String out = rewrite("update orders set status = 2 where id = 5");
		assertTrue(out.contains("orders.deleted"), "update 应带表名限定的 deleted 条件: " + out);
		assertTrue(out.contains("orders.tenant_id = 42"), "update 应带表名限定的 tenant_id: " + out);
	}
	
	@Test
	@DisplayName("delete 语句按表名限定追加")
	void deleteGetsQualifiedConditions() {
		String out = rewrite("delete from orders where id = 5");
		assertTrue(out.contains("orders.deleted"), "delete 应带表名限定的 deleted 条件: " + out);
		assertTrue(out.contains("orders.tenant_id = 42"), "delete 应带表名限定的 tenant_id: " + out);
	}
	
	@Test
	@DisplayName("left join 右表也有限定条件(不过滤右表 deleted 会带出已删关联行)")
	void leftJoinRightTableFiltered() {
		String out = rewrite("select * from orders o left join users u on o.user_id = u.id");
		assertTrue(out.contains("o.deleted"), "左表缺条件: " + out);
		assertTrue(out.contains("u.deleted"), "右表缺条件: " + out);
	}
	
	// ===== 独立评审(deleg_344d236e)发现项的回归用例 =====
	
	@Test
	@DisplayName("F1: 原条件含 OR 时必须整体加括号, 过滤不许吃进 OR 分支")
	void orBranchDoesNotLeakDeletedRows() {
		String out = rewrite("select id from orders where name = 'b' or id = 1");
		// 括号包住原条件: (name = 'b' OR id = 1) AND orders.deleted = 0 ...
		assertTrue(out.contains("(name = 'b' or id = 1)"), "OR 原条件未被整体括号包裹: " + out);
		int orPos = out.indexOf(" or ");
		int andPos = out.indexOf(" and orders.deleted");
		assertTrue(andPos > out.indexOf("(name"), "deleted 条件落在 OR 分支内: " + out);
		assertTrue(orPos < andPos, "AND 过滤条件应在 OR 括号之后: " + out);
	}
	
	@Test
	@DisplayName("F3: 缓存键不许把 tenantId 直接拼在 SQL 尾部(数字字面量会造成串键)")
	void cacheKeyIsNotAmbiguous() {
		// 注意: 两条查询之间不能清缓存——旧实现键=sql+tenantId 拼接,
		// "where id = 1"+租户2 与 "where id = 12"+无租户 会得到同一个键
		ThreadContext.put("tenantId", 2L);
		String a = rewrite("select id from orders where id = 1");
		ThreadContext.put("tenantId", null);
		String b = rewrite("select id from orders where id = 12");
		assertFalse(b.contains("tenant_id"), "无租户的查询被串进了别家的 tenant_id: " + b);
		assertTrue(a.contains("orders.tenant_id = 2"));
	}
	
	@Test
	@DisplayName("F4: WITH 定义的 CTE 名不是物理表, 不许对它追加条件")
	void cteNameIsNotFiltered() {
		String out = rewrite("with c as (select id, name from orders where id > 0) select id from c");
		assertFalse(out.contains("c.deleted"), "CTE 名 c 被当物理表加了条件: " + out);
		assertTrue(out.contains("orders.deleted"), "CTE 内部的物理表应被过滤: " + out);
	}
	
	@Test
	@DisplayName("F5: 多表 UPDATE 每张参与表都要有各自的限定条件")
	void multiTableUpdateFiltersAllTables() {
		String out = rewrite("update book b join product p on b.id = p.id set b.name = 'x' where p.id = 1");
		assertTrue(out.contains("b.deleted"), "主表 b 缺条件: " + out);
		assertTrue(out.contains("p.deleted"), "join 表 p 缺条件: " + out);
		assertTrue(out.contains("p.tenant_id = 42"), "join 表 p 缺 tenant_id: " + out);
	}
	
	@Test
	@DisplayName("F9: 反向写法 0 = o.deleted 也算已有条件, 不重复追加")
	void reversedComparisonRecognized() {
		String out = rewrite("select id from orders o where 0 = o.deleted");
		int first = out.indexOf("o.deleted");
		int last = out.lastIndexOf("o.deleted");
		assertTrue(first == last, "o.deleted 条件被重复追加: " + out);
		assertTrue(out.contains("o.tenant_id = 42"));
	}
}
