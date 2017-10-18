package edu.xjtu.se.hcy.secondencrypt;

import java.security.MessageDigest;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Coder {

    /**
	 * MAC算法可选以下多种算法
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
        MessageDigest md5 = MessageDigest.getInstance("MD5");
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
        MessageDigest sha = MessageDigest.getInstance("SHA");
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
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacMD5");
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
        SecretKey secretKey = new SecretKeySpec(key, "HmacMD5");
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        return mac.doFinal(data);
    }
}
