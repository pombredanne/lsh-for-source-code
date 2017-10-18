package edu.xjtu.se.hcy.encrypt;

import java.security.MessageDigest;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 基础加密组件
 * 
 */
public class Coder {

    public static final String KEY_SHA = "SHA";

    public static final String KEY_MD5 = "MD5";

    /**
	 * MAC算法可选以下多种算法
	 * 
	 * HmacMD5 
	 * HmacSHA1 
	 * HmacSHA256 
	 * HmacSHA384 
	 * HmacSHA512
	 */
    public static final String KEY_MAC = "HmacMD5";

    /**
	 * BASE64解密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
    public byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    /**
	 * BASE64加密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
    public String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }

    /**
	 * MD5加密
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
    public byte[] encryptMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(KEY_MD5);
        md5.update(data);
        return md5.digest();
    }

    /**
	 * SHA加密
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
    public byte[] encryptSHA(byte[] data) throws Exception {
        MessageDigest sha = MessageDigest.getInstance(KEY_SHA);
        sha.update(data);
        return sha.digest();
    }

    /**
	 * 初始化HMAC密钥
	 * 
	 * @return
	 * @throws Exception
	 */
    public byte[] initMacKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_MAC);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }

    /**
	 * HMAC加密
	 * 
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
    public byte[] encryptHMAC(byte[] data, byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, KEY_MAC);
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        return mac.doFinal(data);
    }
}
