package com.awesomecopilot.orm.dao;

import com.awesomecopilot.common.lang.vo.OrderBean;
import com.awesomecopilot.common.lang.vo.OrderBean.DIRECTION;
import com.awesomecopilot.common.lang.vo.Page;

import java.util.List;
import java.util.Map;

/**
 * 原生SQL查询构建器, 为了防止采用方法重载导致的接口臃肿问题
 * <p/>
 * Copyright: Copyright (c) 2025-03-09 16:46
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public interface SqlQueryBuilder {

	/**
	 * 将一个查询条件塞到Map<String, Object> params = new HashMap<>()里面, 作为SQL查询的参数值
	 * @param paramName
	 * @param paramValue
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder addParam(String paramName, Object paramValue);

	/**
	 * 将多个查询条件塞到Map<String, Object> params = new HashMap<>()里面, 作为SQL查询的参数值
	 * @param params
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder addParams(Map<String, Object> params);

	/**
	 * 如果paramValue不为null, 将paramValue改成 "%"+paramValue
	 * @param paramName
	 * @param paramValue
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder addLlikeParam(String paramName, String paramValue);

	/**
	 * 如果paramValue不为null, 将paramValue改成 paramValue+"%"
	 * @param paramName
	 * @param paramValue
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder addRlikeParam(String paramName, String paramValue);

	/**
	 * 如果paramValue不为null, 将paramValue改成 "%"+paramValue+"%"
	 * @param paramName
	 * @param paramValue
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder addlikeParam(String paramName, String paramValue);

	/**
	 * page对象可以包含分页参数业绩排序规则
	 * @param page
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder page(Page page);

	/**
	 * 仅设置分页参数
	 * @param paggeNum
	 * @param pageSize
	 * @return
	 */
	public SqlQueryBuilder page(int paggeNum, int pageSize);

	/**
	 * 设置主排序字段
	 * @param order
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder order(OrderBean order);

	/**
	 * 设置主排序字段
	 * @param orderBy
	 * @param direction
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder order(String orderBy, DIRECTION direction);

	/**
	 * 排序规则
	 * <p>
	 * 可以就写一个字段名(表字段名, 非bean属性名), 表示按这个字段升序排, 也可以在字段名后加“:asc或:desc”指定升序 (降序), 比如"catelog_name:desc" 表示 按catelog_name字段降序排
	 * <p>
	 * （在字段名后加“:asc或:desc”指定升序 (降序), 多个字段使用逗号分隔, 省略排序默认使用升序)", example = "“字段1,字段2” 或者 “字段1:asc,字段2:desc”
	 * @param order
	 * @return SqlQueryBuilder
	 */
	public SqlQueryBuilder order(String order);

	/**
	 * 将查到的每一行数据封装进这个class对应的对象中 <br/>
	 * 如果不指定resultClass, 那么最后返回的结果是List<Object[]>
	 * @param resultClass
	 * @return
	 * @param <T>
	 */
	public <T> SqlQueryBuilder resultClass(Class<T> resultClass);

	/**
	 * <ul>返回列表结果
	 *     <li/>如果指定了resultClass, 那么每行记录都封装进一个对象中, 然后以list形式返回
	 *     <li/>如果指定了primitiveClass, 这种情况是只取某一列, 而不是整行记录, 然后以list形式返回所有这列数据, 这是返回单个值的列表
	 * </ul>
	 * @return
	 * @param <T>
	 */
	public <T> List<T> findList();

	/**
	 * 传入的page对象相关属性值会自动更新
	 * @return
	 * @param <T>
	 */
	public <T> List<T> findPage();

	/**
	 * <ul>返回列表结果
	 *     <li/>如果指定了resultClass, 那么结果封装进这个resultClass对应的对象中, 然后返回这个对象
	 *     <li/>如果指定了primitiveClass, 这种情况是只取某一列, 而不是整行记录, 然后将这个列的数据转成primitiveClass类型后返回, 这是不封装进POJO的形式
	 * </ul>
	 * @return
	 * @param <T>
	 */
	public <T> T findOne();
}
