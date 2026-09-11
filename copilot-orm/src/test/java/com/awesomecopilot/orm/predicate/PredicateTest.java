package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * predicate 包回归测试（对应分析报告 P1-6 七连 bug）。
 * <p>
 * 手法：mock CriteriaBuilder/Root/Path，断言各 Predicate 到底调了 which 比较方法、
 * 参数是什么——无需数据库。null 值统一语义见 StringPredicate 类注释：
 * EQ+null→IS NULL, NOTEQ+null→IS NOT NULL, 其余模式+null→快速失败抛异常
 * （绝不允许返回 null 谓词——它会以 NPE 的形态在 where() 处爆，离病因太远）。
 * <p>
 * Copyright: Copyright (c) 2026-09-11
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PredicateTest {

	private CriteriaBuilder cb;
	private Root root;
	private Path path;

	@BeforeEach
	void setUp() {
		cb = mock(CriteriaBuilder.class);
		root = mock(Root.class);
		path = mock(Path.class);
		when(root.get("prop")).thenReturn(path);
	}

	// ---------------------------------------------------------------- 1. ANYWHERE

	@Nested
	class StringAnywhere {

		/**
		 * bug#1: String.join 的参数是【分隔符】, 原写法 join("%", v, "%") 产出 "v%%"
		 * (前缀匹配), 期望是 "%v%"(包含匹配)。
		 */
		@Test
		void anywhereProducesBothSidePercent() {
			new StringPredicate("prop", "abc", CompareMode.ANYWHERE).toPredicate(cb, root);
			verify(cb).like(path, "%abc%");
		}

		/**
		 * bug#1 附带: value 为 null 时旧代码经 String.join 拼出字面量 "%null%%" 静默错查。
		 */
		@Test
		void anywhereWithNullValueFailsFast() {
			assertThrows(IllegalArgumentException.class, () ->
					new StringPredicate("prop", null, CompareMode.ANYWHERE).toPredicate(cb, root));
		}
	}

	// ------------------------------------------------- 2. GT/GE/LT/LE 分支补齐 + null 快速失败

	@Nested
	class StringCompareModes {

		@Test
		void gtGeLtLeAreSupported() {
			new StringPredicate("prop", "a", CompareMode.GT).toPredicate(cb, root);
			verify(cb).greaterThan(path, "a");
			new StringPredicate("prop", "a", CompareMode.GE).toPredicate(cb, root);
			verify(cb).greaterThanOrEqualTo(path, "a");
			new StringPredicate("prop", "a", CompareMode.LT).toPredicate(cb, root);
			verify(cb).lessThan(path, "a");
			new StringPredicate("prop", "a", CompareMode.LE).toPredicate(cb, root);
			verify(cb).lessThanOrEqualTo(path, "a");
		}

		/**
		 * bug: StringPredicate 收到 GT/GE/... 时 switch 无对应 case 且无 default,
		 * 返回 null 谓词 → 调用方 where() 处 NPE。期望: 构造比较失败时抛可读异常。
		 * 现成触发路径: Querys.gt("name", null) 会 new StringPredicate(name, null, GT)。
		 */
		@Test
		void gtWithNullValueFailsFastNotReturnNull() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
					new StringPredicate("prop", null, CompareMode.GT).toPredicate(cb, root));
			System.out.println("fail-fast msg: " + e.getMessage());
		}

		@Test
		void eqNullStillMeansIsNull() {
			new StringPredicate("prop", null).toPredicate(cb, root);
			verify(cb).isNull(path);
			verify(cb, never()).isNotNull(path);
		}
	}

	// --------------------------------------------------------------- 3. int/long EQ+null

	/**
	 * bug#2: IntegerPredicate/LongPredicate 的 EQ+null 分支写成 isNotNull ——
	 * 与 String/Boolean 版(isNull)相反，"等于null"查成了"非空"。期望统一为 IS NULL。
	 */
	@Nested
	class IntLongEqNull {

		@Test
		void integerEqNullIsNullNotIsNotNull() {
			new IntegerPredicate("prop", null).toPredicate(cb, root);
			verify(cb).isNull(path);
			verify(cb, never()).isNotNull(path);
		}

		@Test
		void longEqNullIsNullNotIsNotNull() {
			new LongPredicate("prop", null).toPredicate(cb, root);
			verify(cb).isNull(path);
			verify(cb, never()).isNotNull(path);
		}

		@Test
		void intLongCompareBranchesStillWork() {
			new IntegerPredicate("prop", 5, CompareMode.GT).toPredicate(cb, root);
			verify(cb).gt(path, 5);
			new IntegerPredicate("prop", 5, CompareMode.LE).toPredicate(cb, root);
			verify(cb).lessThanOrEqualTo(path, 5);
			new LongPredicate("prop", 5L, CompareMode.GE).toPredicate(cb, root);
			verify(cb).ge(path, 5L);
			new LongPredicate("prop", 5L, CompareMode.LT).toPredicate(cb, root);
			verify(cb).lessThan(path, 5L);
			// NOTEQ+null → IS NOT NULL (取反语义)
			new IntegerPredicate("prop", null, CompareMode.NOTEQ).toPredicate(cb, root);
			verify(cb).isNotNull(path);
		}
	}

	// --------------------------------------------------- 4. LocalDatePredicate 三参构造器

	/**
	 * bug#3: new LocalDatePredicate(prop, begin, end) 意图 BETWEEN, 但 matchMode 不置
	 * 保持默认 EXACT → 实际生成 col = begin，end 条件整个丢失。期望按参数推导模式
	 * （对齐 LocalDateTimePredicate 的既有做法）。
	 */
	@Nested
	class LocalDatePredicateTest {

		@Test
		void twoArgConstructorGeneratesBetween() {
			LocalDate begin = LocalDate.of(2026, 1, 1);
			LocalDate end = LocalDate.of(2026, 12, 31);
			new LocalDatePredicate("prop", begin, end).toPredicate(cb, root);
			verify(cb).between(path, begin, end);
			verify(cb, never()).equal(path, begin);
		}

		/**
		 * bug#4: case LATER_THAN 缺 break, 贯穿进 EARLIER_THAN_OR_SAME 覆盖 predicate,
		 * 请求">"实际生成"<="。
		 */
		@Test
		void laterThanSingleValueIsGreaterThan() {
			LocalDate d = LocalDate.of(2026, 6, 1);
			new LocalDatePredicate("prop", d, DateMatchMode.LATER_THAN).toPredicate(cb, root);
			verify(cb).greaterThan(path, d);
			verify(cb, never()).lessThanOrEqualTo(path, d);
		}

		@Test
		void earlierThanSingleValueIsLessThan() {
			LocalDate d = LocalDate.of(2026, 6, 1);
			new LocalDatePredicate("prop", d, DateMatchMode.EARLIER_THAN).toPredicate(cb, root);
			verify(cb).lessThan(path, d);
		}

		/**
		 * bug#5: 子类各自 private matchMode 字段遮蔽父类同名字段, 继承的 setMatchMode()
		 * 写的是父类字段, toPredicate() 读的是子类字段 → set 后行为不变。
		 * 期望 setMatchMode 能真正切换模式。
		 */
		@Test
		void setMatchModeTakesEffect() {
			LocalDate begin = LocalDate.of(2026, 1, 1);
			LocalDate end = LocalDate.of(2026, 12, 31);
			LocalDatePredicate p = new LocalDatePredicate("prop", begin, end);
			p.setMatchMode(DateMatchMode.EXACT);
			p.toPredicate(cb, root);
			verify(cb).equal(path, begin);
		}
	}

	// ------------------------------------------------------------- 5. SingleDatePredicate

	@Nested
	class SingleDatePredicateTest {
		@Test
		void laterThanOrSameIsGreaterThanOrEqualTo() {
			java.util.Date d = new java.util.Date();
			SingleDatePredicate p = new SingleDatePredicate("prop", d, DateMatchMode.LATER_THAN_OR_SAME);
			p.toPredicate(cb, root);
			verify(cb).greaterThanOrEqualTo(path, d);
		}

		@Test
		void laterThanIsGreaterThan() {
			java.util.Date d = new java.util.Date();
			SingleDatePredicate p = new SingleDatePredicate("prop", d, DateMatchMode.LATER_THAN);
			p.toPredicate(cb, root);
			verify(cb).greaterThan(path, d);
			verify(cb, never()).greaterThanOrEqualTo(path, d);
		}
	}

	// --------------------------------------------------------------- 6. InPredicate

	/**
	 * bug#6: 标量值、空集合、不支持的数组类型(Boolean[]/LocalDate[]...)静默生成
	 * "零个值的 IN" —— SQL 层要么语法错误要么恒假, NOT IN 时更是灾难。
	 * 期望: 这三种情况都抛 IllegalArgumentException 并点名属性。
	 */
	@Nested
	class InPredicateTest {

		@Test
		void scalarValueFailsFast() {
			assertThrows(IllegalArgumentException.class, () ->
					new InPredicate("prop", "bob").toPredicate(cb, root));
		}

		@Test
		void emptyCollectionFailsFast() {
			assertThrows(IllegalArgumentException.class, () ->
					new InPredicate("prop", java.util.List.of()).toPredicate(cb, root));
		}

		/**
		 * 改进: 数组分支重写后按组件类型反射取值, Boolean[]/LocalDate[] 这类
		 * 旧 9-case switch 未覆盖、曾静默产出空 IN 的数组现在被真正支持。
		 */
		@Test
		void booleanArrayNowSupported() {
			CriteriaBuilder.In in = mock(CriteriaBuilder.In.class);
			when(cb.in(path)).thenReturn(in);
			new InPredicate("prop", new Boolean[]{true, false}).toPredicate(cb, root);
			verify(in).value(true);
			verify(in).value(false);
		}

		/**
		 * null 值: 旧实现返回 null 谓词(where() 处 NPE), 现在点名抛异常。
		 */
		@Test
		void nullValueFailsFast() {
			assertThrows(IllegalArgumentException.class, () ->
					new InPredicate("prop", null).toPredicate(cb, root));
		}

		@Test
		void supportedTypesStillWork() {
			CriteriaBuilder.In in = mock(CriteriaBuilder.In.class);
			when(cb.in(path)).thenReturn(in);
			new InPredicate("prop", java.util.List.of(1, 2)).toPredicate(cb, root);
			verify(in).value(1);
			verify(in).value(2);
		}
	}

	// ------------------------------------------------------------- 7. Querys.gt 降级

	/**
	 * bug#7: Querys.gt 对未覆盖类型(Double/BigDecimal/LocalDateTime...)落进
	 * basicPredicate(只支持EQ/NE) —— 请求">"生成"=", 静默错查。期望快速失败。
	 */
	@Nested
	class QuerysGtTest {

		@Test
		void gtWithUnsupportedTypeFailsFast() {
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
					Querys.gt("amount", 10.5));
			System.out.println("Querys.gt fail-fast msg: " + e.getMessage());
		}

		@Test
		void gtSupportedTypesStillWork() {
			Querys.gt("age", 18);       // Integer
			Querys.gt("name", "abc");   // String
			Querys.gt("day", LocalDate.now());
		}
	}
}
