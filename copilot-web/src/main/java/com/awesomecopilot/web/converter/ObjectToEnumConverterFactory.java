package com.awesomecopilot.web.converter;

import com.awesomecopilot.common.lang.utils.EnumUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class ObjectToEnumConverterFactory implements ConverterFactory<Object, Enum> {
	
	private Set<String> properties = new HashSet<>();
	
	@SuppressWarnings({"unchecked"})
	@Override
	public <T extends Enum> Converter<Object, T> getConverter(Class<T> targetType) {
		return new ObjectToEnumConverter(targetType, getProperties());
	}
	
	public Set<String> getProperties() {
		return properties;
	}
	
	public void setProperties(Set<String> properties) {
		this.properties = properties;
	}
	
	public final class ObjectToEnumConverter<T extends Enum> implements Converter<Object, Enum> {
		
		private Class<T> enumType;
		
		private Set<String> properties = new HashSet<>();
		
		public ObjectToEnumConverter(Class<T> enumType, Set<String> properties) {
			this.enumType = enumType;
			this.properties = properties;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public T convert(Object source) {
			for (String property : properties) {
				Object resultEnum = EnumUtils.lookupEnum(enumType, source, property);
				if (resultEnum == null) {
					continue;
				}
				return (T) resultEnum;
			}
			/*
			 * 属性都匹配不到时回退到按 name(字符串) / ordinal(数字) 匹配,
			 * 与 GenericEnumConverter 行为一致(评审报告 P1-6): 旧实现没有这一步,
			 * 不配置 properties 时本工厂对任何输入都返回 null。
			 */
			return (T) EnumUtils.lookupEnum(enumType, source);
		}
		
	}
}
