package com.awesomecopilot.search8x.pojo;

import com.awesomecopilot.search8x.annotation.DocId;
import com.awesomecopilot.search8x.annotation.Field;
import com.awesomecopilot.search8x.annotation.Index;
import com.awesomecopilot.search8x.enums.Analyzer;
import com.awesomecopilot.search8x.enums.FieldType;
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
