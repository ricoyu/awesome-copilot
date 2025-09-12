package com.awesomecopilot.orm.entity;

import com.awesomecopilot.orm.generator.CopilotSnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类的父类, 自己选择要不要继承, 这个是采用雪花算法生成主键值的版本
 * 这个版本只包含一个id主键, CREATE_TIME, UPDATE_TIME 等都不包含
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
	@GenericGenerator(name = "snowflake-id", type = CopilotSnowflakeIdGenerator.class)
	private Long id;

	/**
	 * 默认映射的数据库字段类型为TIMESTAMP
	 */
	@Column(name = "CREATE_TIME", columnDefinition = "DATETIME", nullable = false, length = 19)
	private LocalDateTime createTime;

	@Column(name = "UPDATE_TIME", columnDefinition = "DATETIME", nullable = false, length = 19)
	private LocalDateTime updateTime;
	
	/**
	 * 在Entity被持久化之前做一些操作
	 */
	@PrePersist
	protected void onPrePersist() {
		LocalDateTime now = LocalDateTime.now();
		setCreateTime(now);
		setUpdateTime(now);
	}
	
	@PreUpdate
	protected void onPreUpdate() {
		setUpdateTime(LocalDateTime.now());
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
