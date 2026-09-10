package com.awesomecopilot.codec;

import com.awesomecopilot.codec.exception.AESDecryptionException;
import com.awesomecopilot.codec.exception.AESEncryptionException;
import com.awesomecopilot.codec.exception.CipherInitializeException;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * AES 加密解密
 * AES 是一种对称加密算法
 * <p>
 * Copyright: Copyright (c) 2018-08-20 17:12
 * <p>
 * Company: DataSense
 * <p>
 *
 * @author Rico Yu\tricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class AesEncryptUtils {
	
	/**
	 * 当前加密模式: AES-256/GCM, 认证加密自带完整性校验, 每次加密随机 IV
	 */
	private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
	
	/**
	 * 历史遗留模式, 只为兼容存量密文与 JS 侧(CryptoJS 不支持 GCM)的老协议而保留
	 */
	private static final String LEGACY_TRANSFORMATION = "AES/CBC/NoPadding";
	
	/**
	 * GCM 推荐 IV 长度: 12 字节
	 */
	private static final int GCM_IV_LENGTH = 12;
	
	/**
	 * GCM 认证标签长度: 128 bit
	 */
	private static final int GCM_TAG_LENGTH_IN_BITS = 128;
	
	/**
	 * AES 分组长度, 老格式的 CBC 密文长度必须是它的整数倍
	 */
	private static final int BLOCK_SIZE = 16;
	
	/**
	 * 密文前缀, 用来区分新老两种格式。base64url 字母表里不含 $, 所以前缀不会与老密文混淆
	 */
	private static final String GCM_PREFIX = "gcm$";
	
	/**
	 * {@link #key()} 生成的口令长度, 与历史版本保持一致(16)
	 */
	private static final int PASSPHRASE_LENGTH = 16;
	
	/**
	 * 口令字母表: 大小写字母 + 数字, 与历史 RandomStringUtils.randomAlphanumeric 相同
	 */
	private static final char[] PASSPHRASE_ALPHABET =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	
	/**
	 * IV 必须用 CSPRNG 生成, SecureRandom 本身线程安全
	 */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
	/**
	 * Cipher 非线程安全, 用 ThreadLocal 为每个线程缓存独立的 Cipher 实例, 兼顾线程安全与性能。
	 * 老格式每次都要 Cipher.getInstance(变换串含模式差异且只在兼容分支用), 直接现取, 不复用缓存
	 */
	private static final ThreadLocal<Cipher> GCM_CIPHER = ThreadLocal.withInitial(() -> {
		try {
			return Cipher.getInstance(GCM_TRANSFORMATION);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CipherInitializeException(e);
		}
	});
	
	/**
	 * 加密
	 *
	 * @param data 待加密明文
	 * @param key  口令, 任意非空字符串(内部用 SHA-256 派生成 32 字节的 AES-256 密钥, 不再要求必须 16 位)
	 * @return base64url 安全字符串, 形如 {@code gcm$<随机IV + 密文 + 认证标签>}
	 */
	public static String encrypt(String data, String key) {
		Assert.notNull(data, "待加密数据不能为null");
		byte[] keyBytes = deriveKey(key);
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			SECURE_RANDOM.nextBytes(iv);
			
			Cipher cipher = GCM_CIPHER.get();
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_IN_BITS, iv));
			byte[] encrypted = cipher.doFinal(data.getBytes(UTF_8));
			
			// IV 随密文一起输出, 解密端无需另辟通道传递
			byte[] payload = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, payload, 0, iv.length);
			System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
			
			return GCM_PREFIX + Base64.encodeBase64URLSafeString(payload);
		} catch (GeneralSecurityException e) {
			throw new AESEncryptionException(e);
		} finally {
			Arrays.fill(keyBytes, (byte) 0);
		}
	}
	
	/**
	 * 解密, 按前缀自动识别格式:
	 * <ul>
	 *     <li>带 {@code gcm$} 前缀: 新版 AES-256/GCM</li>
	 *     <li>无前缀: 老格式 AES/CBC/NoPadding(IV 复用密钥 + 零填充), 兼容存量数据</li>
	 * </ul>
	 *
	 * @param data 密文
	 * @param key  加密时用的口令
	 * @return 明文, 首尾空白原样保留(老实现用 trim() 去零填充, 会连带删掉明文自身的首尾空白)
	 */
	public static String decrypt(String data, String key) {
		Assert.notNull(data, "密文不能为null");
		Assert.notNull(key, "密钥不能为null");
		try {
			if (data.startsWith(GCM_PREFIX)) {
				return decryptGcm(data.substring(GCM_PREFIX.length()), key);
			}
			return decryptLegacy(data, key);
		} catch (AESDecryptionException | IllegalArgumentException e) {
			// IllegalArgumentException 是口令/参数本身不合法的编程错误, 不能包装成"解密失败"掩盖真实原因
			throw e;
		} catch (Exception e) {
			throw new AESDecryptionException(e);
		}
	}
	
	/**
	 * 老格式加密: AES/CBC/NoPadding + IV 直接复用密钥字节 + 明文尾部补 \0。
	 * <p>
	 * 之所以还留着: ① 存量密文要能解开; ② 前端 CryptoJS 只支持 CBC, 老协议对接时得用它。
	 * 但它有三宗罪, 新代码请勿使用:
	 * <ol>
	 *     <li>IV 与密钥相同, 同一密钥下加密结果可预测, 属于弱实现</li>
	 *     <li>补的 \0 不带长度信息, 解密只能靠 trim()/找 \0 反推边界, 明文首尾的空白会被误删</li>
	 *     <li>CBC 无认证标签, 密文被篡改解密端无从察觉</li>
	 * </ol>
	 *
	 * @param data 待加密明文
	 * @param key  密钥, UTF-8 字节长度必须为 16(AES-128), 老格式的 IV 也得凑满 16 字节
	 * @return base64url 安全字符串
	 * @deprecated 请改用 {@link #encrypt(String, String)}
	 */
	@Deprecated
	public static String legacyEncrypt(String data, String key) {
		Assert.notNull(data, "待加密数据不能为null");
		byte[] keyBytes = legacyKeyBytes(key);
		try {
			Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
			byte[] dataBytes = data.getBytes(UTF_8);
			int blockSize = cipher.getBlockSize();
			
			int plaintextLength = dataBytes.length;
			if (plaintextLength % blockSize != 0) {
				plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
			}
			
			// new byte[] 默认全 0, 即零填充
			byte[] plaintext = new byte[plaintextLength];
			System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
			
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(keyBytes));
			byte[] encrypted = cipher.doFinal(plaintext);
			return Base64.encodeBase64URLSafeString(encrypted);
		} catch (GeneralSecurityException e) {
			throw new AESEncryptionException(e);
		} finally {
			Arrays.fill(keyBytes, (byte) 0);
		}
	}
	
	/**
	 * 生成一个16位的随机字符串作为口令
	 * <p>
	 * 注意: 返回的是 {@link #encrypt(String, String)} 可直接使用的口令, 不是 AES 原始密钥字节。
	 *
	 * @return 16 位大小写字母 + 数字随机串
	 */
	public static String key() {
		// 不用 RandomStringUtils.randomAlphanumeric: commons-lang3 3.4 内部用的是可预测的 java.util.Random,
		// 而带 Random 入参的重载要到 3.6 才有(本项目锁在 3.4). 口令是要拿去当 AES 密钥的, 必须用 CSPRNG
		char[] chars = new char[PASSPHRASE_LENGTH];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = PASSPHRASE_ALPHABET[SECURE_RANDOM.nextInt(PASSPHRASE_ALPHABET.length)];
		}
		return new String(chars);
	}
	
	private static String decryptGcm(String data, String key) throws GeneralSecurityException {
		// 先校验并派生密钥: 口令不合法是调用方的编程错误, 优先级高于密文格式错误
		byte[] keyBytes = deriveKey(key);
		byte[] payload = decodeBase64(data, "gcm密文");
		if (payload.length <= GCM_IV_LENGTH) {
			throw new AESDecryptionException("gcm密文长度不足, 至少需要" + (GCM_IV_LENGTH + 1) + "字节, 实际: " + payload.length);
		}
		
		byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
		byte[] cipherBytes = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);
		try {
			Cipher cipher = GCM_CIPHER.get();
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_IN_BITS, iv));
			// doFinal 内部校验认证标签, 密文被篡改时抛 AEADBadTagException
			byte[] original = cipher.doFinal(cipherBytes);
			return new String(original, UTF_8);
		} finally {
			Arrays.fill(keyBytes, (byte) 0);
			Arrays.fill(iv, (byte) 0);
			Arrays.fill(cipherBytes, (byte) 0);
		}
	}
	
	private static String decryptLegacy(String data, String key) throws GeneralSecurityException {
		// 同 decryptGcm: 密钥校验优先于密文校验
		byte[] keyBytes = legacyKeyBytes(key);
		byte[] encrypted = decodeBase64(data, "密文");
		if (encrypted.length == 0 || encrypted.length % BLOCK_SIZE != 0) {
			throw new AESDecryptionException("旧格式密文长度必须是" + BLOCK_SIZE + "字节的整数倍, 实际: " + encrypted.length);
		}
		
		try {
			Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
			// 老格式的 IV 就是密钥本身, 两者共用同一个数组
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(keyBytes));
			
			byte[] original = cipher.doFinal(encrypted);
			return stripZeroPadding(new String(original, UTF_8));
		} finally {
			Arrays.fill(keyBytes, (byte) 0);
		}
	}
	
	/**
	 * 把任意长度的口令派生成 32 字节密钥(AES-256), 这样口令长度不合法时不会拖到 Cipher.init
	 * 才抛出信息毫无提示价值的 InvalidKeyException。
	 * <p>
	 * 用 SHA-256 而非 PBKDF2/Argon2: 这里的口令由 {@link #key()} 随机生成(16 位字母数字, 约 95 bit 熵),
	 * 不存在弱口令离线爆破的场景, 不必付 KDF 的迭代开销; 若口令来自用户输入的密码, 请改用加盐 PBKDF2。
	 */
	private static byte[] deriveKey(String key) {
		Assert.notNull(key, "密钥不能为null");
		byte[] raw = key.getBytes(StandardCharsets.UTF_8);
		if (raw.length == 0) {
			throw new IllegalArgumentException("密钥不能为空字符串");
		}
		try {
			return MessageDigest.getInstance("SHA-256").digest(raw);
		} catch (NoSuchAlgorithmException e) {
			throw new CipherInitializeException(e);
		} finally {
			Arrays.fill(raw, (byte) 0);
		}
	}
	
	/**
	 * 还原老格式要求的密钥字节。老格式直接把口令 UTF-8 字节喂给 SecretKeySpec, 且 IvParameterSpec
	 * 只接受 16 字节, 所以历史上能跑通的口令恒为 16 字节 —— 这里必须逐字节保持一致, 否则存量密文解不开。
	 */
	private static byte[] legacyKeyBytes(String key) {
		Assert.notNull(key, "密钥不能为null");
		byte[] raw = key.getBytes(StandardCharsets.UTF_8);
		if (raw.length != BLOCK_SIZE) {
			Arrays.fill(raw, (byte) 0);
			throw new IllegalArgumentException("旧格式(AES/CBC)密钥的UTF-8字节长度必须为" + BLOCK_SIZE + ", 当前: " + key.length() + "个字符, 请改用encrypt()/decrypt()");
		}
		return raw;
	}
	
	/**
	 * 去掉老格式尾部补出来的 \0。
	 * <p>
	 * 不能图省事用 {@link String#trim()}: 零填充只补在尾部, trim() 却会把明文自身的首尾空白
	 * (空格、\t、\n)一并删掉。这里只从尾部剥连续的 \0, 明文首尾的真实内容原样保留。
	 */
	private static String stripZeroPadding(String decrypted) {
		int end = decrypted.length();
		while (end > 0 && decrypted.charAt(end - 1) == '\0') {
			end--;
		}
		return decrypted.substring(0, end);
	}
	
	private static byte[] decodeBase64(String data, String label) {
		// commons-codec 对非法字符是静默忽略的, 解出空数组必须显式报错, 否则会把脏输入当成"合法短密文"
		byte[] decoded = Base64.decodeBase64(data);
		if (decoded.length == 0) {
			throw new AESDecryptionException(label + "base64解码失败或内容为空");
		}
		return decoded;
	}
	
}
