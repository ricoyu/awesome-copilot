package com.awesomecopilot.codec.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import jakarta.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public final class JwtUtils {
	
	private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
	
	/**
	 * 秘钥
	 */
	private final static String DEFAULT_SECRET =
			"smlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlzsmlz";
	/**
	 * 有效期，单位秒
	 */
	private final static Long expirationTimeInSecond = 3600L;
	
	/**
	 * 生成 jwt
	 *
	 * @param id
	 * @param subject
	 * @return
	 */
	public static String createJWT(String secretKey, String id, String subject, Long expireSecond) {
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		//设置jwt的body
		JwtBuilder builder = Jwts.builder()
				//设置jti(JWT ID)：是JWT的唯一标识，根据业务需要，这个可以设置为一个不重复的值，主要用来作为一次性token,从而回避重放攻击。
				.setId(id)
				//jwt的签发时间
				.setIssuedAt(now)
				//代表这个JWT的主体，即它的所有人，这个是一个json格式的字符串
				.setSubject(subject)
				//设置签名使用的签名算法和签名使用的秘钥
				.signWith(SignatureAlgorithm.HS256, getKeyInstance(secretKey));
		long ttMillis = expireSecond * 1000;
		if (ttMillis >= 0) {
			long expMillis = nowMillis + ttMillis;
			Date exp = new Date(expMillis);
			//设置过期时间
			builder.setExpiration(exp);
		}
		return builder.compact();
	}
	
	/**
	 * 用默认的secret解析jwt，验证失败返回null
	 * @param token
	 * @return
	 */
	public static Claims parseJWT(String token) {
		return parseJWT(DEFAULT_SECRET, token);
	}
	/**
	 * 解析jwt，验证失败返回null
	 *
	 * @param jwtStr
	 * @return
	 */
	public static Claims parseJWT(String secretKey, String jwtStr) {
		if (isBlank(secretKey)) {
			secretKey = DEFAULT_SECRET;
		}
		try {
			Claims jwtClaims = Jwts.parser()
					//设置签名的秘钥
					.setSigningKey(getKeyInstance(secretKey))
					.build()
					//设置需要解析的jwt
					.parseClaimsJws(jwtStr).getBody();
			return jwtClaims;
		} catch (ExpiredJwtException expiredJwtException) {
			log.error("jwt has expired: {}", jwtStr);
		} catch (SignatureException signatureException) {
			log.error("jwt signature error: {}", jwtStr);
		} catch (Exception e) {
			log.error("jwt parse error: {}", jwtStr);
		}
		
		return null;
	}
	
	/**
	 * 获取jwt的过期时间
	 *
	 * @param token
	 * @return Date
	 */
	public static Date getExpirationDateFromToken(String token) {
		return parseJWT(token).getExpiration();
	}
	
	/**
	 * 判断jwt是否过期
	 *
	 * @param token jwt令牌
	 * @return true 表示过期, false 没有过期
	 */
	private static Boolean isTokenExpired(String token) {
		Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}
	
	/**
	 * 计算过期时间
	 *
	 * @return Date
	 */
	private static Date getExpirationTime() {
		return new Date(System.currentTimeMillis() + expirationTimeInSecond * 1000);
	}
	
	/**
	 * 指定用户生成token
	 * 支持的算法详见：https://github.com/jwtk/jjwt#features
	 *
	 * @param claims 用户信息
	 * @return String jwt Token
	 */
	public static String generateJwt(Map<String, Object> claims) {
		Date createdTime = new Date();
		Date expirationTime = getExpirationTime();
		
		
		byte[] keyBytes = DEFAULT_SECRET.getBytes();
		SecretKey key = Keys.hmacShaKeyFor(keyBytes);
		
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(createdTime)
				.setExpiration(expirationTime)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
	
	/**
	 * 校验token
	 *
	 * @param token jwt
	 * @return 未过期返回true, 否则返回false
	 */
	public static Boolean validateJwt(String token) {
		return !isTokenExpired(token);
	}
	
	/**
	 * 获取密钥
	 *
	 * @return
	 */
	private static Key getKeyInstance(String secretKey) {
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
		return signingKey;
	}
	
}