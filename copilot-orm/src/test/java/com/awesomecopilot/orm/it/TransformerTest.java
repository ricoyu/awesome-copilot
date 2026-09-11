package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.transformer.ResultTransformerFactory;
import com.awesomecopilot.orm.transformer.ValueHandlerResultTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结果集转换器(P2-11 c/e 修复回归):
 * <ul>
 * <li>(c) 工厂缓存 key 必须含 queryMode/enumLookupProperties——同一 SQL 同一 Bean,
 * 先 loose 后 strict 不能再拿回旧实例;</li>
 * <li>(e1) queryMode 传 "Loose"/"STRICT" 等混合大小写时, loose/strict 判断必须照常生效
 * (旧实现构造校验不敏感、运行时比较敏感, 传 "Loose" 会让缺列跳过逻辑失效 NPE);</li>
 * <li>(e2) 单参构造器旧实现不跑初始化, 属于"用了必炸"陷阱, 现在委托双参构造器正常工作。</li>
 * </ul>
 *
 * @author Rico Yu
 */
class TransformerTest {

	/** 缺列测试用 Bean: 只有 name 属性, select 出来的 orphan_col 没有对应 setter */
	public static class NameOnly {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Test
	@DisplayName("(c) 同 SQL+Bean 不同 queryMode: 工厂返回不同实例, 行为不串")
	void factoryKeyIncludesQueryMode() {
		ValueHandlerResultTransformer loose = ResultTransformerFactory
				.getResultTransformer("sql-c1", NameOnly.class, "loose");
		ValueHandlerResultTransformer strict = ResultTransformerFactory
				.getResultTransformer("sql-c1", NameOnly.class, "strict");
		assertNotSame(loose, strict, "queryMode 必须参与缓存 key, 否则 strict 调用拿回 loose 实例");

		// loose 实例: 缺列静默跳过
		NameOnly row = (NameOnly) loose.transformTuple(new Object[]{"rico", 42},
				new String[]{"name", "orphan_col"});
		assertEquals("rico", row.getName());

		// strict 实例: 缺列要抛 PropertyNotFoundException(jakarta.persistence)
		assertThrows(Exception.class, () -> strict.transformTuple(new Object[]{"rico", 42},
				new String[]{"name", "orphan_col"}), "strict 模式缺列应报错而不是静默丢数据");
	}

	@Test
	@DisplayName("(c) 同 SQL+Bean 不同 enumLookupProperties: 缓存 key 区分")
	void factoryKeyIncludesEnumLookupProperties() {
		ValueHandlerResultTransformer noEnumProps = ResultTransformerFactory
				.getResultTransformer("sql-c2", NameOnly.class, "loose", null);
		ValueHandlerResultTransformer withEnumProps = ResultTransformerFactory
				.getResultTransformer("sql-c2", NameOnly.class, "loose", Set.of("code"));
		assertNotSame(noEnumProps, withEnumProps, "enumLookupProperties 必须参与缓存 key");

		// 集合内容相同、顺序不同(HashSet vs LinkedHashSet)应命中同一个实例(TreeSet 归一化)
		ValueHandlerResultTransformer again = ResultTransformerFactory
				.getResultTransformer("sql-c2", NameOnly.class, "loose", Set.of("code"));
		assertSame(withEnumProps, again, "相同配置应命中同一缓存实例");
	}

	@Test
	@DisplayName("(e1) queryMode 传混合大小写 \"Loose\" 时, 缺列跳过逻辑照常生效")
	void mixedCaseQueryModeNormalized() {
		// 旧实现: 校验 equalsIgAny 放行 "Loose", 运行时 queryMode.equals("loose") 为 false
		// → 缺列不跳过 → null methodHandle.invoke NPE。现在构造入口统一转小写
		ValueHandlerResultTransformer t = new ValueHandlerResultTransformer(NameOnly.class, "Loose");
		assertDoesNotThrow(() -> {
			NameOnly row = (NameOnly) t.transformTuple(new Object[]{"rico", 42},
					new String[]{"name", "orphan_col"});
			assertEquals("rico", row.getName());
		}, "传 \"Loose\" 时 loose 语义必须生效, 不能因大小写悄悄变成报错路径");
	}

	@Test
	@DisplayName("(e2) 单参构造器可用: 初始化完整执行, 默认 loose")
	void singleArgConstructorWorks() {
		ValueHandlerResultTransformer t = new ValueHandlerResultTransformer(NameOnly.class);
		NameOnly row = (NameOnly) t.transformTuple(new Object[]{"rico"}, new String[]{"name"});
		assertEquals("rico", row.getName(),
				"旧单参构造器不跑 initializeTmp() 属于用了必炸的陷阱, 委托双参后必须正常工作");
	}

	@Test
	@DisplayName("(b) 多线程共用同一实例并发 transformTuple: 结果全部正确, 不崩不串")
	void concurrentTransformSameInstance() throws Exception {
		// 这个测试的场景就是工厂缓存的真实用法: 一条 SQL 一个实例, 多个查询线程同时往里灌数据
		ValueHandlerResultTransformer t = new ValueHandlerResultTransformer(NameOnly.class, "loose");
		int threads = 8;
		int rowsPerThread = 5000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger failures = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		for (int tid = 0; tid < threads; tid++) {
			final int seq = tid;
			futures.add(pool.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < rowsPerThread; i++) {
						// 带一个 Bean 没有的孤儿列, 顺带压 loose 跳过分支
						Object[] tuple = {"user-" + seq + "-" + i, 42};
						NameOnly row = (NameOnly) t.transformTuple(tuple, new String[]{"name", "orphan_col"});
						if (!("user-" + seq + "-" + i).equals(row.getName())) {
							failures.incrementAndGet();
						}
					}
				} catch (Throwable e) {
					failures.incrementAndGet();
				}
			}));
		}
		start.countDown();
		for (Future<?> f : futures) {
			f.get(30, TimeUnit.SECONDS);
		}
		pool.shutdown();
		assertEquals(0, failures.get(), threads + " 线程 x " + rowsPerThread + " 行并发转换应全部正确");
	}

	@Test
	@DisplayName("(b) 混合 null 列并发: 某列值有 null 有非 null 时, 逐列类型检查跨线程续查不出错")
	void concurrentTransformWithNullColumn() throws Exception {
		// 覆盖"某列一直是 null 时, 第二遍初始化要逐行续查"的慢路径:
		// 一半行该列为 null, 一半为 int, 8 线程乱序进来
		ValueHandlerResultTransformer t = new ValueHandlerResultTransformer(WithAge.class, "loose");
		int threads = 8;
		int rowsPerThread = 2000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger failures = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		for (int tid = 0; tid < threads; tid++) {
			final int seq = tid;
			futures.add(pool.submit(() -> {
				try {
					start.await();
					for (int i = 0; i < rowsPerThread; i++) {
						Object age = (i % 2 == 0) ? null : i; // 半 null 半 int
						WithAge row = (WithAge) t.transformTuple(
								new Object[]{"n" + seq, age}, new String[]{"name", "age"});
						if (!("n" + seq).equals(row.getName())
								|| (age != null && row.getAge() != ((Integer) age).intValue())) {
							failures.incrementAndGet();
						}
					}
				} catch (Throwable e) {
					failures.incrementAndGet();
				}
			}));
		}
		start.countDown();
		for (Future<?> f : futures) {
			f.get(30, TimeUnit.SECONDS);
		}
		pool.shutdown();
		assertEquals(0, failures.get(), "null/非null 混序并发下每行映射都应正确");
	}

	@Test
	@DisplayName("(d) resultClass 没有无参构造器: 创建 transformer 时立刻报清楚, 不是每行数据时才炸")
	void noNoArgConstructorFailsAtConstruction() {
		Exception e = assertThrows(Exception.class,
				() -> new ValueHandlerResultTransformer(NoNoArgCtor.class, "loose"));
		assertTrue(String.valueOf(e.getMessage()).contains("无参构造器"),
				"报错信息应点明缺少无参构造器, 实际: " + e.getMessage());
	}

	@Test
	@DisplayName("(d) Bean 无参构造器内部抛异常: 真实原因被带出来, 不再被旧 newInstance() 吞掉")
	void constructorExceptionSurfacesRealCause() {
		ValueHandlerResultTransformer t = new ValueHandlerResultTransformer(BrokenCtor.class, "loose");
		Exception e = assertThrows(Exception.class,
				() -> t.transformTuple(new Object[]{"x"}, new String[]{"name"}));
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		assertTrue(String.valueOf(cause.getMessage()).contains("构造器里故意炸"),
				"应看到构造器里的真实异常信息, 实际: " + cause.getMessage());
	}

	/** 没有无参构造器的 Bean */
	public static class NoNoArgCtor {
		private final String name;

		public NoNoArgCtor(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/** 无参构造器会抛异常的 Bean */
	public static class BrokenCtor {
		public BrokenCtor() {
			throw new IllegalStateException("构造器里故意炸");
		}
	}

	public static class WithAge {
		private String name;
		private int age; // 基本类型, DB 列 int

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
