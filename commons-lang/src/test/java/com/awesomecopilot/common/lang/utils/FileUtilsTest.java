package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import static com.awesomecopilot.common.lang.utils.FileUtils.cleanFilename;
import static org.assertj.core.api.Assertions.*;

class FileUtilsTest {

	@Test
	void testPureEnglishFilename() {
		String originalFilename = "DSC00064.JPG";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("b99e560c5746_dsc00064.jpg");

		// 多次调用验证确定性
		for (int i = 0; i < 100; i++) {
			assertThat(cleanFilename(originalFilename))
					.isEqualTo("b99e560c5746_dsc00064.jpg");
		}
		assertThat(cleanFilename("b99e560c5746_dsc00064.jpg")).isEqualTo("b99e560c5746_dsc00064.jpg");
	}

	@Test
	void testWithSpacesAndSpecialChars() {
		String originalFilename = "My File @2025!.PDF";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("1096efae9c16_my_file_2025.pdf");
		assertThat(cleanFilename("1096efae9c16_my_file_2025.pdf")).isEqualTo("1096efae9c16_my_file_2025.pdf");
	}

	@Test
	void testPureChineseFilename() {
		String originalFilename = "这是一个测试文件.docx";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("d9b1c8a6ec35_这是一个测试文件.docx");
		assertThat(cleanFilename("d9b1c8a6ec35_这是一个测试文件.docx")).isEqualTo("d9b1c8a6ec35_这是一个测试文件.docx");
	}

	@Test
	void testChineseWithSpecialAndSpaces() {
		String originalFilename = " 报告 #2025 (最终版).xlsx ";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("37545fd3a5f0_报告_2025_最终版.xlsx");
		assertThat(cleanFilename("37545fd3a5f0_报告_2025_最终版.xlsx")).isEqualTo("37545fd3a5f0_报告_2025_最终版.xlsx");
	}

	@Test
	void testDotAtBeginning() {
		String originalFilename = ".hiddenfile";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("1074d22d298b_.hiddenfile");
		assertThat(cleanFilename("1074d22d298b_.hiddenfile")).isEqualTo("1074d22d298b_.hiddenfile");
	}

	@Test
	void testAllSpecialChars() {
		String originalFilename = "!@#$%^&*().zip";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("837ddf55c8af_unknown.zip");
		assertThat(cleanFilename("837ddf55c8af_unknown.zip")).isEqualTo("837ddf55c8af_unknown.zip");
	}

	@Test
	void testNoExtension() {
		String originalFilename = "README";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("5fcba805ac88_readme");
		assertThat(cleanFilename("5fcba805ac88_readme")).isEqualTo("5fcba805ac88_readme");
	}

	@Test
	void testMultipleDots() {
		String originalFilename = "archive.tar.gz";
		String result = cleanFilename(originalFilename);
		System.out.println("原始文件名: " + originalFilename + " => 清洗后的文件名: " + result);

		assertThat(result).isEqualTo("36bc63840537_archive.tar.gz");
		assertThat(cleanFilename("36bc63840537_archive.tar.gz")).isEqualTo("36bc63840537_archive.tar.gz");
	}

	@Test
	void testNullOrBlank() {
		String originalFilename1 = null;
		String result1 = cleanFilename(originalFilename1);
		System.out.println("原始文件名: " + originalFilename1 + " => 清洗后的文件名: " + result1);
		assertThat(result1).isEqualTo("unknown_file");
		assertThat(cleanFilename("unknown_file")).isEqualTo("unknown_file");

		String originalFilename2 = "   ";
		String result2 = cleanFilename(originalFilename2);
		System.out.println("原始文件名: \"" + originalFilename2 + "\" => 清洗后的文件名: " + result2);
		assertThat(result2).isEqualTo("unknown_file");
	}

	@Test
	void testIdempotentBehavior() {
		String originalFilename = "DSC00064.JPG";
		String firstResult = cleanFilename(originalFilename);
		System.out.println("第一次清洗 - 原始: " + originalFilename + " => " + firstResult);

		// 第二次传入已经 clean 过的文件名
		String cleanedFilename = firstResult; // 比如 b99e560c5746_dsc00064.jpg
		String secondResult = cleanFilename(cleanedFilename);
		System.out.println("第二次清洗（幂等） - 原始: " + cleanedFilename + " => 清洗后的文件名: " + secondResult);

		assertThat(secondResult).isEqualTo(cleanedFilename);
		assertThat(secondResult).isEqualTo("b99e560c5746_dsc00064.jpg");

		// 额外验证：即使哈希不对，只要格式匹配也直接返回（当前正则检测）
		String fakeCleaned = "aaaaaaaaaaaa_myfile.jpg";
		String fakeResult = cleanFilename(fakeCleaned);
		System.out.println("伪造的clean文件名: " + fakeCleaned + " => " + fakeResult);
		assertThat(fakeResult).isEqualTo(fakeCleaned);
	}
}