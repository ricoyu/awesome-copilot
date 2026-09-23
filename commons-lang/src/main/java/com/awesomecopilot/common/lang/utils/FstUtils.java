package com.awesomecopilot.common.lang.utils;

import org.nustaq.serialization.FSTConfiguration;

/**
 * 基于Fst实现的基于字节码的序列化/反序列化工具, 传统Java序列化/反序列化的替代方案
 * 限制: 对象必须实现Serializble接口
 * 优势: 对象不需要有默认构造函数
 * 
 * https://github.com/RuedigerMoeller/fast-serialization/wiki
 * 
 * 运行前提: FST 2.x 注册默认类时要反射 java.lang/java.math/java.util 等大量 JDK 内部类,
 * JDK 17+ 默认禁止该反射, 需为 JVM 添加一组 --add-opens 启动参数(完整清单见初始化失败时的异常文案)
 * <p>
 * Copyright: (C), 2021-01-17 20:48
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class FstUtils {
	
	/**
	 * P1-8: 原来在静态字段初始化里直接 createDefaultConfiguration(), JDK 17+ 未加 --add-opens 时
	 * 首次触碰本类即抛 ExceptionInInitializerError(根因 InaccessibleObjectException),
	 * 整个类报废且调用方只拿到一个没有指引的 Error。
	 * 改为懒初始化并捕获初始化异常: 失败时抛带 --add-opens 指引的 IllegalStateException,
	 * 且只尝试一次, 之后每次调用快速抛同一个异常。
	 */
	private static volatile FSTConfiguration fst;
	
	private static volatile IllegalStateException initError;
	
	/**
	 * 评审实测(2026-09-23): 只加 java.base/java.lang 一个 opens 不够, createDefaultConfiguration()
	 * 会依次卡在 java.math(BigDecimal.intVal)→java.util(TreeMap.comparator)→java.util.concurrent
	 * (ConcurrentHashMap.writeObject)→java.lang.reflect→java.net(URL.protocol) 上逐个报
	 * InaccessibleObjectException, 一次性给全下面这组才初始化成功。
	 */
	private static final String ADD_OPENS_GUIDE =
			"--add-opens java.base/java.lang=ALL-UNNAMED"
					+ " --add-opens java.base/java.math=ALL-UNNAMED"
					+ " --add-opens java.base/java.util=ALL-UNNAMED"
					+ " --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
					+ " --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED"
					+ " --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
					+ " --add-opens java.base/java.net=ALL-UNNAMED"
					+ " --add-opens java.base/java.io=ALL-UNNAMED"
					+ " --add-opens java.base/java.time=ALL-UNNAMED"
					+ " --add-opens java.base/java.text=ALL-UNNAMED"
					+ " --add-opens java.base/java.util.function=ALL-UNNAMED";
	
	private static FSTConfiguration configuration() {
		if (initError != null) {
			throw initError;
		}
		FSTConfiguration local = fst;
		if (local == null) {
			synchronized (FstUtils.class) {
				if (initError != null) {
					throw initError;
				}
				local = fst;
				if (local == null) {
					try {
						local = FSTConfiguration.createDefaultConfiguration();
						fst = local;
					} catch (Throwable t) { //含 InaccessibleObjectException / NoClassDefFoundError
						initError = new IllegalStateException(
								"FST 初始化失败: JDK 17+ 需要为 JVM 添加一组 --add-opens 启动参数(只开 java.base/java.lang 不够): "
										+ ADD_OPENS_GUIDE
										+ " (或升级 FST 分支/更换序列化方案)。原始异常: " + t, t);
						throw initError;
					}
				}
			}
		}
		return local;
	}
	
	/**
	 * 将对象序列化为byte[]
	 * 限制: 对象必须实现Serializble接口
	 * 
	 * @param obj
	 * @return byte[]
	 */
	public static byte[] toBytes(Object obj) {
		if (obj == null) {
			return null;
		}
		
		return configuration().asByteArray(obj);
	}
	
	/**
	 * 将byte[]反序列化为Java对象
	 * @param bytes
	 * @param <T>
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toObject(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		
		return (T) configuration().asObject(bytes);
	}
}
