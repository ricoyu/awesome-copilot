package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.ApplicationException;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.common.lang.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P1-8 / P1-9 / P1-19 / P1-21 / P1-24 回归测试（CODE_REVIEW_REPORT 2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P1-8 FstUtils 在 JDK 17+（未加 --add-opens）触碰类即 ExceptionInInitializerError，
 * 改为给出带指引的 IllegalStateException（初始化只尝试一次，不反复抖动）；</li>
 * <li>P1-9 AlgorithmUtils.randomArr 退化入参（len&lt;=1 或 maxValue&lt;=1）死循环，
 * 改为入口抛 IllegalArgumentException，正常入参相邻两值必不等；</li>
 * <li>P1-19 DynamicUtils.createObject 在 for-each 里删调用方 Map 的元素，
 * 抛 ConcurrentModificationException 或悄悄改掉入参，改为不动入参；</li>
 * <li>P1-21 BusinessException 四参构造 / ApplicationException(Throwable) /
 * ServiceException 三参构造三个构造器丢消息，getMessage() 返回 null 或固定文案；</li>
 * <li>P1-24 TraceId 只含"服务名-毫秒-进程内序列"，多实例同毫秒必然重号，
 * 掺入每 JVM 随机实例段，格式回到 javadoc 承诺的 TID-{serviceId}-{millis}-{instance}-{sequence}。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class MiscBatchFixTest {

	// ---------------- P1-8 FstUtils ----------------

	@Test
	public void testFstUtilsDegradesWithGuidanceNotInitializationError() {
		//修复前实测(JDK 21 探针): FstUtils.toBytes 首触即 ExceptionInInitializerError,
		//根因 InaccessibleObjectException: module java.base does not "opens java.lang"。
		//本模块 pom 没有 --add-opens, 所以这里必定走"初始化失败"降级分支
		assertThatThrownBy(() -> FstUtils.toBytes("hello"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("--add-opens java.base/java.lang=ALL-UNNAMED");
	}

	@Test
	public void testFstUtilsInitializationAttemptedOnce() {
		//第一次降级之后, 再次调用不能反复重试初始化(同一个异常快速抛出)
		assertThatThrownBy(() -> FstUtils.toBytes("again"))
				.isInstanceOf(IllegalStateException.class);
	}

	// ---------------- P1-9 AlgorithmUtils ----------------

	@Test
	@Timeout(value = 5, threadMode = ThreadMode.SEPARATE_THREAD) //修复前实测: 退化入参在 main 线程死循环 421s(jstack 看到栈顶), SAME_THREAD 中断不了, 必须独立线程
	public void testRandomArrRejectsDegenerateInputs() {
		//修复前实测: randomArr(1, 10) 与 randomArr(10, 1) 都进入 do-while 死循环
		assertThatThrownBy(() -> AlgorithmUtils.randomArr(1, 10))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> AlgorithmUtils.randomArr(0, 10))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> AlgorithmUtils.randomArr(10, 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testRandomArrNormalBehavior() {
		int[] nums = AlgorithmUtils.randomArr(10, 10);
		assertThat(nums.length).isBetween(1, 9);
		for (int i = 0; i < nums.length; i++) {
			assertThat(nums[i]).isBetween(0, 9);
			if (i > 0) {
				assertThat(nums[i]).as("相邻两值必不等").isNotEqualTo(nums[i - 1]);
			}
		}
	}

	// ---------------- P1-19 DynamicUtils ----------------

	@Test
	public void testCreateObjectDoesNotThrowCmeOnNullValue() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("gone", null);
		properties.put("kept", 1L);
		//修复前实测: 抛 ConcurrentModificationException
		Object[] holder = new Object[1];
		assertThatCode(() -> holder[0] = DynamicUtils.createObject(Object.class, properties))
				.doesNotThrowAnyException();
		assertThat(holder[0]).isNotNull();
	}

	@Test
	public void testCreateObjectLeavesCallerMapUntouched() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("name", null);
		properties.put("age", 3);
		DynamicUtils.createObject(Object.class, properties);
		//修复前实测: 调用方的 Map 被 remove 掉 name 键
		assertThat(properties).containsKey("name");
		assertThat(properties).containsKey("age");
		assertThat(properties).hasSize(2);
	}

	// ---------------- P1-21 异常族 ----------------

	@Test
	public void testBusinessExceptionFourArgKeepsDefaultMessage() {
		BusinessException e = new BusinessException("5001", "stock.{count} shortage", null, "库存不足");
		//修复前实测: getMessage() 返回 null(四参构造从头到尾没给 message 赋值)
		assertThat(e.getMessage()).isEqualTo("库存不足");
	}

	@Test
	public void testApplicationExceptionWrapsCauseMessage() {
		ApplicationException e = new ApplicationException(new IOException("db down"));
		//修复前实测: getMessage() 恒为 "Internal Server Error", 真实原因只能翻堆栈
		assertThat(e.getMessage()).contains("db down");
		assertThat(e.getCause()).isInstanceOf(IOException.class);
	}

	@Test
	public void testServiceExceptionThreeArgKeepsTemplate() {
		ServiceException e = new ServiceException("4002", "order.create.fail", "下单失败");
		assertThat(e.getMessage()).isEqualTo("下单失败");
		//修复前实测: 三参构造没人接收 messageTemplate, 类里也没有 getMsgTemplate
		assertThat(e.getMsgTemplate()).isEqualTo("order.create.fail");
	}

	@Test
	public void testNoArgConstructorsKeepPreviousMessage() {
		//无参/单参等既有构造器行为不回退(默认 "Internal Server Error" / FAIL code)
		assertThat(new BusinessException().getMessage()).isNull();
		assertThat(new ServiceException("plain").getCode()).isEqualTo("1");
	}

	// ---------------- P1-24 TraceId ----------------

	@Test
	public void testTraceIdFormatDocumentedByJavadoc() {
		String traceId = TraceId.traceId("order-service");
		//修复前: TID-{serviceId}-{millis}-{seq}, 无任何实例成分; javadoc 还写着 "REQ-" 前缀
		assertThat(traceId).matches("TID-order-service-\\d{13}-[0-9a-f]{8}-\\d+");
	}

	@Test
	public void testTraceIdWithoutServiceIdAlsoCarriesInstancePart() {
		String traceId = TraceId.traceId();
		assertThat(traceId).matches("TID-\\d{13}-[0-9a-f]{8}-\\d+");
	}
}
