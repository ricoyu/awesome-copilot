package com.awesomecopilot.orm.function;

import java.math.BigDecimal;

/**
 * BigDecimal转int型
 * @author Rico
 * @since May 27, 2016
 * @version 
 *
 */
@FunctionalInterface
public interface BigDecimal2IntFunction{

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    int apply(BigDecimal value);
}