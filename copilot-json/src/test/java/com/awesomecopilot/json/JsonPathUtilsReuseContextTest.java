package com.awesomecopilot.json;

import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.json.jsonpath.context.DocumentContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonPathUtils "解析一次、读多次" API 的回归测试。
 * <p>
 * 背景（2026-09-26 高并发复检测量）：旧的 readNode(String json, ...) 入口每次调用都
 * 把整篇文档从头解析一遍——40KB 文档单次实测 466µs，其中路径求值只占 0.2µs；
 * 同一份文档读 5 个字段的公共入口实测 2.36ms/组，复用已解析的上下文只要 0.48ms/组（5.0 倍差距）。
 * 本测试锁定新 API 的行为：parse 一次后多次读取结果与旧入口逐项一致。
 */
public class JsonPathUtilsReuseContextTest {

	private static final String DOC = """
			{
			  "meta": {"version": "v2", "count": 3},
			  "items": [
			    {"id": 1, "name": "苹果", "note": "he said \\"hi\\"", "billJson": "{\\"FBillNo\\":\\"XSCK001\\"}"},
			    {"id": 2, "name": "香蕉"},
			    {"id": 3, "name": "橘子", "price": 5.5}
			  ]
			}""";

	@Test
	public void testParsedContextReadMatchesPerCallApi() {
		DocumentContext ctx = JsonPathUtils.parse(DOC);
		assertThat(ctx).isNotNull();

		// 与旧入口逐项对账: 标量、嵌套、通配列表、带类型转换
		assertThat((Object) JsonPathUtils.readNode(ctx, "$.meta.version")).isEqualTo("v2");
		assertThat((Object) JsonPathUtils.readNode(ctx, "$.meta.count")).isEqualTo(
				JsonPathUtils.readNode(DOC, "$.meta.count"));
		assertThat((Object) JsonPathUtils.readNode(ctx, "$.items[*].name")).isEqualTo(
				JsonPathUtils.readNode(DOC, "$.items[*].name"));
		assertThat(JsonPathUtils.<String>readNode(ctx, "$.items[0].name")).isEqualTo("苹果");
		assertThat(JsonPathUtils.<Integer>readNode(ctx, "$.meta.count", Integer.class)).isEqualTo(3);
		assertThat(JsonPathUtils.readNode(ctx, "$.items[2].price", Double.class)).isEqualTo(5.5);
	}

	@Test
	public void testParsedContextKeepsEmbeddedJsonUnwrapAndEscapedQuotes() {
		DocumentContext ctx = JsonPathUtils.parse(DOC);

		// 默认 parse 保留内嵌JSON树级展开能力: $.billJson.FBillNo 直接穿透
		assertThat(JsonPathUtils.<String>readNode(ctx, "$.items[0].billJson.FBillNo")).isEqualTo("XSCK001");
		// 转义双引号的普通字符串值不被破坏 (P0-2 修复成果的延续断言)
		assertThat(JsonPathUtils.<String>readNode(ctx, "$.items[0].note")).isEqualTo("he said \"hi\"");

		// ifExists / readNodeIfExists / readNodeSingleValue / readListNode 的 ctx 重载行为与旧入口一致
		assertThat(JsonPathUtils.ifExists(ctx, "$.items[0].billJson.FBillNo")).isTrue();
		assertThat(JsonPathUtils.<Object>readNodeIfExists(ctx, "$.items[1].price")).isNull();
		assertThat(JsonPathUtils.<String>readNodeSingleValue(ctx, "$.items[*].name")).isEqualTo("苹果");
		assertThat(JsonPathUtils.readListNode(ctx, "$.items[*].name")).isEqualTo(
				JsonPathUtils.readListNode(DOC, "$.items[*].name"));
	}

	@Test
	public void testParseWithoutUnwrapSkipsEmbeddedJsonExpansion() {
		// 第二个参数 false = 跳过内嵌JSON展开: 高频纯标量读取省掉整树遍历的开销,
		// billJson 保持原始字符串, 普通字段与转义引号不受影响
		DocumentContext strict = JsonPathUtils.parse(DOC, false);
		assertThat(strict).isNotNull();
		assertThat(JsonPathUtils.<String>readNode(strict, "$.items[0].name")).isEqualTo("苹果");
		assertThat(JsonPathUtils.<String>readNode(strict, "$.items[0].note")).isEqualTo("he said \"hi\"");
		Object raw = JsonPathUtils.readNode(strict, "$.items[0].billJson");
		assertThat(raw).isInstanceOf(String.class);
		assertThat((String) raw).isEqualTo("{\"FBillNo\":\"XSCK001\"}");
		// 未展开时 $.billJson.FBillNo 读不到, 旧入口(默认展开)能读到——差异是本参数定义的行为
		assertThat(JsonPathUtils.<Object>readNode(strict, "$.items[0].billJson.FBillNo")).isNull();
		assertThat(JsonPathUtils.<String>readNode(JsonPathUtils.parse(DOC), "$.items[0].billJson.FBillNo"))
				.isEqualTo("XSCK001");
	}

	@Test
	public void testParseReturnsNullOnInvalidJson() {
		assertThat(JsonPathUtils.parse(null)).isNull();
		assertThat(JsonPathUtils.parse("  ")).isNull();
		assertThat(JsonPathUtils.parse("}{ not json }{", false)).isNull();
	}

	@Test
	public void testSharedContextSafeAcrossThreads() throws InterruptedException {
		// 解析产物是只读共享的树: 多线程并发通过 ctx 读不同路径, 结果必须与单线程一致
		DocumentContext ctx = JsonPathUtils.parse(DOC);
		List<String> paths = List.of("$.meta.version", "$.items[0].name", "$.items[*].name",
				"$.items[2].price", "$.items[0].billJson.FBillNo");
		List<String> expected = new ArrayList<>();
		for (String p : paths) expected.add(String.valueOf(JsonPathUtils.<Object>readNode(ctx, p)));

		int threads = 16, iterations = 2000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicLong wrong = new AtomicLong();
		for (int t = 0; t < threads; t++) {
			pool.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < iterations; i++) {
						for (int p = 0; p < paths.size(); p++) {
							Object r = JsonPathUtils.readNode(ctx, paths.get(p));
							if (!expected.get(p).equals(String.valueOf(r))) wrong.incrementAndGet();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
		pool.shutdown();
		assertThat(wrong.get()).isZero();
	}

	@Test
	public void testLegacyStringApiStillWorks() {
		// 旧入口保持原语义(内部改为委托 parse), 全部既有调用点不受影响
		assertThat(JsonPathUtils.<String>readNode(DOC, "$.meta.version")).isEqualTo("v2");
		assertThat(JsonPathUtils.<String>readNode(DOC, "$.items[0].billJson.FBillNo")).isEqualTo("XSCK001");
	}

	@Test
	public void testReadNodeDoubleTargetAcrossTokenTypes() {
		// 评审E2(2026-09-26): 目标类型 Double/double 的成败不能取决于 JSON 里的数字写法。
		// 修复前实测: 5.5(Double节点)+Double.class=有值; 5(Integer节点)+Double.class=null;
		// double.class 两种输入都 null——四条路径三种结果。
		String doc = "{\"p1\":5.5,\"p2\":5}";
		assertThat(JsonPathUtils.<Double>readNode(doc, "$.p1", Double.class)).isEqualTo(5.5);
		assertThat(JsonPathUtils.<Double>readNode(doc, "$.p2", Double.class)).isEqualTo(5.0);
		assertThat(JsonPathUtils.<Double>readNode(doc, "$.p1", double.class)).isEqualTo(5.5);
		assertThat(JsonPathUtils.<Double>readNode(doc, "$.p2", double.class)).isEqualTo(5.0);
		assertThat(JsonPathUtils.<Integer>readNode(doc, "$.p1", Integer.class)).isEqualTo(5);
	}

	@Test
	public void testFunctionPathsCompiledPerCallNotShared() throws InterruptedException {
		// 评审R2-E1(2026-09-26): json-path 3.0.0 的函数路径(如 sum()/max())编译产物在求值时
		// 会把参数写进共享 token (FunctionPathToken 写 Parameter.lateBinding/evaluated, 非 volatile),
		// 同一 path 跨报文并发读实测 48 万次串值 10.4 万次。JsonPathCache 对含 '(' 的 path
		// 每次重新 compile 规避; 本用例断言含函数表达式的读取结果正确(串值缺陷的正向对照)。
		String small = "{\"nums\":[1,2,3]}";
		String big = "{\"nums\":[10,20,30,40]}";
		// 注: sum(param) 的语义是"当前数组与参数数组合并求和"(实测 $.nums.sum($.nums)=两倍),
		// 这里用无参形式, 结果只依赖各自报文
		assertThat(JsonPathUtils.<Double>readNode(small, "$.nums.sum()")).isEqualTo(6.0);
		assertThat(JsonPathUtils.<Double>readNode(big, "$.nums.sum()")).isEqualTo(100.0);
		assertThat(JsonPathUtils.<Double>readNode(small, "$.nums.max()")).isEqualTo(3.0);
		assertThat(JsonPathUtils.<Double>readNode(big, "$.nums.max()")).isEqualTo(40.0);
		assertThat(((Number) JsonPathUtils.readNode(small, "$.nums.length()")).intValue()).isEqualTo(3);

		// 评审R2-E1 的回归形态: 8线程 × 两份报文交替读同一函数路径, 若缓存了共享编译产物,
		// 实测(修复前) 48万次会有约22%拿到另一份报文的参数值且无异常。含'('的 path 现在
		// 每次重新 compile(JsonPathCache), 本用例断言 0 串值。
		int threads = 8, iterations = 20_000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicLong wrong = new AtomicLong();
		for (int t = 0; t < threads; t++) {
			final boolean useSmall = t % 2 == 0;
			pool.submit(() -> {
				try {
					start.await();
					String doc = useSmall ? small : big;
					double expectSum = useSmall ? 6.0 : 100.0;
					for (int i = 0; i < iterations; i++) {
						Double r = JsonPathUtils.readNode(doc, "$.nums.sum()");
						if (r == null || r != expectSum) wrong.incrementAndGet();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
		pool.shutdown();
		assertThat(wrong.get()).isZero();
	}

	@Test
	public void testReadNodeWithPathWhitespaceConsistent() {
		// 评审R2-E3: 带首尾空白的 path 在 JsonPathUtils 与 JsonContext 两个入口必须行为一致(都能读到)。
		// 修复前实测: ctx.read("  $.meta.count  ") 返回 3, 而 JsonPathUtils.readNode(ctx, 同串) 返回 null。
		DocumentContext ctx = JsonPathUtils.parse(DOC);
		assertThat(JsonPathUtils.<Object>readNode(ctx, "  $.meta.version  ")).isEqualTo("v2");
		assertThat((Object) ctx.read("  $.meta.version  ")).isEqualTo("v2");
	}

	@Test
	public void testReadNodeWithCollectionTargetClass() {
		// readNode(json, path, Map.class) 这类"目标类型不是标量"的读取:
		// 旧实现里 ValueHandlerFactory.determineAppropriateHandler 对 Map 返回 null,
		// 接着 valueHandler.convert(...) 发生空指针, 被 catch 捕获, 永远返回 null 且无提示 (2026-09-26 复检测量发现的既有缺陷)。
		// 修复后 mappingProvider 的转换结果直接返回。
		Map<String, Object> m = JsonPathUtils.readNode(DOC, "$.meta", Map.class);
		assertThat(m).containsEntry("version", "v2");
		assertThat(JsonPathUtils.<List>readNode(DOC, "$.items[*].name", List.class).size()).isEqualTo(3);
	}
}
