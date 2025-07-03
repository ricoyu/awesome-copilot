package com.awesomecopilot.common.lang.utils;

import java.util.Arrays;

public final class ObjectUtils {

	public static boolean equals(Object o1, Object o2) {
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
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			Object[] objects1 = (Object[]) o1;
			Object[] objects2 = (Object[]) o2;
			return java.util.Arrays.equals(objects1, objects2);
		}
		if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
			boolean[] booleans1 = (boolean[]) o1;
			boolean[] booleans2 = (boolean[]) o2;
			return java.util.Arrays.equals(booleans1, booleans2);
		}
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			byte[] bytes1 = (byte[]) o1;
			byte[] bytes2 = (byte[]) o2;
			return java.util.Arrays.equals(bytes1, bytes2);
		}
		if (o1 instanceof char[] && o2 instanceof char[]) {
			char[] chars1 = (char[]) o1;
			char[] chars2 = (char[]) o2;
			return java.util.Arrays.equals(chars1, chars2);
		}
		if (o1 instanceof double[] && o2 instanceof double[]) {
			double[] doubles1 = (double[]) o1;
			double[] doubles2 = (double[]) o2;
			return java.util.Arrays.equals(doubles1, doubles2);
		}
		if (o1 instanceof float[] && o2 instanceof float[]) {
			float[] floats1 = (float[]) o1;
			float[] floats2 = (float[]) o2;
			return java.util.Arrays.equals(floats1, floats2);
		}
		if (o1 instanceof int[] && o2 instanceof int[]) {
			int[] ints1 = (int[]) o1;
			int[] ints2 = (int[]) o2;
			return java.util.Arrays.equals(ints1, ints2);
		}
		if (o1 instanceof long[] && o2 instanceof long[]) {
			long[] longs1 = (long[]) o1;
			long[] longs2 = (long[]) o2;
			return java.util.Arrays.equals(longs1, longs2);
		}
		if (o1 instanceof short[] && o2 instanceof short[]) {
			short[] shorts1 = (short[]) o1;
			short[] shorts2 = (short[]) o2;
			return java.util.Arrays.equals(shorts1, shorts2);
		}
		return false;
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
			if (obj instanceof Object[]) {
				Object[] objects = (Object[]) obj;
				return Arrays.hashCode(objects);
			}
			if (obj instanceof boolean[]) {
				boolean[] booleans = (boolean[]) obj;
				return Arrays.hashCode(booleans);
			}
			if (obj instanceof byte[]) {
				byte[] bytes = (byte[]) obj;
				return Arrays.hashCode(bytes);
			}
			if (obj instanceof char[]) {
				char[] chars = (char[]) obj;
				return Arrays.hashCode(chars);
			}
			if (obj instanceof double[]) {
				double[] doubles = (double[]) obj;
				return Arrays.hashCode(doubles);
			}
			if (obj instanceof float[]) {
				float[] floats = (float[]) obj;
				return Arrays.hashCode(floats);
			}
			if (obj instanceof int[]) {
				int[] ints = (int[]) obj;
				return Arrays.hashCode(ints);
			}
			if (obj instanceof long[]) {
				long[] longs = (long[]) obj;
				return Arrays.hashCode(longs);
			}
			if (obj instanceof short[]) {
				short[] shorts = (short[]) obj;
				return Arrays.hashCode(shorts);
			}
		}
		return obj.hashCode();
	}

	@SafeVarargs
	public static <T> boolean equalsAny(T obj, T... array) {
		return Arrays.asList(array).contains(obj);
	}
}
