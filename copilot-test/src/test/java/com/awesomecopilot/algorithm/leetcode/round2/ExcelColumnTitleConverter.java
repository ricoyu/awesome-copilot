package com.awesomecopilot.algorithm.leetcode.round2;

/**
 * Excel 表列名称
 * <p>
 * 给你一个整数 columnNumber ，返回它在 Excel 表中相对应的列名称。
 * <p>
 * <pre>
 * 例如：
 *
 * A -> 1
 * B -> 2
 * C -> 3
 * ...
 * Z -> 26
 * AA -> 27
 * AB -> 28
 * ...
 * </pre>
 *
 * <pre>
 * 示例 1：
 *
 * 输入：columnNumber = 1
 * 输出："A"
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：columnNumber = 28
 * 输出："AB"
 * </pre>
 *
 * <pre>
 * 示例 3：
 *
 * 输入：columnNumber = 701
 * 输出："ZY"
 * </pre>
 *
 * <pre>
 * 示例 4：
 *
 * 输入：columnNumber = 2147483647
 * 输出："FXSHRXW"
 * </pre>
 * <ul>核心解题思路
 *     <li/>Excel 列名转换不是标准的 26 进制（标准 26 进制是 0-25），而是 1-26 对应 A-Z，因此核心技巧是：
 *     <li/>每次转换前先将 columnNumber 减 1，把数值映射到 0-25 的范围（对应 A-Z）；
 *     <li/>对 26 取余得到当前位的字符（0→A，1→B…25→Z）；
 *     <li/>将 columnNumber 除以 26，重复上述步骤直到数值为 0；
 *     <li/>最后反转拼接的字符，得到最终结果。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-01-12 9:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ExcelColumnTitleConverter {
}
