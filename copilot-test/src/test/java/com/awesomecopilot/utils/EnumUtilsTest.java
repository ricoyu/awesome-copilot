package com.awesomecopilot.utils;

import com.awesomecopilot.common.lang.utils.EnumUtils;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import org.junit.Test;

public class EnumUtilsTest {

	@Test
	public void testGetField() {
		ExportType preview = ExportType.PREVIEW;
		Object value = ReflectionUtils.getFieldValue("code", preview);
		System.out.println(value);

		Enum preview1 = EnumUtils.lookupEnum(ExportType.class, "PREVIEW");
		Enum anEnum = EnumUtils.lookupEnum(ExportType.class, "PREVIEW", "code");
		Enum anEnum1 = EnumUtils.lookupEnum(ExportType.class, 1);
		Enum anEnum2 = EnumUtils.lookupEnum(ExportType.class, 1, "code");
		System.out.println(ExportType.PREVIEW.ordinal());
	}

	public static enum ExportType {
	    /** 预览 */
	    PREVIEW,
	    /** 报告 */
	    REPORT
	    ;
	}
}