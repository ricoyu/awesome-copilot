package com.awesomecopilot.codec;

import com.awesomecopilot.codec.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class JwtUtilsTest {
	
	@Test
	public void testCreateJajaJwt() {
		String secret = "123456789abcdefghijklmnopqrstuvwxyz123456789abcdefghijklmnopqrstuvwxyz";
		String token = JwtUtils.createJWT(secret, "token", "999", 720000000000L);
		System.out.println(token);
		Claims claims = JwtUtils.parseJWT(secret, token);
		String subject = claims.getSubject();
		assertEquals(subject, "999");
	}

	/**
	 * 回归：createJWT/parseJWT/generateJwt 必须共用同一密钥派生方式，token 可互解析。
	 * 历史上 generateJwt 用 DEFAULT_SECRET.getBytes()（不解 base64）签，与 parseJWT 的
	 * base64 解码派生不兼容，validateJwt(generateJwt(...)) 恒为 false。
	 */
	@Test
	public void testKeyDerivationConsistency() {
		String secret = "123456789abcdefghijklmnopqrstuvwxyz123456789abcdefghijklmnopqrstuvwxyz";
		//1. generateJwt 的 token 能被 parseJWT(默认密钥) 解析、validateJwt 判为有效
		HashMap<String, Object> claims = new HashMap<>(8);
		claims.put("userName", "张三");
		String gen = JwtUtils.generateJwt(claims);
		assertTrue(JwtUtils.validateJwt(gen));
		Claims parsed = JwtUtils.parseJWT(gen);
		assertNotNull(parsed);
		assertEquals("张三", parsed.get("userName"));
		//2. createJWT 显式传 null/空密钥时回退 DEFAULT_SECRET，parseJWT(单参) 可解析
		String fallback = JwtUtils.createJWT(null, "id", "sub", 60L);
		assertEquals("sub", JwtUtils.parseJWT(fallback).getSubject());
		//3. 密钥不匹配、签名被篡改、alg 伪装的 token 一律拒收
		//（注意：不能只在 secret 尾部加 1 个字符——宽松的 base64 解码会丢弃不足 4 字符的
		//残组，派生出同一把密钥，这里换一把真正不同的密钥）
		String token = JwtUtils.createJWT(secret, "token", "999", 7200L);
		assertNull(JwtUtils.parseJWT("zyxwvutsrqponmlkjihgfedcba9876543210zyxwvutsrqponmlkjihgfedcba987654", token));
		String sig = token.substring(token.lastIndexOf('.') + 1);
		char flip = sig.charAt(0) == 'A' ? 'B' : 'A';
		assertNull(JwtUtils.parseJWT(secret,
				token.substring(0, token.lastIndexOf('.') + 1) + flip + sig.substring(1)));
		String forgedHeader = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"alg\":\"HS512\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertNull(JwtUtils.parseJWT(secret, forgedHeader + token.substring(token.indexOf('.'))));
	}
	@Test
	public void test() {
		// 2.设置用户信息
		HashMap<String, Object> claims = new HashMap<>(8);
		claims.put("id", "1");
		claims.put("userName", "张三");
		
		// 生成jwt
		String jwt = JwtUtils.generateJwt(claims);
		// 会生成类似该字符串的内容:  aaaa.bbbb.cccc
		log.info("生成的jwt:{}", jwt);
		
		//校验jwt
       Boolean validateFlag = JwtUtils.validateJwt(jwt);
       log.info("校验jwt结果:{}",validateFlag);
		
		//异常校验jwt
		//Boolean validateFlag2 = JwtUtils.validateJwt(jwt + 2);
		//log.info("校验jwt结果:{}", validateFlag2);
		
		//解析jwt的头部信息
		String jwtHeader = jwt.split("\\.")[0];
		log.info("头部信息:{}", jwtHeader);
		byte[] header = Base64.decodeBase64(jwtHeader.getBytes());
		log.info("解密头部信息:{}", new String(header));
		
		String pload = jwt.split("\\.")[1];
		log.info("pload信息:{}", pload);
		byte[] jwtPload = Base64.decodeBase64(pload.getBytes());
		log.info("解密头部信息:{}", new String(jwtPload));
		
		
	}
}
