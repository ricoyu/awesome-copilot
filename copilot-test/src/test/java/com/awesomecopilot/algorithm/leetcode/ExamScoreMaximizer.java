package com.awesomecopilot.algorithm.leetcode;

/**
 * 解决智力问题
 * <p>
 * 给你一个下标从 0 开始的二维整数数组 questions ，其中 questions[i] = [pointsi, brainpoweri] 。
 * <p>
 * 这个数组表示一场考试里的一系列题目，你需要按顺序 （也就是从问题 0 开始依次解决），针对每个问题选择 解决 或者 跳过 操作。解决问题 i 将让你 获得  pointsi 的分数，
 * 但是你将无法 解决接下来的 brainpoweri 个问题（即只能跳过接下来的 brainpoweri 个问题）。如果你跳过问题 i ，你可以对下一个问题决定使用哪种操作。
 * <p>
 * 比方说，给你 questions = [[3, 2], [4, 3], [4, 4], [2, 5]] ： <br/>
 * 如果问题 0 被解决了， 那么你可以获得 3 分，但你不能解决问题 1 和 2 。 <br/>
 * 如果你跳过问题 0 ，且解决问题 1 ，你将获得 4 分但是不能解决问题 2 和 3 。 <br/>
 * 请你返回这场考试里你能获得的 最高分数。
 * <p>
 * 示例 1：
 * <p>
 * 输入：questions = [[3,2],[4,3],[4,4],[2,5]] <br/>
 * 输出：5
 *   <ul>解释：解决问题 0 和 3 得到最高分。
 *     <li/>解决问题 0 ：获得 3 分，但接下来 2 个问题都不能解决。
 *     <li/>不能解决问题 1 和 2
 *     <li/>解决问题 3 ：获得 2 分
 *     <li/>总得分为：3 + 2 = 5 。没有别的办法获得 5 分或者多于 5 分。
 * </ul>
 * <p>
 * 示例 2：
 * <p>
 *   输入：questions = [[1,1],[2,2],[3,3],[4,4],[5,5]] <br/>
 *   输出：7 <br/>
 *   <ul>解释：解决问题 1 和 4 得到最高分。
 *     <li/>跳过问题 0
 *     <li/>解决问题 1 ：获得 2 分，但接下来 2 个问题都不能解决。
 *     <li/>不能解决问题 2 和 3
 *     <li/>解决问题 4 ：获得 5 分
 *     <li/>总得分为：2 + 5 = 7 。没有别的办法获得 7 分或者多于 7 分。
 * </ul>
 * <p>
 * 假设数组的长度是n。我们需要创建一个dp数组，长度为n+1。其中dp[i]代表从第i个问题开始处理能得到的最大分数。这样，当i >=n的时候，dp[i]=0。
 * 然后我们从i=n-1倒推到i=0：
 * <p>
 * 对于每个i来说，如果我们选择解决，那么得到的分数是questions[i][0] + dp[i + questions[i][1] +1]。但是要注意，i + questions[i][1] +1可能超过n，这时候对应的dp值就是0。
 * <p>
 * 如果我们选择不解决，那么得到的分数是dp[i+1]。
 * <p>
 * 因此，dp[i] = Math.max(questions[i][0] + (下一个位置的dp值), dp[i+1])
 * <p>
 * 这样整个过程就可以完成了。
 * <p>
 * 举个例子，比如示例1中的输入：<br/>
 * questions = [[3,2],[4,3],[4,4],[2,5]] <br/>
 * 那么n=4。初始化dp数组长度是5，dp[4]=0。 <br/>
 * <p>
 * 计算i=3的时候：
 * 解决的话，得到2分，然后下一个位置是3+5+1=9，超出数组，所以加0。总共有2。跳过的话，取dp[4]=0。所以dp[3]=max(2,0)=2.
 * <p>
 * i=2：
 * 解决的话，得到4分，下一个位置是2+4+1=7，超出数组。总分4。跳过的话取dp[3]=2。所以dp[2]=4.
 * <p>
 * i=1:
 * 解决的话，得到4分，下一个位置是1+3+1=5，超出数组。总分4。跳过的话取dp[2]=4。所以dp[1] = max(4,4)=4.
 * <p>
 * i=0:
 * 解决的话，得到3分，下一个位置是0+2+1=3，dp[3]=2。总分3+2=5。跳过的话取dp[1]=4。所以dp[0] = max(5,4)=5。结果正确。
 * <p>
 * 那么现在要考虑如何实现这个逻辑。在Java中，我们可以创建一个一维数组dp，长度n+1。初始化为0。然后逆序填充。
 * <ol>具体步骤如下：
 *     <li/>获取questions的长度n。
 *     <li/>创建dp数组，大小为n+1。初始值都为0。
 *     <li/>从i=n-1循环到i=0：
 *          <ul>
 *              <li/>解决当前的题目，得到的分数是points + dp[i + brain +1]。如果i+brain+1超过n，则后面部分为0。
 *              <li/>不解决的分数是dp[i+1]。
 *              <li/>dp[i]取两者的最大值。
 *          </ul>
 *     <li/>返回dp[0]。
 * </ol>
 * <p/>
 * Copyright: Copyright (c) 2025-03-25 9:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ExamScoreMaximizer {

	public long mostPoints(int[][] questions) {
		int n = questions.length;
		long[] dp = new long[questions.length + 1];

		for (int i = n - 1; i >= 0; i--) {
			int questionsPoints = questions[i][0];
			int questionsBrain = questions[i][1];
			int nextPosition = i + questionsBrain + 1;
			dp[i] = Math.max(questionsPoints + (nextPosition >= n ? 0 : dp[i + questionsBrain + 1]), dp[i + 1]);
		}

		return dp[0];
	}
}
