package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.SerializeException;
import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Kryo序列化/反序列化
 * <p>
 * 优势: 不需要实现Serializble接口; 反序列化不需要提供Class对象
 * 限制: 只能在Java生态圈用, 不能跨语言
 * (P2-15 修复后"对象需要有默认构造函数"这一限制已解除: 没有无参构造的类
 * 走 StdInstantiator fallback 也能反序列化)
 *
 * <p>
 * Copyright: (C), 2021-01-19 20:35
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class KryoUtils {
	
	private static final String DEFAULT_ENCODING = "UTF-8";
	
	/**
	 * Kryo 实例不是线程安全的(官方文档明确声明, 内部复用 class 注册表和引用解析表),
	 * 多线程共用一个实例会把字节流写坏。改为每个线程持有自己的实例——
	 * Kryo 实例创建成本低, 线程复用时可长期持有。
	 * <p>
	 * 不要轻易改变下面的配置, 更改之后序列化的格式就会发生变化;
	 * 上线的同时就必须清除 Redis 里的所有缓存, 否则旧缓存反序列化时会报错。
	 */
	private static final ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(() -> {
		Kryo instance = new Kryo();
		
		/*
		 * 支持对象循环引用(否则会栈溢出)
		 * 默认值就是 true, 不要改变这个配置
		 */
		instance.setReferences(true);
		
		/*
		 * 不强制要求注册类
		 * 注册行为无法保证多个 JVM 内同一个类的注册编号相同
		 * 而且业务系统中大量的 Class 也难以一一注册
		 * 默认值就是 false
		 */
		instance.setRegistrationRequired(false);
		
		//P2-15(CODE_REVIEW_REPORT): 修复前这里写的是 instance.getInstantiatorStrategy()——
		//调了 getter 把返回值丢弃, 注释声称的 "Fix the NPE bug" 实际什么配置都没生效,
		//还误导后续维护者。现改为真的设置 fallback 策略: 没有无参构造的类走 StdInstantiator
		//(探针实测: 默认策略反序列化无无参构造的类抛 "Class cannot be created
		//(missing no-arg constructor)", 配置后往返成功; 原有 ArrayList 往返行为不受影响)。
		//注意: 该配置不改变已有字节流的格式, Redis 里旧缓存仍可正常读取。
		instance.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		
		return instance;
	});
	
	/**
	 * 将对象【及类型】序列化为字节数组
	 *
	 * @param obj 任意对象
	 * @param <T> 对象的类型
	 * @return 序列化后的字节数组
	 */
	public static <T> byte[] toBytes(T obj) {
		if (obj == null) { //P2-9: 统一 null 语义, 入参 null 返回 null
			return null;
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Output output = new Output(byteArrayOutputStream);
		try { //P2-9: 写失败统一包 SerializeException
			kryo.get().writeClassAndObject(output, obj);
			output.flush();
		} catch (Exception e) {
			throw new SerializeException("kryo serialize failed: " + obj.getClass().getName(), e);
		}
		
		return byteArrayOutputStream.toByteArray();
	}
	
	/**
	 * 将字节数组反序列化为原对象
	 *
	 * @param bytes toBytes 方法序列化后的字节数组
	 * @param <T>   原对象的类型
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toObject(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			//P2-9: 修复前 toObject(null) 直接 NullPointerException(与 FstUtils 的返回 null 不一致);
			//空字节数组不是任何对象的有效编码, 一并视为 null(与 FstUtils 一致)
			return null;
		}
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		Input input = new Input(byteArrayInputStream);
		try {
			return (T) kryo.get().readClassAndObject(input);
		} catch (Exception e) { //P2-9: 坏字节统一包 SerializeException
			throw new SerializeException("kryo deserialize failed", e);
		}
	}
	
	/**
	 * 将对象【及类型】序列化为 String
	 * 利用了 Base64 编码
	 *
	 * @param obj 任意对象
	 * @param <T> 对象的类型
	 * @return 序列化后的字符串
	 */
	public static <T> String writeToString(T obj) {
		byte[] bytes = toBytes(obj);
		if (bytes == null) { //P2-9: 入参 null 返回 null(修复前 encodeToString(null) 抛 NPE)
			return null;
		}
		return Base64.getUrlEncoder().encodeToString(bytes);
	}
	
	/**
	 * 将 String 反序列化为原对象
	 * 利用了 Base64 编码
	 *
	 * @param str writeToString 方法序列化后的字符串
	 * @param <T> 原对象的类型
	 * @return 原对象
	 */
	public static <T> T readFromString(String str) {
		if (str == null) { //P2-9: 入参 null 返回 null(修复前 Base64.decode(null) 抛 NPE)
			return null;
		}
		return toObject(Base64.getUrlDecoder().decode(str));
	}
}
