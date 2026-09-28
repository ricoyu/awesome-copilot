package com.awesomecopilot.common.lang.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 2016-01-16 改用TransmittableThreadLocal
 * ThreadContext 提供了一种基于键值对把对象绑定到当前线程、以及解绑的机制。
 * <p/>
 * <p>内部使用一个 {@link HashMap} 来维护每个线程的键值对。</p>
 * <p/>
 * <p>如果希望确保绑定的数据在线程池等可复用线程环境中不跨线程共享，
 * 应用程序（或更可能是框架）必须在线程执行的开始处绑定所需的值、
 * 在结束处移除它们（即逐个显式操作，或通过 <tt>clear</tt> 方法一次性全部清除）。</p>
 *
 * @on
 * @see #remove()
 * @since 0.1
 */
public final class ThreadContext {
	/**
	 * 私有的内部日志实例。
	 */
	private static final Logger logger = LoggerFactory.getLogger(ThreadContext.class);

	private static final ThreadLocal<Map<Object, Object>> resources = new TransmittableThreadLocalMap<>();

	/**
	 * 默认的无参构造器。
	 */
	private ThreadContext() {
	}

	/**
	 * 返回 ThreadLocal 内部的 Map。该 Map 在内部通过把每个对象存放在唯一键下，
	 * 来实现对象与当前线程的绑定。
	 * <p/>
	 * P2-35: 返回的是当前绑定资源表的<b>拷贝</b>(空时返回不可变的 emptyMap), 不是内部
	 * 那张表本身——{@code getResources().put(k, v)} 这种写法编译通过, 但值写进的是拷贝,
	 * 真实绑定里读不到, 修改绑定请用 {@link #put(Object, Object)}。
	 *
	 * @return 已绑定资源 Map 的拷贝（没有任何绑定时返回不可变的空 Map）
	 */
	public static Map<Object, Object> getResources() {
		if (resources.get() == null) {
			return Collections.emptyMap();
		} else {
			return new HashMap<Object, Object>(resources.get());
		}
	}

	/**
	 * 允许调用方显式设置整个资源 Map。该操作会覆盖 ThreadContext 中此前存在的
	 * 全部内容——如果需要保留调用本方法前线程上已有的内容，请先调用
	 * {@link #getResources()} 获取现有状态。
	 * <p/>
	 * P2-35: 如实描述实现契约——入参为 null 或空 Map 时本方法是<b>空操作</b>,
	 * 不会清空已有绑定(javadoc 旧句"overwrites everything"在这两个入参下不成立)。
	 * 想清空当前线程全部绑定请用 {@link #remove()}。
	 *
	 * @param newResources 用于替换现有 {@link #getResources() resources} 的资源；
	 *            传入 null 或空 Map 时被忽略（空操作）。
	 * @since 1.0
	 */
	public static void setResources(Map<Object, Object> newResources) {
		if (newResources == null || newResources.isEmpty()) {
			return;
		}
		ensureResourcesInitialized();
		resources.get().clear();
		resources.get().putAll(newResources);
	}

	/**
	 * 返回 {@code ThreadContext} 中绑定在指定 {@code key} 下的值；
	 * 若该 {@code key} 没有绑定值则返回 {@code null}。
	 *
	 * @param key 用于查找值的 Map 键
	 * @return {@code ThreadContext} 中绑定在指定 {@code key} 下的值，
	 *         若该 {@code key} 没有绑定值则返回 {@code null}。
	 * @since 1.0
	 */
	private static Object getValue(Object key) {
		Map<Object, Object> perThreadResources = resources.get();
		return perThreadResources != null ? perThreadResources.get(key) : null;
	}

	private static void ensureResourcesInitialized() {
		if (resources.get() == null) {
			resources.set(new HashMap<Object, Object>());
		}
	}

	/**
	 * 返回绑定在当前线程上、指定 <code>key</code> 对应的对象。
	 *
	 * @param key 用于标识待返回值的键
	 * @return <code>key</code> 对应的对象；若指定 <code>key</code>
	 *         不存在绑定值则返回 <code>null</code>
	 */
	public static <T> T get(Object key) {
		if (logger.isTraceEnabled()) {
			String msg = "get() - in thread [" + Thread.currentThread().getName() + "]";
			logger.trace(msg);
		}

		Object value = getValue(key);
		if ((value != null) && logger.isTraceEnabled()) {
			String msg = "Retrieved value of type [" + value.getClass().getName() + "] for key [" +
					key + "] " + "bound to thread [" + Thread.currentThread().getName() + "]";
			logger.trace(msg);
		}
		return (T)value;
	}

	/**
     * 把 <tt>value</tt> 以给定 <code>key</code> 绑定到当前线程。
     * <p/>
     * <p><tt>null</tt> 的 <tt>value</tt> 与对给定 <tt>key</tt> 调用 <tt>remove</tt>
     * 效果相同，即：
     * <p/>
     * <pre>
     * if ( value == null ) {
     *     remove( key );
     * }</pre>
     *
     * @on
     * @param key   用于标识 <code>value</code> 的键。
     * @param value 要绑定到线程的值。
     * @throws IllegalArgumentException 如果 <code>key</code> 参数为 <tt>null</tt>。
     */
	public static void put(Object key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}

		if (value == null) {
			remove(key);
			return;
		}

		ensureResourcesInitialized();
		resources.get().put(key, value);

		if (logger.isTraceEnabled()) {
			String msg = "Bound value of type [" + value.getClass().getName() + "] for key [" +
					key + "] to thread " + "[" + Thread.currentThread().getName() + "]";
			logger.trace(msg);
		}
	}

	/**
	 * 从当前线程解绑给定 <code>key</code> 对应的值。
	 *
	 * @param key 标识绑定在当前线程上的值的键。
	 * @return 被解绑的对象；若指定 <tt>key</tt> 下没有绑定内容则返回 <tt>null</tt>。
	 */
	public static Object remove(Object key) {
		Map<Object, Object> perThreadResources = resources.get();
		Object value = perThreadResources != null ? perThreadResources.remove(key) : null;

		if ((value != null) && logger.isTraceEnabled()) {
			String msg = "Removed value of type [" + value.getClass().getName() + "] for key [" +
					key + "]" + "from thread [" + Thread.currentThread().getName() + "]";
			logger.trace(msg);
		}

		return value;
	}

	/**
	 * 从线程上移除底层的 {@link ThreadLocal ThreadLocal}
	 * （调用 {@link ThreadLocal#remove Remove}）。
	 * <p/> 本方法是在线程池环境中防止线程数据残留、互相干扰的最终"清理"操作，
	 * 应在线程执行结束时调用。
	 *
	 * @since 1.0
	 */
	public static void remove() {
		resources.remove();
	}

	private static final class TransmittableThreadLocalMap<T extends Map<Object, Object>>
			extends TransmittableThreadLocal<Map<Object, Object>> {

		/**
		 * 该实现是为了解决一个
		 * <a href="http://jsecurity.markmail.org/search/?q=#query:+page:1+mid:xqi2yxurwmrpqrvj+state:results">
		 * 用户报告的问题</a>。
		 * 
		 * @param parentValue 父线程的值，即 {@link #initialValue()} 方法所定义的 HashMap。
		 * @return 父线程派生的子线程要使用的 HashMap（父 HashMap 的一份拷贝）。
		 */
		@SuppressWarnings({ "unchecked" })
		protected Map<Object, Object> childValue(Map<Object, Object> parentValue) {
			if (parentValue != null) {
				return (Map<Object, Object>) ((HashMap<Object, Object>) parentValue).clone();
			} else {
				return null;
			}
		}
	}
}