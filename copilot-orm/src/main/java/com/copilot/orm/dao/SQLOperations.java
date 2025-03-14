package com.copilot.orm.dao;

import java.util.List;
import java.util.Map;

/**
 * 定义在XML中命名SQL查询接口，返回resultset将绑定到给定类型的Bean中。
 * ResultSet中field_name到Bean中property的映射规则为 foo_bar --> fooBar username --> username
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 * @since 2017-01-27 17:04
 */
public interface SQLOperations {

	/**
	 * 执行一个原生SQL茶行行行, 可以直接传SQL语句或者是xml中的查询语句名
	 * @param sqlOrQueryName
	 * @return
	 */
	public SqlQueryBuilder query(String sqlOrQueryName);

	/**
	 * 据SQL查询返回单个结果, JDBC返回的是什么类型这里也返回什么类型
	 * RawSqlQuery跟sqlQuery的差别是结果不封装进POJO,
	 * 而是转成Integer, Long, Float, BigDecimal, String, Boolean, Short等相对基本类型或者是LocalDateTime, Date, LocalDate, LocalTime类型
	 *
	 * @param queryName
	 * @return Object
	 */
	public <T> T findOne(String queryName);

	/**
	 * 跟带class参数的版本的差别就是结果集不封装到Bean里面
	 *
	 * @param queryName
	 * @return Object
	 */
	public <T> T findOne(String queryName, String paramName, Object paramValue);

	/**
	 * 返回单个对象，不存在则返回null
	 *
	 * @param queryName 可以是定义在xx.hbm.xml中的sql-query的名字, 也可以是完整的一个SQL语句, 最新的loser-orm对这块更新支持了
	 * @param paramName
	 * @param paramValue
	 * @param clazz
	 * @return
	 */
	public <T> T findOne(String queryName, String paramName, Object paramValue, Class<T> clazz);

	/**
	 * 没有分页和参数的命名SQL查询, 结果封装进clazz代表的对象中
	 *
	 * @param queryName 可以是定义在xx.hbm.xml中的sql-query的名字, 也可以是完整的一个SQL语句, 最新的loser-orm对这块更新支持了
	 * @param clazz
	 * @return List<T>
	 */
	public <T> List<T> findList(String queryName, Class<T> clazz);

	/**
	 * 不带分页的命名SQL查询, 一个参数。
	 *
	 * @param queryName 可以是定义在xx.hbm.xml中的sql-query的名字, 也可以是完整的一个SQL语句, 最新的loser-orm对这块更新支持了
	 * @param paramName
	 * @param paramValue
	 * @param clazz
	 * @return List<T>
	 */
	public <T> List<T> findList(String queryName, String paramName, Object paramValue, Class<T> clazz);

	/**
	 * 不带分页的命名SQL查询
	 *
	 * @param queryName 可以是定义在xx.hbm.xml中的sql-query的名字, 也可以是完整的一个SQL语句, 最新的loser-orm对这块更新支持了
	 * @param params
	 * @param clazz
	 * @return List<T>
	 */
	public <T> List<T> findList(String queryName, Map<String, Object> params, Class<T> clazz);


	/**
	 * 执行更新, 插入或删除语句
	 *
	 * @param queryName xml中的sql名字或者直接是一条原生SQL update, insert, delete语句
	 * @param params  SQL语句中的参数
	 * @return int 更新影响的记录数
	 */
	public int execute(String queryName, Map<String, Object> params);
	
	/**
	 * 执行更新, 插入或删除语句
	 *
	 * @param queryName xml中的sql名字或者直接是一条原生SQL update, insert, delete语句
	 * @param paramName SQL语句中的参数名, 形式为 :name
	 * @param paramValue  SQL语句中的参数值
	 * @return int 更新影响的记录数
	 */
	public int execute(String queryName, String paramName, Object paramValue);


}
