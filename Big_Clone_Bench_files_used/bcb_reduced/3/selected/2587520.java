package com.rb.lottery.analysis.util;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @类功能说明: 提供了一个安全算法类,其中包括对称密码算法和散列算法
 * @类修改者:
 * @修改日期:
 * @修改说明:
 * @作者: robin
 * @创建时间: 2011-10-28 上午10:51:40
 * @版本: 1.0.0
 */
public final class Base64Coder {

    private static final BASE64Encoder base64encoder = new BASE64Encoder();

    private static final BASE64Decoder base64decoder = new BASE64Decoder();

    private static final String ENCRYPT_ENCODING = "gbk";

    private static final String ENCRYPT_KEY = "AD67EA2F3BE6E5ADD368DFE03120B5DF92A8FD8FEC2F0746";

    private static final String DES = "DES";

    private static final String HASH = "SHA-1";

    /**
	 * 加密字符串
	 */
    public static String base64Encrypt(String str) {
        String result = str;
        if (str != null && str.length() > 0) {
            try {
                byte[] encodeByte = Base64Coder.symmetricEncrypt(str.getBytes(ENCRYPT_ENCODING));
                result = base64encoder.encode(encodeByte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
	 * 解密字符串
	 */
    public static String base64Decrypt(String str) {
        String result = str;
        if (str != null && str.length() > 0) {
            try {
                byte[] encodeByte = base64decoder.decodeBuffer(str);
                byte[] decoder = Base64Coder.symmetricDecrypto(encodeByte);
                result = new String(decoder, ENCRYPT_ENCODING);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
	 * 对称加密方法
	 * 
	 * @param byteSource
	 *            需要加密的数据
	 * @return 经过加密的数据
	 * @throws Exception
	 */
    public static byte[] symmetricEncrypt(byte[] byteSource) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int mode = Cipher.ENCRYPT_MODE;
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
            byte[] keyData = ENCRYPT_KEY.getBytes();
            DESKeySpec keySpec = new DESKeySpec(keyData);
            Key key = keyFactory.generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(mode, key);
            byte[] result = cipher.doFinal(byteSource);
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            baos.close();
        }
    }

    /**
	 * 对称解密方法
	 * 
	 * @param byteSource
	 *            需要解密的数据
	 * @return 经过解密的数据
	 * @throws Exception
	 */
    public static byte[] symmetricDecrypto(byte[] byteSource) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int mode = Cipher.DECRYPT_MODE;
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
            byte[] keyData = ENCRYPT_KEY.getBytes();
            DESKeySpec keySpec = new DESKeySpec(keyData);
            Key key = keyFactory.generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(mode, key);
            byte[] result = cipher.doFinal(byteSource);
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            baos.close();
        }
    }

    /**
	 * 散列算法
	 * 
	 * @param byteSource
	 *            需要散列计算的数据
	 * @return 经过散列计算的数据
	 * @throws Exception
	 */
    public static byte[] hashMethod(byte[] byteSource) throws Exception {
        try {
            MessageDigest currentAlgorithm = MessageDigest.getInstance(HASH);
            currentAlgorithm.reset();
            currentAlgorithm.update(byteSource);
            return currentAlgorithm.digest();
        } catch (Exception e) {
            throw e;
        }
    }
}
