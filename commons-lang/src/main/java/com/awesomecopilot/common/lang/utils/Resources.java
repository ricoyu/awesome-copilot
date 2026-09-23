package com.awesomecopilot.common.lang.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class Resources {
	
	/**
	 * for all elements of java.class.path get a Collection of resources Pattern
	 * pattern = Pattern.compile(".*"); gets all resources
	 *
	 * @param pattern the pattern to match
	 * @return the resources in the order they are found
	 */
	public static List<File> getResources(Pattern pattern) {
		List<File> retval = new ArrayList<File>();
		String classPath = System.getProperty("java.class.path", ".");
		String[] classPathElements = classPath.split(System.getProperty("path.separator"));
		for (String element : classPathElements) {
			retval.addAll(getResources(element, pattern));
		}
		return retval;
	}
	
	public static List<File> getResources(String fileName) {
		List<File> retval = new ArrayList<File>();
		String classPath = System.getProperty("java.class.path", ".");
		String[] classPathElements = classPath.split(System.getProperty("path.separator"));
		for (String element : classPathElements) {
			retval.addAll(getResources(element, fileName));
		}
		return retval;
	}
	
	private static List<File> getResources(String element, Pattern pattern) {
		List<File> files = new ArrayList<File>();
		File file = new File(element);
		if (file.isDirectory()) {
			files.addAll(getResourcesFromDirectory(file, pattern));
		}
		return files;
	}
	
	private static List<File> getResources(String element, String fileName) {
		List<File> files = new ArrayList<File>();
		File file = new File(element);
		if (file.isDirectory()) {
			files.addAll(getResourcesFromDirectory(file, fileName));
		}
		return files;
	}
	
	private static List<File> getResourcesFromDirectory(File directory, Pattern pattern) {
		List<File> files = new ArrayList<File>();
		//P2-19(CODE_REVIEW_REPORT): File.listFiles() 在权限不足/IO 错误/目录被并发删除时
		//返回 null, 修复前直接 for 遍历抛 NullPointerException(实测复现)。null 按"此目录
		//无文件可读"处理, 不影响其余 classpath 元素的扫描。
		File[] fileList = directory.listFiles();
		if (fileList == null) {
			return files;
		}
		for (File file : fileList) {
			if (file.isDirectory()) {
				files.addAll(getResourcesFromDirectory(file, pattern));
			} else {
				try {
					String fileName = file.getCanonicalPath();
					boolean accept = pattern.matcher(fileName).matches();
					if (accept) {
						files.add(file);
					}
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		}
		return files;
	}
	
	/**
	 * 如果fileOrDirectory是普通文件，则比较文件名是否与fileName相同
	 * 否则递归查询fileOrDirectory的子目录，重复以上步骤
	 * 找到则加入files
	 *
	 * @param fileOrDirectory
	 * @param fileName
	 * @return
	 */
	private static List<File> getResourcesFromDirectory(File fileOrDirectory, String fileName) {
		List<File> files = new ArrayList<File>();
		//P2-19: 同 Pattern 重载, listFiles() 可能为 null(权限/IO/并发删除)
		File[] fileList = fileOrDirectory.listFiles();
		if (fileList == null) {
			return files;
		}
		for (File file : fileList) {
			if (file.isDirectory()) {
				files.addAll(getResourcesFromDirectory(file, fileName));
			} else {
				String foundFileName = file.getName();
				if (foundFileName.equals(fileName)) {
					files.add(file);
				}
			}
		}
		return files;
	}
	
}