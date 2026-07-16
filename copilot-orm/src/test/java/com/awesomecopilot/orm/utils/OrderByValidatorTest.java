package com.awesomecopilot.orm.utils;

import com.awesomecopilot.common.lang.vo.OrderBean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderByValidatorTest {

	@Test
	void validateField_acceptsSafeIdentifiers() {
		assertEquals("create_time", OrderByValidator.validateField("create_time"));
		assertEquals("t.user_id", OrderByValidator.validateField(" t.user_id "));
		assertEquals("CREATE_TIME", OrderByValidator.validateField("CREATE_TIME"));
	}

	@Test
	void validateField_rejectsInjectionPayloads() {
		assertThrows(IllegalArgumentException.class, () -> OrderByValidator.validateField("id; drop table users"));
		assertThrows(IllegalArgumentException.class, () -> OrderByValidator.validateField("id--"));
		assertThrows(IllegalArgumentException.class, () -> OrderByValidator.validateField("(select 1)"));
		assertThrows(IllegalArgumentException.class, () -> OrderByValidator.validateField(""));
	}

	@Test
	void appendOrderClause_buildsSafeFragment() {
		StringBuilder sql = new StringBuilder("ORDER BY ");
		OrderByValidator.appendOrderClause(sql, new OrderBean("sort", OrderBean.DIRECTION.ASC));
		assertEquals("ORDER BY sort ASC", sql.toString());
	}
}
