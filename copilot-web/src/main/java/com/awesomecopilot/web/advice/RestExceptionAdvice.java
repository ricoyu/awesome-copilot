package com.awesomecopilot.web.advice;

import com.awesomecopilot.common.lang.errors.ErrorTypes;
import com.awesomecopilot.common.lang.exception.ApplicationException;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.common.lang.exception.EntityNotFoundException;
import com.awesomecopilot.common.lang.exception.ServiceException;
import com.awesomecopilot.common.lang.vo.Result;
import com.awesomecopilot.common.lang.vo.Results;
import com.awesomecopilot.common.spring.i18n.I18N;
import com.awesomecopilot.validation.bean.ErrorMessage;
import com.awesomecopilot.validation.exception.GeneralValidationException;
import com.awesomecopilot.validation.exception.UniqueConstraintViolationException;
import com.awesomecopilot.validation.exception.ValidationException;
import com.awesomecopilot.validation.utils.ValidationUtils;
import com.awesomecopilot.web.exception.LocalizedException;
import com.awesomecopilot.web.utils.MessageHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.awesomecopilot.common.lang.errors.ErrorTypes.ACCESS_DENIED;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.BAD_REQUEST;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.INTERNAL_SERVER_ERROR;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.MAX_UPLOAD_SIZE_EXCEEDED;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.METHOD_NOT_ALLOWED;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.NOT_FOUND;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.READ_TIMEOUT;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.TOKEN_EXPIRED;
import static com.awesomecopilot.common.lang.errors.ErrorTypes.VALIDATION_FAIL;
import static java.util.stream.Collectors.*;

/**
 * 全局异常处理
 * <p>
 * Copyright: Copyright (c) 2019-10-11 15:04
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@RestControllerAdvice
public class RestExceptionAdvice extends ResponseEntityExceptionHandler implements ApplicationContextAware {

	/**
	 * 本类日志统一用这一个 slf4j logger(评审报告 P2-2: 旧代码混用父类 commons-logging 的
	 * logger 和自建 log, 且父类 logger 不支持 {} 占位符)。
	 */
	private static final Logger log = LoggerFactory.getLogger(RestExceptionAdvice.class);

	private static final Pattern MESSAGE_TEMPLATE_PATTERN = Pattern.compile("\\{(.+)\\}");

	/**
	 * 这个Pattern用来提取 multipart 解析底层异常文本里的实际请求大小（评审报告 P2-3 修复后仅作为兼容性回退：
	 * Spring 6.1 的 MaxUploadSizeExceededException.getMessage() 只剩 "Maximum upload size of N bytes exceeded"，
	 * 完整大小信息在 cause 里，首选从 cause 的 getActualSize() 获取）。
	 * 旧版正则用 matches() 且要求整段文本以 maximum(...) 结尾，经探针实测对 Spring 6.1.5 的消息
	 * 永远不命中（消息结构已变——源码里 \\s 转义后就是 \s，报告"转义腐蚀"的说法不准，真正原因是消息形态），
	 * 保留正则仅为兼容老版本 Spring/其他容器，改用 find() 语义。
	 */
	private static final Pattern ACTUAL_SIZE_PATTERN =
			Pattern.compile("size\\s*\\((\\d+)\\).*maximum\\s*\\((\\d+)\\)");

	/**
	 * 遍历 cause 链的深度上限。JDK 的 initCause 只拒绝"cause==this"自引用, 不检测 a→b→a 这类
	 * 跨对象环(独立评审 S-2 用探针证实), 无上限遍历会永久占死处理请求的线程。
	 * 正常框架包裹链不超过十层, 100 已是极宽余量。
	 */
	private static final int MAX_CAUSE_DEPTH = 100;

	/**
	 * 标识微服务是否整合了阿里巴巴Sentinel。null=还没探测过；探测结果终身缓存，
	 * 异常处理路径上最多只查一次容器（评审报告 P2-1）。
	 */
	private volatile Boolean sentinelPresent;

	private ApplicationContext applicationContext;

	@Override
	@ResponseBody
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("参数类型不匹配: {}", ex.getMessage());
		return super.handleTypeMismatch(ex, headers, status, request);
	}
	/**
	 * 表单提交数据校验失败(非 @RequestBody 那条 @Valid 校验链路, 那条由父类 handleMethodArgumentNotValid 分发).
	 * 2025-03-21(e6bea5fd) 此方法被块注释停用, 评审修复轮把注释块整个删除——删除时未核功能是否还需要,
	 * 属误删(用户指出表单校验出参依赖它). 现在恢复并按全仓统一约定出参: HTTP 200 + Result(4002, 字段错误列表),
	 * 不再返回 200 空 headers 的旧形态.
	 * <p>
	 * 关于 @SuppressWarnings("removal"): 父类钩子 6.0 起被标 forRemoval, IDEA 对"重写该类方法"报错.
	 * 不改用"另起方法名 + @ExceptionHandler(BindException.class)"的写法——Spring 的
	 * ExceptionHandlerMethodResolver 对同一异常类型注册两个无重写关系的 handler 会在启动时直接抛
	 * Ambiguous @ExceptionHandler method mapped, 而父类 handleException 的映射列表里就有 BindException,
	 * 合法覆盖它的唯一方式就是保持同签名重写. Spring 6.1.5 分派链仍调用此钩子
	 * (handleException -> :204 -> handleBindException, 由 RestExceptionAdviceBindTest 堆栈证实).
	 * 未来升级到真正删除该方法(7.x)时, RestExceptionAdviceBindTest 会变红提醒迁移, 不是无声失效.
	 */
	@SuppressWarnings("removal")
	@Override
	@ResponseBody
	protected ResponseEntity<Object> handleBindException(
			BindException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("表单校验失败: {}", ex.getMessage());
		headers.add("Content-Type", "application/json");
		ErrorMessage errorMessage = ValidationUtils.getErrorMessage(ex.getBindingResult());
		//业务码与消息处理和 handleMethodArgumentNotValid(表单/@RequestBody 校验失败的主路径)完全一致:
		//Spring 6.0+ 表单 @Valid 校验失败实际抛子类 MethodArgumentNotValidException, 已不走本方法;
		//保留它只为兜底"应用代码直接抛裸 BindException"的场景, 出参口径必须与主路径统一(2026-09-21)
		Result result = Results.status(VALIDATION_FAIL.code(), resolveTemplateMessages(errorMessage.getErrors())).build();
		return new ResponseEntity<>(result, headers, HttpStatus.OK);
	}

	/**
	 * 处理验证相关的异常
	 * 目前只处理了BindException
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	protected ResponseEntity<Object> handleValidationException(ValidationException e) {
		log.info("校验异常: {}", e.getMessage(), e);
		Throwable cause = e.getCause();
		if (cause instanceof BindException bindException) {
			BindingResult bindingResult = bindException.getBindingResult();
			ErrorMessage errorMessage = ValidationUtils.getErrorMessage(bindingResult);
			List<String[]> msgs = errorMessage.getErrors();
			/*
			 * 这边不要对msgs执行Jackson序列化, 如果这么做, 输出到前端的JSON串会对双引号转义, 看到的将会是类似这样:
			 * <pre>
			 * {
			 *   "code": "4002",
			 *   "status": "fail",
			 *   "message": "[[\"endTime\",\"结束时间不能为空\"],[\"name\",\"场次名称不能超过200个字符\"],[\"startTime\",\"开始时间不能为空\"]]"
			 * }
			 * </pre>
			 */
			Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
			return new ResponseEntity(result, HttpStatus.OK);
		}
		Result result = Results.status(VALIDATION_FAIL.code(), e.getMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	/**
	 * 请求体(JSON等)反序列化失败属于客户端提交的数据有问题, 按 HTTP 语义返回 400。
	 * 修复前(评审报告 P2-4)返回 500, 会让网关/熔断把客户端错误计入服务端失败率。
	 * (旧注释"处理输出JSON串时候序列化报错"写反了: 那是 NotWritable 的职责, NotReadable 是输入侧)
	 *
	 * @param ex the exception to handle
	 * @param headers the headers to use for the response
	 * @param status the status code to use for the response
	 * @param request the current request
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("请求体解析失败: {}", ex.getMessage());
		headers.add("Content-Type", "application/json");
		Result result = Results.status(BAD_REQUEST).build();
		return new ResponseEntity(result, headers, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 响应序列化失败是服务端问题, 但旧实现返回 200+4001; 保持响应码不变(下游有依赖),
	 * 日志级别升为 error 并带上原因(评审报告 P2-2: 空消息补全)。
	 */
	@Override
	protected @Nullable ResponseEntity<Object> handleHttpMessageNotWritable(
			HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.error("响应序列化失败: {}", ex.getMessage(), ex);
		headers.add("Content-Type", "application/json");
		Result result = Results.status(BAD_REQUEST).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.info("@RequestBody参数校验失败: {}", ex.getMessage());
		ErrorMessage errorMessage = ValidationUtils.getErrorMessage(ex.getBindingResult());
		List<String[]> msgs = resolveTemplateMessages(errorMessage.getErrors());
		/*
		 * 这边不要对msgs执行Jackson序列化, 理由同 handleValidationException 内注释
		 */
		Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}
	/**
	 * 校验错误数组([字段,消息模板])里的消息若是 {key} 形态的国际化模板占位符, 解析成实际文案.
	 * handleBindException / handleMethodArgumentNotValid 两条校验失败路径共用(2026-09-21 抽取自重复代码).
	 */
	private static List<String[]> resolveTemplateMessages(List<String[]> errors) {
		return errors.stream()
				.map((errArray) -> {
					if (errArray.length > 1) {
						Matcher matcher = MESSAGE_TEMPLATE_PATTERN.matcher(errArray[1]);
						if (matcher.matches()) {
							errArray[1] = MessageHelper.getMessage(matcher.group(1));
						}
						return errArray;
					} else {
						Matcher matcher = MESSAGE_TEMPLATE_PATTERN.matcher(errArray[0]);
						if (matcher.matches()) {
							errArray[0] = MessageHelper.getMessage(matcher.group(1));
						}
						return errArray;
					}
				}).collect(toList());
	}

	/**
	 * 手工验证不通过时抛出。
	 * 旧实现方法名写成 handleMethodArgumentNotValid(GeneralValidationException), 与上面处理
	 * Spring 标准异常的 override 方法同名不同参, 极易混淆(评审报告 P2-10), 改名。
	 *
	 * @param e
	 * @return ResponseEntity<Object>
	 */
	@ExceptionHandler(GeneralValidationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	protected ResponseEntity<Object> handleGeneralValidationException(GeneralValidationException e) {
		log.info("手工校验失败: {}", e.getMessage());
		ErrorMessage errorMessage = e.getErrorMessage();
		List<String[]> msgs = errorMessage.getErrors()
				.stream()
				.map((errArray) -> {
					Matcher matcher = MESSAGE_TEMPLATE_PATTERN.matcher(errArray[1]);
					if (matcher.matches()) {
						errArray[1] = MessageHelper.getMessage(matcher.group(1));
					}
					return errArray;
				})
				.collect(toList());
		/*
		 * 这边不要对msgs执行Jackson序列化, 理由同 handleValidationException 内注释
		 */
		Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@ExceptionHandler(UniqueConstraintViolationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleUniqueConstraintViolationException(UniqueConstraintViolationException e) {
		log.error("唯一约束冲突: {}", e.getMessage(), e);
		Result result = Results.status(INTERNAL_SERVER_ERROR).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException e) {
		log.warn("实体未找到: {}", e.getMessage());
		Result result = Results.status(NOT_FOUND.code(), e.getMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status,
			WebRequest request) {
		log.info("不支持的请求方法: {}", ex.getMethod());
		headers.add("Content-Type", "application/json");
		Result result = Results.status(METHOD_NOT_ALLOWED).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}

	/**
	 * 通用业务异常处理
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(BusinessException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleBusinessException(BusinessException e) {
		log.error("业务异常 code={}: {}", e.getCode(), e.getMessage(), e);
		Result result = Results.status(e.getCode(), I18N.i18nMessage(e)).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	/**
	 * 通用业务异常处理
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(ServiceException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleServiceException(ServiceException e) {
		log.error("服务异常 code={}: {}", e.getCode(), e.getMessage(), e);
		Result result = Results.status(e.getCode(), e.getMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@ExceptionHandler(LocalizedException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleLocalizedException(LocalizedException e) {
		log.error("国际化异常 status={}: {}", e.getStatusCode(), e.getLocalizedMessage(), e);
		Result result = Results.status(e.getStatusCode(), e.getLocalizedMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	@ExceptionHandler(ApplicationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public Result handleApplicationException(ApplicationException e) {
		log.error("应用异常 code={}: {}", e.getCode(), e.getMessage(), e);
		return Results.status(e.getCode(), e.getMessage()).build();
	}

	/**
	 * 上传文件是multipart/form-data类型, 所以这边@ResponseBody实际是不生效的, 需要通过Response手工返回REST结果
	 * <p>
	 * 评审报告 P2-3: 补 @Override 明确这是覆写父类 protected 钩子(父类 public final handleException
	 * 按 instanceof 分发到它, 之前没写注解但签名一致, 实际一直生效——复现测试证实); 并修复大小提取——
	 * 限制大小直接用 e.getMaxUploadSize(), 实际大小沿 cause 链找 getActualSize() 或按消息文本回退提取。
	 *
	 * @param e       the exception to handle
	 * @param headers the headers to use for the response
	 * @param status  the status code to use for the response
	 * @param request the current request
	 */
	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		long limitSize = e.getMaxUploadSize();
		Long actualSize = findActualSize(e);
		String defaultMessage = MAX_UPLOAD_SIZE_EXCEEDED.message();
		// 大小提示: i18n 取不到消息时的替代文本(I18N.i18nMessage 在 MessageSource 缺失时返回 null,
		// 不会自动落到 defaultMessage, 所以这里自己构造带大小的中文提示)
		String sizeHint;
		if (actualSize != null && limitSize >= 0) {
			log.error("上传大小超限: 实际 {} 字节, 限制 {} 字节", actualSize, limitSize, e);
			sizeHint = String.format("%s(实际 %d 字节, 限制 %d 字节)", defaultMessage, actualSize, limitSize);
		} else if (limitSize >= 0) {
			log.error("上传大小超限: 限制 {} 字节", limitSize, e);
			sizeHint = String.format("%s(限制 %d 字节)", defaultMessage, limitSize);
		} else {
			log.error("上传大小超限", e);
			sizeHint = defaultMessage;
		}
		String message;
		if (actualSize != null && limitSize >= 0) {
			message = I18N.i18nMessage(MAX_UPLOAD_SIZE_EXCEEDED.msgTemplate(),
					new Long[]{actualSize, limitSize}, sizeHint);
		} else {
			message = I18N.i18nMessage(MAX_UPLOAD_SIZE_EXCEEDED.msgTemplate(), sizeHint);
		}
		if (StringUtils.isBlank(message)) {
			message = sizeHint;
		}

		Result result = Results.status(MAX_UPLOAD_SIZE_EXCEEDED.code(), message).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}

	/**
	 * 沿 cause 链找"实际请求大小": 优先反射调用 Tomcat SizeException 系的 getActualSize(),
	 * 其次按老版本消息文本回退提取。
	 * 独立评审 S-2: Throwable.initCause 允许构造 a→b→a 环, 必须有深度上限, 否则永久占死线程。
	 * 独立评审 S-1: 文本里的数字理论上可超过 Long 范围, parseLong 不能把解析器自己弄抛异常。
	 */
	private Long findActualSize(Throwable top) {
		int depth = 0;
		for (Throwable t = top; t != null && depth++ < MAX_CAUSE_DEPTH; t = t.getCause()) {
			try {
				Object value = t.getClass().getMethod("getActualSize").invoke(t);
				if (value instanceof Number number) {
					return number.longValue();
				}
			} catch (ReflectiveOperationException | RuntimeException ignored) {
				// 该层cause没有可用的 getActualSize(), 继续试消息文本
			}
			if (t.getMessage() != null) {
				Matcher matcher = ACTUAL_SIZE_PATTERN.matcher(t.getMessage());
				if (matcher.find()) {
					try {
						return Long.parseLong(matcher.group(1));
					} catch (NumberFormatException e) {
						// 数字超出 Long 范围等异常形态: 视为提取失败, 继续往下找
					}
				}
			}
		}
		return null;
	}

	/**
	 * 未捕获异常的最终处理。实现与策略(评审报告 P2-1 修正了旧注释的矛盾说法):
	 * <p>
	 * 1. 应用整合了 Sentinel(容器里存在 restBlockExceptionHandler Bean)时, 对异常执行
	 *    Tracer.trace(e) 计入熔断统计——注意是"只统计、不重新抛出", 响应仍由本方法产出;
	 *    旧注释说"直接重新抛出让 RestBlockExceptionHandler 处理"与实现不符, 以实现为准
	 *    (重新抛出会让非业务异常落到 Spring Boot 默认错误页, 微服务间调用方拿不到统一 Result 结构)。
	 * 2. 业务异常(含被包裹在其他异常 cause 里的)返回 HTTP 200 + 业务码。
	 * 3. 非业务异常返回 HTTP 500, 是为了微服务之间调用时调用方直接感知失败、方便触发熔断;
	 *    否则接口一直是正常返回, 熔断不了。
	 *    (旧代码这里挂着 @ResponseStatus(OK) 与"强烈建议500"的矛盾注释: 方法返回带状态的
	 *    ResponseEntity 时 @ResponseStatus 本就不生效, 实际状态由 ResponseEntity 决定, 故删注解。)
	 */
	@ExceptionHandler(Throwable.class)
	public ResponseEntity<?> handleThrowable(Throwable e) {
		if (isSentinelPresent()) {
			com.alibaba.csp.sentinel.Tracer.trace(e); // 让 Sentinel 统计异常
		}
		return findRealCause(e);
	}

	/**
	 * Sentinel 整合探测结果终身缓存: 旧实现每个错误请求都 getBean("restBlockExceptionHandler")
	 * 查一次容器(成功路径上还重复 trace 三次), 评审报告 P2-1。双检锁, 只查一次。
	 */
	private boolean isSentinelPresent() {
		Boolean present = sentinelPresent;
		if (present != null) {
			return present;
		}
		synchronized (this) {
			present = sentinelPresent;
			if (present == null) {
				try {
					applicationContext.getBean("restBlockExceptionHandler");
					present = true;
					log.info("检测到 restBlockExceptionHandler, 未捕获异常将计入 Sentinel 熔断统计");
				} catch (BeansException e) {
					present = false;
					log.debug("当前没有整合Sentinel, Throwable异常由本Advice直接处理");
				}
				sentinelPresent = present;
			}
			return present;
		}
	}

	/**
	 * 有时候, 业务代码抛出了某个比较有意义的异常, 但是由于系统组件比较多, 可能这个异常被系统组件捕获并包裹成另外一个异常, 比如RumtimeException
	 * 导致返回的错误信息没有正确反应错误类型, 这里试图找到真正的的异常类型
	 *
	 * @param e
	 * @return
	 */
	private ResponseEntity<?> findRealCause(Throwable e) {
		if (e.getCause() != null && e.getCause() instanceof BusinessException) {
			return handleBusinessException((BusinessException) e.getCause());
		}
		if (e.getCause() != null && e.getCause() instanceof ValidationException) {
			return handleValidationException((ValidationException) e.getCause());
		}
		if (e.getCause() != null && e.getCause() instanceof UniqueConstraintViolationException) {
			return handleUniqueConstraintViolationException((UniqueConstraintViolationException) e.getCause());
		}
		if (e.getCause() != null && e.getCause() instanceof EntityNotFoundException) {
			return handleEntityNotFoundException((EntityNotFoundException) e.getCause());
		}
		if (e.getCause() != null && e.getCause() instanceof LocalizedException) {
			return handleLocalizedException((LocalizedException) e.getCause());
		}
		if (e.getCause() != null && e.getCause() instanceof ApplicationException) {
			Result result = handleApplicationException((ApplicationException) e.getCause());
			return new ResponseEntity(result, HttpStatus.OK);
		}
		/*
		 * 下面这几个是Sa-Token支持
		 */
		if ("cn.dev33.satoken.exception.NotLoginException".equalsIgnoreCase(e.getClass().getName())) {
			Result result = Results.fail().status(TOKEN_EXPIRED).build();
			return new ResponseEntity(result, HttpStatus.OK);
		}
		if ("cn.dev33.satoken.exception.NotPermissionException".equalsIgnoreCase(e.getClass().getName())) {
			Result result = Results.fail().status(ACCESS_DENIED).build();
			return new ResponseEntity(result, HttpStatus.OK);
		}

		/*
		 * 上面这些都属于已经被处理的业务异常
		 */
		log.error("未捕获异常: {}", e.getMessage(), e);
		/*
		 * 下面这两个是非业务异常, 返回status code 500, 是为了微服务之间调用时调用方调接口直接报错;
		 * 方便做熔断处理, 否则接口一直是正常返回, 熔断不了
		 */
		if (e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
			Result<Object> result = Results.fail().status(READ_TIMEOUT).build();
			return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		Result result = Results.status(ErrorTypes.INTERNAL_SERVER_ERROR).build();
		return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
