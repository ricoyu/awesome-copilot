package com.awesomecopilot.orm.interceptor;

import com.awesomecopilot.common.lang.utils.SqlUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * 如果原始SQL中不包含deleted=0的条件, 则加上, 否则不加 <p>
 * 如果原始SQL中不包含tenant_id=xxx的条件, 并且http请求头中携带了Tenant-Id, 那么加上tenant_id=xxx的条件, 否则不加
 * 需要把这个类名配置到
 * <pre> {@code
 * spring:
 *   jpa:
 *     properties:
 *       hibernate:
 *         session_factory:
 *           statement_inspector: com.awesomecopilot.orm.interceptor.DeletedTenantIdConditionInterceptor
 * }</pre>
 * <p>
 * 对SQLOperations和CriteriaOperations都有效
 * <p/>
 * Copyright: Copyright (c) 2025-04-08 12:32
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DeletedTenantIdConditionInterceptor implements StatementInspector {
    @Override
    public String inspect(String sql) {
        // 在已有WHERE后追加条件，或新增WHERE
        return SqlUtils.addDeleteTenantIdCondition(sql);
    }
}