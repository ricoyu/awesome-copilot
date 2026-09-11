package com.awesomecopilot.orm.dao;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.orm.criteria.JPACriteriaQuery;
import com.awesomecopilot.orm.exception.EntityOperationException;
import com.awesomecopilot.orm.exception.PersistenceException;
import com.awesomecopilot.orm.exception.RawSQLQueryException;
import com.awesomecopilot.orm.exception.SQLQueryException;
import com.awesomecopilot.orm.transformer.ResultTransformerFactory;
import com.awesomecopilot.orm.utils.HashUtils;
import com.awesomecopilot.orm.utils.JsonUtils;
import com.awesomecopilot.orm.utils.NamedQueryUtils;
import com.awesomecopilot.orm.utils.SQLUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
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
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 基于JPA 2.1封装的通用DAO
 *
 * @author Rico Yu
 * @since Mar 6, 2016
 */
@Repository
public class JpaDao implements SQLOperations, CriteriaOperations,
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

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/**
	 * 无 Spring 事务时，当前线程自建 EntityManager 的持有者
	 */
	private final ThreadLocal<EntityManagerHolder> entityManagerHolderThreadLocal = new ThreadLocal<>();

	@PersistenceContext
	protected EntityManager entityManager;

	/**
	 * 是否自动支持逻辑删除
	 */
	@Value("${copilot.orm.logical-delete.enabled:false}")
	private boolean logicalDeleteEnabled = false;
	/**
	 * 逻辑删除的字段名
	 */
	@Value("${copilot.orm.logical-delete.field:deleted}")
	private String logicalDeleteField = "deleted";

	@Value("${hibernate.query.mode:loose}")
	private String hibernateQueryMode = "loose";

	@Value("${hibernate.query.cache:false}")
	private boolean hibernateUseQueryCache = false;

	/**
	 * 是否自动修复SQL, 比如动态SQL因为某些条件没传, 导致多了一个 AND 关键字之类
	 */
	@Value("${copilot.orm.sql.auto-fix:true}")
	private boolean sqlAutofix = true;

	@Value("${copilot.orm.sql.batch-size:100}")
	private int batchSize = 100;

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

		Properties properties = new Properties();
		properties.setProperty("userdirective",
				"com.awesomecopilot.orm.directive.IfNotNull," +
						"com.awesomecopilot.orm.directive.IfNull," +
						"com.awesomecopilot.orm.directive.Between," +
						"com.awesomecopilot.orm.directive.IfEqual," +
						"com.awesomecopilot.orm.directive.IfPresent");
		properties.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log" +
				".Log4JLogChute");
		properties.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
		//初始化运行时引擎
		Velocity.init(properties);
		log.info("Velocity初始化完成");
	}

	@PostConstruct
	public void initialize() {
		try {
			SQLUtils.configureLogicalDelete(this.logicalDeleteEnabled, this.logicalDeleteField);
			this.query("首次使用前的初始化");
			log.info("首次使用前的初始化完成");
		} finally {
			releaseEntityManagerIfIdle();
		}
	}

	/**
	 * Make an instance managed and persistent.
	 *
	 * @param entity
	 */
	@Override
	public <T> void persist(T entity) {
		try {
			persistImpl(entity);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void persistImpl(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			em().persist(entity);
		} catch (Throwable e) {
			String msg = MessageFormat.format("Entity: {0}", JsonUtils.toPrettyJson(entity));
			log.error(msg, e);
			throw new PersistenceException(e);
		}
	}

	@Override
	public <T> void persist(List<T> entities) {
		try {
			persistImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void persistImpl(List<T> entities) {
		Objects.requireNonNull(entities, "entities cannot be null");
		if (entities.isEmpty()) {
			return;
		}
		try {
			for (int i = 0; i < entities.size(); i++) {
				persistImpl(entities.get(i));
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
		try {
			return mergeImpl(entity);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> T mergeImpl(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			return em().merge(entity);
		} catch (Throwable e) {
			String msg = format("Entity: {0}", JsonUtils.toPrettyJson(entity));
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
		try {
			return mergeImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> mergeImpl(List<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		for (T entity : entities) {
			Objects.requireNonNull(entity, "entity cannot be null");
			try {
				results.add(em().merge(entity));
			} catch (Throwable e) {
				String msg = format("Entity: {0}", JsonUtils.toPrettyJson(entity));
				log.error(msg, e);
				throw new PersistenceException(e);
			}
		}
		flush();
		return results;
	}

	@Override
	public <T> T save(T entity) {
		try {
			return saveImpl(entity);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> T saveImpl(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		/*
		 * 找到主键字段, 原来按照字段名称来找, 固定找"id"字段, 但实际开发可能主键字段不叫id, 而叫xxx_id
		 * 所以2026-01-23改成找到标注了@Id注解的字段, 然后取该字段的值, 这样比较稳妥
		 */
		Field idField = ReflectionUtils.findFirstFieldWithAnnotation(entity.getClass(), Id.class);
		Object id = ReflectionUtils.getFieldValue(idField, entity);
		if (id == null) {
			persistImpl(entity);
			return entity;
		} else {
			try {
				return em().merge(entity);
			} catch (Throwable e) {
				String msg = format("Entity: {0}", JsonUtils.toPrettyJson(entity));
				log.error(msg, e);
				throw new PersistenceException(e);
			}
		}
	}

	@Override
	public <T> List<T> save(List<T> entities) {
		try {
			return saveImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> saveImpl(List<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		for (int i = 0, length = entities.size(); i < length; i++) {
			results.add(saveImpl(entities.get(i)));
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
		try {
			return saveImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> saveImpl(Set<T> entities) {
		if (isEmpty(entities)) {
			return emptyList();
		}
		List<T> results = new ArrayList<>();
		int i = 0;
		for (T entity : entities) {
			results.add(saveImpl(entity));
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
		try {
			deleteImpl(entity);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void deleteImpl(T entity) {
		Objects.requireNonNull(entity, "entity cannot be null");
		try {
			em().remove(entity);
		}catch (EntityNotFoundException e) {
			throw new com.awesomecopilot.common.lang.exception.EntityNotFoundException("要删除的记录不存在",  e);
		}
		catch (Throwable e) {
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
		try {
			deleteImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void deleteImpl(List<T> entities) {
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
		deleteByPKBulk(entityClass, List.of(id));
	}

	/**
	 * 根据主键数组删除
	 *
	 * @param entityClass 实体类
	 * @param ids         主键数组
	 */
	@Override
	public <T, PK extends Serializable> void deleteByPK(Class<T> entityClass, PK[] ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (ids.length == 0) {
			return;
		}
		deleteByPKBulk(entityClass, Arrays.stream(ids).filter(Objects::nonNull).collect(toList()));
	}

	@Override
	public <T> void deleteByPK(Class<T> entityClass, long[] ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (ids.length == 0) {
			return;
		}
		List<Long> idList = new ArrayList<>(ids.length);
		for (long id : ids) {
			idList.add(id);
		}
		deleteByPKBulk(entityClass, idList);
	}

	@Override
	public <T> void deleteByPK(Class<T> entityClass, int[] ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (ids.length == 0) {
			return;
		}
		List<Integer> idList = new ArrayList<>(ids.length);
		for (int id : ids) {
			idList.add(id);
		}
		deleteByPKBulk(entityClass, idList);
	}

	@Override
	public <T, PK extends Serializable> void deleteByPK(Class<T> entityClass, Collection<PK> ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (ids.isEmpty()) {
			return;
		}
		deleteByPKBulk(entityClass, ids.stream().filter(Objects::nonNull).collect(toList()));
	}

	private String resolveIdAttributeName(Class<?> entityClass) {
		Field idField = ReflectionUtils.findFirstFieldWithAnnotation(entityClass, Id.class);
		if (idField == null) {
			throw new IllegalArgumentException(
					"No @Id field found on entity class: " + entityClass.getName());
		}
		return idField.getName();
	}

	private void applyLogicalDeleteFilter(JPACriteriaQuery<?> query, Class<?> entityClass,
	                                      boolean includeDeleted) {
		if (!logicalDeleteEnabled || includeDeleted) {
			return;
		}
		query.eq(logicalDeleteField, resolveNotDeletedValue(entityClass));
	}

	private Object resolveNotDeletedValue(Class<?> entityClass) {
		Field field = ReflectionUtils.findField(logicalDeleteField, entityClass);
		if (field == null) {
			return false;
		}
		Class<?> type = field.getType();
		if (type == boolean.class || type == Boolean.class) {
			return false;
		}
		if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
			return 0;
		}
		return false;
	}

	private <T> void deleteByPKBulk(Class<T> entityClass, Collection<?> ids) {
		try {
			deleteByPKBulkImpl(entityClass, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	private <T> void deleteByPKBulkImpl(Class<T> entityClass, Collection<?> ids) {
		if (ids.isEmpty()) {
			return;
		}
		List<?> idList = ids.stream().distinct().collect(toList());
		String idAttribute = resolveIdAttributeName(entityClass);
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();

		for (int i = 0; i < idList.size(); i += batchSize) {
			List<?> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
			if (logicalDeleteEnabled) {
				CriteriaUpdate<T> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(entityClass);
				Root<T> root = criteriaUpdate.from(entityClass);
				criteriaUpdate.set(root.get(logicalDeleteField), true);
				criteriaUpdate.where(root.get(idAttribute).in(batch));
				em().createQuery(criteriaUpdate).executeUpdate();
			} else {
				CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(entityClass);
				Root<T> root = criteriaDelete.from(entityClass);
				criteriaDelete.where(root.get(idAttribute).in(batch));
				em().createQuery(criteriaDelete).executeUpdate();
			}
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
		try {
			return getImpl(clazz, id);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> T getImpl(Class<T> clazz, PK id) {
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
		try {
			return getMultiImpl(clazz, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> getMultiImpl(Class<T> clazz, PK... ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		// ==========核心兼容逻辑：拦截 long[] 被封装成单个元素的场景==========
		Object[] targetIds = ids;
		// 兼容 int[] 原生数组
		if (ids.length == 1 && ids[0] instanceof int[]) {
			int[] arr = (int[]) ids[0];
			targetIds = Arrays.stream(arr).boxed().toArray(Integer[]::new);
		}
		// 兼容 long[] 原生数组
		else if (ids.length == 1 && ids[0] instanceof long[]) {
			long[] arr = (long[]) ids[0];
			targetIds = Arrays.stream(arr).boxed().toArray(Long[]::new);
		}
		if (log.isDebugEnabled()) {
			log.debug("Try to find " + clazz.getName() + " by ids " + ids);
		}
		try {
			Session session = em().unwrap(Session.class);
			return session.byMultipleIds(clazz).multiLoad(targetIds);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	/*@Override
	public <T, PK extends Serializable> List<T> getMulti(Class<T> clazz, long... ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		if (log.isDebugEnabled()) {
			log.debug("Try to find " + clazz.getName() + " by ids " + ids);
		}
		try {
			Object[] idArr = toBoxedArray(ids);
			Session session = em().unwrap(Session.class);
			return session.byMultipleIds(clazz).multiLoad(idArr);
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}*/

	@Override
	public <T, PK extends Serializable> List<T> getMulti(Class<T> clazz, List<PK> ids) {
		try {
			return getMultiImpl(clazz, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> getMultiImpl(Class<T> clazz, List<PK> ids) {
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
	public <T, PK extends Serializable> List<T> listByIds(Class<T> entityClass, PK... ids) {
		try {
			return listByIdsImpl(entityClass, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> listByIdsImpl(Class<T> entityClass, PK... ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		log.debug("Try to find " + entityClass.getName() + " by ids " + ids);
		try {
			List<PK> distinctIds = asList(ids).stream().distinct().collect(toList());
			Session session = em().unwrap(Session.class);
			List<T> entities = session.byMultipleIds(entityClass).multiLoad(distinctIds);
			return entities.stream().filter(Objects::nonNull).collect(toList());
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T, PK extends Serializable> List<T> listByIds(Class<T> entityClass, List<PK> ids) {
		try {
			return listByIdsImpl(entityClass, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> listByIdsImpl(Class<T> entityClass, List<PK> ids) {
		Objects.requireNonNull(ids, "ids cannot be null");
		log.debug("Try to find " + entityClass.getName() + " by ids " + ids);
		try {
			List<PK> distinctIds = ids.stream().distinct().collect(toList());
			Session session = em().unwrap(Session.class);
			List<T> entities = session.byMultipleIds(entityClass).multiLoad(distinctIds);
			return entities.stream().filter(Objects::nonNull).collect(toList());
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T, PK extends Serializable> T find(Class<T> clazz, PK id) {
		try {
			return findImpl(clazz, id);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> T findImpl(Class<T> clazz, PK id) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		String idAttribute = resolveIdAttributeName(clazz);
		jakarta.persistence.criteria.Predicate idPredicate =
				criteriaBuilder.equal(root.get(idAttribute), id);
		if (logicalDeleteEnabled) {
			jakarta.persistence.criteria.Predicate deletedPredicate = criteriaBuilder.equal(
					root.get(logicalDeleteField), resolveNotDeletedValue(clazz));
			criteriaQuery.select(root).where(idPredicate, deletedPredicate).distinct(true);
		} else {
			criteriaQuery.select(root).where(idPredicate).distinct(true);
		}
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
		try {
			return findListImpl(entityClass, propertyName, value);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findListImpl(Class<T> entityClass, String propertyName, Object value) {
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
		NativeSqlQueryBuilder sqlQueryBuilder = new NativeSqlQueryBuilder(entityManager, entityManagerFactory);
		sqlQueryBuilder.setSqlOrQueryName(sqlOrQueryName);
		//auto-fix 以 JpaDao 的 Spring 配置为单一来源, builder 不再自行读 application.yml
		sqlQueryBuilder.setSqlAutofix(sqlAutofix);
		sqlQueryBuilder.setHibernateQueryMode(hibernateQueryMode);
		sqlQueryBuilder.setEnumLookupProperties(enumLookupProperties);
		//ReflectionUtils.setField("sqlOrQueryName", sqlQueryBuilder, sqlOrQueryName);
		//ReflectionUtils.setField("hibernateQueryMode", sqlQueryBuilder, hibernateQueryMode);
		//ReflectionUtils.setField("enumLookupProperties", sqlQueryBuilder, enumLookupProperties);
		return sqlQueryBuilder;
	}

	@Override
	public CriteriaQueryBuilder query(Class entityClass) {
		return new CriteriaQueryBuilder(entityManager, entityManagerFactory, entityClass);
	}

	@Override
	public <T> T findOne(Class<T> entityClass, String propertyName, Object value) {
		try {
			return findOneImpl(entityClass, propertyName, value);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> T findOneImpl(Class<T> entityClass, String propertyName, Object value) {
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
		try {
			return findOneImpl(clazz, id);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> Optional<T> findOneImpl(Class<T> clazz, PK id) {
		CriteriaBuilder criteriaBuilder = em().getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		String idAttribute = resolveIdAttributeName(clazz);
		jakarta.persistence.criteria.Predicate idPredicate =
				criteriaBuilder.equal(root.get(idAttribute), id);
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
			return loadImpl(entityClass, id);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> T loadImpl(Class<T> entityClass, PK id) {
		try {
			T reference = em().getReference(entityClass, id);
			/*
			 * 无 Spring 事务且不在 begin() 手动事务中时, 本次终端调用结束即释放自建 EM,
			 * 懒代理会因失去 Session 而在访问属性时抛 LazyInitializationException。
			 * 这里立即初始化, 保证返回值脱离 EM 仍可用（语义上向 get() 靠拢）;
			 * 事务内不初始化, 保持懒加载原语义。记录不存在时会在此抛 EntityNotFoundException,
			 * 比延迟到属性访问时才报错更可预期。
			 */
			if (!isInSpringTransaction() && !entityManagerHolder().hasActiveLocalTransaction()) {
				org.hibernate.Hibernate.initialize(reference);
			}
			return reference;
		} catch (Throwable e) {
			log.error("", e);
			throw new EntityOperationException(e);
		}
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass) {
		try {
			return findAllImpl(entityClass);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findAllImpl(Class<T> entityClass) {
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
		try {
			return findInImpl(entityClass, propertyName, values);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findInImpl(Class<T> entityClass, final String propertyName, Collection<?> values) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (isEmpty(values)) {
			return new ArrayList<>();
		}

		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.in(propertyName, values);
		applyLogicalDeleteFilter(jpaCriteriaQuery, entityClass, includeDeleted);
		return jpaCriteriaQuery.list();
	}

	public <T, E> List<T> findIn(Class<T> entityClass, String propertyName, E[] values) {
		try {
			return findInImpl(entityClass, propertyName, values);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, E> List<T> findInImpl(Class<T> entityClass, String propertyName, E[] values) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (values == null || values.length == 0) {
			return new ArrayList<>();
		}
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.in(propertyName, asList(values));
		applyLogicalDeleteFilter(jpaCriteriaQuery, entityClass, includeDeleted);
		return jpaCriteriaQuery.list();
	}

	@Override
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, LocalDateTime begin,
	                               LocalDateTime end) {
		try {
			return findBetweenImpl(entityClass, propertyName, begin, end);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findBetweenImpl(Class<T> entityClass, String propertyName, LocalDateTime begin,
	                               LocalDateTime end) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		if (begin == null && end == null) {
			throw new IllegalArgumentException("begin and end cannot be null at the same time!");
		}
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.between(propertyName, begin, end);
		applyLogicalDeleteFilter(jpaCriteriaQuery, entityClass, includeDeleted);
		return jpaCriteriaQuery.list();
	}

	@Override
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, Long begin, Long end) {
		try {
			return findBetweenImpl(entityClass, propertyName, begin, end);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findBetweenImpl(Class<T> entityClass, String propertyName, Long begin, Long end) {
		boolean includeDeleted = false;
		Objects.requireNonNull(propertyName);
		Objects.requireNonNull(begin);
		Objects.requireNonNull(end);
		JPACriteriaQuery<T> jpaCriteriaQuery = JPACriteriaQuery.from(entityClass, em(), hibernateUseQueryCache)
				.between(propertyName, begin, end);
		applyLogicalDeleteFilter(jpaCriteriaQuery, entityClass, includeDeleted);
		return jpaCriteriaQuery.list();
	}


	@Override
	public <T> List<T> findIsNull(Class<T> entityClass, String propertyName) {
		try {
			return findIsNullImpl(entityClass, propertyName);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findIsNullImpl(Class<T> entityClass, String propertyName) {
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
	public CriteriaDeleteBuilder deleteBy(Class entityClass) {
		return new CriteriaDeleteBuilder(entityManager, entityManagerFactory, entityClass);
	}

	@Override
	public <T> boolean ifExists(Class<T> entityClass, String propertyName, Object value) {
		try {
			return ifExistsImpl(entityClass, propertyName, value);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> boolean ifExistsImpl(Class<T> entityClass, String propertyName, Object value) {
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

	/**
	 * 返回单个对象，不存在则返回null
	 *
	 * @param queryName
	 * @param paramName
	 * @param paramValue
	 * @param clazz      这个指的是SQL查询结果集要封装进哪个POJO对象里面, 查询单列不要用这个
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
	public <T> T findOne(String queryName, Map<String, Object> params, Class<T> clazz) {
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
		try {
			return findListImpl(queryName, params, clazz);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> List<T> findListImpl(String queryName, Map<String, Object> params, Class<T> clazz) {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			rawQuery = queryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			rawQuery = NamedQueryUtils.resolveNamedQueryString(em(), queryName);
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

		String preParsedSQL = sql.toString();
		String parsedSQL = preParsedSQL;
		if (sqlAutofix) {
			if (log.isDebugEnabled()) {
				log.debug("未裁剪前解析得到的原生SQL: \n {}", preParsedSQL);
			}
			parsedSQL = SQLUtils.build(preParsedSQL);
			if (log.isDebugEnabled()) {
				log.debug("裁剪后解析得到的原生SQL: \n {}", parsedSQL);
			}
		}
		query = em()
				.createNativeQuery(parsedSQL)
				.unwrap(org.hibernate.query.Query.class);
		query.setResultTransformer(ResultTransformerFactory.getResultTransformer(HashUtils.sha256(parsedSQL), clazz,
				hibernateQueryMode,
				enumLookupProperties));

		if (isNotEmpty(params)) {
			/*
			 * 绑参前先复制一份, 只清副本, 不动调用方传进来的 params。
			 * 旧实现直接在 params 上删 null 键、把空集合改成字符串 "''", 调用方拿着
			 * 同一个 map 再查第二次时参数已经残缺或被改样——这是个隐蔽的坑。
			 * null 值的键要剔除(setProperties 碰到 null 会抛 NullPointerException);
			 * 空 List/空数组原样绑定即可, Hibernate 会生成恒假条件, 查询返回 0 行。
			 */
			Map<String, Object> bindParams = new HashMap<>(params);
			bindParams.entrySet().removeIf(entry -> entry.getValue() == null);
			query.setProperties(bindParams);
		}

		List<T> resultList;
		try {
			resultList = query.getResultList();
		} catch (Throwable e) {
			String msg = format("\nFailed to get resultlist from query\n{0}\n Parameters\n{1}",
					sql,
					JsonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		} finally {
			releaseEntityManagerIfIdle();
		}

		return resultList;
	}
	
	@Override
	public <T> List<T> findRawList(String queryName, String propertyName, Object value) {
		Map<String, Object> params = new HashMap<>();
		params.put(propertyName, value);
		return query4RawList(queryName, params);
	}
	
	@Override
	public <T> int deleteIn(Class<T> entityClass, String propertyName, Collection<?> values) {
		try {
			return deleteInImpl(entityClass, propertyName, values);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> int deleteInImpl(Class<T> entityClass, String propertyName, Collection<?> values) {
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
		try {
			return ensureMultiEntityExistsImpl(entityClass, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> ensureMultiEntityExistsImpl(Class<T> entityClass, PK... ids) {
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
		try {
			return ensureMultiEntityExistsImpl(entityClass, ids);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T, PK extends Serializable> List<T> ensureMultiEntityExistsImpl(Class<T> entityClass, List<PK> ids) {
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
	public <T> void detach(T entity) {
		try {
			detachImpl(entity);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void detachImpl(T entity) {
		requireNonNull(entity, "entity cannot be null!");
		em().detach(entity);
	}

	@Override
	public <T> void detach(List<T> entities) {
		try {
			detachImpl(entities);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public <T> void detachImpl(List<T> entities) {
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
		return results.isEmpty() ? null : (T) results.get(0);
	}

	@Override
	public <T> T findOne(String queryName, Map<String, Object> params) {
		List<?> results = query4RawList(queryName, params);
		return results.isEmpty() ? null : (T) results.get(0);
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
		try {
			return executeImpl(queryName, params);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	public int executeImpl(String queryName, Map<String, Object> params) {
		String rawQuery = null;
		org.hibernate.query.Query<Integer> query = null;
		if (isSqlStatement(queryName)) {
			rawQuery = queryName;
		} else {
			rawQuery = NamedQueryUtils.resolveNamedQueryString(em(), queryName);
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
			//空集合不再偷换成字符串 "''": 直接绑空 List, DELETE/UPDATE 里的 IN 条件恒假,
			//影响 0 行; 而 "''" 绑到数字列在 MySQL 上会报类型转换错误(报告 P1-10)
			query.setProperties(params);
		}

		try {
			return query.executeUpdate();
		} catch (Throwable e) {
			String msg = format("\nFailed to get resultlist from query\n{0}\n Parameters\n{1}!",
					sql,
					JsonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		} finally {
			releaseEntityManagerIfIdle();
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
	// 修改em()方法，在适当时候调用清理
	@Override
	public EntityManager em() {
		return entityManagerHolder().get();
	}

	private EntityManagerHolder entityManagerHolder() {
		EntityManagerHolder holder = entityManagerHolderThreadLocal.get();
		if (holder == null) {
			holder = new EntityManagerHolder(entityManager, entityManagerFactory);
			entityManagerHolderThreadLocal.set(holder);
		}
		return holder;
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
			cleanupEntityManager();
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
			cleanupEntityManager();
		}
	}

	/**
	 * 无 Spring 事务且当前无活跃本地事务时，关闭自建 EntityManager（单次读/写场景）
	 */
	private void releaseEntityManagerIfIdle() {
		if (isInSpringTransaction()) {
			return;
		}
		EntityManagerHolder holder = entityManagerHolderThreadLocal.get();
		if (holder == null || holder.hasActiveLocalTransaction()) {
			return;
		}
		cleanupEntityManager();
	}
	/**
	 * P0-5: 终端操作统一收尾。无 Spring 事务且线程自建 EM 上没有活跃本地事务（begin() 手动
	 * 开启的）时，关闭并摘除自建 EntityManager；Spring 事务内与 begin()/commit() 流程中
	 * 为空操作，行为不变。
	 */
	private void releaseAfterTerminalOp() {
		releaseEntityManagerIfIdle();
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
		try {
			return query4RawListImpl(queryName, params);
		} finally {
			releaseAfterTerminalOp();
		}
	}

	private <T> List<T> query4RawListImpl(String queryName, Map<String, Object> params) {
		String queryString = null;
		org.hibernate.query.Query<?> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(queryName);
		if (matcher.find()) {
			queryString = queryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {
			queryString = NamedQueryUtils.resolveNamedQueryString(em(), queryName);
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
					JsonUtils.toJson(params));
			throw new RawSQLQueryException(msg, e);
		}
	}
	
	// 在JpaDao类中添加清理方法
	public void cleanupEntityManager() {
		EntityManagerHolder holder = entityManagerHolderThreadLocal.get();
		if (holder != null) {
			holder.closeIfNeeded();
			entityManagerHolderThreadLocal.remove();
		}
	}
	
	private Object[] toBoxedArray(long... ids) {
		Long[] boxedIds = new Long[ids.length];
		for (int i = 0; i < ids.length; i++) {
			boxedIds[i] = Long.valueOf(ids[i]);
		}
		return (Object[])boxedIds;
	}
}