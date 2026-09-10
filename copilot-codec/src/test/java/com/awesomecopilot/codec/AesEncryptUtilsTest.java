package com.awesomecopilot.codec;

import com.awesomecopilot.codec.exception.AESDecryptionException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesEncryptUtils 单元测试
 * <p>
 * Copyright: Copyright (c) 2026
 * <p>
 * Company: DataSense
 * <p>
 *
 * @author Rico Yu\tricoyu520@gmail.com
 * @version 1.0
 */
public class AesEncryptUtilsTest {
	
	/**
	 * 历史上前端 CryptoJS(AES/CBC/ZeroPadding, IV=密钥)加密产生的存量密文, 用于验证向后兼容
	 */
	private static final String LEGACY_CIPHERTEXT = "sjKlu/pgQ7gy79KjWqKpb4lMmRp/6FaunjWLG2s2jN1fqwwp+LXAKqHN1XuKycQ7FLQNj5UKuR2qM3yszUWgm2cH1GbfP/PdPLsss2Ce3Ru93GxleLwlu/0208m+TCJEfRePWzkcoPp4gNdCHeTSZ+2D+2MSzAEqcOp/UOV+P5yVwnyplLTlKv+5FUyBbjFG";
	
	private static final String LEGACY_KEY = "VkqnmlLM3zFXlyzv";
	
	private static final String LEGACY_PLAINTEXT = "uri=api/v1/resources&access_token=8B9OkolEY\u4f60\u597dIFR807wLTLModwJJSypMMVVW2i3haoiWWBpSOLEoGqSZygtn4WYLyCQz8&timestamp=1534323558718";
	
	@Test
	public void testKeyIsRandom16Alphanumeric() {
		String key = AesEncryptUtils.key();
		assertEquals(16, key.length());
		assertTrue(key.matches("[A-Za-z0-9]+"));
		// SecureRandom 下连续生成不应重复
		assertNotEquals(key, AesEncryptUtils.key());
	}
	
	@Test
	public void testEncryptDecryptRoundTrip() {
		String key = AesEncryptUtils.key();
		assertEquals("\u4f60\u597d, AES-256/GCM!", AesEncryptUtils.decrypt(AesEncryptUtils.encrypt("\u4f60\u597d, AES-256/GCM!", key), key));
		assertTrue(AesEncryptUtils.encrypt("x", key).startsWith("gcm$"));
	}
	
	@Test
	public void testEmptyPlaintext() {
		String key = AesEncryptUtils.key();
		assertEquals("", AesEncryptUtils.decrypt(AesEncryptUtils.encrypt("", key), key));
	}
	
	/**
	 * 同一明文同一密钥两次加密必须产生不同密文(随机 IV), 且都能解回原文
	 */
	@Test
	public void testRandomIvProducesDistinctCiphertexts() {
		String key = AesEncryptUtils.key();
		String first = AesEncryptUtils.encrypt("same message", key);
		String second = AesEncryptUtils.encrypt("same message", key);
		assertNotEquals(first, second);
		assertEquals("same message", AesEncryptUtils.decrypt(first, key));
		assertEquals("same message", AesEncryptUtils.decrypt(second, key));
	}
	
	/**
	 * GCM 自带认证标签, 密文被篡改必须解密失败而不是返回错误明文
	 */
	@Test
	public void testTamperedCiphertextRejected() {
		String key = AesEncryptUtils.key();
		String encrypted = AesEncryptUtils.encrypt("transfer 100 to bob", key);
		char last = encrypted.charAt(encrypted.length() - 1);
		String tampered = encrypted.substring(0, encrypted.length() - 1) + (last == 'A' ? 'B' : 'A');
		assertThrows(AESDecryptionException.class, () -> AesEncryptUtils.decrypt(tampered, key));
	}
	
	@Test
	public void testWrongKeyRejected() {
		String encrypted = AesEncryptUtils.encrypt("secret", AesEncryptUtils.key());
		assertThrows(AESDecryptionException.class, () -> AesEncryptUtils.decrypt(encrypted, AesEncryptUtils.key()));
	}
	
	/**
	 * 任意长度的口令都能用(内部 SHA-256 派生), 不再要求"必须16位",
	 * 也不会拖到 Cipher.init 才抛晦涩的 InvalidKeyException
	 */
	@Test
	public void testAnyLengthPassphraseWorks() {
		String key = "\u53e3\u4ee4\u53ef\u4ee5\u662f\u4efb\u610f\u957f\u5ea6\u7684\u4e2d\u6587\u4e32";
		assertEquals("\u957f\u53e3\u4ee4", AesEncryptUtils.decrypt(AesEncryptUtils.encrypt("\u957f\u53e3\u4ee4", key), key));
	}
	
	@Test
	public void testNullAndEmptyKeyFailFast() {
		String key = AesEncryptUtils.key();
		assertThrows(IllegalArgumentException.class, () -> AesEncryptUtils.encrypt("data", null));
		assertThrows(IllegalArgumentException.class, () -> AesEncryptUtils.encrypt("data", ""));
		assertThrows(IllegalArgumentException.class, () -> AesEncryptUtils.decrypt("gcm$AAAA", null));
		assertThrows(IllegalArgumentException.class, () -> AesEncryptUtils.decrypt("gcm$AAAA", ""));
	}
	
	@Test
	public void testJunkCiphertextRejected() {
		String key = AesEncryptUtils.key();
		assertThrows(AESDecryptionException.class, () -> AesEncryptUtils.decrypt("!!!\u4e0d\u662fbase64!!!", key));
		// 无前缀按老格式解, 长度不是 16 的整数倍要快速失败
		assertThrows(AESDecryptionException.class, () -> AesEncryptUtils.decrypt("QUJDRA", key));
	}
	
	/**
	 * 存量兼容: 老格式(JS CryptoJS 产出)密文仍能被 decrypt 解开
	 */
	@Test
	public void testLegacyCiphertextStillDecrypts() {
		assertEquals(LEGACY_PLAINTEXT, AesEncryptUtils.decrypt(LEGACY_CIPHERTEXT, LEGACY_KEY));
	}
	
	/**
	 * legacyEncrypt 与升级前的 encrypt 行为逐字节一致(能复现存量 JS 密文)
	 */
	@Test
	public void testLegacyEncryptByteCompatibleWithJs() {
		assertEquals(LEGACY_CIPHERTEXT.replace('+', '-').replace('/', '_'),
				AesEncryptUtils.legacyEncrypt(LEGACY_PLAINTEXT, LEGACY_KEY));
	}
	
	@Test
	public void testLegacyRejectsNon16ByteKey() {
		// 老格式的 IV 直接复用密钥字节, 长度必须恒为 16, 否则报错信息点名老格式
		assertThrows(IllegalArgumentException.class, () -> AesEncryptUtils.legacyEncrypt("data", "short-key"));
	}
	
	/**
	 * 老实现用 trim() 剥零填充, 会误删明文首尾空白; 新实现必须原样保留
	 */
	@Test
	public void testWhitespacePreserved() {
		String key = AesEncryptUtils.key();
		String padded = "  \u4e24\u8fb9\u6709\u7a7a\u767d\n";
		assertEquals(padded, AesEncryptUtils.decrypt(AesEncryptUtils.encrypt(padded, key), key));
	}
	
	/**
	 * 明文内部的 \0 字符: 老格式靠 \0 定位边界无法无损, GCM 应原样还原
	 */
	@Test
	public void testNulCharInsidePlaintextPreserved() {
		String key = AesEncryptUtils.key();
		String withNul = "a\u0000b";
		assertEquals(withNul, AesEncryptUtils.decrypt(AesEncryptUtils.encrypt(withNul, key), key));
	}
	
	/**
	 * ThreadLocal 缓存的 Cipher 在并发下必须互不干扰
	 */
	@Test
	public void testConcurrentEncryptDecrypt() throws Exception {
		int threads = 8;
		int rounds = 200;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		Set<Future<Boolean>> results = new HashSet<>();
		try {
			for (int t = 0; t < threads; t++) {
				final int tid = t;
				results.add(pool.submit(() -> {
					for (int i = 0; i < rounds; i++) {
						String message = "thread-" + tid + "-iter-" + i + " \u4e2d\u6587";
						String key = AesEncryptUtils.key();
						if (!message.equals(AesEncryptUtils.decrypt(AesEncryptUtils.encrypt(message, key), key))) {
							return false;
						}
					}
					return true;
				}));
			}
		} finally {
			pool.shutdown();
			assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
		}
		for (Future<Boolean> result : results) {
			assertTrue(result.get());
		}
	}
}
