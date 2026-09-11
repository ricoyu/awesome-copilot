package com.awesomecopilot.common.lang.resource;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * <p>
 * Copyright: (C), 2021-01-21 14:05
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class YamlProfileReaders implements YamlOps {
	
	/**
	 * 按 resource 名缓存的进程级实例。YamlReader 构造时会立即读盘(classpath/工作目录/config
	 * 目录共 6 个候选文件)并做 YAML 解析,单次可达几十毫秒;本类构造后全部字段只读,线程安全,
	 * 故可安全共享。高频调用点(如 ORM 每次 query)不要再重复 new。<p>
	 * 需要感知配置文件变更(热加载)时调用 {@link #clearCache()} 后重新获取。
	 */
	private static final ConcurrentMap<String, YamlProfileReaders> INSTANCES = new ConcurrentHashMap<>();
	
	private YamlReader yamlReader;
	
	private YamlReader profileReader;
	
	/**
	 * yml的文件名, 不带.yml后缀<p>
	 * 支持profile以及工作目录, classpath下不同优先级配置文件读取<p>
	 *
	 * 优先级从高到低
	 * <ol>
	 * <li/>工作目录下config目录下的同名配置文件
	 * <li/>工作目录下的同名配置文件
	 * <li/>classpath下的同名配置文件
	 * </ol>
	 * 同一 resource 名在进程内只解析一次,返回共享缓存实例;
	 * 需要重新读文件时先调 {@link #clearCache()}。
	 * @param resource
	 */
	public static YamlProfileReaders instance(String resource) {
		return INSTANCES.computeIfAbsent(resource, r -> new YamlProfileReaders(new YamlReader(r)));
	}
	
	/**
	 * 清空缓存,下次 instance(resource) 会重新读盘解析。用于配置文件热加载场景或单元测试。
	 */
	public static void clearCache() {
		INSTANCES.clear();
	}
	
	public YamlProfileReaders(YamlReader yamlReader) {
		Objects.requireNonNull(yamlReader, "yamlReader cannot be null!");
		this.yamlReader = yamlReader;
		String profile = yamlReader.getString("spring.profiles.active");
		if (isNotBlank(profile)) {
			this.profileReader = new YamlReader(yamlReader.getResource() + "-" + profile);
		}
	}
	
	@Override
	public boolean exists() {
		return yamlReader.exists();
	}
	
	@Override
	public Integer getInt(String path) {
		if (profileReader == null) {
			return yamlReader.getInt(path);
		}
		
		Integer value = profileReader.getInt(path);
		if (value != null) {
			return value;
		}
		
		return yamlReader.getInt(path);
	}
	
	@Override
	public Integer getInt(String path, Integer defaultValue) {
		Integer value = getInt(path);
		if (value == null) {
			return defaultValue;
		}
		
		return value;
	}
	
	@Override
	public String getString(String path) {
		if (profileReader == null) {
			return yamlReader.getString(path);
		}
		
		String value = profileReader.getString(path);
		if (isNotBlank(value)) {
			return value;
		}
		
		return yamlReader.getString(path);
	}
	
	@Override
	public String getString(String path, String defaultValue) {
		String value = getString(path);
		if (isBlank(value)) {
			return defaultValue;
		}
		
		return value;
	}

	@Override
	public Boolean getBoolean(String path) {
		if (profileReader == null) {
			return yamlReader.getBoolean(path);
		}

		Boolean value = profileReader.getBoolean(path);
		return value;
	}

	@Override
	public Boolean getBoolean(String path, boolean defaultValue) {
		Boolean value = getBoolean(path);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
}
