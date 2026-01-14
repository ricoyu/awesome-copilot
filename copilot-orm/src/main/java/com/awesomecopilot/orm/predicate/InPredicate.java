package com.awesomecopilot.orm.predicate;

import com.awesomecopilot.common.lang.utils.ArrayTypes;
import com.awesomecopilot.common.lang.utils.Types;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;

public class InPredicate extends AbstractPredicate {

	private Object propertyValues;
	private CompareMode compareMode = CompareMode.IN;

	/**
	 *
	 * @param propertyName
	 * @param propertyValues Object类型即可, 因为下面toPredicate方法里面通过反射动态判断实际类型了
	 */
	public InPredicate(String propertyName, Object propertyValues) {
		setPropertyName(propertyName);
		this.propertyValues = propertyValues;
	}

	/**
	 *
	 * @param propertyName
	 * @param propertyValues Object类型即可, 因为下面toPredicate方法里面通过反射动态判断实际类型了
	 * @param compareMode
	 */
	public InPredicate(String propertyName, Object propertyValues, CompareMode compareMode) {
		setPropertyName(propertyName);
		this.propertyValues = propertyValues;
		this.compareMode = compareMode;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		if (propertyValues == null) {
			return null;
		}

		Path path = root.get(getPropertyName());
		In inPredicate = null;
		
		if (compareMode == CompareMode.IN) {
			inPredicate = criteriaBuilder.in(path);
		} else {
			inPredicate = (In)criteriaBuilder.in(path).not();
		}
		if (Collection.class.isAssignableFrom(propertyValues.getClass())) {
			Collection values = (Collection) propertyValues;
			for (Object value : values) {
				inPredicate.value(value);
			}
		} else {
			ArrayTypes arrayTypes = Types.arrayTypes(propertyValues);
			if (arrayTypes != null) {
				switch (arrayTypes) {
				case LONG_WRAPPER:
					Long[] arr1 = (Long[]) propertyValues;
					for (int i = 0; i < arr1.length; i++) {
						Long value = arr1[i];
						inPredicate.value(value);
					}
					break;
				case LONG:
					long[] arr2 = (long[]) propertyValues;
					for (int i = 0; i < arr2.length; i++) {
						long value = arr2[i];
						inPredicate.value(value);
					}
					break;
				case INTEGER:
					int[] arr3 = (int[]) propertyValues;
					for (int i = 0; i < arr3.length; i++) {
						int value = arr3[i];
						inPredicate.value(value);
					}
					break;
				case INTEGER_WRAPPER:
					Integer[] arr4 = (Integer[]) propertyValues;
					for (int i = 0; i < arr4.length; i++) {
						Integer value = arr4[i];
						inPredicate.value(value);
					}
					break;
				case STRING:
					String[] arr5 = (String[]) propertyValues;
					for (int i = 0; i < arr5.length; i++) {
						String value = arr5[i];
						inPredicate.value(value);
					}
					break;
				case DOUBLE:
					double[] arr6 = (double[]) propertyValues;
					for (int i = 0; i < arr6.length; i++) {
						double value = arr6[i];
						inPredicate.value(value);
					}
					break;
				case DOUBLE_WRAPPER:
					Double[] arr7 = (Double[]) propertyValues;
					for (int i = 0; i < arr7.length; i++) {
						Double value = arr7[i];
						inPredicate.value(value);
					}
					break;
				case FLOAT:
					float[] arr8 = (float[]) propertyValues;
					for (int i = 0; i < arr8.length; i++) {
						float value = arr8[i];
						inPredicate.value(value);
					}
					break;
				case FLOAT_WRAPPER:
					Float[] arr9 = (Float[]) propertyValues;
					for (int i = 0; i < arr9.length; i++) {
						float value = arr9[i];
						inPredicate.value(value);
					}
					break;

				default:
					break;
				}
			}
		}

		return inPredicate;
	}

}
