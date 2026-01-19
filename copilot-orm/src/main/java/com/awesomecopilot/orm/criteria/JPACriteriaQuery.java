package com.awesomecopilot.orm.criteria;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.exception.JPACriteriaQueryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class JPACriteriaQuery<T> implements Serializable {

	private static final long serialVersionUID = 5064932771068929342L;

	private static final String HINT_QUERY_CACHE = "org.hibernate.cacheable";

	private static final Logger logger = LoggerFactory.getLogger(JPACriteriaQuery.class);

	private EntityManager entityManager;

	/**
	 *  要查询的对象
	 */
	private Class<T> clazz;

	// 查询条件
	private Root<T> root;

	/**
	 * count查询要跟主查询分开, 不然会报错
	 * java.lang.IllegalArgumentException: Already registered a copy: SqmBasicValuedSimplePath(com.awesomecopilot.tidb.copilot.entity.Employee(1225998227700).username)
	 */
	private Root<T> countRoot;

	private List<Predicate> predicates;

	/**
	 * count查询要跟主查询分开, 不然会报错
	 * java.lang.IllegalArgumentException: Already registered a copy: SqmBasicValuedSimplePath(com.awesomecopilot.tidb.copilot.entity.Employee(1225998227700).username)
	 */
	private List<Predicate> countPredicates;

	private Map<String, Object> queryHints = new HashMap<>();

	private CriteriaQuery<T> criteriaQuery;

	/**
	 * count查询要跟主查询分开, 不然会报错
	 * java.lang.IllegalArgumentException: Already registered a copy: SqmBasicValuedSimplePath(com.awesomecopilot.tidb.copilot.entity.Employee(1225998227700).username)
	 */
	private CriteriaQuery<Long> countQuery;

	private CriteriaBuilder criteriaBuilder;

	// 排序方式列表
	private List<Order> orders;

	private String projection;

	private String groupBy;

	private Page page;

	private JPACriteriaQuery() {
	}

	private JPACriteriaQuery(Class<T> clazz, EntityManager entityManager) {
		this.clazz = clazz;
		this.entityManager = entityManager;
		this.criteriaBuilder = this.entityManager.getCriteriaBuilder();
		this.criteriaQuery = criteriaBuilder.createQuery(this.clazz);
		this.countQuery = criteriaBuilder.createQuery(Long.class);
		this.root = criteriaQuery.from(this.clazz);
		this.countRoot = countQuery.from(this.clazz);
		this.predicates = new ArrayList<Predicate>();
		this.countPredicates = new ArrayList<Predicate>();
		this.orders = new ArrayList<Order>();
	}

	/**
	 * 通过类创建查询条件
	 * @param clazz
	 * @param entityManager
	 * @param useQueryCache
	 * @return
	 */
	public static <T> JPACriteriaQuery<T> from(Class<T> clazz, EntityManager entityManager, boolean useQueryCache) {
		JPACriteriaQuery<T> jpaCriteriaQuery = new JPACriteriaQuery<T>(clazz, entityManager);
		if (useQueryCache) {
			jpaCriteriaQuery.setHint(HINT_QUERY_CACHE, true);
		}
		return jpaCriteriaQuery;
	}

	/**
	 * 相等
	 *
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public JPACriteriaQuery<T> eq(String propertyName, Object value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.equal(root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.equal(countRoot.get(propertyName), value));
		return this;
	}

	public JPACriteriaQuery<T> isNull(String propertyName) {
		this.predicates.add(criteriaBuilder.isNull(root.get(propertyName)));
		this.countPredicates.add(criteriaBuilder.isNull(countRoot.get(propertyName)));
		return this;
	}

	public JPACriteriaQuery<T> isNotNull(String propertyName) {
		this.predicates.add(criteriaBuilder.isNotNull(root.get(propertyName)));
		this.countPredicates.add(criteriaBuilder.isNotNull(countRoot.get(propertyName)));
		return this;
	}

	public JPACriteriaQuery<T> notEq(String propertyName, Object value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.notEqual(root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.notEqual(countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * not in
	 *
	 * @param propertyName 属性名称
	 * @param value 值集合
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JPACriteriaQuery<T> notIn(String propertyName, Collection<?> value) {
		if ((value == null) || (value.size() == 0)) {
			return this;
		}
		Iterator<?> iterator = value.iterator();
		In in = criteriaBuilder.in(root.get(propertyName));
		In countIn = criteriaBuilder.in(countRoot.get(propertyName));
		while (iterator.hasNext()) {
			Object next = iterator.next();
			in.value(next);
			countIn.value(next);
		}
		this.predicates.add(criteriaBuilder.not(in));
		this.countPredicates.add(criteriaBuilder.not(countIn));
		return this;
	}

	/**
	 * 模糊匹配
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> like(String propertyName, String value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		if (value.indexOf("%") < 0) {
			value = "%" + value + "%";
		}
		this.predicates.add(criteriaBuilder.like((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.like((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * 模糊匹配，不区分大小写
	 *
	 * @param propertyName
	 * @param value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> ilike(String propertyName, String value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		if (value.indexOf("%") < 0) {
			value = "%" + value.toUpperCase() + "%";
		}
		this.predicates.add(criteriaBuilder.like(criteriaBuilder.upper((Expression) root.get(propertyName)), value));
		this.countPredicates.add(criteriaBuilder.like(criteriaBuilder.upper((Expression) countRoot.get(propertyName)), value));
		return this;
	}

	/**
	 * 时间区间查询
	 *
	 * @param propertyName 属性名称
	 * @param begin 属性起始值
	 * @param end 属性结束值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> between(String propertyName, Date begin, Date end) {
		if (!isNullOrEmpty(begin) && !isNullOrEmpty(end)) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
			this.countPredicates.add(criteriaBuilder.between((Expression) countRoot.get(propertyName), begin, end));
		}

		return this;
	}

	public JPACriteriaQuery<T> between(String propertyName, LocalDateTime begin, LocalDateTime end) {
		if (!isNullOrEmpty(begin) && !isNullOrEmpty(end)) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
			this.countPredicates.add(criteriaBuilder.between((Expression) countRoot.get(propertyName), begin, end));
		}

		return this;
	}

	public JPACriteriaQuery<T> between(String propertyName, Long begin, Long end) {
		if (!(isNullOrEmpty(begin)) && !(isNullOrEmpty(end))) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
			this.countPredicates.add(criteriaBuilder.between((Expression) countRoot.get(propertyName), begin, end));
		}

		return this;
	}

	public JPACriteriaQuery<T> between(String propertyName, Integer begin, Integer end) {
		if (!(isNullOrEmpty(begin)) && !(isNullOrEmpty(end))) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
			this.countPredicates.add(criteriaBuilder.between((Expression) countRoot.get(propertyName), begin, end));
		}

		return this;
	}

	/**
	 * 小于等于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> le(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.le((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.le((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * 小于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> lt(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.lt((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.lt((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * 大于等于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> ge(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.ge((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.ge((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * 大于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> gt(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.gt((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.gt((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * 大于等于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JPACriteriaQuery<T> gte(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return this;
		}
		this.predicates.add(criteriaBuilder.ge((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.ge((Expression) countRoot.get(propertyName), value));
		return this;
	}

	/**
	 * in
	 *
	 * @param propertyName 属性名称
	 * @param values       值集合
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JPACriteriaQuery<T> in(String propertyName, Collection<?> values) {
		if ((values == null) || (values.size() == 0)) {
			return this;
		}
		Iterator<?> iterator = values.iterator();
		In in = criteriaBuilder.in(root.get(propertyName));
		In countIn = criteriaBuilder.in(countRoot.get(propertyName));
		while (iterator.hasNext()) {
			Object next = iterator.next();
			in.value(next);
			countIn.value(next);
		}
		this.predicates.add(in);
		this.countPredicates.add(countIn);
		return this;
	}

	/**
	 * 创建查询条件
	 *
	 * @return JPA离线查询
	 */
	public CriteriaQuery<T> fillUpCriterias() {
		criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));
		if (!isNullOrEmpty(groupBy)) {
			criteriaQuery.groupBy(root.get(groupBy));
		}
		if (this.orders != null && orders.size() != 0) {
			criteriaQuery.orderBy(orders);
		}
		return criteriaQuery;
	}

	/**
	 * 在已有排序基础上再加上当前排序
	 *
	 * @param orderBean
	 */
	public JPACriteriaQuery<T> addOrder(OrderBean orderBean) {
		if (orderBean == null) {
			logger.warn("orderBean is null, ignore order!");
			return this;
		}
		if (orderBean.getDirection() == null || orderBean.getOrderBy() == null) {
			logger.warn("No order by property or order direction is set, ignore order!");
			return this;
		}

		if (this.orders == null)
			this.orders = new ArrayList<Order>();

		if (orderBean.getDirection() == DIRECTION.ASC) {
			this.orders.add(criteriaBuilder.asc(root.get(orderBean.getOrderBy())));
		} else if (orderBean.getDirection() == DIRECTION.DESC) {
			this.orders.add(criteriaBuilder.desc(root.get(orderBean.getOrderBy())));
		}
		return this;
	}

	public JPACriteriaQuery<T> addOrders(OrderBean... orders) {
		if (orders != null && orders.length > 0) {
			for (int i = 0; i < orders.length; i++) {
				addOrder(orders[i]);
			}
		}
		return this;
	}

	public JPACriteriaQuery<T> addOrders(List<OrderBean> orders) {
		orders.forEach(o -> addOrder(o));
		return this;
	}

	public JPACriteriaQuery<T> distinct(boolean distinct) {
		criteriaQuery.distinct(distinct);
		return this;
	}

	/**
	 * 先清空所有排序，再添加当前排序
	 *
	 * @param order
	 */
	public JPACriteriaQuery<T> setOrder(OrderBean order) {
		if (order == null) {
			return this;
		}
		this.orders = new ArrayList<>();
		addOrder(order);
		return this;
	}

	public Class<T> getModelClass() {
		return this.clazz;
	}

	public String getProjection() {
		return this.projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

	public Class<T> getClazz() {
		return this.clazz;
	}

	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	public void setEntityManager(EntityManager em) {
		this.entityManager = em;
	}

	public Root<T> getFrom() {
		return root;
	}

	public Root<T> getRoot() {
		return root;
	}

	public List<Predicate> getPredicates() {
		return predicates;
	}

	public void setPredicates(List<Predicate> predicates) {
		this.predicates = predicates;
	}

	public CriteriaQuery<T> getCriteriaQuery() {
		return criteriaQuery;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	/**
	 * 别看这个方法名是list(), 分页查询也是走这个方法哦
	 * @return
	 */
	public List<T> list() {
		TypedQuery<T> query = entityManager.createQuery(fillUpCriterias());
		if (page != null) {
			query.setFirstResult(page.getFirstResult())
					.setMaxResults(page.getMaxResults());

			// 获取总记录数
			countQuery.where(countPredicates.toArray(new Predicate[countPredicates.size()]));
			// 6. 设置SELECT子句（COUNT）
			countQuery.select(criteriaBuilder.count(countRoot));

			// 7. 执行查询并返回结果
			Long totalCount = entityManager.createQuery(countQuery).getSingleResult();
			page.setTotalCount(totalCount.intValue());
		}
		if (!queryHints.isEmpty()) {
			for (String hintName : queryHints.keySet()) {
				query.setHint(hintName, queryHints.get(hintName));
			}
		}

		/*
		 * 这边放到ThreadContext里面是为了保证PageResultAspect能从ThreadContext拿到将Page对象并回填到最终返回的Result对象里面
		 */
		ThreadContext.put("page", page);

		try {
			return query.getResultList();
		} catch (Throwable e) {
			logger.error("Error executing JPA criteria query for entity: " + clazz.getSimpleName(), e);
			throw new JPACriteriaQueryException(e);
		}
	}

	private boolean isNullOrEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String) {
			return "".equals(((String) value).trim());
		}
		return false;
	}

	public Page getPage() {
		return page;
	}

	public JPACriteriaQuery<T> setPage(Page page) {
		this.page = page;
		if (page.getOrder() != null) {
			this.addOrder(page.getOrder());
		}
		return this;
	}

	public JPACriteriaQuery<T> setHint(String hintName, Object value) {
		requireNonNull(hintName, "老铁, hintName不可以为null哦");
		queryHints.put(hintName, value);
		return this;
	}

}
