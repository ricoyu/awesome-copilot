package com.awesomecopilot.common.lang.utils;

import java.util.Arrays;

public final class ObjectUtils {

	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] objects1 && o2 instanceof Object[] objects2) {
			return java.util.Arrays.equals(objects1, objects2);
		}
		if (o1 instanceof boolean[] booleans1 && o2 instanceof boolean[] booleans2) {
			return java.util.Arrays.equals(booleans1, booleans2);
		}
		if (o1 instanceof byte[] bytes1 && o2 instanceof byte[] bytes2) {
			return java.util.Arrays.equals(bytes1, bytes2);
		}
		if (o1 instanceof char[] chars1 && o2 instanceof char[] chars2) {
			return java.util.Arrays.equals(chars1, chars2);
		}
		if (o1 instanceof double[] doubles1 && o2 instanceof double[] doubles2) {
			return java.util.Arrays.equals(doubles1, doubles2);
		}
		if (o1 instanceof float[] floats1 && o2 instanceof float[] floats2) {
			return java.util.Arrays.equals(floats1, floats2);
		}
		if (o1 instanceof int[] ints1 && o2 instanceof int[] ints2) {
			return java.util.Arrays.equals(ints1, ints2);
		}
		if (o1 instanceof long[] longs1 && o2 instanceof long[] longs2) {
			return java.util.Arrays.equals(longs1, longs2);
		}
		if (o1 instanceof short[] shorts1 && o2 instanceof short[] shorts2) {
			return Arrays.equals(shorts1, shorts2);
		}
		return false;
	}

	/**
	 * 判断两个对象是否相等, 如果是原子类型, 用原子值比较, 否则用Object.equals(obj2)
	 * 都为null认为相等, 一个为null一个不为null认为不等
	 *
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	public static boolean equalTo(Object obj1, Object obj2) {
		if (obj1 == null && obj2 == null) {
			return true;
		}
		if (obj1 == null) {
			return false;
		}
		if (obj2 == null) {
			return false;
		}

		return obj1.equals(obj2);
	}

	/**
	 * Return a hash code for the given object; typically the value of
	 * {@code Object#hashCode()}}. If the object is an array,
	 * this method will delegate to any of the {@code Arrays.hashCode}
	 * methods. If the object is {@code null}, this method returns 0.
	 * @see Object#hashCode()
	 * @see Arrays
	 */
	public static int nullSafeHashCode(Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[] objects) {
				return Arrays.hashCode(objects);
			}
			if (obj instanceof boolean[] booleans) {
				return Arrays.hashCode(booleans);
			}
			if (obj instanceof byte[] bytes) {
				return Arrays.hashCode(bytes);
			}
			if (obj instanceof char[] chars) {
				return Arrays.hashCode(chars);
			}
			if (obj instanceof double[] doubles) {
				return Arrays.hashCode(doubles);
			}
			if (obj instanceof float[] floats) {
				return Arrays.hashCode(floats);
			}
			if (obj instanceof int[] ints) {
				return Arrays.hashCode(ints);
			}
			if (obj instanceof long[] longs) {
				return Arrays.hashCode(longs);
			}
			if (obj instanceof short[] shorts) {
				return Arrays.hashCode(shorts);
			}
		}
		return obj.hashCode();
	}
}
