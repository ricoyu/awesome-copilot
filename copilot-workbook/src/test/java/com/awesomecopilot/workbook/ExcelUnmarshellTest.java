package com.awesomecopilot.workbook;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.workbook.exception.ValidationException;
import com.awesomecopilot.workbook.unmarshal.ExcelUnmarshaller;
import com.awesomecopilot.workbook.pojo.FaPiao;
import com.awesomecopilot.workbook.pojo.SettlementItem;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class ExcelUnmarshellTest {
	
	@Test(expected = ValidationException.class)
	public void testUnmarshall() throws Exception {
		File file = IOUtils.readClasspathFileAsFile("excel/1005466.xlsx");
		ExcelUnmarshaller unmarshaller = ExcelUnmarshaller.builder(file)
				.sheetName("Sheet1")
				.fallbackSheetIndex(0)
				.pojoType(SettlementItem.class)
				.validate(true)
				.build();
		
		List<SettlementItem> settlementItems = unmarshaller.unmarshall();
		settlementItems.forEach(System.out::println);
	}
	
	@Test
	public void testUnmarshall11() throws Exception {
		//		Class.forName("com.awesomecopilot.commons.utils.DateUtils");
		File file = IOUtils.readClasspathFileAsFile("excel/1005466.xlsx");
		//		Workbook workbook = ExcelUtils.getWorkbook(IOUtils.readClasspathFileAsFile("excel/958395-one.csv"));
		ExcelUnmarshaller unmarshaller = ExcelUnmarshaller.builder(file)
				.sheetName("Sheet1")
				//.fallbackSheetIndex(0)
				.pojoType(SettlementItem.class)
				.validate(true)
				.build();
		long total = 0;
		//		for (int i = 0; i < 10; i++) {
		long begin = System.currentTimeMillis();
		List<SettlementItem> settlementItems = unmarshaller.unmarshall();
		long end = System.currentTimeMillis();
		System.out.println("Total row : " + settlementItems.size() + ", Cost " + (end - begin) + " miliseconds");
		total += (end - begin);
		unmarshaller.reset();
		//		}
		System.out.println("Average time: " + total / 10);
	}
	
	@Test
	public void testUnmarshall2() throws Exception {
		File file = IOUtils.readClasspathFileAsFile("excel/gys-apple.xls");
		ExcelUnmarshaller unmarshaller = ExcelUnmarshaller.builder(file)
				.sheetName("Sheet1")
				.fallbackSheetIndex(0)
				.pojoType(FaPiao.class)
				.build();
		long begin = System.currentTimeMillis();
		List<FaPiao> settlementItems = unmarshaller
				.unmarshall();
		long end = System.currentTimeMillis();
		System.out.println("Total row : " + settlementItems.size() + ", Cost " + (end - begin) + " miliseconds");
		//		System.out.println(toJson(settlementItems));
	}
	
}
