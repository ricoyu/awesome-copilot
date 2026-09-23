package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-28 ReflectionUtils / ClassUtils 簇回归测试
 * （CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>P2-28-1 getDeclaredFields/getDeclaredMethods 把缓存数组本体交给调用方:
 * 修复前实测两次调用返回同一实例, 任一处 a[0]=null 之后全 JVM 后续调用拿到的
 * 字段/方法列表就是坏的。修复后返回副本, 调用方随便改不影响缓存;</li>
 * <li>P2-28-2 getFieldValue(String, Class) 对实例字段执行 field.get(null),
 * 修复前实测抛一条没有任何消息的 NullPointerException, 看不出"字段不是静态的";
 * 修复后抛带字段名与提示的 IllegalArgumentException;</li>
 * <li>P2-28-3 findFieldRelaxable 的 Assert 允许 name 为 null(按类型查找),
 * 但下一行直接 matcher(name) —— 修复前实测抛 Pattern 内部 NPE,
 * 循环里 name == null 的判断是永远走不到的死代码;</li>
 * <li>P2-28-4 invokeStatic 按实参运行时类型精确 getMethod —— 修复前实测
 * invokeStatic("sizeOfCollection", X.class, new ArrayList&lt;&gt;()) 报
 * NoSuchMethod(方法声明参数是 Collection, 明明可接收)。修复后精确匹配失败
 * 时回退到可赋值性查找;</li>
 * <li>P2-28-5 ClassUtils.interfaceMethodCache 全文件只有 put 没有读取点,
 * 缓存纯占内存 —— 删除缓存。行为零变化, 本用例守住删除后
 * getInterfaceMethodIfPossible 的接口方法解析仍然正确。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ReflectionUtilsP2FixTest {

	public static class Sample {
		public static String staticGreeting = "hello";
		private String instanceField = "iv";
		private int count = 3;

		public static int sizeOfCollection(Collection<?> c) {
			return c.size();
		}

		public static int answer() {
			return 42;
		}

		public String greet() {
			return staticGreeting;
		}
	}

	public interface Greeter {
		String greet();
	}

	public static class GreeterImpl implements Greeter {
		@Override
		public String greet() {
			return "hi";
		}
	}

	// ---------------- P2-28-1 缓存数组不得外流 ----------------

	@Test
	public void testGetDeclaredFieldsReturnsCopyNotSharedCache() {
		Field[] first = ReflectionUtils.getDeclaredFields(Sample.class);
		first[0] = null; //模拟调用方把返回数组改坏
		Field[] second = ReflectionUtils.getDeclaredFields(Sample.class);
		//修复前实测: second 与 first 是同一数组, 上面写入的 null 原样带回来
		assertThat(second)
				.as("上一次调用方改坏数组不能污染本次结果(修复前实测返回同一实例)")
				.doesNotContainNull();
		assertThat(Arrays.asList(second)).extracting(Field::getName)
				.containsExactlyInAnyOrder("staticGreeting", "instanceField", "count");
	}

	@Test
	public void testGetDeclaredMethodsReturnsCopyNotSharedCache() {
		Method[] first = ReflectionUtils.getDeclaredMethods(Sample.class);
		first[0] = null;
		Method[] second = ReflectionUtils.getDeclaredMethods(Sample.class);
		assertThat(second)
				.as("方法数组同样不得把缓存本体交出去(修复前实测同一实例)")
				.doesNotContainNull();
		assertThat(Arrays.asList(second)).extracting(Method::getName)
				.contains("sizeOfCollection", "greet");
	}

	// ---------------- P2-28-2 实例字段读静态值要给明确报错 ----------------

	@Test
	public void testGetFieldValueOfInstanceFieldNamesProblem() {
		//修复前实测: field.get(null) 抛一条无消息 NPE, 完全看不出原因
		assertThatThrownBy(() -> ReflectionUtils.getFieldValue("instanceField", Sample.class))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("instanceField");
	}

	@Test
	public void testGetFieldValueOfStaticFieldStillWorks() {
		assertThat(ReflectionUtils.getFieldValue("staticGreeting", Sample.class)).isEqualTo("hello");
	}

	// ---------------- P2-28-3 按类型查找(name=null)不再崩 ----------------

	@Test
	public void testFindFieldRelaxableWithNullNameSearchesByType() {
		//修复前实测: Assert 允许 name==null, 但紧接着 flaxableNamePattern.matcher(name) NPE
		Field f = ReflectionUtils.findFieldRelaxable(Sample.class, null, int.class);
		assertThat(f)
				.as("按类型查找(不传 name)应能找到 int count(修复前实测抛 NPE)")
				.isNotNull();
		assertThat(f.getName()).isEqualTo("count");
	}

	@Test
	public void testFindFieldRelaxableByNameStillWorks() {
		//带 name 时"放宽匹配"(忽略 -_ 空格)的原行为保持
		assertThat(ReflectionUtils.findFieldRelaxable(Sample.class, "instance-field", String.class))
				.as("连字符写法应能匹配 instanceField")
				.isNotNull();
	}

	// ---------------- P2-28-4 invokeStatic 可赋值回退 ----------------

	@Test
	public void testInvokeStaticFallsBackToAssignableSignature() {
		//修复前实测: getMethod("sizeOfCollection", ArrayList.class) 精确匹配失败,
		//报 NoSuchMethod——而方法参数声明为 Collection, 实参明明能接收
		List<String> args = new ArrayList<>(Arrays.asList("a", "b", "c"));
		Object result = ReflectionUtils.invokeStatic("sizeOfCollection", Sample.class, args);
		assertThat(result).isEqualTo(3);
	}

	@Test
	public void testInvokeStaticExactMatchStillWorks() {
		//无参静态方法(精确匹配本来就命中)行为不变
		Object r = ReflectionUtils.invokeStatic("answer", Sample.class);
		assertThat(r).isEqualTo(42);
	}

	// ---------------- P2-28-5 删除只写不读缓存后, 解析行为不变 ----------------

	@Test
	public void testInterfaceMethodResolutionUnchangedAfterCacheRemoval() throws Exception {
		Method impl = GreeterImpl.class.getMethod("greet");
		Method resolved = ClassUtils.getInterfaceMethodIfPossible(impl, null);
		assertThat(resolved.getDeclaringClass())
				.as("实现类方法应被解析为接口方法(修复前后行为一致)")
				.isEqualTo(Greeter.class);
	}
}
