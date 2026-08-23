package com.awesomecopilot.cache.factory;

/**
 * Redis 部署类型相关的配置 key 常量。
 * <p>
 * Redis 连接池的创建逻辑统一由 {@link JedisOperationFactory#create()} 负责,
 * 该入口通过 {@link com.awesomecopilot.cache.config.RedisConfigReader} 支持从
 * Nacos(Spring Environment) / 系统属性 / 环境变量 / 本地 redis.properties 读取配置。
 * <p>
 * Copyright: Copyright (c) 2019-10-17 14:04
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class JedisPoolFactories {

	/**
	 * 哨兵模式配置 key, 值为 ip:port,ip:port 形式
	 */
	public static final String SENTINELS = "redis.sentinels";

	/**
	 * 集群模式配置 key, 值为 ip:port,ip:port 形式
	 */
	public static final String CLUSTERS = "redis.clusters";
}
