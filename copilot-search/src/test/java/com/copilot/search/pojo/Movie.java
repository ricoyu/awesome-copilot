package com.copilot.search.pojo;

import com.copilot.search.annotation.DocId;
import com.copilot.search.annotation.Field;
import com.copilot.search.annotation.Index;
import com.copilot.search.enums.Analyzer;
import com.copilot.search.enums.FieldType;
import lombok.Data;

/**
 * <p>
 * Copyright: (C), 2021-02-08 12:58
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Data
@Index(value = "movie", numberOfShards = 1, numberOfReplicas = 0)
public class Movie {
	
	@DocId
	private Long id;
	
	@Field
	private Integer year;
	
	@Field(value = "title", type = FieldType.TEXT, analyzer = Analyzer.IK_MAX_WORD, searchAnalyzer = Analyzer.IK_SMART)
	private String title;
	
	@Field
	private String[] genre;
}
