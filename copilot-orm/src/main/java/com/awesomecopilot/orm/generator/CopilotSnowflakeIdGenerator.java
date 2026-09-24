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
 * > application.properties > application.yml (properties 与 yml 是整体二选一:
 * 存在任一 application*.properties 时 yml 不再被读取)。全部未配置时:
 * datacenterId 固定为 1, workerId 按本机 IP 混入进程号推导
 * (见 {@code WorkerIdGenerator})。自动推导的唯一性分两层: 同一 JVM 内多个推导
 * 实例由静态槽位登记强制错开(占满 32 槽抛 IllegalStateException); 跨进程/跨机器
 * 不共享该登记, 只靠 IP 尾段+进程号区分, 约 1/32 概率撞号——容器内进程号恒为 1
 * 时只剩 IP 提供区分度, 若网卡枚举失败(容器裁剪网络接口)退化为纯进程号推导,
 * 多容器会取到同一 workerId, 必撞。因此多实例生产部署请显式配置每实例唯一的
 * worker-id(超过 32 实例时并配 datacenter-id 扩位)。
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
