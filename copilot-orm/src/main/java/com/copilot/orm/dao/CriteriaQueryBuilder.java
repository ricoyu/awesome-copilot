package com.copilot.orm.dao;

import com.copilot.common.lang.vo.OrderBean;
import com.copilot.common.lang.vo.Orders;
import com.copilot.orm.criteria.JPACriteriaQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

	public CriteriaQueryBuilder(EntityManager entityManager, Class entityClass) {

		this.entityManager = entityManager;
		this.entityClass = entityClass;

		this.criteriaBuilder = em().getCriteriaBuilder();
		this.jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), false);
	}


	public CriteriaQueryBuilder eq(String propertyName, Object propertyValue) {
		if (propertyValue == null) {
			jpaCriteriaQuery.isNull(propertyName);
		} else {
			jpaCriteriaQuery.eq(propertyName, propertyValue);
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

	public <T> List<T> findList() {
		return (List<T>) jpaCriteriaQuery.list();
	}

	public <T> T findOne() {
		List results = jpaCriteriaQuery.list();
		if (results.isEmpty()) {
			return null;
		}
		return (T) results.get(0);
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
