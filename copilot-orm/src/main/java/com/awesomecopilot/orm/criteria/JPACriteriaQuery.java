package com.awesomecopilot.orm.criteria;

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
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
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

	public void isNull(String propertyName) {
		this.predicates.add(criteriaBuilder.isNull(root.get(propertyName)));
		this.countPredicates.add(criteriaBuilder.isNull(countRoot.get(propertyName)));
	}

	public void isNotNull(String propertyName) {
		this.predicates.add(criteriaBuilder.isNotNull(root.get(propertyName)));
		this.countPredicates.add(criteriaBuilder.isNotNull(countRoot.get(propertyName)));
	}

	public void notEq(String propertyName, Object value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.notEqual(root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.notEqual(countRoot.get(propertyName), value));
	}

	/**
	 * not in
	 *
	 * @param propertyName 属性名称
	 * @param value 值集合
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void notIn(String propertyName, Collection<?> value) {
		if ((value == null) || (value.size() == 0)) {
			return;
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
	}

	/**
	 * 模糊匹配
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void like(String propertyName, String value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		if (value.indexOf("%") < 0) {
			value = "%" + value + "%";
		}
		this.predicates.add(criteriaBuilder.like((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.like((Expression) countRoot.get(propertyName), value));
	}

	/**
	 * 模糊匹配，不区分大小写
	 *
	 * @param propertyName
	 * @param value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void ilike(String propertyName, String value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		if (value.indexOf("%") < 0) {
			value = "%" + value.toUpperCase() + "%";
		}
		this.predicates.add(criteriaBuilder.like(criteriaBuilder.upper((Expression) root.get(propertyName)), value));
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
		}

		return this;
	}

	public JPACriteriaQuery<T> between(String propertyName, LocalDateTime begin, LocalDateTime end) {
		if (!isNullOrEmpty(begin) && !isNullOrEmpty(end)) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
		}

		return this;
	}

	public JPACriteriaQuery<T> between(String propertyName, Long begin, Long end) {
		if (!(isNullOrEmpty(begin)) && !(isNullOrEmpty(end))) {
			this.predicates.add(criteriaBuilder.between((Expression) root.get(propertyName), begin, end));
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
	public void le(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.le((Expression) root.get(propertyName), value));
	}

	/**
	 * 小于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void lt(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.lt((Expression) root.get(propertyName), value));
	}

	/**
	 * 大于等于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void ge(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.ge((Expression) root.get(propertyName), value));
		this.countPredicates.add(criteriaBuilder.ge((Expression) countRoot.get(propertyName), value));
	}

	/**
	 * 大于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void gt(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.gt((Expression) root.get(propertyName), value));
	}

	/**
	 * 大于等于
	 *
	 * @param propertyName 属性名称
	 * @param value 属性值
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void gte(String propertyName, Number value) {
		if (isNullOrEmpty(value)) {
			return;
		}
		this.predicates.add(criteriaBuilder.ge((Expression) root.get(propertyName), value));
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

	public JPACriteriaQuery<T> addPredicate(com.awesomecopilot.orm.predicate.Predicate predicate) {
		this.predicates.add(predicate.toPredicate(criteriaBuilder, root));
		return this;
	}

	public JPACriteriaQuery<T> addPredicates(com.awesomecopilot.orm.predicate.Predicate... predicates) {
		if (predicates != null && predicates.length > 0) {
			for (int i = 0; i < predicates.length; i++) {
				addPredicate(predicates[i]);
			}
		}
		return this;
	}

	public JPACriteriaQuery<T> addPredicates(List<com.awesomecopilot.orm.predicate.Predicate> predicates) {
		if (predicates != null && predicates.size() > 0) {
			for (com.awesomecopilot.orm.predicate.Predicate predicate : predicates) {
				addPredicate(predicate);
			}
		}
		return this;
	}

	public void addCriterions(Predicate predicate) {
		this.predicates.add(predicate);
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
	public void addOrder(OrderBean orderBean) {
		if (orderBean == null) {
			logger.warn("orderBean is null, ignore order!");
			return;
		}
		if (orderBean.getDirection() == null || orderBean.getOrderBy() == null) {
			logger.warn("No order by property or order direction is set, ignore order!");
			return;
		}

		if (this.orders == null)
			this.orders = new ArrayList<Order>();

		if (orderBean.getDirection() == DIRECTION.ASC) {
			this.orders.add(criteriaBuilder.asc(root.get(orderBean.getOrderBy())));
		} else if (orderBean.getDirection() == DIRECTION.DESC) {
			this.orders.add(criteriaBuilder.desc(root.get(orderBean.getOrderBy())));
		}
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

	public <Z, X> Join<Z, X> join(String attributeName) {
		return join(attributeName, JoinType.INNER);
	}

	public <Z, X> Join<Z, X> leftJoin(String attributeName) {
		return join(attributeName, JoinType.LEFT);
	}

	public <Z, X> Join<Z, X> join(String attributeName, JoinType joinType) {
		return root.join(attributeName, joinType);
	}

	/**
	 * JPA 2.1 JOIN FETCH
	 *
	 * @param attributeNames
	 * @return Fetch<Z, X>
	 */
	public JPACriteriaQuery<T> joinFetch(String... attributeNames) {
		Fetch<?, ?> fetch = null;
		for (int i = 0; i < attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			if (fetch == null) {
				fetch = root.fetch(attributeName, JoinType.INNER);
			} else {
				fetch = fetch.fetch(attributeName, JoinType.INNER);
			}
		}
		return this;
	}

	/**
	 * 层级LEFT JOIN FETCH 如三个实体类 A B C
	 *
	 * A中有属性Set<B> bSet,
	 * B中有Set<C> cSet
	 *
	 * 想要一次取出A和A包含的所有B，还有B包含的所有C 则可以这样做：
	 *
	 * 先JPACriteriaQuery jpaCriteriaQuery = JPACriteriaQuery.from(A.class, getEntityManager());
	 * jpaCriteriaQuery.leftJoinFetch("bSet", "cSet").list();
	 *
	 * JPA将发出类似语句:SELECT A.*,B.*,C.* FROM A LEFT OUT JOIN B LEFT OUT JOIN C 默认去除重复的A
	 *
	 * @on
	 * @param attributeNames
	 * @return
	 */
	public JPACriteriaQuery<T> leftJoinFetch(String... attributeNames) {
		Fetch<?, ?> fetch = null;
		for (int i = 0; i < attributeNames.length; i++) {
			String attributeName = attributeNames[i];
			if (fetch == null) {
				fetch = root.fetch(attributeName, JoinType.LEFT);
			} else {
				fetch = fetch.fetch(attributeName, JoinType.LEFT);
			}
		}
		distinct(true);
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
	public void setOrder(OrderBean order) {
		if (order == null) {
			return;
		}
		this.orders = null;
		addOrder(order);
	}

	public Class<T> getModleClass() {
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

		try {
			return query.getResultList();
		} catch (Throwable e) {
			logger.error("msg", e);
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
