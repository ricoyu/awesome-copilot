package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.SerializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 传统JDK的序列化/反序列化
 * <p>
 * P2-9(CODE_REVIEW_REPORT): 修复前 serialize/deserialize 把异常捕获后只 log.error
 * 再返回 null——调用方分不清"值本来就是空"和"字节损坏/对象不可序列化", 缓存存坏时
 * 表现成"值不存在"继续往下走。现统一契约: 入参 null 返回 null, 真正失败抛
 * SerializeException(携带原始 cause)。
 * <p>
 * Copyright: (C), 2021-01-17 20:59
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SerializeUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(SerializeUtils.class);
	
	public static byte[] serialize(Object object) {
		if (object == null) { //P2-9: 入参 null 返回 null, 与反序列化方向对称
			return null;
		}
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		     ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
			objectOutputStream.writeObject(object);
			return byteArrayOutputStream.toByteArray();
		} catch (Exception e) { //P2-9: 失败不再降级成 null
			logger.error("序列化对象异常[" + e.getMessage() + "]", e);
			throw new SerializeException("serialize object failed: " + object.getClass().getName(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		     ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
			return (T) objectInputStream.readObject();
		} catch (Exception e) { //P2-9: 垃圾字节不再降级成 null
			logger.error("反序列化对象异常[" + e.getMessage() + "]", e);
			throw new SerializeException("deserialize bytes failed", e);
		}
	}
}
