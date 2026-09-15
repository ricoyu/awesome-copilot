package com.awesomecopilot.bloom;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guava BloomFilter（布隆过滤器，一种用很少的位内存判断"元素可能在不在这个集合里"的概率型数据结构）
 * 的系统性单元测试，基于本仓库锁定的 guava 32.0.0-jre 实测行为编写。
 * <p>
 * 先说清楚布隆过滤器的两条铁律，本类的用例全部围绕它们展开：
 * <p>
 * 第一条铁律：说"不存在"就一定不存在。因为 put 的时候会把若干位(bit)置成 1，
 * mightContain 检查这些位是不是全为 1。只要有一位还是 0，说明这个元素从来没被放进去过
 * （放任何元素都只会把 0 变 1，不会把 1 变 0），所以"不存在"的回答是百分之百可信的。
 * <p>
 * 第二条铁律：说"存在"可能是骗你的。别的元素 put 时可能恰好把这些位也置成了 1，
 * 于是新来一个从没放过的元素，一查位全是 1，过滤器就说"存在"——这就是误判
 * （英文叫 false positive，false 是"错误的"，positive 是"判定为存在"，合起来就是
 * "错误地报告说存在"）。创建过滤器时可以指定能容忍多大的误判概率，Guava 会据此反推
 * 需要多少位内存、每个元素哈希几轮。
 * <p>
 * 覆盖的 API：create 的各种重载、put/mightContain/apply、putAll/isCompatible、copy、
 * expectedFpp、approximateElementCount、writeTo/readFrom、Java 原生序列化、
 * BloomFilter.toBloomFilter 流收集器，以及 Funnels 的 integerFunnel/stringFunnel/
 * unencodedCharsFunnel/sequentialFunnel。
 * <p>
 * Copyright: (C), 2026-09-15
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class GuavaBloomFilterTest {

	/**
	 * 铁律一的直接验证：放进去的元素, mightContain 永远返回 true, 一百次也不会有例外。
	 * <p>
	 * 布隆过滤器没有任何"删除后查不到"或者"存了却查不出来"的路径——put 置位之后,
	 * 那些位在元素活着的整个周期里不可能自己变回 0（Guava 这个版本连 clear 方法都没有,
	 * 想清空只能重建或者反序列化一份新的）。所以只要元素真的 put 进去了,
	 * 检查时它自己那几位必然全是 1, 判定必然为"存在"。这就是常说的
	 * "布隆过滤器没有漏报, 只有误报"。
	 * <p>
	 * 顺带验证 apply：BloomFilter 实现了 Guava 自己的 Predicate（断言接口）,
	 * apply 就是 mightContain 的转发, 行为完全一致。
	 */
	@Test
	public void testNoFalseNegativesForInsertedElements() {
		BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 10_000, 0.01);
		for (int i = 0; i < 1_000; i++) {
			filter.put(i);
		}
		for (int i = 0; i < 1_000; i++) {
			// 同一个元素反复查 5 次, 每次都必须报"存在"
			for (int j = 0; j < 5; j++) {
				assertThat(filter.mightContain(i)).isTrue();
			}
			// apply 与 mightContain 等价
			assertThat(filter.apply(i)).isTrue();
		}
	}

	/**
	 * 负数、0 这些边界数值 key 也能正常存取。
	 * <p>
	 * integerFunnel（把 Integer 喂给哈希函数的"漏斗"）最终是把 int 的原始 4 个字节写进哈希器,
	 * 补码表示的负数和 0 只是普通字节组合, 不存在特殊分支。所以 -5 存进去查得出来,
	 * 而 -5 和 5 的字节不同, 各自算各自的位, 互不串味。
	 */
	@Test
	public void testNegativeAndZeroKeysWork() {
		BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 10, 0.01);
		filter.put(-5);
		filter.put(0);

		assertThat(filter.mightContain(-5)).isTrue();
		assertThat(filter.mightContain(0)).isTrue();
		// 没放过的 5 大概率查不到（过滤器很空, 位几乎全是 0, 误报概率极低）
		assertThat(filter.mightContain(5)).isFalse();
	}

	/**
	 * put 的返回值可以当作"这个元素第一次出现"的标志来用。
	 * <p>
	 * 实测：第一次 put(1) 返回 true（本次调用新置了位, 相当于"集合里多了一个成员"）,
	 * 第二次再 put(1) 返回 false（它要置的那几位早就是 1 了, 这次什么也没改变）。
	 * 注意这个"true/false"只对"没放过的元素"百分之百可靠：万一两个不同元素的哈希位恰好重叠,
	 * 第二个元素首次 put 时位可能已经被第一个置满, 也会返回 false——所以它不能替代
	 * 精确去重, 只能当作概率性的"大概率第一次见"提示。
	 * <p>
	 * 同时验证 approximateElementCount（按位被置起的比例反推出来的元素数估计值）:
	 * 重复 put 同一个元素不会让它虚增, 三个不同元素报 3。
	 */
	@Test
	public void testPutReturnValueAndApproximateCount() {
		BloomFilter<String> filter = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.01);

		assertThat(filter.put("apple")).isTrue();   // 首次放入
		assertThat(filter.put("apple")).isFalse();  // 重复放入, 没有新位置起
		filter.put("banana");
		filter.put("cherry");

		assertThat(filter.approximateElementCount()).isEqualTo(3);
	}

	/**
	 * expectedFpp 是过滤器自己报的"当前误判概率"（expected false positive probability,
	 * 即按现在已置起的位的比例推算, 下一个全新元素被误报成"存在"的概率）。
	 * <p>
	 * 它随着 put 单调上涨, 这条测试把它在三个阶段的量级钉下来, 数字均为本机实测值：
	 * <p>
	 * 阶段一, 刚建好还没放元素：所有位都是 0, 全新元素必然有 0 位可查, 误判概率严格为 0。
	 * 阶段二, 设计容量 500 只放了 3 个：位数组几乎空着, 误判概率低到 3.07E-17,
	 * 比设计值 1% 小十几个数量级——说明 1% 是"装满之后的上限承诺", 没装满时远好于它。
	 * 阶段三, 设计容量 10 万、真放了 5 万（一半）：实测查 1 万个全新元素只有 2 个被误报
	 * （0.02%）, 此时 expectedFpp 报 0.025%, 和实测同量级、略有偏差——expectedFpp
	 * 是按"位被置起的比例"做的数学估算, 不是数出来的真实值, 两者接近但不可能完全相等。
	 */
	@Test
	public void testExpectedFppGrowsFromZeroToDesignValue() {
		BloomFilter<Integer> empty = BloomFilter.create(Funnels.integerFunnel(), 500, 0.01);
		assertThat(empty.expectedFpp()).isEqualTo(0.0);

		empty.put(1);
		empty.put(2);
		empty.put(3);
		// 500 容量只放 3 个, 误判概率远小于 1%（实测约 3.07E-17）
		assertThat(empty.expectedFpp()).isGreaterThan(0.0).isLessThan(0.000001);

		BloomFilter<Integer> halfLoaded = BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.01);
		for (int i = 0; i < 50_000; i++) {
			halfLoaded.put(i);
		}
		// 装一半时, 自报的误判概率还在往 1% 爬（实测约 0.00025）
		assertThat(halfLoaded.expectedFpp()).isBetween(0.0001, 0.001);
	}

	/**
	 * 核心用例：按设计规格装满之后, 真实误判率确实贴着承诺的 1%。
	 * <p>
	 * 建一个"预计 10 万个元素、容忍 1% 误判"的过滤器, 把 0~99999 全部 put 进去；
	 * 然后拿 100000~199999 这 10 万个从来没放进去的整数去查, 数一数有多少个被误报成"存在"。
	 * 本机实测：100000 个查询里 1018 个误报, 真实误判率 1.018%, 和设计的 1% 只差一丝。
	 * 之所以略超一点点, 是因为哈希并非理论上的完美随机, 1018 这个数也会随查询区间微调,
	 * 所以断言放宽到 0.8%~1.3% 区间, 既能证明"确实在 1% 附近", 又不会哪天换个查询区间就红。
	 * <p>
	 * 同时对比 expectedFpp：自报 1.0054%, 与实测 1.018% 吻合在同一量级。
	 * <p>
	 * 反过来看漏报方向：0~99999 全部必须查得到, 一个都不能少——这条同时守住了第一条铁律。
	 */
	@Test
	public void testMeasuredFalsePositiveRateMatchesDesignOnePercent() {
		BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.01);
		for (int i = 0; i < 100_000; i++) {
			filter.put(i);
		}

		// 已放入的绝不漏报
		for (int i = 0; i < 100_000; i++) {
			assertThat(filter.mightContain(i)).isTrue();
		}

		// 从没放入的 10 万个整数, 统计误报个数
		int falsePositives = 0;
		for (int i = 100_000; i < 200_000; i++) {
			if (filter.mightContain(i)) {
				falsePositives++;
			}
		}
		double measuredFpp = falsePositives / 100_000.0;
		// 实测 1.018%, 断言放宽为区间防止哈希分布的微小波动引起偶发失败
		assertThat(measuredFpp).isBetween(0.008, 0.013);
		// 自报值与实测值同量级（实测 0.010054）
		assertThat(filter.expectedFpp()).isBetween(0.008, 0.013);
		// 计数估计值也应接近真实的 10 万（实测 100033, 偏差约万分之三）
		assertThat(filter.approximateElementCount()).isBetween(95_000L, 105_000L);
	}

	/**
	 * 反面教材：实际放入量远超声明的"预期元素数量"时, 误判率承诺彻底作废。
	 * <p>
	 * 声明容量只有 1000, 却硬塞了 10 万个元素。位数组总共才千把字节, 10 万次 put
	 * 把几乎每一位都置成了 1——这时拿 1 万个全新的、从没放过的整数去查, 实测 100% 全部
	 * 被误报成"存在", expectedFpp 也爬到 1.0（它的意思就是"谁来都说过"）。
	 * 布隆过滤器的内存是按"预期元素数 × 每元素位数"一次性算好的, 塞超了不会自动扩容,
	 * 只会无声地 degrade（变差）, 所以业务上元素总量会增长时, 容量要按最终规模的峰值报,
	 * 或者定期重建。
	 * <p>
	 * 附送一个实测坑：超载到这种程度后, 调 approximateElementCount 不再返回估计值,
	 * 而是直接抛 ArithmeticException（"input is infinite or NaN"）——它的估算公式里有
	 * ln(1 - 置位比例) 这一项, 位被置满时 1-比例 趋于 0, ln(0) 是负无穷, double 运算
	 * 传给 Guava 的整型转换就直接炸出异常了。也就是说"数都数不出来"本身就是过滤器
	 * 已经饱和的强烈信号, 生产代码里不要裸调这个方法。
	 */
	@Test
	public void testOverSaturationBreaksFppGuarantee() {
		BloomFilter<Integer> over = BloomFilter.create(Funnels.integerFunnel(), 1_000, 0.01);
		for (int i = 0; i < 100_000; i++) {
			over.put(i);
		}

		// 已放入的仍然全部查得到（漏报永远没有, 饱和也不能破坏第一条铁律）
		assertThat(over.mightContain(0)).isTrue();
		assertThat(over.mightContain(99_999)).isTrue();

		// 全新的 key 无一幸免, 全部被误报（实测 10000 个查询 100% 误报）
		int falsePositives = 0;
		for (int i = 200_000; i < 210_000; i++) {
			if (over.mightContain(i)) {
				falsePositives++;
			}
		}
		assertThat(falsePositives).isEqualTo(10_000);
		assertThat(over.expectedFpp()).isEqualTo(1.0);

		// 饱和后连元素数估计都算不出来, 直接抛 ArithmeticException
		assertThatThrownBy(over::approximateElementCount)
				.isInstanceOf(ArithmeticException.class)
				.hasMessageContaining("infinite");
	}

	/**
	 * 误判率、内存、二者之间的换算关系, 用实测字节数说话。
	 * <p>
	 * 布隆过滤器的位数组大小公式是 m = -n·ln(p)/(ln2)²（n 是预期元素数, p 是容忍误判率,
	 * ln 是自然对数）, 换成 p 为底的幂看就是个等比关系：p 每缩小到十分之一, m 大约乘
	 * 1.44（1/ln2 的平方相关系数）。所以下面三个 10 万元素的过滤器, 序列化后的实际字节数
	 * （含几十字节的头部元数据）是：
	 * <pre>
	 *   p=0.1   →  59,918 字节 ≈ 58 KB
	 *   p=0.01  → 119,822 字节 ≈ 117 KB（比上一档多一倍）
	 *   p=0.001 → 179,726 字节 ≈ 175 KB（比 0.01 档再多半成品）
	 * </pre>
	 * 这组数字的工程含义：想把误判率从 1% 压到千分之一, 内存要多花约 50%；
	 * 反过来 1% 放宽到 10%, 能省掉一半内存。选型时别拍脑袋"越小越好",
	 * 要拿"误判的代价"换"内存的代价"来比。比如拿布隆过滤器挡数据库查询,
	 * 1% 的误判只是让百分之一的不存在主键白跑一趟数据库, 数据库完全扛得住,
	 * 那就没必要为千分之一付 1.5 倍内存。
	 * <p>
	 * 断言不钉死精确字节数（guava 升级时头部格式可能微调）, 只钉实测得到的比例关系：
	 * 0.01 档约是 0.1 档的两倍, 0.001 档约是 0.01 档的一倍半。
	 */
	@Test
	public void testMemoryGrowsAsFalsePositiveRateShrinks() throws IOException {
		int bytes10 = serializeSize(BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.1));
		int bytes1 = serializeSize(BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.01));
		int bytes01 = serializeSize(BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.001));

		// 0.1 → 0.01: 实测 59,918 → 119,822, 恰好翻倍
		assertThat(bytes1).isGreaterThan((int) (bytes10 * 1.9)).isLessThan(bytes10 * 3);
		// 0.01 → 0.001: 实测 119,822 → 179,726, 涨约 1.5 倍
		assertThat(bytes01).isBetween((int) (bytes1 * 1.4), (int) (bytes1 * 1.6));
	}

	/**
	 * create(funnel, expectedInsertions) 两参重载：不指定误判率, 走 Guava 的默认值 3%。
	 * <p>
	 * 没装元素时 expectedFpp 恒为 0, 看不出默认值；放 1 个元素后再看自报概率:
	 * 若默认误判率真是 3%（按 100 容量算一轮位数）, 放 1 个之后的自报值应该远小于 3%
	 * 但明显大于 3 参重载那种精配小数值场景的量级。本机实测: 100 容量放 1 个,
	 * 两参版自报 1.17E-11, 与三参版传 0.01 时同量级——默认 3% 只影响"位数组开多宽",
	 * 空载时都低到可忽略。这条测试主要验证两参重载可用、行为与三参一致地单调,
	 * 顺带记录"不传第三个参数 Guava 默认按 3% 误判率配置"这一事实。
	 */
	@Test
	public void testTwoArgCreateUsesDefaultFpp() {
		BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 100);
		assertThat(filter.expectedFpp()).isEqualTo(0.0);
		filter.put(7);
		assertThat(filter.mightContain(7)).isTrue();
		assertThat(filter.expectedFpp()).isBetween(0.0, 0.03);
	}

	/**
	 * create 的参数校验：边界值当场拒绝, 报错信息说人话。
	 * <p>
	 * 误判率要求开区间 (0, 1)：传 0 拒绝（消息 "False positive probability (0.0) must be
	 * > 0.0"）, 传 1 也拒绝（"must be < 1.0"）。为什么 0 和 1 都不行——0 意味着要求
	 * 零误判, 布隆过滤器这种结构根本做不到, 会算出无穷大的位数组; 1 意味着"反正随便猜",
	 * 位数组算出来是 0 位, 建出来也没有意义。
	 * <p>
	 * 预期元素数要求 >= 0：传 -1 拒绝（"Expected insertions (-1) must be >= 0"）。
	 * 但注意 0 是放行的：n=0 算出来的位数组是 0 位宽, Guava 照样给你建一个"空壳过滤器",
	 * 之后再往里 put 也不报错, 元素也真能查出来（它把位数组撑到了最小可用宽度）,
	 * 只是此时的一切概率承诺都不复存在。实测 n=0 建完 put 一个元素, mightContain
	 * 对该元素返回 true、对另一个新元素返回 false, 看着挺正常——这正是"不校验反而埋雷",
	 * 业务代码里预期数量应当来自真实统计, 别偷懒传 0。
	 */
	@Test
	public void testCreateArgumentsAreValidated() {
		assertThatThrownBy(() -> BloomFilter.create(Funnels.integerFunnel(), 100, 0.0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be > 0.0");
		assertThatThrownBy(() -> BloomFilter.create(Funnels.integerFunnel(), 100, 1.0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be < 1.0");
		assertThatThrownBy(() -> BloomFilter.create(Funnels.integerFunnel(), -1, 0.01))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be >= 0");

		// n=0 不报错, 但这是个陷阱: 建出来的过滤器没有按规格预留内存, 概率承诺全部作废
		BloomFilter<Integer> zero = BloomFilter.create(Funnels.integerFunnel(), 0, 0.01);
		zero.put(1);
		assertThat(zero.mightContain(1)).isTrue();
	}

	/**
	 * writeTo/readFrom 序列化往返：10 万元素的过滤器写出约 117 KB 字节, 读回来
	 * 与原对象 equals 相等、approximateElementCount 一致, 且读回来的对象照常能查。
	 * <p>
	 * 这个往返的价值在于"共享与预热"：布隆过滤器可以离线构建（比如启动时从数据库
	 * 拉全量黑名单）, 序列化成文件推到各个应用节点, 各节点 readFrom 加载, 省掉
	 * 每台机器各自灌数据的重复开销。equals 比较的就是位数组本身加哈希轮数,
	 * 所以往返后 equals 为 true 说明序列化没有丢任何位信息。
	 * <p>
	 * readFrom 需要重新传 Funnel（漏斗, 即"怎么把对象拆成字节喂给哈希函数"）：
	 * 序列化的字节流里只存了位数组和哈希轮数, 没有存 Funnel 和字符集——因为 Funnel
	 * 是函数不是数据, 没法随流传输, 读的时候必须口头约定"用什么方式算哈希"。
	 * 这个约定就是下一条测试踩坑的根源。
	 */
	@Test
	public void testWriteToReadFromRoundTripKeepsContent() throws IOException {
		BloomFilter<Integer> original = BloomFilter.create(Funnels.integerFunnel(), 100_000, 0.01);
		for (int i = 0; i < 100_000; i++) {
			original.put(i);
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		original.writeTo(bos);
		// 10 万 @ 1% 实测 119,822 字节（约 117 KB）
		assertThat(bos.size()).isBetween(115_000, 125_000);

		BloomFilter<Integer> restored = BloomFilter.readFrom(
				new ByteArrayInputStream(bos.toByteArray()), Funnels.integerFunnel());

		assertThat(restored).isEqualTo(original);
		assertThat(restored.hashCode()).isEqualTo(original.hashCode());
		assertThat(restored.approximateElementCount()).isEqualTo(original.approximateElementCount());
		assertThat(restored.mightContain(99_999)).isTrue();   // 放过的查得到
		assertThat(restored.mightContain(123_456_789)).isFalse(); // 没放过的不报
	}

	/**
	 * 大坑测试：writeTo/readFrom 的字节流里不记录字符集, 写入端和读取端必须约好同一个
	 * Charset, 否则中文（任何非纯 ASCII 字符串）会"查无此人"。
	 * <p>
	 * 复现过程：用 GBK 字符集的 stringFunnel 建过滤器并放入"中文字符串"——GBK 把这三个字
	 * 编码成 6 个字节后哈希置位。把这枚过滤器 writeTo 成字节流, 再 readFrom 回来时
	 * 手里换成了 UTF-8 的 funnel。读回来的位数组还是那份位数组, 但查"中文字符串"时
	 * 是拿 UTF-8 编码的 9 个字节重新算哈希位置——UTF-8 算出的位和 GBK 当初置的位
	 * 对不上, mightContain 返回 false。明明放进去过的中文串, 查出来却说"不存在",
	 * 直接违反第一条铁律的直觉, 而且全程没有任何报错。
	 * <p>
	 * 为什么英文字符串躲得过这一劫：ASCII 字符在 GBK 和 UTF-8 里编码字节完全相同,
	 * 哈希自然一致, 所以测试环境里用英文名试一下没问题, 上线碰上中文就翻车——
	 * 这种"英文正常、中文失灵"的 bug 最难查。用 unencodedCharsFunnel（按 UTF-16
	 * 字符单元直接喂哈希, 不经过字节编码）则与字符集无关, 也是解法之一。
	 * <p>
	 * 结论：跨进程/跨语言传布隆过滤器, 协议文档里必须白纸黑字写清 funnel 的字符集。
	 */
	@Test
	public void testCharsetMismatchBreaksLookupAfterDeserialize() throws IOException {
		BloomFilter<String> gbkBuilt = BloomFilter.create(
				Funnels.stringFunnel(Charset.forName("GBK")), 10_000, 0.0001);
		gbkBuilt.put("中文字符串");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		gbkBuilt.writeTo(bos);

		// 读取端错用了 UTF-8 的 funnel: 位数组没变, 但中文串算出的哈希位对不上
		BloomFilter<String> utf8Read = BloomFilter.readFrom(
				new ByteArrayInputStream(bos.toByteArray()),
				Funnels.stringFunnel(StandardCharsets.UTF_8));
		assertThat(utf8Read.mightContain("中文字符串")).isFalse();

		// 字符集一致时读取就完全正常
		BloomFilter<String> gbkRead = BloomFilter.readFrom(
				new ByteArrayInputStream(bos.toByteArray()),
				Funnels.stringFunnel(Charset.forName("GBK")));
		assertThat(gbkRead.mightContain("中文字符串")).isTrue();
	}

	/**
	 * BloomFilter 同时实现了 java.io.Serializable, 走 Java 原生序列化也能完整往返。
	 * <p>
	 * 与 writeTo 殊途同归：put(42) 后 ObjectOutputStream 写出、ObjectInputStream 读回,
	 * 42 查得到, 且读回对象与原对象 equals 相等。两条路选一条即可——writeTo 的格式
	 * 是 Guava 自定义的紧凑格式（跨版本、跨语言生态更友好）, 原生序列化则方便直接
	 * 塞进任何接受 Serializable 的缓存/消息组件。
	 */
	@Test
	public void testJavaNativeSerializationRoundTrip() throws IOException, ClassNotFoundException {
		BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01);
		filter.put(42);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(filter);
		}
		BloomFilter<Integer> restored;
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
			restored = (BloomFilter<Integer>) ois.readObject();
		}

		assertThat(restored.mightContain(42)).isTrue();
		assertThat(restored).isEqualTo(filter);
	}

	/**
	 * putAll 做并集合并 + isCompatible 先做兼容性检查。
	 * <p>
	 * 两个过滤器各自装了不同的词, a.putAll(b) 之后 a 变成"两个集合的并"——hello 和 world
	 * 都能查出来。合并的本质是两份位数组按位取或, 所以只可能增加置起的位, 原有的"存在"
	 * 判定一个都不会丢。
	 * <p>
	 * isCompatible 检查的是三件事是否全部一致：位数组宽度、哈希轮数、Funnel 类型。
	 * 实测：同为"1000 容量 1% 误判"的两个 stringFunnel 过滤器 compatible 为 true；
	 * 把容量改成 500（位数组宽度不同）就为 false, 而且直接 putAll 会抛
	 * IllegalArgumentException——合并前不自己检查的话, 运行时异常会直接砸在脸上,
	 * 这就是官方提供 isCompatible 的意义。
	 * <p>
	 * 更隐蔽的一种不兼容：容量和误判率声明都一样, 但一个是 32 位哈希策略一个是 64 位
	 * 策略（create 内部按预期元素数自动选, 小集合用 32 位版）, 或者 funnel 一个 UTF-8
	 * 一个 GBK——equals/putAll 比较的都是算好的位, 策略不同位就不同, 强行合并只会得到
	 * 一份"两边话都听不懂"的位数组。所以合并不同来源的过滤器之前, isCompatible 是必查项。
	 */
	@Test
	public void testPutAllMergesAndIsCompatibleGuards() {
		BloomFilter<String> a = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 1_000, 0.01);
		BloomFilter<String> b = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 1_000, 0.01);
		a.put("hello");
		b.put("world");

		assertThat(a.isCompatible(b)).isTrue();
		a.putAll(b);
		assertThat(a.mightContain("hello")).isTrue();  // 自己的还在
		assertThat(a.mightContain("world")).isTrue();  // 对方的并进来
		assertThat(a.approximateElementCount()).isEqualTo(2);

		// 规格不同的两个过滤器: isCompatible 说 false, putAll 直接抛异常
		BloomFilter<String> differentSize = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 500, 0.01);
		assertThat(a.isCompatible(differentSize)).isFalse();
		assertThatThrownBy(() -> a.putAll(differentSize)).isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * copy 是深拷贝, 改动副本不影响原对象。
	 * <p>
	 * 场景是"公共基线 + 各自试算"：比如拿一份装好全量黑名单的过滤器作为只读基线,
	 * 每个请求线程 copy 一份出来往里 put 本次候选元素做试验, 谁也不能污染公共基线。
	 * 实测：原过滤器只有 1, copy 之后往副本里放 2——原过滤器查 2 仍是 false,
	 * 副本查 2 为 true, 两个对象 equals 也不再相等（位数组内容已经不同）。
	 */
	@Test
	public void testCopyIsIndependentFromOriginal() {
		BloomFilter<Integer> original = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01);
		original.put(1);

		BloomFilter<Integer> copy = original.copy();
		copy.put(2);

		assertThat(original.mightContain(2)).isFalse(); // 原对象没被副本污染
		assertThat(copy.mightContain(2)).isTrue();
		assertThat(copy.mightContain(1)).isTrue();      // 副本带着原对象的全部数据
		assertThat(copy).isNotEqualTo(original);
	}

	/**
	 * Stream 收集器 BloomFilter.toBloomFilter：把一条流直接收集成过滤器,
	 * 免去手工 forEach(put)。产出的就是普通 BloomFilter 实例, 后续照常
	 * put/mightContain/序列化。
	 * <p>
	 * 断言里故意避开 approximateElementCount：实测容量声明 10、真实放了 3 个元素时,
	 * 它报 2 而不是 3。这个方法是按"置起的位的比例"反解方程估出来的, 位数组很小时
	 * 一个元素的几位就把总宽度的比例推偏了, 估计值明显低估; 容量上百之后才基本准确
	 * （前面 100 容量的测试里 3 个元素就报 3）。所以小过滤器上的这个数只能看个量级,
	 * 别拿它做业务计数。
	 */
	@Test
	public void testStreamToBloomFilterCollector() {
		BloomFilter<Integer> filter = Arrays.stream(new Integer[]{1, 2, 3})
				.collect(BloomFilter.toBloomFilter(Funnels.integerFunnel(), 10, 0.01));

		assertThat(filter.mightContain(1)).isTrue();
		assertThat(filter.mightContain(2)).isTrue();
		assertThat(filter.mightContain(3)).isTrue();
		assertThat(filter.mightContain(99)).isFalse();
	}

	/**
	 * sequentialFunnel：给"可迭代容器"做漏斗, 把集合逐元素摊平后一起哈希,
	 * 让过滤器判断的是"整个列表有没有出现过", 而不是列表里的单个元素。
	 * <p>
	 * 实测三个现象, 正好刻画出它的语义：
	 * ① 放进去 [1,2,3], 再拿内容相同的 [1,2,3] 去查——true, 因为逐元素喂进去的
	 *    字节序列一模一样, 哈希出的位自然一样；
	 * ② 拿 [3,2,1] 去查——false, 顺序变了字节流就变了, 这是"按顺序敏感的整体指纹",
	 *    所以它适合做"固定顺序的操作序列是否出现过"这类判断；
	 * ③ 拿子集 [1,2] 去查——false, 少喂一个元素, 哈希链没有走满, 位对不上。
	 *    注意这不代表"1 单独 put 过", 过滤器里存的是列表级指纹, 元素级查询要另建过滤器。
	 */
	@Test
	public void testSequentialFunnelIsOrderSensitive() {
		Funnel<Iterable<? extends Integer>> listFunnel =
				Funnels.sequentialFunnel(Funnels.integerFunnel());
		BloomFilter<Iterable<? extends Integer>> filter =
				BloomFilter.create(listFunnel, 100, 0.01);

		List<Integer> list = Arrays.asList(1, 2, 3);
		filter.put(list);

		assertThat(filter.mightContain(Arrays.asList(1, 2, 3))).isTrue();  // 内容相同顺序相同
		assertThat(filter.mightContain(Arrays.asList(3, 2, 1))).isFalse(); // 顺序不同则位不同
		assertThat(filter.mightContain(Arrays.asList(1, 2))).isFalse();    // 子集不算出现过
	}

	/**
	 * unencodedCharsFunnel 与 stringFunnel(charset) 是两套互不兼容的哈希输入。
	 * <p>
	 * stringFunnel 先把字符串按指定字符集编码成字节再喂哈希, 编码方式参与结果；
	 * unencodedCharsFunnel 跳过编码, 直接按 Java String 内部的 UTF-16 字符单元
	 * （char 值）两个字节两个字节地喂。所以同一个中文串分别用两种方式 put 进
	 * 两个规格相同的过滤器, 置的位不同, equals 为 false, isCompatible 也为 false
	 * （Funnel 不同）。两者不能混用在"写入端一种、读取端另一种"的场景里——
	 * 又回到上一条测试的教训: funnel 必须全链路一致。
	 */
	@Test
	public void testDifferentFunnelsAreIncompatible() {
		BloomFilter<String> byCharset = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 1_000_000, 0.000001);
		BloomFilter<String> byChars = BloomFilter.create(
				Funnels.unencodedCharsFunnel(), 1_000_000, 0.000001);
		byCharset.put("中文字符串");
		byChars.put("中文字符串");

		assertThat(byCharset.isCompatible(byChars)).isFalse();
		assertThat(byCharset).isNotEqualTo(byChars);
	}

	/**
	 * null 元素当场抛 NullPointerException, 不会静默放进位数组。
	 * <p>
	 * put(null) 和 mightContain(null) 都在 funnel 解引用对象时 NPE, 快速失败、
	 * 位置就在调用行, 不会污染数据。业务里可空字段要先自己挡一层再喂过滤器。
	 * 空字符串 "" 则完全合法：编码后是零字节输入, 照样能稳定哈希出一组位置,
	 * put 过之后 mightContain("") 返回 true。
	 */
	@Test
	public void testNullThrowsAndEmptyStringWorks() {
		BloomFilter<String> filter = BloomFilter.create(
				Funnels.stringFunnel(StandardCharsets.UTF_8), 10, 0.01);

		assertThatThrownBy(() -> filter.put(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> filter.mightContain(null)).isInstanceOf(NullPointerException.class);

		filter.put("");
		assertThat(filter.mightContain("")).isTrue();
	}

	/**
	 * int 与 long 两个容量的 create 重载在正常范围内完全等价。
	 * <p>
	 * expectedInsertions（预期元素数）既有 int 版也有 long 版, long 版是为元素数
	 * 超过 21 亿的场景准备的。100 这种小数字走哪个重载, 算出的位数组、哈希轮数都一样,
	 * 实测两个分别用 int/long 重载建出的、put 了同一元素的过滤器 equals 相等。
	 * 也就是说日常业务里用哪个全凭手边数字的类型, 不必纠结。
	 */
	@Test
	public void testIntAndLongCapacityOverloadsAreEquivalent() {
		BloomFilter<Integer> byInt = BloomFilter.create(Funnels.integerFunnel(), 500, 0.01);
		BloomFilter<Integer> byLong = BloomFilter.create(Funnels.integerFunnel(), 500L, 0.01);
		byInt.put(1);
		byLong.put(1);

		assertThat(byInt).isEqualTo(byLong);
	}

	/**
	 * equals 比的是"内容"而不是"声明"：两份声明规格相同、装的数据也相同的过滤器才相等,
	 * 数据没对齐就不等, 哪怕预期容量数字一致。
	 * <p>
	 * 实测两个方向：① 同为"100 容量、都只放 1 个元素 1", equals 为 true；
	 * ② 声明同为 100 容量, 但一个按 1% 误判率建、一个按 0.1% 建——位数组宽度不同,
	 * 哪怕放同样的元素也 equals false, isCompatible 也 false。这再次说明 equals 的
	 * 比较对象是最终生成的位数组和哈希轮数这些"算出来的结果", 声明参数只是原材料。
	 */
	@Test
	public void testEqualsComparesBitArrayNotDeclaredArgs() {
		BloomFilter<Integer> one = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01);
		BloomFilter<Integer> two = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01);
		one.put(1);
		two.put(1);
		assertThat(one).isEqualTo(two);

		BloomFilter<Integer> strict = BloomFilter.create(Funnels.integerFunnel(), 100, 0.001);
		strict.put(1);
		// 误判率不同 → 位数组宽度不同 → 内容比较不相等, 也无法合并
		assertThat(one).isNotEqualTo(strict);
		assertThat(one.isCompatible(strict)).isFalse();
	}

	/** 辅助：把过滤器 writeTo 成字节数组并返回字节数（模拟序列化后占用的存储） */
	private static int serializeSize(BloomFilter<?> filter) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		filter.writeTo(bos);
		return bos.size();
	}
}
