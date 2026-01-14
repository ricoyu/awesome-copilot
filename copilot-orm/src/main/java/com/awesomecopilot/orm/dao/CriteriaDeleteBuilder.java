package com.awesomecopilot.orm.dao;

import com.awesomecopilot.orm.predicate.Predicates;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Criteria API 删除
 * <p/>
 * Copyright: Copyright (c) 2025-03-23 18:30
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CriteriaDeleteBuilder {

	private final EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	/**
	 * Spring环境下拿到的是LocalContainerEntityManagerFactoryBean的代理类
	 */
	protected transient ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();

	private CriteriaBuilder criteriaBuilder;

	private CriteriaDelete delete;

	private Root root;

	private Class entityClass;

	List<Predicate> conditions = new ArrayList<Predicate>();

	public CriteriaDeleteBuilder(EntityManager entityManager, Class entityClass) {
		this.entityManager = entityManager;
		this.entityClass = entityClass;

		this.criteriaBuilder = em().getCriteriaBuilder();
		this.delete = criteriaBuilder.createCriteriaDelete(entityClass);
		this.root = delete.from(entityClass);
	}

	public CriteriaDeleteBuilder eq(String propertyName, Object propertyValue) {
		com.awesomecopilot.orm.predicate.Predicate predicate = Predicates.eq(propertyName, propertyValue);
		conditions.add(predicate.toPredicate(criteriaBuilder, root));
		return this;
	}

	/**
	 * 为了传参方便, value的类型设为Object, 实际处理的时候已经通过反射动态判断了是否是
	 * Collection类型, 是否是Long[], 是否是Integer[]等等
	 * @param propertyName
	 * @param values
	 * @return
	 */
	public CriteriaDeleteBuilder in(String propertyName, Object values) {
		com.awesomecopilot.orm.predicate.Predicate predicate = Predicates.inPredicate(propertyName, values);
		conditions.add(predicate.toPredicate(criteriaBuilder, root));
		return this;
	}

	public int execute() {
		delete.where(conditions.toArray(new jakarta.persistence.criteria.Predicate[0]));
		return this.em().createQuery(delete).executeUpdate();
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
