package com.awesomecopilot.orm.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CriteriaOperations {

	/**
	 * 根据属性查找，返回一个对象，如果找到多个，取第一个
	 * @param entityClass 实体类
	 * @return CriteriaQueryBuilder
	 * @param entityClass
	 */
	public CriteriaQueryBuilder query(Class entityClass);

	/**
	 * 根据属性查找，返回一个对象，如果找到多个，取第一个
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return
	 * @param <T>
	 */
	public <T> T findOne(Class<T> entityClass, String propertyName, Object value);

	/**
	 * 根据某个属性查找
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return
	 */
	public <T> List<T> findList(Class<T> entityClass, String propertyName, Object value);

	/**
	 * 数字字段范围查询
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param begin
	 * @param end
	 * @return List<T>
	 * @param <T>
	 */
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, Long begin, Long end);

	/**
	 * 时间范围查询
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param begin
	 * @param end
	 * @return List<T>
	 * @param <T>
	 */
	public <T> List<T> findBetween(Class<T> entityClass, String propertyName, LocalDateTime begin, LocalDateTime end);

	/**
	 * 根据属性在给定值列表中来获取，可以指定是否包含软删除的对象
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return List<T>
	 */
	public <T> List<T> findIn(Class<T> entityClass, String propertyName, Collection<?> value);

	/**
	 * propertyName 对应字段是null对象
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @return
	 */
	public <T> List<T> findIsNull(Class<T> entityClass, String propertyName);
	
	/**
	 * 根据属性查找，返回唯一一个对象，如果找到多个，取第一个，如果找不到则抛出 EntityNotFoundException
	 * 
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return T
	 */
	public <T> T ensureExists(Class<T> entityClass, String propertyName, Object value) throws EntityNotFoundException;


	/**
	 * 根据属性值来删除
	 * @param entityClass
	 * @return CriteriaDeleteBuilder 删除生成器
	 * @param entityClass
	 */
	public  CriteriaDeleteBuilder deleteBy(Class entityClass);

	/**
	 * 删除属性值在指定列表里面的所有对象
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return
	 */
	public <T> int deleteIn(Class<T> entityClass, String propertyName, Collection<?> value);


	/**
	 * 检查有对应entity是否存在, 不会真正把数据查询出来, 不存在返回false不会抛异常
	 * @param entityClass 实体类
	 * @param propertyName 实体类属性名, 不是数据库字段名
	 * @param value
	 * @return boolean
	 */
	public <T> boolean ifExists(Class<T> entityClass, String propertyName, Object value);

	public EntityManager em();
}
