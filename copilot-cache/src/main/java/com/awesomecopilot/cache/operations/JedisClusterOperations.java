package com.awesomecopilot.cache.operations;

import com.awesomecopilot.cache.concurrent.ThreadPool;
import com.awesomecopilot.cache.exception.JedisException;
import com.awesomecopilot.cache.utils.CancellableJedisPubSub;
import com.awesomecopilot.json.jackson.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.awesomecopilot.cache.utils.ByteUtils.toBytes;
import static java.util.stream.Collectors.*;

/**
 * <p>
 * Copyright: (C), 2019/10/24 7:38
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisClusterOperations implements JedisOperations {
	
	private static final Logger log = LoggerFactory.getLogger(JedisClusterOperations.class);
	
	private final JedisCluster jedisCluster;

	/**
	 * 订阅动作专用线程池: SUBSCRIBE 会独占连接直到退订, 不能在调用线程上执行
	 * (与 JedisPoolOperations 的订阅线程池同理由), 见 subscribe/psubscribe
	 */
	private static final ExecutorService SUBSCRIBE_POOL = ThreadPool.newThreadPool();

	public JedisClusterOperations(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}
	
	@Override
	public String set(byte[] key, byte[] value) {
		return jedisCluster.set(key, value);
	}
	
	@Override
	public Long setnx(byte[] key, byte[] value) {
		return jedisCluster.setnx(key, value);
	}
	
	@Override
	public byte[] get(byte[] key) {
		return jedisCluster.get(key);
	}
	
	@Override
	public Boolean exists(String key) {
		return jedisCluster.exists(key);
	}
	
	@Override
	public Boolean exists(byte[] key) {
		return jedisCluster.exists(key);
	}
	
	@Override
	public List<String> keys(String pattern) {
		// 2026-09-28 集群兼容修复(clauter评审 P1-4): 旧版直接调 jedisCluster.keys(pattern),
		// 而 Jedis 对集群版 KEYS 有两道客户端限制——pattern 必须含 hash tag(否则本地抛
		// IllegalArgumentException)、有 tag 也只路由到该 tag 的单一 slot, 结果不是全库的键。
		// 改为向每个节点的连接池借一条连接、逐个节点执行原始 KEYS 再合并(Redis 对 KEYS 就地执行,
		// 不做 MOVED 重定向; 从节点执行返回其副本键空间内容, 用 Set 去重)。
		Set<String> merged = new HashSet<>();
		for (ConnectionPool pool : jedisCluster.getClusterNodes().values()) {
			try (Connection connection = pool.getResource()) {
				Set<String> nodeKeys = connection.executeCommand(new CommandObject<>(
						new CommandArguments(Protocol.Command.KEYS).add(pattern),
						BuilderFactory.STRING_SET));
				merged.addAll(nodeKeys);
			} catch (Exception e) {
				throw new JedisException(
						"集群 keys() 在某节点执行失败, pattern=" + pattern + ": " + e.getMessage(), e);
			}
		}
		return new ArrayList<>(merged);
	}
	
	@Override
	public Long incr(String key) {
		return jedisCluster.incr(key);
	}
	
	@Override
	public Long incrBy(String key, long increment) {
		return jedisCluster.incrBy(key, increment);
	}
	
	@Override
	public Double zscore(String key, String member) {
		return jedisCluster.zscore(key, member);
	}
	
	@Override
	public Long zadd(String key, double score, String member) {
		return jedisCluster.zadd(key, score, member);
	}
	
	@Override
	public Long zadd(byte[] key, double score, byte[] member) {
		return jedisCluster.zadd(key, score, member);
	}
	
	@Override
	public Long zadd(String key, double score, Object member) {
		return jedisCluster.zadd(toBytes(key), score, JacksonUtils.toBytes(member));
	}
	
	@Override
	public Long zremByRank(String key, long start, long end) {
		return jedisCluster.zremrangeByRank(key, start, end);
	}
	
	@Override
	public Long zremRangeByScore(String key, String min, String max) {
		return jedisCluster.zremrangeByScore(key, min, max);
	}
	
	@Override
	public double zincrby(String key, String member, int increment) {
		return jedisCluster.zincrby(key, (double)increment, member);
	}
	
	@Override
	public List<String> zrevrange(String key, int start, int end) {
		return jedisCluster.zrevrange(key, (long)start, (long)end);
	}
	
	@Override
	public Long zcard(String key) {
		return jedisCluster.zcard(key);
	}
	
	@Override
	public byte[] lpop(byte[] key) {
		return jedisCluster.lpop(key);
	}
	
	@Override
	public String lpop(String key) {
		return jedisCluster.lpop(key);
	}
	
	@Override
	public Long lpush(String key, String... values) {
		return jedisCluster.lpush(key, values);
	}
	
	@Override
	public Long lpush(byte[] key, byte[]... values) {
		return jedisCluster.lpush(key, values);
	}
	
	@Override
	public Long rpush(String key, String... values) {
		return jedisCluster.rpush(key, values);
	}
	
	@Override
	public Long rpush(byte[] key, byte[]... values) {
		return jedisCluster.rpush(key, values);
	}
	
	@Override
	public List<String> blpop(int timeout, String key) {
		return jedisCluster.blpop(timeout, key);
	}
	
	@Override
	public List<byte[]> blpop(int timeout, byte[]... keys) {
		return jedisCluster.blpop(timeout, keys);
	}
	
	@Override
	public List<String> brpop(int timeout, String key) {
		return jedisCluster.brpop(timeout, key);
	}
	
	@Override
	public List<String> brpop(int timeout, String... keys) {
		return jedisCluster.brpop(timeout, keys);
	}
	
	@Override
	public List<byte[]> brpop(int timeout, byte[]... keys) {
		return jedisCluster.brpop(timeout, keys);
	}
	
	@Override
	public String rpop(String key) {
		return jedisCluster.rpop(key);
	}
	
	@Override
	public List<String> rpop(String key, Integer count) {
		return jedisCluster.rpop(key, count);
	}
	
	@Override
	public Long llen(String key) {
		return jedisCluster.llen(key);
	}
	
	@Override
	public String lindex(String key, int index) {
		return jedisCluster.lindex(key, index);
	}
	
	@Override
	public String lindex(String key, long index) {
		return jedisCluster.lindex(key, index);
	}
	
	@Override
	public List<String> lrange(String key, long start, long stop) {
		return jedisCluster.lrange(key, start, stop);
	}
	
	@Override
	public List<byte[]> lrange(byte[] key, long start, long stop) {
		return jedisCluster.lrange(key, start, stop);
	}
	
	@Override
	public Long lrem(String key, long count, String value) {
		return jedisCluster.lrem(key, count, value);
	}
	
	@Override
	public Long sadd(String key, String... members) {
		return jedisCluster.sadd(key, members);
	}
	
	@Override
	public Long sadd(byte[] key, byte[]... members) {
		return jedisCluster.sadd(key, members);
	}
	
	@Override
	public Long srem(byte[] key, byte[]... member) {
		return jedisCluster.srem(key, member);
	}
	
	@Override
	public Long scard(String key) {
		return jedisCluster.scard(key);
	}
	
	@Override
	public Boolean sismember(byte[] key, byte[] member) {
		return jedisCluster.sismember(key, member);
	}
	
	@Override
	public Set<byte[]> smembers(byte[] key) {
		return jedisCluster.smembers(key);
	}
	
	@Override
	public Set<String> smembers(String key) {
		return jedisCluster.smembers(key);
	}
	
	@Override
	public List<String> sirandmember(String key, int count) {
		return jedisCluster.srandmember(key, count);
	}
	
	@Override
	public Set<String> spop(String key, int count) {
		return jedisCluster.spop(key, (long)count);
	}
	
	@Override
	public Set<String> sinter(String key1, String key2) {
		return jedisCluster.sinter(key1, key2);
	}
	
	@Override
	public Boolean hexists(byte[] key, byte[] field) {
		return jedisCluster.hexists(key, field);
	}
	
	@Override
	public Boolean hexists(String key, String field) {
		return jedisCluster.hexists(key, field);
	}
	
	@Override
	public byte[] hget(byte[] key, byte[] field) {
		return jedisCluster.hget(key, field);
	}
	
	@Override
	public Long hset(byte[] key, byte[] field, byte[] value) {
		return jedisCluster.hset(key, field, value);
	}
	
	@Override
	public String hmset(String key, Map<String, String> hash) {
		return jedisCluster.hmset(key, hash);
	}
	
	@Override
	public long hincrby(String key, String field, long value) {
		return jedisCluster.hincrBy(key, field, value);
	}
	
	@Override
	public List<byte[]> hmget(byte[] key, byte[]... fields) {
		return jedisCluster.hmget(key, fields);
	}
	
	@Override
	public List<String> hmget(String key, String... fields) {
		return jedisCluster.hmget(key, fields);
	}
	
	@Override
	public Map<byte[], byte[]> hgetAll(byte[] key) {
		return jedisCluster.hgetAll(key);
	}
	
	@Override
	public Map<String, String> hgetAll(String key) {
		return jedisCluster.hgetAll(key);
	}
	
	@Override
	public List<String> hvals(String key) {
		return jedisCluster.hvals(key);
	}
	
	@Override
	public List<String> zrange(String key, long start, long end) {
		return jedisCluster.zrange(key, start, end);
	}
	
	@Override
	public List<String> zrangeByScore(String key, String min, String max) {
		return jedisCluster.zrangeByScore(key, min, max);
	}
	
	@Override
	public Long hlen(byte[] key) {
		return jedisCluster.hlen(key);
	}
	
	@Override
	public Long expire(String key, int seconds) {
		return jedisCluster.expire(key, seconds);
	}
	
	@Override
	public Long expire(byte[] key, int seconds) {
		return jedisCluster.expire(key, seconds);
	}
	
	@Override
	public Long expireAt(String key, long unixTime) {
		return jedisCluster.expireAt(key, unixTime);
	}
	
	@Override
	public Long expireAt(byte[] key, long unixTime) {
		return jedisCluster.expireAt(key, unixTime);
	}
	
	@Override
	public Long persist(String key) {
		return jedisCluster.persist(key);
	}
	
	@Override
	public Long persist(byte[] key) {
		return jedisCluster.persist(key);
	}
	
	@Override
	public Long ttl(String key) {
		return jedisCluster.ttl(key);
	}
	
	@Override
	public Long ttl(byte[] key) {
		return jedisCluster.ttl(key);
	}
	
	@Override
	public Long del(String key) {
		return jedisCluster.del(key);
	}
	
	@Override
	public Long del(byte[] key) {
		return jedisCluster.del(key);
	}
	
	@Override
	public Object eval(String script, String sampleKey) {
		return jedisCluster.eval(script, sampleKey);
	}
	
	@Override
	public Object eval(String script, int keyCount, String... params) {
		return jedisCluster.eval(script, keyCount, params);
	}
	
	/**
	 * 集群版广播加载: 委托 jedisCluster.scriptLoad(script) —— Jedis 会向集群所有节点
	 * (主+从, 实测 6/6)逐个 SCRIPT LOAD, 各节点返回同一 SHA。
	 * 供无路由键的脚本(keyCount=0, 如 hash.lua 的 time 操作)使用: 随机路由到的任何节点都持有脚本,
	 * 不会 NOSCRIPT(clauter评审 P0-3)。
	 */
	@Override
	public String scriptLoad(String script) {
		return jedisCluster.scriptLoad(script);
	}

	@Override
	public String scriptLoad(String script, String sampleKey) {
		return jedisCluster.scriptLoad(script, sampleKey);
	}

	/**
	 * 接口里 (String, Object) 这个重载在集群下必须实现: JedisUtils.loadScript 以 Object 传 sampleKey,
	 * 静态分派到这里; 若不覆盖会落进接口的默认实现并抛"不支持"(集群脚本加载即失效)。
	 */
	@Override
	public String scriptLoad(String script, Object sampleKey) {
		if (sampleKey instanceof byte[]) {
			return jedisCluster.scriptLoad(script, new String((byte[]) sampleKey, java.nio.charset.StandardCharsets.UTF_8));
		}
		return jedisCluster.scriptLoad(script, String.valueOf(sampleKey));
	}
	
	@Override
	public byte[] scriptLoad(byte[] script, byte[] sampleKey) {
		return jedisCluster.scriptLoad(script, sampleKey);
	}
	
	@Override
	public Object evalsha(String sha1, String sampleKey) {
		return jedisCluster.evalsha(sha1, sampleKey);
	}
	
	@Override
	public Object evalsha(String sha1, int keyCount, String... params) {
		return jedisCluster.evalsha(sha1, keyCount, params);
	}
	
	@Override
	public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
		return jedisCluster.evalsha(sha1, keyCount, params);
	}
	
	@Override
	public Long publish(byte[] channel, byte[] message) {
		return jedisCluster.publish(channel, message);
	}
	
	/**
	 * 集群订阅(clauter评审 P1-5 修复): 旧实现只打一行日志不做事, 调用方无感知,
	 * BlockingLock 等锁线程收不到解锁通知、auth 频道监听丢消息。
	 * <p>
	 * 原理: Redis 普通 pubsub 消息经集群总线在所有节点间转发, 在任意一个节点执行 SUBSCRIBE
	 * 就能收到全集群的发布(Jedis 官方 UnifiedJedis.subscribe 即"取一条随机连接执行 proceed");
	 * 这里从集群各节点连接池随机挑一个照做, 订阅线程池异步执行(SUBSCRIBE 独占连接直到退订,
	 * 不能占用调用线程), 逐个节点尝试直到建立成功; 排队期间被取消的订阅直接放弃。
	 */
	@Override
	public void subscribe(JedisPubSub jedisPubSub, String... channels) {
		SUBSCRIBE_POOL.execute(() -> {
			if (jedisPubSub instanceof CancellableJedisPubSub cancellable && cancellable.isCancelled()) {
				log.debug("订阅在排队期间已被取消, 不再建立订阅: {}", String.join(",", channels));
				return;
			}
			runSubscribed(jedisPubSub, "subscribe", String.join(",", channels),
					(connection) -> jedisPubSub.proceed(connection, channels));
		});
	}

	/**
	 * 集群模式订阅, 语义同 {@link #subscribe(JedisPubSub, String...)}
	 */
	@Override
	public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
		SUBSCRIBE_POOL.execute(() -> {
			if (jedisPubSub instanceof CancellableJedisPubSub cancellable && cancellable.isCancelled()) {
				log.debug("订阅在排队期间已被取消, 不再建立模式订阅: {}", String.join(",", patterns));
				return;
			}
			runSubscribed(jedisPubSub, "psubscribe", String.join(",", patterns),
					(connection) -> jedisPubSub.proceedWithPatterns(connection, patterns));
		});
	}

	/**
	 * 从集群各节点连接池中随机依次挑连接执行订阅动作(proceed 阻塞到退订结束),
	 * 某节点借连接失败就换下一个; 全部失败记错误日志。
	 */
	private void runSubscribed(JedisPubSub jedisPubSub, String op, String targets,
	                           java.util.function.Consumer<Connection> action) {
		List<ConnectionPool> pools = new ArrayList<>(jedisCluster.getClusterNodes().values());
		java.util.Collections.shuffle(pools);
		Exception last = null;
		for (ConnectionPool pool : pools) {
			try (Connection connection = pool.getResource()) {
				action.accept(connection);
				return;
			} catch (Exception e) {
				last = e;
				log.warn("集群 {} 在节点 {} 上失败, 尝试下一个节点: {}", op, pool, e.getMessage());
			}
		}
		log.error("集群 {} 所有节点都失败, 订阅未建立: targets={}", op, targets, last);
	}
	
	@Override
	public Boolean setbit(String key, long offset, int value) {
		return jedisCluster.setbit(key, offset, value == 1);
	}
	
	@Override
	public Boolean setbit(String key, long offset, Boolean value) {
		return jedisCluster.setbit(key, offset, value);
	}
	
	@Override
	public Boolean getbit(String key, long offset) {
		return jedisCluster.getbit(key, offset);
	}
	
	@Override
	public Long bitAnd(String destKey, String[] srcKeys) {
		return jedisCluster.bitop(BitOP.AND, destKey, srcKeys);
	}
	
	@Override
	public Long bitOr(String destKey, String[] srcKeys) {
		return jedisCluster.bitop(BitOP.OR, destKey, srcKeys);
	}
	
	@Override
	public Long bitNot(String destKey, String srcKey) {
		return jedisCluster.bitop(BitOP.NOT, destKey, srcKey);
	}
	
	@Override
	public long bitCount(String key, int start, int end) {
		return jedisCluster.bitcount(key, start, end);
	}
	
	@Override
	public long bitCount(String key) {
		return jedisCluster.bitcount(key);
	}
	
	@Override
	public Long pfadd(String key, String... elements) {
		return jedisCluster.pfadd(key, elements);
	}
	
	@Override
	public Long pfcount(String... keys) {
		return jedisCluster.pfcount(keys);
	}
	
	@Override
	public String pfmerge(String destKey, String[] sourceKeys) {
		return jedisCluster.pfmerge(destKey, sourceKeys);
	}
	
	@Override
	public Long geoadd(String key, double longitude, double latitude, String member) {
		return jedisCluster.geoadd(key, longitude, latitude, member);
	}
	
	@Override
	public Long geoadd(String key, Map<String, GeoCoordinate> geoCoordinateMap) {
		return jedisCluster.geoadd(key, geoCoordinateMap);
	}
	
	@Override
	public Double geoDist(String key, String member1, String member2) {
		return jedisCluster.geodist(key, member1, member2);
	}
	
	@Override
	public Double geoDist(String key, String member1, String member2, GeoUnit unit) {
		return jedisCluster.geodist(key, member1, member2, unit);
	}
	
	@Override
	public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		return jedisCluster.georadiusByMember(key, member, radius, unit);
	}
	
	@Override
	public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
		return jedisCluster.georadius(key, longitude, latitude, radius, unit);
	}
	
	@Override
	public Jedis jedis() {
		throw new UnsupportedOperationException("JedisClusterOperations不支持暴露Jedis");
	}
}