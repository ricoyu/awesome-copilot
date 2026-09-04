package com.awesomecopilot.codec;

import com.awesomecopilot.codec.exception.AESDecryptionException;
import com.awesomecopilot.codec.exception.AESEncryptionException;
import com.awesomecopilot.codec.exception.CipherInitializeException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class AesEncryptUtils {
	
	/**
	 * Cipher 非线程安全, 用 ThreadLocal 为每个线程缓存独立的 Cipher 实例, 兼顾线程安全与性能
	 */
	private static final ThreadLocal<Cipher> CIPHER_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
		try {
			return Cipher.getInstance("AES/CBC/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CipherInitializeException(e);
		}
	});
	
	/**
	 * 加密
	 *
	 * @param data
	 * @param key  必须16位
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String data, String key) {
		byte[] dataBytes = data.getBytes(UTF_8);
		
		byte[] bytes = key.getBytes(UTF_8);
		SecretKeySpec keyspec = new SecretKeySpec(bytes, "AES");
		IvParameterSpec ivspec = new IvParameterSpec(bytes);
		
		try {
			Cipher cipher = CIPHER_THREAD_LOCAL.get();
			int blockSize = cipher.getBlockSize();
			
			int plaintextLength = dataBytes.length;
			if (plaintextLength % blockSize != 0) {
				plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
			}
			
			byte[] plaintext = new byte[plaintextLength];
			System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
			
			cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
			byte[] encrypted = cipher.doFinal(plaintext);
			return Base64.encodeBase64URLSafeString(encrypted);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new AESEncryptionException(e);
		}
	}
	
	/**
	 * 解密
	 *
	 * @param data
	 * @param key  必须16位
	 * @return
	 * @throws Exception
	 */
	public static String decrypt(String data, String key) {
		try {
			byte[] encrypted = Base64.decodeBase64(data);
			
			Cipher cipher = CIPHER_THREAD_LOCAL.get();
			byte[] bytes = key.getBytes(UTF_8);
			SecretKeySpec keyspec = new SecretKeySpec(bytes, "AES");
			IvParameterSpec ivspec = new IvParameterSpec(bytes);
			
			cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
			
			byte[] original = cipher.doFinal(encrypted);
			String originalString = new String(original, UTF_8);
			return originalString.trim();
		} catch (Exception e) {
			throw new AESDecryptionException(e);
		}
	}
	
	/**
	 * 生成一个16位IDE随机字符串作为密钥
	 *
	 * @return
	 */
	public static String key() {
		return RandomStringUtils.randomAlphanumeric(16);
	}
	
}
