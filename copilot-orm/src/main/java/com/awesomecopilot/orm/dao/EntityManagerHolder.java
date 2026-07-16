package com.awesomecopilot.orm.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 无 Spring 事务时按需创建 EntityManager，并在操作结束后统一关闭，避免连接泄漏。
 *
 * @author Rico Yu
 */
final class EntityManagerHolder {

	private static final Logger log = LoggerFactory.getLogger(EntityManagerHolder.class);

	private final EntityManager transactionalEntityManager;

	private final EntityManagerFactory entityManagerFactory;

	private EntityManager createdEntityManager;

	EntityManagerHolder(EntityManager transactionalEntityManager, EntityManagerFactory entityManagerFactory) {
		this.transactionalEntityManager = transactionalEntityManager;
		this.entityManagerFactory = entityManagerFactory;
	}

	EntityManager get() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			return transactionalEntityManager;
		}
		if (entityManagerFactory == null) {
			throw new IllegalStateException(
					"EntityManagerFactory is required when no Spring transaction is active");
		}
		if (createdEntityManager != null && createdEntityManager.isOpen()) {
			return createdEntityManager;
		}
		createdEntityManager = entityManagerFactory.createEntityManager();
		return createdEntityManager;
	}

	void closeIfNeeded() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			return;
		}
		if (createdEntityManager == null) {
			return;
		}
		try {
			if (createdEntityManager.isOpen()) {
				if (createdEntityManager.getTransaction().isActive()) {
					createdEntityManager.getTransaction().rollback();
				}
				createdEntityManager.close();
			}
		} catch (Exception e) {
			log.warn("Error closing EntityManager", e);
		} finally {
			createdEntityManager = null;
		}
	}
}
