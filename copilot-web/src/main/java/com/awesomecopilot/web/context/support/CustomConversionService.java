package com.awesomecopilot.web.context.support;

import com.awesomecopilot.web.converter.GenericEnumConverter;
import com.awesomecopilot.web.converter.StringToArrayConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.HashSet;
import java.util.Set;

public class CustomConversionService extends DefaultConversionService {
	
	private Set<String> properties = new HashSet<>();
	
	/**
	 * 枚举 converter 单实例缓存。旧实现在 getConverter 的枚举分支里每次都
	 * new GenericEnumConverter(评审报告 P1-3), 每次枚举参数绑定都白白新建对象;
	 * 这里改为懒创建后复用, setProperties 时置空重建, 保证配置变更能生效。
	 */
	private volatile GenericEnumConverter enumConverter;
	
	/**
	 * Hook method to lookup the converter for a given sourceType/targetType pair.
	 * First queries this ConversionService's converter cache.
	 * On a cache miss, then performs an exhaustive search for a matching converter.
	 * If no converter matches, returns the default converter.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the generic converter that will perform the conversion,
	 * or {@code null} if no suitable converter was found
	 * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (Enum.class.isAssignableFrom(targetType.getObjectType())) {
			GenericEnumConverter converter = enumConverter;
			if (converter == null) {
				synchronized (this) {
					// 双检锁。注意用局部变量接力: 若在"赋值字段"和"返回"之间被别的
					// 线程调 setProperties 置空字段, 再读字段会拿到 null 返回给上层
					// (独立评审用探针复现过), 所以字段只写一次、返回一律用本地引用
					converter = enumConverter;
					if (converter == null) {
						converter = new GenericEnumConverter(properties);
						enumConverter = converter;
					}
				}
			}
			return converter;
		}
		return super.getConverter(sourceType, targetType);
	}

	public Set<String> getProperties() {
		return properties;
	}

	public void setProperties(Set<String> properties) {
		this.properties = properties;
		// 配置变了, 缓存的converter持有的是旧properties, 必须作废重建
		this.enumConverter = null;
	}
	
	public void afterPropertiesSet() {
		addConverter(new StringToArrayConverter());
	}
}
