package com.awesomecopilot.web.advice;

import com.awesomecopilot.common.lang.vo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表单校验失败(BindException)的统一出参测试.
 * <p>
 * 背景: handleBindException 在 2025-03-21(e6bea5fd) 被块注释掉, 评审修复轮又把注释块整个删除.
 * 表单提交 @Valid 校验失败(非 @RequestBody 那条 MethodArgumentNotValid 链路)时,
 * 没有重写方法可走, 落到父类默认实现: HTTP 400 + RFC7807 ProblemDetail——
 * 与全仓 "HTTP 200 + 统一 Result JSON" 的约定不符, 前端按 code 字段解析会拿到 undefined.
 * 本测试把恢复后的契约固定下来: 200 + Result(code=4001, message 带字段错误).
 */
class RestExceptionAdviceBindTest {
	
	@Test
	void bindExceptionReturnsUnifiedResultJsonNotProblemDetail() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();
		
		BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "userForm");
		br.addError(new FieldError("userForm", "name", "场次名称不能超过200个字符"));
		BindException e = new BindException(br);
		
		var entity = advice.handleException(e, new ServletWebRequest(new MockHttpServletRequest()));
		
		assertEquals(HttpStatus.OK, entity.getStatusCode(),
				"表单校验失败必须回 200+Result(评审前该重写方法被注释/删除, 落到父类 400 ProblemDetail)");
		assertTrue(entity.getBody() instanceof Result,
				"响应体必须是统一 Result, 实际=" + (entity.getBody() == null ? "null" : entity.getBody().getClass()));
		Result body = (Result) entity.getBody();
		//2026-09-21 与主路径统一: Spring 6.0+ 表单校验实际抛子类 MethodArgumentNotValidException,
		//走 handleMethodArgumentNotValid 返回 4002+同一 msgs 结构; 本方法只剩"应用代码直接抛裸
		//BindException"的兜底职责, 业务码与其保持一致(此前 4001 与主路径 4002 两套码口径不一致)
		assertEquals("4002", body.getCode(), "VALIDATION_FAIL 业务码, 与 @RequestBody 校验失败主路径一致");
		//message 是 List<String[]>([字段,错误文本]), Jackson 在响应出口序列化成 JSON 数组;
		//单元测试拿的是内存对象, 按结构断言, 不做 String.valueOf 文本匹配
		Object msg = body.getMessage();
		assertTrue(msg instanceof java.util.List, "message 必须是 List, 实际=" + (msg == null ? "null" : msg.getClass()));
		boolean fieldErrorCarried = ((java.util.List<?>) msg).stream().anyMatch(o ->
				o instanceof String[] && java.util.Arrays.toString((String[]) o).contains("场次名称不能超过200个字符"));
		assertTrue(fieldErrorCarried, "字段错误必须透传给前端, 实际=" + java.util.Arrays.toString(((java.util.List<?>) msg).toArray()));
	}
}
