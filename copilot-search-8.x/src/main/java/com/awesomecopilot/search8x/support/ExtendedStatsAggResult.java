package com.awesomecopilot.search8x.support;

import lombok.Builder;
import lombok.Data;

/**
 * Extended Stats 聚合结果, 比 StatsAggResult 多了平方和、方差、标准差、标准差界限
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
public class ExtendedStatsAggResult {

	/**
	 * 聚合名称
	 */
	private String name;

	/**
	 * 参与聚合计算的文档数量(非空值的个数)
	 */
	private long count;

	/**
	 * 字段中的最小值
	 */
	private double min;

	/**
	 * 字段中的最大值
	 */
	private double max;

	/**
	 * 字段的算术平均值
	 */
	private double avg;

	/**
	 * 字段中所有值的总和
	 */
	private double sum;

	/**
	 * 平方和, 即所有值的平方累加 (Σx²), 用于计算方差和标准差
	 */
	private double sumOfSquares;

	/**
	 * 方差(总体方差), 衡量数据离散程度, 计算公式: Σ(x - avg)² / N
	 */
	private double variance;

	/**
	 * 总体方差, 与 variance 相同, 假设数据为完整总体, 计算公式: Σ(x - avg)² / N
	 */
	private Double variancePopulation;

	/**
	 * 样本方差, 假设数据为总体的一个样本, 使用贝塞尔校正, 计算公式: Σ(x - avg)² / (N - 1)
	 */
	private Double varianceSampling;

	/**
	 * 标准差(总体标准差), 方差的平方根, 衡量数据偏离均值的程度, 计算公式: √variance
	 */
	private double stdDeviation;

	/**
	 * 总体标准差, 与 stdDeviation 相同, 基于总体方差计算
	 */
	private Double stdDeviationPopulation;

	/**
	 * 样本标准差, 基于样本方差计算, 计算公式: √varianceSampling
	 */
	private Double stdDeviationSampling;

	/**
	 * 标准差上界, 默认值为 avg + 2 * stdDeviation, 可通过 sigma 参数调整倍数
	 */
	private Double stdDeviationBoundsUpper;

	/**
	 * 标准差下界, 默认值为 avg - 2 * stdDeviation, 可通过 sigma 参数调整倍数
	 */
	private Double stdDeviationBoundsLower;

	/**
	 * 总体标准差上界, 基于总体标准差计算: avg + 2 * stdDeviationPopulation
	 */
	private Double stdDeviationBoundsUpperPopulation;

	/**
	 * 总体标准差下界, 基于总体标准差计算: avg - 2 * stdDeviationPopulation
	 */
	private Double stdDeviationBoundsLowerPopulation;

	/**
	 * 样本标准差上界, 基于样本标准差计算: avg + 2 * stdDeviationSampling
	 */
	private Double stdDeviationBoundsUpperSampling;

	/**
	 * 样本标准差下界, 基于样本标准差计算: avg - 2 * stdDeviationSampling
	 */
	private Double stdDeviationBoundsLowerSampling;
}
