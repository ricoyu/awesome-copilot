package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.SerializeException;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-9 / P2-15 序列化门面族回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 统一契约（三个类一致，与既有 FstUtils 的 null 语义保持一致）：
 * <ul>
 * <li>入参 null → 返回 null（序列化: toBytes/serialize(null)=null; 反序列化: toObject(null)=null）；</li>
 * <li>字节损坏 / 对象不可序列化 → 抛 SerializeException（修复前 SerializeUtils 只 log.error
 * 然后返回 null——调用方分不清"值本来就是空"和"字节损坏"，缓存存坏时表现成"值不存在"继续往下走）；</li>
 * <li>P2-15: KryoUtils 里 "Fix the NPE bug" 注释下调用的是 getter(空操作), 修复为真的
 * setInstantiatorStrategy; 并用"无默认构造的类往返成功"锁定生效性。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SerializeP2FixTest {

	// ---------------- P2-9 SerializeUtils ----------------

	@Test
	public void testSerializeUnserializableObjectThrows() {
		assertThatThrownBy(() -> SerializeUtils.serialize(new Object())) //Object 未实现 Serializable
				.as("不可序列化对象必须抛 SerializeException(修复前返回 null)")
				.isInstanceOf(SerializeException.class);
	}

	@Test
	public void testDeserializeGarbageBytesThrows() {
		assertThatThrownBy(() -> SerializeUtils.deserialize(new byte[]{1, 2, 3, 4, 5}))
				.as("垃圾字节必须抛 SerializeException(修复前返回 null, 缓存损坏会伪装成'值不存在')")
				.isInstanceOf(SerializeException.class);
	}

	@Test
	public void testDeserializeNullReturnsNull() {
		Object deserialized = SerializeUtils.deserialize(null);
		assertThat(deserialized).isNull(); //入参 null 才返回 null
	}

	@Test
	public void testSerializeRoundTripStillWorks() {
		List<String> src = new ArrayList<>(List.of("a", "b"));
		byte[] bytes = SerializeUtils.serialize(src);
		assertThat(SerializeUtils.<List<String>>deserialize(bytes)).isEqualTo(src);
	}

	// ---------------- P2-9 KryoUtils ----------------

	@Test
	public void testKryoToObjectOfNullReturnsNull() {
		//修复前实测: new ByteArrayInputStream(null) 抛 NullPointerException
		Object restored = KryoUtils.toObject(null);
		assertThat(restored)
				.as("KryoUtils.toObject(null) 应返回 null, 与 FstUtils/SerializeUtils 统一")
				.isNull();
	}

	@Test
	public void testKryoNullSemantics() {
		//统一契约: null 进 null 出
		assertThat(KryoUtils.toBytes(null)).isNull();
	}

	@Test
	public void testKryoReadFromStringNull() {
		Object fromString = KryoUtils.readFromString(null);
		assertThat(fromString).isNull(); //修复前 Base64.decode(null) 抛 NPE
	}

	// ---------------- P2-9 ProtostuffUtils ----------------

	@Test
	public void testProtostuffToBytesNullReturnsNull() {
		//修复前实测返回 byte[0](伪装成"空消息"), 与 FstUtils(返回 null)不一致
		byte[] nullBytes = ProtostuffUtils.toBytes(null);
		assertThat(nullBytes)
				.as("toBytes(null) 统一返回 null(修复前实测返回 byte[0], 伪装成'空消息')")
				.isNull();
	}

	@Test
	public void testProtostuffToObjectNullBytesReturnsNull() {
		Point nullIn = ProtostuffUtils.toObject(null, Point.class);
		assertThat(nullIn)
				.as("toObject(null, clazz) 统一返回 null(修复前直接 NPE)")
				.isNull();
	}

	@Test
	public void testProtostuffRoundTripStillWorks() {
		Point p = new Point(3, 4);
		Point back = ProtostuffUtils.toObject(ProtostuffUtils.toBytes(p), Point.class);
		assertThat(back.x).isEqualTo(3);
		assertThat(back.y).isEqualTo(4);
	}

	// ---------------- P2-15 KryoUtils InstantiatorStrategy ----------------

	@Test
	public void testKryoDeserializesClassWithoutDefaultConstructor() {
		//修复前: 注释声称 "Fix the NPE bug when deserializing Collections" 但代码只调用了
		//getInstantiatorStrategy()(getter, 返回值丢弃), 没有任何配置生效——
		//Kryo 默认策略对"没有可访问无参构造"的类会抛 "no valid constructor"。
		//修复后 setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy())),
		//该对象可以往返。
		NoDefaultCtor src = new NoDefaultCtor("rico", 30);
		byte[] bytes = KryoUtils.toBytes(src);
		NoDefaultCtor back = KryoUtils.toObject(bytes);
		assertThat(back.name).isEqualTo("rico");
		assertThat(back.age).isEqualTo(30);
	}

	@Test
	public void testKryoCollectionRoundTripStillWorks() {
		//探针实测: 默认的 DefaultInstantiatorStrategy 已能处理 ArrayList, 改配置不能破坏
		ArrayList<String> src = new ArrayList<>(List.of("x", "y"));
		List<String> back = KryoUtils.toObject(KryoUtils.toBytes(src));
		assertThat(back).isEqualTo(src);
	}

	public static class Point {
		public int x;
		public int y;

		public Point() {
		}

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static class NoDefaultCtor implements Serializable {
		private final String name;
		private final int age;

		public NoDefaultCtor(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}
}
