package com.awesomecopilot.orm.generator;

import com.awesomecopilot.common.lang.utils.SnowflakeId;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * 雪花 ID 主键生成器。
 * <p/>
 * 内部使用 {@link SnowflakeId} 的无参构造器, workerId/datacenterId 的解析优先级:
 * 系统属性 copilot.snowflake.worker-id / copilot.snowflake.datacenter-id
 * > 环境变量 COPILOT_SNOWFLAKE_WORKER_ID / COPILOT_SNOWFLAKE_DATACENTER_ID
 * > application.properties > application yml; 全部未配置时按本机 IP 混入进程号推导
 * (见 {@code WorkerIdGenerator}, 同机多实例大概率错开槽位, 但不保证——槽位只有 32 个,
 * 容器内 PID 恒为 1 等场景仍可能撞)。多实例生产部署建议显式配置 worker-id。
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

	private static final SnowflakeId snowflakeId = new SnowflakeId();

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) {
		return snowflakeId.nextId();
	}

}
