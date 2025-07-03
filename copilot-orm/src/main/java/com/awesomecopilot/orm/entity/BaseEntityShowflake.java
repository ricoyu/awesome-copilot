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
 * <p>
 * Copyright: Copyright (c) 2019-10-31 15:36
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@MappedSuperclass
public class BaseEntityShowflake implements Serializable {
	
	private static final long serialVersionUID = -7833247830642842225L;

	@Id
	@GeneratedValue(generator = "snowflake-id")
	@GenericGenerator(
			name = "snowflake-id",
			strategy = "com.awesomecopilot.orm.generator.CopilotSnowflakeIdGenerator"
	)
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
