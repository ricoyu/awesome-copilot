package com.awesomecopilot.codec;

import com.awesomecopilot.codec.exception.CipherInitializeException;
import com.awesomecopilot.codec.exception.DESDecryptionException;
import com.awesomecopilot.codec.exception.DESEncryptionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * DES 加密解密
 * DES 是一种对称加密算法
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
public class DesEncryptUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(DesEncryptUtils.class);
	
	private static Cipher cipher = null;
	
	static {
		try {
			cipher = Cipher.getInstance("DES");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			logger.error("实例化Cipher失败", e);
			throw new CipherInitializeException(e);
		}
	}
	
	/**
	 * 加密
	 *
	 * @param data
	 * @param key  建议8位，非8位会自动处理
	 * @return
	 */
	public static String encrypt(String data, String key) {
		byte[] dataBytes = data.getBytes(UTF_8);
		
		try {
			SecretKeySpec keyspec = getDESSecretKey(key);
			cipher.init(Cipher.ENCRYPT_MODE, keyspec);
			byte[] encrypted = cipher.doFinal(dataBytes);
			return Base64.encodeBase64String(encrypted);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new DESEncryptionException(e);
		}
	}
	
	/**
	 * 解密
	 *
	 * @param data
	 * @param key  建议8位，非8位会自动处理
	 * @return
	 */
	public static String decrypt(String data, String key) {
		try {
			byte[] encrypted = Base64.decodeBase64(data);
			
			Cipher cipher = Cipher.getInstance("DES");
			SecretKeySpec keyspec = getDESSecretKey(key);
			
			cipher.init(Cipher.DECRYPT_MODE, keyspec);
			
			byte[] original = cipher.doFinal(encrypted);
			String originalString = new String(original, UTF_8);
			return originalString.trim();
		} catch (Exception e) {
			throw new DESDecryptionException(e);
		}
	}
	
	/**
	 * 生成一个8位随机字符串作为密钥
	 *
	 * @return
	 */
	public static String key() {
		return RandomStringUtils.randomAlphanumeric(8);
	}
	
	/**
	 * 获取DES密钥，处理密钥长度不足或超长的情况
	 *
	 * @param key 原始密钥字符串
	 * @return DES密钥
	 */
	public static SecretKeySpec getDESSecretKey(String key) {
		byte[] result = new byte[8];
		byte[] keys = key.getBytes(UTF_8);
		for(int i = 0; i < 8; i++){
			if(i < keys.length){
				result[i] = keys[i];
			}else{
				result[i] = 0x01;
			}
		}
		return new SecretKeySpec(result, "DES");
	}
	
}
