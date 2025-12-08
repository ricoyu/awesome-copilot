package com.awesomecopilot.validation.validation;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.common.spring.context.ApplicationContextHolder;
import com.awesomecopilot.validation.validation.annotation.UniqueValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * If value of referenceField equals as specified in referenceValue, mandatoryField is
 * mandatory, otherwise not.
 *
 * @author xuehyu
 * @since Aug 22, 2014
 */
public class UniqueValueValidator implements ConstraintValidator<UniqueValue, Object> {

	private static final Logger log = LoggerFactory.getLogger(UniqueValueValidator.class);

	private String table = null;
	private String field = null;
	private String softDeleteField = null;
	private String primaryKeyField = null;
	private String property = null;
	private boolean isSoftDelete;

	@Override
	public void initialize(UniqueValue constraintAnnotation) {
		field = constraintAnnotation.field();
		table = constraintAnnotation.table();
		primaryKeyField = constraintAnnotation.primaryKey();
		property = constraintAnnotation.property();
		isSoftDelete = constraintAnnotation.isSoftDelete();
		softDeleteField = constraintAnnotation.softDeleteField();
	}

	@Override
	public boolean isValid(Object bean, ConstraintValidatorContext context) {
		Object PropertyValue = ReflectionUtils.getFieldValue(property, bean);
		if (PropertyValue == null) {
			return true;
		}

		DataSource dataSource = ApplicationContextHolder.getBean(DataSource.class);
		try {
			Connection connection = dataSource.getConnection();
			PreparedStatement stmt = null;
			String sql = null;

			//propertyValues是SQL where条件中的值
			Object primaryKeyValue = ReflectionUtils.getFieldValue(primaryKeyField, bean);
			//主键值为null表示INSERT场景
			if (primaryKeyValue == null) {
				sql = "select count(*) from " + table + " where " + field + " = ?";
			} else {
				sql = sql + "and " + primaryKeyField + "!=" + primaryKeyValue;
			}
			if (isSoftDelete) {
				sql = sql + "and " + softDeleteField + " = 0";
			}

			stmt = connection.prepareStatement(sql);
			if (PropertyValue instanceof String) {
				stmt.setString(1, (String) PropertyValue);
			} else if (PropertyValue instanceof Long) {
				stmt.setLong(1, (Long) PropertyValue);
			} else if (PropertyValue instanceof Integer) {
				stmt.setInt(1, (Integer) PropertyValue);
			}

			ResultSet rs = stmt.executeQuery();
			int count = 0;
			// 关键：ResultSet默认指向第一行之前，必须调用next()移动到结果行
			if (rs.next()) {
				// 获取count(*)的结果，列索引从1开始（或用列名如"count(*)"）
				count = rs.getInt(1);
			}
			return count == 0;
		} catch (SQLException e) {
			log.error("", e);
			throw new RuntimeException(e);
		}
	}

}
