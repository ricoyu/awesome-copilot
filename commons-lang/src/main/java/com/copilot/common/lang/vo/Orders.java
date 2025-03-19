package com.copilot.common.lang.vo;

public final class Orders {

	private Orders() {

	}

	public static OrderBean asc(String propertyName) {
		return new OrderBean(propertyName, OrderBean.DIRECTION.ASC);
	}

	public static OrderBean desc(String propertyName) {
		return new OrderBean(propertyName, OrderBean.DIRECTION.DESC);
	}

}
