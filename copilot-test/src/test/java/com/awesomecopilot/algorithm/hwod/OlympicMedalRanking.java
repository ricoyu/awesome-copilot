package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * 冠亚军排名
 * <p>
 * 一、题目描述
 * <p>
 * 2012年伦敦奥运会即将到来，大家都非常关注奖牌榜的情况，现在我们假设奖牌榜的排名规则如下：
 * <p>
 * 首先gold medal数量最多的排在前面；<br/>
 * 其次silver medal数量最多的排在前面；<br/>
 * 然后bronze medal数量最多的排在前面；<br/>
 * 若以上三个条件仍无法区分名次，则以国家名称的字典顺序排定。
 * <p>
 * 我们假设国家名称由二十六个字母组成，各奖牌数据不超过100，且大于0。
 * <p>
 * 二、输入描述 <p>
 * 第一行输入一个整数N（0 < N < 21），代表国家数量；
 * <p>
 * 然后接下来的N行，每行包含： 一个字符串Name表示各个国家的名称和三个整数Gi, Si, Bi表示每个国家获得的gold medal, silver medal, bronze medal的数量，以空格隔开，如China 51
 * 20 21。
 * <p>
 * 具体见样例输入。
 * <pre>
 * 5
 * China 32 28 34
 * England 12 34 22
 * France 23 33 2
 * Japan 12 34 25
 * Rusia 23 43 0
 * </pre>
 * <p>
 * 三、输出描述 <p>
 * 按照奖牌榜的依次顺序，只输出国家名称，每行占一行，具体如下：
 * <p>
 * 示例输出：
 * <pre>
 * China
 * Rusia
 * France
 * Japan
 * England
 * </pre>
 * <p>
 * 测试用例1：
 *
 * <pre>
 * 1、输入
 *
 * 4
 * Germany 10 10 10
 * Austria 10 10 10
 * Belgium 10 10 10
 * Denmark 10 10 10
 *
 * 2、输出
 *
 * Austria
 * Belgium
 * Denmark
 * Germany
 * </pre>
 * <p>
 * 测试用例2：
 *
 * <pre>
 * 1、输入
 *
 * 6
 * India 15 20 25
 * Australia 15 20 20
 * NewZealand 15 20 25
 * SouthAfrica 10 30 40
 * Italy 15 25 20
 * Spain 15 20 25
 *
 * 2、输出
 *
 * Italy
 * India
 * NewZealand
 * Spain
 * Australia
 * SouthAfrica
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-05-16 8:39
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class OlympicMedalRanking {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入国家数量: ");
		int n = scanner.nextInt();
		List<Country> countries = new ArrayList<>();
		scanner.nextLine();
		for (int i = 0; i < n; i++) {
			System.out.print("请输入第" + (i + 1) + "国家名称, 奖牌数量:");
			String[] parts = scanner.nextLine().trim().split(" ");
			String name = parts[0].trim();
			int gold = Integer.valueOf(parts[1].trim());
			int silver = Integer.valueOf(parts[2].trim());
			int bronze = Integer.valueOf(parts[3].trim());
			Country country = new Country(name, gold, silver, bronze);
			countries.add(country);
			//System.out.println(country.name);
		}

		countries.sort(new Comparator<Country>() {
			@Override
			public int compare(Country c1, Country c2) {
				if (c1.gold != c2.gold) {
					return c2.gold - c1.gold;
				} else if (c1.silver != c2.silver) {
					return c2.silver - c1.silver;
				} else if (c1.bronze != c2.bronze) {
					return c2.bronze - c1.bronze;
				} else {
					return c1.name.compareTo(c2.name);
				}
			}
		});

		countries.stream().map(Country::getName).forEach(System.out::println);
	}


	public static class Country {
		private String name;
		private int gold;
		private int silver;
		private int bronze;

		public Country(String name, int gold, int silver, int bronze) {
			this.name = name;
			this.gold = gold;
			this.silver = silver;
			this.bronze = bronze;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getGold() {
			return this.gold;
		}

		public int getSilver() {
			return this.silver;
		}

		public int getBronze() {
			return this.bronze;
		}

		public void setGold(int gold) {
			this.gold = gold;
		}

		public void setSilver(int silver) {
			this.silver = silver;
		}

		public void setBronze(int bronze) {
			this.bronze = bronze;
		}
	}
}
