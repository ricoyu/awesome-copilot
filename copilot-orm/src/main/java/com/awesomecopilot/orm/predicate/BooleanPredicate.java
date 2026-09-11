package com.awesomecopilot.orm.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class BooleanPredicate extends AbstractPredicate {

	private Boolean propertyValue;
	private CompareMode compareMode = CompareMode.EQ;

	public BooleanPredicate(String propertyName, Boolean propertyValue) {
		setPropertyName(propertyName);
		this.propertyValue = propertyValue;
	}

	public BooleanPredicate(String propertyName, Boolean propertyValue, CompareMode compareMode) {
		this(propertyName, propertyValue);
		if(compareMode != CompareMode.EQ && compareMode != CompareMode.NOTEQ) {
			throw new IllegalArgumentException("BooleanPredicate only accept compareMode EQ or NOTEQ!");
		}
		this.setCompareMode(compareMode);
	}

	public CompareMode getCompareMode() {
		return compareMode;
	}

	public void setCompareMode(CompareMode compareMode) {
		this.compareMode = compareMode;
	}

	@SuppressWarnings({ "rawtypes"})
	public Predicate toPredicate(CriteriaBuilder criteriaBuilder, Root root) {
		Path path = root.get(getPropertyName());
		switch (compareMode) {
		case EQ:
			return propertyValue != null
					? criteriaBuilder.equal(path, propertyValue)
					: criteriaBuilder.isNull(path);
		case NOTEQ:
			return propertyValue != null
					? criteriaBuilder.notEqual(path, propertyValue)
					: criteriaBuilder.isNotNull(path);
		default:
			// 修复两处旧问题: NOTEQ case 缺 break(恰好是最后一个 case 才没出事故);
			// GT/GE/LT/LE/ANYWHERE 进来会静默返回 null 谓词, 到 where() 才 NPE
			throw new IllegalArgumentException(
					"BooleanPredicate 只支持 EQ/NOTEQ, 传入 " + compareMode + ", 属性: " + getPropertyName());
		}
	}

}