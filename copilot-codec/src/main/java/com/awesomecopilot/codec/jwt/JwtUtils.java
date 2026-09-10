package com.awesomecopilot.codec.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * JWT 工具类，底层 jjwt 0.12.x。
 * <p>
 * 密钥派生只有一条路径：{@link #getKeyInstance(String)}——把 secretKey 当作 base64 文本解码成
 * HMAC-SHA256 密钥字节。createJWT / parseJWT / generateJwt 全部走它，因此三条 API 生成的
 * token 互相可解析（历史上 generateJwt 用 DEFAULT_SECRET.getBytes() 直接派生，与另两个方法
 * 不兼容，导致 validateJwt(generateJwt(...)) 恒为 false）。
 * <p>
 * 注意：secretKey 传入的必须是 base64 文本；DatatypeConverter 是宽松解码，会丢弃非法字符以及
 * 末尾不足 4 字符的残组，所以“看起来不一样”的两个口令（如仅在尾部多 1 个字符）可能派生出同一
 * 把密钥。按 RFC 7518，HS256 要求密钥 >=256 bit，不满足时 jjwt 在 compact() 抛
 * SignatureException（包装 WeakKeyException）。generateJwt 固定使用 DEFAULT_SECRET 签发，
 * 须用 parseJWT(String) 单参重载解析。
 */
public final class JwtUtils {
	
	private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
	
	/**
	 * 秘钥（base64 文本）
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
	 * @param secretKey base64 编码的密钥，为空时使用 DEFAULT_SECRET
	 * @param id
	 * @param subject
	 * @param expireSecond 有效期秒数，为 null 用默认值，为负数则不带过期时间
	 * @return
	 */
	public static String createJWT(String secretKey, String id, String subject, Long expireSecond) {
		if (isBlank(secretKey)) {
			secretKey = DEFAULT_SECRET;
		}
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		//设置jwt的body
		JwtBuilder builder = Jwts.builder()
				//设置jti(JWT ID)：是JWT的唯一标识，根据业务需要，这个可以设置为一个不重复的值，主要用来作为一次性token,从而回避重放攻击。
				.id(id)
				//jwt的签发时间
				.issuedAt(now)
				//代表这个JWT的主体，即它的所有人，这个是一个json格式的字符串
				.subject(subject)
				//签名算法固定 HS256：密钥字节数可能 >=384 bit，交给 jjwt 按长度推断会选成 HS512 而解析失败
				.signWith(getKeyInstance(secretKey), Jwts.SIG.HS256);
		//expireSecond 为 null 时回退到默认有效期，避免自动拆箱 NPE
		long ttMillis = (expireSecond == null ? expirationTimeInSecond : expireSecond) * 1000;
		if (ttMillis >= 0) {
			long expMillis = nowMillis + ttMillis;
			Date exp = new Date(expMillis);
			//设置过期时间
			builder.expiration(exp);
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
	 * @param secretKey base64 编码的密钥，为空时使用 DEFAULT_SECRET
	 * @param jwtStr
	 * @return
	 */
	public static Claims parseJWT(String secretKey, String jwtStr) {
		if (isBlank(secretKey)) {
			secretKey = DEFAULT_SECRET;
		}
		try {
			return Jwts.parser()
					//设置签名的秘钥。密钥固定命名为 HmacSHA256，jjwt 会拒绝 alg 与之不符的
					//token（HS384/512 要求 HmacSHA384/512），parseSignedClaims 也拒绝 alg=none，
					//无需再配 sig() 白名单——且 sig().clear() 会因算法表为空直接抛异常
					.verifyWith(getKeyInstance(secretKey))
					.build()
					//设置需要解析的jwt
					.parseSignedClaims(jwtStr).getPayload();
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
		//parseJWT 对非法/过期 token 返回 null，需判空避免 NPE
		Claims claims = parseJWT(token);
		return claims == null ? null : claims.getExpiration();
	}
	
	/**
	 * 判断jwt是否过期
	 *
	 * @param token jwt令牌
	 * @return true 表示过期, false 没有过期
	 */
	private static Boolean isTokenExpired(String token) {
		Date expiration = getExpirationDateFromToken(token);
		//无法解析出过期时间（token 非法或已失效）视为已过期
		if (expiration == null) {
			return true;
		}
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
	 * 用 DEFAULT_SECRET 指定用户生成token，生成的 token 可直接交给 parseJWT(token)/validateJwt 校验。
	 * 支持的算法详见：https://github.com/jwtk/jjwt#features
	 *
	 * @param claims 用户信息
	 * @return String jwt Token
	 */
	public static String generateJwt(Map<String, Object> claims) {
		Date createdTime = new Date();
		Date expirationTime = getExpirationTime();
		
		return Jwts.builder()
				//claims() 是替换语义（等价旧的 setClaims），放在 issuedAt/expiration 之前，
				//避免 map 里同名的 iat/exp 覆盖掉后面设置的值
				.claims().add(claims).and()
				.issuedAt(createdTime)
				.expiration(expirationTime)
				.signWith(getKeyInstance(DEFAULT_SECRET), Jwts.SIG.HS256)
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
	 * 获取密钥：唯一的密钥派生入口。secretKey 按 base64 解码，算法固定 HmacSHA256。
	 *
	 * @return SecretKey
	 */
	private static SecretKey getKeyInstance(String secretKey) {
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
		return new SecretKeySpec(apiKeySecretBytes, "HmacSHA256");
	}
	
}
