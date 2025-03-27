package com.awesomecopilot.common.lang.io.support;

import com.awesomecopilot.common.lang.io.VfsUtils;
import com.awesomecopilot.common.lang.io.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

abstract class VfsPatternUtils extends VfsUtils {

	@Nullable
	static Object getVisitorAttributes() {
		return doGetVisitorAttributes();
	}

	static String getPath(Object resource) {
		String path = doGetPath(resource);
		return (path != null ? path : "");
	}

	static Object findRoot(URL url) throws IOException {
		return getRoot(url);
	}

	static void visit(Object resource, InvocationHandler visitor) throws IOException {
		Object visitorProxy = Proxy.newProxyInstance(
				VIRTUAL_FILE_VISITOR_INTERFACE.getClassLoader(),
				new Class<?>[] {VIRTUAL_FILE_VISITOR_INTERFACE}, visitor);
		invokeVfsMethod(VIRTUAL_FILE_METHOD_VISIT, resource, visitorProxy);
	}

}