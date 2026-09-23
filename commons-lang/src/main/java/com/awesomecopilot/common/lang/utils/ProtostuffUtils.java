package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.SerializeException;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protostuff是基于ProtoBuf实现的, 主要用于Java领域的序列化框架。
 * <ul>
 * <li/>可以直接使用Java类, 而不是.proto文件
 * <li/>它较于protobuf最明显的好处是, 在几乎不损耗性能的情况下做到了不用我们写.proto文件来实现序列化。
 * <li/>Protostuff基于Protobuf。它使用Protobuf的核心概念, 但添加了更易用的API和额外的功能。
 * <li/>Protobuf是面向多种语言的更通用解决方案, 而Protostuff主要针对Java开发者, 提供更简洁的使用方式。
 * <li/>Protobuf需要.proto文件来定义数据结构, 而Protostuff可以直接利用Java类进行序列化。
 * <li/>Protostuff在Protobuf的基础上提供了更多的功能, 如schema-less序列化和数据压缩。
 * </ul>
 * 优势: 对象不需要有默认构造函数也不需要实现Serializble接口<p/>
 * 限制: 反序列化需要提供Class对象<p>
 * <p>
 * Copyright: (C), 2020-10-10 17:31
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ProtostuffUtils {
	/**
	 * 缓存Schema
	 */
	private static Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();
	
	/**
	 * 序列化方法，把指定对象序列化成字节数组
	 * P2-9(CODE_REVIEW_REPORT): 修复前 obj==null 返回 byte[0]——调用方把它和"空消息"
	 * 混在一起, 且与同族 FstUtils.toBytes(null) 返回 null 不一致。现统一为返回 null。
	 *
	 * @param obj
	 * @param <T>
	 * @return byte[]
	 */
	public static <T> byte[] toBytes(T obj) {
		if (obj == null) {
			return null; //P2-9: 统一 null 语义(修复前返回 byte[0] 并 log.info)
		}
		
		Class<T> clazz = (Class<T>) obj.getClass();
		Schema<T> schema = getSchema(clazz);
		LinkedBuffer buffer = LinkedBuffer.allocate();
		try {
			return ProtobufIOUtil.toByteArray(obj, schema, buffer);
		} catch (Exception e) { //P2-9: 失败统一包 SerializeException, 保留原始 cause
			throw new SerializeException("protostuff serialize failed: " + clazz.getName(), e);
		} finally {
			buffer.clear();
		}
	}
	
	/**
	 * 反序列化方法，将字节数组反序列化成指定Class类型
	 * P2-9(CODE_REVIEW_REPORT): 修复前 toBytes(null) 产生的 byte[0] 喂回来会得到
	 * "字段全默认值的对象"而不是 null——坏数据被伪装成正常对象。现:
	 * bytes 为 null/空 → 返回 null; 解析失败 → 抛 SerializeException。
	 *
	 * @param bytes
	 * @param clazz
	 * @param <T>
	 * @return T
	 */
	public static <T> T toObject(byte[] bytes, Class<T> clazz) {
		if (bytes == null || bytes.length == 0) { //P2-9: 空/坏输入不再造"默认值对象"
			return null;
		}
		Schema<T> schema = getSchema(clazz);
		T obj = schema.newMessage();
		try {
			ProtobufIOUtil.mergeFrom(bytes, obj, schema);
		} catch (Exception e) { //P2-9
			throw new SerializeException("protostuff deserialize failed: " + clazz.getName(), e);
		}
		return obj;
	}
	
	private static <T> Schema<T> getSchema(Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
		if (schema == null) {
			/*
			 * 这个schema通过RuntimeSchema进行懒创建并缓存
			 * 所以可以一直调用RuntimeSchema.getSchema(), 这个方法是线程安全的
			 */
			schema = RuntimeSchema.getSchema(clazz);
			if (schema != null) {
				schemaCache.put(clazz, schema);
			}
		}
		
		return schema;
	}
}
