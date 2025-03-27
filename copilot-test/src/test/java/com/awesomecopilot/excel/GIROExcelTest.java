package com.awesomecopilot.excel;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.workbook.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;

public class GIROExcelTest {

	@Test
	public void testRead() throws Exception {
		Workbook workbook = ExcelUtils.getWorkbook(IOUtils.readClasspathFileAsFile("excel/PCSCC - Sept'18 2nd GIRO DD.xlsx").toPath());
		
	}
}
