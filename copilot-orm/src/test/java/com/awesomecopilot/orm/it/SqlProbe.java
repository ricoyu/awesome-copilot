package com.awesomecopilot.orm.it;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SQL 探针（测试基础设施）：挂在 persistence.xml 的
 * hibernate.session_factory.statement_inspector 上，收集 Hibernate 实际发给
 * 数据库的 SQL 文本，用于断言"某条查询是否带 LIMIT/批量语句"这类只能从
 * SQL 文本验证的行为（评审报告 M-4：findOne 全量拉取问题）。
 * <p>
 * 默认 enabled=false，inspect 原样透传，不影响其他测试；
 * 用例里 SqlProbe.enabled = true 打开收集，finally 关掉。
 *
 * @author Rico Yu
 */
public class SqlProbe implements StatementInspector {

	public static volatile boolean enabled = false;
	public static final List<String> sqls = new CopyOnWriteArrayList<>();

	public static void reset() {
		sqls.clear();
	}

	public static boolean matches(String keyword) {
		return sqls.stream().anyMatch(s -> s.contains(keyword));
	}

	public static long countMatching(String keyword) {
		return sqls.stream().filter(s -> s.contains(keyword)).count();
	}

	public static String dump() {
		return String.join("\n", sqls);
	}

	@Override
	public String inspect(String sql) {
		if (enabled) {
			sqls.add(sql);
		}
		return sql;
	}
}
