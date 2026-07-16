package com.awesomecopilot.orm.utils;

import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

/**
 * 命名查询解析工具，用于获取 XML / 注解中注册的 SQL 或 HQL 原文。
 * <p>
 * Copyright: Copyright (c) 2026-07-16
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class NamedQueryUtils {

	private NamedQueryUtils() {
	}

	/**
	 * 解析命名查询的原始 SQL/HQL 字符串，供 Velocity 等模板引擎进一步处理。
	 * <p>
	 * 原生 SQL 必须返回带 {@code :paramName} 占位符的完整原文（含 Velocity 片段）。
	 * {@link Query#getQueryString()} 返回的是 Hibernate 解析后的 {@code ?} SQL，不能用于模板。
	 * <p>
	 * 原生 SQL 优先从 {@link NativeQueryImplementor#toMemento(String)} 读取，与旧版
	 * 反射 {@code NativeQueryImpl.originalSqlString} 行为一致；{@link NamedObjectRepository}
	 * 中的 memento 在部分场景下会返回不完整字符串，不能作为首选来源。
	 */
	public static String resolveNamedQueryString(EntityManager entityManager, String queryName) {
		Query<?> query = entityManager.createNamedQuery(queryName).unwrap(Query.class);
		if (query instanceof NativeQueryImplementor<?> nativeQuery) {
			String originalSql = nativeQuery.toMemento(queryName).getOriginalSqlString();
			if (StringUtils.isNotBlank(originalSql)) {
				return originalSql;
			}
		}

		String sqmQuery = resolveSqmQueryFromRepository(entityManager, queryName);
		if (StringUtils.isNotBlank(sqmQuery)) {
			return sqmQuery;
		}

		String queryString = query.getQueryString();
		if (StringUtils.isNotBlank(queryString)) {
			return queryString;
		}

		throw new IllegalArgumentException("无法解析命名查询 [" + queryName + "] 的 SQL/HQL 原文");
	}

	private static String resolveSqmQueryFromRepository(EntityManager entityManager, String queryName) {
		SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
		if (!(sessionFactory instanceof SessionFactoryImplementor factory)) {
			return null;
		}

		NamedSqmQueryMemento sqmMemento = factory.getQueryEngine()
				.getNamedObjectRepository()
				.getSqmQueryMemento(queryName);
		if (sqmMemento == null) {
			return null;
		}
		return sqmMemento.getHqlString();
	}
}
