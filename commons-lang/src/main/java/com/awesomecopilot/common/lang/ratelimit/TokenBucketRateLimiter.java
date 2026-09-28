package com.awesomecopilot.common.lang.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * 令牌桶限流器
 * <p>
 * <b>算法思想</b>：把"请求配额"想象成往一个桶里投令牌——一个后台定时器以固定速率
 * （refillRate 个/秒）往桶里补令牌，桶最多装 capacity 个（装不下的多余配额作废丢弃）；
 * 每个请求进来要先从桶里取走 1 枚令牌，取到才放行，取不到（桶空）就拒绝。
 * <p>
 * <b>两个参数各自管什么</b>：
 * <ul>
 * <li>refillRate（补充速率）决定<b>长期平均</b>放行速率——跑得足够久，桶进出平衡，
 * 平均每秒最多过 refillRate 个请求；</li>
 * <li>capacity（桶容量）决定<b>瞬时突发</b>的上限——桶是满的时候（系统空闲了一阵，
 * 补充的令牌攒到了 capacity），一波 capacity 个请求可以一次性全部通过。
 * 这正是令牌桶比漏桶多出来的能力：漏桶的输出速率是恒定整形的，令牌桶允许攒钱消费。</li>
 * </ul>
 * <b>初始满桶</b>：构造时 tokens = capacity。含义是"服务刚启动就允许第一波 capacity
 * 个请求直接过"，这对预热/启动流量友好；如果业务要求冷启动也限速，需自行改初始值。
 * <p>
 * <b>精度说明</b>：本实现用"每 tick 兑换整数个令牌 + 毫秒余数结转"（见
 * pendingRefillMillis）把速率换算成整数令牌，长时间平均速率精确等于 refillRate；
 * 单个 tick 内可能因取整早/晚放 1 枚，误差在一个令牌一个 tick 的量级。
 * <p>
 * <b>与漏桶的选择</b>：希望"允许突发、长期限速"选令牌桶；希望"下游以完全恒定
 * 的间隔收到请求（流量整形）"选 LeakyBucketRateLimiter。
 * <p/>
 * Copyright: Copyright (c) 2025-07-25 9:02
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TokenBucketRateLimiter implements RateLimiter {

	private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

	/**
	 * 桶的最大容量（令牌数）。双重身份：
	 * <ol>
	 * <li>突发上限——桶满时允许一口气放行 capacity 个请求；</li>
	 * <li>计数器封顶——每次补充令牌都以 capacity 为上界做封顶累加
	 * （accumulateAndGet 里 updated &gt; capacity 就取 capacity），
	 * 系统空闲时攒出来的多余配额被丢弃，不允许"无限存钱"。</li>
	 * </ol>
	 */
	private final long capacity;          // 桶的最大容量 (令牌数)

	/**
	 * 当前桶中可用的令牌数。所有请求线程通过 CAS 从这里扣减（canPass），
	 * 后台补令牌线程通过原子累加往这里加（refillTokens），两侧都不加锁，
	 * 靠 AtomicInteger 级原子操作保证不超卖、不少记。
	 */
	private final AtomicLong tokens;      // 当前桶中的令牌数量

	/**
	 * 每秒补充的令牌数，即长期平均放行速率。
	 * 与 refillIntervalMs 组合换算每个 tick 应补多少（refillRate 个/秒 × 间隔秒数）。
	 */
	private final long refillRate;        // 每秒补充的令牌数

	/**
	 * 补令牌定时任务的执行间隔（毫秒）。间隔越短，令牌投放越平滑
	 * （capacity 很大、refillRate 很高时，大间隔会让桶瞬间充满又瞬间抽干，
	 * 突发窗口变粗）；代价是调度开销。默认用法 1000ms 量级足够。
	 */
	private final long refillIntervalMs;  // 补充令牌的时间间隔(毫秒)

	/**
	 * 补令牌的单线程调度器。线程为 daemon（P2-3：修复前默认非 daemon 线程，
	 * 使用者忘调 shutdown() 会挂住 JVM 退不出去），命名 token-bucket-refill 便于排查。
	 */
	private final ScheduledExecutorService scheduler;

	/**
	 * 毫秒配额池：把"应补速率"和"整数令牌"解耦的余数账本。
	 * <p>
	 * 每个 tick 应补的令牌数 = refillRate × refillIntervalMs ÷ 1000（毫秒→令牌换算），
	 * 换算不尽的毫秒余额不能丢——丢了就等价于长期少放令牌。所以每个 tick 先把
	 * refillRate × refillIntervalMs 毫秒记进这里，兑换整数个令牌后，只扣掉
	 * "真正兑成令牌"的那部分（tokensToAdd × 1000），余数结转给下一个 tick。
	 * <p>
	 * 例：refillRate=3、interval=1000ms → 每 tick 记 3000ms、兑 3 枚、余额恒 0；
	 * refillRate=1、interval=2500ms → 每 tick 记 2500ms、第一轮兑 2 枚留 500ms、
	 * 第二轮凑满 1000ms 多兑 1 枚，长期平均精确 1 枚/秒。
	 * <p>
	 * 只被 scheduler 单线程的 refillTokens 读写，无需同步。
	 */
	private long pendingRefillMillis;     // 累积未兑换成令牌的毫秒配额（定时任务单线程访问）

	/**
	 * 令牌桶限流器（初始为满桶）
	 *
	 * @param capacity         桶的最大容量 (令牌数)，即允许的突发请求数，须 &gt; 0
	 * @param refillRate       每秒补充的令牌数，即长期平均放行速率，须 &gt; 0
	 *                         （P2-33：修复前无校验，refillRate≤0 时令牌只出不进，
	 *                         桶空后全量拒绝且无任何告警）
	 * @param refillIntervalMs 补充令牌的时间间隔(毫秒)，须 &gt; 0
	 */
	public TokenBucketRateLimiter(long capacity, long refillRate, long refillIntervalMs) {
		//P2-33: capacity/refillRate 任一 ≤0 都让限流器失去意义（永远拒绝或永远不补），
		//在构造期就抛出来，不让它带着错误配置上线。
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		if (refillRate <= 0) {
			throw new IllegalArgumentException("refillRate must be positive");
		}
		if (refillIntervalMs <= 0) {
			throw new IllegalArgumentException("refillIntervalMs must be positive");
		}
		this.capacity = capacity;
		this.tokens = new AtomicLong(capacity); // 初始时桶是满的
		this.refillRate = refillRate;
		this.refillIntervalMs = refillIntervalMs;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			//P2-3: 修复前默认线程工厂是非 daemon, 忘记 shutdown() 会挂住 JVM 退不出去
			Thread t = new Thread(r, "token-bucket-refill");
			t.setDaemon(true);
			return t;
		});

		// 启动定时任务：按固定速率补充令牌
		scheduler.scheduleAtFixedRate(
				this::refillTokens,
				this.refillIntervalMs,
				this.refillIntervalMs,
				TimeUnit.MILLISECONDS
		);
	}

	/**
	 * 补充令牌（由定时任务每个 refillIntervalMs 调用一次），逻辑两步：
	 * <ol>
	 * <li>把本轮新增的毫秒配额记进 pendingRefillMillis，兑换成整数令牌数
	 * tokensToAdd = 配额池 ÷ 1000，池子里只扣走已兑换的部分（余数结转，见字段注释）；</li>
	 * <li>用 accumulateAndGet 把 tokensToAdd 原子累加进桶并以 capacity 封顶：
	 * 与并发的扣减（canPass 的 CAS）互不丢账。</li>
	 * </ol>
	 * P2-3 修复说明：修复前是 get → 算新值 → compareAndSet，CAS 失败（恰有请求线程
	 * 同时扣减）就直接 return，而 tokensToAdd 已从配额池扣掉——本轮增量作废。
	 * 实测（高水位 8 消费者压测）46 万注入丢失 1~2 万令牌，实际放行速率低于配置值。
	 * 改为 accumulateAndGet 后，无论并发怎么扣减，增量都完整入账。
	 * <p>
	 * 任务体内不抛异常是硬要求：scheduleAtFixedRate 遇到未捕获异常会取消后续执行，
	 * 桶将永不再补令牌（表现为"运行一段时间后流量全拒"），所以这里全程只用原子操作。
	 */
	private void refillTokens() {
		pendingRefillMillis += refillRate * refillIntervalMs;
		long tokensToAdd = pendingRefillMillis / 1000;
		if (tokensToAdd <= 0) {
			return; //本轮不足 1 枚, 全部配额留到池里结转
		}
		//只扣除真正兑换成令牌的那部分毫秒配额(保持整除关系), 尾数留在池子里下次再兑
		pendingRefillMillis -= tokensToAdd * 1000;
		tokens.accumulateAndGet(tokensToAdd, (current, add) -> {
			long updated = current + add;
			//current<=capacity 恒成立(入口满桶且每次累加都封顶), capacity 受构造校验为正数,
			//相加溢出时结果必然远超 capacity, 收敛到上限即可, 不需要单独判溢出
			return updated > capacity ? capacity : updated;
		});
		if (log.isDebugEnabled()) {
			log.debug("[Refill] Tokens: {}", tokens.get()); // 调试日志
		}
	}

	/**
	 * 尝试获取一个令牌（请求放行的唯一入口），自旋 CAS 扣减：
	 * <ol>
	 * <li>读当前令牌数，≤0 立即返回拒绝——不排队、不等待,
	 * 这是与漏桶"排队等处理"的本质区别；</li>
	 * <li>compareAndSet 扣 1：成功即放行；失败说明有别的线程刚改过计数
	 * （可能又扣了也可能刚补了），回到第 1 步重读重试。</li>
	 * </ol>
	 * 循环不会空转失控：每轮重试要么 CAS 成功、要么桶被补令牌线程抬高、
	 * 要么被消费线程扣到 0（下一轮直接拒绝），三种进展都让循环走向出口。
	 *
	 * @return true表示获取成功（请求允许通过），false表示失败（被限流）
	 */
	public boolean canPass() {
		while (true) {
			long currentTokens = tokens.get();
			if (currentTokens <= 0) {
				return false; // 令牌不足，拒绝请求
			}
			// 尝试扣减令牌（CAS操作保证线程安全）
			if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
				return true;
			}
			// 如果CAS失败，说明其他线程修改了令牌数，重试; 重试是通过上面这个while循环实现的
		}
	}

	/**
	 * 关闭限流器（释放资源）：停掉补令牌调度线程。幂等，重复调用无害。
	 * 关闭后 tokens 计数仍在内存里，但桶不会再补令牌，canPass 在令牌耗尽后恒 false。
	 */
	public void shutdown() {
		scheduler.shutdown();
	}

	/**
	 * 释放补令牌用的调度线程; 与 shutdown() 同义, 让 try-with-resources 可用（P2-3）。
	 */
	@Override
	public void close() {
		shutdown();
	}
}
