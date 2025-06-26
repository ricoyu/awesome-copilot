package com.awesomecopilot.orm.generator;

import com.awesomecopilot.common.lang.utils.SnowflakeId;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * workerId和datacenterId默认都为1的主键生成器
 * <p/>
 * Copyright: Copyright (c) 2025-06-25 20:22
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CopilotSnowflakeIdGenerator implements IdentifierGenerator {

	/// / 根据实际情况配置workerId和datacenterId
	private static final SnowflakeId snowflakeId = new SnowflakeId(1, 1);

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) {
		return snowflakeId.nextId();
	}

}
