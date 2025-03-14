package com.copilot.orm.dao;

import com.copilot.common.lang.vo.Page;

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

	public SqlQueryBuilder setSql(String sqlOrQueryName);

	public SqlQueryBuilder addParam(String paramName, Object paramValue);

	public SqlQueryBuilder addParams(Map<String, Object> params);

	public SqlQueryBuilder page(Page page);

	public SqlQueryBuilder page(int paggeNum, int pageSize);

	/**
	 * 将查到的每一行数据封装进这个class对应的对象中 <br/>
	 * 如果不指定resultClass, 那么最后返回的结果是List<Object[]>
	 * @param resultClass
	 * @return
	 * @param <T>
	 */
	public <T> SqlQueryBuilder resultClass(Class<T> resultClass);

	/**
	 * 这种情况是只取某一列, 而不是整行记录
	 * @param primitiveClass
	 * @return
	 * @param <T>
	 */
	public <T> SqlQueryBuilder primitiveClass(Class<T> primitiveClass);

	/**
	 * 是否考虑逻辑删除, 如果考虑, 逻辑删除的记录将不会被查询出来 <br/>
	 * 如果不配置, 默认的逻辑删除条件是where deleted=0
	 * @param logicalDelete 是否要添加逻辑删除的过滤条件
	 * @return
	 */
	public SqlQueryBuilder logicalDelete(boolean logicalDelete);

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
