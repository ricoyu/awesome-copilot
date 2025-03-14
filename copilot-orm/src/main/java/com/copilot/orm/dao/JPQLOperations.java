package com.copilot.orm.dao;

import jakarta.persistence.Query;

import java.util.List;
import java.util.Map;

public interface JPQLOperations {

	public <T> List<T> find(String jpql, Class<T> clazz);
	
	public <T> T findUnique(String jpql, Class<T> clazz);
	
	public <T> T findUnique(String jpql, Map<String, Object> params, Class<T> clazz);
	
	public <T> T findUnique(String jpql, String paramName, Object paramValue, Class<T> clazz);

	/**
	 * 根据HQL或者JPQL查询
	 * 
	 * @param jpql
	 * @param params
	 * @return List<T>
	 */
	public <T> List<T> find(String jpql, Map<String, Object> params, Class<T> clazz);
	
	/**
	 * 根据HQL或者JPQL查询
	 * 
	 * @param jpql
	 * @param paramName
	 * @param paramValue
	 * @return List<T>
	 */
	public <T> List<T> find(String jpql, String paramName, Object paramValue, Class<T> clazz);
	
	/**
	 * 根据HQL或者JPQL查询,根据参数位置绑定
	 * 
	 * @param jpql
	 * @return
	 */
	public <T> Query createQuery(String jpql, Class<T> clazz);

	public <T> Query createQuery(String jpql, String paramName, Object paramValue, Class<T> clazz);
	
}
