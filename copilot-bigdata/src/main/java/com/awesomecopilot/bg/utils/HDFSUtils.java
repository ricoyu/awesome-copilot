package com.awesomecopilot.bg.utils;

import com.awesomecopilot.common.lang.resource.PropertyReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Hadoop HDFS 工具类
 * <p/>
 * Copyright: Copyright (c) 2025-06-14 16:06
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HDFSUtils {

	private static final Logger log = LoggerFactory.getLogger(HDFSUtils.class);

	private static FileSystem fs;

	static {
		/**
		 * 默认读取classpath下hdfs.properties文件
		 */
		PropertyReader propertyReader = new PropertyReader("hdfs");
		Configuration conf = new Configuration();
		// 设置HDFS地址
		conf.set("fs.defaultFS", propertyReader.getString("fs.defaultFS"));
		// 根据需要可以设置其他配置
		 conf.set("dfs.replication", propertyReader.getString("dfs.replication"));

		// 获取FileSystem实例
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e) {
			log.error("", e);
			throw new RuntimeException(e);
		}
		System.setProperty("hadoop.home.dir", "C:\\hadoop");
	}

	/**
	 * 创建目录
	 * @param dir
	 * @return boolean 创建成功与否
	 */
	public static boolean mkdir(String dir) {
		Path path = new Path(dir);
		try {
			if (fs.exists(path)) {
				log.info("目录已存在");
				return false;
			} else {
				boolean success = fs.mkdirs(path);
				log.info(success ? "目录创建成功" : "目录创建失败");
				return success;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
