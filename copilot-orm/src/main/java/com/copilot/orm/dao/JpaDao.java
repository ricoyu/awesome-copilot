package com.copilot.orm.dao;

import com.copilot.common.lang.utils.ArrayTypes;
import com.copilot.common.lang.utils.ReflectionUtils;
import com.copilot.common.lang.vo.OrderBean;
import com.copilot.orm.criteria.JPACriteriaQuery;
import com.copilot.orm.exception.EntityOperationException;
import com.copilot.orm.exception.JPQLException;
import com.copilot.orm.exception.PersistenceException;
import com.copilot.orm.exception.RawSQLQueryException;
import com.copilot.orm.exception.SQLQueryException;
import com.copilot.orm.transformer.ResultTransformerFactory;
import com.copilot.orm.utils.HashUtils;
import com.copilot.orm.utils.JacksonUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 基于JPA 2.1封装的通用DAO
 *
 * @author Loser
 * @since Mar 6, 2016
 */
@Repository
public class JpaDao implements JPQLOperations, SQLOperations, CriteriaOperations,
		EntityOperations, InitializingBean {

	private static Logger log = LoggerFactory.getLogger(JpaDao.class);

	private static final String HINT_QUERY_CACHE = "org.hibernate.cacheable";

	/**
	 * 用于判断是否是查询语句
	 */
	private static final Pattern SELECT_PATTERN =
			Pattern.compile("\\bSELECT\\b[\\s\\S]*?\\bFROM\\b\\s+", Pattern.CASE_INSENSITIVE);

	//用于判断是否insert语句
	private static final Pattern DELETE_PATTERN =
			Pattern.compile("\\bdelete\\b[\\s\\S]*?\\bFROM\\b\\s+", Pattern.CASE_INSENSITIVE);

	//用于判断是否insert语句
	private static final Pattern INSERT_PATTERN =
			Pattern.compile("\\binsert\\b[\\s\\S]*?\\binto\\b\\s+", Pattern.CASE_INSENSITIVE);

	//用于判断是否update语句
	private static final Pattern UPDATE_PATTERN =
			Pattern.compile("\\bupdate\\b[\\s\\S]*?\\bset\\b\\s+", Pattern.CASE_INSENSITIVE);

	private static final ConcurrentMap<String, ArrayTypes> ARRAY_TYPE_MAP = new ConcurrentHashMap<>();

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/**
	 * Spring环境下拿到的是LocalContainerEntityManagerFactoryBean的代理类
	 */
	protected transient ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();

	@PersistenceContext
	protected EntityManager entityManager;

	@Value("${hibernate.query.mode:loose}")
	private String hibernateQueryMode = "loose";

	@Value("${hibernate.query.cache:false}")
	private boolean hibernateUseQueryCache = false;

	@Value("${hibernate.jdbc.batch_size:100}")
	private int batchSize = 0;

	/**
	 * 如果类的某个属性是enum类型，并且需要根据这个enum类型的某个属性来和数据库列值匹配，那么要指明这个属性的名字
	 */
	private Set<String> enumLookupProperties = new HashSet<>();

	/**
	 * 默认会根据CREATE_TIME倒序排
	 */
	private OrderBean order = new OrderBean("CREATE_TIME", OrderBean.DIRECTION.DESC);

	/**
	 * 配置JpaDao的时候指定context-className
	 */
	private Map<String, String> contextClasses = new HashMap<>();

	/**
	 * 根据contextClasses找对应的Class对象在namedSqlQuery中会把classMap中的key/value对put到VelocityContext中
	 * 这样在SQL里面就可以用了, 示例如下:
	 * <pre>{@code
	 *  #if($StringUtils.isNotBlank($userName))
	 *     AND u.username LIKE :userName
	 *  #end
	 * }</pre>
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, Class> classMap = new HashMap<>();

	static {
		ARRAY_TYPE_MAP.put(ArrayTypes.LONG.getClassName(), ArrayTypes.LONG);
		ARRAY_TYPE_MAP.put(ArrayTypes.LONG_WRAPPER.getClassName(), ArrayTypes.LONG_WRAPPER);
		ARRAY_TYPE_MAP.put(ArrayTypes.INTEGER.getClassName(), ArrayTypes.INTEGER);
		ARRAY_TYPE_MAP.put(ArrayTypes.INTEGER_WRAPPER.getClassName(), ArrayTypes.INTEGER_WRAPPER);
		ARRAY_TYPE_MAP.put(ArrayTypes.STRING.getClassName(), ArrayTypes.STRING);
		ARRAY_TYPE_MAP.put(ArrayTypes.DOUBLE.getClassName(), ArrayTypes.DOUBLE);
		ARRAY_TYPE_MAP.put(ArrayTypes.DOUBLE_WRAPPER.getClassName(), ArrayTypes.DOUBLE_WRAPPER);
		ARRAY_TYPE_MAP.put(ArrayTypes.FLOAT.getClassName(), ArrayTypes.FLOAT);
		ARRAY_TYPE_MAP.put(ArrayTypes.FLOAT_WRAPPER.getClassName(), ArrayTypes.FLOAT_WRAPPER);

		Properties properties = new Properties();
		properties.setProperty("userdirective",
				"com.copilot.orm.directive.IfNotNull," +
						"com.copilot.orm.directive.IfNull," +
						"com.copilot.orm.directive.Between," +
						"com.copilot.orm.directive.LLike," +
						"com.copilot.orm.directive.RLike," +
						"com.copilot.orm.directive.Like," +
						"com.copilot.orm.directive.IfPresent");
		properties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log" +
				".Log4JLogChute");
		properties.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
		//初始化运行时引擎
		Velocity.init(properties);
	}

	/**
	 * Make an instance managed and persistent.
	 *
	 * @param entity
	 */
	@Override
	public <T> void persist(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			em().persist(entity);
		} catch (Throwable e) {
			String msg = MessageFormat.format("Entity: {0}", JacksonUtils.toPrettyJson(entity));
			log.error(msg, e);
			throw new PersistenceException(e);
		}
	}

	@Override
	public <T> void persist(List<T> entities) {
		Objects.requireNonNull(entities, "entities cannot be null");
		if (entities.isEmpty()) {
			return;
		}
		try {
			for (int i = 0; i < entities.size(); i++) {
				em().persist(entities.get(i));
			}
		} catch (Throwable e) {
			log.error("", e);
			throw new PersistenceException(e);
		}
	}

	/**
	 * merge返回的是一个受当前Persistence Context管理的新对象
	 *
	 * @param entity
	 * @return T
	 */
	@Override
	public <T> T merge(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			return em().merge(entity);
		} catch (Throwable e) {
			String msg = format("Entity: {0}", JacksonUtils.toPrettyJson(entity));
			log.error(msg, e);
			throw new PersistenceException(e);
		}
	}

	/**
	 * merge返回的是一个受当前Persistence Context管理的新对象
	 *
	 * @param entities
	 * @return List<T>
	 */
	@Override
	public <T> List<T> merge(List<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		for (T entity : entities) {
			Objects.requireNonNull(entity, "entity cannot be null");
			try {
				results.add(em().merge(entity));
			} catch (Throwable e) {
				String msg = format("Entity: {0}", JacksonUtils.toPrettyJson(entity));
				log.error(msg, e);
				throw new PersistenceException(e);
			}
		}
		flush();
		return results;
	}

	@Override
	public <T> T save(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		Object id = ReflectionUtils.getFieldValue("id", entity);
		if (id == null) {
			persist(entity);
			return entity;
		} else {
			try {
				return em().merge(entity);
			} catch (Throwable e) {
				String msg = format("Entity: {0}", JacksonUtils.toPrettyJson(entity));
				log.error(msg, e);
				throw new PersistenceException(e);
			}
		}
	}

	@Override
	public <T> List<T> save(List<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		for (int i = 0, length = entities.size(); i < length; i++) {
			results.add(save(entities.get(i)));
			/*
			 * i+1是因为i是从0开始的, 如果batchSize=100, 那么i=99的时候, i+1=100, 正好是100的倍数, 此时需要flush
			 */
			if (i > 0 && ((i + 1) % batchSize == 0)) {
				flush();
			}
		}
		/*
		 * You may also want to flush and clear the persistence context
		 * after each batch to release memory, otherwise all the managed
		 * objects remain in the persistence context until it is closed.
		 */
		flush();
		return results;
	}

	@Override
	public <T> List<T> save(Set<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		int i = 0;
		for (T entity : entities) {
			results.add(save(entity));
			i++;
			if (i > 0 && (i % batchSize == 0)) {
				flush();
			}
		}
		/*
		 * You may also want to flush and clear the persistence context
		 * after each batch to release memory, otherwise all of the managed
		 * objects remain in the persistence context until it is closed.
		 */
		flush();
		return results;
	}

	/**
	 * 删除
	 *
	 * @param entity
	 */
	@Override
	public <T> void delete(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			em().remove(entity);
		} catch (Throwable e) {
			log.error("", e);
			throw e;
		}
	}

	/**
	 * 删除
	 *
	 * @param entities
	 */
	@Override
	public <T> void delete(List<T> entities) {
		Objects.requireNonNull(entities, "entity cannot be null");
		try {
			for (T t : entities) {
				if (t != null) {
					em().remove(t);
				}
			}
		} catch (Throwable e) {
			log.error("", e);
			throw e;
		}
	}

	/**
	 * 根据主键删除
	 *
	 * @param entityClass
	 * @param id
	 */
	@Override
	public <T, PK extends Serializable> void deleteByPK(Class<T> entityClass, PK id) {
		Objects.requireNonNull(id, "id cannot be null");
		T entity = em().getReference(entityClass, id);
		delete(entity);
	}

	@Override
	public <T, PK extends Serializable> void deleteByPK(Class<T> entityClass, List<PK> ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (ids.size() == 0) {
			return;
		}
		for (PK pk : ids) {
			deleteByPK(entityClass, pk);
		}
	}

	/**
	 * 根据主键查找
	 *
	 * @param clazz
	 * @param id
	 * @return
	 */
	@Override
	public <T, PK extends Serializable> T get(Class<T> clazz, PK id) {
		Objects.requireNonNull(id, "id cannot be null");
		if (log.isDebugEnabled()) {
			log.debug("Try to find " + clazz.getName() + " by id " + id);
		}
		try {
			return em().find(clazz, id);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, PK extends Serializable> List<T> getMulti(Class<T> clazz, PK... ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (log.isDebugEnabled()) {
			log.debug("Try to find " + clazz.getName() + " by ids " + ids);
		}
		try {
			Session session = em().unwrap(Session.class);
			return session.byMultipleIds(clazz).multiLoad(ids);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T, PK extends Serializable> List<T> getMulti(Class<T> clazz, List<PK> ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (log.isDebugEnabled()) {
			log.debug("Try to find " + clazz.getName() + " by ids " + ids);
		}
		try {
			Session session = em().unwrap(Session.class);
			return session.byMultipleIds(clazz).multiLoad(ids);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T, PK extends Serializable> T find(Class<T> clazz, PK id) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		jakarta.persistence.criteria.Predicate idPredicate = criteriaBuilder.equal(root.get("id"), id);
		jakarta.persistence.criteria.Predicate deletedPredicate = criteriaBuilder.equal(root.get("deleted"), false);
		criteriaQuery.select(root)
				.where(idPredicate, deletedPredicate)
				.distinct(true);
		TypedQuery<T> query = em().createQuery(criteriaQuery);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		List<T> results = query.getResultList();
		if (results.isEmpty()) {
			return null;
		}
		return results.get(0);
	}

	@Override
	public <T> List<T> findList(Class<T> entityClass, String propertyName, Object value) {
		Objects.requireNonNull(propertyName, "propertyName cannot be null!");
		JPACriteriaQuery<T> jpaCriteriaQuery =
				JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache);
		if (value == null) {
			jpaCriteriaQuery.isNull(propertyName);
		} else {
			jpaCriteriaQuery.eq(propertyName, value);
		}

		return jpaCriteriaQuery.list();
	}


	@Override
	public SqlQueryBuilder query(String sqlOrQueryName) {
		SqlQueryBuilder sqlQueryBuilder = new NativeSqlQueryBuilder(entityManager);
		ReflectionUtils.setField("sqlOrQueryName", sqlQueryBuilder, sqlOrQueryName);
		ReflectionUtils.setField("hibernateQueryMode", sqlQueryBuilder, hibernateQueryMode);
		ReflectionUtils.setField("enumLookupProperties", sqlQueryBuilder, enumLookupProperties);
		return sqlQueryBuilder;
	}

	@Override
	public <T> T findOne(Class<T> entityClass, String propertyName, Object value) {
		List<T> resultList = null;
		JPACriteriaQuery<T> jpaCriteriaQuery =
				JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache);
		if (value == null) {
			jpaCriteriaQuery.isNull(propertyName);
		} else {
			jpaCriteriaQuery.eq(propertyName, value);
		}
		resultList = jpaCriteriaQuery.list();
		return resultList.isEmpty() ? null : resultList.get(0);
	}

	@Override
	public <T, PK extends Serializable> Optional<T> findOne(Class<T> clazz, PK id) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		jakarta.persistence.criteria.Predicate idPredicate = criteriaBuilder.equal(root.get("id"), id);
		criteriaQuery.select(root)
				.where(idPredicate)
				.distinct(true);
		TypedQuery<T> query = em().createQuery(criteriaQuery);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		List<T> results = query.getResultList();
		if (results.isEmpty()) {
			return Optional.ofNullable(null);
		}
		return Optional.ofNullable(results.get(0));
	}

	@Override
	public <T, PK extends Serializable> T load(Class<T> entityClass, PK id) {
		try {
			return em().getReference(entityClass, id);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass) {
		CriteriaQuery<T> criteriaQuery = em().getCriteriaBuilder().createQuery(entityClass);
		criteriaQuery.from(entityClass);
		TypedQuery<T> query = em().createQuery(criteriaQuery);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		return query.getResultList();
	}


	@Override
	public <T> List<T> findIn(Class<T> entityClass, final String propertyName, Collection<?> values) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (isEmpty(values)) {
			return new ArrayList<>();
		}

		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.in(propertyName, values);
		if (!includeDeleted) {
			jpaCriteriaQuery.eq("deleted", false);
		}
		return jpaCriteriaQuery.list();
	}

	public <T, E> List<T> findIn(Class<T> entityClass, String propertyName, E[] values) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (values == null || values.length == 0) {
			return new ArrayList<>();
		}
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.in(propertyName, asList(values));
		if (!includeDeleted) {
			jpaCriteriaQuery.eq("deleted", false);
		}
		return jpaCriteriaQuery.list();
	}

	@Override
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, LocalDateTime begin,
	                               LocalDateTime end) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (begin == null && end == null) {
			throw new IllegalArgumentException("begin and end cannot be null at the same time!");
		}
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.between(propertyName, begin, end);
		if (!includeDeleted) {
			jpaCriteriaQuery.eq("deleted", false);
		}
		return jpaCriteriaQuery.list();
	}

	@Override
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, Long begin, Long end) {
		Objects.requireNonNull(propertyName);
		Objects.requireNonNull(begin);
		Objects.requireNonNull(end);
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.between(propertyName, begin, end);
		return jpaCriteriaQuery.list();
	}


	@Override
	public <T> List<T> findIsNull(Class<T> entityClass, String propertyName) {
		Objects.requireNonNull(propertyName, "propertyName cannot be null!");
		JPACriteriaQuery<T> jpaCriteriaQuery =
				JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache);
		jpaCriteriaQuery.isNull(propertyName);
		return jpaCriteriaQuery.list();
	}

	@Override
	public <T> T ensureExists(Class<T> entityClass, String propertyName, Object value) throws EntityNotFoundException {
		List<T> resultList = findList(entityClass, propertyName, value);
		T entity = resultList.isEmpty() ? null : resultList.get(0);
		if (entity == null) {
			throw new EntityNotFoundException(format("Unable to find {0} by property {1} with value {2}",
					entityClass.getSimpleName(), propertyName, value));
		}
		return entity;
	}

	@Override
	public <T> boolean ifExists(Class<T> entityClass, String propertyName, Object value) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();

		CriteriaQuery<Boolean> query = criteriaBuilder.createQuery(Boolean.class);
		query.from(entityClass);
		query.select(criteriaBuilder.literal(true));

		Subquery<T> subquery = query.subquery(entityClass);
		Root<T> subRootEntity = subquery.from(entityClass);
		subquery.select(subRootEntity);

		Path<?> attributePath = subRootEntity.get(propertyName);
		jakarta.persistence.criteria.Predicate predicate = criteriaBuilder.equal(attributePath,
				criteriaBuilder.literal(value));
		subquery.where(predicate);
		query.where(criteriaBuilder.exists(subquery));

		TypedQuery<Boolean> typedQuery = em().createQuery(query);
		List<Boolean> results = typedQuery.getResultList();
		if (results.isEmpty()) {
			return false;
		}
		return results.get(0);
	}

	@Override
	public <T> List<T> find(String jpql, Class<T> clazz) {
		try {
			return find(jpql, null, null, clazz);
		} catch (Throwable e) {
			log.error("msg", e);
			throw new JPQLException(e);
		}
	}

	@Override
	public <T> T findUnique(String jpql, Class<T> clazz) {
		List<T> results = find(jpql, clazz);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public <T> T findUnique(String jpql, Map<String, Object> params, Class<T> resultClass) {
		List<T> results = find(jpql, params, resultClass);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public <T> T findUnique(String jpql, String paramName, Object paramValue, Class<T> clazz) {
		List<T> results = find(jpql, paramName, paramValue, clazz);
		return results.isEmpty() ? null : results.get(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> find(String jpql, Map<String, Object> params, Class<T> resultClass) {
		try {
			return createQuery(jpql, params, resultClass).getResultList();
		} catch (Throwable e) {
			log.error("msg", e);
			throw new JPQLException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> find(String jpql, String paramName, Object paramValue, Class<T> clazz) {
		try {
			return createQuery(jpql, paramName, paramValue, clazz).getResultList();
		} catch (Throwable e) {
			log.error("msg", e);
			throw new JPQLException(e);
		}
	}

	/**
	 * 返回单个对象，不存在则返回null
	 *
	 * @param queryName
	 * @param paramName
	 * @param paramValue
	 * @param clazz
	 * @return T
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public <T> T findOne(String queryName, String paramName, Object paramValue, Class<T> clazz) {
		//如果参数值是List类型，那么该SQL语句认为是IN查询，如果List的size则为0，就不需要查询了，直接返回空的ArrayList
		if (paramValue instanceof List) {
			if (((List) paramValue).size() == 0) {
				return null;
			}
		}
		Map<String, Object> params = new HashMap<String, Object>();
		if (isNotBlank(paramName)) {
			params.put(paramName, paramValue);
		}
		List<T> results = findList(queryName, params, clazz);
		if (results.isEmpty()) {
			return null;
		}

		return results.get(0);
	}


	@Override
	public <T> List<T> findList(String queryName, Class<T> clazz) {
		return findList(queryName, null, clazz);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <T> List<T> findList(String queryName, String paramName, Object paramValue, Class<T> clazz) {
		//如果参数值是List类型，那么该SQL语句认为是IN查询，如果List的斯则为0，就不需要查询了，直接返回空的ArrayList
		if (paramValue instanceof List) {
			if (((List) paramValue).size() == 0) {
				return new ArrayList<>();
			}
		}
		Map<String, Object> params = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(paramName)) {
			params.put(paramName, paramValue);
		}
		return findList(queryName, params, clazz);
	}

	@Override
	public <T> List<T> findList(String queryName, Map<String, Object> params, Class<T> clazz) {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			rawQuery = queryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			query = em().createNamedQuery(queryName)
					.unwrap(org.hibernate.query.Query.class);
			rawQuery = ReflectionUtils.getFieldValue("originalSqlString", query);
		}
		StringBuilder queryString = new StringBuilder(rawQuery);


		//建立context， 并放入数据
		VelocityContext context = new VelocityContext();
		context.put("StringUtils", StringUtils.class);
		if (isNotEmpty(params)) {
			for (String paramName : params.keySet()) {
				context.put(paramName, params.get(paramName));
			}
		}
		for (String contextName : classMap.keySet()) {
			context.put(contextName, classMap.get(contextName));
		}
		//解析后数据的输出目标，java.io.Writer的子类
		StringWriter sql = new StringWriter();
		//进行解析
		Velocity.evaluate(context, sql, queryName, queryString.toString());

		String parsedSQL = sql.toString();
		query = em()
				.createNativeQuery(parsedSQL)
				.unwrap(org.hibernate.query.Query.class);
		query.setResultTransformer(ResultTransformerFactory.getResultTransformer(HashUtils.sha256(parsedSQL), clazz,
				hibernateQueryMode,
				enumLookupProperties));

		if (isNotEmpty(params)) {
			/*
			 * 如果params里面某个key对应的value是null, 下面query.setProperties(params)会抛NullpointException, 所以这里要移除值为null的key
			 */
			Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = iterator.next();
				if (entry.getValue() == null) {
					iterator.remove();
				}
			}
			for (String key : params.keySet()) {
				Object value = params.get(key);
				processInOperate(params, key, value);
			}
			query.setProperties(params);
		}

		List<T> resultList;
		try {
			resultList = query.getResultList();
		} catch (Throwable e) {
			String msg = format("\nFailed to get resultlist from query\n{0}\n Parameters\n{1}!",
					sql,
					JacksonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		}

		return resultList;
	}

	@Override
	public <T> int deleteIn(Class<T> entityClass, String propertyName, Collection<?> values) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
		CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(entityClass);
		Root<T> root = criteriaDelete.from(entityClass);
		criteriaDelete.where(root.get(propertyName).in(values));

		return em().createQuery(criteriaDelete).executeUpdate();
	}

	@Override
	public <T, PK extends Serializable> T ensureEntityExists(Class<T> entityClass,
	                                                         PK id) throws EntityNotFoundException {
		T entity = get(entityClass, id);
		if (entity == null) {
			throw new EntityNotFoundException(format("Unable to find {0} with id {1}", entityClass.getSimpleName(),
					id));
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, PK extends Serializable> List<T> ensureMultiEntityExists(Class<T> entityClass, PK... ids) {
		Session session = em().unwrap(Session.class);
		MultiIdentifierLoadAccess<T> multiIdentifierLoadAccess = session.byMultipleIds(entityClass);
		List<T> entities = multiIdentifierLoadAccess.multiLoad(ids);
		if (entities.size() != ids.length) {
			throw new EntityNotFoundException(format("Unable to find {0} with id {1}", entityClass.getSimpleName(),
					ids));
		}
		for (T entity : entities) {
			if (entity == null) {
				throw new EntityNotFoundException(format("Unable to find {0} with id {1}", entityClass.getSimpleName()
						, ids));
			}
		}
		return entities;
	}

	@Override
	public <T, PK extends Serializable> List<T> ensureMultiEntityExists(Class<T> entityClass, List<PK> ids) {
		requireNonNull(ids, "ids cannot be null!");
		Session session = em().unwrap(Session.class);
		MultiIdentifierLoadAccess<T> multiIdentifierLoadAccess = session.byMultipleIds(entityClass);
		List<T> entities = multiIdentifierLoadAccess.multiLoad(ids);
		if (entities.size() != ids.size()) {
			throw new EntityNotFoundException(format("Unable to find {0} with id {1}", entityClass.getSimpleName(),
					ids));
		}
		for (T entity : entities) {
			if (entity == null) {
				throw new EntityNotFoundException(format("Unable to find {0} with id {1}", entityClass.getSimpleName()
						, ids));
			}
		}
		return entities;
	}

	@Override
	public <T> Query createQuery(String jpql, Class<T> resultClass) {
		Objects.requireNonNull(jpql, "jpql cannot be null");
		TypedQuery<T> query = em().createQuery(jpql, resultClass);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		return query;
	}

	@Override
	public <T> Query createQuery(final String jpql, String paramName, Object paramValue, Class<T> resultClass) {
		Objects.requireNonNull(jpql, "jpql cannot be null");
		TypedQuery<T> query = em().createQuery(jpql, resultClass);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		if (isNotBlank(paramName)) {
			query.setParameter(paramName, paramValue);
		}
		return query;
	}

	@Override
	public <T> void detach(T entity) {
		requireNonNull(entity, "entity cannot be null!");
		em().detach(entity);
	}

	@Override
	public <T> void detach(List<T> entities) {
		requireNonNull(entities, "entities cannot be null!");
		entities.forEach((entity) -> {
			requireNonNull(entity);
			em().detach(entity);
		});
	}

	@Override
	public Object findOne(String queryName) {
		List<?> results = query4RawList(queryName, null);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public <T> T findOne(String queryName, String paramName, Object paramValue) {
		Map<String, Object> params = new HashMap<>();
		if (!isBlank(paramName)) {
			params.put(paramName, paramValue);
		}
		List<?> results = query4RawList(queryName, params);
		return results.isEmpty() ? null : (T)results.get(0);
	}

	@Override
	public int execute(String queryName, String paramName, Object paramValue) {
		Map<String, Object> params = new HashMap<>();
		if (!isBlank(paramName)) {
			params.put(paramName, paramValue);
		}
		return execute(queryName, params);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int execute(String queryName, Map<String, Object> params) {
		String rawQuery = null;
		org.hibernate.query.Query<Integer> query = null;
		if (isSqlStatement(queryName)) {
			rawQuery = queryName;
		} else {
			query =
					em().createNamedQuery(queryName).unwrap(org.hibernate.query.Query.class);
			rawQuery = ReflectionUtils.getFieldValue("originalSqlString", query);
		}

		//建立context， 并放入数据  
		VelocityContext context = new VelocityContext();
		context.put("StringUtils", StringUtils.class);
		if (isNotEmpty(params)) {
			for (String paramName : params.keySet()) {
				context.put(paramName, params.get(paramName));
			}
		}
		for (String contextName : classMap.keySet()) {
			context.put(contextName, classMap.get(contextName));
		}
		//解析后数据的输出目标，java.io.Writer的子类  
		StringWriter sql = new StringWriter();
		//进行解析  
		Velocity.evaluate(context, sql, queryName, rawQuery);

		String parsedSQL = sql.toString();
		query = em().createNativeQuery(parsedSQL)
				.unwrap(org.hibernate.query.Query.class);

		if (isNotEmpty(params)) {
			for (String key : params.keySet()) {
				Object value = params.get(key);
				processInOperate(params, key, value);
			}
			query.setProperties(params);
		}

		try {
			return query.executeUpdate();
		} catch (Throwable e) {
			String msg = format("\nFailed to get resultlist from query\n{0}\n Parameters\n{1}!",
					sql,
					JacksonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (String contextName : getContextClasses().keySet()) {
			String className = getContextClasses().get(contextName);
			Class<?> contextClass;
			try {
				contextClass = Class.forName(className);
				if (contextClass != null) {
					classMap.put(contextName, contextClass);
				}
			} catch (ClassNotFoundException e) {
				log.error("Class [{}] does not exists", className);
			}
		}
	}

	@Override
	public void flush() {
		em().flush();
	}

	/**
	 * Bind named parameters.
	 *
	 * @param params
	 */
	private <T> Query createQuery(final String jpql, final Map<String, ?> params, Class<T> resultClass) {
		Objects.requireNonNull(jpql, "jpql cannot be null");
		TypedQuery<T> query = em().createQuery(jpql, resultClass);
		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}
		return setParameters(query, params);
	}

	/**
	 * 如果value是List或者数组，当他们是空、长度为0，则需要特殊处理一下，将value改写为'',这样SQL IN 语句才不会出错
	 *
	 * @param params
	 * @param key
	 * @param value
	 */
	private void processInOperate(Map<String, Object> params, String key, Object value) {
		if (value == null) {
			return;
		}

		if (value instanceof List) {
			List<?> values = (List<?>) value;
			if (values.size() == 0) {
				params.put(key, "''");
			}
			return;
		}

		ArrayTypes arrayTypes = ARRAY_TYPE_MAP.get(value.getClass().getName());
		if (arrayTypes == null) {
			return;
		}

		switch (arrayTypes) {
			case LONG_WRAPPER:
				Long[] arr1 = (Long[]) value;
				if (arr1.length == 0) {
					params.put(key, "''");
				}
				break;
			case LONG:
				long[] arr2 = (long[]) value;
				if (arr2.length == 0) {
					params.put(key, "''");
				}
				break;
			case INTEGER:
				int[] arr3 = (int[]) value;
				if (arr3.length == 0) {
					params.put(key, "''");
				}
				break;
			case INTEGER_WRAPPER:
				Integer[] arr4 = (Integer[]) value;
				if (arr4.length == 0) {
					params.put(key, "''");
				}
				break;
			case STRING:
				String[] arr5 = (String[]) value;
				if (arr5.length == 0) {
					params.put(key, "''");
				}
				break;
			case DOUBLE:
				double[] arr6 = (double[]) value;
				if (arr6.length == 0) {
					params.put(key, "''");
				}
				break;
			case DOUBLE_WRAPPER:
				Double[] arr7 = (Double[]) value;
				if (arr7.length == 0) {
					params.put(key, "''");
				}
				break;
			case FLOAT:
				float[] arr8 = (float[]) value;
				if (arr8.length == 0) {
					params.put(key, "''");
				}
				break;
			case FLOAT_WRAPPER:
				Float[] arr9 = (Float[]) value;
				if (arr9.length == 0) {
					params.put(key, "''");
				}
				break;

			default:
				break;
		}
	}

	private boolean isEmpty(Collection entities) {
		return entities == null || entities.isEmpty();
	}

	private boolean isNotEmpty(Collection entities) {
		return entities != null && !entities.isEmpty();
	}

	private boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}

	private boolean isNotEmpty(Map map) {
		return map != null && !map.isEmpty();
	}

	private Query setParameters(Query query, Map<String, ?> params) {
		if (isEmpty(params)) {
			return query;
		}

		for (String paramName : params.keySet()) {
			query.setParameter(paramName, params.get(paramName));
		}
		return query;
	}

	public Map<String, String> getContextClasses() {
		return contextClasses;
	}

	public void setContextClasses(Map<String, String> contextClasses) {
		this.contextClasses = contextClasses;
	}

	public OrderBean getOrder() {
		return order;
	}

	public void setOrder(OrderBean order) {
		this.order = order;
	}

	public Set<String> getEnumLookupProperties() {
		return enumLookupProperties;
	}

	public void setEnumLookupProperties(Set<String> enumLookupProperties) {
		this.enumLookupProperties = enumLookupProperties;
	}

	/**
	 * 判断是否在spring事务中
	 *
	 * @return
	 */
	public boolean isInSpringTransaction() {
		return TransactionSynchronizationManager.isActualTransactionActive();
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

	/**
	 * 如果未开启spring事务，则需要手动开启事务
	 */
	public void begin() {
		if (isInSpringTransaction()) {
			log.warn("Spring transaction is active, no need to begin transaction");
			return;
		} else {
			em().getTransaction().begin();
		}
	}

	/**
	 * 如果未开启spring事务，则需要手动提交事务
	 */
	public void commit() {
		if (isInSpringTransaction()) {
			log.warn("Spring transaction is active, no need to commit transaction");
			return;
		} else {
			em().getTransaction().commit();
		}
	}

	/**
	 * 如果未开启spring事务，则需要手动回滚事务
	 */
	public void rollback() {
		if (isInSpringTransaction()) {
			log.debug("Spring transaction is active, no need to commit transaction");
			return;
		} else {
			em().getTransaction().rollback();
		}
	}

	private boolean isSqlStatement(String queryName) {
		Matcher matcher = SELECT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			return true;
		}

		matcher = UPDATE_PATTERN.matcher(queryName);
		if (matcher.find()) {
			return true;
		}

		matcher = DELETE_PATTERN.matcher(queryName);
		if (matcher.find()) {
			return true;
		}

		matcher = INSERT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			return true;
		}

		return false;
	}


	private <T> List<T> query4RawList(String queryName, Map<String, Object> params) {
		String queryString = null;
		org.hibernate.query.Query<?> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			queryString = queryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {
			query = em()
					.createNamedQuery(queryName)
					.unwrap(org.hibernate.query.Query.class);

			queryString = ReflectionUtils.getFieldValue("originalSqlString", query);
		}
		//建立context， 并放入数据
		VelocityContext context = new VelocityContext();
		if (isNotEmpty(params)) {
			for (String paramName : params.keySet()) {
				context.put(paramName, params.get(paramName));
			}
		}
		for (String contextName : classMap.keySet()) {
			context.put(contextName, classMap.get(contextName));
		}
		//解析后数据的输出目标，java.io.Writer的子类
		StringWriter sql = new StringWriter();
		//进行解析
		Velocity.evaluate(context, sql, queryName, queryString);
		queryString = sql.toString();

		query = em()
				.createNativeQuery(queryString)
				.unwrap(org.hibernate.query.Query.class);

		if (hibernateUseQueryCache) {
			query.setHint(HINT_QUERY_CACHE, true);
		}

		if (isNotEmpty(params)) {
			query.setProperties(params);
		}
		try {
			return (List<T>) query.getResultList();
		} catch (Throwable e) {
			String msg = format("Execute raw SQL query[{0}] with parameter[{1}] failed!", queryString,
					JacksonUtils.toJson(params));
			throw new RawSQLQueryException(msg, e);
		}
	}

}
