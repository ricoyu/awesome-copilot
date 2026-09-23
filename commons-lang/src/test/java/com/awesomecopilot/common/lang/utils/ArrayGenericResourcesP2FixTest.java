package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-17 / P2-18 / P2-19 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>P2-17: ArrayUtils.nonNull 用 stream().toArray() 产出 Object[], 按 T[] 接收抛
 * ClassCastException(实测 "[Ljava.lang.Object; cannot be cast to [Ljava.lang.String;"),
 * 且一个叫 nonNull 的方法同时做了排序+去重(名字没有表达); 修复后按入参组件类型创建结果数组、
 * 只过滤 null、保持原顺序、不去重;</li>
 * <li>P2-18: GenericUtils.getTypeArgument 只看第一个泛型接口——实参是类型变量时
 * 内外层一起中断, 第二个接口不再看(实测 class X implements Supplier&lt;T&gt;,
 * Comparable&lt;String&gt; 返回 null); 嵌套泛型 Supplier&lt;Map&lt;K,V&gt;&gt;
 * 把 ParameterizedType 强转 Class 抛 ClassCastException。修复后: 遍历全部接口找到
 * 第一个"非类型变量/通配符"的实参; 实参本身是嵌套 ParameterizedType 时返回其 rawType;
 * 找不到返回 null;</li>
 * <li>P2-19: Resources.getResourcesFromDirectory 没判 File.listFiles() 的 null——
 * 权限不足/IO 错误时 NPE(该方法在 P2-16 的通配扫描路径上每次 classpath 扫描都会走)。
 * 修复后 null 直接按"此目录无文件"处理。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ArrayGenericResourcesP2FixTest {

	// ---------------- P2-17 ArrayUtils.nonNull ----------------

	@Test
	public void testNonNullResultIsActualStringArray() {
		//修复前实测: String[] a = ArrayUtils.nonNull("b","a","a") 抛
		//ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.String;
		assertThatCode(() -> {
			String[] a = ArrayUtils.nonNull("b", "a", "a");
			assertThat(a).isNotNull();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testNonNullOnlyFiltersNullsKeepsOrderNoDedup() {
		String[] result = ArrayUtils.nonNull("b", null, "a", "a", null);
		//修复前实测结果 [a, b]——sorted+distinct 藏在 nonNull 这个名字后面
		assertThat(result)
				.as("只过滤 null: 保持原顺序、不去重(要排序去重请用 stream 自行处理)")
				.containsExactly("b", "a", "a");
	}

	@Test
	public void testNonNullNullVarargsReturnsNull() {
		assertThat(ArrayUtils.nonNull((String[]) null)).isNull();
	}

	@Test
	public void testNonNullWorksWithLongArray() {
		//copilot-orm QueryUtils:193 的真实用法: Long[] 过滤 null 后塞进参数 Map
		Long[] result = ArrayUtils.nonNull(3L, null, 1L, 2L);
		assertThat(result).containsExactly(3L, 1L, 2L);
	}

	// ---------------- P2-18 GenericUtils.getTypeArgument ----------------

	static class StringSupplier implements Supplier<String> {
		@Override
		public String get() {
			return null;
		}
	}

	static class TypeVarFirstSupplier<T> implements Supplier<T>, Comparable<String> {
		@Override
		public int compareTo(String o) {
			return 0;
		}

		@Override
		public T get() {
			return null;
		}
	}

	static class MapSupplier implements Supplier<Map<String, Long>> {
		@Override
		public Map<String, Long> get() {
			return null;
		}
	}

	@Test
	public void testGetTypeArgumentSimpleInterface() {
		assertThat(GenericUtils.getTypeArgument(StringSupplier.class)).isEqualTo(String.class);
	}

	@Test
	public void testGetTypeArgumentSkipsTypeVariableInterface() {
		//修复前实测返回 null: 第一个接口 Supplier<T> 的实参是类型变量, 内外层一起中断
		Class<?> arg = GenericUtils.getTypeArgument(TypeVarFirstSupplier.class);
		assertThat(arg)
				.as("第二个接口 Comparable<String> 的实参 String 应被取到(修复前返回 null)")
				.isEqualTo(String.class);
	}

	@Test
	public void testGetTypeArgumentNestedGenericDoesNotThrow() {
		//修复前实测: Supplier<Map<String,Long>> 直接把 ParameterizedType 强转 Class
		//抛 ClassCastException; 修复后返回 rawType(Map.class)
		assertThat(GenericUtils.getTypeArgument(MapSupplier.class)).isEqualTo(Map.class);
	}

	@Test
	public void testGetTypeArgumentNoGenericInterfaceReturnsNull() {
		assertThat(GenericUtils.getTypeArgument(Object.class)).isNull();
	}

	// ---------------- P2-19 Resources listFiles null ----------------

	@Test
	public void testGetResourcesFromUnlistableDirectoryDoesNotThrowNpe(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
		//用 icacls 拒绝当前用户对目录的读取权限, 制造 isDirectory()==true 而
		//listFiles()==null 的真实条件(修复前该条件下 Resources 遍历 null 直接 NPE)
		String user = System.getenv("USERNAME");
		org.junit.jupiter.api.Assumptions.assumeTrue(user != null, "环境缺少 USERNAME 变量, 无法构造权限拒绝");
		File locked = dir.resolve("locked").toFile();
		assertThat(locked.mkdirs()).isTrue();

		Process deny = new ProcessBuilder("cmd.exe", "/c",
				"icacls", locked.getAbsolutePath(), "/deny", user + ":(OI)(CI)(RX)")
				.redirectErrorStream(true).start();
		int code = deny.waitFor();
		org.junit.jupiter.api.Assumptions.assumeTrue(code == 0 && locked.listFiles() == null,
				"icacls 拒绝读取未生效(域策略?), 跳过");
		try {
			String saved = System.getProperty("java.class.path");
			try {
				System.setProperty("java.class.path", locked.getAbsolutePath());
				assertThatCode(() -> Resources.getResources(Pattern.compile(".*")))
						.as("目录列不出来(listFiles()==null)时应按空处理, 修复前实测 NPE")
						.doesNotThrowAnyException();
				assertThatCode(() -> Resources.getResources("anything.txt"))
						.doesNotThrowAnyException();
			} finally {
				System.setProperty("java.class.path", saved);
			}
		} finally {
			new ProcessBuilder("cmd.exe", "/c", "icacls", locked.getAbsolutePath(), "/reset")
					.redirectErrorStream(true).start().waitFor();
		}
	}
}
