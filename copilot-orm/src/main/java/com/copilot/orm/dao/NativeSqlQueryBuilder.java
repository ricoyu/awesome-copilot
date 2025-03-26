package com.copilot.orm.dao;

import com.copilot.common.lang.utils.ArrayTypes;
import com.copilot.common.lang.utils.PrimitiveUtils;
import com.copilot.common.lang.utils.ReflectionUtils;
import com.copilot.common.lang.utils.SqlUtils;
import com.copilot.common.lang.vo.OrderBean;
import com.copilot.common.lang.vo.Page;
import com.copilot.orm.exception.SQLCountQueryException;
import com.copilot.orm.exception.SQLQueryException;
import com.copilot.orm.transformer.ResultTransformerFactory;
import com.copilot.orm.utils.HashUtils;
import com.copilot.orm.utils.JsonUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 原生SQL查询生成器
 * <p/>
 * Copyright: Copyright (c) 2025-03-10 10:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class NativeSqlQueryBuilder implements SqlQueryBuilder {

	private static final Logger log = LoggerFactory.getLogger(NativeSqlQueryBuilder.class);

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

	private static final String IS_COUNT_QUERY = "isCountQuery";

	/**
	 * 默认会根据CREATE_TIME倒序排
	 */
	private static final OrderBean DFAULT_ORDER = new OrderBean("CREATE_TIME", OrderBean.DIRECTION.DESC);

	private static final ConcurrentMap<String, ArrayTypes> ARRAY_TYPE_MAP = new ConcurrentHashMap<>();

	private String sqlOrQueryName;

	private Map<String, Object> params = new HashMap<>();

	private List<OrderBean> orders = new ArrayList<>();

	private boolean logicalDelete;

	private String logicalDeleteField = "deleted";

	private Page page;

	private Class resultClass;

	protected final EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	private String hibernateQueryMode = "loose";

	/**
	 * 如果类的某个属性是enum类型，并且需要根据这个enum类型的某个属性来和数据库列值匹配，那么要指明这个属性的名字
	 */
	private Set<String> enumLookupProperties = new HashSet<>();

	/**
	 * Spring环境下拿到的是LocalContainerEntityManagerFactoryBean的代理类
	 */
	protected transient ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();

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
	}

	public NativeSqlQueryBuilder(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public NativeSqlQueryBuilder(EntityManager entityManager, String logicalDeleteField) {
		this.entityManager = entityManager;
		this.logicalDeleteField = logicalDeleteField;
	}

	public NativeSqlQueryBuilder(EntityManager entityManager, EntityManagerFactory entityManagerFactory,
	                             String logicalDeleteField) {
		this.entityManager = entityManager;
		this.entityManagerFactory = entityManagerFactory;
		this.logicalDeleteField = logicalDeleteField;
	}

	@Override
	public SqlQueryBuilder addParam(String paramName, Object paramValue) {
		if (isBlank(paramName)) {
			throw new IllegalArgumentException("paramName 不能为空");
		}
		params.put(paramName, paramValue);
		return this;
	}

	@Override
	public SqlQueryBuilder addParams(Map<String, Object> params) {
		this.params.putAll(params);
		return this;
	}

	@Override
	public SqlQueryBuilder page(Page page) {
		this.page = page;
		if (page.getOrder() != null) {
			orders.add(page.getOrder());
		}
		if (!page.getOrders().isEmpty()) {
			orders.addAll(page.getOrders());
		}
		return this;
	}

	@Override
	public SqlQueryBuilder page(int paggeNum, int pageSize) {
		Page page = new Page();
		page.setPageNum(paggeNum);
		page.setPageSize(pageSize);
		this.page = page;
		return this;
	}

	@Override
	public SqlQueryBuilder order(OrderBean order) {
		orders.add(order);
		return this;
	}

	@Override
	public SqlQueryBuilder order(String orderBy, OrderBean.DIRECTION direction) {
		OrderBean order = new OrderBean(orderBy, direction);
		orders.add(order);
		return this;
	}

	@Override
	public <T> SqlQueryBuilder resultClass(Class<T> resultClass) {
		this.resultClass = resultClass;
		return this;
	}

	@Override
	public <T> List<T> findList() {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(sqlOrQueryName);
		if (matcher.find()) {
			rawQuery = sqlOrQueryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			query = em().createNamedQuery(sqlOrQueryName)
					.unwrap(org.hibernate.query.Query.class);
			rawQuery = ReflectionUtils.getFieldValue("originalSqlString", query);
		}
		StringBuilder queryString = new StringBuilder(rawQuery);
		addOrder(queryString);

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
		Velocity.evaluate(context, sql, sqlOrQueryName, queryString.toString());

		String parsedSQL = sql.toString();
		query = em()
				.createNativeQuery(parsedSQL)
				.unwrap(org.hibernate.query.Query.class);
		Class clazz = null;
		if (this.resultClass != null) {
			clazz = this.resultClass;
		}
		if (clazz != null) {
			query.setResultTransformer(ResultTransformerFactory.getResultTransformer(HashUtils.sha256(parsedSQL),
					clazz,
					hibernateQueryMode,
					enumLookupProperties));
		}

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
					JsonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		}

		return resultList;
	}

	@Override
	public <T> List<T> findPage() {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(sqlOrQueryName);
		if (matcher.find()) {
			rawQuery = sqlOrQueryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			query = em().createNamedQuery(sqlOrQueryName).unwrap(org.hibernate.query.Query.class);
			rawQuery = ReflectionUtils.getFieldValue("originalSqlString", query);
		}
		StringBuilder queryString = new StringBuilder(rawQuery);

		// 排序
		if (page != null) {
			boolean primaryOrdered = false;
			//优先取order
			if (page.getOrder() != null) {
				primaryOrdered = true;
				queryString.append(" ORDER BY ")
						.append(page.getOrder().getOrderBy()).append(" ")
						.append(page.getOrder().getDirection());
			}
			if (!page.getOrders().isEmpty()) { //2,3候选排序
				if (!primaryOrdered) {//没有提供page.order
					queryString.append(" ORDER BY ");
				} else {
					queryString.append(", ");
				}
				for (OrderBean orderBean : page.getOrders()) {
					queryString.append(orderBean.getOrderBy()).append(" ").append(orderBean.getDirection())
							.append(", ");
				}
			}
			if (queryString.lastIndexOf(", ") == queryString.length() - 2) {
				queryString.delete(queryString.length() - 2, queryString.length());
			}

			//如果没有提供排序, 则默认采用create_time desc
			if (queryString.indexOf("ORDER BY") == -1) {
				page.setOrder(DFAULT_ORDER);
				queryString.append(" ORDER BY ")
						.append(page.getOrder().getOrderBy())
						.append(" ")
						.append(page.getOrder().getDirection());
			}

		}

		//建立context， 并放入数据
		VelocityContext context = new VelocityContext();
		context.put("StringUtils", StringUtils.class);
		if (isNotEmpty(params)) {
			for (String paramName : params.keySet()) {
				context.put(paramName, params.get(paramName));
			}
		}
		for (String contextName : classMap.keySet()) { //TODO classMap有任何卵用吗?
			context.put(contextName, classMap.get(contextName));
		}
		//解析后数据的输出目标，java.io.Writer的子类
		StringWriter sql = new StringWriter();
		//进行解析
		Velocity.evaluate(context, sql, sqlOrQueryName, queryString.toString());

		String parsedSQL = sql.toString();
		query = em()
				.createNativeQuery(parsedSQL)
				.unwrap(org.hibernate.query.Query.class);

		Class clazz = null;
		if (this.resultClass != null) {
			clazz = this.resultClass;
		}
		if (clazz != null) {
			query.setResultTransformer(ResultTransformerFactory.getResultTransformer(HashUtils.sha256(parsedSQL),
					clazz,
					hibernateQueryMode,
					enumLookupProperties));
		}
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

		if (page != null && !page.isPagingIgnore()) {
			query.setMaxResults(page.getMaxResults());
			query.setFirstResult(page.getFirstResult());
		}

		List<T> resultList;
		try {
			resultList = query.getResultList();
		} catch (Throwable e) {
			String msg = format("\nFailed to get resultlist from query\n{0}\n Parameters\n{1}!",
					sql,
					JsonUtils.toJson(params));
			log.error(msg, e);
			throw new SQLQueryException(msg, e);
		}

		//接下来是分页查询中的查询总记录数
		if (page != null && page.isAutoCount()) {
			context.put(IS_COUNT_QUERY, true);
			//查总记录数不需要排序, 提升性能
			int orderByIndex = parsedSQL.toLowerCase().lastIndexOf("order by");
			if (orderByIndex != -1) {
				parsedSQL = parsedSQL.substring(0, orderByIndex);
			}
			// 查询总记录数
			String countSql = SqlUtils.generateCountSql(parsedSQL);
			log.info("Count SQL: {}", countSql);
			org.hibernate.query.Query<T> countQuery = em().createNativeQuery(countSql)
					.unwrap(org.hibernate.query.Query.class);
			if (isNotEmpty(params)) {
				countQuery.setProperties(params);
			}
			try {
				Integer totalRecords = PrimitiveUtils.toInt(countQuery.getSingleResult());
				page.setTotalCount(totalRecords);
			} catch (Throwable e) {
				String msg = format("Failed to get result count from query[{0}] with parameters[{1}]!", countSql,
						JsonUtils.toJson(params));
				throw new SQLCountQueryException(msg, e);
			}
		}

		return resultList;
	}

	@Override
	public <T> T findOne() {
		List<Object> results = findList();
		if (!results.isEmpty()) {
			return (T) results.get(0);
		}
		return null;
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


	private boolean isNotEmpty(Map map) {
		return map != null && !map.isEmpty();
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

	private void addOrder(StringBuilder queryString) {
		if (!this.orders.isEmpty()) { //2,3候选排序
			queryString.append(" ORDER BY ");

			for (OrderBean orderBean : this.orders) {
				queryString.append(orderBean.getOrderBy()).append(" ").append(orderBean.getDirection()).append(", ");
			}
		}
		if (queryString.lastIndexOf(", ") == queryString.length() - 2) {
			queryString.delete(queryString.length() - 2, queryString.length());
		}
	}

	private String addOrder(String sql) {
		StringBuilder queryString = new StringBuilder(sql);
		if (!this.orders.isEmpty()) { //2,3候选排序
			queryString.append(" ORDER BY ");

			for (OrderBean orderBean : this.orders) {
				queryString.append(orderBean.getOrderBy()).append(" ").append(orderBean.getDirection()).append(", ");
			}
		}
		if (queryString.lastIndexOf(", ") == queryString.length() - 2) {
			queryString.delete(queryString.length() - 2, queryString.length());
		}

		return queryString.toString();
	}
}
