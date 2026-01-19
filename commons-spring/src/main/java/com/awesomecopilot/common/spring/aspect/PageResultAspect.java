package com.awesomecopilot.common.spring.aspect;

import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.common.lang.vo.Result;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

/**
 * 分页查询时, 在返回结果前, 将分页信息写入Result对象
 * <p>
 * Copyright: (C), 2021-04-17 19:48
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Aspect
public class PageResultAspect {

	/**
	 * 分页方法需要持有一个Page类型参数, 或者第一个参数VO里面包含一个Page类型的字段page
	 *
	 * @param joinPoint
	 */
	@AfterReturning(value = "@annotation(org.springframework.web.bind.annotation.PostMapping)", returning = "result")
	public void afterReturning(JoinPoint joinPoint, Object result) {
			Page page = ThreadContext.get("page");
			setPage(result, page);
	}

	private void setPage(Object result, Object page) {
		if (!(result instanceof Result)) {
			return;
		}
		Result resultObj = (Result) result;
		Page pageObj = (Page) page;
		resultObj.setPage(pageObj);
	}

}
