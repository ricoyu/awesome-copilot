package com.awesomecopilot.cache.factory;

import com.awesomecopilot.cache.config.RedisProperties;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;

/**
 * 配置方式
 *   redis.sentinels=192.168.10.101:16379,192.168.10.102:16379,192.168.10.103:16379
 *   redis.masterName=mymaster
 *   redis.password=deepdata$
 *   redis.timeout=2000
 *   redis.db=0
 * Sentinel连接方式的连接池工厂类 
 * <p>
 * Copyright: Copyright (c) 2019-10-17 14:10
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisSentinelPoolFactory implements PoolFactory {
	
	private static final Logger log = LoggerFactory.getLogger(JedisSentinelPoolFactory.class);
	
	@Override
	public Pool<Jedis> createPool(PropertyReader propertyReader) {
		//host:port,host:port,host:port
		String sentinels = propertyReader.getString("redis.sentinels");
		String password = propertyReader.getString("redis.password");
		//默认5秒超时
		int timeout = propertyReader.getInt("redis.timeout", 5000); 
		int db = propertyReader.getInt("redis.db", 0);

		String masterName = propertyReader.getString("redis.maserName", "mymaster");
		
		boolean isDebug = propertyReader.getBoolean("redis.debug", false);
		if (isDebug) {
			log.info("sentinels: {}", sentinels);
			log.info("timeout: {}", timeout);
			log.info("db: {}", db);
		}
		JedisSentinelPool sentinelPool;
		if (StringUtils.isNotBlank(password)) {
			sentinelPool = new JedisSentinelPool(masterName,
					asList(sentinels.split(",")).stream().collect(toSet()),
					genericPoolConfig(propertyReader),
					timeout,
					password,
					db);
		} else {
			sentinelPool = new JedisSentinelPool(masterName,
					asList(sentinels.split(",")).stream().collect(toSet()),
					genericPoolConfig(propertyReader),
					timeout,
					null,
					db);
		}

		log.debug("Current master: {}", sentinelPool.getCurrentHostMaster().toString());
		return sentinelPool;
	}

	@Override
	public Pool<Jedis> createPool(RedisProperties redisProperties) {
		return null;
	}

}