package com.awesomecopilot.algorithm.hwod;

/**
 * 对称美学
 * <p>
 * 对称就是最大的美学，现在有一道关于对称字符串的美学。已知：
 * <p>
 * <pre>
 * 第1个字符串：R
 * 第2个字符串：BR
 * 第3个字符串：RBBR
 * 第4个字符串：BRRBRBBR
 * 第5个字符串：RBBRBRRBBRBBBRR
 * </pre>
 * <p>
 * 相信你已经发现规律了，没错！就是第i个字符串 = 第i-1号字符串取反 + 第i-1号字符串； <p>
 * 取反（R->B，B->R）； <p>
 *
 * 现在告诉你n和k，让你求得第n个字符串的第k个字符是多少。（k的编号从0开始） <p>
 *
 * 二、输入描述 <p>
 * 第一行输入一个T，表示有T组用例； <p>
 *
 * 解析来输入T行，每行输入两个数字，表示n，k <p>
 *
 * 三、输出描述 <p>
 * 输出T行示例答案； <p>
 *
 * 输出“blue”表示字符等于B； <p>
 * 输出“red”表示字符等于R； <p>
 *
 * 注：输出字符区分大小写，请注意输出小写字符串，不带双引号。
 * <p>
 * <pre>
 * 测试用例1：
 * 1、输入
 * 5
 * 1 0
 * 2 1
 * 3 2
 * 4 6
 * 5 8
 *
 * 2、输出
 * red
 * red
 * blue
 * blue
 * blue
 * </pre>
 * <p>
 * 3、说明 <p>
 * 第1个字符串：R -> 第0个字符为R <p>
 * 第2个字符串：BR -> 第1个字符为R <p>
 * 第3个字符串：RBBR -> 第2个字符为B <p>
 * 第4个字符串：BRRBRBBR -> 第6个字符为B <p>
 * 第5个字符串：RBBRBRRBBRBBBRR -> 第8个字符为B <p>
 *
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * 3
 * 1 0
 * 2 0
 * 3 3
 *
 * 2、输出
 * red
 * blue
 * red
 * </pre>
 *
 * 3、说明 <p>
 * 第1个字符串：R -> 第0个字符为R <p>
 * 第2个字符串：BR -> 第0个字符为B <p>
 * 第3个字符串：RBBR -> 第3个字符为R <p>
 *
 * <p/>
 * Copyright: Copyright (c) 2025-04-09 9:37
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SymmetricAesthetics {
}
