package com.awwesomecopilot.orm;

import org.junit.jupiter.api.Test;

import static com.awesomecopilot.orm.utils.SQLUtils.build;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLUtilsTest {

	@Test
	public void testBuild() {
		String sql1 = "select * from sys_menu name='rico'";
		System.out.println("sql1原始 " + sql1); // 输出: select * from sys_menu WHERE name='rico'
		System.out.println("sql1    " + build(sql1)); // 输出: select * from sys_menu WHERE name='rico'
			assertEquals("select * from sys_menu where name='rico'", build(sql1));
		System.out.print("\n=============================\n");

		String sql2 = "select * from sys_menu where and name='rico'";
		System.out.println("sql2原始 " + sql2); // 输出: select * from sys_menu WHERE name='rico'
		System.out.println("sql2    " + build(sql2)); // 输出: select * from sys_menu WHERE name='rico'
			assertEquals("select * from sys_menu where name='rico'", build(sql2));
		System.out.print("\n=============================\n");

		String sql3 = "select * from sys_menu where name='rico' type=1";
		System.out.println("sql3原始 " + sql3); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		System.out.println("sql3    " + build(sql3)); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql3));
		System.out.print("\n=============================\n");

		// 额外测试：多条件
		String sql4 = "select * from sys_menu name='rico' type=1 status=active";
		System.out.println("sql4原始 " + sql4); // 输出: select * from sys_menu WHERE name='rico' AND type=1 AND status=active
		System.out.println("sql4    " + build(sql4)); // 输出: select * from sys_menu WHERE name='rico' AND type=1 AND status=active
		assertEquals("select * from sys_menu where name='rico' and type=1 and status=active", build(sql4));
		System.out.print("\n=============================\n");

		String sql5 = "select * from sys_menu where and name='rico' and and type=1";
		System.out.println("sql5原始 " + sql5); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		System.out.println("sql5    " + build(sql5)); // 输出: select * from sys_menu WHERE name='rico' AND type=1
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql5));
		System.out.print("\n=============================\n");

		String sql6 = "select * from sys_menu where id IN (select id from sys_user name='rico') type=1";
		System.out.println("sql6原始 " + sql6);
		System.out.println("sql6    " + build(sql6));
		assertEquals("select * from sys_menu where id in (select id from sys_user where name='rico') and type=1",
				build(sql6));
		System.out.print("\n=============================\n");

		//String sql7 = "select * from sys_menu where type in (0, 1) order by menu_id asc, order_num asc";
		//System.out.println("sql7原始 " + sql7);
		//System.out.println("sql7    " + build(sql7));
		//assertEquals("select * from sys_menu where type in (0, 1)  order by menu_id asc, order_num asc",
		//		build(sql7));
		//System.out.print("\n=============================\n");

		String sql8 = "select * from sys_menu name=:name type=:type parent_id = (select menu_id from sys_menu where name=:parentName)";
		System.out.println("sql8原始 " + sql8);
		System.out.println("sql8    " + build(sql8));
		assertEquals("select * from sys_menu where name=:name and type=:type and parent_id = (select menu_id from sys_menu where name=:parentName)",
				build(sql8));
		System.out.print("\n=============================\n");

		// 测试用例 9：大小写混乱 + 多余 and + 缺失 where
		String sql9 = "SELECT * FROM sys_menu WHERE AND name = 'rico' AND AND type=1 AND";
		System.out.println("sql9原始 " + sql9);
		System.out.println("sql9    " + build(sql9));
		assertEquals("select * from sys_menu where name = 'rico' and type=1", build(sql9));
		System.out.print("\n=============================\n");

		// 测试用例 10：完全没有 where，只有条件堆在一起
		String sql10 = "select * from sys_menu name='rico' age>18 is_deleted=0";
		System.out.println("sql10原始 " + sql10);
		System.out.println("sql10    " + build(sql10));
		assertEquals("select * from sys_menu where name='rico' and age>18 and is_deleted=0", build(sql10));
		System.out.print("\n=============================\n");

		// 测试用例 11：子查询嵌套 + 主查询缺 where
		String sql11 = "select * from sys_menu parent_id in (select menu_id from sys_menu where type=1 name='root') status=1";
		System.out.println("sql11原始 " + sql11);
		System.out.println("sql11    " + build(sql11));
		assertEquals("select * from sys_menu where parent_id in (select menu_id from sys_menu where type=1 and name='root') and status=1", build(sql11));
		System.out.print("\n=============================\n");

		// 测试用例 12：带 having 的情况（having 前不需要动）
		//String sql12 = "select type,count(*) c from sys_menu name='rico' group by type having c>10";
		//System.out.println("sql12原始 " + sql12);
		//System.out.println("sql12    " + build(sql12));
		//assertEquals("select type,count(*) c from sys_menu where name='rico'  group by type having c>10", build(sql12));
		//System.out.print("\n=============================\n");

		// 测试用例 13：WHERE 后面紧跟左括号（exists/in 等常见场景）
		String sql13 = "select * from sys_menu where exists(select 1 from sys_role where role_id = sys_menu.id) and deleted=0";
		System.out.println("sql13原始 " +sql13);
		System.out.println("sql13    " + build(sql13));
		assertEquals("select * from sys_menu where exists(select 1 from sys_role where role_id = sys_menu.id) and deleted=0", build(sql13));
		System.out.print("\n=============================\n");

		// 测试用例 14：多个连续空格 + 换行符 + 制表符
		String sql14 = "select * from sys_menu    \n\t   where   \t\n  and   name='rico'   \t   and   type=1";
		System.out.println("sql14原始 " + sql14);
		System.out.println("sql14    " + build(sql14));
		assertEquals("select * from sys_menu where name='rico' and type=1", build(sql14));
		System.out.print("\n=============================\n");

		// 测试用例 15：条件里包含 AND 关键字作为字符串内容
		String sql15 = "select * from sys_menu where remark like '%and%' name='rico'";
		System.out.println("sql15原始 " + sql15);
		System.out.println("sql15    " + build(sql15));
		assertEquals("select * from sys_menu where remark like '%and%' and name='rico'", build(sql15));
		System.out.print("\n=============================\n");

		// 测试用例 16：limit 前置条件缺失 where
		//String sql16 = "select * from sys_menu create_time >= '2025-01-01' limit 10";
		//System.out.println("sql16原始 " + sql16);
		//System.out.println("sql16    " + build(sql16));
		//assertEquals("select * from sys_menu where create_time >= '2025-01-01'  limit 10", build(sql16));
		//System.out.print("\n=============================\n");

		// 测试用例 17：between 场景
		String sql17 = "select * from sys_menu create_time between '2025-01-01' and '2025-12-31' status=1";
		System.out.println("sql17原始 " + sql17);
		System.out.println("sql17    " + build(sql17));
		assertEquals("select * from sys_menu where create_time between '2025-01-01' and '2025-12-31' and status=1", build(sql17));
		System.out.print("\n=============================\n");

		// 测试用例 18：双引号标识符（MySQL/PostgreSQL 风格）
		String sql18 = "select * from sys_menu \"userId\"=123 \"isDeleted\"=false";
		System.out.println("sql18原始 " + sql18);
		System.out.println("sql18    " + build(sql18));
		assertEquals("select * from sys_menu where \"userId\"=123 and \"isDeleted\"=false", build(sql18));
		System.out.print("\n=============================\n");

		// 测试用例 28：子查询里也有 group by having（递归测试）
		//String sql28 = "select * from sys_menu dept_id in (select dept_id from sys_user name='admin' group by dept_id having count(*)>4) status=1";
		//System.out.println("sql28原始 " + sql28);
		//System.out.println("sql28    " + build(sql28));
		//assertEquals("select * from sys_menu where dept_id in (select dept_id from sys_user where name='admin'  group by dept_id having count(*)>4) and status=1", build(sql28));
		//System.out.print("\n=============================\n");

		// 测试用例 29：字符串中包含 where/and/or 关键词
		String sql29 = "select * from sys_menu where sql like '%where and or having%' name='evil'";
		System.out.println("sql29原始 " + sql29);
		System.out.println("sql29    " + build(sql29));
		assertEquals("select * from sys_menu where sql like '%where and or having%' and name='evil'", build(sql29));
		System.out.print("\n=============================\n");

		// 测试用例 30：字段名就是 and（变态中的变态）
		String sql30 = "select * from config where `and`=1 `or`=0 name='test'";
		System.out.println("sql30原始 " + sql30);
		System.out.println("sql30    " + build(sql30));
		assertEquals("select * from config where `and`=1 and `or`=0 and name='test'", build(sql30));
		System.out.print("\n=============================\n");

		System.out.print("\n=============================\n");
		String sql34 = "select * from pms_category order by cat_id asc";
		System.out.println("sql34原始 " + sql34);
		System.out.println("sql34    " + build(sql34));
		assertEquals("select * from pms_category order by cat_id asc", build(sql34));
		System.out.println(" =========================== ");

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
}
