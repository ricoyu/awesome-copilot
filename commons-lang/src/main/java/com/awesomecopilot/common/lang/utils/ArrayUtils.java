package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.functional.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public final class ArrayUtils {

	@SuppressWarnings("unchecked")
	public static final <T> T[] asArray(T... args) {
		return args;
	}

	/**
	 * 过滤Array中的null元素并排序
	 *
	 * @param args
	 * @return T[]
	 */
	@SuppressWarnings("unchecked")
	public static final <T> T[] nonNull(T... args) {
		if (args == null) {
			return null;
		}
		return (T[]) stream(args)
				.filter(Objects::nonNull)
				.sorted()
				.distinct()
				.toArray();
	}

	/**
	 * 数组转List
	 * @param nums
	 * @return
	 */
	public static List<Integer> toList(int[] nums) {
		return Arrays.stream(nums)
				.boxed()
				.collect(Collectors.toList());
	}

	/**
	 * 打印数组
	 *
	 * @param array
	 */
	public static void print(int[] array) {
		for (int i : array) {
			System.out.print(i + " ");
		}
		System.out.println("");
	}

	/**
	 * 产生一个随机的数组, 数组长度也是随机的, 但不超过maxSize, 数组元素随机, 最大值为maxValue
	 *
	 * @param maxSize
	 * @param maxValue
	 * @return
	 */
	public static int[] generateRandomArray(int maxSize, int maxValue) {
		// Math.random() -> [0,1) 所有的小数，等概率返回一个
		// Math.random() * N -> [0,N) 所有小数，等概率返回一个
		// (int)(Math.random() * N) -> [0,N-1] 所有的整数，等概率返回一个
		int[] arr = new int[(int) ((maxSize + 1) * Math.random())]; // 长度随机
		for (int i = 0; i < arr.length; i++) {
			arr[i] = (int) ((maxValue + 1) * Math.random()) - (int) (maxValue * Math.random());
		}
		return arr;
	}

	/**
	 * 在原数组基础上复制一个数组
	 *
	 * @param arr
	 * @return
	 */
	public static int[] copyArray(int[] arr) {
		if (arr == null) {
			return null;
		}
		int[] res = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			res[i] = arr[i];
		}
		return res;
	}

	/**
	 * 如果两个数组长度不一样, 或者元素不一样, 返回false
	 * 两个数组都为null返回true
	 *
	 * @param arr1
	 * @param arr2
	 * @return boolean
	 */
	public static boolean isEqual(int[] arr1, int[] arr2) {
		if ((arr1 == null && arr2 != null) || (arr1 != null && arr2 == null)) {
			return false;
		}
		if (arr1 == null && arr2 == null) {
			return true;
		}
		if (arr1.length != arr2.length) {
			return false;
		}
		for (int i = 0; i < arr1.length; i++) {
			if (arr1[i] != arr2[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 返回数组中第一个匹配规则的值
	 * @param <T>     数组元素类型
	 * @param matcher 匹配接口，实现此接口自定义匹配规则
	 * @param array   数组
	 * @return 匹配元素，如果不存在匹配元素或数组为空，返回 {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T firstMatch(Matcher<T> matcher, T... array) {
		final int index = matchIndex(matcher, array);
		if (index < 0) {
			return null;
		}

		return array[index];
	}

	/**
	 * 返回数组中第一个匹配规则的值的位置
	 *
	 * @param <T>               数组元素类型
	 * @param matcher           匹配接口，实现此接口自定义匹配规则
	 * @param array             数组
	 * @return 匹配到元素的位置，-1表示未匹配到
	 * @since 5.7.3
	 */
	@SuppressWarnings("unchecked")
	public static <T> int matchIndex(Matcher<T> matcher, T... array) {
		Assert.notNull(matcher, "Matcher must be not null !");
		if (isNotEmpty(array)) {
			for (int i = 0; i < array.length; i++) {
				if (matcher.match(array[i])) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * 将一个字符串形式的一维数组转成int[]
	 *
	 * @param str
	 * @return
	 */
	public static int[] parseOneDimensionArray(String str) {
		if (str == null || str.length() == 0) {
			return null;
		}
		str = str.trim();
		if ('[' == str.charAt(0) && ']' == str.charAt(str.length() - 1)) {
			str = str.substring(1, str.length() - 1); // 去掉两边的方括号
		}
		String[] strArray = str.split(","); // 按逗号分割
		int[] arr = new int[strArray.length]; // 创建 int 数组

		for (int i = 0; i < strArray.length; i++) {
			arr[i] = Integer.parseInt(strArray[i].trim()); // 转换每个元素为 int
		}

		return arr;
	}

	/**
	 * 将一个字符串形式的二维数组[[1 ,4],[4,5]]转成int[][]
	 *
	 * @param input
	 * @return
	 */
	public static int[][] parseTwoDimensionArray(String input) {
		// 去除字符串两端的方括号
		String trimmed = input.substring(1, input.length() - 1);

		// 特殊情况处理：空数组
		if (trimmed.isEmpty()) {
			return new int[0][];
		}

		// 分割外层数组元素
		String[] outerElements = trimmed.split(",(?=\\[)");

		List<int[]> resultList = new ArrayList<>();

		for (String element : outerElements) {
			// 去除每个元素两端的方括号
			String innerTrimmed = element.substring(1, element.length() - 1);

			// 分割内层数组元素
			String[] innerElements = innerTrimmed.split(",");

			int[] innerArray = new int[innerElements.length];
			for (int i = 0; i < innerElements.length; i++) {
				innerArray[i] = Integer.parseInt(innerElements[i].trim());
			}

			resultList.add(innerArray);
		}

		// 转换为二维数组
		int[][] result = new int[resultList.size()][];
		for (int i = 0; i < resultList.size(); i++) {
			result[i] = resultList.get(i);
		}

		return result;
	}

	/**
	 * 将输入的字符串转化为字符串数组，处理两种情况：
	 * 1. 如果存在中括号，去掉中括号；
	 * 2. 去掉字符串中的双引号，并按逗号分隔。
	 * @param input 输入的字符串，格式类似于["flower","flow","flight"] 或 "flower","flow","flight"
	 * @return 字符串数组
	 */
	public static String[] toStringArray(String input) {
		if (input == null ||input.isEmpty()) {
			return null;
		}
		// 判断输入是否以中括号开头和结尾
		if (input.startsWith("[") && input.endsWith("]")) {
			// 去掉两边的中括号
			input = input.substring(1, input.length() - 1);
		}

		// 去掉双引号
		input = input.replaceAll("\"", "");

		// 使用逗号分隔字符串并转换为数组
		String[] arr = input.split(",");
		return java.util.Arrays.stream(arr)
				.map(String::trim)
				.toArray(String[]::new);
	}

	/**
	 * 数组是否为非空
	 *
	 * @param <T>   数组元素类型
	 * @param array 数组
	 * @return 是否为非空
	 */
	public static <T> boolean isNotEmpty(T[] array) {
		return (null != array && array.length != 0);
	}
}
