package com.handy.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 加密工具封装. CryptTool 封装了一些加密工具方法, 包括 3DES, MD5 等.
 * 
 * @author rocken.zeng@gmail.com(整理)
 * @version 1.0
 */
public class CryptTool {

    /**
	 * 构造函数.
	 * 
	 */
    public CryptTool() {
    }

    /**
	 * 生成3DES密钥.
	 * 
	 * @param key_byte
	 *            seed key
	 * @throws Exception
	 * @return javax.crypto.SecretKey Generated DES key
	 */
    public static javax.crypto.SecretKey genDESKey(byte[] key_byte) {
        SecretKey k = null;
        k = new SecretKeySpec(key_byte, "DESede");
        return k;
    }

    /**
	 * 3DES 解密(byte[]).
	 * 
	 * @param key
	 *            SecretKey
	 * @param crypt
	 *            byte[]
	 * @throws Exception
	 * @return byte[]
	 */
    public static byte[] desDecrypt(javax.crypto.SecretKey key, byte[] crypt) {
        byte[] bytes = null;
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DESede");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            bytes = cipher.doFinal(crypt);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        } catch (NoSuchPaddingException e) {
            log.error(e.getMessage());
        } catch (InvalidKeyException e) {
            log.error(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            log.error(e.getMessage());
        } catch (BadPaddingException e) {
            log.error(e.getMessage());
        }
        return bytes;
    }

    /**
	 * 3DES 解密(String).
	 * 
	 * @param key
	 *            SecretKey
	 * @param crypt
	 *            byte[]
	 * @throws Exception
	 * @return byte[]
	 */
    public static String desDecrypt(javax.crypto.SecretKey key, String crypt) {
        return new String(desDecrypt(key, crypt.getBytes()));
    }

    /**
	 * 3DES加密(byte[]).
	 * 
	 * @param key
	 *            SecretKey
	 * @param src
	 *            byte[]
	 * @throws Exception
	 * @return byte[]
	 */
    public static byte[] desEncrypt(javax.crypto.SecretKey key, byte[] src) {
        byte[] bytes = null;
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DESede");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            bytes = cipher.doFinal(src);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        } catch (NoSuchPaddingException e) {
            log.error(e.getMessage());
        } catch (InvalidKeyException e) {
            log.error(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            log.error(e.getMessage());
        } catch (BadPaddingException e) {
            log.error(e.getMessage());
        }
        return bytes;
    }

    /**
	 * 3DES加密(String).
	 * 
	 * @param key
	 *            SecretKey
	 * @param src
	 *            byte[]
	 * @throws Exception
	 * @return byte[]
	 */
    public static String desEncrypt(javax.crypto.SecretKey key, String src) {
        return new String(desEncrypt(key, src.getBytes()));
    }

    /**
	 * MD5 摘要计算(byte[]).
	 * 
	 * @param src
	 *            byte[]
	 * @throws Exception
	 * @return byte[] 16 bit digest
	 */
    public static byte[] md5Digest(byte[] src) {
        byte[] bytes = null;
        try {
            java.security.MessageDigest alg = java.security.MessageDigest.getInstance("MD5");
            bytes = alg.digest(src);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }
        return bytes;
    }

    /**
	 * MD5 摘要计算(String).
	 * 
	 * @param src
	 *            String
	 * @throws Exception
	 * @return String
	 */
    public static String md5Digest(String src) {
        return new String(md5Digest(src.getBytes()));
    }

    /**
	 * md5加密
	 * @return
	 */
    public static String md5(String str) {
        byte[] byteArray = null;
        try {
            byteArray = md5Digest(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        StringBuffer md5StrBuff = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i])); else md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return md5StrBuff.toString();
    }

    /**
	 * 对给定字符进行 URL 编码.
	 * 
	 * @param src
	 *            String
	 * @return String
	 */
    public static String urlEncode(String src, String charset) {
        try {
            src = java.net.URLEncoder.encode(src, charset);
            return src;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return src;
    }

    /**
	 * 对给定字符进行 URL 解码
	 * 
	 * @param value
	 *            解码前的字符串
	 * @return 解码后的字符串
	 */
    public static String urlDecode(String value, String charset) {
        try {
            return java.net.URLDecoder.decode(value, charset);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return value;
    }

    private static Log log = LogFactory.getLog(CryptTool.class);

    public static void main(String[] args) {
        System.out.println(CryptTool.md5("123"));
    }
}
