package com.awesomecopilot.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptPasswordEncoderTest {

	@Test
	public void testEncoder() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String encode = encoder.encode("123456");
		System.out.print(encode);
		// 验证密码（底层会自动解析盐值和成本因子）
		boolean isMatch = encoder.matches("123456", encode);
		System.out.println("密码是否匹配：" + isMatch); // 输出 true
	}
}