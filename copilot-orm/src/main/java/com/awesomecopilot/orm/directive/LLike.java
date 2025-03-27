/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.awesomecopilot.orm.directive;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.directive.DirectiveConstants;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

/**
 * #llike("column_name", "%" + $param)
 * "column_name" 是表的字段名, 记得一定要用引号括起来, 否则会报错
 * 如果param为null则不会生成 column_name like %param条件
 * <p>
 * Copyright: Copyright (c) 2018-06-07 16:25
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class LLike extends Directive {

	public String getName() {
		return "llike";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.velocity.runtime.directive.Directive#getType()
	 */
	public int getType() {
		return DirectiveConstants.LINE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.velocity.runtime.directive.Directive#render(org.apache.velocity.
	 * context.InternalContextAdapter, java.io.Writer,
	 * org.apache.velocity.runtime.parser.node.Node)
	 */
	public boolean render(InternalContextAdapter context, Writer writer,
			Node node) throws IOException, ResourceNotFoundException,
			ParseErrorException, MethodInvocationException {
		Object column = node.jjtGetChild(0).value(context); //拿到#like("column_name", $param)的column_name
		//拿到 #like("column_name", $param)的param值
		Object likeCondition = "'%" + node.jjtGetChild(1).value(context) +"'";

		//取变量的名字，如拿到的是$param, 转成:param
		String likeParam = ":" + node.jjtGetChild(1).literal().substring(1);

		if (likeParam != null && !likeParam.isEmpty()) {
			writer.append(column + " like " + likeCondition);
		}
		return true;
	}

}
