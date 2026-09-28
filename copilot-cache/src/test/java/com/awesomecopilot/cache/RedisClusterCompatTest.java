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
import java.util.List;
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

	// ================= 2026-09-28 clauter评审.md 六项修复的回归用例 =================

	/**
	 * 评审 P0-1: slidingWindow.lua 里自己拼键(脚本内拼 slading_window:zset:<name> 去读写)在集群下
	 * 违反"脚本只能访问 KEYS 声明过的同槽键"的服务端限制, 每次调用都报错。
	 * 修复后: Java 拼出完整键作 KEYS[1], 限流判定应正常返回。
	 */
	@Test
	public void testSlidingWindowOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String name = "cluster-swin-" + ProcessHandle.current().pid();
		long base = System.currentTimeMillis();
		try {
			assertTrue(JedisUtils.AFFLUENT.slidingWindows(name, "m1", base, 5000, 2),
					"窗口内第1个请求应放行(修复前脚本报错)");
			assertTrue(JedisUtils.AFFLUENT.slidingWindows(name, "m2", base + 10, 5000, 2),
					"窗口内第2个请求应放行");
			assertFalse(JedisUtils.AFFLUENT.slidingWindows(name, "m3", base + 20, 5000, 2),
					"超出限流值的第3个请求应拒绝");
		} finally {
			try {
				cluster.del("slading_window:zset:" + name);
			} catch (Exception ignore) {
				// 清理尽力而为
			}
		}
	}

	/**
	 * 评审 P0-2: rateLimit 脚本按原始 key 加载、按 rate:limit:<key> 执行。两键 slot 分属
	 * 不同主节点时(实测约 2/3 概率, KeyPick/RateLimitReplay 探针), 执行节点没有脚本缓存,
	 * 报 NOSCRIPT 且自愈无效(自愈重新加载仍用原始 key, 加载点不变)。
	 * 本用例用 CRC16 现算, 选一个"两键必分属不同主节点"的 key 并先清空全部节点脚本缓存,
	 * 使旧代码 100% 复现故障、新代码确定性通过。
	 * 修复后: 加载与执行用同一个真实键, 限流计数应正常。
	 */
	@Test
	public void testRateLimitOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		flushAllScriptCaches();
		// 找一个原始键与 rate:limit: 前缀键分属不同主节点的 key 后缀
		String key = null;
		for (int i = 0; i < 64; i++) {
			String candidate = "cluster-ratelimit-" + ProcessHandle.current().pid() + "-" + i;
			if (!masterOf(candidate).equals(masterOf("rate:limit:" + candidate))) {
				key = candidate;
				break;
			}
		}
		assertNotNull(key, "64 次都没选到跨主节点的 key, 检查本测试逻辑");
		try {
			assertTrue(JedisUtils.AFFLUENT.rateLimit(key, 10, 2), "第1次请求应放行(修复前 NOSCRIPT)");
			assertTrue(JedisUtils.AFFLUENT.rateLimit(key, 10, 2), "第2次请求应放行");
			assertFalse(JedisUtils.AFFLUENT.rateLimit(key, 10, 2), "第3次超过阈值应拒绝");
		} finally {
			try {
				cluster.del("rate:limit:" + key);
			} catch (Exception ignore) {
				// 清理尽力而为
			}
		}
	}

	/**
	 * 评审 P0-3: HASH.time() 用 keyCount=0 的 evalsha, 集群下被随机路由(实测 30 次失败 12 次),
	 * 落到的节点没有脚本就报 NOSCRIPT 且自愈无效(加载点仍是字符串 "hash.lua" 的固定槽)。
	 * 先清空全部节点脚本缓存排除其他用例加载的干扰; 旧代码下 50 次全中的概率约 (1/3)^50,
	 * 等价确定性复现。修复后(time() 的脚本全节点加载)应 50 次全成功。
	 */
	@Test
	public void testHashServerTimeOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		flushAllScriptCaches();
		long before = System.currentTimeMillis() - 2000;
		for (int i = 0; i < 50; i++) {
			long serverTime = JedisUtils.HASH.time();
			assertTrue(serverTime > before, "第" + i + "次取服务器时间应返回合理毫秒值(修复前随机 NOSCRIPT)");
		}
	}

	/**
	 * 清空集群所有节点的 Lua 脚本缓存 + 本 JVM 的 shaHashs 缓存, 让脚本加载路径从零开始,
	 * 排除同 JVM 其他用例已把脚本加载到某些节点的干扰。
	 */
	@SuppressWarnings("unchecked")
	private static void flushAllScriptCaches() {
		try {
			Field shas = JedisUtils.class.getDeclaredField("shaHashs");
			shas.setAccessible(true);
			((Map<String, String>) shas.get(null)).clear();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		// 通过 broadcast 语义的 scriptFlush: JedisCluster 继承 UnifiedJedis 的 scriptFlush() 就是全节点执行
		cluster.scriptFlush();
	}

	/** 键所属主节点的标识(ip:port), 用集群槽表计算; JedisCluster 没有 clusterSlots, 借任一节点直连查询, 结果缓存 */
	private static volatile List<Object> slotRanges;

	@SuppressWarnings("unchecked")
	private static String masterOf(String key) {
		if (slotRanges == null) {
			String firstNode = cluster.getClusterNodes().keySet().iterator().next();
			String[] hp = firstNode.split(":");
			try (redis.clients.jedis.Jedis j = new redis.clients.jedis.Jedis(hp[0], Integer.parseInt(hp[1]), 2000)) {
				slotRanges = j.clusterSlots();
			}
		}
		int slot = redis.clients.jedis.util.JedisClusterCRC16.getSlot(key);
		for (Object rangeObj : slotRanges) {
			List<Object> range = (List<Object>) rangeObj;
			long start = (Long) range.get(0), end = (Long) range.get(1);
			if (slot >= start && slot <= end) {
				List<Object> master = (List<Object>) range.get(2);
				String ip = redis.clients.jedis.util.SafeEncoder.encode((byte[]) master.get(0));
				return ip + ":" + ((Number) master.get(1)).longValue();
			}
		}
		throw new IllegalStateException("slot " + slot + " 未找到归属主节点");
	}

	/**
	 * 评审 P1-4: 集群下 jedis 的 KEYS 要求 pattern 含 hash tag 且只扫该 tag 的单一 slot。
	 * 修复后 JedisClusterOperations.keys() 遍历所有主节点合并结果, 跨 slot 的键也能查全。
	 */
	@Test
	public void testKeysScansAllNodesOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String pid = String.valueOf(ProcessHandle.current().pid());
		List<String> written = List.of("k4a:" + pid, "k4b:" + pid, "k4c:" + pid);
		try {
			for (String k : written) {
				cluster.set(k, "1");
			}
			List<String> found = JedisUtils.keys("k4*:" + pid);
			assertTrue(found.containsAll(written),
					"无 tag 的 pattern 应跨节点查全 3 个键(修复前抛 IllegalArgumentException), 实际: " + found);
		} finally {
			for (String k : written) {
				try {
					cluster.del(k);
				} catch (Exception ignore) {
					// 清理尽力而为: 集群下多键 del 可能跨 slot, 逐个删
				}
			}
		}
	}

	/**
	 * 评审 P1-4 补充: 带 tag 的 pattern 结果同样要正确(不能漏掉 tag 路由这条已有语义)。
	 */
	@Test
	public void testKeysWithTagPatternOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String pid = String.valueOf(ProcessHandle.current().pid());
		String key = "{k5tag}:" + pid + ":a";
		try {
			cluster.set(key, "1");
			List<String> found = JedisUtils.keys("{k5tag}:" + pid + "*");
			assertTrue(found.contains(key), "tag pattern 应查到键, 实际: " + found);
		} finally {
			try {
				cluster.del(key);
			} catch (Exception ignore) {
				// 清理尽力而为
			}
		}
	}

	/**
	 * 评审 P1-6: 报告断言"blpop 等待超过 socketTimeout 会抛读超时"。
	 * 复核 Jedis 5.0.2 源码发现 CommandObjects 给全部 blpop/brpop 变体打了 .blocking() 标记,
	 * Connection.executeCommand 对 blocking 命令临时把读超时放开(setTimeoutInfinite)再恢复,
	 * 本测试用 3 秒等待 > 2 秒 socketTimeout 来实测这条结论: 应正常返回 null 而不是抛异常。
	 */
	@Test
	public void testBlpopWaitsBeyondSocketTimeoutOnCluster() {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String key = "cluster-blpop-" + ProcessHandle.current().pid();
		try {
			java.util.List<String> result = JedisUtils.LIST.blpop(key, 3);
			assertNull(result, "空列表 blpop(3) 应在约 3 秒后返回 null(读超时被 blocking 标记豁免)");
		} finally {
			try {
				cluster.del(key);
			} catch (Exception ignore) {
				// 清理尽力而为
			}
		}
	}

	/**
	 * 评审 P1-5: JedisClusterOperations.subscribe 是空实现只打日志, 调用方无感知。
	 * 走 JedisUtils.subscribe(封装层入口)验证: 修复后集群下订阅真实建立,
	 * publish 的消息能被收到(Redis 普通 pubsub 经集群总线在所有节点间转发)。
	 * 未建立订阅前 publish 会丢, 因此重试发布直到收到或超时。
	 */
	@Test
	public void testSubscribeOnCluster() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(clusterReachable, "集群不可达, 跳过");
		String channel = "cluster-sub-" + ProcessHandle.current().pid();
		java.util.concurrent.CompletableFuture<String> received = new java.util.concurrent.CompletableFuture<>();
		redis.clients.jedis.JedisPubSub pubSub = JedisUtils.subscribe(
				(ch, msg) -> received.complete(msg), channel);
		try {
			for (int i = 0; i < 10 && !received.isDone(); i++) {
				JedisUtils.publish(channel, "hello");
				TimeUnit.MILLISECONDS.sleep(300);
			}
			String msg = received.get(3, TimeUnit.SECONDS);
			assertEquals("hello", msg, "集群下应收到消息(修复前 subscribe 空实现永远收不到)");
		} finally {
			try {
				JedisUtils.unsubscribe(pubSub, channel);
			} catch (Exception ignore) {
				// 退订尽力而为
			}
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
