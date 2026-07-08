package com.awesomecopilot.search8x.support;

import lombok.Builder;
import lombok.Data;

/**
 * Value Count 聚合结果
 * <p>
 * Copyright: (C), 2023-08-15 12:09
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Data
@Builder
public class ValueCountAggResult {

	/**
	 * 聚合名称
	 */
	private String name;

	/**
	 * 聚合结果值(非空值数量)
	 */
	private long value;
}
