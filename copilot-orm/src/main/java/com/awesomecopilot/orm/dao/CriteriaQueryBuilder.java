package com.awesomecopilot.orm.dao;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.Orders;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.criteria.JPACriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

public class CriteriaQueryBuilder {

	private final EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	/**
	 * Spring环境下拿到的是LocalContainerEntityManagerFactoryBean的代理类
	 */
	protected transient ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();

	private CriteriaBuilder criteriaBuilder;

	private JPACriteriaQuery jpaCriteriaQuery;

	private Class entityClass;

	public CriteriaQueryBuilder(EntityManager entityManager, EntityManagerFactory entityManagerFactory, Class entityClass) {

		this.entityManagerFactory = entityManagerFactory;
		this.entityManager = entityManager;
		this.entityClass = entityClass;

		this.criteriaBuilder = em().getCriteriaBuilder();
		this.jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), false);
	}

	/**
	 * 如果propertyValue为null则查询propertyName为null的记录
	 * @param propertyName
	 * @param propertyValue
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder isNull(String propertyName, Object propertyValue) {
		if (propertyValue == null) {
			jpaCriteriaQuery.isNull(propertyName);
		}
		return this;
	}

	/**
	 * 如果propertyValue为null则忽略这个eq条件
	 * @param propertyName
	 * @param propertyValue
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder eq(String propertyName, Object propertyValue) {
		if (propertyValue == null) {
			return this;
		} else {
			jpaCriteriaQuery.eq(propertyName, propertyValue);
		}
		return this;
	}

	/**
	 * SQL 中的 like, 如果propertyValue为null则忽略这个like条件
	 * @param propertyName
	 * @param propertyValue
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder like(String propertyName, Object propertyValue) {
		if (propertyValue == null) {
			return this;
		} else {
			jpaCriteriaQuery.like(propertyName, propertyValue.toString());
		}
		return this;
	}

	/**
	 * SQL 中的 in
	 * @param propertyName
	 * @param values
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder in(String propertyName, Collection<?> values) {
		if (values == null || values.isEmpty()) {
			return this;
		} else {
			jpaCriteriaQuery.in(propertyName, values);
		}
		return this;
	}

	public CriteriaQueryBuilder asc(String propertyName) {
		OrderBean order = Orders.asc(propertyName);
		jpaCriteriaQuery.addOrder(order);
		return this;
	}

	public CriteriaQueryBuilder desc(String propertyName) {
		OrderBean order = Orders.desc(propertyName);
		jpaCriteriaQuery.addOrder(order);
		return this;
	}

	public <T> T findOne() {
		List results = jpaCriteriaQuery.list();
		if (results.isEmpty()) {
			return null;
		}
		return (T) results.get(0);
	}

	public <T> List<T> findList() {
		return (List<T>) jpaCriteriaQuery.list();
	}

	/**
	 * 分页查询
	 * @param page 分页信息, 查询得到的总页数会更新到page里面
	 * @return List<T>
	 * @param <T>
	 */
	public <T> List<T> findPage(Page page) {
		if (page != null) {
			jpaCriteriaQuery.setPage(page);
		}
		return (List<T>) jpaCriteriaQuery.list();
	}

	/**
	 * 基于是否受Spring事务管理，获取Spring管理的EntityManager或者自行通过EntityManagerFactory创建的EntityManager
	 *
	 * @return EntityManager
	 */
	private EntityManager em() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			return entityManager;
		} else {
			if (entityManagerThreadLocal.get() != null) {
				return entityManagerThreadLocal.get();
			} else {
				EntityManager noTransactionalEntityManager = entityManagerFactory.createEntityManager();
				entityManagerThreadLocal.set(noTransactionalEntityManager);
				return noTransactionalEntityManager;
			}
		}
	}
}
