package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.common.lang.resource.YamlOps;
import com.awesomecopilot.common.lang.resource.YamlProfileReaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.Cleaner;
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
	 * 槽位租约释放器: 自动推导实例被 GC 时释放文件锁并移出登记表(P2-38)。
	 * JDK Cleaner 的注册对象只被弱引用持有, 不会阻止实例回收。
	 */
	private static final Cleaner CLEANER = Cleaner.create();
	
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
	 * 回拨幅度超过此值（毫秒）不再用虚拟时间续发, 直接抛异常。
	 * 回拨超过1分钟多半是虚机快照恢复/手动大改系统时间, 机器ID可能已被别的实例复用, 必须人工介入
	 */
	private static final long MAX_VIRTUAL_COMPENSATE_MS = 60_000L;
	
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
			workerId = claimAutoSlot(datacenterId, workerId, this);
		} else {
			checkExplicitSlotCollision(datacenterId, workerId);
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}
	
	/**
	 * 在自动推导实例之间认领 (datacenterId, workerId) 槽位（P2-38 重写）。同JVM里两个
	 * 生成器共用同一编号时, 各自的序列计数器互相看不见, 同一毫秒必生成重复ID, 认领按两层做:
	 * <ol>
	 * <li><b>跨进程租约</b>：先调 {@link #acquireSlotLease} 申请槽位租约(默认是 tmp 目录
	 *     文件锁), 租不到说明同机另一个进程在用这个编号, 顺延下一个槽位——修复前只登记
	 *     JVM 内集合, 进程重启后拿同一推导值与还在跑的旧进程并行发号, 重复ID无人发现;</li>
	 * <li><b>JVM 内登记</b>：AUTO_SLOTS 挡住本进程内重复认领; 租约随实例被回收而释放
	 *     (Cleaner), 登记表同时移除——修复前登记表只进不出, 同JVM累计构造第33个自动推导
	 *     实例时无限抛 IllegalStateException。</li>
	 * </ol>
	 * 32 个槽位全部租不到时构造直接抛异常, 提示显式配置, 不带病发号。
	 * 显式配置的实例不认领登记, 但会通过 checkExplicitSlotCollision 探测同一槽位的租约,
	 * 探测不到只 WARN(编号是配置者指定的, 不能擅自顺延)。
	 * <p>
	 * 租约文件放在 java.io.tmpdir: Linux 的 /tmp 全机共享, 跨进程检测对所有账号有效;
	 * Windows 的 TEMP 按用户隔离, 不同服务账号的进程互相看不到 lock 文件, 跨账号场景
	 * 只能靠显式配置 worker-id 保证不重号。
	 *
	 * @return 实际认领到的 workerId (可能与推导值不同, 发生顺延时会打日志)
	 */
	private long claimAutoSlot(long datacenterId, long workerId, SnowflakeId owner) {
		for (long probe = 0; probe <= maxWorkerId; probe++) {
			long candidate = (workerId + probe) & maxWorkerId;
			long slot = datacenterId * (maxWorkerId + 1) + candidate;
			AutoCloseable lease = acquireSlotLease(slot);
			if (lease == null) {
				continue; // 租约被同机其它进程持有, 顺延
			}
			if (!AUTO_SLOTS.add(slot)) {
				try {
					lease.close();
				} catch (Exception ignored) {
				}
				continue; // 本JVM已占用(与租约判断等效, 防御两个构造器同时进入)
			}
			// 实例被回收时释放租约并从登记表移除(Cleaner 只弱引用 lease/slot, 不引用 owner)
			CLEANER.register(owner, new SlotReleaseAction(lease, slot));
			if (candidate != workerId) {
				logger.warn("雪花ID自动推导撞号: datacenterId={}, workerId={} 槽位不可用(本进程或其它进程已认领), 顺延到 workerId={}",
						datacenterId, workerId, candidate);
			}
			return candidate;
		}
		throw new IllegalStateException(String.format(
				"datacenterId=%d 下32个workerId槽位已全部被占用(本进程或其它进程), 请显式配置 %s",
				datacenterId, "copilot.snowflake.worker-id"));
	}

	/**
	 * 槽位释放动作: 单独 static 类, 只持有租约与槽位号——若写成 owner 的实例方法引用,
	 * lambda 会捕获 SnowflakeId 本体, 实例永远不可回收, Cleaner 就失去意义。
	 */
	private static final class SlotReleaseAction implements Runnable {
		private final AutoCloseable lease;
		private final long slot;

		SlotReleaseAction(AutoCloseable lease, long slot) {
			this.lease = lease;
			this.slot = slot;
		}

		@Override
		public void run() {
			try {
				lease.close();
			} catch (Exception e) {
				// 租约释放失败只影响该槽位复用, 不影响数据正确性
			}
			AUTO_SLOTS.remove(slot);
		}
	}
	
	/**
	 * 为一个 (datacenterId, workerId) 槽位申请**跨进程租约**（P2-38）。默认实现是
	 * java.io.tmpdir 下的独占文件锁：文件 copilot-snowflake-{slot}.lock，锁是 OS 级的，
	 * 同机另一个 Java 进程持有时本方法返回 null（租不到，调用方应顺延下一个槽位）。
	 * 租约要一直持有到生成器实例废弃——返回的 {@link AutoCloseable} 就是租约本身，
	 * 由实例的 Cleaner 释放（见 claimAutoSlot）。
	 * <p>
	 * 这是刻意保留的**测试接缝**：SnowflakeP238FixTest 覆盖它注入"哪些槽位被别的进程占用"，
	 * 不需要起第二个真实进程。文件锁机制本身不可用（受限环境/容器只读tmp）时返回一个
	 * 空租约并记 WARN——退化成修复前的"仅JVM内登记"，不让租约故障阻断发号。
	 *
	 * @param slot 槽位编号 = datacenterId * 32 + workerId
	 * @return 租约（close 即释放）；null 表示该槽位已被同机其它进程占用
	 */
	protected AutoCloseable acquireSlotLease(long slot) {
		java.nio.file.Path lockFile = java.nio.file.Paths.get(
				System.getProperty("java.io.tmpdir"), "copilot-snowflake-" + slot + ".lock");
		java.nio.channels.FileChannel channel = null;
		try {
			channel = java.nio.channels.FileChannel.open(lockFile,
					java.util.EnumSet.of(java.nio.file.StandardOpenOption.CREATE,
							java.nio.file.StandardOpenOption.WRITE));
			java.nio.channels.FileLock lock;
			try {
				lock = channel.tryLock();
			} catch (java.nio.channels.OverlappingFileLockException sameJvmConflict) {
				// 本JVM已有通道持有该文件锁(显式配置实例多实例共用同一编号时会走到这里)
				// 或按"租不到"处理, 由调用方决定顺延/告警
				channel.close();
				channel = null;
				return null;
			} catch (IOException osConflict) {
				// 打开/锁定文件本身失败(权限、只读文件系统等 OS 级故障)
				channel.close();
				channel = null;
				return null;
			}
			if (lock == null) {
				// 其它进程已持有该文件的 OS 级独占锁。双进程探针实测: Windows 与 POSIX
				// 下 tryLock 冲突都走这里返回 null(不抛异常), 调用方据此顺延
				channel.close();
				channel = null;
				return null;
			}
			final java.nio.channels.FileChannel leasedChannel = channel;
			channel = null; // 所有权移交租约 lambda, 外层异常分支不再关闭
			return () -> {
				try {
					lock.release();
				} finally {
					leasedChannel.close();
				}
			};
		} catch (Exception e) {
			// 评审修复(2026-09-28): 异常分支也要关掉已打开的 channel, 否则泄漏的 OS 句柄
			// 可能仍持有槽位锁, 该槽位对其他进程变成假占用
			if (channel != null) {
				try {
					channel.close();
				} catch (Exception ignored) {
				}
			}
			logger.warn("雪花ID槽位租约机制不可用(slot={}, {}), 退化为仅JVM内登记——"
					+ "同机多进程场景请显式配置 copilot.snowflake.worker-id", slot, e.toString());
			return () -> {
			};
		}
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
		checkExplicitSlotCollision(datacenterId, workerId);
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	/**
	 * 评审补漏(2026-09-28): 显式配置实例也探测槽位租约。修复前租约只在自动推导分支申请,
	 * 同机"显式配了 worker-id=N 的进程"与"自动推导恰好撞到 N 的进程"并行发号无人检测,
	 * 跨进程重复 ID 风险漏掉这一半。探测到冲突时不顺延、不抛异常(编号是配置者指定的,
	 * 擅自改动违反配置), 只 WARN 可能重号。租约申请到时同样交给 Cleaner, 实例回收即释放。
	 */
	private void checkExplicitSlotCollision(long datacenterId, long workerId) {
		long slot = datacenterId * (maxWorkerId + 1) + workerId;
		AutoCloseable lease = acquireSlotLease(slot);
		if (lease == null) {
			logger.warn("workerId={}, datacenterId={} 的槽位租约已被本机另一个生成器或其它进程持有, "
					+ "两边并行发号可能产生重复ID, 请检查 copilot.snowflake.worker-id 配置(同机实例必须不同编号)。",
					workerId, datacenterId);
			return;
		}
		CLEANER.register(this, new SlotReleaseAction(lease, -1L)); // -1: 只释放租约, 不碰 AUTO_SLOTS(显式实例不登记)
	}

	/**
	 * 获得下一个ID (该方法是线程安全的)
	 *
	 * @return SnowflakeId
	 */
	public synchronized long nextId() {
		long timestamp = getTimeMillis();
		
		// 当前时间小于上一次生成ID的时间戳, 说明系统时钟回退过(或者上次的时间戳是挂钟被向前跳时写高的)。分两种情况处理:
		// 1) 回拨在1分钟内: 改用"虚拟时间"续发——从lastTimestamp继续, 时间戳在本进程内永不倒退,
		//    (时间戳, 序列)组合不会重复, 生成器立即恢复工作。P2-38: 修复前对≤50ms的轻微回拨
		//    先在 synchronized 临界区内 sleep(1) 循环等挂钟回头(最长1秒), 一次轻微回拨会把全部
		//    取号线程(包括与本次无关的)在同一把锁上排队; 等待分支的收益(用真表时间)不值得这个代价, 删除。
		// 2) 回拨超过1分钟: 多半是虚机快照恢复或手动大改系统时间, 机器ID可能已被别的实例占用,
		//    进程内状态防不住跨进程的重复ID, 立刻抛异常并说清原因, 请人工检查。
		boolean virtualTimestamp = false;
		if (timestamp < lastTimestamp) {
			long timeDiff = lastTimestamp - timestamp;
			if (timeDiff <= MAX_VIRTUAL_COMPENSATE_MS) {
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
				if (virtualTimestamp) {
					timestamp = lastTimestamp + 1;
					//P1-10: 虚拟毫秒的 sequence 不从 0 起步, 从 sequenceMask/2 起步。
					//跨进程场景: 同 workerId 的另一进程从未经历时钟回拨, 挂钟真正走到这个毫秒后
					//从 0 开始发号; 本进程若也从 0 起步, 两边在同一毫秒的 sequence 空间完全重叠,
					//理论上可产出完全相同的 ID。错开一半(2048个号)后, 对端单毫秒要发到 2047 个号
					//以上才会撞进本进程用过的区间。
					sequence = (sequenceMask + 1) / 2; //2048(sequenceMask=4095, 直接/2得2047差一)
				} else {
					timestamp = tilNextMillis(lastTimestamp);
				}
			}
		}
		//时间戳改变, 毫秒内序列重置
		else {
			sequence = 0L;
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
	 * 时钟回拨的两档处理(≤60秒虚拟时间续发/>60秒抛异常)必须有确定性测试, 而 System.currentTimeMillis()
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