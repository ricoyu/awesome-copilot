package com.awwesomecopilot.orm;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.awesomecopilot.orm.utils.SQLUtils.build;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SQLUtilsTest {

	@Test
	@Order(1)
	public void test1() {
		long begin = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			String sql1 = "select * from sys_menu name='rico'";
			//System.out.println("sql1原始 " + sql1); // 输出: select * from sys_menu WHERE name='rico'
			//System.out.println("sql1    " + build(sql1)); // 输出: select * from sys_menu WHERE name='rico'
			String finalSql = build(sql1);
			//assertEquals("select * from sys_menu where name='rico'", build(sql1));
			//System.out.print("\n=============================\n");
		}
		long end = System.currentTimeMillis();
		System.out.println("耗时：" + (end - begin) + "ms");
	}

	@Test
	@Order(2)
	public void test2() {
		String sql2 = "select * from sys_menu where and name='rico'";
		System.out.println("sql2原始 " + sql2); // 输出: select * from sys_menu WHERE name='rico'
		System.out.println("sql2    " + build(sql2)); // 输出: select * from sys_menu WHERE name='rico'
		assertEquals("select * from sys_menu where name='rico'", build(sql2));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(3)
	public void test3() {
		String sql3 = "select * from sys_menu where name='rico' type=1";
		System.out.println("sql3原始 " + sql3); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		System.out.println("sql3    " + build(sql3)); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql3));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(4)
	public void test4() {
		// 额外测试：多条件
		String sql4 = "select * from sys_menu name='rico' type=1 status=active";
		System.out.println("sql4原始 " + sql4); // 输出: select * from sys_menu WHERE name='rico' AND type=1 AND
		// status=active
		System.out.println("sql4    " + build(sql4)); // 输出: select * from sys_menu WHERE name='rico' AND type=1 AND
		// status=active
		assertEquals("select * from sys_menu where name='rico' and type=1 and status=active", build(sql4));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(5)
	public void test5() {
		String sql5 = "select * from sys_menu where and name='rico' and and type=1";
		System.out.println("sql5原始 " + sql5); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		System.out.println("sql5    " + build(sql5)); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql5));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(6)
	public void test6() {
		String sql6 = "select * from sys_menu where id IN (select id from sys_user name='rico') type=1";
		System.out.println("sql6原始 " + sql6);
		System.out.println("sql6    " + build(sql6));
		assertEquals("select * from sys_menu where id in (select id from sys_user where name='rico') and type=1",
				build(sql6));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(7)
	public void test7() {
		String sql7 = "select * from sys_menu where type in (0, 1) order by menu_id asc, order_num asc";
		System.out.println("sql7原始 " + sql7);
		System.out.println("sql7    " + build(sql7));
		assertEquals("select * from sys_menu where type in (0, 1) order by menu_id asc, order_num asc",
				build(sql7));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(8)
	public void test8() {
		String sql8 =
				"select * from sys_menu name=:name type=:type parent_id = (select menu_id from sys_menu where " +
						"name=:parentName)";
		System.out.println("sql8原始 " + sql8);
		System.out.println("sql8    " + build(sql8));
		assertEquals("select * from sys_menu where name=:name and type=:type and parent_id = (select menu_id from " +
						"sys_menu where name=:parentName)",
				build(sql8));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(9)
	public void test9() {
		// 测试用例 9：大小写混乱 + 多余 and + 缺失 where
		String sql9 = "SELECT * FROM sys_menu WHERE AND name = 'rico' AND AND type=1 AND";
		System.out.println("sql9原始 " + sql9);
		System.out.println("sql9    " + build(sql9));
		assertEquals("select * from sys_menu where name = 'rico' and type=1", build(sql9));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(10)
	public void test10() {
		// 测试用例 10：完全没有 where，只有条件堆在一起
		String sql10 = "select * from sys_menu name='rico' age>18 is_deleted=0";
		System.out.println("sql10原始 " + sql10);
		System.out.println("sql10    " + build(sql10));
		assertEquals("select * from sys_menu where name='rico' and age>18 and is_deleted=0", build(sql10));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(11)
	public void test11() {
		// 测试用例 11：子查询嵌套 + 主查询缺 where
		String sql11 =
				"select * from sys_menu parent_id in (select menu_id from sys_menu where type=1 name='root') status=1";
		System.out.println("sql11原始 " + sql11);
		System.out.println("sql11    " + build(sql11));
		assertEquals("select * from sys_menu where parent_id in (select menu_id from sys_menu where type=1 and " +
				"name='root') and status=1", build(sql11));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(12)
	public void test12() {
		//测试用例 12：带 having 的情况（having 前不需要动）
		String sql12 = "select type,count(*) c from sys_menu name='rico' group by type having c>10";
		System.out.println("sql12原始 " + sql12);
		System.out.println("sql12    " + build(sql12));
		assertEquals("select type,count(*) c from sys_menu where name='rico' group by type having c>10", build(sql12));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(13)
	public void test13() {
		// 测试用例 13：WHERE 后面紧跟左括号（exists/in 等常见场景）
		String sql13 =
				"select * from sys_menu where exists(select 1 from sys_role where role_id = sys_menu.id) and " +
						"deleted=0";
		System.out.println("sql13原始 " + sql13);
		System.out.println("sql13    " + build(sql13));
		assertEquals("select * from sys_menu where exists(select 1 from sys_role where role_id = sys_menu.id) and " +
				"deleted=0", build(sql13));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(14)
	public void test14() {
		// 测试用例 14：多个连续空格 + 换行符 + 制表符
		String sql14 = "select * from sys_menu    \n\t   where   \t\n  and   name='rico'   \t   and   type=1";
		System.out.println("sql14原始 " + sql14);
		System.out.println("sql14    " + build(sql14));
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql14));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(15)
	public void test15() {
		// 测试用例 15：条件里包含 AND 关键字作为字符串内容
		String sql15 = "select * from sys_menu where remark like '%and%' name='rico'";
		System.out.println("sql15原始 " + sql15);
		System.out.println("sql15    " + build(sql15));
		assertEquals("select * from sys_menu where remark like '%and%' and name='rico'", build(sql15));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(16)
	public void test16() {
		// 测试用例 16：limit 前置条件缺失 where
		String sql16 = "select * from sys_menu create_time >= '2025-01-01' limit 10";
		System.out.println("sql16原始 " + sql16);
		System.out.println("sql16    " + build(sql16));
		assertEquals("select * from sys_menu where create_time >= '2025-01-01' limit 10", build(sql16));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(17)
	public void test17() {
		// 测试用例 17：between 场景
		String sql17 = "select * from sys_menu create_time between '2025-01-01' and '2025-12-31' status=1";
		System.out.println("sql17原始 " + sql17);
		System.out.println("sql17    " + build(sql17));
		assertEquals("select * from sys_menu where create_time between '2025-01-01' and '2025-12-31' and status=1",
				build(sql17));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(18)
	public void test18() {
		// 测试用例 18：双引号标识符（MySQL/PostgreSQL 风格）
		String sql18 = "select * from sys_menu \"userId\"=123 \"isDeleted\"=false";
		System.out.println("sql18原始 " + sql18);
		System.out.println("sql18    " + build(sql18));
		assertEquals("select * from sys_menu where \"userId\"=123 and \"isDeleted\"=false", build(sql18));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(19)
	public void test19() {
		String sql19 = """
				select * from pms_attr_group where
						order by `sort`""";
		System.out.println("sql19原始 " + sql19);
		System.out.println("sql19    " + build(sql19));
		assertEquals("select * from pms_attr_group order by `sort`", build(sql19));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(20)
	public void test20() {
		String sql20 = "select * from (select * from a_table name='rico')";
		System.out.println("sql20原始 " + sql20);
		System.out.println("sql20    " + build(sql20));
		assertEquals("select * from (select * from a_table where name='rico')", build(sql20));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(21)
	public void test21() {
		String sql21 = "select *  (select * from a_table name='rico')";
		System.out.println("sql21原始 " + sql21);
		System.out.println("sql21    " + build(sql21));
		assertEquals("select * from (select * from a_table where name='rico')", build(sql21));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(22)
	public void test22() {
		String sql22 = """
				select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id
				      LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
				      LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
				      where and a.attr_name like :attrName
				
				ORDER BY CREATE_TIME DESC""";
		System.out.println("sql22原始 " + sql22);
		System.out.println("sql22    " + build(sql22));
		String expected = """
				select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
				      left join pms_category c on a.catelog_id = c.cat_id
				      LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
				      LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
				      where
				                                a.attr_name like :attrName
				
				ORDER BY CREATE_TIME DESC""";
		assertEquals(expected, build(sql22));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(23)
	public void test23() {
		String sql23 = "select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id";
		System.out.println("sql23原始 " + sql23);
		System.out.println("sql23    " + build(sql23));
		String expected = "select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a left join pms_category c on a.catelog_id = c.cat_id";
		assertEquals(expected, build(sql23));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(28)
	public void test28() {
		// 测试用例 28：子查询里也有 group by having（递归测试）
		String sql28 =
				"select * from sys_menu dept_id in (select dept_id from sys_user name='admin' group by dept_id " +
						"having" +
						" " +
						"count(*)>4) status=1";
		System.out.println("sql28原始 " + sql28);
		System.out.println("sql28    " + build(sql28));
		assertEquals("select * from sys_menu where dept_id in (select dept_id from sys_user where name='admin' group" +
				" " +
				"by dept_id having count(*)>4) and status=1", build(sql28));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(29)
	public void test29() {
		// 测试用例 29：字符串中包含 where/and/or 关键词
		String sql29 = "select * from sys_menu where sql like '%where and or having%' name='evil'";
		System.out.println("sql29原始 " + sql29);
		System.out.println("sql29    " + build(sql29));
		assertEquals("select * from sys_menu where sql like '%where and or having%' and name='evil'", build(sql29));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(30)
	public void test30() {
		// 测试用例 30：字段名就是 and（变态中的变态）
		String sql30 = "select * from config where `and`=1 `or`=0 name='test'";
		System.out.println("sql30原始 " + sql30);
		System.out.println("sql30    " + build(sql30));
		assertEquals("select * from config where `and`=1 and `or`=0 and name='test'", build(sql30));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(34)
	public void test34() {
		System.out.print("\n=============================\n");
		String sql34 = "select * from pms_category order by cat_id asc";
		System.out.println("sql34原始 " + sql34);
		System.out.println("sql34    " + build(sql34));
		assertEquals("select * from pms_category order by cat_id asc", build(sql34));
		System.out.println(" =========================== ");

	}

	@Test
	@Order(35)
	public void test35() {
		String sql35 = """
				select * 
				from sys_menu 
				name='rico'
				""";
		System.out.println("sql35原始 " + sql35); // 输出: select * from sys_menu WHERE name='rico'
		System.out.println("sql35    " + build(sql35)); // 输出: select * from sys_menu WHERE name='rico'
		assertEquals("select * from sys_menu where name='rico'", build(sql35));
		System.out.print("\n=============================\n");

	}

	@Test
	@Order(36)
	public void test36() {
		String sql36 = """
				select * from pms_brand where
				""";
		System.out.println("sql36原始 " + sql36); // 输出: select * from pms_brand where
		System.out.println("sql36    " + build(sql36)); // 输出: select * from pms_brand
		assertEquals("select * from pms_brand", build(sql36));
		System.out.print("\n=============================\n");
	}

	@Test
	@Order(37)
	public void test37() {
		String sql37 = """
				select * from pms_brand where order by `sort` asc limit 10,10""";
		System.out.println("sql37原始 " + sql37); // 输出: select * from pms_brand where
		System.out.println("sql37    " + build(sql37)); // 输出: select * from pms_brand
		assertEquals("select * from pms_brand order by `sort` asc limit 10,10", build(sql37));
		System.out.print("\n=============================\n");
	}
}
