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

import static org.apache.commons.lang3.StringUtils.isBlank;

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
		table = constraintAnnotation.table();
		primaryKeyField = constraintAnnotation.primaryKey();
		field = constraintAnnotation.field();
		property = constraintAnnotation.property();
		isSoftDelete = constraintAnnotation.isSoftDelete();
		softDeleteField = constraintAnnotation.softDeleteField();

		//如果没有显式指定表字段名, 默认属性名转下划线风格
		if (isBlank(field)) {
			field = ReflectionUtils.toUnderScore(property);
		}
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
			//表主键字段名
			String idField = null;
			//bean的主键字段名
			String idProperty = null;

			/*
			 * 如果字段是驼峰式命名, 那么认为提供的是bean的属性名, 会自动转成下划线分隔的表字段名
			 */
			if (ReflectionUtils.isCamelCase(primaryKeyField)) {
				idProperty = primaryKeyField;
				idField = ReflectionUtils.toUnderScore(primaryKeyField); // 将驼峰式命名转换成下划线分隔的表字段名
			} else {
				idField = primaryKeyField;
				idProperty = ReflectionUtils.toCamelCase(primaryKeyField); // 将下划线分隔的表字段名转换成驼峰式命名
			}
			Object primaryKeyValue = ReflectionUtils.getFieldValue(idProperty, bean);
			//主键值为null表示INSERT场景

			sql = "select count(*) from " + table + " where " + field + " = ?";

			if (primaryKeyValue != null) {
				sql = sql + " and " + idField + " != " + primaryKeyValue;
			}
			if (isSoftDelete) {
				sql = sql + " and " + softDeleteField + " = 0";
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
