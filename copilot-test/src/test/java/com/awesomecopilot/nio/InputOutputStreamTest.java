package com.awesomecopilot.nio;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.READ;

public class InputOutputStreamTest {
	
	private static final Logger logger = LoggerFactory.getLogger(InputOutputStreamTest.class);

	/**
	 * Java 原生 API
	 * @throws IOException
	 */
	@Test
	public void testPath2InputStream() throws IOException {
		File initialFile = new File("D:\\awesome-copilot\\awesome-copilot-tests\\src\\test\\java\\com\\copilot\\nio\\test.txt");
		InputStream inputStream = new FileInputStream(initialFile);
		System.out.println(inputStream.available());
		inputStream.close();
	}
	
	/**
	 * NIO 方式
	 */
	@Test
	public void testPath2InputStreamUsingNIO() {
		Path path = Paths.get("D:\\awesome-copilot\\awesome-copilot-tests\\src\\test\\java\\com\\copilot\\nio\\test.txt");
		try (InputStream inputStream = java.nio.file.Files.newInputStream(path, READ)) {
			System.out.println(inputStream.available());
		} catch (IOException e) {
			logger.error("msg", e);
		}
	}
	
	/**
	 * Guava 
	 * @throws IOException
	 */
	@Test
	public void testPath2InputStreamUsingGuava() throws IOException {
		File initialFile = new File("D:\\awesome-copilot\\awesome-copilot-tests\\src\\test\\java\\com\\copilot\\nio\\test.txt");
		InputStream inputStream = Files.asByteSource(initialFile).openStream();
		System.out.println(inputStream.available());
	}
	
	/**
	 * Apache Commons IO
	 * @throws IOException 
	 */
	@Test
	public void testPath2InputStreamUsingCommonsIO() throws IOException {
		File initialFile = new File("D:\\awesome-copilot\\awesome-copilot-tests\\src\\test\\java\\com\\copilot\\nio\\test.txt");
		InputStream inputStream = FileUtils.openInputStream(initialFile);
		System.out.println(inputStream.available());
	}
}
