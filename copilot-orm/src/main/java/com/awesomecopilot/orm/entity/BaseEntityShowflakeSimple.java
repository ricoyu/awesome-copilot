package com.awesomecopilot.orm.entity;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类的父类, 自己选择要不要继承, 这个是采用雪花算法生成主键值的版本
 * 这个版本值包含一个id主键, CREATE_TIME, UPDATE_TIME 等都不包含
 *
 * 实体类的父类, 自己选择要不要继承, 这个是采用雪花算法生成主键值的版本
 * <p>
 * Copyright: Copyright (c) 2019-10-31 15:36
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@MappedSuperclass
public class BaseEntityShowflakeSimple implements Serializable {
	
	private static final long serialVersionUID = -7833247830642842225L;

	@Id
	@GeneratedValue(generator = "snowflake-id")
	@GenericGenerator(
			name = "snowflake-id",
			strategy = "com.awesomecopilot.orm.generator.CopilotSnowflakeIdGenerator"
	)
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
