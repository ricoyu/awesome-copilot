package com.awwesomecopilot.orm;

import org.junit.jupiter.api.Test;

import static com.awesomecopilot.orm.utils.SQLWhereCleaner.cleanInvalidWhere;
import static com.awesomecopilot.orm.utils.SQLWhereCleaner.cleanWhereAndOr;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLWhereCleanerTest {

	@Test
	public void test1() {
		String sql1 = "select * from pms_attr_group where";
		String sql1where = cleanInvalidWhere(sql1);
		assertEquals("select * from pms_attr_group", sql1where);
		System.out.println("处理前sql1：" + sql1);
		System.out.println("处理后sql1：" + cleanInvalidWhere(sql1));
		System.out.println("--------------------------");

		String sql1WhereAnd = cleanWhereAndOr(sql1);
		assertEquals("select * from pms_attr_group", sql1WhereAnd);
		System.out.println("处理前sql1：" + sql1);
		System.out.println("处理后sql1：" + sql1WhereAnd);
	}

	@Test
	public void test2() {
		String sql2 = "select * from pms_attr_group where order by `sort` asc, attr_group_name asc limit ?";
		String sql2where = cleanInvalidWhere(sql2);
		assertEquals("select * from pms_attr_group order by `sort` asc, attr_group_name asc limit ?", sql2where);
		System.out.println("处理前sql1：" + sql2);
		System.out.println("处理后sql1：" + sql2where);
		System.out.println("--------------------------");

		String sql1WhereAnd = cleanWhereAndOr(sql2);
		assertEquals("select * from pms_attr_group order by `sort` asc, attr_group_name asc limit ?", sql1WhereAnd);
		System.out.println("处理前sql1：" + sql2);
		System.out.println("处理后sql1：" + sql1WhereAnd);
	}

	@Test
	public void test3() {
		// 测试用例3：where后多个空格+and，再无其他条件
		String sql3 = "select * from user where    and order by create_time";
		String sqlwhere = cleanInvalidWhere(sql3);
		assertEquals("select * from user order by create_time", sqlwhere);
		System.out.println("处理前sql3：" + sql3);
		System.out.println("处理后sql3：" + sqlwhere);
		System.out.println("--------------------------");

		String sqlWhereAnd = cleanWhereAndOr(sql3);
		assertEquals("select * from user order by create_time", sqlWhereAnd);
		System.out.println("处理前sql3：" + sql3);
		System.out.println("处理后sql3：" + sqlWhereAnd);
	}

	@Test
	public void test4() {
		// 测试用例4：where后有有效条件（不处理）
		String sql4 = "select * from user where name = 'test' order by age";
		String sqlwhere = cleanInvalidWhere(sql4);
		assertEquals("select * from user where name = 'test' order by age", sqlwhere);
		System.out.println("处理前sql4：" + sql4);
		System.out.println("处理后sql4：" + sqlwhere);
		System.out.println("--------------------------");

		String sqlWhereAnd = cleanWhereAndOr(sql4);
		assertEquals("select * from user where name = 'test' order by age", sqlWhereAnd);
		System.out.println("处理前sql4：" + sql4);
		System.out.println("处理后sql4：" + sqlWhereAnd);
	}

	@Test
	public void test5() {
		// 测试用例5：where后换行+or，再跟limit
		String sql5 = "select * from order where\n   or limit 10";
		String sqlwhere = cleanInvalidWhere(sql5);
		assertEquals("select * from order limit 10", sqlwhere);
		System.out.println("处理前sql5：" + sql5);
		System.out.println("处理后sql5：" + sqlwhere);
		System.out.println("--------------------------");
		String sqlWhereAnd = cleanWhereAndOr(sql5);
		assertEquals("select * from order limit 10", sqlWhereAnd);
		System.out.println("处理前sql5：" + sql5);
		System.out.println("处理后sql5：" + sqlWhereAnd);
	}

	@Test
	public void test6() {
		String sql6 = """
				select * from pms_attr_group where
				                        order by `sort` asc, attr_group_name asc limit ?""";
		String sqlwhere = cleanInvalidWhere(sql6);
		assertEquals("select * from pms_attr_group order by `sort` asc, attr_group_name asc limit ?", sqlwhere);
		System.out.println("处理前sql6：" + sql6);
		System.out.println("处理后sql6：" + sqlwhere);
		System.out.println("--------------------------");
		String sqlWhereAnd = cleanWhereAndOr(sql6);
		assertEquals("select * from pms_attr_group order by `sort` asc, attr_group_name asc limit ?", sqlWhereAnd);
		System.out.println("处理前sql6：" + sql6);
		System.out.println("处理后sql6：" + sqlWhereAnd);
	}

	@Test
	public void test7() {
		String sql7 = """
				select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
				left join pms_category c on a.catelog_id = c.cat_id
				LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
				LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
				where
				                        order by agr.attr_group_id asc, agr.attr_sort""";
		String sqlwhere = cleanInvalidWhere(sql7);
		assertEquals("select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id order by agr.attr_group_id asc, agr.attr_sort", sqlwhere);
		System.out.println("处理前sql7：" + sql7);
		System.out.println("处理后sql7：" + sqlwhere);
		System.out.println("--------------------------");
		String sqlWhereAnd = cleanWhereAndOr(sql7);
		assertEquals("select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id order by agr.attr_group_id asc, agr.attr_sort", sqlWhereAnd);
		System.out.println("处理前sql7：" + sql7);
		System.out.println("处理后sql7：" + sqlWhereAnd);
	}
}
