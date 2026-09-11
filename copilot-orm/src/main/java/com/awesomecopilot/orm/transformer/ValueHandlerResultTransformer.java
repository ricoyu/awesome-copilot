package com.awesomecopilot.orm.transformer;

import com.awesomecopilot.common.lang.transformer.ValueHandlerFactory;
import com.awesomecopilot.common.lang.utils.EnumUtils;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.orm.exception.AliasLengthNotMatchException;
import com.awesomecopilot.orm.exception.ApplicationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.transform.ResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * 将数据库的resultset注入进bean中，包括数据库字段名与bean属性名之间的转换与数据类型的转换工作
 * 
 * @author Rico
 * @since May 30, 2016
 * @version 3.0
 *
 */
public class ValueHandlerResultTransformer implements ResultTransformer {

	private static final long serialVersionUID = -487079650392740751L;

	private static final Logger logger = LoggerFactory.getLogger(ValueHandlerResultTransformer.class);

	public static final String STRICT = "strict";

	public static final String LOOSE = "loose";
	
	//所有列名连接之后的值，用来判断数据库返回的字段数量、顺序有没有改变
	//(保留用于日志/调试; 热路径判断已改用 boundAliases 数组逐位比较)
	private String aliasConcated = null;

	/*
	 * 上次成功绑定时的列名快照。每行数据进来时只需逐位 equals,
	 * 代替旧实现"每行把整个列名数组拼成一个新字符串再比较"的固定开销
	 */
	private String[] boundAliases = null;

	@SuppressWarnings("rawtypes")
	private final Class resultClass;

	/**
	 * resultClass 的无参构造器, 初始化时解析一次并缓存。
	 * 替代废弃的 Class.newInstance(): 旧 API 在无参构造器缺失/不可见时把异常原样甩给调用方,
	 * 且会吞掉构造器内部抛出的异常(只剩一个 InvocationTargetException 壳都不给),
	 * 现在构造失败会明确报出是哪一层的问题
	 */
	@SuppressWarnings("rawtypes")
	private Constructor resultConstructor;

	private MethodHandles.Lookup lookup = MethodHandles.lookup();

	private String queryMode;

	private Set<String> enumLookupProperties = new HashSet<>();

	/**
	 * 当第一行数据过来以后，会去匹配alias和bean中的setter 
	 * 这个用于标识这个步骤是否完成 
	 * 这一步完成后下面这些temp开头的数组就都初始化完毕了
	 * @on
	 */
	public boolean alias2MethodInitialized = false;

	/**
	 * 标识每一列的类型转换的检查都否都完成了
	 * 比如：
	 * 第一行数据过来，第三列值为null，那么就无法判断这一列是否需要做类型转换，所以castChecks[2]为null
	 * 因此，只有当castChecks中每个元素都是true，才表示类型转换检查完成了
	 * <p>
	 * volatile: transformTuple 的无锁快路径靠它做"发布开关"——它在锁内最后被置 true,
	 * 快路径线程读到 true 就能保证看到之前所有数组写入(Java 内存模型的 happens-before)
	 * @on
	 */
	private volatile boolean castChecksComplete = false;

	//只是为了在初次完成castChecks时打印log用
	private byte castChecksCompletePoint = 0;

	/**
	 * 如果第一行数据过来，某个column是null，那么就不知道这列到底是否需要做类型转换
	 * 所以记录下是否每一列都完成了类型转换检查
	 * 注：特意用Boolean包装类
	 * @on
	 */
	private Boolean[] castChecks = null;

	/**
	 * 上面的castChecks只是标识有没有对每一列数据做过类型转换检查
	 * 而needCasts则记录了该列实际上是否需要类型转换
	 * 注：特意用Boolean包装类
	 * @on
	 */
	private Boolean[] needCasts = null;

	/**
	 * 下面这些temp开头的数组缓存了bean中所有setter相关信息，他们在这个Transformer实例化的时候被初始化
	 * 注：通过initializeTmp()初始化 此时还不知道resultset里面具体包含哪些field，可能bean里面的某些属性resultset不包含。
	 * @on
	 */
	private Method[] tempSetterMethods = null;

	private MethodHandle[] tempMethodHandles = null;

	// 包含对应setter的参数类型
	private Class<?>[] tempParameterTypes = null;

	// 包含setter的名字，转成小写,并把前缀"set"去掉
	private String[] tempPropertyNames = null;

	//暂时存储一下resultClass中的字段, 不保证顺序
	private Field[] tempFields = null;

	//存储resultClass中的字段, 与alias保持顺序一致
	private Field[] fields = null;

	// 当自动类型转换失败后尝试找JPA2提供的基于注解的Converter,这个数组大部分的元素应该是空的
	@SuppressWarnings("rawtypes")
	private AttributeConverter[] tempAttributeConverters = null;

	/**
	 * 下面这些缓存数据是在处理第一个resultset的时候生成的，因此temp*缓存中的一些多余的缓存项都去掉了
	 * 根据返回的resultset包含的alias选取对应Method
	 * @on
	 */
	private Method[] methods = null;

	//包含setter的名字，转成小写,并把前缀"set"去掉
	private String[] propertyNames = null;

	//其实就是alias，但是顺序跟methodNames一一对应
	private String[] columnNames = null;

	private Class<?>[] parameterTypes = null;

	private MethodHandle[] methodHandles = null;

	// 当自动类型转换失败后尝试找JPA2 @Convert
	@SuppressWarnings("rawtypes")
	private AttributeConverter[] attributeConverters = null;
	
	/**
	 * 检查alias数组长度和methodHandles， parameterTypes是否一致，如果不一致则打印log信息并抛异常
	 */
	private boolean tupleAliasLengthCheck = false;

	@SuppressWarnings("rawtypes")
	public ValueHandlerResultTransformer(Class resultClass) {
		// 委托双参构造器: 旧实现不跑 initializeTmp() 且 queryMode=null, 属于"用了必炸"的陷阱
		this(resultClass, LOOSE);
	}

	/**
	 * 将这个ValueHandlerResultTransformer与Class绑定，并进行第一波初始化.
	 * 
	 * @param resultClass
	 * @param queryMode
	 */
	@SuppressWarnings("rawtypes")
	public ValueHandlerResultTransformer(Class resultClass, String queryMode) {
		if (isEmpty(queryMode)) {
			queryMode = LOOSE;
		}
		if (!StringUtils.equalsIgAny(queryMode, STRICT, LOOSE)) {
			throw new InvalidParameterException("hibernateQueryMode can only be empty or strict, loose");
		}
		// 统一转小写存起来: 校验用的是 equalsIgAny(大小写不敏感), 但 transformTuple 里的
		// queryMode.equals(LOOSE) 是大小写敏感的——旧实现允许传 "Loose" 进来, 后面
		// loose 判断全部失效, 缺列时不再跳过、转而拿 null methodHandle invoke 出 NPE。
		// 在构造入口归一化, 下游怎么比较都不会漏
		this.queryMode = queryMode.toLowerCase(Locale.ROOT);
		this.resultClass = resultClass;
		// 无参构造器解析一次缓存起来, 每行数据不再走废弃的 Class.newInstance()
		try {
			this.resultConstructor = resultClass.getDeclaredConstructor();
			if (!this.resultConstructor.canAccess(null)) {
				this.resultConstructor.setAccessible(true);
			}
		} catch (NoSuchMethodException e) {
			throw new HibernateException("resultClass [" + resultClass.getName()
					+ "] 缺少可访问的无参构造器, 无法作为查询结果映射目标", e);
		}
		initializeTmp();
	}

	/**
	 * 这个初始化方法在ValueHandlerResultTransformer实例化的时候调用一次
	 * 
	 * 将resultClass的所有setter/getter缓存起来
	 * 同时找到getter上有没有JPA2.0的@Convert注解
	 * 
	 * 包括:
	 * setter Method对象本身		tempMethods
	 * 去掉set前缀后的名字			tempMethodNames
	 * setter参数类型			tempParameterTypes
	 * 对应MethodHandle			tempMethodHandles
	 * 对应AttributeConverter	tempAttributeConverters
	 * 
	 * 该方法在Transformer实例化的时候被调用一次，所以对性能影响较小
	 * 
	 * @on
	 * @throws Throwable
	 */
	@SuppressWarnings("rawtypes")
	protected void initializeTmp() {
		Method[] methods = resultClass.getMethods();
		Field[] fields = ReflectionUtils.getFields(resultClass);
		tempFields = fields;
		/*
		 * 过滤掉Object对象中声明的方法
		 */
		methods = Arrays.stream(methods).filter(m -> m.getDeclaringClass() != Object.class).toArray(Method[]::new);

		List<Method> setters = new ArrayList<>();
		List<Method> getters = new ArrayList<Method>();

		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (method.getName().startsWith("set")) {// 只找setter
				if (method.getParameterTypes().length == 0) {// setter不带参数的话就忽略这个setter
					logger.debug("Found setter method {} with no parameters, so ignore it.", method.getName());
					continue;
				}
				setters.add(method);
			} else if (method.getName().startsWith("get") || method.getName().startsWith("is")) {// 缓存getter是为了找AttrubuteConverter
				getters.add(method);
			}
		}

		int length = setters.size();
		tempSetterMethods = new Method[length];
		setters.toArray(tempSetterMethods);
		tempPropertyNames = new String[length];
		tempMethodHandles = new MethodHandle[length];
		tempParameterTypes = new Class<?>[length];
		tempAttributeConverters = new AttributeConverter[length];

		try {
			for (int i = 0; i < tempSetterMethods.length; i++) {
				Method setter = tempSetterMethods[i];
				Class<?> parameterType = setter.getParameterTypes()[0]; //参数类型;
				tempParameterTypes[i] = parameterType;
				String[] propertyNamePair = getPropertyName(setter, parameterType, resultClass);
				tempPropertyNames[i] = propertyNamePair[1];
				tempMethodHandles[i] = lookup.unreflect(setter);

				String propertyName = propertyNamePair[0];
				//先找field上有没有@Convert注解，没有再找getter
				for (int j = 0, len = fields.length; j < len; j++) {
					Field field = fields[j];
					if (field.getName().equalsIgnoreCase(propertyName)) {
						Convert convert = field.getAnnotation(Convert.class);
						if (convert != null) {
							AttributeConverter converter = (AttributeConverter) convert.converter().getDeclaredConstructor().newInstance();
							tempAttributeConverters[i] = converter;
							break;
						}
					}
				}

				//如果在field上找到了Convert注解，则不在getter上找了
				if (tempAttributeConverters[i] != null) {
					continue;
				}
				/*
				 * 开始找这个setter对应的getter，然后获取converter
				 * 如果是boolean型的，不管属姓名是带is还是不带is，getter都是不带is的，所以取propertyNamePair[0]
				 * 进行匹配
				 */
				boolean found = false;
				for (Method getter : getters) {
					//boolean型是is开头的
					if (parameterType.equals(Boolean.class) || parameterType.equals(Boolean.TYPE)) {
						found = getter.getName().toLowerCase().indexOf(propertyName) == 2;
					} else {
						found = getter.getName().toLowerCase().indexOf(propertyName) == 3;
					}
					if (found) {
						Convert convert = getter.getAnnotation(Convert.class);
						if (convert != null) {
							AttributeConverter converter = (AttributeConverter) convert.converter().getDeclaredConstructor().newInstance();
							tempAttributeConverters[i] = converter;
						}
						// 找到则跳出该层循环
						break;
					}
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		logger.debug("{} resultClass[{}]initializeTmp()初始化完毕", getClass().getName(), resultClass.getName());
	}

	/**
	 * 每行数据的初始化检查入口。
	 * <p>
	 * 每行数据都需要调用这个方法的原因：
	 * tuple 的某个元素是 null 时无法判断该列是否需要类型转换，要等后面非 null 的行来确定。
	 * <p>
	 * 旧实现整个方法 synchronized, 且每行都要拿锁进来走一遍判断——这个 transformer
	 * 实例是按 SQL 全局共享的(工厂缓存), 大结果集 × 多线程查询同一个 SQL 时, 所有线程
	 * 在一把锁上排队, 纯属无谓竞争。
	 * <p>
	 * 现在拆成两段:
	 *  - 快路径(无锁): 完全初始化 + 列名快照一致时直接返回, 只读 volatile 标志和数组,
	 *    多线程完全并行;
	 *  - 慢路径(synchronized): 首轮初始化/列名变化重建/某列值一直为 null 需要续查,
	 *    才进锁。这些本来就是低频事件。
	 * 正确性依据: castChecksComplete 是 volatile 且在锁内最后置 true, 快路径读到 true
	 * 即可安全看到之前的全部数组写入; 快路径校验过列名一致, 期间即使有线程触发重建,
	 * 重建对"当前行能用的绑定"是幂等的(相同列名会重建出相同绑定), 不会读到半成品。
	 */
	private void initialize(String[] aliases, Object[] tuple) {
		if (fastPathReady(aliases)) {
			return;
		}
		synchronizedInitialize(aliases, tuple);
	}

	/**
	 * 无锁快路径判定: 两套初始化都已完成、列名与上次成功绑定完全一致 → 无需任何动作。
	 * 同一实例只被"相同 SQL + 相同 Bean"共享(工厂按 key 缓存), 并发行的列名天然一致,
	 * 快路径的列名比较只是防御性检查。
	 * 已知极限情况: 数据库列结构在运行中变更、且旧列查询恰好撞上另一线程触发的重建时,
	 * 可能有个别行读到构建中的数组抛异常——列结构变更窗口内的瞬时现象, 重建完成后自愈;
	 * 旧加锁版本同样要面对列变更时首轮绑定失败重试, 只是排队方式不同。
	 */
	private boolean fastPathReady(String[] aliases) {
		if (!castChecksComplete) {
			return false;
		}
		String[] bound = boundAliases;
		if (bound == null || bound.length != aliases.length) {
			return false;
		}
		for (int i = 0; i < bound.length; i++) {
			if (!StringUtils.equalsIgCase(bound[i], aliases[i])) {
				return false;
			}
		}
		return true;
	}

	private synchronized void synchronizedInitialize(String[] aliases, Object[] tuple) {
		/*
		 * 系统在运行期间, 如果数据库表新增/删除了字段, 查询返回的列(aliases)就和之前
		 * 匹配好的 setter 数组对不上了。旧代码遇到这种情况会把 alias2MethodInitialized
		 * 置回 false 等下一行数据来了重建——但重建时 castChecksComplete 忘了重置,
		 * 第二遍初始化直接跳过, 类型标记全是空, 之后每一行数据都会抛
		 * "没有办法将值...转换为..." 的异常, 而且坏的是全局共享的缓存实例,
		 * 结果就是: 加个字段, 这个查询到重启之前永远报错。
		 * 现在改为: 发现列对不上, 当场把两套状态全部清零重建, 不再"等下一行"。
		 * @on
		 */
		if (alias2MethodInitialized) {
			if (methods.length != aliases.length) {
				//列数量都变了, 上次的判断结果没有任何保留价值, 整套状态清零重建
				resetAliasBinding();
			} else { //列数相同但顺序/名称变了(比如调整了字段顺序), 会造成类型判断错位
				if (!aliasesUnchanged(aliases)) {
					resetAliasBinding();
				}
			}
		}
		/*
		 * 第一部分初始化
		 */
		if (!alias2MethodInitialized) {
			logger.debug("{}还未初始化，开始匹配alias与bean中的setter", getClass().getSimpleName());
			int length = aliases.length;
			fields = new Field[length];
			methods = new Method[length]; //Resultset中每一列对应的Method
			methodHandles = new MethodHandle[length];
			propertyNames = new String[length]; //对应的方法名
			columnNames = new String[length]; //暂存一下alias，方便下面比较
			parameterTypes = new Class<?>[length]; //方法参数
			attributeConverters = new AttributeConverter[length]; //该列需要用AttributeConverter来转换
			needCasts = new Boolean[length]; //标记对应的列实际是否需要做类型转换
			castChecks = new Boolean[length];//标记对应的列是否做了类型转换检查

			boolean hasPropertyNotFound = false;
			for (int i = 0; i < aliases.length; i++) {
				//根据alias匹配bean中的方法名
				boolean found = false;
				String alias = aliases[i].replaceAll("_", "").toLowerCase();
				for (int j = 0; j < tempFields.length; j++) {
				    Field field = tempFields[j];
					if (field.getName().equalsIgnoreCase(alias)) {
						fields[i] = field;
					}
				}
				/*
				 * 找对应的方法 
				 * 注意methods、methodHandles、methodNames、parameterTypes、columnNames
				 * 这几个数组在顺序上都是与aliases一一对应的,避免后面再去根据alias查找对应的setter
				 */
				for (int j = 0; j < tempPropertyNames.length; j++) {
					if (tempPropertyNames[j].equals(alias)) {
						methods[i] = tempSetterMethods[j];
						methodHandles[i] = tempMethodHandles[j];
						propertyNames[i] = tempPropertyNames[j];
						parameterTypes[i] = tempParameterTypes[j];
						attributeConverters[i] = tempAttributeConverters[j];
						columnNames[i] = aliases[i];
						found = true;
						break;
					}
				}

				if (!found) {
					hasPropertyNotFound = true;
					String message = format("Cannot find property with name:[{0}] in class:[{1}]", alias,
							resultClass.getName());
					/*
					 * If in strict mode, throw exception when property not found, 
					 * in loose mode, ignore this exception
					 * @on
					 */
					logger.debug(message);
					/*
					 * 因为要支持loose模式下不报错，找不到对应method情况下对应位置也要给一个null，不然顺序就错乱了
					 */
					methods[i] = null;
					methodHandles[i] = null;
					propertyNames[i] = null;
					parameterTypes[i] = null;
					columnNames[i] = null;
				}
			}
			if (hasPropertyNotFound && STRICT.equalsIgnoreCase(queryMode)) {
				throw new PropertyNotFoundException("Bean中缺少属性，您的数据接收不完整!");
			}

			alias2MethodInitialized = true;
			//记下这次绑定对应的列名快照, 下一行数据进来时逐位比较即可(零字符串分配)
			boundAliases = aliases.clone();
			aliasConcated = StringUtils.concat(aliases);
		}

		/********************* 这是华丽的分隔符 *************************/

		/*
		 * 第二部分初始化
		 * 标记：
		 * 		每一列是否需要做类型转换
		 * 		以及是否每一列都完成了类型转换检查
		 * @on
		 */
		if (!castChecksComplete) {
			for (int i = 0; i < aliases.length; i++) {
				if (i >= castChecks.length) {
					break;
				}
				Object value = tuple[i];

				/*
				 * 表示这列的类型转换检查已经完成了，接着检查下一列
				 */
				if (castChecks[i] != null) {
					continue;
				}

				Class<?> parameterType = parameterTypes[i];
				/*
				 * value值不为null才需要做类型转换检查
				 * 并且前面提到在prod模式下，返回的result list里面如果有一列handsome在bean里面没有定义对应的属性，那么也不报错
				 * 所以这边除了检查value不等于null还需要检查parameterType!=null
				 * 因为上述情况下parameterTypes[i]回给一个占位的null值
				 * 
				 * @on
				 */
				if (value != null && parameterType != null) {
					/*
					 * 设为true只是表示这一列完成类型转换的检查，但这列到底需不需要做类型转换由needCasts决定
					 */
					castChecks[i] = true;
					/*
					 * 是否可以直接传给setter 如果bean里面是基本类型，但是SQL返回的tuple会是其包装类型,所以需要额外检查一下
					 */
					if (Boolean.TYPE.equals(parameterType)) {
						needCasts[i] = !Boolean.class.isInstance(value);
					} else if (Long.TYPE.equals(parameterType)) {
						needCasts[i] = !Long.class.isInstance(value);
					} else if (Integer.TYPE.equals(parameterType)) {
						needCasts[i] = !Integer.class.isInstance(value);
					} else if (Float.TYPE.equals(parameterType)) {
						needCasts[i] = !Float.class.isInstance(value);
					} else if (Double.TYPE.equals(parameterType)) {
						needCasts[i] = !Double.class.isInstance(value);
					} else if (Character.TYPE.equals(parameterType)) {
						needCasts[i] = !Character.class.isInstance(value);
					} else if (Short.TYPE.equals(parameterType)) {
						needCasts[i] = !Short.class.isInstance(value);
					} else if (Byte.TYPE.equals(parameterType)) {
						needCasts[i] = !Byte.class.isInstance(value);
					} else {
						needCasts[i] = !parameterType.isInstance(value);
					}
				}

			}

			/*
			 * castChecks[i]为null表示tuple[i]为null，在一次查询返回多条记录的情况下，
			 * 就要求助下一条记录的tuple[i]来确定是否需要做类型转换了。 如果下一条记录的tuple[i]还是null，那么再下一条，以此类推。
			 * 
			 * castChecks会有三种值： null 表示tuple[i]需要读取下一条记录来确定是否要做类型转换 true
			 * 表示tuple[i]需要做类型转换 false表示tuple[i]不需要做类型转换
			 */
			boolean done = true;
			for (int i = 0; i < castChecks.length; i++) {
				Boolean caseCheck = castChecks[i];
				if (caseCheck == null) {
					done = false;
					break;
				}
			}
			castChecksComplete = done;

		}

		if (castChecksComplete) {
			if (castChecksCompletePoint == 0) {
				logger.debug("{} resultClass[{}]initialize()初始化完毕", getClass().getName(), resultClass.getName());
				castChecksCompletePoint = 1;
			}
		} else {
			logger.debug("仍然有某列的值一直为null，所以处理完本条记录后还不能确定这列是否需要做类型转换，下一条记录要继续第二部分的初始化!");
		}
	}

	@Override
	public List transformList(List resultList) {
		return ResultTransformer.super.transformList(resultList);
	}

	/**
	 * 把"列名 -> setter 绑定"以及基于列名的类型判断标记全部作废, 下一条数据进来时
	 * 从头重建。必须整套一起清: 旧代码只清了 alias2MethodInitialized,
	 * castChecksComplete 还是 true, 导致第二轮初始化被整体跳过, 之后每行都报错。
	 */
	private void resetAliasBinding() {
		alias2MethodInitialized = false;
		//第二部分初始化(逐列判断要不要做类型转换)的结果也全部作废
		castChecksComplete = false;
		castChecksCompletePoint = 0;
		//列数变了, 长度一致性检查也要重新做一遍
		tupleAliasLengthCheck = false;
		boundAliases = null;
		aliasConcated = null;
	}

	/**
	 * 本行的列名和上次成功绑定时是否完全一致(区分顺序)。
	 * 逐位 equalsIgnoreCase, 不拼接新字符串——这个判断每行数据都要做一次,
	 * 旧实现每行都 StringUtils.concat 整个列名数组再比较, 大结果集上是白扔的开销
	 */
	private boolean aliasesUnchanged(String[] aliases) {
		String[] bound = boundAliases;
		if (bound == null || bound.length != aliases.length) {
			return false;
		}
		for (int i = 0; i < bound.length; i++) {
			if (!StringUtils.equalsIgCase(bound[i], aliases[i])) {
				return false;
			}
		}
		return true;
	}

	/*
	 * 将resultset注入到bean中
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		initialize(aliases, tuple);
		Object result = null;

		if(!tupleAliasLengthCheck) {
			if(aliases.length != methodHandles.length) {
				logger.error("aliases 和 methodHandles 长度不一致，alias长度为{}, methodHandles长度为{}", aliases.length, methodHandles.length);
				throw new AliasLengthNotMatchException();
			}
			if(aliases.length != parameterTypes.length) {
				logger.error("aliases 和 parameterTypes 长度不一致，alias长度为{}, parameterTypes长度为{}", aliases.length, parameterTypes.length);
				throw new AliasLengthNotMatchException();
			}
			tupleAliasLengthCheck = true;
		}
		
		int index = 0; //一旦抛异常时，用于记录是哪个字段出错
		try {
			// 用构造时缓存的无参构造器创建 Bean, 替代废弃的 resultClass.newInstance()
			result = resultConstructor.newInstance();

			for (int i = 0; i < aliases.length; i++) {
				index = i;
				/*
				 * aliases、tuple、methodHandles、parameterTypes 在顺序上都是一一对应的，所以直接用i
				 */
				Object value = tuple[i];
				MethodHandle methodHandle = methodHandles[i];
				Class<?> parameterType = parameterTypes[i];

				/*
				 * 产线环境下容忍select子句包含列 col, 但是bean不包含col这种情况
				 * 此时对应parameterType就为null, 所以产线环境要跳过
				 */
				if (parameterType == null && queryMode.equals(LOOSE)) {
					continue;
				}

				/*
				 * 如果为null还是有必要setter一下，防止bean属性有默认值的情况
				 */
				if (value == null) {
					//如果是原始类型就不需要set null了
					if (Boolean.TYPE.equals(parameterType)
							|| Long.TYPE.equals(parameterType)
							|| Integer.TYPE.equals(parameterType)
							|| Float.TYPE.equals(parameterType)
							|| Double.TYPE.equals(parameterType)
							|| Character.TYPE.equals(parameterType)
							|| Short.TYPE.equals(parameterType)
							|| Byte.TYPE.equals(parameterType)) {
						continue;
					}
					logger.debug("alias[{}]值为null,仍然调用methodHandle.invoke(result, null)，避免该属性有默认值的情况", i);
					methodHandle.invoke(result, null);
					continue;
				}

				/*
				 * 接下来是value不为null的情况
				 */
				Boolean needCast = needCasts[i];
				/*
				 * 此时value不为null，那么应该知道是否需要做类型转换
				 * 所以如果needCast还是null的话就要抛异常出来了，这一列没法处理
				 * @on
				 */
				if (needCast == null) {
					// 没有办法处理则抛异常
					String message = format("没有办法将值为[{0}]的[{1}]转换为{2}，抛出异常!",
							value,
							value.getClass().getName(),
							parameterType);
					logger.debug(message);
					throw new RuntimeException(message);
				}

				/*
				 * 需要做类型转换的情况
				 */
				if (needCast) {
					logger.debug("needCasts[{}]为true, 因此列[{}]需要做类型转换, 值为[{}],先确定采用哪个ValueHandler", i, aliases[i], value);
					ValueHandlerFactory.ValueHandler valueHandler = ValueHandlerFactory.determineAppropriateHandler(parameterType, fields[i]);
					//可以通过valueHandler处理
					if (valueHandler != null) {
						logger.debug("找到对应的valuehandler[{}]，用valueHandler做类型转换", valueHandler.getClass().getName());
						methodHandle.invoke(result, valueHandler.convert(value));
					} else {
						logger.debug("valueHandler不能处理，尝试通过attributeConverter来处理");
						AttributeConverter attributeConverter = attributeConverters[i];
						if (attributeConverter != null) {
							logger.debug("找到[{}]，通过其处理[{}][{}]到{}的转换", attributeConverter, value.getClass().getName(), value, parameterType.getName());
							methodHandle.invoke(result, attributeConverter.convertToEntityAttribute(value));
						} else if (parameterType.isEnum()) {//valueHandler处理不了, 也没有AttributeConverter, 但是该类型为Enum，反射可以解决
							Object transformed = null;
							//如果指定了属性，则优先按属性查找Enum
							for (String property : enumLookupProperties) {
								transformed = EnumUtils.lookupEnum((Class<Enum>) parameterType, value, property);
								if (transformed != null) {
									break;
								}
							}
							
							//如果根据指定的属性找不到enum，那么根据name和ordinal去找
							if(transformed == null) {
								transformed = EnumUtils.lookupEnum((Class<Enum>) parameterType, value);
							}
							//如果value是boolean, 则 false对应0 true对应1
							if(value != null && Boolean.class.isInstance(value)) {
								boolean booleanValue = (boolean) value;
								if(booleanValue) {
									transformed = EnumUtils.lookupEnum((Class<Enum>) parameterType, 1);
								} else {
									transformed = EnumUtils.lookupEnum((Class<Enum>) parameterType, 0);
								}
							}
							
							methodHandle.invoke(result, transformed);
						} else {
							String message = format("没有办法处理字段: {0}, 类型为: {1}，值为: {2} ,到目标类型: {3} 的转换，抛出异常!",
									value.getClass().getName(),
									value, parameterType);
							logger.debug(message);
							// 没有办法处理则抛异常
							throw new RuntimeException(message);
						}
					}
				} else {// 不需要做类型转换
					logger.debug("alias[{}] needCasts[{}]为false，因此不需要做类型转换", aliases[i], i);
					methodHandle.invoke(result, value);
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName() + ", alias: "
					+ aliases[index] + ", parameterType: " + parameterTypes[index] + ", value: " + tuple[index]);
		} catch (InvocationTargetException e) {
			// Bean 自己的无参构造器里抛了异常, 把真实原因带出来, 不再被 newInstance() 吞掉
			throw new HibernateException("resultClass [" + resultClass.getName()
					+ "] 的无参构造器执行失败: " + e.getCause(), e.getCause());
		} catch (Throwable e) {
			logger.error("通用异常 alias: " + aliases[index] +", parameterType: " + parameterTypes[index] + ", value: " + tuple[index]);
			throw new ApplicationException(e);
		}

		return result;
	}

	/**
	 * 如果setter参数是布尔型的，那么确定其propertyName是带is的还是不带的
	 * 返回的propertyName都是小写的
	 * 
	 * 如果属性是boolean型的，命名是isXXX,那么返回{"xx", "isxx"},否则{"xx", "xx"}
	 * 
	 * @param setter
	 * @param parameterType
	 * @return String[]
	 */
	private String[] getPropertyName(Method setter, Class<?> parameterType, Class<?> clazz) {
		String setterName = setter.getName().toLowerCase();
		String propertyName = setterName.substring(3);// 截掉set
		/*
		 * 如果是布尔型的，那么boolean类型属性handsome、isHanndsome的setter都是setHandsome
		 * 所以要确定真正的属性名要再去bean定义里面确认一下
		 * @on
		 */
		if (Boolean.class.equals(parameterType) || Boolean.TYPE.equals(parameterType)) {
			Field[] fields = clazz.getDeclaredFields();
			/*
			 * 如果找得到，表示property声明的是handsome，否则是isHandsome
			 */
			boolean found = false;
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				if (field.getName().equalsIgnoreCase(propertyName)) {
					found = true;
					break;
				}
			}
			if (found) {
				return new String[] { propertyName, propertyName };
			}

			if (clazz.getSuperclass() != Object.class) {
				return getPropertyName(setter, parameterType, clazz.getSuperclass());
			}
			return new String[] { propertyName, "is" + propertyName };
		}

		return new String[] { propertyName, propertyName };
	}

	public Set<String> getEnumLookupProperties() {
		return enumLookupProperties;
	}

	public void setEnumLookupProperties(Set<String> enumLookupProperties) {
		this.enumLookupProperties = enumLookupProperties;
	}

}
