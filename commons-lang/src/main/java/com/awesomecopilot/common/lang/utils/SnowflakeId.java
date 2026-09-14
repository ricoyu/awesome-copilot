package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.common.lang.resource.YamlOps;
import com.awesomecopilot.common.lang.resource.YamlProfileReaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * <image src="images/snowflakeId.png"/>
 * <p>
 * 1位标识, 由于long基本类型在Java中是带符号的, 最高位是符号位, 正数是0, 负数是1, 所以id一般是正数, 最高位是0<p>
 * 41位时间戳(毫秒级), 注意, 41位时间戳不是存储当前时间的时间截, 而是存储时间截的差值(当前时间戳 - 开始时间戳) <p>
 * 这里的的开始时间戳, 一般是我们的id生成器开始使用的时间, 由我们程序来指定的(如下下面程序IdWorker类的startTime属性)
 * 41位的时间戳, 可以使用69年, 年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<p>
 * <p>
 * 10位的数据机器位, 可以部署在1024个节点, 包括5位datacenterId和5位workerId<p>
 * <p>
 * 12位序列, 毫秒内的计数, 12位的计数顺序号支持每个节点每毫秒(同一机器, 同一时间截)产生4096个ID序号, 加起来刚好64位, 为一个Long型。<p>
 * <p>
 * SnowFlake的优点是, 整体上按照时间自增排序, 并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分), 并且效率较高, 经测试, SnowFlake每秒能够产生330万ID左右
 */
public class SnowflakeId {
	
	private static final Logger logger = LoggerFactory.getLogger(SnowflakeId.class);
	
	/**
	 * 同JVM内自动推导实例占用的 (datacenterId, workerId) 槽位登记表, 元素 = datacenterId * 32 + workerId。
	 * 见 {@link #claimAutoSlot} 的说明。
	 */
	private static final Set<Long> AUTO_SLOTS = ConcurrentHashMap.newKeySet();
	
	/**
	 * 开始时间截 (2015-01-01)
	 */
	private final long twepoch = 1420041600000L;
	
	/**
	 * 机器id所占的位数
	 */
	private final long workerIdBits = 5L;
	
	/**
	 * 数据标识id所占的位数
	 */
	private final long datacenterIdBits = 5L;
	
	/**
	 * 支持的最大机器id, 结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
	 */
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
	
	/**
	 * 支持的最大数据标识id, 结果是31
	 */
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
	
	/**
	 * 序列在id中占的位数
	 */
	private final long sequenceBits = 12L;
	
	/**
	 * 机器ID向左移12位
	 */
	private final long workerIdShift = sequenceBits;
	
	/**
	 * 数据标识id向左移17位(12+5)
	 */
	private final long datacenterIdShift = sequenceBits + workerIdBits;
	
	/**
	 * 时间截向左移22位(5+5+12)
	 */
	private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
	
	/**
	 * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
	 */
	private final long sequenceMask = -1L ^ (-1L << sequenceBits);
	
	/**
	 * 可容忍的最大时钟回拨时间（毫秒），适配NTP同步场景
	 */
	private static final long MAX_CLOCK_BACKWARD_MS = 50L;
	
	/**
	 * 回拨幅度超过此值（毫秒）不再用虚拟时间续发, 直接抛异常。
	 * 回拨超过1分钟多半是虚机快照恢复/手动大改系统时间, 机器ID可能已被别的实例复用, 必须人工介入
	 */
	private static final long MAX_VIRTUAL_COMPENSATE_MS = 60_000L;
	
	/**
	 * 等待时钟恢复的最大超时时间（毫秒）
	 */
	private static final long MAX_WAIT_MS_FOR_CLOCK = 1000L;
	
	/**
	 * 工作机器ID(0~31)
	 */
	protected long workerId;
	
	/**
	 * 数据中心ID(0~31)
	 */
	protected long datacenterId;
	
	/**
	 * 毫秒内序列(0~4095)
	 */
	protected long sequence = 0L;
	
	/**
	 * 上次生成ID的时间截
	 */
	protected long lastTimestamp = -1L;
	
	/**
	 * 轻微回拨等待是否已经超时过。超时过说明挂钟短期不会回头, 后续取号不再重复等待1秒,
	 * 直接虚拟时间续发; 挂钟重新走上来(timestamp超过lastTimestamp)时清掉
	 */
	private boolean clockWaitTimedOut = false;
	
	//==============================Constructors=====================================
	
	/**
	 * 免配置构造器。workerId/datacenterId 按以下优先级解析:
	 * <ol>
	 * <li>JVM系统属性 -Dcopilot.snowflake.worker-id=7 (最高, 排查时可临时覆盖一切)</li>
	 * <li>环境变量 COPILOT_SNOWFLAKE_WORKER_ID (容器部署正解: K8s StatefulSet 用 ordinal 序号注入, 每实例天然唯一)</li>
	 * <li>classpath 下 application*.properties 的 copilot.snowflake.worker-id</li>
	 * <li>classpath 下 application*.yml 的 copilot.snowflake.worker-id</li>
	 * <li>都配不了才按本机IP+进程号自动推导(仅适合本地开发/单机; datacenterId 缺省为1)</li>
	 * </ol>
	 * 自动推导不保证跨机器唯一(workerId只有0~31共32个槽), 所以走这条路时会打WARN日志提醒,
	 * 且同JVM内两个自动推导实例撞同一槽位时, 后构造的会自动顺延到空闲槽位, 不会生成重复ID。
	 * <p>
	 * 位结构回顾(每部分用-分开):<br>
	 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <p>
	 * 1位符号位(ID恒为正数) + 41位时间戳差值(当前毫秒 - 构造器里的开始时间戳, 可用69年) +
	 * 5位datacenterId + 5位workerId(共1024个节点) + 12位序列号(同一节点同一毫秒4096个)。<br>
	 * 优点: 整体趋势递增, 分布式系统内不产生碰撞(由机房ID和机器ID区分), 效率高(实测每秒330万个左右)。
	 */
	public SnowflakeId() {
		String workerKey = "copilot.snowflake.worker-id";
		String datacenterKey = "copilot.snowflake.datacenter-id";
		
		PropertyReader propertyReader = new PropertyReader("application");
		YamlOps yamlOps = propertyReader.resourceExists() ? null : YamlProfileReaders.instance("application");
		if (yamlOps != null && !yamlOps.exists()) {
			yamlOps = null;
		}
		
		// 显式配置: 系统属性 > 环境变量 > properties > yml (getString在资源缺失时返回null, 可放心传入)
		Long workerId = firstLong(System.getProperty(workerKey), System.getenv("COPILOT_SNOWFLAKE_WORKER_ID"),
				propertyReader.getString(workerKey), yamlOps == null ? null : yamlOps.getString(workerKey));
		Long datacenterId = firstLong(System.getProperty(datacenterKey), System.getenv("COPILOT_SNOWFLAKE_DATACENTER_ID"),
				propertyReader.getString(datacenterKey), yamlOps == null ? null : yamlOps.getString(datacenterKey));
		
		boolean autoAssigned = false;
		if (workerId == null) {
			workerId = (long) WorkerIdGenerator.generateWorkerIdFromIp();
			autoAssigned = true;
		}
		if (datacenterId == null) {
			datacenterId = 1L;
		}
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("工作机器ID不能大于%d或小于0",
					maxWorkerId));
		}
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("数据中心ID不能大于%d或小于0",
					maxDatacenterId));
		}
		if (autoAssigned) {
			logger.warn("未显式配置 {}, 按本机IP+进程号推导出 workerId={}, datacenterId={}。自动推导不保证跨机器不重号,"
					+ " 生产环境请用系统属性 -D{}=N 或环境变量 COPILOT_SNOWFLAKE_WORKER_ID 为每个实例显式指定。",
					workerKey, workerId, datacenterId, workerKey);
			workerId = claimAutoSlot(datacenterId, workerId);
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}
	
	/**
	 * 在自动推导实例之间登记并认领 (datacenterId, workerId) 槽位: 同JVM里两个生成器共用同一编号时,
	 * 各自的序列计数器互相看不见, 同一毫秒必生成重复ID——这种必撞场景当场解决掉:
	 * 槽位已被占用则顺延到本 datacenterId 下的下一个空闲 workerId。
	 * 显式配置的实例不登记(编号由配置者负责, 且同配置多实例本就该在不同机器上)。
	 *
	 * @return 实际认领到的 workerId (可能与推导值不同, 发生顺延时会打日志)
	 */
	private long claimAutoSlot(long datacenterId, long workerId) {
		for (long probe = 0; probe <= maxWorkerId; probe++) {
			long candidate = (workerId + probe) & maxWorkerId;
			long slot = datacenterId * (maxWorkerId + 1) + candidate;
			if (AUTO_SLOTS.add(slot)) {
				if (candidate != workerId) {
					logger.warn("雪花ID自动推导撞号: datacenterId={}, workerId={} 已被本进程另一个生成器占用, 顺延到 workerId={}",
							datacenterId, workerId, candidate);
				}
				return candidate;
			}
		}
		throw new IllegalStateException(String.format(
				"datacenterId=%d 下32个workerId槽位已全部被本进程的自动推导生成器占用, 请显式配置 %s",
				datacenterId, "copilot.snowflake.worker-id"));
	}
	
	/**
	 * 按参数顺序取第一个能解析成long的配置值(用于"系统属性>环境变量>配置文件"的优先级读取),
	 * 全部为空白则返回null; 遇到非数字值记日志跳过继续向后找
	 */
	private static Long firstLong(String... candidates) {
		for (String value : candidates) {
			if (value == null || value.trim().isEmpty()) {
				continue;
			}
			try {
				return Long.valueOf(value.trim());
			} catch (NumberFormatException e) {
				logger.warn("雪花ID配置项值'{}'不是数字, 忽略并继续向后查找", value);
			}
		}
		return null;
	}

	public SnowflakeId(long workerId, long datacenterId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("工作机器ID不能大于%d或小于0",
					maxWorkerId));
		}
		if (datacenterId > maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(String.format("数据中心ID不能大于%d或小于0",
					maxDatacenterId));
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	/**
	 * 获得下一个ID (该方法是线程安全的)
	 *
	 * @return SnowflakeId
	 */
	public synchronized long nextId() {
		long timestamp = getTimeMillis();
		
		// 当前时间小于上一次生成ID的时间戳, 说明系统时钟回退过(或者上次的时间戳是挂钟被向前跳时写高的)。分三种情况处理:
		// 1) 轻微回拨(≤50ms, NTP微调的常见幅度): 短暂等待挂钟越过上次时间戳, 等待超时则降级到第2种情况继续发号;
		// 2) 回拨在1分钟内: 多数是挂钟曾被向前跳、把lastTimestamp写高后又被纠正。这时死等挂钟追上那个虚高值,
		//    生成器会停摆与跳变同样长的时间, 所以改用"虚拟时间": 从lastTimestamp续发, 时间戳在本进程内永不倒退,
		//    (时间戳, 序列)组合不会重复, 生成器立即恢复工作;
		// 3) 回拨超过1分钟: 多半是虚机快照恢复或手动大改系统时间, 机器ID可能已被别的实例占用, 进程内状态防不住
		//    跨进程的重复ID, 立刻抛异常并说清原因, 请人工检查。
		boolean virtualTimestamp = false;
		if (timestamp < lastTimestamp) {
			long timeDiff = lastTimestamp - timestamp;
			
			if (timeDiff <= MAX_CLOCK_BACKWARD_MS && !clockWaitTimedOut) {
				long waitStart = System.nanoTime();
				while (timestamp <= lastTimestamp) {
					try {
						// 短暂休眠, 避免自旋消耗CPU
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// 恢复中断标志再抛出, 不让上层线程池的关闭流程丢掉这次中断
						Thread.currentThread().interrupt();
						throw new RuntimeException("等待时钟恢复时被中断", e);
					}
					timestamp = getTimeMillis();
					
					// 等待超时(挂钟迟迟不回头): 不再抛异常停摆, 降级用虚拟时间续发, 并记住超时避免下次再等1秒
					if (System.nanoTime() - waitStart > MAX_WAIT_MS_FOR_CLOCK * 1_000_000L) {
						virtualTimestamp = true;
						clockWaitTimedOut = true;
						timestamp = lastTimestamp;
						break;
					}
				}
			}
			// 回拨幅度在可补偿范围内: 用虚拟时间续发, 不阻塞
			else if (timeDiff <= MAX_VIRTUAL_COMPENSATE_MS) {
				virtualTimestamp = true;
				timestamp = lastTimestamp;
			}
			// 严重回拨(>1分钟): 直接抛出异常, 避免重复ID
			else {
				throw new RuntimeException(
						String.format("系统时钟回拨超出容忍范围(%d毫秒), 疑似虚机快照恢复或手动改表, 拒绝生成ID, 回拨时长: %d毫秒",
								MAX_VIRTUAL_COMPENSATE_MS, timeDiff));
			}
		}
		
		//如果是同一时间生成的, 则进行毫秒内序列
		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			//毫秒内序列溢出
			if (sequence == 0) {
				//虚拟时间模式下不能等挂钟(会停摆), 直接把虚拟时间戳加1毫秒; 正常模式阻塞到下一毫秒
				timestamp = virtualTimestamp ? lastTimestamp + 1 : tilNextMillis(lastTimestamp);
			}
		}
		//时间戳改变, 毫秒内序列重置
		else {
			sequence = 0L;
			// 挂钟重新走到了lastTimestamp前面(回拨影响已过), 清掉"等待超时"标记, 下次轻微回拨还可以再等
			clockWaitTimedOut = false;
		}
		
		//上次生成ID的时间截
		lastTimestamp = timestamp;
		
		//移位并通过或运算拼到一起组成64位的ID
		return ((timestamp - twepoch) << timestampLeftShift) //
				| (datacenterId << datacenterIdShift) //
				| (workerId << workerIdShift) //
				| sequence;
	}
	
	/**
	 * 获取当前毫秒时间戳。
	 * <p/>
	 * 注意: 这是刻意的**测试接缝**(test seam), 不要被"一行转发方法应内联"的规则误伤删掉。
	 * 时钟回拨的三档处理(等待/虚拟时间续发/抛异常)必须有确定性测试, 而 System.currentTimeMillis()
	 * 不可伪造: 改系统时钟要管理员权限且CI不可行, 真实等待又慢且测不了超时降级分支。
	 * SnowflakeIdTest 用子类覆盖本方法注入假时钟, 复现向前跳/回退/停走场景。业务代码不要覆盖它。
	 *
	 * @return 当前毫秒时间戳
	 */
	protected long getTimeMillis() {
		return System.currentTimeMillis();
	}
	
	/**
	 * 阻塞到下一个毫秒, 直到获得新的时间戳
	 *
	 * @param lastTimestamp 上次生成ID的时间截
	 * @return 当前时间戳
	 */
	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = getTimeMillis();
		while (timestamp <= lastTimestamp) {
			timestamp = getTimeMillis();
		}
		return timestamp;
	}

}