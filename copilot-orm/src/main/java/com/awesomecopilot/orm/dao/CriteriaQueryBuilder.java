package com.awesomecopilot.orm.dao;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.Orders;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.criteria.JPACriteriaQuery;
import com.awesomecopilot.orm.vo.PageResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class CriteriaQueryBuilder {

	private static final Logger log = LoggerFactory.getLogger(CriteriaQueryBuilder.class);

	private final EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	/**
	 * Spring环境下拿到的是LocalContainerEntityManagerFactoryBean的代理类
	 */
	protected transient ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();

	private CriteriaBuilder criteriaBuilder;

	private JPACriteriaQuery jpaCriteriaQuery;

	private Class entityClass;

	public CriteriaQueryBuilder(EntityManager entityManager, EntityManagerFactory entityManagerFactory,
	                            Class entityClass) {

		this.entityManagerFactory = entityManagerFactory;
		this.entityManager = entityManager;
		this.entityClass = entityClass;

		this.criteriaBuilder = em().getCriteriaBuilder();
		this.jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), false);
	}

	/**
	 * 如果propertyValue为null则查询propertyName为null的记录
	 *
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
	 *
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
	 *
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
	 *
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

	/**
	 * SQL 中的 between
	 *
	 * @param propertyName
	 * @param values
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder between(String propertyName, Object[] values) {
		if (values == null || values.length != 2) {
			return this;
		} else {
			Object begin = values[0];
			Object end = values[1];
			if ((begin instanceof Date) && (end instanceof Date)) {
				jpaCriteriaQuery.between(propertyName, (Date) begin, (Date) end);
				return this;
			}
			if ((begin instanceof Long) && (end instanceof Long)) {
				jpaCriteriaQuery.between(propertyName, (Long) begin, (Long) end);
				return this;
			}
			if ((begin instanceof LocalDateTime) && (end instanceof LocalDateTime)) {
				jpaCriteriaQuery.between(propertyName, (LocalDateTime) begin, (LocalDateTime) end);
				return this;
			}
			log.info("between(propertyName, values) 只支持java.util.Date, java.lang.Long, java.time.LocalDateTime类型");
		}
		return this;
	}

	/**
	 * SQL 中的 between
	 *
	 * @param propertyName
	 * @param values
	 * @return CriteriaQueryBuilder
	 */
	public CriteriaQueryBuilder between(String propertyName, List<?> values) {
		if (values == null || values.isEmpty() || values.size() != 2) {
			return this;
		} else {
			Object begin = values.get(0);
			Object end = values.get(1);
			if ((begin instanceof Date) && (end instanceof Date)) {
				jpaCriteriaQuery.between(propertyName, (Date) begin, (Date) end);
				return this;
			}
			if ((begin instanceof Long) && (end instanceof Long)) {
				jpaCriteriaQuery.between(propertyName, (Long) begin, (Long) end);
				return this;
			}
			if ((begin instanceof LocalDateTime) && (end instanceof LocalDateTime)) {
				jpaCriteriaQuery.between(propertyName, (LocalDateTime) begin, (LocalDateTime) end);
				return this;
			}
			log.info("between(propertyName, values) 只支持java.util.Date, java.lang.Long, java.time.LocalDateTime类型");
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

	/**
	 * 返回一条数据, 如果查到多条数据，则返回第一条, 不会报错
	 *
	 * @param <T>
	 * @return T
	 */
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
	 *
	 * @param page 分页信息, 查询得到的总页数会更新到page里面
	 * @param <T>
	 * @return List<T>
	 */
	public <T> List<T> findPage(Page page) {
		if (page != null) {
			jpaCriteriaQuery.setPage(page);
		}
		return (List<T>) jpaCriteriaQuery.list();
	}

	/**
	 * 分页查询
	 *
	 * @param pageNum
	 * @param pageSize
	 * @param <T>
	 * @return PageResult<T>
	 */
	public <T> PageResult<T> findPage(int pageNum, int pageSize) {
		Page page = new Page();
		page.setPageNum(pageNum);
		page.setPageSize(pageSize);
		if (page != null) {
			jpaCriteriaQuery.setPage(page);
		}
		List<T> data = jpaCriteriaQuery.list();
		PageResult<T> pageResult = new PageResult<>();
		pageResult.setData(data);
		pageResult.setPage(page);
		return pageResult;
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
