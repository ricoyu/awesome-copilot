package com.awesomecopilot.jackson.javatime;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import org.junit.Test;

/**
 * <p>
 * Copyright: (C), 2020/5/7 19:24
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class StringBasedObjectMapperDeserializeEpocMillisTest {
	
	@Test
	public void test() {
		String json = IOUtils.readClassPathFileAsString("check_token_response.json");
		TokenInfo tokenInfo = JacksonUtils.toObject(json, TokenInfo.class);
		System.out.println(tokenInfo.getExp());
	}
}
