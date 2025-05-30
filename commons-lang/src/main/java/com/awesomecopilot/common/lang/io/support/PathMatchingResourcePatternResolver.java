package com.awesomecopilot.common.lang.io.support;

import com.awesomecopilot.common.lang.io.FileSystemResource;
import com.awesomecopilot.common.lang.io.Resource;
import com.awesomecopilot.common.lang.io.ResourceLoader;
import com.awesomecopilot.common.lang.io.ResourcePatternResolver;
import com.awesomecopilot.common.lang.io.ResourceUtils;
import com.awesomecopilot.common.lang.io.UrlResource;
import com.awesomecopilot.common.lang.io.VfsResource;
import com.awesomecopilot.common.lang.io.annotations.Nullable;
import com.awesomecopilot.common.lang.io.core.AntPathMatcher;
import com.awesomecopilot.common.lang.io.core.DefaultResourceLoader;
import com.awesomecopilot.common.lang.io.core.NativeDetector;
import com.awesomecopilot.common.lang.io.core.PathMatcher;
import com.awesomecopilot.common.lang.utils.Assert;
import com.awesomecopilot.common.lang.utils.ClassUtils;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.common.lang.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Logger logger = LoggerFactory.getLogger(PathMatchingResourcePatternResolver.class);

	/**
	 * {@link Set} of {@linkplain ModuleFinder#ofSystem() system module} names.
	 * @since 6.0
	 * @see #isNotSystemModule
	 */
	private static final Set<String> systemModuleNames = NativeDetector.inNativeImage() ? Collections.emptySet() :
			ModuleFinder.ofSystem().findAll().stream()
					.map(moduleReference -> moduleReference.descriptor().name())
					.collect(Collectors.toSet());

	/**
	 * is not a {@linkplain ModuleFinder#ofSystem() system module}.
	 * @since 6.0
	 * @see #systemModuleNames
	 */
	private static final Predicate<ResolvedModule> isNotSystemModule =
			resolvedModule -> !systemModuleNames.contains(resolvedModule.name());

	private static Method equinoxResolveMethod;

	static {
		try {
			// Detect Equinox OSGi (e.g. on WebSphere 6.1)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
		}
		catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}


	private final ResourceLoader resourceLoader;

	private PathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * Create a {@code PathMatchingResourcePatternResolver} with a
	 * <p>ClassLoader access will happen via the thread context class loader.
	 */
	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * Create a {@code PathMatchingResourcePatternResolver} with the supplied
	 * {@link ResourceLoader}.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @param resourceLoader the {@code ResourceLoader} to load root directories
	 * and actual resources with
	 */
	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Create a {@code PathMatchingResourcePatternResolver} with a
	 * {@link DefaultResourceLoader} and the supplied {@link ClassLoader}.
	 * @param classLoader the ClassLoader to load class path resources with,
	 * or {@code null} for using the thread context class loader
	 * at the time of actual resource access
	 */
	public PathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}


	/**
	 * Return the {@link ResourceLoader} that this pattern resolver works with.
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * Set the {@link PathMatcher} implementation to use for this
	 * resource pattern resolver.
	 * <p>Default is {@link AntPathMatcher}.
	 * @see AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the {@link PathMatcher} that this resource pattern resolver uses.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}


	@Override
	public Resource getResource(String location) {
		return getResourceLoader().getResource(location);
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			String locationPatternWithoutPrefix = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
			// Search the module path first.
			Set<Resource> resources = findAllModulePathResources(locationPatternWithoutPrefix);
			// Search the class path next.
			if (getPathMatcher().isPattern(locationPatternWithoutPrefix)) {
				// a class path resource pattern
				Collections.addAll(resources, findPathMatchingResources(locationPattern));
			}
			else {
				// all class path resources with the given name
				Collections.addAll(resources, findAllClassPathResources(locationPatternWithoutPrefix));
			}
			return resources.toArray(new Resource[0]);
		}
		else {
			// Generally only look for a pattern after a prefix here,
			// and on Tomcat only after the "*/" separator for its "war:" protocol.
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}

	/**
	 * Find all class location resources with the given location via the ClassLoader.
	 * <p>Delegates to {@link #doFindAllClassPathResources(String)}.
	 * @param location the absolute path within the class path
	 * @return the result as Resource array
	 * @throws IOException in case of I/O errors
	 * @see java.lang.ClassLoader#getResources
	 * @see #convertClassLoaderURL
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = stripLeadingSlash(location);
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved class path location [" + path + "] to resources " + result);
		}
		return result.toArray(new Resource[0]);
	}

	/**
	 * Find all class path resources with the given path via the configured
	 * {@link #getClassLoader() ClassLoader}.
	 * <p>Called by {@link #findAllClassPathResources(String)}.
	 * @param path the absolute path within the class path (never a leading slash)
	 * @return a mutable Set of matching Resource instances
	 * @since 4.1.1
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(16);
		ClassLoader cl = getClassLoader();
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		if (!StringUtils.hasLength(path)) {
			// The above result is likely to be incomplete, i.e. only containing file system references.
			// We need to have pointers to each of the jar files on the class path as well...
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * Convert the given URL as returned from the configured
	 * {@link #getClassLoader() ClassLoader} into a {@link Resource}, applying
	 * to path lookups without a pattern (see {@link #findAllClassPathResources}).
	 * in case of the "file" protocol or a {@link UrlResource} otherwise, matching
	 * the outcome of pattern-based class path traversal in the same resource layout,
	 * as well as matching the outcome of module path searches.
	 * @param url a URL as returned from the configured ClassLoader
	 * @return the corresponding Resource object
	 * @see java.lang.ClassLoader#getResources
	 * @see #doFindAllClassPathResources
	 * @see #doFindPathMatchingFileResources
	 */
	protected Resource convertClassLoaderURL(URL url) {
		if (ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol())) {
			try {
				// URI decoding for special characters such as spaces.
				return new FileSystemResource(ResourceUtils.toURI(url).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new FileSystemResource(url.getFile());
			}
		}
		else {
			return new UrlResource(url);
		}
	}

	/**
	 * Search all {@link URLClassLoader} URLs for jar file references and add each to the
	 * given set of resources in the form of a pointer to the root of the jar file content.
	 * @param classLoader the ClassLoader to search (including its ancestors)
	 * @param result the set of resources to add jar roots to
	 * @since 4.1.1
	 */
	protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, Set<Resource> result) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			try {
				for (URL url : urlClassLoader.getURLs()) {
					try {
						UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
								new UrlResource(url) :
								new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					}
					catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}

		if (classLoader == ClassLoader.getSystemClassLoader()) {
			// JAR "Class-Path" manifest header evaluation...
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				// Hierarchy traversal...
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}

	/**
	 * Determine jar file references from {@code Class-Path} manifest entries (which
	 * are added to the {@code java.class.path} JVM system property by the system
	 * class loader) and add each to the given set of resources in the form of
	 * a pointer to the root of the jar file content.
	 * @param result the set of resources to add jar roots to
	 * @since 4.3
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		try {
			String javaClassPathProperty = System.getProperty("java.class.path");
			for (String path : StringUtils.delimitedListToStringArray(javaClassPathProperty, File.pathSeparator)) {
				try {
					String filePath = new File(path).getAbsolutePath();
					int prefixIndex = filePath.indexOf(':');
					if (prefixIndex == 1) {
						// Possibly a drive prefix on Windows (for example, "c:"), so we prepend a slash
						// and convert the drive letter to uppercase for consistent duplicate detection.
						filePath = "/" + org.apache.commons.lang3.StringUtils.capitalize(filePath);
					}
					// Since '#' can appear in directories/filenames, java.net.URL should not treat it as a fragment
					filePath = StringUtils.replace(filePath, "#", "%23");
					// Build URL that points to the root of the jar file
					UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
							ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
					// Potentially overlapping with URLClassLoader.getURLs() result in addAllClassLoaderJarRoots().
					if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
						result.add(jarResource);
					}
				}
				catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
		}
	}

	/**
	 * Check whether the given file path has a duplicate but differently structured entry
	 * in the existing result, i.e. with or without a leading slash.
	 * @param filePath the file path (with or without a leading slash)
	 * @param result the current result
	 * @return {@code true} if there is a duplicate (i.e. to ignore the given file path),
	 * {@code false} to proceed with adding a corresponding resource to the current result
	 */
	private boolean hasDuplicate(String filePath, Set<Resource> result) {
		if (result.isEmpty()) {
			return false;
		}
		String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
		try {
			return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
					duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
		}
		catch (MalformedURLException ex) {
			// Ignore: just for testing against duplicate.
			return false;
		}
	}

	/**
	 * Find all resources that match the given location pattern via the Ant-style
	 * {@link #getPathMatcher() PathMatcher}.
	 * <p>Supports resources in OSGi bundles, JBoss VFS, jar files, zip files,
	 * and file systems.
	 * @param locationPattern the location pattern to match
	 * @return the result as Resource array
	 * @throws IOException in case of I/O errors
	 * @see #determineRootDir(String)
	 * @see #resolveRootDirResource(Resource)
	 * @see #isJarResource(Resource)
	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
	 * @see #doFindPathMatchingFileResources(Resource, String)
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		String rootDirPath = determineRootDir(locationPattern);
		String subPattern = locationPattern.substring(rootDirPath.length());
		Resource[] rootDirResources = getResources(rootDirPath);
		Set<Resource> result = new LinkedHashSet<>(64);
		for (Resource rootDirResource : rootDirResources) {
			rootDirResource = resolveRootDirResource(rootDirResource);
			URL rootDirUrl = rootDirResource.getURL();
			if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
				URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
				if (resolvedUrl != null) {
					rootDirUrl = resolvedUrl;
				}
				rootDirResource = new UrlResource(rootDirUrl);
			}
			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
			}
			else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
			}
			else {
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		return result.toArray(new Resource[0]);
	}

	/**
	 * Determine the root directory for the given location.
	 * <p>Used for determining the starting point for file matching, resolving the
	 * root directory location to be passed into {@link #getResources(String)},
	 * with the remainder of the location to be used as the sub pattern.
	 * <p>Will return "/WEB-INF/" for the location "/WEB-INF/*.xml", for example.
	 * @param location the location to check
	 * @return the part of the location that denotes the root directory
	 * @see #findPathMatchingResources(String)
	 */
	protected String determineRootDir(String location) {
		int prefixEnd = location.indexOf(':') + 1;
		int rootDirEnd = location.length();
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		return location.substring(0, rootDirEnd);
	}

	/**
	 * Resolve the supplied root directory resource for path matching.
	 * <p>By default, {@link #findPathMatchingResources(String)} resolves Equinox
	 * OSGi "bundleresource:" and "bundleentry:" URLs into standard jar file URLs
	 * that will be traversed using Spring's standard jar file traversal algorithm.
	 * <p>For any custom resolution, override this template method and replace the
	 * supplied resource handle accordingly.
	 * <p>The default implementation of this method returns the supplied resource
	 * unmodified.
	 * @param original the resource to resolve
	 * @return the resolved resource (may be identical to the supplied resource)
	 * @throws IOException in case of resolution failure
	 * @see #findPathMatchingResources(String)
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * Determine if the given resource handle indicates a jar resource that the
	 * {@link #doFindPathMatchingJarResources} method can handle.
	 * <p>{@link #findPathMatchingResources(String)} delegates to
	 * {@link ResourceUtils#isJarURL(URL)} to determine whether the given URL
	 * points to a resource in a jar file, and only invokes this method as a fallback.
	 * <p>This template method therefore allows for detecting further kinds of
	 * jar-like resources &mdash; for example, via {@code instanceof} checks on
	 * the resource handle type.
	 * <p>The default implementation of this method returns {@code false}.
	 * @param resource the resource handle to check (usually the root directory
	 * to start path matching from)
	 * @return {@code true} if the given resource handle indicates a jar resource
	 * @throws IOException in case of I/O errors
	 * @see #findPathMatchingResources(String)
	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * Find all resources in jar files that match the given location pattern
	 * via the Ant-style {@link #getPathMatcher() PathMatcher}.
	 * @param rootDirResource the root directory as Resource
	 * @param rootDirUrl the pre-resolved root directory URL
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 * @since 4.3
	 * @see java.net.JarURLConnection
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirUrl, String subPattern)
			throws IOException {

		URLConnection con = rootDirUrl.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection jarCon) {
			// Should usually be the case for traditional JAR files.
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		}
		else {
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			String urlFile = rootDirUrl.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
				if (separatorIndex == -1) {
					separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				}
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar class path entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// Root entry path must end with slash to allow for proper matching.
				// The Sun JRE does not return a slash here, but BEA JRockit does.
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(64);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * Resolve the given jar file URL into a JarFile object.
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			}
			catch (URISyntaxException ex) {
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * Find all resources in the file system of the supplied root directory that
	 * match the given location sub pattern via the Ant-style {@link #getPathMatcher()
	 * PathMatcher}.
	 * @param rootDirResource the root directory as a Resource
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		Set<Resource> result = new LinkedHashSet<>(64);
		URI rootDirUri;
		try {
			rootDirUri = rootDirResource.getURI();
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to resolve directory [%s] as URI: %s".formatted(rootDirResource, ex));
			}
			return result;
		}

		Path rootPath = null;
		if (rootDirUri.isAbsolute() && !rootDirUri.isOpaque()) {
			// Prefer Path resolution from URI if possible
			try {
				try {
					rootPath = Path.of(rootDirUri);
				}
				catch (FileSystemNotFoundException ex) {
					// If the file system was not found, assume it's a custom file system that needs to be installed.
					FileSystems.newFileSystem(rootDirUri, Map.of(), ClassUtils.getDefaultClassLoader());
					rootPath = Path.of(rootDirUri);
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to resolve %s in file system: %s".formatted(rootDirUri, ex));
				}
				// Fallback via Resource.getFile() below
			}
		}

		if (rootPath == null) {
			// Resource.getFile() resolution as a fallback -
			// for custom URI formats and custom Resource implementations
			try {
				rootPath = Path.of(rootDirResource.getFile().getAbsolutePath());
			}
			catch (FileNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot search for matching files underneath " + rootDirResource +
							" in the file system: " + ex.getMessage());
				}
				return result;
			}
			catch (Exception ex) {
				if (logger.isInfoEnabled()) {
					logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
				}
				return result;
			}
		}

		if (!Files.exists(rootPath)) {
			if (logger.isInfoEnabled()) {
				logger.info("Skipping search for files matching pattern [%s]: directory [%s] does not exist"
						.formatted(subPattern, rootPath.toAbsolutePath()));
			}
			return result;
		}

		String rootDir = StringUtils.cleanPath(rootPath.toString());
		if (!rootDir.endsWith("/")) {
			rootDir += "/";
		}

		Path rootPathForPattern = rootPath;
		String resourcePattern = rootDir + StringUtils.cleanPath(subPattern);
		Predicate<Path> isMatchingFile = path -> (!path.equals(rootPathForPattern) &&
				getPathMatcher().match(resourcePattern, StringUtils.cleanPath(path.toString())));

		if (logger.isTraceEnabled()) {
			logger.trace("Searching directory [%s] for files matching pattern [%s]"
					.formatted(rootPath.toAbsolutePath(), subPattern));
		}

		try (Stream<Path> files = Files.walk(rootPath)) {
			files.filter(isMatchingFile).sorted().map(FileSystemResource::new).forEach(result::add);
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to search in directory [%s] for files matching pattern [%s]: %s"
						.formatted(rootPath.toAbsolutePath(), subPattern, ex));
			}
		}
		return result;
	}

	/**
	 * Resolve the given location pattern into {@code Resource} objects for all
	 * matching resources found in the module path.
	 * <p>The location pattern may be an explicit resource path such as
	 * {@code "com/example/config.xml"} or a pattern such as
	 * <code>"com/example/**&#47;config-*.xml"</code> to be matched using the
	 * configured {@link #getPathMatcher() PathMatcher}.
	 * <p>The default implementation scans all modules in the {@linkplain ModuleLayer#boot()
	 * boot layer}, excluding {@linkplain ModuleFinder#ofSystem() system modules}.
	 * @param locationPattern the location pattern to resolve
	 * @return a modifiable {@code Set} containing the corresponding {@code Resource}
	 * objects
	 * @throws IOException in case of I/O errors
	 * @since 6.0
	 * @see ModuleLayer#boot()
	 * @see ModuleFinder#ofSystem()
	 * @see ModuleReader
	 * @see PathMatcher#match(String, String)
	 */
	protected Set<Resource> findAllModulePathResources(String locationPattern) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(64);

		// Skip scanning the module path when running in a native image.
		if (NativeDetector.inNativeImage()) {
			return result;
		}

		String resourcePattern = stripLeadingSlash(locationPattern);
		Predicate<String> resourcePatternMatches = (getPathMatcher().isPattern(resourcePattern) ?
				path -> getPathMatcher().match(resourcePattern, path) :
				resourcePattern::equals);

		try {
			ModuleLayer.boot().configuration().modules().stream()
					.filter(isNotSystemModule)
					.forEach(resolvedModule -> {
						// NOTE: a ModuleReader and a Stream returned from ModuleReader.list() must be closed.
						try (ModuleReader moduleReader = resolvedModule.reference().open();
								Stream<String> names = moduleReader.list()) {
							names.filter(resourcePatternMatches)
									.map(name -> findResource(moduleReader, name))
									.filter(Objects::nonNull)
									.forEach(result::add);
						}
						catch (IOException ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("Failed to read contents of module [%s]".formatted(resolvedModule), ex);
							}
							throw new UncheckedIOException(ex);
						}
					});
		}
		catch (UncheckedIOException ex) {
			// Unwrap IOException to conform to this method's contract.
			throw ex.getCause();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Resolved module-path location pattern [%s] to resources %s".formatted(resourcePattern, result));
		}
		return result;
	}

	@Nullable
	private Resource findResource(ModuleReader moduleReader, String name) {
		try {
			return moduleReader.find(name)
					.map(this::convertModuleSystemURI)
					.orElse(null);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to find resource [%s] in module path".formatted(name), ex);
			}
			return null;
		}
	}

	/**
	 * If it's a "file:" URI, use {@link FileSystemResource} to avoid duplicates
	 * for the same path discovered via class path scanning.
	 */
	private Resource convertModuleSystemURI(URI uri) {
		return (ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme()) ?
				new FileSystemResource(uri.getPath()) : UrlResource.from(uri));
	}

	private static String stripLeadingSlash(String path) {
		return (path.startsWith("/") ? path.substring(1) : path);
	}


	/**
	 * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirUrl, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirUrl);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * VFS visitor for path matching purposes.
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<>(64);

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				switch (methodName) {
					case "equals" -> {
						// Only consider equal when proxies are identical.
						return (proxy == args[0]);
					}
					case "hashCode" -> {
						return System.identityHashCode(proxy);
					}
				}
			}
			return switch (methodName) {
				case "getAttributes" -> getAttributes();
				case "visit" -> {
					visit(args[0]);
					yield null;
				}
				case "toString" -> toString();
				default -> throw new IllegalStateException("Unexpected method invocation: " + method);
			};
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		@Nullable
		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttributes();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}

}