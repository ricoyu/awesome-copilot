package com.awesomecopilot.cache.factory;

import com.awesomecopilot.cache.config.RedisConfigReader;
import com.awesomecopilot.cache.config.RedisProperties;
import com.awesomecopilot.cache.operations.JedisClusterOperations;
import com.awesomecopilot.cache.operations.JedisOperations;
import com.awesomecopilot.cache.operations.JedisPoolOperations;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.common.lang.resource.YamlOps;
import com.awesomecopilot.common.lang.resource.YamlProfileReaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * <p>
 * Copyright: (C), 2019/10/25 14:44
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class JedisOperationFactory {
	
	private static final Logger log = LoggerFactory.getLogger(JedisOperationFactory.class);
	
	private static final String SENTINELS = "redis.sentinels";
	private static final String CLUSTERS = "redis.clusters";

	/**
	 * 注册 Spring ApplicationContext, 使工厂能够从 Spring Environment(含 Nacos 配置中心)读取 Redis 配置。
	 * <p>
	 * 在 Spring Boot 应用中无需调用本方法, 由 {@link com.awesomecopilot.cache.config.RedisConfigInitializer}
	 * 通过 spring.factories 在应用启动阶段自动注册 Spring Environment。仅当在非 Spring Boot 的
	 * Spring 环境中使用时, 才需要手动调用本方法注册。
	 *
	 * @param applicationContext Spring 应用上下文
	 */
	public static void registerContext(Object applicationContext) {
		RedisConfigReader.registerContext(applicationContext);
	}

	public static JedisOperations create() {
		
		/**
		 * 默认读取classpath下redis.properties文件,
		 * 如果注册了 Spring ApplicationContext, 则优先从 Spring Environment(Nacos) 读取配置
		 */
		PropertyReader propertyReader = new RedisConfigReader("redis");
		RedisProperties redisProperties = null;
		
		/*
		 * 如果没有找到redis.properties, 那么先尝试从SpringBoot Redis配置里面获取配置信息
		 * 如果redis.properties 和 SpringBoot的application.yml都没有, 就默认配置访问
		 * localhost 6379 的Redis
		 */
		if (!propertyReader.resourceExists()) {
			YamlOps yamlOps = YamlProfileReaders.instance("application");
			if (yamlOps.exists()) {
				String host = yamlOps.getString("spring.redis.host", "localhost");
				int port = yamlOps.getInt("spring.redis.port", 6379);
				String password = yamlOps.getString("spring.redis.password");
				int database = yamlOps.getInt("spring.redis.database", 0);
				
				//spring redis里面这个配置项是connectionTimeout
				int connectionTimeout = yamlOps.getInt("spring.redis.timeout", 50000);
				
				int maxTotal = yamlOps.getInt("spring.redis.jedis.pool.max-active", 50);
				int maxIdle = yamlOps.getInt("spring.redis.jedis.pool.max-idle", 50);
				int minIdle = yamlOps.getInt("spring.redis.jedis.pool.min-idle", 8);
				
				redisProperties = new RedisProperties();
				redisProperties.setHost(host);
				redisProperties.setPort(port);
				redisProperties.setPassword(password);
				redisProperties.setDatabase(database);
				
				redisProperties.setConnectionTimeout(connectionTimeout);
				
				redisProperties.setMaxTotal(maxTotal);
				redisProperties.setMaxIdle(maxIdle);
				redisProperties.setMinIdle(minIdle);
				
				/*
				 * 如果是采用spring-data-redis的配置的话, 暂时先只支持单节点模式
				 */
				JedisPool jedisPool = new JedisPoolFactory().createPool(redisProperties);
				warmUp(jedisPool, minIdle);
				return new JedisPoolOperations(jedisPool);
			}
		}
		
		/*
		 * Redis的部署类型：single, sentinel, cluster
		 * redis.sentinels 属性存在则采用sentinel形式
		 * redis.clusters 属性存在则用Cluster形式
		 * 都不存在则 Redis 单节点
		 */
		String sentinels = propertyReader.getString(SENTINELS);
		
		if (isNotEmpty(sentinels)) {
			//TODO Sentinel里面创建Pool时, 还没有把所有可配置属性加进去
			Pool<Jedis> pool = new JedisSentinelPoolFactory().createPool(propertyReader);
			boolean warnmUp = propertyReader.getBoolean("redis.warmUp", true);
			if (warnmUp) {
				warmUp(pool, propertyReader.getInt("redis.minIdle", 8));
			}
			return new JedisPoolOperations(pool);
		}
		
		if (isNotEmpty(propertyReader.getString(CLUSTERS))) {
			//TODO Cluster里面创建Pool时, 还没有把所有可配置属性加进去
			JedisCluster jedisCluster = new JedisClusterPoolFactory().createCluster(propertyReader);
			return new JedisClusterOperations(jedisCluster);
		}
		
		//这是Redis单Instance
		JedisPool jedisPool = new JedisPoolFactory().createPool(propertyReader);
		boolean warnmUp = propertyReader.getBoolean("redis.warmUp", true);
		if (warnmUp) {
			warmUp(jedisPool, propertyReader.getInt("redis.minIdle", 8));
		}
		return new JedisPoolOperations(jedisPool);
	}
	
	
	/**
	 * 预热JedisPool <p>
	 * 由于一些原因(例如超时时间设置较小原因), 有的项目在启动成功后会出现超时。
	 * JedisPool定义最大资源数、最小空闲资源数时, 不会真的把Jedis连接放到池子里,
	 * 第一次使用时, 池子没有资源使用, 会new Jedis, 使用后放到池子里, 可能会有一定的时间开销,
	 * 所以也可以考虑在JedisPool定义后, 为JedisPool提前进行预热, 例如以最小空闲数量为预热数量.
	 */
	public static void warmUp(Pool<Jedis> pool, int minIdle) {
		// 不卡住, 不影响Spring的启动
		new Thread(() -> {
			List<Jedis> minIdleJedisList = new ArrayList<Jedis>(minIdle);
			
			for (int i = 0; i < minIdle; i++) {
				Jedis jedis = null;
				try {
					jedis = pool.getResource();
					minIdleJedisList.add(jedis);
					jedis.ping();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				} finally {
					//注意, 这里不能马上close将连接还回连接池, 否则最后连接池里只会建立1个 连接
					//因为close是把连接还回连接池, 所以下次取出的还是这个连接, 即只会建立1个连接
					//jedis.close();
				}
			}
			
			for (int i = 0; i < minIdle; i++) {
				Jedis jedis = null;
				try {
					jedis = minIdleJedisList.get(i);
					//将连接还回连接池
					jedis.close();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}
			
		}, "<<<< JedisPool warmup thread >>>>").start();
	}
}