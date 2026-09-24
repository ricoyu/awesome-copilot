package com.awesomecopilot.cache;

import com.awesomecopilot.cache.operations.JedisClusterOperations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Cluster 兼容性回归测试(评估报告 P0-3 / P0-4)。
 * <p>
 * 前提: 需要一个 6 节点 Redis Cluster(3主3从)。本机环境是 WSL docker 起的
 * 172.30.31.182:7001-7006, 重建命令(WSL 内执行):
 * <pre>
 * for p in 7001 7002 7003 7004 7005 7006; do
 *   docker run -d --name rc$p --network host redis:6.2.7 redis-server --port $p \
 *     --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 \
 *     --appendonly no --save "" --protected-mode no
 * done
 * docker exec -it rc7001 redis-cli --cluster create \
 *   172.30.31.182:7001 172.30.31.182:7002 172.30.31.182:7003 \
 *   172.30.31.182:7004 172.30.31.182:7005 172.30.31.182:7006 --cluster-replicas 1
 * </pre>
 * 集群不可达时整个类跳过(assumeTrue), 不误报失败。
 * <p>
 * 原缺陷:
 * <ul>
 *   <li>P0-4: HASH 带 field 过期的接口把 hash key 与派生 zset key
 *       (jedis_utils:__timeout__set:&lt;key&gt;)一起作为 KEYS 传给 Lua, 两个 key 的 slot
 *       几乎必然不同, jedis 客户端直接抛 "Keys must belong to same hashslot";</li>
 *   <li>P0-3: AuthUtils 用 keyCount=0 的 evalsha, 集群下被随机路由到没有加载过脚本的
 *       节点, 每个登录/认证请求都报 NOSCRIPT。</li>
 * </ul>
 * Copyright: (C), 2026/9/24
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RedisClusterCompatTest {

	private static final String CLUSTER_HOST = "172.30.31.182";
	private static final int[] CLUSTER_PORTS = {7001, 7002, 7003, 7004, 7005, 7006};

	/** 集群模式验证的公共 hash tag: 所有 auth 相关 key 都带上它, 保证同 slot */
	private static final String AUTH_TAG = "{auth}";

	private static JedisCluster cluster;
	private static Object originalOperations;
	private static boolean clusterReachable;

	@BeforeAll
	public static void switchJedisUtilsToCluster() throws Exception {
		Set<HostAndPort> nodes = new HashSet<>();
		for (int p : CLUSTER_PORTS) {
			nodes.add(new HostAndPort(CLUSTER_HOST, p));
		}
		try {
			cluster = new JedisCluster(nodes, 2000, 2000, 3,
					(String) null, new ConnectionPoolConfig());
			cluster.ping();
			clusterReachable = true;
		} catch (Exception e) {
			clusterReachable = false;
			System.err.println("[skip] Redis Cluster 不可达: " + e.getMessage());
			return;
		}

		// 把 JedisUtils 的 operations 换成集群(生产是类加载时从 redis.properties 建的单节点池)
		// 顺序: 先清 shaHashs 再换 operations——若反射本身失败(字段改名等), 交换还没发生,
		// 不会把集群对象留在静态字段上(评审发现 F6: @BeforeAll 抛错时 JUnit5 不执行 @AfterAll)
		Field shas = JedisUtils.class.getDeclaredField("shaHashs");
		shas.setAccessible(true);
		((Map<?, ?>) shas.get(null)).clear();
		Field ops = JedisUtils.class.getDeclaredField("jedisOperations");
		ops.setAccessible(true);
		originalOperations = ops.get(null);
		ops.set(null, new JedisClusterOperations(cluster));
	}

	@AfterAll
	public static void restore() throws Exception {
		if (clusterReachable) {
			Field ops = JedisUtils.class.getDeclaredField("jedisOperations");
			ops.setAccessible(true);
			ops.set(null, originalOperations);
			Field shas = JedisUtils.class.getDeclaredField("shaHashs");
			shas.setAccessible(true);
			((Map<?, ?>) shas.get(null)).clear();
			if (cluster != null) {
				cluster.close();
			}
		}
	}

	/**
	 * P0-4: 集群下带 field 过期时间的 hset/hget 应正常工作(修复前抛 "Keys must belong to same hashslot")
	 */
	@Test
	public void testHashFieldTtlOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String key = "cluster-hash-test-" + ProcessHandle.current().pid();
		try {
			JedisUtils.HASH.hset(key, "f1", "v1", 5, TimeUnit.SECONDS);
			assertEquals("v1", JedisUtils.HASH.hget(key, "f1"), "写入后立即读取应拿到值");
			// 派生 zset 必须与 hash key 同 slot(修复后 key 形如 {<hashkey>}:...)
			Map<String, String> all = JedisUtils.HASH.hgetAll(key);
			assertEquals(1, all.size());
		} finally {
			JedisUtils.del(key);
		}
	}

	/**
	 * P0-4: field 过期语义在集群下同样成立
	 */
	@Test
	public void testHashFieldExpiryOnCluster() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String key = "cluster-hash-expire-" + ProcessHandle.current().pid();
		try {
			JedisUtils.HASH.hset(key, "f1", "v1", 2, TimeUnit.SECONDS);
			TimeUnit.SECONDS.sleep(3);
			assertNull(JedisUtils.HASH.hget(key, "f1"), "2 秒过期的 field 在 3 秒后应读不到");
		} finally {
			JedisUtils.del(key);
		}
	}

	/**
	 * P0-4: hset 不带 ttl(不过期)的常规路径在集群下不受影响
	 */
	@Test
	public void testPlainHashOpsOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String key = "cluster-hash-plain-" + ProcessHandle.current().pid();
		try {
			Map<String, String> m = new HashMap<>();
			m.put("a", "1");
			m.put("b", "2");
			JedisUtils.HASH.hmset(key, m);
			assertEquals(2, JedisUtils.HASH.hgetAll(key).size());
			JedisUtils.HASH.hset(key, "a", "9");
			assertEquals("9", JedisUtils.HASH.hget(key, "a"));
		} finally {
			JedisUtils.del(key);
		}
	}

	/**
	 * P0-3: 集群下 AuthUtils 登录→认证→登出全链路应可用(修复前每步 NOSCRIPT/随机路由)。
	 * 顺带验证 Java 常量与 Lua 键名一致性: login 传的 additionalInfo 要能被 loginInfo() 读回
	 * (修复前两边键名不同: Java 用 auth:token:login:info, Lua 用 auth:token:loginInfo, 永远读不到)
	 */
	@Test
	public void testAuthFlowOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String token = "cluster-token-" + ProcessHandle.current().pid();
		try {
			// additionalInfo 语义上要传对象(toBytes 走 Jackson 序列化); 裸 String 存的不是合法 JSON, loginInfo() 解析会失败
			boolean ok = com.awesomecopilot.cache.auth.AuthUtils.login(
					"clusteruser", token, 60, TimeUnit.SECONDS,
					"details-x", java.util.List.of("ROLE_USER"),
					java.util.Map.of("device", "dev1"));
			assertTrue(ok, "集群下登录应成功(修复前 NOSCRIPT)");
			assertEquals("clusteruser", com.awesomecopilot.cache.auth.AuthUtils.checkToken(token),
					"checkToken 应返回用户名");
			assertTrue(com.awesomecopilot.cache.auth.AuthUtils.isLogined("clusteruser"));
			assertEquals("dev1",
					com.awesomecopilot.cache.auth.AuthUtils.loginInfo(token, java.util.Map.class).get("device"),
					"登录附加信息应能按 token 读回");
			assertTrue(com.awesomecopilot.cache.auth.AuthUtils.logout(token));
			assertNull(com.awesomecopilot.cache.auth.AuthUtils.checkToken(token), "登出后 token 应失效");
		} finally {
			try {
				com.awesomecopilot.cache.auth.AuthUtils.logout(token);
			} catch (Exception ignore) {
				// 清理尽力而为
			}
			for (String k : new String[]{AUTH_TAG + ":token:username", AUTH_TAG + ":token:userdetails",
					AUTH_TAG + ":token:authorities", AUTH_TAG + ":token:login:info",
					AUTH_TAG + ":token:ttl", AUTH_TAG + ":token:ttl:zset",
					AUTH_TAG + ":clusteruser:token", "token:authentication"}) {
				try {
					cluster.del(k);
				} catch (Exception ignore) {
					// 清理尽力而为
				}
			}
		}
	}
}
