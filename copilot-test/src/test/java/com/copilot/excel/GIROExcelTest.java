package com.copilot.excel;

import com.copilot.common.lang.utils.IOUtils;
import com.copilot.workbook.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;

public class GIROExcelTest {

	@Test
	public void testRead() throws Exception {
		Workbook workbook = ExcelUtils.getWorkbook(IOUtils.readClasspathFileAsFile("excel/PCSCC - Sept'18 2nd GIRO DD.xlsx").toPath());
		
	}
}
