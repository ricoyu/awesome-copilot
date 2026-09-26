package com.awesomecopilot.orm.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityManagerHolder.closeIfNeeded() 的守卫回归（第三轮复审 deleg_d5016c35 R1）。
 * <p>
 * 旧缺陷：closeIfNeeded 只认 Spring 事务，调用方 begin() 开启的本地事务它不认——
 * 一次终结读查询就会把持有活跃事务的 EntityManager rollback+close，写入被悄悄
 * 回滚，后续查询抛 Session/EntityManager is closed。与
 * JpaDao.releaseEntityManagerIfIdle 用 hasActiveLocalTransaction 守卫的口径不一致
 * （两套守卫，一个护住本地事务、一个不护）。
 * <p>
 * 期望：本地事务活跃时 closeIfNeeded 什么都不做；事务 commit/rollback 结束后
 * 恢复关闭（防泄漏）；Spring 事务活跃时永远不关（EM 归容器管）。
 * <p>
 * holder 构造器与 closeIfNeeded 是包级可见，测试必须放在同包下。
 *
 * @author Rico Yu
 */
class EntityManagerHolderTest {

	private static EntityManagerFactory emf;

	@BeforeAll
	static void bootEmf() {
		emf = jakarta.persistence.Persistence.createEntityManagerFactory("orm-test");
	}

	@AfterAll
	static void shutdownEmf() {
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
	}

	@Test
	@DisplayName("本地事务活跃时, closeIfNeeded 不得回滚/关闭该 EntityManager")
	void keepsEmWhileLocalTransactionActive() {
		EntityManager em = emf.createEntityManager();
		EntityManagerHolder holder = new EntityManagerHolder(null, emf);
		try {
			EntityTransaction tx = em.getTransaction();
			tx.begin();
			// 等价于 begin() 经 holder.get() 创建并记录该 EM 的场景
			setCreated(holder, em);

			holder.closeIfNeeded();

			assertTrue(em.isOpen(), "事务活跃期间 EM 必须保持打开(旧行为: 被 close)");
			assertTrue(tx.isActive(), "事务必须仍活跃(旧行为: 被 rollback)");
			tx.rollback();
		} finally {
			if (em.isOpen()) {
				em.close();
			}
		}
	}

	@Test
	@DisplayName("本地事务已结束后, closeIfNeeded 正常关闭 EM(不泄漏)")
	void closesEmAfterTransactionEnded() {
		EntityManager em = emf.createEntityManager();
		EntityManagerHolder holder = new EntityManagerHolder(null, emf);
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		tx.commit();
		setCreated(holder, em);

		holder.closeIfNeeded();

		assertFalse(em.isOpen(), "事务已结束, closeIfNeeded 应关闭 EM 防泄漏");
	}

	@Test
	@DisplayName("Spring 事务活跃时, closeIfNeeded 永不关闭(transactional EM 归容器管)")
	void neverClosesUnderSpringTransaction() {
		EntityManager em = emf.createEntityManager();
		EntityManagerHolder holder = new EntityManagerHolder(em, emf);
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			holder.closeIfNeeded();
			assertTrue(em.isOpen(), "Spring 事务上下文里容器负责关 EM");
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			em.close();
		}
	}

	private static void setCreated(EntityManagerHolder holder, EntityManager em) {
		try {
			java.lang.reflect.Field f = EntityManagerHolder.class.getDeclaredField("createdEntityManager");
			f.setAccessible(true);
			f.set(holder, em);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
