package com.awesomecopilot.common.lang.io;

import java.io.IOException;

public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * Pseudo URL prefix for all matching resources from the class path: {@code "classpath*:"}.
	 * <p>This differs from ResourceLoader's {@code "classpath:"} URL prefix in
	 * that it retrieves all matching resources for a given path &mdash; for
	 * example, to locate all "beans.xml" files in the root of all deployed JAR
	 * files you can use the location pattern {@code "classpath*:/beans.xml"}.
	 * <p>As of Spring Framework 6.0, the semantics for the {@code "classpath*:"}
	 * prefix have been expanded to include the module path as well as the class path.
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * Resolve the given location pattern into {@code Resource} objects.
	 * <p>Overlapping resource entries that point to the same physical
	 * resource should be avoided, as far as possible. The result should
	 * have set semantics.
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding {@code Resource} objects
	 * @throws IOException in case of I/O errors
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}