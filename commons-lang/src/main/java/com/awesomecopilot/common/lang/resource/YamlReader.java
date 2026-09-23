package com.awesomecopilot.common.lang.resource;

import com.awesomecopilot.common.lang.transformer.Transformers;
import com.awesomecopilot.common.lang.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static java.util.Arrays.asList;

/**
 * 读取yml/yaml文件
 * <p>
 * 同时支持 .yml 和 .yaml 后缀, 优先查找 .yml, 未找到时回退 .yaml。
 * <p>
 * Copyright: (C), 2021-01-21 11:10
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class YamlReader implements YamlOps {
	
	private static final Logger log = LoggerFactory.getLogger(PropertyReader.class);
	
	private static final String WORKING_DIR = System.getProperty("user.dir");
	
	private static final String FILE_SEPRATOR = System.getProperty("file.separator");
	
	/**
	 * 支持的 YAML 后缀, 按优先级排列: .yml 优先于 .yaml
	 */
	private static final String[] YAML_SUFFIXES = {".yml", ".yaml"};
	
	/**
	 * 资源基础名称(不含后缀), 用于 getResource() 返回以及 profile 文件拼接
	 */
	private String resource;
	
	/**
	 * 这三个Yaml优先级: 低 -- 高
	 * yaml1 classpath root下
	 * yaml2 工作目录下
	 * yaml3 工作目录的config目录
	 */
	private Map<String, Object> yaml1 = null;
	private Map<String, Object> yaml2 = null;
	private Map<String, Object> yaml3 = null;
	
	/**
	 * yml/yaml的文件名, 带不带后缀都可以识别, 同时支持 .yml 和 .yaml 后缀<p>
	 * 推荐使用: YamlOps yamlOps = YamlProfileReaders.instance("application");<p>
	 * 支持profile以及工作目录, classpath下不同优先级配置文件读取<p>
	 * <p>
	 * 优先级从高到低
	 * <ol>
	 * <li/>工作目录下config目录下的同名配置文件
	 * <li/>工作目录下的同名配置文件
	 * <li/>classpath下的同名配置文件
	 * </ol>
	 * 每个位置优先查找 .yml, 未找到时回退 .yaml
	 *
	 * @param resource
	 */
	public YamlReader(String resource) {
		notNull(resource, "resource cannot be null!");
		this.resource = stripYamlSuffix(resource);
		
		Yaml yaml = new Yaml();
		/*
		 * 读取classpath下的yml/yaml
		 */
		yaml1 = loadFirst(yaml, suffix -> IOUtils.readClasspathFileAsInputStream(resource + suffix));
		/*
		 * 读取工作目录下的yml/yaml
		 */
		yaml2 = loadFirst(yaml, suffix -> {
			try {
				return IOUtils.readFileAsStream(WORKING_DIR + FILE_SEPRATOR + resource + suffix);
			} catch (IOException e) {
				log.warn(e.getMessage());
				return null;
			}
		});
		/*
		 * 读取工作目录config下的yml/yaml
		 */
		yaml3 = loadFirst(yaml, suffix -> {
			try {
				return IOUtils.readFileAsStream(WORKING_DIR + FILE_SEPRATOR + "config" + FILE_SEPRATOR + resource + suffix);
			} catch (IOException e) {
				log.warn(e.getMessage());
				return null;
			}
		});
	}
	
	/**
	 * 去除资源名称中的 .yml 或 .yaml 后缀, 返回基础名称
	 */
	private static String stripYamlSuffix(String name) {
		if (name.endsWith(".yml")) {
			return name.substring(0, name.length() - 4);
		}
		if (name.endsWith(".yaml")) {
			return name.substring(0, name.length() - 5);
		}
		return name;
	}
	
	/**
	 * 依次尝试 .yml / .yaml 后缀, 加载第一个找到的 YAML 文件
	 *
	 * @param yaml           SnakeYaml 实例
	 * @param streamProvider 根据后缀返回 InputStream 的函数, 找不到时返回 null
	 * @return 解析后的 Map, 未找到任何文件时返回 null
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFirst(Yaml yaml, Function<String, InputStream> streamProvider) {
		for (String suffix : YAML_SUFFIXES) {
			InputStream inputStream = streamProvider.apply(suffix);
			if (inputStream != null) {
				try {
					Map<String, Object> result = yaml.load(inputStream);
					inputStream.close();
					return result;
				} catch (IOException e) {
					log.warn(e.getMessage());
				}
			}
		}
		return null;
	}
	
	/**
	 * 判断yml/yaml是否存在
	 *
	 * @return
	 */
	@Override
	public boolean exists() {
		return yaml1 != null || yaml2 != null || yaml3 != null;
	}
	
	@Override
	public Integer getInt(String path) {
		try {
			Object value = get(path);
			return Transformers.convert(value, Integer.class);
		} catch (Exception e) {
			log.error("", e);
		}
		return null;
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
		Object value = get(path);
		return Transformers.convert(value, String.class);
	}
	
	@Override
	public String getString(String path, String defaultValue) {
		String value = getString(path);
		if (value == null) {
			return defaultValue;
		}
		
		return value;
	}

	@Override
	public Boolean getBoolean(String path) {
		Object value = get(path);
		return Transformers.convert(value, Boolean.class);
	}

	@Override
	public Boolean getBoolean(String path, boolean defaultValue) {
		Boolean value = getBoolean(path);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	public String getResource() {
		return this.resource;
	}
	
	private Object get(String path) {
		/*
		 * yaml中属性可以有两种写法: 
		 * ip.db.path
		 * ip:
		 *   db:
		 *     path
		 * 前者ip.db.path整体作为一个key
		 * 后者每个.号隔开的部分是一个key
		 * 
		 * 把spring.profiles.active这种key根据.拆开来
		 */
		List<Map<String, Object>> yamls = asList(yaml3, yaml2, yaml1);
		
		//先看ip.db.path整体作为一个key能不能找到
		for (Map<String, Object> yaml : yamls) {
			/*
			 * 先取工作目录config目录下的yaml文件
			 * 如果读到对应的配置项, 那么直接返回, 因为它的优先级最高
			 */
			if (yaml != null) {
				Object value = yaml.get(path);
				if (value != null) {
					return value;
				}
			}
		}
		
		//再按 . 拆开的 key 逐层下钻(P1-14: 单个源下钻失败要换下一个源, 见 drillDown)
		return drillDown(yamls, path);
	}
	
	/**
	 * 按 . 拆分的 key 在一组 yaml 源(按优先级从高到低)里逐层下钻取值。
	 * <p>
	 * P1-14: 修复前某层取不到就直接 return null——高优先级文件里只有半个 key 前缀
	 * (如只有 copilot.other, 没有 copilot.text)时, 低优先级文件里的完整值被整个挡掉。
	 * 现在该源下钻失败即换下一个源, 全部源试完才返回 null。
	 * 中途节点不是 Map(值是标量)时同样视为该源无此路径, 不抛 ClassCastException。
	 */
	@SuppressWarnings("unchecked")
	static Object drillDown(List<Map<String, Object>> yamls, String path) {
		String[] paths = path.split("\\.");
		for (Map<String, Object> yaml : yamls) {
			Object temp = yaml;
			for (int i = 0; i < paths.length && temp != null; i++) {
				if (!(temp instanceof Map)) {
					temp = null; //当前层是标量, 无法继续下钻
					break;
				}
				temp = ((Map<Object, Object>) temp).get(paths[i]);
			}
			if (temp != null) {
				return temp;
			}
		}
		return null;
	}
	
}
