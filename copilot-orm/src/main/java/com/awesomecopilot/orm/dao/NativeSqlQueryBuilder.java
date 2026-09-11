package com.awesomecopilot.orm.dao;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.utils.ArrayUtils;
import com.awesomecopilot.common.lang.utils.PrimitiveUtils;
import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.exception.SQLCountQueryException;
import com.awesomecopilot.orm.exception.SQLQueryException;
import com.awesomecopilot.orm.transformer.ResultTransformerFactory;
import com.awesomecopilot.orm.utils.HashUtils;
import com.awesomecopilot.orm.utils.JsonUtils;
import com.awesomecopilot.orm.utils.NamedQueryUtils;
import com.awesomecopilot.orm.utils.OrderByValidator;
import com.awesomecopilot.orm.utils.SQLUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION.ASC;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
	
	
	private String sqlOrQueryName;
	
	/**
	 * 是否自动修复SQL(补/删 WHERE、AND 关键字)。默认 true；
	 * 经 JpaDao.query() 构建时会注入 JpaDao 的 Spring 配置值(copilot.orm.sql.auto-fix),
	 * 保持同一配置单一来源(此前 builder 自行读 application.yml, 与环境变量/nacos 等
	 * 非 YAML 渠道的配置会静默分叉, 且每次 new builder 触发重复读盘)
	 */
	private boolean sqlAutofix = true;
	
	private Map<String, Object> params = new HashMap<>();
	
	private List<OrderBean> orders = new ArrayList<>();
	
	private boolean logicalDelete;
	
	private String logicalDeleteField = "deleted";
	
	private Page page;
	
	private Class resultClass;
	
	protected final EntityManager entityManager;

	private EntityManagerHolder entityManagerHolder;

	private String hibernateQueryMode = "loose";
	
	/**
	 * 如果类的某个属性是enum类型，并且需要根据这个enum类型的某个属性来和数据库列值匹配，那么要指明这个属性的名字
	 */
	private Set<String> enumLookupProperties = new HashSet<>();

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
	
	
	public NativeSqlQueryBuilder(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.entityManagerHolder = new EntityManagerHolder(entityManager, null);
	}

	public NativeSqlQueryBuilder(EntityManager entityManager, EntityManagerFactory entityManagerFactory) {
		this.entityManager = entityManager;
		this.entityManagerHolder = new EntityManagerHolder(entityManager, entityManagerFactory);
	}

	public NativeSqlQueryBuilder(EntityManager entityManager, String logicalDeleteField) {
		this.entityManager = entityManager;
		this.logicalDeleteField = logicalDeleteField;
		this.entityManagerHolder = new EntityManagerHolder(entityManager, null);
	}

	public NativeSqlQueryBuilder(EntityManager entityManager, EntityManagerFactory entityManagerFactory,
	                             String logicalDeleteField) {
		this.entityManager = entityManager;
		this.logicalDeleteField = logicalDeleteField;
		this.entityManagerHolder = new EntityManagerHolder(entityManager, entityManagerFactory);
	}
	
	@Override
	public SqlQueryBuilder addParam(String paramName, Object paramValue) {
		//如果参数paramValue是数组, 那么该参数肯定是用在IN查询中, 那么该参数的value应该被转换成对应的List
		if (isBlank(paramName)) {
			throw new IllegalArgumentException("paramName 不能为空");
		}
		if (ArrayUtils.isArray(paramValue)) {
			params.put(paramName, ArrayUtils.toListIfArray(paramValue));
		} else {
			params.put(paramName, paramValue);
		}
		return this;
	}
	
	@Override
	public SqlQueryBuilder addParams(Map<String, Object> params) {
		this.params.putAll(params);
		return this;
	}
	
	@Override
	public SqlQueryBuilder addLlikeParam(String paramName, String paramValue) {
		if (isNotBlank(paramValue)) {
			paramValue = "%" + paramValue;
			params.put(paramName, paramValue);
		}
		return this;
	}
	
	@Override
	public SqlQueryBuilder addRlikeParam(String paramName, String paramValue) {
		if (isNotBlank(paramValue)) {
			paramValue = paramValue + "%";
			params.put(paramName, paramValue);
		}
		return this;
	}
	
	@Override
	public SqlQueryBuilder addlikeParam(String paramName, String paramValue) {
		if (isNotBlank(paramValue)) {
			paramValue = "%" + paramValue + "%";
			params.put(paramName, paramValue);
		}
		return this;
	}
	
	@Override
	public SqlQueryBuilder page(Page page) {
		this.page = page;
		if (page.getOrder() != null) {
			OrderByValidator.validateField(page.getOrder().getOrderBy());
			OrderByValidator.validateDirection(page.getOrder().getDirection());
			orders.add(page.getOrder());
		}
		if (!page.getOrders().isEmpty()) {
			for (OrderBean orderBean : page.getOrders()) {
				OrderByValidator.validateField(orderBean.getOrderBy());
				OrderByValidator.validateDirection(orderBean.getDirection());
			}
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
		OrderByValidator.validateField(order.getOrderBy());
		OrderByValidator.validateDirection(order.getDirection());
		orders.add(order);
		return this;
	}

	@Override
	public SqlQueryBuilder order(String orderBy, OrderBean.DIRECTION direction) {
		OrderBean order = new OrderBean(OrderByValidator.validateField(orderBy), direction);
		orders.add(order);
		return this;
	}

	@Override
	public SqlQueryBuilder order(String order) {
		if (StringUtils.isNotEmpty(order)) {
			String[] orderList = order.split(",");
			Stream.of(orderList).forEach(o -> {
				String[] arr = o.split(":");
				OrderBean.DIRECTION direction = arr.length == 1 ? ASC : OrderBean.DIRECTION.of(arr[1]);
				OrderBean orderBean = new OrderBean();
				orderBean.setOrderBy(OrderByValidator.validateField(arr[0]));
				orderBean.setDirection(direction);
				orders.add(orderBean);
			});
		}
		return this;
	}
	
	@Override
	public <T> SqlQueryBuilder resultClass(Class<T> resultClass) {
		this.resultClass = resultClass;
		return this;
	}
	
	@Override
	public <T> List<T> findList() {
		try {
			return doFindList();
		} finally {
			entityManagerHolder.closeIfNeeded();
		}
	}

	private <T> List<T> doFindList() {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(sqlOrQueryName);
		if (matcher.find()) {
			rawQuery = sqlOrQueryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			rawQuery = NamedQueryUtils.resolveNamedQueryString(em(), sqlOrQueryName);
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
		
		String preParsedSQL = sql.toString();
		String parsedSQL = preParsedSQL;
		boolean autoFix = sqlAutofix;
		if (autoFix) {
			if (log.isDebugEnabled()) {
				log.debug("未裁剪前解析得到的原生SQL: \n {}", preParsedSQL);
			}
			//用来添加/删除 WHERE 或者 AND 关键字
			parsedSQL = SQLUtils.build(preParsedSQL);
			if (log.isDebugEnabled()) {
				log.debug("裁剪后解析得到的原生SQL: \n {}", parsedSQL);
			}
		}
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
			 * 绑参前先复制一份, 只清副本——旧实现直接删调用方 params 里的 null 键,
			 * 同一个 map 查第二次时参数已经残缺。null 键必须剔除(setProperties 遇到
			 * null 值会抛 NullPointerException); 空 List/空数组原样绑定, Hibernate
			 * 生成的 IN 条件恒假、返回 0 行, 不再改成字符串 "''"(绑数字列会报错)。
			 */
			Map<String, Object> bindParams = new HashMap<>(params);
			bindParams.entrySet().removeIf(entry -> entry.getValue() == null);
			query.setProperties(bindParams);
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
		try {
			return doFindPage();
		} finally {
			entityManagerHolder.closeIfNeeded();
		}
	}

	/**
	 * 走分页查询时删掉 SQL 最外层自带的 LIMIT/OFFSET 子句, 返回处理后的 SQL。
	 * <p>
	 * 为什么要删: findPage 会用 page 对象控制"取第几页、每页几条"(Hibernate 在语句
	 * 尾部加 fetch first ? rows only)。SQL 里再写 limit 就是两套分页机制打架,
	 * MySQL/H2 直接报语法错误。删掉后: 谁调用谁负责分页, 语义也更好解释——
	 * "我这条 SQL 就是全部数据, 每页取几条由 page 参数定"。
	 * 只删最外层(括号深度0)且引号外的 limit; 子查询里的 limit 是业务语义, 保留。
	 * <p>
	 * 若被删掉的 limit 里带 :参数 或 ? 占位(如 limit :n), 其参数无法再绑定,
	 * 继续执行会报"参数不存在", 不如在这里抛一个把原因说清楚的异常。
	 */
	private String stripTopLevelLimitForPaging(String sql) {
		String upper = sql.toUpperCase(Locale.ROOT);
		int idx = indexOfTopLevel(upper, sql, "LIMIT", 0);
		int offsetIdx = indexOfTopLevel(upper, sql, "OFFSET", 0);
		if (offsetIdx >= 0 && (idx < 0 || offsetIdx < idx)) {
			idx = offsetIdx;
		}
		if (idx < 0) {
			return sql;
		}
		// 从 limit 之后逐字符吃掉"limit 表达式"本体: 数字/冒号参数/问号/? 占位/逗号/
		// offset 关键字; 遇到其它任何字符(下一个子句)即停
		int end = idx + (upper.startsWith("LIMIT", idx) ? 5 : 6);
		StringBuilder removed = new StringBuilder();
		while (end < sql.length()) {
			char c = sql.charAt(end);
			removed.append(c);
			if (Character.isLetterOrDigit(c) || c == ':' || c == '?' || c == ',' || c == '.'
					|| Character.isWhitespace(c)) {
				end++;
				continue;
			}
			break;
		}
		// 处理 "LIMIT 10 OFFSET 20": 表达式扫描顺带吞了 OFFSET 字样则无需再处理
		String stripped = (sql.substring(0, idx) + sql.substring(end)).trim();
		if (removed.indexOf(":") >= 0 || removed.indexOf("?") >= 0) {
			throw new IllegalStateException("分页查询(findPage)会接管 limit/offset, 但 SQL 里自带的 "
					+ "limit 子句含参数占位: [" + removed + " ...], 删除后参数无处可绑。" +
					"请去掉 SQL 里的 limit, 分页统一交给 page(页码, 每页条数) 控制");
		}
		return stripped;
	}

	private <T> List<T> doFindPage() {
		String rawQuery = null;
		org.hibernate.query.Query<T> query = null;
		Matcher matcher = SELECT_PATTERN.matcher(sqlOrQueryName);
		if (matcher.find()) {
			rawQuery = sqlOrQueryName; // 这就是一个完整的查询语句,而不是定义在xml中的查询语句名
		} else {//表示queryName是定义在xml中的查询语句名
			rawQuery = NamedQueryUtils.resolveNamedQueryString(em(), sqlOrQueryName);
		}
		// 排序
		StringBuilder queryString = new StringBuilder(rawQuery);
		if (page != null) {
			/*
			 * 注意: 这段拼排序的代码跑在 Velocity 渲染之前, 命名SQL模板里可能有
			 * #if($isCountQuery) 之类的指令, 整条语句级解析不可靠, 所以这里用
			 * 保守的词扫描(appendOrderBy), 不用 JSqlParser
			 */
			List<OrderBean> wantedOrders = new ArrayList<>();
			if (page.getOrder() != null) {
				wantedOrders.add(page.getOrder());
			}
			wantedOrders.addAll(page.getOrders());
			//如果调用方和 page 都没给排序, 按老约定默认 create_time desc
			if (wantedOrders.isEmpty()) {
				if (!this.orders.isEmpty()) {
					wantedOrders.addAll(this.orders);
				} else {
					page.setOrder(DFAULT_ORDER);
					wantedOrders.add(DFAULT_ORDER);
				}
			}
			appendOrderBy(queryString, wantedOrders);
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
		String preParsedSQL = sql.toString();
		String parsedSQL = preParsedSQL;
		boolean autoFix = sqlAutofix;
		if (autoFix) {
			if (log.isDebugEnabled()) {
				log.debug("未裁剪前解析得到的原生SQL: \n {}", preParsedSQL);
			}
			//用来添加/删除 WHERE 或者 AND 关键字
			parsedSQL = SQLUtils.build(preParsedSQL);
			if (log.isDebugEnabled()) {
				log.debug("裁剪后解析得到的原生SQL: \n {}", parsedSQL);
			}
		}
		/*
		 * 分页查询会自己控制取几条(Hibernate 在尾部加 fetch first), SQL 里再写 limit 就是
		 * 两套分页打架、数据库报语法错误 —— 这里把最外层的 limit/offset 删掉, 只保留排序。
		 */
		if (page != null && !page.isPagingIgnore()) {
			parsedSQL = stripTopLevelLimitForPaging(parsedSQL);
		}
		
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
			 * 绑参前先复制一份, 只清副本——旧实现直接删调用方 params 里的 null 键,
			 * 同一个 map 查第二次时参数已经残缺。null 键必须剔除(setProperties 遇到
			 * null 值会抛 NullPointerException); 空 List/空数组原样绑定, Hibernate
			 * 生成的 IN 条件恒假、返回 0 行, 不再改成字符串 "''"(绑数字列会报错)。
			 */
			Map<String, Object> bindParams = new HashMap<>(params);
			bindParams.entrySet().removeIf(entry -> entry.getValue() == null);
			query.setProperties(bindParams);
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
			/*
			 * 旧实现先用 lastIndexOf("order by") 截断再 count, 会把子查询里带 ORDER BY 的
			 * 外层 WHERE 后半截丢掉(如 WHERE id IN (SELECT ... ORDER BY x LIMIT 1)),
			 * count 结果静默错误。现在 generateCountSql 内部统一走派生表包裹并剥离最外层
			 * ORDER BY/LIMIT/OFFSET, 这里直接传完整 SQL 即可, 不再做字符串级截断。
			 */
			String countSql = SQLUtils.generateCountSql(parsedSQL);
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
		/*
		 * 这边放到ThreadContext里面是为了保证PageResultAspect能从ThreadContext拿到将Page对象并回填到最终返回的Result对象里面
		 * 不为null才填充是因为执行多次查询时, 前一个是分页查询, 后一个不是, 那么后一个查询会把ThreadContext中的page对象给清掉
		 */
		if (page != null) {
			ThreadContext.put("page", page);
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
	
	public void setSqlOrQueryName(String sqlOrQueryName) {
		this.sqlOrQueryName = sqlOrQueryName;
	}
	
	public void setSqlAutofix(boolean sqlAutofix) {
		this.sqlAutofix = sqlAutofix;
	}
	
	public void setHibernateQueryMode(String hibernateQueryMode) {
		this.hibernateQueryMode = hibernateQueryMode;
	}
	
	public void setEnumLookupProperties(Set<String> enumLookupProperties) {
		this.enumLookupProperties = enumLookupProperties;
	}
	
	private EntityManager em() {
		return entityManagerHolder.get();
	}
	
	
	private boolean isNotEmpty(Map map) {
		return map != null && !map.isEmpty();
	}
	
	

	/**
	 * 把排序字段安全地拼进 SQL, 解决旧实现的两类语法错误:
	 * <ol>
	 * <li>旧代码用 toUpperCase().contains("ORDER BY") 判断"SQL是否已有排序"——
	 * 子查询里的 order by 会引起误判; 新实现做词级扫描, 只在括号深度为 0
	 * (即最外层语句)找 order by, 且跳过单引号字符串里的内容;</li>
	 * <li>旧实现把 ORDER BY 直接拼到语句最末尾——SQL 自带 LIMIT/OFFSET/
	 * FOR UPDATE 时产出 "... LIMIT 10 ORDER BY x" 这种 MySQL 语法错误;
	 * 新实现找到尾部这些子句的起点, 把排序插在它们前面。</li>
	 * </ol>
	 * 本方法跑在 Velocity 渲染之前(模板可能未闭合), 所以只做词扫描不做语句级解析。
	 */
	private void appendOrderBy(StringBuilder queryString, List<OrderBean> orders) {
		if (orders == null || orders.isEmpty()) {
			return;
		}
		String sql = queryString.toString();
		String upper = sql.toUpperCase(Locale.ROOT);
		int topOrderByIdx = findTopLevelKeyword(upper, sql, "ORDER BY");
		StringBuilder clause = new StringBuilder();
		for (OrderBean orderBean : orders) {
			if (clause.length() > 0) {
				clause.append(", ");
			}
			//validateField/validateDirection 在这里顺带挡掉非法排序字段(防注入, 报告 I-2 的既有防线)
			OrderByValidator.appendOrderClause(clause, orderBean);
		}
		if (topOrderByIdx >= 0) {
			//已有最外层 ORDER BY: 把新字段用逗号追加进既有排序列表,
			//插入点 = 既有排序段的末尾(下一个顶层 LIMIT/OFFSET/锁子句之前)。
			//只有插入点后面紧跟着非空白字符时才补空格, 防止 "DESClimit" 粘连又不留多余尾空格
			int insertAt = findTailClauseStart(upper, sql, topOrderByIdx + "ORDER BY".length());
			queryString.insert(insertAt, padIfNeeded(", " + clause, sql, insertAt));
			return;
		}
		//没有最外层 ORDER BY: 在尾部 LIMIT/OFFSET/FOR UPDATE 等子句之前插入整段
		int insertAt = findTailClauseStart(upper, sql, 0);
		if (insertAt == 0) {
			queryString.append(" ORDER BY ").append(clause);
		} else {
			queryString.insert(insertAt, padIfNeeded(" ORDER BY " + clause, sql, insertAt));
		}
	}

	/**
	 * 拼接文本要插到 sql 的 pos 处时, 若 pos 处不是空白/串尾, 补一个空格再插,
	 * 避免和后面的关键字粘成一个词
	 */
	private String padIfNeeded(String toInsert, String sql, int pos) {
		if (pos < sql.length() && !Character.isWhitespace(sql.charAt(pos))) {
			return toInsert + " ";
		}
		return toInsert;
	}

	/**
	 * 顶层子句关键字: 出现在 ORDER BY 之后、不属于排序表达式本身, 遇到它们必须停
	 */
	private static final String[] TAIL_CLAUSES = {
			"LIMIT", "OFFSET", "FOR UPDATE", "LOCK IN SHARE MODE", "FETCH FIRST", "FETCH NEXT", "UNION"};

	/**
	 * 在括号深度为 0、引号外的范围里找 keyword(大小写不敏感、词边界), 找不到返回 -1。
	 * from 为搜索起点。单引号字符串里的关键字(如 column 'ORDER BY X')会被跳过。
	 */
	private int findTopLevelKeyword(String upperSql, String rawSql, String keyword) {
		int depth = 0;
		boolean inQuote = false;
		int n = upperSql.length();
		int klen = keyword.length();
		for (int i = 0; i < n; i++) {
			char c = rawSql.charAt(i);
			if (c == '\'') {
				//'' 是转义引号, 不算字符串结束
				if (inQuote && i + 1 < n && rawSql.charAt(i + 1) == '\'') {
					i++;
					continue;
				}
				inQuote = !inQuote;
				continue;
			}
			if (inQuote) {
				continue;
			}
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				if (depth > 0) depth--;
			} else if (depth == 0
					&& upperSql.startsWith(keyword, i)
					&& isWordBoundaryBefore(rawSql, i) && isWordBoundaryAfter(upperSql, i + klen)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 从 from 起找第一个顶层尾部子句(LIMIT/OFFSET/FOR UPDATE/UNION...)的起点,
	 * 没有则返回语句末尾——排序表达式/ORDER BY 子句必须插在这些子句之前。
	 */
	private int findTailClauseStart(String upperSql, String rawSql, int from) {
		int best = rawSql.length();
		for (String tail : TAIL_CLAUSES) {
			int idx = indexOfTopLevel(upperSql, rawSql, tail, from);
			if (idx >= 0 && idx < best) {
				best = idx;
			}
		}
		return best;
	}

	/**
	 * 在括号深度 0、引号外, 从 start 开始找 keyword 第一次出现的位置, 找不到返回 -1
	 */
	private int indexOfTopLevel(String upperSql, String rawSql, String keyword, int start) {
		int depth = 0;
		boolean inQuote = false;
		int n = upperSql.length();
		for (int i = start; i < n; i++) {
			char c = rawSql.charAt(i);
			if (c == '\'') {
				if (inQuote && i + 1 < n && rawSql.charAt(i + 1) == '\'') {
					i++;
					continue;
				}
				inQuote = !inQuote;
				continue;
			}
			if (inQuote) {
				continue;
			}
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				if (depth > 0) depth--;
			} else if (depth == 0
					&& upperSql.startsWith(keyword, i)
					&& isWordBoundaryBefore(rawSql, i) && isWordBoundaryAfter(upperSql, i + keyword.length())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * keyword 出现在 idx 处时, 检查它前面是不是词边界(idx==0 或者前一个字符
	 * 不是字母/数字/下划线, 防止 ORDERBY 这类粘连词被误判为含关键字)
	 */
	private boolean isWordBoundaryBefore(String rawSql, int idx) {
		if (idx == 0) {
			return true;
		}
		char prev = rawSql.charAt(idx - 1);
		return !Character.isLetterOrDigit(prev) && prev != '_';
	}

	/**
	 * keyword 结束位置 end 之后同样做词边界检查(防止 LIMITE 之类误判)
	 */
	private boolean isWordBoundaryAfter(String upperSql, int end) {
		if (end >= upperSql.length()) {
			return true;
		}
		char next = upperSql.charAt(end);
		return !Character.isLetterOrDigit(next) && next != '_';
	}

	/**
	 * 老入口: doFindList 走这里。两个 addOrder 版本都改成调用 appendOrderBy,
	 * 排序拼接只留一处实现, 不再各写各的
	 * (修复: SQL 自带 LIMIT 或子查询里有 order by 时, 拼出来的排序位置不对、MySQL 报错)
	 */
	private void addOrder(StringBuilder queryString) {
		appendOrderBy(queryString, this.orders);
	}

	private String addOrder(String sql) {
		StringBuilder queryString = new StringBuilder(sql);
		appendOrderBy(queryString, this.orders);
		return queryString.toString();
	}
}